package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.support.v4.util.Pair;
import android.util.Log;
import android.util.SparseArray;

import org.opencv.BuildConfig;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */

public class DetectSeeds {

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

    //class used to define user-tunable parameters
    public static class DetectSeedsParams {

        private int areaHigh;

        public DetectSeedsParams(int areaHigh) {
            this.areaHigh = areaHigh;
        }
        int getAreaHigh() { return areaHigh; }
    }

    public DetectSeeds(DetectSeedsParams params) { mParams = params; }

    private double calcCircularity(double area, double peri) {

        return 4 * (Math.PI) * (area / (Math.pow(peri, 2)));
    }

    private double calcStdDev(ArrayList<Double> values) {

        double mean = 0;
        for (double x : values) mean += x;
        mean = mean / values.size();

        Log.d("CoinRecMean", String.valueOf(mean));

        double variance = 0;
        for (double x : values) {
            Log.d("CoinRecVal", String.valueOf(x));
            variance += Math.pow(x-mean, 2);
        }

        variance /= values.size();

        Log.d("CoinRecVar", String.valueOf(variance));

        return Math.sqrt(variance);
    }

    private Pair<Boolean, String> coinRecognition(List<Map.Entry<Double, MatOfPoint>> entries) {

        ArrayList<Double> areas = new ArrayList<>();
        ArrayList<Double> circus = new ArrayList<>();

        for (Map.Entry<Double, MatOfPoint> entry : entries) {
            double peri = Imgproc.arcLength(new MatOfPoint2f(entry.getValue().toArray()), true);
            areas.add(entry.getKey());
            circus.add(calcCircularity(entry.getKey(), peri));
        }

        double areaStdDev = calcStdDev(areas);
        double circuStdDev = calcStdDev(circus);

        Log.d("CoinRec", String.valueOf(areaStdDev));
        Log.d("CoinRec", String.valueOf(circuStdDev));

        double areaThreshold = (1288.2439780084362 * 0.5);
        double areaLow = 1288.2439780084362 - areaThreshold;
        double areaHigh = 1288.2439780084362 + areaThreshold;

        double circuThreshold = 0.002341670768322188 * 0.5;
        double circuLow = 0.002341670768322188 - circuThreshold;
        double circuHigh = 0.002341670768322188 + circuThreshold;

        return new Pair<>(true, "");

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

    private Pair<Mat, ArrayList<Seed>> process(Mat src) {

        Mat threshed = src.clone();

        Imgproc.cvtColor(threshed, threshed, Imgproc.COLOR_RGB2GRAY);

        Imgproc.medianBlur(threshed, threshed, 9);

        Imgproc.adaptiveThreshold(threshed, threshed, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Imgproc.Canny(threshed, threshed, 200, 255);

        Imgproc.GaussianBlur(threshed, threshed, new Size(5,5), 10);

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(threshed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        HashMap<Double, MatOfPoint> areaMap = new HashMap<>();

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            areaMap.put(area, contour);
        }

        ArrayList<Map.Entry<Double, MatOfPoint>> areaKeys = new ArrayList<>(areaMap.entrySet());
        Collections.sort(areaKeys, new Comparator<Object>() {

            @Override
            public int compare(Object a, Object b) {

                Double key1 = ((Map.Entry<Double, MatOfPoint>) a).getKey();
                Double key2 = ((Map.Entry<Double, MatOfPoint>) b).getKey();

                if (key1 == key2) return 0;
                else if (key1 > key2) return 1;
                else return -1;
            }
        });

        if (areaKeys.size() >= 4) {

            List<Map.Entry<Double, MatOfPoint>> coins = areaKeys.subList(areaKeys.size()-4, areaKeys.size());

            Pair<Boolean, String> output = coinRecognition(coins);


            if (!output.first) {

                Log.d("CoinRec", output.second);

            } else {

                Mat dst = src.clone();

                Map.Entry<Double, MatOfPoint> coinGroundTruth = coins.get(0);

                List<Map.Entry<Double, MatOfPoint>> seeds = areaKeys.subList(0, areaKeys.size()-4);
                //List<Map.Entry<Double, MatOfPoint>> seeds = areaKeys; //.subList(0, areaKeys.size()-4);

                ArrayList<Seed> seedMetrics = new ArrayList<Seed>();
                int seedCount = 0;

                for ( int i = 0; i < seeds.size(); i++) {

                    Map.Entry<Double, MatOfPoint> entry = seeds.get(i);

                    if (entry.getKey() > 2000) {

                        double area = entry.getKey();

                        double perimeter = Imgproc.arcLength(new MatOfPoint2f(entry.getValue().toArray()), true);

                        Rect bbox = Imgproc.boundingRect(entry.getValue());

                        double length = bbox.height;

                        double width = bbox.width;

                        seedCount += 1;

                        seedMetrics.add(new Seed(area, perimeter, length, width));

                        List<MatOfPoint> contour = new ArrayList<>();
                        contour.add(entry.getValue());


                        Imgproc.drawContours(dst, contour, -1, CONTOUR_COLOR, 3);

                        Imgproc.rectangle(dst, new Point(bbox.x, bbox.y), new Point(bbox.x+bbox.width, bbox.y+bbox.height), RECTANGLE_COLOR, 2);

                        //TODO add ground truth as parameter to detect seeds
                        String measurement = String.valueOf(measureArea(coinGroundTruth.getKey(), 280.552077, area));
                        Imgproc.putText(dst, measurement.substring(0, measurement.lastIndexOf('.')+2), new Point(bbox.x, bbox.y), Imgproc.FONT_HERSHEY_PLAIN, 4, TEXT_COLOR, 3);
                    }
                }

                return new Pair<>(dst, seedMetrics);
            }
        } else {
            Log.d("CoinRec", "recognized less than 4 objects");
        }

        return new Pair<>(threshed, new ArrayList<Seed>());
    }

    public Pair<Bitmap, ArrayList<Seed>> process(Bitmap inputBitmap) {

        Mat frame = new Mat();

        Utils.bitmapToMat(inputBitmap, frame);

        Pair<Mat, ArrayList<Seed>> output = process(frame);

        Utils.matToBitmap(output.first, inputBitmap);

        return new Pair<>(inputBitmap, output.second);
    }
}
