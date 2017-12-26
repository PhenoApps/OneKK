package org.wheatgenetics.imageprocess.watershedLB;

import android.graphics.Bitmap;
import android.graphics.Region;
import android.util.Log;
import android.util.Pair;

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
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opencv.imgproc.Imgproc.CC_STAT_AREA;
import static org.opencv.imgproc.Imgproc.CC_STAT_LEFT;
import static org.opencv.imgproc.Imgproc.CC_STAT_TOP;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by chaneylc on 8/22/2017.
 */

public class WatershedLB {

    //opencv constants
    private final Scalar mContourColor = new Scalar(255, 0, 0);
    private final Scalar mContourMomentsColor = new Scalar(0, 0, 0);

    //seed counter algorithm variables
    private int mNumSeeds = 0;
    double ssum = 0;
    //seed counter parameters
    private WatershedParams mParams;

    //class used to define user-tunable parameters
    public static class WatershedParams {
        private int areaLow, areaHigh, defaultRate;
        private double sizeLowerBoundRatio, newSeedDistRatio;

        public WatershedParams(int areaLow, int areaHigh, int defaultRate,
                        double sizeLowerBoundRatio, double newSeedDistRatio) {
            this.areaLow = areaLow;
            this.areaHigh = areaHigh;
            this.defaultRate = defaultRate;
            this.sizeLowerBoundRatio = sizeLowerBoundRatio;
            this.newSeedDistRatio = newSeedDistRatio;
        }
        int getAreaLow() { return areaLow; }
        int getAreaHigh() { return areaHigh; }
        int getDefaultRate() { return defaultRate; }
        double getSizeLowerBoundRatio() { return sizeLowerBoundRatio; }
        double getNewSeedDistRatio() { return newSeedDistRatio; }
    }

    public WatershedLB(WatershedParams params) {
        mParams = params;
    }

    public Pair<Mat, String> process(Mat frame) {

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

        final List<Mat> images = new ArrayList<>();
        images.add(gray);

        Mat hist = new Mat();
        //images
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
        Imgproc.threshold(dt, sure_fg, 12, 255, 0);

        Mat unknown = new Mat();
        Core.subtract(sure_bg, sure_fg, unknown, new Mat(), CvType.CV_8UC1);
        //////Mat invertMat = Mat.ones(sure_fg.size(), sure_fg.type()).setTo(new Scalar(255));

       // Core.subtract(invertMat, sure_fg, unknown);
// unknown = 255 - sure_fg

        int connectivity = 8;
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        sure_fg.convertTo(sure_fg, CvType.CV_8UC1);

        int numObjects = Imgproc.connectedComponentsWithStats(sure_fg, labels, stats, centroids, connectivity, CvType.CV_32S);
        for (int k = 1; k < numObjects; k++) {
            final double area = stats.get(k, CC_STAT_AREA)[0]; // ConnectedComponentsTyps CC_STAT_AREA = 4
            final double cx = centroids.get(k, CC_STAT_LEFT)[0];
            final double cy = centroids.get(k, CC_STAT_TOP)[0];
            if (area < 20) {// && labels.get(n, m)[0] == k) {
                //markers[unknown==255] = 0
                //markers = markers[i] - 1
                //unknown[markers == i] = 255 # by setting to unknown, it will get zeroed out below
                //unknown[markers == i] = 0
                Mat mask = new Mat(labels.size(), labels.type());
                Scalar labelId = new Scalar(k, k, k);
                Core.inRange(labels, labelId, labelId, mask);
                unknown.copyTo(unknown, mask);
                mask.release();

               // Mat invMask = new Mat();
               // Core.invert(mask, invMask);
               // unknown.put(n, m, 255);
                //labels.put(n, m, 0);
            } else
                Imgproc.circle(frame, new Point(cx, cy), 10, new Scalar(64, 32, 255), -1);
        }

        //markers = markers + 1
        Core.add(labels, Mat.ones(labels.size(), labels.type()), labels);

        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2BGR);
        labels.convertTo(labels, CvType.CV_32S);
        Imgproc.watershed(gray, labels);

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

        Log.d("Unique labels", String.valueOf(unique.size()));
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
                    if (area < 200 || area > 20000) i = i - 1;
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
                        Imgproc.circle(frame, new Point(cx, cy), 5, new Scalar(0,0,255), -1);
                    }
                }

            }
            mask.release();
            hierarchy.release();
        }

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
        return new Pair<>(frame, String.valueOf(ssum));

    }

    public Pair<Bitmap, String> process(Bitmap inputBitmap) {

        Mat frame = new Mat();
        Utils.bitmapToMat(inputBitmap, frame);
        Pair<Mat, String> ret = process(frame);
        Utils.matToBitmap(ret.first, inputBitmap);
        return new Pair<>(inputBitmap, ret.second);
    }

    public double getNumSeeds() {
        return this.ssum;
    }
}
