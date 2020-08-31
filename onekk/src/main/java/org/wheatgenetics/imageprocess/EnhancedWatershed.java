package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.os.Environment;
import androidx.core.util.Pair;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */

public class EnhancedWatershed {

    /**
     * Scalar colors require an alpha channel to be opaque
     * Fourth parameter is the alpha channel
     * Not required for Android version < 9
     */
    private final Scalar CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    private final Scalar RECTANGLE_COLOR = new Scalar(0, 255, 0, 255);
    private final Scalar TEXT_COLOR = new Scalar(0, 0, 0, 255);

    private DetectSeedsParams mParams;

    public static class Seed {

        private double area;
        private double perimeter;
        private double length;
        private double width;

        public Seed(double a, double p, double l, double w) {
            this.area = a;
            this.perimeter = p;
            this.length = l;
            this.width = w;
        }

        public double getWidth() {
            return width;
        }

        public double getLength() {
            return length;
        }

        public double getPerimeter() {
            return perimeter;
        }

        public double getArea() {
            return area;
        }
    }


    private double EuclideanDistance(MatOfPoint a, MatOfPoint b){
        Moments momentsA = Imgproc.moments(a);
        Moments momentsB = Imgproc.moments(b);


        if(momentsA.m00 > 0 && momentsB.m00 > 0){
            int X1 = (int)(momentsA.m10/ momentsA.m00);
            int Y1 = (int)(momentsA.m01/ momentsA.m00);

            int X2 = (int)(momentsB.m10/ momentsB.m00);
            int Y2 = (int)(momentsB.m01/ momentsB.m00);

            return Math.sqrt((X1 - X2)^2 + (Y1 - Y2)^2);
        }

        return 0.0;

    }

    private class ContourAreaPair {
        MatOfPoint contour;
        Double area;
        public ContourAreaPair(Double area, MatOfPoint contour) {
            this.contour = contour;
            this.area = area;
        }
    }

    //class used to define user-tunable parameters
    public static class DetectSeedsParams {

        private int areaHigh;

        public DetectSeedsParams(int areaHigh) {
            this.areaHigh = areaHigh;
        }
        int getAreaHigh() { return areaHigh; }
    }

    public EnhancedWatershed(DetectSeedsParams params) { mParams = params; }

    private double calcCircularity(double area, double peri) {

        return 4 * (Math.PI) * (area / (Math.pow(peri, 2)));
    }

    private <T extends Number> double calcStdDev(List<T> values) {

        double mean = 0;
        for (T x : values) mean += x.doubleValue();
        mean = mean / values.size();

        Log.d("CoinRecMean", String.valueOf(mean));

        double variance = 0;
        for (T x : values) {
            Log.d("CoinRecVal", String.valueOf(x));
            variance += Math.pow(x.doubleValue()-mean, 2);
        }

        variance /= values.size();

        Log.d("CoinRecVar", String.valueOf(variance));

        return Math.sqrt(variance);
    }

    private Pair<ArrayList<MatOfPoint>, ArrayList<MatOfPoint>> coinRecognition(List<MatOfPoint> contours) {

        ArrayList<Double> areas = new ArrayList<>();
        ArrayList<Double> circus = new ArrayList<>();

        for (MatOfPoint contour : contours) {

            double peri = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double area = Imgproc.contourArea(contour);

            areas.add(area);

            circus.add(calcCircularity(area, peri));
        }

        ArrayList<ContourAreaPair> contourMap = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if (circus.get(i) >= .8) {
                contourMap.add(new ContourAreaPair(areas.get(i), contours.get(i)));
            }
        }

        ContourAreaPair[] contourArray = new ContourAreaPair[contourMap.size()];
        for (int i = 0; i < contourMap.size(); i++) {
            contourArray[i] = contourMap.get(i);
        }



        Arrays.sort(contourArray, new Comparator<ContourAreaPair>() {

            @Override
            public int compare(ContourAreaPair a, ContourAreaPair b) {
                return a.area.compareTo(b.area);
            }
        });

        ArrayList<MatOfPoint> coins = new ArrayList<MatOfPoint>();
        ArrayList<MatOfPoint> notCoins = new ArrayList<MatOfPoint>();

