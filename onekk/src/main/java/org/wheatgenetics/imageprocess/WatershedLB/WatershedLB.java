package org.wheatgenetics.imageprocess.WatershedLB;

import android.graphics.Bitmap;
import android.os.Environment;
import android.renderscript.RenderScript;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.wheatgenetics.imageprocess.Seed.Seed;
import org.wheatgenetics.onekk.MainActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opencv.imgproc.Imgproc.CC_STAT_AREA;
import static org.opencv.imgproc.Imgproc.CC_STAT_LEFT;
import static org.opencv.imgproc.Imgproc.CC_STAT_TOP;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2Lab;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_Lab2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_Lab2RGB;

/**
 * Created by chaneylc on 8/22/2017.
 */

public class WatershedLB {

    private Mat processedMat;
    private ArrayList<Seed> seedArrayList = null;
    private List<MatOfPoint> seedContours = null;
    private double ssum = 0;

    /**
     * This class consists of variables and methods used to setup Watershed light box parameters
     *
     */
    public static class WatershedParams {
        protected int areaLow, areaHigh, defaultRate;
        protected double sizeLowerBoundRatio, newSeedDistRatio;

        /**
         * Constructor to initialize the Watershed parameters before processing
         * <p>
         *  This is a convenience for calling
         * {@link org.wheatgenetics.imageprocess.WatershedLB.WatershedLB.WatershedParams#WatershedParams(int, int, int, double, double)}.
         * </p>
         *
         *  @param areaLow minimum area value of the seed
         *  @param areaHigh maximum hue value of the seed
         *  @param defaultRate default rate at which the seeds are flowing
         *  @param sizeLowerBoundRatio
         *  @param newSeedDistRatio
         */

        public WatershedParams(int areaLow, int areaHigh, int defaultRate,
                               double sizeLowerBoundRatio, double newSeedDistRatio) {
            this.areaLow = areaLow;
            this.areaHigh = areaHigh;
            this.defaultRate = defaultRate;
            this.sizeLowerBoundRatio = sizeLowerBoundRatio;
            this.newSeedDistRatio = newSeedDistRatio;
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
        WatershedParams mParams = params;
    }

    public WatershedLB() {
    }

    public Bitmap process(Bitmap inputBitmap) {
        seedArrayList = new ArrayList<>();
        seedContours = new ArrayList<>();

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

        /*
         * Automatic thresholding as in ImageJ's Binary + Make Binary
         * Threshold value (thresh) is computed iteratively so that the
         * threshold is less than the average of the low average (pixels below the threshold)
         * and the high average (pixels at or above the threshold).
         * Generally converges in just a few iterations.
         */

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

        /* Convert to grayscale */
        Mat binMat = new Mat();
        Imgproc.threshold(gray, binMat, thresh, 255, Imgproc.THRESH_BINARY_INV);

        Mat opening = new Mat();
        Imgproc.morphologyEx(binMat, opening, Imgproc.MORPH_OPEN, Mat.ones(new Size(3,3), CvType.CV_8UC1));
        Mat sure_bg = new Mat();
        Imgproc.dilate(opening, sure_bg, Mat.ones(new Size(3,3), CvType.CV_8UC1));

        /* Perform distance transform to compute EDM = Euclidean Distance Map */
        Mat dt = new Mat();
        Imgproc.distanceTransform(opening, dt, 2, 3);

        /* Test with lower bound on distance threshold set to 12 to compute sure_fg
         * Should be computed based on dt.max() without considering the corner circles
         * Use the sure foreground (sure_fg) components as markers
         */
        Mat sure_fg = new Mat();
        Imgproc.threshold(dt, sure_fg, 8, 255, 0);

        Mat unknown = new Mat();
        Core.subtract(sure_bg, sure_fg, unknown, new Mat(), CvType.CV_8UC1);

        /* Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/binMat.jpg",binMat);
         * Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/dt.jpg",dt);
         * Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/surefg.jpg",sure_fg);
         * Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/surebg.jpg",sure_bg);
         * Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/unknown.jpg",unknown);
         */

        /* Select 4 or 8 for connectivity type
         * generally the same for 4 or 8
         */
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

        /* Consider all except for background which is marker 0 */
        for (int k = 1; k < numObjects; k++) {
            final double area = stats.get(k, CC_STAT_AREA)[0]; // ConnectedComponentsTyps CC_STAT_AREA = 4
            final double cx = centroids.get(k, CC_STAT_LEFT)[0];
            final double cy = centroids.get(k, CC_STAT_TOP)[0];

            /* Remove markers with small size */
            if (area < 20) {
                Mat mask = new Mat(labels.size(), labels.type());
                Scalar labelId = new Scalar(k, k, k);
                Core.inRange(labels, labelId, labelId, mask);
                unknown.copyTo(unknown, mask);
                mask.release();
            } else {
                areaList.add(area);
                sumArea = sumArea + area;
                //Imgproc.circle(frame, new Point(cx, cy), 20, new Scalar(32, 64, 255), -1);
                //Imgproc.getTextSize(String.valueOf(k), Core.FONT_HERSHEY_COMPLEX,0.5,1,textSize);
                //Imgproc.putText(frame,String.valueOf(k),new Point(cx+(textSize[0]*3),cy+(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(32,64,255),2);
            }
        }

        meanArea = sumArea / areaList.size();
        for (Double are : areaList) {
            variance = variance + Math.pow((are - meanArea), 2);
        }

        maxAreaThreshold = meanArea + Math.pow(variance, 2);
        minAreaThreshold = meanArea - Math.pow(variance,2);

        Core.add(labels, Mat.ones(labels.size(), labels.type()), labels);

        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2BGR);
        labels.convertTo(labels, CvType.CV_32S);
        Imgproc.watershed(gray, labels);

