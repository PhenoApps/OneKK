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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */

public class CircularWatershed {

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

    public CircularWatershed(DetectSeedsParams params) { mParams = params; }

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

    private Pair<Mat, ArrayList<Seed>> process(Mat src) {

        Mat threshed = src.clone();

        Imgproc.cvtColor(threshed, threshed, Imgproc.COLOR_RGB2GRAY);

        Imgproc.threshold(threshed, threshed, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/threshed.jpg", threshed);

        Mat kernel = new Mat(3, 3, CvType.CV_8U);

        Mat opening = new Mat();

        Imgproc.morphologyEx(threshed, opening, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 3);

        Mat sure_bg = new Mat();

        Imgproc.dilate(opening, sure_bg, kernel, new Point(-1, -1), 3);

        Mat dist_transform = new Mat();

        Imgproc.distanceTransform(sure_bg, dist_transform, Imgproc.DIST_L2, 5);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/dt.jpg", dist_transform);

        double max_val = findMax(dist_transform);

        Mat sure_fg = new Mat();

        Imgproc.threshold(dist_transform, sure_fg, 0.2 * max_val, 255, 0);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/sure_fg.jpg", sure_fg);

        sure_fg.convertTo(sure_fg, CvType.CV_8U);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/sure_fg8u.jpg", sure_fg);

        Mat unknown = new Mat();

        Core.subtract(sure_bg, sure_fg, unknown);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/unknown.jpg", unknown);

        Mat markers = new Mat();

        Imgproc.connectedComponents(sure_fg, markers);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/markers.jpg", markers);

        Core.add(markers, Mat.ones(markers.size(), markers.type()), markers);

//        for (int i = 0; i < markers.rows(); i++) {
//            for (int j = 0; j < markers.cols(); j++) {
//                double[] value = unknown.get(i, j);
//                if (unknown.get(i, j)[0] == 255) {
//                    markers.put(i, j, 0);
//                }
//            }
//        }

        markers.convertTo(markers, CvType.CV_32SC1);

        src.convertTo(src, CvType.CV_8UC3);

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2RGB);

        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/OneKK/markersA.jpg", markers);


        Imgproc.watershed(src, markers);

        markers.convertTo(markers, CvType.CV_8U);

        Mat markers2 = new Mat();

        Imgproc.adaptiveThreshold(markers, markers2, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 1);

        List<MatOfPoint> contours = new ArrayList<>();

        Mat hierarchy = new Mat();

        Imgproc.findContours(markers2, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        int count = 0;

        MatOfInt hull = new MatOfInt();

        ArrayList<Seed> seedMetrics = new ArrayList<Seed>();

        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {

            if (hierarchy.get(0, i)[2] == -1) {
                //Imgproc.convexHull(contours.get(i), hull);
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxCurve, 0.01, true);
                if (approxCurve.height() * approxCurve.width() > 25) {
                    count += 1;

                    Rect rect = Imgproc.boundingRect(contours.get(i));

                    Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), RECTANGLE_COLOR, 2);

                    Imgproc.putText(src, String.valueOf(count), new Point(rect.x, rect.y), Imgproc.FONT_HERSHEY_SIMPLEX, 4, TEXT_COLOR, 3);

                    double area = Imgproc.contourArea(contours.get(i));

                    double perimeter = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true);

                    seedMetrics.add(new Seed(area, perimeter, rect.height, rect.width));
                }
            }
        }


        return new Pair<>(src, seedMetrics);


    }

    public Pair<Bitmap, ArrayList<Seed>> process(Bitmap inputBitmap) {

        Mat frame = new Mat();

        Utils.bitmapToMat(inputBitmap, frame);

        Pair<Mat, ArrayList<Seed>> output = process(frame);

        Utils.matToBitmap(output.first, inputBitmap);

        return new Pair<>(inputBitmap, output.second);
    }
}
