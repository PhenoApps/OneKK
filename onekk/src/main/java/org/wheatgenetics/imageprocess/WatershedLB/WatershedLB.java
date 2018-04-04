package org.wheatgenetics.imageprocess.WatershedLB;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.wheatgenetics.imageprocess.Seed.Seed;
import org.wheatgenetics.onekk.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opencv.imgproc.Imgproc.CC_STAT_AREA;
import static org.opencv.imgproc.Imgproc.CC_STAT_LEFT;
import static org.opencv.imgproc.Imgproc.CC_STAT_TOP;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by chaneylc on 8/22/2017.
 */

public class WatershedLB {

    private WatershedParams mParams;
    private Mat processedMat;
    private ArrayList<Seed> seedArrayList = null;
    double ssum = 0;

    /**
     * This class consists of variables and methods used to setup Watershed light box parameters
     *
     */
    public static class WatershedParams {
        protected int areaLow, areaHigh, defaultRate;
        protected double sizeLowerBoundRatio, newSeedDistRatio, pixelMetric;

        /**
         * Constructor to initialize the Watershed parameters before processing
         * <p>
         *  This is a convenience for calling
         * {@link org.wheatgenetics.imageprocess.WatershedLB.WatershedLB.WatershedParams#WatershedParams(int, int, int, double, double, double)}.
         * </p>
         *
         *  @param areaLow minimum area value of the seed
         *  @param areaHigh maximum hue value of the seed
         *  @param defaultRate default rate at which the seeds are flowing
         *  @param sizeLowerBoundRatio
         *  @param newSeedDistRatio
         */

        public WatershedParams(int areaLow, int areaHigh, int defaultRate,
                               double sizeLowerBoundRatio, double newSeedDistRatio, double pixelMetric) {
            this.areaLow = areaLow;
            this.areaHigh = areaHigh;
            this.defaultRate = defaultRate;
            this.sizeLowerBoundRatio = sizeLowerBoundRatio;
            this.newSeedDistRatio = newSeedDistRatio;
            this.pixelMetric = pixelMetric;
        }

        public int getAreaLow() { return areaLow; }

        public int getAreaHigh() { return areaHigh; }

        public int getDefaultRate() { return defaultRate; }

        public double getSizeLowerBoundRatio() { return sizeLowerBoundRatio; }

        public double getNewSeedDistRatio() { return newSeedDistRatio; }
    }

    /**
     * WatershedLB constructor to setup the watershed light box parameters
     * <p>
     *  This is a convenience for calling
     * {@link org.wheatgenetics.imageprocess.WatershedLB.WatershedLB#WatershedLB(WatershedParams)}.
     * </p>
     *
     */
    public WatershedLB(WatershedParams params) {
        mParams = params;
    }

    public Bitmap process(Bitmap inputBitmap) {
        seedArrayList = new ArrayList<>();
        Mat frame = new Mat();
        Utils.bitmapToMat(inputBitmap, frame);
        Mat ret = subProcess(frame);
        Utils.matToBitmap(ret, inputBitmap);
        processedMat = new Mat();
        processedMat = ret;
        return inputBitmap;
    }

    private Mat subProcess(Mat frame) {

        Imgproc.cvtColor(frame,frame,COLOR_BGR2RGB);

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);

        final List<Mat> images = new ArrayList<>();
        images.add(gray);