        /* Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/labels.jpg",labels); */

        Mat borderMask = new Mat(labels.size(), labels.type());
        Scalar labelId = new Scalar(-1,-1,-1);
        Core.inRange(labels, labelId, labelId, borderMask);
        frame.copyTo(frame, borderMask);
        borderMask.release();

        int i = 0;

        final ArrayList<Double> areas = new ArrayList<>();
        final ArrayList<Double> perimeters = new ArrayList<>();
        final ArrayList<Integer> seedCount = new ArrayList<>();
        final ArrayList<Double> cxCoord = new ArrayList<>();
        final ArrayList<Double> cyCoord = new ArrayList<>();
        final ArrayList<Point[]> cpoints = new ArrayList<>();

        /* Create unique labels set */
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

        /* loop over the unique labels returned by the watershed algorithm */
        for (Double label : unique) {
            /* if the label is zero, it is the 'background', so ignore it */
            if (label < 1){
                continue;
            }

            /* otherwise, allocate memory for the label region and draw it on the mask */
            Mat mask = Mat.zeros(labels.size(), CvType.CV_8U);
            int l = (int) label.doubleValue() + 1;
            Scalar colorLabel = new Scalar(l,l,l);
            Core.inRange(labels, colorLabel, colorLabel, mask);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();

            /* detect contours in the mask and grab the largest one */
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

                        /* if we treat it as a seed, then draw center of contour in black */
                        areas.add(area);
                        perimeters.add(perimeter);
                        seedCount.add(1);
                        cpoints.add(contourPoints);
                        ssum = ssum + area;

                        /* get the moments to compute centroid */
                        final Moments M = Imgproc.moments(contour);
                        final double cx = (int) (M.get_m10() / M.get_m00());
                        cxCoord.add(cx);
                        final double cy = (int) (M.get_m01() / M.get_m00());
                        cyCoord.add(cy);

                        /* draw the contours on the mat */
                        Imgproc.drawContours(frame,contours,-1, new Scalar(0,255,0),3);

                        /* draw a circle to mark the center of the seed */
                        //Imgproc.circle(frame, new Point(cx, cy), 15, new Scalar(0,255,255), -1);

                        /* get the text size to write values on the mat */
                        Imgproc.getTextSize(String.valueOf(i), Core.FONT_HERSHEY_COMPLEX,0.5,1,textSize);

                        /* put a number on each seed, along with the length and width */
                        Imgproc.putText(frame,String.valueOf(i),new Point(cx-(textSize[0]*3),cy-(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,new Scalar(0,255,255),2);

                        /* create a seed object for each seed and store them in an ArrayList */
                        seedArrayList.add(new Seed(cx,cy, area, perimeter, null,0,null, matOfPoint));

                        seedContours.add(matOfPoint);
                    }
                }

            }
            mask.release();
            hierarchy.release();
        }

        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/uniqueFrame.jpg",frame);

        /* Estimate number of seeds based only on size of contours - iterate to get better estimate
         * for average size of one seed
         */

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

                Imgproc.getTextSize(" +" + String.valueOf(seeds-1), Core.FONT_HERSHEY_COMPLEX,
                        0.5,1,textSize);

