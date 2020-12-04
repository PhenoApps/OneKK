package org.wheatgenetics.imageprocess.lightbox;

import android.graphics.Bitmap;

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
import org.wheatgenetics.utils.ImageProcessingUtil;
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.GaussianBlur;
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Identity identity = new Identity();
    private ConvertGrey convertGrey = new ConvertGrey();
    private Threshold threshold = new Threshold();
    private DrawContours drawContours = new DrawContours();
    private DistanceTransform distanceTransform = new DistanceTransform();
    private GaussianBlur gaussianBlur = new GaussianBlur();
    private Dilate dilate = new Dilate();

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



    //class used to define user-tunable parameters
    public static class DetectSeedsParams {

        private int areaHigh;

        public DetectSeedsParams(int areaHigh) {
            this.areaHigh = areaHigh;
        }
        int getAreaHigh() { return areaHigh; }
    }

    private File outputDirectory = null;

    public EnhancedWatershed(File output) {

        outputDirectory = output;

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

    /**
     * Estimates the diameter of a circle in px taking in areas in mm
     * @param circleAreas
     * @return
     */
    private Double EstimateCircleDiameterpx(Double[] circleAreas){
        Double[] radii = new Double[circleAreas.length];
        for(int i = 0; i < circleAreas.length; i++) {
            radii[i] = Math.sqrt(circleAreas[i]/ 3.14);
        }
        Double diameterpx = 2 * Mean(radii);

        return diameterpx;
    }

    /**
     * Estimates the height and width of a contour. The idea is to use the method to estimate the height and widths of contours deemed kernels.
     * @param kernelContours
     * @param coinDiametermm
     * @param gtCoins
     * @return
     */
    private ArrayList<Pair<MatOfPoint, Pair<Double, Double>>> EstimateSeedHeightWidth(ArrayList<MatOfPoint> kernelContours, double coinDiametermm, ArrayList<MatOfPoint> gtCoins){
        ArrayList<Pair<MatOfPoint, Pair<Double, Double>>> estimations = new ArrayList<>();
        Double[] coinAreapx = new Double[gtCoins.size()];

        for(int i = 0; i < gtCoins.size(); i++){
            coinAreapx[i] = Imgproc.contourArea(gtCoins.get(i));
        }

        Double coinDiameterpx = EstimateCircleDiameterpx(coinAreapx);

        for(MatOfPoint contour: kernelContours) {
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
            Double height = measureArea(coinDiameterpx, coinDiametermm, rect.size.height);
            Double width =  measureArea(coinDiameterpx, coinDiametermm, rect.size.width);
            estimations.add(new Pair<>(contour, new Pair<>(height, width)));
        }


        return estimations;
    }

    /**
     * Estimates the area of a contour. The idea is to estimate the area of the contours deemed kernels.
     * @param kernelContours
     * @param coinAreamm
     * @param gtCoins
     * @return
     */
    private ArrayList<Pair<MatOfPoint, Double>> EstimateSeedArea(ArrayList<MatOfPoint> kernelContours, double coinAreamm, ArrayList<MatOfPoint> gtCoins){
        Double[] coinAreapx = new Double[gtCoins.size()];
        ArrayList<Pair<MatOfPoint, Double>> contourAreas = new ArrayList<Pair<MatOfPoint, Double>>();
        for(int i = 0; i < gtCoins.size(); i++){
            coinAreapx[i] = Imgproc.contourArea(gtCoins.get(i));
        }

        Double meanCoinAreapx = Mean(coinAreapx);

        for(int i = 0; i < kernelContours.size(); i++){
            Double kernelAreapx = Imgproc.contourArea(kernelContours.get(i));
            contourAreas.add(new Pair<>(kernelContours.get(i), measureArea(meanCoinAreapx, coinAreamm, kernelAreapx)));
        }
        return contourAreas;
    }

    private <T extends Number> double Mean(T[] values){
        double sum = 0.0;
        for(int i = 0; i < values.length; i++){
            sum += values[i].doubleValue();
        }
        return (float)(sum/values.length);
    }

    /**
     * Computes the quartile range of a given list of values.
     * @param values
     * @return
     */
    private Pair<Double, Double> quartileRange(ArrayList<Double> values){
        int size = values.size();

        if (size > 1) {
            Double[] doubleValues = values.toArray(new Double[]{});

            Arrays.sort(doubleValues);

            double Q2 = doubleValues[doubleValues.length / 2];

            Double[] lowerArray = Arrays.copyOfRange(doubleValues, 0, doubleValues.length / 2);
            Double[] upperArray = Arrays.copyOfRange(doubleValues, doubleValues.length / 2, doubleValues.length);

            double Q1 = lowerArray[lowerArray.length / 2];
            double Q3 = upperArray[upperArray.length / 2];

            double IQR = Q3 - Q1;

            Double firstValue = Q1 - 1.5 * IQR;
            Double secondValue = Q3 + 1.5 * IQR;

            return new Pair<>(firstValue, secondValue);
        }

        return new Pair<Double, Double>(-1.0, -1.0);

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

    /**
     * Class to hold the estimated seed counts of the contours deemed seeds on the image
     */
    class EstimatedSeedCounts{
        private int floorCount;
        private int roundCount;
        private int areaRoundCount;


        EstimatedSeedCounts(int floorCount, int roundCount, int areaRoundCount) {
            this.floorCount = floorCount;
            this.roundCount = roundCount;
            this.areaRoundCount = areaRoundCount;
        }
    }

    /**
     * Class that holds the properties of contours deemed ground truths i.e. segmented kernel contours on the image.
     */
    private class GroundTruths {

        ArrayList<MatOfPoint> contours;
        double stdev;
        Integer defectMean;

        GroundTruths(ArrayList<MatOfPoint> contours, double stddev, Integer defectMean) {
            this.contours = contours;
            this.stdev = stddev;
            this.defectMean = defectMean;
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
        Imgcodecs.imwrite(outputDirectory.getAbsolutePath()+"/"+name+".bmp", img);
    }

    private Bitmap toBitmap(Mat src) {
        Bitmap bmp = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bmp);
        return bmp;
    }

    private Double[] ComputeContourAreas(ArrayList<MatOfPoint> contours){
        Double[] areas = new Double[contours.size()];

        for (int i = 0; i < contours.size(); i++) {
            areas[i] = Imgproc.contourArea(contours.get(i));
        }
        return areas;
    }

    /**
     * Applies watershed algorithm on an image and returns the markers identified.
     * @param src - The source image on which watershed needs to be applied
     * @param sure_fg - The sure foreground on the source image
     * @param unknown - The unknown part of the source image
     * @return
     */
    private Mat ApplyWatershed(Mat src, Mat sure_fg, Mat unknown){
        Mat markers = new Mat();

        Imgproc.connectedComponents(sure_fg, markers, 8, CvType.CV_32S);

        Core.multiply(markers, new Scalar(100), markers);

        writeFile("cc", markers);

        Core.add(markers, new Scalar(1), markers);

        markers = matSwap(markers, 255, 0, false, unknown);

        writeFile("ccswap", markers);

        Mat blurred = new Mat();

        Imgproc.GaussianBlur(src, blurred, new Size(3,3), 5);

        //result.getPi

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

        Imgproc.GaussianBlur(markers, markers, new Size(3,3), 5);

        writeFile("markersC", markers);

        return markers;

    }

    /**
     * Identifies the child contours that are their own independent seeds based on a threshold i.e. the average ground truth area of the seeds.
     * A child contour is deemed its own seed if its area is at least 1/4th of the average ground truth area
     * @param contours
     * @param hierarchy
     * @param gtAvgArea
     * @return
     */
    private ArrayList<MatOfPoint> FindAllSeedContours(ArrayList<MatOfPoint> contours, Mat hierarchy, Double gtAvgArea){
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

                            //                            for (MatOfPoint coin : gts.coins) {
                            //
                            //                                if (!within) {
                            //                                    RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(child.toArray()));
                            //
                            //                                    int minX = (int) (rect.center.x - rect.size.width / 2);
                            //                                    int minY = (int) (rect.center.y - rect.size.height / 2);
                            //                                    int maxX = (int) (rect.center.x + rect.size.width / 2);
                            //                                    int maxY = (int) (rect.center.y + rect.size.height / 2);
                            //
                            //                                    for (int x = minX; x < maxX; x += 4) {
                            //                                        if (!within) {
                            //                                            for (int y = minY; y < maxY; y += 4) {
                            //                                                if (Imgproc.pointPolygonTest(new MatOfPoint2f(coin.toArray()), new Point(x, y), false) > 0) {
                            //                                                    within = true;
                            //                                                    break;
                            //                                                }
                            //                                            }
                            //                                        } else break;
                            //                                    }
                            //                                }
                            //                            }

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
        return measuredContours;
    }

    /**
     * Provides an estimated seed count based on the contours identified on the image deemed to be seeds.
     * Algorithm:
     * 1. Checks to see if the contour is less than a threshold. This is to ensure that the contour for the entire image is not even processed because that happens frequently.
     * 2.For all contours that truly point to seeds, compute the Convexity Defects and identify the maximum distance from the hull to contour (Max_Pt_Depths).
     *
     * Two types of counts are provided. A) Area Based B) Contour Based
     *
     * Area Based Count: Computes area of contours and uses ground truth areas to estimate the number of seeds in the contour.
     *
     * Contour Based Count: Counts all contours deemed as seeds based on a criterion.
     *       If mean(Max_Pt_Depths) > GT_DefectMedian + 4 x GT_Defect_Std_Dev:
     *          ContourBasedCount += round(contourArea/AverageGroundTruthArea)
     *       Else:
     *          ContourBasedCount += 1
     * @param seedContours
     * @param gtAvgArea
     * @param gtDefectMean
     * @param gtStdDev
     * @return
     */
    private EstimatedSeedCounts EstimateSeedCount(ArrayList<MatOfPoint> seedContours, Double gtAvgArea, int gtDefectMean, Double gtStdDev){
        int floorCount = 0;
        int roundCount = 0;
        int areaRoundCount = 0;

        for (int i = 0; i < seedContours.size(); i++) {

            MatOfPoint contour = seedContours.get(i);

            double area = Imgproc.contourArea(contour);

            if (area < 500000) {

                RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));

                MatOfInt hull = new MatOfInt();

                Imgproc.convexHull(contour, hull);

                double areaPercent = area/gtAvgArea;

//                    int minX = (int) (rect.center.x - rect.size.width/2);
//                    int minY = (int) (rect.center.y - rect.size.height/2);
//                    int maxX = (int) (rect.center.x + rect.size.width/2);
//                    int maxY = (int) (rect.center.y + rect.size.height/2);

                ArrayList<Double> pixels = new ArrayList<>();

//                    for (int x = minX; x < maxX; x += 4) {
//                        for (int y = minY; y < maxY; y += 4) {
//                            if (Imgproc.pointPolygonTest(new MatOfPoint2f(contour.toArray()), new Point(x,y), false) > 0) {
//                                pixels.add(grayPolygonTest.get(y, x)[0]);
//                            }
//                        }
//                    }

                //       if (pixels.size() > 0 && Mean(pixels.toArray(new Double[]{})) < 100) {

                if (hull.rows() > 0) {

                    ArrayList<Double> defVals = new ArrayList<>();
                    MatOfInt4 defects = new MatOfInt4();
                    Imgproc.convexityDefects(contour, hull, defects);

                    for (int d=0; d < defects.rows(); d++) {

                        defVals.add(defects.get(d, 0)[3]);
                    }

                    if (Mean(defVals.toArray(new Double[]{})) > gtDefectMean + 4 * gtStdDev) {

                        roundCount += Math.round(areaPercent);
                    } else {
                        roundCount += 1;
                    }
                }

                areaRoundCount += Math.round(areaPercent);

                floorCount += 1;

            }
        }
        return new EstimatedSeedCounts(floorCount, roundCount, areaRoundCount);

    }

    /**
     * Finds the segmented kernel contours i.e. contours that don't touch other kernels in the image.
     * Algorithm:
     * 1. Computes the convex hull of each of the contours on the image.
     * 2. All contours that meet the criteria If(length(contour)/ length(convex_hull)) <= 3 are marked as potential ground truths.
     * 3. Remove the outliers from the potential ground truths by computing the Interquartile Range (IR) of the areas. Areas greater than 1.5 * IR and less than 1.5 * IR are considered in range and those contours are labeled as ground truths.
     *------------------------------------------------------------------------------
     * (Used later for parameter estimation)
     * On each of the identified ground truths, compute the convexity defects and the median and the standard deviation of the fixed point depths i.e. the farthest point from the hull to the object on the image.
     * @param contours
     * @return
     */
    private GroundTruths FindGroundTruths(ArrayList<MatOfPoint> contours){
        ArrayList<Pair<Double, MatOfPoint>> areas = new ArrayList<Pair<Double, MatOfPoint>>();
        ArrayList<MatOfPoint> potentialGT = new ArrayList<>();
        MatOfInt hull = new MatOfInt();
        for(int i = 0; i < contours.size(); i++){
            Imgproc.convexHull(contours.get(i), hull);

            if(contours.get(i).rows()/ hull.rows() <= 3){
                potentialGT.add(contours.get(i));
            }

        }

        if(!potentialGT.isEmpty()){
            for(int i = 0; i < contours.size(); i++){
                Double area = Imgproc.contourArea(contours.get(i));
                areas.add(new Pair(area, contours.get(i)));
            }
        }

        areas = InterquartileReduce(areas);

        ArrayList<MatOfPoint> groundTruths = new ArrayList<>();
        for(Pair<Double, MatOfPoint> area: areas){
            groundTruths.add(area.second);
        }

        List<Integer> defectVals = new ArrayList<>();


        for(MatOfPoint contour : groundTruths){
            //            Moments M = Imgproc.moments(contour);
            //            int cX = (int)(M.m10/ M.m00);
            //            int cY = (int)(M.m01/ M.m00);

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

        return new GroundTruths(groundTruths, calcStdDev(defectVals), defectMean);
    }

    /**
     * Estimates the count, length, height and area of the kernels on the image using the coins as ground truths.
     *
     *
     * @param src the source image captured by the camera
     * @param gts the coin contours identified during the coin recognition step
     * @return
     */
    private ImageProcessingUtil.Companion.ContourResult process(Mat src, List<MatOfPoint> gts) {

        writeFile("source", src);

        ImageProcessingUtil.Companion.ContourResult result = new ImageProcessingUtil.Companion.ContourResult();

        result.getImages().add(toBitmap(src.clone()));

        Mat graySrc =  Mat.zeros(src.size(), CvType.CV_8U);

        Mat markerContours = Mat.zeros(src.size(), CvType.CV_8U);

        GroundTruths groundTruths = FindGroundTruths(ImageProcessingUtil.Companion.getSeedContours());

        Imgproc.drawContours(graySrc, groundTruths.contours, -1 , new Scalar(255, 255, 255, 255), -1);

        writeFile("ground truths", graySrc);

        Double[] areas = ComputeContourAreas(groundTruths.contours);

        double gtAvgArea = Mean(areas);

        Mat kernel = new Mat(3, 3, CvType.CV_8U);

        Imgproc.GaussianBlur(graySrc, graySrc, new Size(3.0, 3.0), 15.0);

        Mat grayPolygonTest = graySrc.clone();

        Imgproc.threshold(graySrc, graySrc, 0, 255, Imgproc.THRESH_BINARY);

        writeFile("gt thresh", graySrc);

        Imgproc.erode(graySrc, graySrc, kernel, new Point(-1,-1), 1);

        Mat seeds = Mat.zeros(graySrc.size(), graySrc.type());

        Imgproc.drawContours(seeds, ImageProcessingUtil.Companion.getSeedContours(), -1, new Scalar(255, 255, 255, 255), -1);

        writeFile("nonGTContours", seeds);

        result.getPipeline().add(drawContours);

        result.getImages().add(toBitmap(seeds));
        //
        //        for (MatOfPoint contour : groundTruths) {
        //            Imgproc.drawContours(gray, gts, gts.indexOf(contour), new Scalar(0, 0, 0), 5);
        //            Imgproc.drawContours(gray, gts, gts.indexOf(contour), new Scalar(0, 0, 0), -1);
        //        }
        //
        //        writeFile("blur_erode", gray);

        Mat sure_bg = new Mat();

        Imgproc.dilate(seeds, sure_bg, kernel, new Point(-1,-1), 2);

        writeFile("sure_bg", sure_bg);

        Imgproc.distanceTransform(seeds, seeds, distanceTransform.getDistanceType(), distanceTransform.getMaskSize());

        // result.getPipeline().add(distanceTransform);

        //  result.getImages().add(toBitmap(seeds));

        Core.normalize(seeds, seeds, 0, 1.0, Core.NORM_MINMAX);

        writeFile("dt", seeds);

        Mat sure_fg = new Mat(seeds.size(), seeds.type());

        Imgproc.threshold(seeds, sure_fg, 0.2, 255, Imgproc.THRESH_BINARY);

        sure_fg.convertTo(sure_fg, CvType.CV_8U);

        writeFile("sure_fg", sure_fg);

        result.getPipeline().add(threshold);

        result.getImages().add(toBitmap(sure_fg));

        Mat unknown = new Mat();

        Core.subtract(sure_bg, sure_fg, unknown);

        writeFile("unknown", unknown);

        Mat markers = ApplyWatershed(src, sure_fg, unknown);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgproc.findContours(markers, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.drawContours(markerContours, contours, -1 , new Scalar(255, 255, 255, 255), 1);

        writeFile("markerContours", markerContours);

        ArrayList<MatOfPoint> seedContours = FindAllSeedContours(new ArrayList<>(contours), hierarchy, gtAvgArea);

        EstimatedSeedCounts seedCounts = EstimateSeedCount(seedContours, gtAvgArea, groundTruths.defectMean, groundTruths.stdev);

        Random rand = new Random();
        Imgproc.drawContours(src, seedContours, -1, new Scalar(rand.nextInt(255),rand.nextInt(255), 255,255), -1);

        result.getImages().add(toBitmap(src.clone()));

        int maxCount = Math.max(seedCounts.floorCount, seedCounts.roundCount);

        int minCount = Math.min(seedCounts.floorCount, seedCounts.roundCount);

        Log.d("Contours size: ", String.valueOf(contours.size()));

        Log.d("Count: ", minCount + "-" + maxCount);

        maxCount = Math.max(seedCounts.floorCount, seedCounts.areaRoundCount);
        minCount = Math.min(seedCounts.floorCount, seedCounts.areaRoundCount);

        Log.d("Area based Count: ", minCount + "-" + maxCount);

        ArrayList<Pair<MatOfPoint, Double>> contourAreas = EstimateSeedArea(groundTruths.contours, 284.87, new ArrayList<>(gts));
        ArrayList<Pair<MatOfPoint, Pair<Double, Double>>> estimations = EstimateSeedHeightWidth(groundTruths.contours, 19.05, new ArrayList<>(gts));
//        for(Pair<MatOfPoint, Double> contourArea: contourAreas){
//            RotatedRect rotatedRect = Imgproc.minAreaRect(ImageProcessingUtil.Companion.toMatOfPoint2f(contourArea.first));
//            Double area = Imgproc.contourArea(contourArea.first);
//            Log.d("Contour Area: ", String.valueOf(contourArea.second));
//            Point centroid = ImageProcessingUtil.Companion.calculateMomentCenter(contourArea.first);
//            Imgproc.putText(src, String.format("%.2f", contourArea.second), centroid, 3, 1, new Scalar(255, 255, 0));
//
////            data class Detections(var rect: Rect, var circ: Double, var center: Point, var contour: MatOfPoint, var area: Double, var minAxis: Double, var maxAxis: Double)
//
//            int maxAxis = Math.max(rotatedRect.boundingRect().height, rotatedRect.boundingRect().width);
//            int minAxis = Math.min(rotatedRect.boundingRect().height, rotatedRect.boundingRect().width);
//
//            result.getDetections().add(new ContourStats(centroid.x, centroid.y, area, minAxis, maxAxis, area > gtAvgArea));
//        }

        for(MatOfPoint contour: seedContours){
            RotatedRect rotatedRect = Imgproc.minAreaRect(ImageProcessingUtil.Companion.toMatOfPoint2f(contour));
            Double area = Imgproc.contourArea(contour);
            Point centroid = ImageProcessingUtil.Companion.calculateMomentCenter(contour);

            int maxAxis = Math.max(rotatedRect.boundingRect().height, rotatedRect.boundingRect().width);
            int minAxis = Math.min(rotatedRect.boundingRect().height, rotatedRect.boundingRect().width);

            result.getDetections().add(new ContourStats(centroid.x, centroid.y, area, minAxis, maxAxis, area > gtAvgArea * 2));
        }

        //        for(Pair<MatOfPoint, Pair<Double, Double>> estimate: estimations){
        //            Log.d("Height and Width: ", String.valueOf(estimate.second.first) + String.valueOf(estimate.second.second));
        //            Point centroid = ImageProcessingUtil.Companion.calculateMomentCenter(estimate.first);
        //            Imgproc.putText(src, String.format("%.2f %.2f", estimate.second.first, estimate.second.second), centroid, 3, 1, new Scalar(255, 255, 0));
        //        }

        writeFile("finalImg", src);

        return result; //new Pair<>(src, new ArrayList<Seed>());

    }

    public ImageProcessingUtil.Companion.ContourResult process(Bitmap inputBitmap, List<MatOfPoint> gts) {

        Mat frame = new Mat();

        Utils.bitmapToMat(inputBitmap, frame);

        ImageProcessingUtil.Companion.ContourResult result = process(frame, gts);

        return result;
    }
}