        for(int i = 0; i < contourArray.length; i++){
            if(contourArray[i].area > 15000){
                coins.add(contourArray[i].contour);
            } else {
                notCoins.add(contourArray[i].contour);
            }

//        ArrayList<MatOfPoint> coins = new ArrayList<>();
//
//
//        for (int j=contourArray.length-1; j>contourArray.length/2; j--) {
//            boolean duplicate = false;
//            for(MatOfPoint b: coins){
//                if(EuclideanDistance(b, contourArray[j].contour) < 100)    {
//                    duplicate = true;
//                }
//            }
//            if(!duplicate){
//                coins.add(contourArray[j].contour);
//            }

        }

        return new Pair<>(notCoins, coins);

//        double areaStdDev = calcStdDev(areas);
//        double circuStdDev = calcStdDev(circus);
//
//        Log.d("CoinRec", String.valueOf(areaStdDev));
//        Log.d("CoinRec", String.valueOf(circuStdDev));
//
//        double areaThreshold = (1288.2439780084362 * 0.5);
//        double areaLow = 1288.2439780084362 - areaThreshold;
//        double areaHigh = 1288.2439780084362 + areaThreshold;
//
//        double circuThreshold = 0.002341670768322188 * 0.5;
//        double circuLow = 0.002341670768322188 - circuThreshold;
//        double circuHigh = 0.002341670768322188 + circuThreshold;
//
//        return new Pair<>(true, "");

//        if (areaStdDev >= areaLow && areaStdDev <= areaHigh) {
//
//            if (circuStdDev >= circuLow && circuStdDev <= circuHigh) {
//
//                return new CoinRecognitionOutput(true, "");
//            } else {
//
//                return new CoinRecognitionOutput(false, "Circularity variance threshold failed.");
//            }
//        }
//
//        return new CoinRecognitionOutput(false, "Area variance threshold failed.");
    }

    private double measureArea(double groundTruthAreaPixel, double groundTruthAreamm, double kernelAreaPx) {

        return (kernelAreaPx * groundTruthAreamm) / groundTruthAreaPixel;
    }

    private double findMax(Mat mat)
    {
        // Initializing max element as INT_MIN
        double maxElement = Integer.MIN_VALUE;

        // checking each element of matrix
        // if it is greater than maxElement,
        // update maxElement
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                if (mat.get(i, j)[0] > maxElement) {
                    maxElement = mat.get(i, j)[0];
                }
            }
        }

        // finally return maxElement
        return maxElement;
    }

    private Double[] toDoubleArray(double[] vals) {
        Double[] output = new Double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            output[i] = vals[i];
        }
        return output;
    }

    private <T extends Number> double Mean(T[] values){
        double sum = 0.0;
        for(int i = 0; i < values.length; i++){
            sum += values[i].doubleValue();
        }
        return (float)(sum/values.length);
    }

    private Pair<Double, Double> quartileRange(ArrayList<Double> values){
        int size = values.size();
        Double[] doubleValues = values.toArray(new Double[] {});

        Arrays.sort(doubleValues);

        double Q2 = doubleValues[doubleValues.length/2];

        Double[] lowerArray = Arrays.copyOfRange(doubleValues, 0, doubleValues.length/ 2);
        Double[] upperArray = Arrays.copyOfRange(doubleValues, doubleValues.length/ 2, doubleValues.length);

        double Q1 = lowerArray[lowerArray.length/2];
        double Q3 = upperArray[upperArray.length/2];

        double IQR = Q3 - Q1;

        Double firstValue = Q1 - 1.5 * IQR;
        Double secondValue = Q3 + 1.5 * IQR;

        return new Pair<>(firstValue, secondValue);

    }

    private ArrayList<Pair<Double, MatOfPoint>> InterquartileReduce(List<Pair<Double, MatOfPoint>> pairs){
        ArrayList<Double> areas = new ArrayList<>();
        for(Pair<Double, MatOfPoint> p : pairs){
            areas.add(p.first);
        }

        Pair<Double, Double> range = quartileRange(areas);

        ArrayList<Pair<Double, MatOfPoint>> output = new ArrayList<>();

        for(Pair<Double, MatOfPoint> p: pairs){
            if(p.first >= range.first && p.first <= range.second){
                output.add(p);
            }
        }
        return output;
    }

    private GroundTruths findGroundTruths(Mat img){

        long start = System.currentTimeMillis();

        Mat gtImg = img.clone();

        Mat copy = img.clone();

        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(img, img, new Size(3, 3), 5);
        Imgproc.threshold(img, img, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat mask = Mat.zeros(img.size(), CvType.CV_8U);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();


        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfInt hull = new MatOfInt();
        for(int i = 0; i < contours.size(); i++){
            Imgproc.convexHull(contours.get(i), hull);

            //ToDo: Verify that the rows()  is what needs to be used.
            if(contours.get(i).rows()/ hull.rows() <= 3){
                Imgproc.drawContours(mask, contours, i, new Scalar(128, 128, 128, 255), -1);
            }

        }

        Imgproc.medianBlur(mask, mask, 9);

        Imgproc.Canny(mask, mask, 200, 255);

        writeFile("canny", mask);

        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        Pair<ArrayList<MatOfPoint>, ArrayList<MatOfPoint>> pair = coinRecognition(contours);
        //Imgproc.drawContours(gtImg, coins, -1, new Scalar(255,0,0), -1);

//        for(int i = 0; i < coins.size(); i++){
//            Imgproc.drawContours(gtImg, coins.get(i), -1, new Scalar(255,0,0), -1);
//        }



        //writeFile("coins", gtImg);

        contours = pair.first;

        List<Pair<Double, MatOfPoint>> areas = new ArrayList();

        for(int i = 0; i < contours.size(); i++){
            double area = Imgproc.contourArea(contours.get(i));

             Moments moments = Imgproc.moments(contours.get(i));

             if(moments.m00 > 0){
                 int cX = (int)(moments.m10/ moments.m00);
                 int cY = (int)(moments.m01/ moments.m00);

                 double avgVal = Mean(toDoubleArray(copy.get(cY, cX)));
                 if(avgVal < 170){
                     areas.add(new Pair<Double, MatOfPoint>(area, contours.get(i)));
                 }
             }
        }

        areas = InterquartileReduce(areas);

        List<MatOfPoint> groundTruths = new ArrayList<>();

        for(Pair<Double, MatOfPoint> area : areas){
            groundTruths.add(area.second);
        }

//
        //Imgproc.drawContours(gtImg, groundTruths, -1, new Scalar(0, 255, 0, 0), -1);

        List<Integer> defectVals = new ArrayList<>();


        for(MatOfPoint contour : groundTruths){
            Moments M = Imgproc.moments(contour);
            int cX = (int)(M.m10/ M.m00);
            int cY = (int)(M.m01/ M.m00);

            //ToDo: Check with Chaney
            Imgproc.convexHull(contour, hull);

            if(hull.rows() > 0){
                MatOfInt4 defects = new MatOfInt4();
                Imgproc.convexityDefects(contour, hull, defects);

                for(int i = 0; i < defects.rows(); i++){
                   defectVals.add((int)defects.get(i,0)[3]);
                }

            }

        }

        Integer[] sortedDefectVals = defectVals.toArray(new Integer[] {});
        Arrays.sort(sortedDefectVals);
        Integer defectMean = sortedDefectVals[sortedDefectVals.length/2];



        Log.d("GTTIME", String.valueOf(System.currentTimeMillis()-start));
        return new GroundTruths(groundTruths, calcStdDev(defectVals), defectMean, pair.second);

    }

    private class GroundTruths {

        List<MatOfPoint> contours;
        double stdev;
        Integer defectMean;
        ArrayList<MatOfPoint> coins;

        GroundTruths(List<MatOfPoint> contours, double stddev, Integer defectMean, ArrayList<MatOfPoint> coins) {
            this.contours = contours;
            this.stdev = stddev;
            this.defectMean = defectMean;
            this.coins = coins;
        }

    }

    private void logMat(Mat m) {

        for (int r = 0; r<m.rows(); r++) {
            for (int c = 0; c<m.cols(); c++) {
                Log.d("PIXEL", String.valueOf(m.get(r,c)[0]));
            }
        }
    }

    private Mat matSwap(Mat src, Integer value, Integer newVal, boolean invert, Mat mask) {

        long toc = System.currentTimeMillis();

        src.convertTo(src, CvType.CV_8U);

        Mat dst = new Mat(src.size(), CvType.CV_8U);

        Mat val = new Mat(src.size(), CvType.CV_8U, new Scalar(value));
        //mask = mask.setTo(new Scalar(value));

        //logMat(mask);

        int cmp = Core.CMP_EQ;
        if (invert) cmp = Core.CMP_NE;

        if (mask == null) {
            Core.compare(src, val, dst, cmp);
        } else {
            Core.compare(mask, val, dst, cmp);
        }

        //logMat(dst);

        src = src.setTo(new Scalar(newVal), dst);


        long tic = System.currentTimeMillis();
        Log.d("TIME", String.valueOf(tic-toc));
        return src;
    }

    private void writeFile(String name, Mat img) {
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/"+name+".jpg", img);
    }

    private Pair<Mat, ArrayList<Seed>> process(Mat src) {

        long start = System.currentTimeMillis();

        GroundTruths gts = findGroundTruths(src.clone());

        Mat gray = src.clone();

        Double[] areas = new Double[gts.contours.size()];

        for (int i = 0; i < gts.contours.size(); i++) {
            areas[i] = Imgproc.contourArea(gts.contours.get(i));
        }

        double gtAvgArea = Mean(areas);

        Mat kernel = new Mat(3, 3, CvType.CV_8U);

        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_BGR2GRAY);

        Mat grayPolygonTest = gray.clone();


        Imgproc.threshold(gray, gray, 0, 255, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);

        writeFile("thresh", gray);

        Imgproc.erode(gray, gray, kernel, new Point(-1,-1), 4);

        Mat sure_bg = new Mat();

        Imgproc.dilate(gray, sure_bg, kernel, new Point(-1,-1), 5);

        writeFile("sure_bg", sure_bg);

        Imgproc.distanceTransform(gray, gray, Imgproc.DIST_L2, Imgproc.DIST_MASK_5);

        writeFile("dt", gray);

        Core.normalize(gray, gray, 0, 1.0, Core.NORM_MINMAX);

        Mat sure_fg = new Mat(gray.size(), gray.type());
        Imgproc.threshold(gray, sure_fg, 0.2, 255, Imgproc.THRESH_BINARY);

        sure_fg.convertTo(sure_fg, CvType.CV_8U);

        writeFile("sure_fg", sure_fg);

        Mat unknown = new Mat();
        Core.subtract(sure_bg, sure_fg, unknown);

        writeFile("unknown", unknown);

        Mat markers = new Mat();
        Imgproc.connectedComponents(sure_fg, markers, 8, CvType.CV_32S);

       // Core.multiply(markers, new Scalar(100), markers);

        writeFile("cc", markers);

        Core.add(markers, new Scalar(1), markers);

        markers = matSwap(markers, 255, 0, false, unknown);

        writeFile("ccswap", markers);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(src, blurred, new Size(3,3), 5);

        markers.convertTo(markers, CvType.CV_32SC1);

        blurred.convertTo(src, CvType.CV_8UC3);

        Imgproc.cvtColor(blurred, blurred, Imgproc.COLOR_RGBA2RGB);

        Imgproc.watershed(blurred, markers);

        writeFile("markers", markers);

        markers = matSwap(markers, -1, 0, false, null);

        writeFile("markersA", markers);

        markers.convertTo(markers, CvType.CV_8U);

        Mat whiteMat = new Mat(markers.size(), markers.type(), new Scalar(255));

        Core.subtract(whiteMat, markers, markers);

        writeFile("markersB", markers);

        markers = matSwap(markers, 255, 0, true, null);

        writeFile("markersC", markers);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(markers, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        int floorCount = 0;
        int roundCount = 0;
        int areaFloorCount = 0;
        int areaRoundCount = 0;

        ArrayList<MatOfPoint> measuredContours = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            ArrayList<MatOfPoint> children = new ArrayList<>();

            double area = Imgproc.contourArea(contour);

            if (area < 500000) {

                for (int j = 0; j < hierarchy.cols(); j++) {

                    Mat hier = hierarchy.col(j);

                    int parentIndex = (int) hier.get(0, 0)[3];

                    if (parentIndex == i) {

                        MatOfPoint child = contours.get(j);

                        double childArea = Imgproc.contourArea(child);

                        if (childArea > gtAvgArea * .25) {

                            boolean within = false;

                            for (MatOfPoint coin : gts.coins) {

                                if (!within) {
                                    RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(child.toArray()));

                                    int minX = (int) (rect.center.x - rect.size.width / 2);
                                    int minY = (int) (rect.center.y - rect.size.height / 2);
                                    int maxX = (int) (rect.center.x + rect.size.width / 2);
                                    int maxY = (int) (rect.center.y + rect.size.height / 2);

                                    for (int x = minX; x < maxX; x += 4) {
                                        if (!within) {
                                            for (int y = minY; y < maxY; y += 4) {
                                                if (Imgproc.pointPolygonTest(new MatOfPoint2f(coin.toArray()), new Point(x, y), false) > 0) {
                                                    within = true;
                                                    break;
                                                }
                                            }
                                        } else break;
                                    }
                                }
                            }

                            if (!within) measuredContours.add(child);
                        }
                    }
                }
            }
        }