                Imgproc.putText(frame, " +" + String.valueOf(seeds-1), new Point(cx-(textSize[0]*3),
                        cy-(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,
                        new Scalar(0,255,255),2);
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

        Log.d("WatershedLB","Adjusted estimated area for one seed: " + String.valueOf(est_size));
        Log.d("WatershedLB","Seed count with adjustment: " + String.valueOf(count));

        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/adjustedFrame.jpg",frame);

        /* Apply our algorithm for extending watershed segmentation, but just compute the result
         * without re-running watershed segmentation. Check if we need to segment seeds with size
         * greater than estimated average size and perimeter at least 1.5 * estimated perimeter
         */

        int delta = 4;
        for (i = 1; i < n; i++) {
            int numBig = 0;
            double dotSum = 0;
            double area = areas.get(i);
            double perimeter = perimeters.get(i);
            double startPt = -1;
            double endPt   = -1;
            List<Double> xCoords = new ArrayList<>();
            List<Double> yCoords = new ArrayList<>();
            List<Double> dotProds = new ArrayList<>();

            if (area > est_size && perimeter > est_perimeter * 1.5) {
                Log.d("WatershedLB","Segmenting Seed: " + String.valueOf(i) + " with area "
                + String.valueOf(area) + " and perimeter " + String.valueOf(perimeter));
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
                nPoints = (nPoints / 2);
                for (int j = 0; j < nPoints; j++) {
                    dotProds.set(j, ((xCoords.get(j) - xCoords.get((j+nPoints-delta) % nPoints)) * (yCoords.get((j+delta) % nPoints)
                            - ((yCoords.get(j) - yCoords.get((j+nPoints-delta) % nPoints)) * (xCoords.get((j+delta) % nPoints) - xCoords.get(j))))));

                    /* Compute absolute value of negative dot products and sum */
                    if (dotProds.get(j) > 0) {
                        dotSum = dotSum + dotProds.get(j);
                        if(startPt < 0)
                            startPt = j;
                    } else {
                        if (dotSum >= 200) {
                            endPt = j - 1;
                            numBig = numBig + 1;
                            if (dotSum > maxDotSum) {
                                maxDotSum = dotSum;
                            }
                            if (endPt > startPt) {
                                Log.d("WatershedLB","StartPt: " + String.valueOf(startPt) + ", EndPt: " + String.valueOf(endPt));

                                int midPt = (int)(startPt + endPt) / 2;
                                endPt = -1;   //MLN NEW
                                Log.d("WatershedLB","Big dot sum: " + String.valueOf(dotSum) +
                                        " numBig: " + String.valueOf(numBig) + " at " +
                                        String.valueOf(xCoords.get(midPt)) + ", " + String.valueOf(yCoords.get(midPt)));
                            }
                        }
                        if (dotSum >= 140 && dotSum < 200) {
                            if (dotSum > nextLargest)
                                nextLargest = dotSum;
                        }
                        startPt = -1; //MLN NEW
                        dotSum = 0;
                    }
                }
                int numSeeds = (1 + numBig / 2);

                /* Double check small seeds with relatively large dot products */
                if (numSeeds == 1 && ((maxDotSum + nextLargest) >= 550)) {
                    numSeeds = 2;
                }

                if (numSeeds == seedCount.get(i)) {
                    Log.d("WatershedLB","Segmentation count agrees with area count: " + String.valueOf(seedCount.get(i)));
                } else {
                    Log.d("WatershedLB","Segmentation count does not agree with area count: " + String.valueOf(seedCount.get(i)));
                    if (seedCount.get(i) < numSeeds) {
                        seeds = seedCount.get(i);
                        seedCount.set(i, numSeeds);
                        double cx = cxCoord.get(i);
                        double cy = cyCoord.get(i);

                        /* Erase the old count by setting to black */
                        if (seeds > 1) {
                            Imgproc.getTextSize(" +" + String.valueOf(seeds-1), Core.FONT_HERSHEY_COMPLEX,
                                    0.5,1,textSize);

                            Imgproc.putText(frame, "+" + String.valueOf(seeds-1), new Point(cx-(textSize[0]*3),
                                            cy-(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,
                                    new Scalar(0,0,0),2);
                        }

                        seeds = numSeeds;

                        /* Display the new count in white */
                        Imgproc.getTextSize(" +" + String.valueOf(seeds-1), Core.FONT_HERSHEY_COMPLEX,
                                0.5,1,textSize);

                        Imgproc.putText(frame, " +" + String.valueOf(seeds-1), new Point(cx-(textSize[0]*3),
                                        cy-(textSize[0]*3)),Core.FONT_HERSHEY_COMPLEX,1.0,
                                new Scalar(255,255,255),2);
                    }
                }
            }
        }
        ssum = 0;
        for (int k = 0; k < n; k++) {
            ssum = ssum + seedCount.get(k);
        }
        Log.d("SEED OUTPUT COUNT", String.valueOf(ssum));
        //Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/finalFrame.jpg",frame);
        return frame;
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

    public List<MatOfPoint> getSeedContours() {
        return seedContours;
    }
}