        Mat hist = new Mat();
        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0f, 256f));

        double pixels = 0;
        double hsum = 0.0;
        final ArrayList<Double> prod = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            final double pix = hist.get(i, 0)[0]; //256x1x1 matrix
            final double val = pix * i;
            prod.add(val);
            hsum = hsum + val;
            pixels = pixels + pix;
        }

        int ave = (int) (hsum / pixels);
        int thresh = 256;


        while (thresh > ave) {
            thresh = ave;
            hsum = 0;
            pixels = 0;
            for (int i = 0; i < ave + 1; i++) {
                hsum = hsum + prod.get(i);
                pixels = pixels + hist.get(i, 0)[0];
            }
            int aveLow = (int) (hsum / pixels);
            hsum = 0;
            pixels = 0;
            for (int i = ave + 1; i < 256; i++) {
                hsum = hsum + prod.get(i);
                pixels = pixels + hist.get(i, 0)[0];
            }
            int aveHigh = (int) (hsum / pixels);
            ave = (int) ((aveLow + aveHigh) * 0.5 + 1);

        }

        Mat binMat = new Mat();
        Imgproc.threshold(gray, binMat, thresh, 255, Imgproc.THRESH_BINARY_INV);
        //cvtColor(binMat, binMat, COLOR_BGR2GRAY);

        Mat opening = new Mat();
        Imgproc.morphologyEx(binMat, opening, Imgproc.MORPH_OPEN, Mat.ones(new Size(3,3), CvType.CV_8UC1));

        Mat sure_bg = new Mat();
        Imgproc.dilate(opening, sure_bg, Mat.ones(new Size(3,3), CvType.CV_8UC1));

        Mat dt = new Mat();
        Imgproc.distanceTransform(opening, dt, 2, 3);

        Mat sure_fg = new Mat();
        Imgproc.threshold(dt, sure_fg, 8, 255, 0);

        Mat unknown = new Mat();
        Core.subtract(sure_bg, sure_fg, unknown, new Mat(), CvType.CV_8UC1);

        /*Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/binMat.jpg",binMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/dt.jpg",dt);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/surefg.jpg",sure_fg);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/surebg.jpg",sure_bg);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/unknown.jpg",unknown);*/

        /*Mat invertMat = Mat.ones(sure_fg.size(), sure_fg.type()).setTo(new Scalar(255));

        Core.subtract(invertMat, sure_fg, unknown);
        unknown = 255 - sure_fg*/

        int connectivity = 8;
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        sure_fg.convertTo(sure_fg, CvType.CV_8UC1);

        /**
         * Chaney's implementation of calculating the max area threshold based on variance
         * MAX AREA THRESHOLD START
         */
        List<Double> areaList = new ArrayList<>();
        double sumArea = 0.0;
        double contourArea = 0;
        double meanArea = 0.0;
        double variance = 0.0;
        double maxAreaThreshold = 0.0;
        double minAreaThreshold = 0.0;

        int numObjects = Imgproc.connectedComponentsWithStats(sure_fg, labels, stats, centroids, connectivity, CvType.CV_32S);
        int[] textSize = new int[1];

        for (int k = 1; k < numObjects; k++) {
            final double area = stats.get(k, CC_STAT_AREA)[0]; // ConnectedComponentsTyps CC_STAT_AREA = 4
            final double cx = centroids.get(k, CC_STAT_LEFT)[0];
            final double cy = centroids.get(k, CC_STAT_TOP)[0];
            //Log.d("DEBUG : ","MARKER : " + k + " Centroid : (" + cx +"," + cy + ") has size : " + area);
            if (area < 20) {// && labels.get(n, m)[0] == k) {
                //markers[unknown==255] = 0
                //markers = markers[i] - 1
                //unknown[markers == i] = 255 # by setting to unknown, it will get zeroed out below
                //unknown[markers == i] = 0
                //Log.d("DEBUG","Removing Marker " + k);
                Mat mask = new Mat(labels.size(), labels.type());
                Scalar labelId = new Scalar(k, k, k);
                Core.inRange(labels, labelId, labelId, mask);
                unknown.copyTo(unknown, mask);
                mask.release();

               // Mat invMask = new Mat();
               // Core.invert(mask, invMask);
               // unknown.put(n, m, 255);
                //labels.put(n, m, 0);
            } else {
                areaList.add(area);
                sumArea = sumArea + area;
                Imgproc.circle(frame, new Point(cx, cy), 20, new Scalar(32, 64, 255), -1);
                Imgproc.getTextSize(String.valueOf(k), Core.FONT_HERSHEY_COMPLEX,0.5,1,textSize);
                Imgproc.putText(frame,String.valueOf(k),new Point(cx+(textSize[0]*3),cy+(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(32,64,255),2);
            }
        }

        meanArea = sumArea / areaList.size();
        for (Double are : areaList) {
            variance = variance + Math.pow((are - meanArea), 2);
        }


        maxAreaThreshold = meanArea + Math.pow(variance, 2);
        minAreaThreshold = meanArea - Math.pow(variance,2);

        /*Log.d("Average seed area", String.valueOf(meanArea));
        Log.d("Area Max Threshold", String.valueOf(maxAreaThreshold));
        Log.d("Area Min Threshold", String.valueOf(minAreaThreshold));*/

        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/contours.jpg",frame);
        //markers = markers + 1


        Core.add(labels, Mat.ones(labels.size(), labels.type()), labels);

        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2BGR);
        labels.convertTo(labels, CvType.CV_32S);
        Imgproc.watershed(gray, labels);
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/labels.jpg",labels);
        Mat borderMask = new Mat(labels.size(), labels.type());
        Scalar labelId = new Scalar(-1,-1,-1);
        Core.inRange(labels, labelId, labelId, borderMask);
        frame.copyTo(frame, borderMask);
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/frame-borderMask.jpg",frame);
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/borderMask.jpg",borderMask);
        borderMask.release();


        int i = 0;

        final ArrayList<Double> areas = new ArrayList<>();
        final ArrayList<Double> perimeters = new ArrayList<>();
        final ArrayList<Integer> seedCount = new ArrayList<>();
        final ArrayList<Double> cxCoord = new ArrayList<>();
        final ArrayList<Double> cyCoord = new ArrayList<>();
        final ArrayList<Point[]> cpoints = new ArrayList<>();

        //create unique set
        final Set<Double> unique = new HashSet<>();
        final int r = labels.rows();
        final int c = labels.cols();
        for (int m = 0; m < r - 8; m+=8) {
            for (int n = 0; n < c - 8; n+=8) {
                unique.add(labels.get(m, n)[0]);
            }
        }

        sure_bg.release();
        sure_fg.release();
        binMat.release();
        opening.release();
        dt.release();
        unknown.release();

        //Log.d("Unique labels", String.valueOf(unique.size()));
        for (Double label : unique) {
            if (label < 2) continue;
            Mat mask = Mat.zeros(labels.size(), CvType.CV_8U);
            int l = (int) label.doubleValue() + 1;
            Scalar colorLabel = new Scalar(l,l,l);
            Core.inRange(labels, colorLabel, colorLabel, mask);
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

            i = i + 1;
            final int countContour = contours.size();

            if (countContour > 0) {
                for (MatOfPoint matOfPoint : contours) {
                    double area = Imgproc.contourArea(matOfPoint);
                    if (area < minAreaThreshold || area > 32000) i = i - 1;
                    else {
                        final Point[] contourPoints = matOfPoint.toArray();
                        final MatOfPoint2f contour = new MatOfPoint2f(contourPoints);
                        double perimeter = Imgproc.arcLength(contour, true);
                        areas.add(area);
                        perimeters.add(perimeter);
                        seedCount.add(1);
                        cpoints.add(contourPoints);
                        ssum = ssum + area;

                        final Moments M = Imgproc.moments(contour);
                        final double cx = (int) (M.get_m10() / M.get_m00());
                        cxCoord.add(cx);
                        final double cy = (int) (M.get_m01() / M.get_m00());
                        cyCoord.add(cy);

                        Rect boundingRect = Imgproc.boundingRect(matOfPoint);
                        String strWidth = String.format("%.2f",boundingRect.width * mParams.pixelMetric);
                        String strHeight = String.format("%.2f",boundingRect.height * mParams.pixelMetric);

                        /* draw the bounding box on the mat */
                        Imgproc.rectangle(frame,boundingRect.tl(),boundingRect.br(),new Scalar(0,0,0),3);

                        /* draw the contours on the mat */
                        Imgproc.drawContours(frame,contours,-1, new Scalar(0,255,0),3);

                        /* draw a circle to mark the center of the seed */
                        Imgproc.circle(frame, new Point(cx, cy), 15, new Scalar(0,255,255), -1);

                        /* get the text size to write values on the mat */
                        Imgproc.getTextSize(String.valueOf(i), Core.FONT_HERSHEY_COMPLEX,0.5,1,textSize);

                        /* put a number on each seed, along with the width and height */
                        Imgproc.putText(frame,String.valueOf(i),new Point(cx-(textSize[0]*3),cy-(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(0,255,255),2);
                        Imgproc.putText(frame,strWidth,new Point(boundingRect.tl().x + ((boundingRect.width)/2)-(textSize[0]*5),boundingRect.tl().y -(textSize[0]*5)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(0,255,255),2);
                        Imgproc.putText(frame,strHeight,new Point(boundingRect.br().x +(textSize[0]*5),boundingRect.br().y - ((boundingRect.height)/2)  + (textSize[0]*5)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(0,255,255),2);

                        /* create a seed object for each seed and store them in an ArrayList */
                        seedArrayList.add(new Seed(cx,cy, area, perimeter, seedColor(frame,boundingRect), mParams.pixelMetric, boundingRect, matOfPoint));
                    }
                }

            }
            mask.release();
            hierarchy.release();
        }
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/uniqueFrame.jpg",frame);
        int count = i;
        int est_size = (int) (ssum / count);
        final List<Integer> bound = new ArrayList<>();
        bound.add(0);
        bound.add(200);
        bound.add((int)(1.5 * est_size));
        bound.add((int)(2.5 * est_size));
        bound.add((int)(3.5 * est_size));
        bound.add(20000);

        ssum = 0;
        double psum = 0;
        count = 0;
        int n = areas.size();

        for (i = 1; i < n; i++) {
            int seeds = 0;
            double area = areas.get(i);
            double perimeter = perimeters.get(i);
            for (int j = 0; j < 4; j++) {
                if (area >= bound.get(j)) {
                    seeds = j;
                    seedCount.set(i, j);
                }
            }
            count = count + seeds;
            ssum = ssum + area;
            psum = psum + perimeter;
        }

        est_size = (int) (ssum / count);
        int est_perimeter = (int) (psum / count);

        bound.set(0, 0);
        bound.set(1, 200);
        bound.set(2, (int) (1.5 * est_size));
        bound.set(3, (int) (2.5 * est_size));
        bound.set(4, (int) (3.5 * est_size));
        bound.add(20000);

        ssum = 0;
        psum = 0;
        count = 0;
        n = areas.size();

        int seeds = 0;
        for (i = 1; i < n; i++) {
            double area = areas.get(i);
            double perimeter = perimeters.get(i);
            for (int j = 0; j < 4; j++) {
                if (area >= bound.get(j)) {
                    seeds = j;
                    seedCount.set(i, j);
                }
            }
            if (seeds > 1 && perimeter > est_perimeter * 1.5) {
                double cx = cxCoord.get(i);
                double cy = cyCoord.get(i);
                //puttext
            } else {
                seeds = 1;
                seedCount.set(i, 1);
            }
            count = count + seeds;
            ssum = ssum = area;
            psum = psum + perimeter;
        }
        est_size = (int) (ssum / count);
        est_perimeter = (int) (psum / count);

        bound.set(0, 0);
        bound.set(1, 200);
        bound.set(2, (int) (1.5 * est_size));
        bound.set(3, (int) (2.5 * est_size));
        bound.set(4, (int) (3.5 * est_size));
        bound.add(20000);

        int delta = 4;
        for (i = 1; i < n; i++) {
            int numBig = 0;
            double dotSum = 0;
            double area = areas.get(i);
            double perimeter = perimeters.get(i);
            List<Double> xCoords = new ArrayList<>();
            List<Double> yCoords = new ArrayList<>();
            List<Double> dotProds = new ArrayList<>();
            if (area > est_size && perimeter > est_perimeter * 1.5) {
                Point[] points = cpoints.get(i);
                int nPoints = points.length;
                numBig = 0;
                double nextLargest = 0;
                double maxDotSum = 0;
                for (int j = 0; j < nPoints; j++) {
                    xCoords.add(points[j].x);
                    yCoords.add(points[j].y);
                    dotProds.add(0.0);
                }
                nPoints = (int) (nPoints / 2);
                for (int j = 0; j < nPoints; j++) {
                    dotProds.set(j, ((xCoords.get(j) - xCoords.get((j+nPoints-delta) % nPoints)) * (yCoords.get((j+delta) % nPoints)
                            - ((yCoords.get(j) - yCoords.get((j+nPoints-delta) % nPoints)) * (xCoords.get((j+delta) % nPoints) - xCoords.get(j))))));
                    if (dotProds.get(j) < 0) {
                        dotSum = dotSum - dotProds.get(j);
                    } else {
                        if (dotSum >= 300) {
                            //Log.d("DOTSUM","CONCAVE DETECTED");
                            numBig = numBig + 1;
                            if (dotSum > maxDotSum) {
                                maxDotSum = dotSum;
                            }
                        }
                        if (dotSum >= 140 && dotSum < 300) {
                            if (dotSum > nextLargest)
                                nextLargest = dotSum;
                        }
                        dotSum = 0;
                    }
                }
                int numSeeds = (int) (1 + numBig / 2);
                if (numSeeds == 1 && ((maxDotSum + nextLargest) >= 550)) {
                    numSeeds = 2;
                }
                if (numSeeds == seedCount.get(i)) {
                    //print
                } else {
                    if (seedCount.get(i) < numSeeds) {
                        seeds = seedCount.get(i);
                        seedCount.set(i, numSeeds);
                        double cx = cxCoord.get(i);
                        double cy = cyCoord.get(i);
                        if (seeds > 1) {
                            //put text
                        }
                        seeds = numSeeds;
                        //put text
                    }
                }
            }
        }
        ssum = 0;
        for (int k = 1; k < n; k++) {
            ssum = ssum + seedCount.get(k);
        }
        Log.d("SEED OUTPUT COUNT", String.valueOf(ssum));
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/finalFrame.jpg",frame);
        return frame;
    }

    private Scalar seedColor(Mat initialMat, Rect boundingRect){
        org.opencv.core.Rect seedRect = boundingRect;
        Scalar mBlobColorHsv;

        Mat touchedRegionRgba = initialMat.submat(seedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = seedRect.width * seedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, mBlobColorHsv);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public Mat getProcessedMat(){
        return processedMat;
    }

    public double getNumSeeds() {
        return this.ssum;
    }

    public ArrayList<Seed> getSeedArrayList() {
        return seedArrayList;
    }
}