//        Double[] childAreas = new Double[measuredContours.size()];
//        for (int i = 0; i < measuredContours.size(); i++) {
//            childAreas[i] = Imgproc.contourArea(measuredContours.get(i));
//        }
//        Mat grayPolygonTest = gray.clone();

//        double childSum = Mean(childAreas);

        Random rand = new Random();



        for (int i = 0; i < measuredContours.size(); i++) {

            MatOfPoint contour = measuredContours.get(i);

            double area = Imgproc.contourArea(contour);

            if (area < 500000) {

                RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

                MatOfInt hull = new MatOfInt();

                Imgproc.convexHull(contour, hull);

                double areaPercent = area/gtAvgArea;

                int minX = (int) (rect.center.x - rect.size.width/2);
                int minY = (int) (rect.center.y - rect.size.height/2);
                int maxX = (int) (rect.center.x + rect.size.width/2);
                int maxY = (int) (rect.center.y + rect.size.height/2);

                ArrayList<Double> pixels = new ArrayList<>();

                for (int x = minX; x < maxX; x += 4) {
                    for (int y = minY; y < maxY; y += 4) {
                        if (Imgproc.pointPolygonTest(new MatOfPoint2f(contour.toArray()), new Point(x,y), false) > 0) {
                            pixels.add(grayPolygonTest.get(y, x)[0]);
                        }
                    }
                }

                if (pixels.size() > 0 && Mean(pixels.toArray(new Double[]{})) < 100) {

                    if (hull.rows() > 0) {

                        ArrayList<Double> defVals = new ArrayList<>();
                        MatOfInt4 defects = new MatOfInt4();
                        Imgproc.convexityDefects(contour, hull, defects);

                        for (int d=0; d < defects.rows(); d++) {

                            defVals.add(defects.get(d, 0)[3]);
                        }

                        if (Mean(defVals.toArray(new Double[]{})) > gts.defectMean+4*gts.stdev) {

                            roundCount += Math.round(areaPercent);
                        } else {
                            roundCount += 1;
                        }
                    }

                    areaRoundCount += Math.round(areaPercent);

                    floorCount += 1;



                    Imgproc.drawContours(src, measuredContours, i, new Scalar(rand.nextInt(255),rand.nextInt(255), 255,255), -1);
                }
            }
        }

        int maxCount = Math.max(floorCount, roundCount);
        int minCount = Math.min(floorCount, roundCount);

        Log.d("Count: ", minCount + "-" + maxCount);

        maxCount = Math.max(floorCount, areaRoundCount);
        minCount = Math.min(floorCount, areaRoundCount);

        Log.d("Area based Count: ", minCount + "-" + maxCount);

        Log.d("TIME: ", String.valueOf(System.currentTimeMillis()-start));
        return new Pair<>(src, new ArrayList<Seed>());


