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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */

public class HoughCircles {

    /**
     * Scalar colors require an alpha channel to be opaque
     * Fourth parameter is the alpha channel
     * Not required for Android version < 9
     */
    private final Scalar CONTOUR_COLOR = new Scalar(255, 0, 0, 255);
    private final Scalar RECTANGLE_COLOR = new Scalar(0, 255, 0, 255);
    private final Scalar TEXT_COLOR = new Scalar(0, 0, 0, 255);

    public HoughCircles() { }

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

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

        Mat circles = new Mat();
        Imgproc.HoughCircles(src, circles, Imgproc.CV_HOUGH_GRADIENT, 1, src.rows()/8, 200, 100, 0, 0);

        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfInt hull = new MatOfInt();

        Log.d("DrawContours", String.valueOf(contours.size()));

        for(int i = 0; i < contours.size(); i++){

            if (Imgproc.contourArea(contours.get(i)) > 100) {

                Rect rect = Imgproc.boundingRect(contours.get(i));
                //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));

                Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), RECTANGLE_COLOR, 2);

            }

            //Imgproc.convexHull(contours.get(i), hull);

            //ToDo: Verify that the rows()  is what needs to be used.
            //if(contours.get(i).rows()/ hull.rows() <= 3){
                //Imgproc.drawContours(src, contours, i, new Scalar(128, 128, 128, 255), 1);
            //}

        }

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
