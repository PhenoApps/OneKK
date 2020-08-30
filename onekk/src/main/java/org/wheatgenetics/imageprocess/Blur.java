package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.core.util.Pair;

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

public class Blur {

    /**
     * Scalar colors require an alpha channel to be opaque
     * Fourth parameter is the alpha channel
     * Not required for Android version < 9
     */
    private final Scalar CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    private final Scalar RECTANGLE_COLOR = new Scalar(0, 255, 0, 255);
    private final Scalar TEXT_COLOR = new Scalar(0, 0, 0, 255);

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

    public Blur() { }

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

        if (size > 0) {

            Double[] doubleValues = values.toArray(new Double[]{});

            Arrays.sort(doubleValues);

            double Q2 = doubleValues[doubleValues.length / 2];

            Double[] lowerArray = Arrays.copyOfRange(doubleValues, 0, doubleValues.length / 2);
            Double[] upperArray = Arrays.copyOfRange(doubleValues, doubleValues.length / 2, doubleValues.length);

            if (lowerArray.length > 0 && upperArray.length > 0) {

                double Q1 = lowerArray[lowerArray.length / 2];
                double Q3 = upperArray[upperArray.length / 2];

                double IQR = Q3 - Q1;

                Double firstValue = Q1 - 1.5 * IQR;
                Double secondValue = Q3 + 1.5 * IQR;

                return new Pair<>(firstValue, secondValue);

            }

            return new Pair<>(doubleValues[0], doubleValues[doubleValues.length - 1]);
        } else {

            return new Pair<Double, Double>(0.0, 0.0);

        }
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

        //Mat gtImg = img.clone();

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

        //writeFile("canny", mask);

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

        Integer defectMean = 0;
        if (sortedDefectVals.length > 0) {
            defectMean = sortedDefectVals[sortedDefectVals.length / 2];
        }


        //Log.d("GTTIME", String.valueOf(System.currentTimeMillis()-start));
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

    private Mat process(Mat src) {

        long start = System.currentTimeMillis();

        //Mat copy = src.clone();

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.GaussianBlur(copy, copy, new Size(3, 3), 5);

        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35, 8);
        //Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
       // Imgproc.threshold(src, src, 0, 128, Imgproc.THRESH_BINARY_INV);


//        Mat mask = Mat.zeros(copy.size(), CvType.CV_8U);
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//
//
//        Imgproc.findContours(copy, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        MatOfInt hull = new MatOfInt();
//        for(int i = 0; i < contours.size(); i++){
//
//            Imgproc.convexHull(contours.get(i), hull);
//
//            //ToDo: Verify that the rows()  is what needs to be used.
//            if(contours.get(i).rows()/ hull.rows() <= 3){
//                Imgproc.drawContours(src, contours, i, new Scalar(128, 128, 128, 255), 1);
//            }
//
//        }

        //Imgproc.medianBlur(mask, mask, 9);

        //Imgproc.Canny(mask, mask, 200, 255);

        //writeFile("canny", mask);

//        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        //Imgproc.cvtColor(gray, gray, Imgproc.COLOR_BGR2GRAY);
//
//        for (int i = 0; i < contours.size(); i++) {
//
//            Imgproc.drawContours(src, contours, i, new Scalar(255, 255, 255), -1);
//
//        }
////
//        return gray;

        //Imgproc.cvtColor(mask, mask, Imgproc.COLOR_GRAY2BGR);

        return src;
    }

    public void process(Bitmap inputBitmap) {

        Mat frame = new Mat();

        Utils.bitmapToMat(inputBitmap, frame);

        process(frame);

        Utils.matToBitmap(frame, inputBitmap);

    }
}