//        Mat threshed = src.clone();
//
//        Imgproc.cvtColor(threshed, threshed, Imgproc.COLOR_RGB2GRAY);
//
//        Imgproc.threshold(threshed, threshed, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/threshed.jpg", threshed);
//
//        Mat kernel = new Mat(3, 3, CvType.CV_8U);
//
//        Mat opening = new Mat();
//
//        Imgproc.morphologyEx(threshed, opening, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 3);
//
//        Mat sure_bg = new Mat();
//
//        Imgproc.dilate(opening, sure_bg, kernel, new Point(-1, -1), 3);
//
//        Mat dist_transform = new Mat();
//
//        Imgproc.distanceTransform(sure_bg, dist_transform, Imgproc.DIST_L2, 5);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/dt.jpg", dist_transform);
//
//        double max_val = findMax(dist_transform);
//
//        Mat sure_fg = new Mat();
//
//        Imgproc.threshold(dist_transform, sure_fg, 0.2 * max_val, 255, 0);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/sure_fg.jpg", sure_fg);
//
//        sure_fg.convertTo(sure_fg, CvType.CV_8U);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/sure_fg8u.jpg", sure_fg);
//
//        Mat unknown = new Mat();
//
//        Core.subtract(sure_bg, sure_fg, unknown);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/unknown.jpg", unknown);
//
//        Mat markers = new Mat();
//
//        Imgproc.connectedComponents(sure_fg, markers);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/markers.jpg", markers);
//
//        Core.add(markers, Mat.ones(markers.size(), markers.type()), markers);
//
////        for (int i = 0; i < markers.rows(); i++) {
////            for (int j = 0; j < markers.cols(); j++) {
////                double[] value = unknown.get(i, j);
////                if (unknown.get(i, j)[0] == 255) {
////                    markers.put(i, j, 0);
////                }
////            }
////        }
//
//        markers.convertTo(markers, CvType.CV_32SC1);
//
//        src.convertTo(src, CvType.CV_8UC3);
//
//        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2RGB);
//
//        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/markersA.jpg", markers);
//
//
//        Imgproc.watershed(src, markers);
//
//        markers.convertTo(markers, CvType.CV_8U);
//
//        Mat markers2 = new Mat();
//
//        Imgproc.adaptiveThreshold(markers, markers2, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 1);
//
//        List<MatOfPoint> contours = new ArrayList<>();
//
//        Mat hierarchy = new Mat();
//
//        Imgproc.findContours(markers2, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        int count = 0;
//
//        MatOfInt hull = new MatOfInt();
//
//        ArrayList<Seed> seedMetrics = new ArrayList<Seed>();
//
//
//        for (int i = 0; i < contours.size(); i++) {
//
//            if (hierarchy.get(0, i)[2] == -1) {
//                //Imgproc.convexHull(contours.get(i), hull);
        //        MatOfPoint2f approxCurve = new MatOfPoint2f();

//                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxCurve, 0.01, true);
//                if (approxCurve.height() * approxCurve.width() > 25) {
//                    count += 1;
//
//                    Rect rect = Imgproc.boundingRect(contours.get(i));
//
//                    Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), RECTANGLE_COLOR, 2);
//
//                    Imgproc.putText(src, String.valueOf(count), new Point(rect.x, rect.y), Imgproc.FONT_HERSHEY_SIMPLEX, 4, TEXT_COLOR, 3);
//
//                    double area = Imgproc.contourArea(contours.get(i));
//
//                    double perimeter = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true);
//
//                    seedMetrics.add(new Seed(area, perimeter, rect.height, rect.width));
//                }
//            }
//        }
//
//
//        return new Pair<>(src, seedMetrics);


    }

    public Pair<Bitmap, ArrayList<Seed>> process(Bitmap inputBitmap) {

        Mat frame = new Mat();

        Utils.bitmapToMat(inputBitmap, frame);

        Pair<Mat, ArrayList<Seed>> output = process(frame);

        Utils.matToBitmap(output.first, inputBitmap);

        return new Pair<>(inputBitmap, output.second);
    }
}
