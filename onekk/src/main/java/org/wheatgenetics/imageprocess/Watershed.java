//package org.wheatgenetics.imageprocess;
//
//import android.graphics.Bitmap;
//
//import android.os.Build;
//import android.util.Log;
//import android.util.SparseArray;
//import android.util.SparseIntArray;
//
//import junit.framework.Assert;
//
//import org.opencv.BuildConfig;
//import org.opencv.android.Utils;
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfFloat;
//import org.opencv.core.MatOfInt;
//import org.opencv.core.MatOfInt4;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.imgproc.Moments;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
///**
// * Created by chaneylc on 8/22/2017.
// */
//
//public class Watershed {
//
//    //opencv constants
//    private final Scalar mContourColor = new Scalar(255, 0, 0);
//    private final Scalar mContourMomentsColor = new Scalar(0, 0, 0);
//
//    //seed counter algorithm variables
//    int mNumSeeds = 0;
//
//    private Mat mLabels;
//    private long mRuntime;
//    private Mat mAnnotated;
//
//    private SparseArray<ContourLocation> mContours;
//
//    //seed counter parameters
//    private WatershedParams mParams;
//
//    public SparseArray<ContourLocation> getContourLocations() {
//        return mContours;
//    }
//
//    //class used to define user-tunable parameters
//    public static class WatershedParams {
//
//        private int areaLow, areaHigh, defaultRate;
//        private double sizeLowerBoundRatio, newSeedDistRatio;
//
//        public WatershedParams(int areaLow, int areaHigh, int defaultRate,
//                               double sizeLowerBoundRatio, double newSeedDistRatio) {
//            this.areaLow = areaLow;
//            this.areaHigh = areaHigh;
//            this.defaultRate = defaultRate;
//            this.sizeLowerBoundRatio = sizeLowerBoundRatio;
//            this.newSeedDistRatio = newSeedDistRatio;
//        }
//        int getAreaLow() { return areaLow; }
//        int getAreaHigh() { return areaHigh; }
//        int getDefaultRate() { return defaultRate; }
//        double getSizeLowerBoundRatio() { return sizeLowerBoundRatio; }
//        double getNewSeedDistRatio() { return newSeedDistRatio; }
//    }
//
//    public class ContourLocation {
//
//        private double x, y;
//        private int count;
//
//        public ContourLocation(double x, double y, int count) {
//            this.x = x;
//            this.y = y;
//            this.count = count;
//        }
//
//        public double getX() { return this.x; }
//        public double getY() { return this.y; }
//        public int getCount() { return this.count; }
//        public double dstTo(double x, double y) {
//            return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
//        }
//    }
//
//    public Watershed(WatershedParams params) { mParams = params; }
//
//    //function for assignment only certain Mat indices s.a python index assignments (dst[src==lower/upper] = val)
//    private Mat maskReplace(Mat src, Mat dst, Scalar lower, Scalar upper, Scalar val) {
//
//        //initialize mask
//        Mat mask = new Mat(src.size(), src.type());
//
//        //save all values between lower/upper to a mask
//        Core.inRange(src, lower, upper, mask);
//
//        //set all mask values to val in dst
//        dst.setTo(val, mask);
//
//        mask.release();
//
//        return dst;
//    }
//
//
//    //iterates through areas and perimeters
//    //outputs a double[] estimates = { justOnesAreaAverage, justOnesPerimeterAverage }
//    //uses assumption that most seeds will not be touching other seeds, therefore the average
//    //contour area/perimeter should be ideally one. The purpose of this function is to find
//    //these just-one-truths and return the given estimate for thresholds in our algorithm.
//    private double[] justOnesAverage(List<Double> areas, List<Double> perimeters) {
//
//        if (BuildConfig.DEBUG && (areas.size() != perimeters.size()))
//            throw new AssertionError();
//
//        double avgArea = 0.0;
//        double avgPerimeter = 0.0;
//
//        for (double d : areas)
//            avgArea += d;
//
//        for (double d: perimeters)
//            avgPerimeter += d;
//
//
//        avgPerimeter = avgPerimeter / perimeters.size();
//        avgArea = avgArea / areas.size();
//
//        int count = 0;
//        double[] oneAvg = {0.0, 0.0};
//
//        for (Double d: areas) {
//
//            int index = areas.indexOf(d);
//
//            double p = perimeters.get(index);
//
//            int scoreA = (int) Math.round(d / avgArea);
//            int scoreP = (int) Math.round(p / avgPerimeter);
//
//            if (scoreA == 1 && scoreP == 1) {
//                count++;
//                //oneAvg[0] += d;
//                //oneAvg[1] += p;
//                oneAvg[0] += Math.pow(d - avgArea, 2);
//                oneAvg[1] += Math.pow(d - avgPerimeter, 2);
//            }
//        }
//
//        oneAvg[0] /= count;
//        oneAvg[1] /= count;
//
//        return oneAvg;
//    }
//
//    //TODO add option to subtract counted areas by variance, essentially recognize smallest seeds/noise
//    //similar to justOnesAverage, this function returns a count formally
//    //count = min(estimatedAreaCount, estimatedPerimeterCount)
//    private List<Integer> estimatedClusterCount(List<Double> areas, List<Double> perimeters) {
//
//        if (BuildConfig.DEBUG && (areas.size() != perimeters.size()))
//            throw new AssertionError();
//
//        //calculate mean area of a contour
//        double avgArea = 0.0;
//        double avgPerimeter = 0.0;
//        for (double d : areas)
//            avgArea += d;
//        for (double d : perimeters)
//            avgPerimeter += d;
//
//        avgArea = avgArea / areas.size();
//        avgPerimeter = avgPerimeter / perimeters.size();
//
//        double areaVariance = .0;
//        double periVariance = 0.;
//        for (double d : areas)
//            areaVariance += Math.pow(d - avgArea, 2);
//        for (double d: perimeters)
//            periVariance += Math.pow(d - avgPerimeter, 2);
//
//        areaVariance = areaVariance / areas.size();
//        periVariance = periVariance / perimeters.size();
//
//        double stdDev = Math.sqrt(areaVariance);
//        double stdDevPeri = Math.sqrt(periVariance);
//
//        //Log.d("AREA VARIANCE", String.valueOf(areaVariance / (areas.size())));
//        //Log.d("PERIMETER VARIANCE", String.valueOf(periVariance / (perimeters.size() )));
//
//        List<Double> onesA = new ArrayList<>();
//        List<Double> onesP = new ArrayList<>();
//
//        List<Integer> fakeCounts = new ArrayList<>();
//        List<Integer> counts = new ArrayList<>();
//        //double scoreAvg = 0.0;
//        //List<Double> areaScore = new ArrayList<>();
//        double fakeAvgCount = 0.0;
//        for (Double d: areas) {
//
//            int score = (int) Math.round(d / avgArea);
//
//            fakeCounts.add(score);
//
//            if (score == 1 || (d > avgArea - stdDev && d < avgArea + stdDev)) onesA.add(d); //counts.add(score);
//
//            if (BuildConfig.DEBUG) {
//                //Log.d(String.valueOf(areas.indexOf(d)), String.valueOf(score));
//            }
//        }
//
//        for (Double d: perimeters) {
//
//            int score = (int) Math.round(d / avgPerimeter);
//
//            if (score == 1 || (d > avgPerimeter - stdDevPeri && d < avgPerimeter + stdDevPeri)) onesP.add(d);
//
//            int index = perimeters.indexOf(d);
//
//            score = Math.min(score, fakeCounts.get(index));
//
//            fakeCounts.set(index, score);
//
//            fakeAvgCount += score;
//            //int index = perimeters.indexOf(d);
//            //counts.set(index, Math.min(score, counts.get(index)));
//
//            if (BuildConfig.DEBUG) {
//                //Log.d(String.valueOf(perimeters.indexOf(d)), String.valueOf(score));
//            }
//        }
//
//        fakeAvgCount /= counts.size();
//
//        double onesAvgA = 0.0, onesAvgP = 0.0;
//        for (Double d: onesA) onesAvgA += d;
//        for (Double d: onesP) onesAvgP += d;
//        onesAvgA = onesAvgA / onesA.size();
//        onesAvgP = onesAvgP / onesP.size();
//
//        double onesVarA = 0.;
//        double onesVarP = .0;
//        for (Double d: onesA) onesVarA += Math.pow(d - onesAvgA, 2);
//        for (Double d: onesP) onesVarP += Math.pow(d - onesAvgP, 2);
//
//        double avgCount = 0.0;
//        for (Double d: areas) {
//
//            int score = (int) Math.round(d / onesAvgA);
//
//            if (d > onesAvgA - onesVarA && d < onesAvgA + onesVarA) score = 1;
//
//            counts.add(score);
//
//            if (BuildConfig.DEBUG) {
//                //Log.d(String.valueOf(areas.indexOf(d)), String.valueOf(score));
//            }
//        }
//
//        for (Double d: perimeters) {
//
//            int score = (int) Math.round(d / onesAvgP);
//
//            if (d > onesAvgP - onesVarP && d < onesAvgP + onesVarP) score = 1;
//
//            int index = perimeters.indexOf(d);
//
//            //score = Math.min(score, counts.get(index));  TODO CHECK WHY THIS HELPS ACCURACY
//            avgCount += score;
//            counts.set(index, (int)Math.round((score + counts.get(index)) / 2.0));//score); //(int)Math.round((score + counts.get(index)) / 2.0)); //Math.min(score, counts.get(index)));
//            //Log.d("coutn", String.valueOf(Math.min(score, counts.get(index))));
//            if (BuildConfig.DEBUG) {
//                //Log.d(String.valueOf(perimeters.indexOf(d)), String.valueOf(score));
//            }
//        }
//
//        avgCount /= counts.size();
//
//        /*Mat data = new Mat(areas.size(), 4, CvType.CV_8UC1);
//        Mat mean = new Mat(1, 4, CvType.CV_8UC1);
//
//        for (int i = 0; i < data.cols(); i++) {
//            data.put(i, 0, areas.get(i));
//            data.put(i, 1, perimeters.get(i));
//            data.put(i, 2, counts.get(i));
//            data.put(i, 3, fakeCounts.get(i));
//        }
//
//        mean.put(0, 0, avgArea);
//        mean.put(0, 1, avgPerimeter);
//        mean.put(0, 2, avgCount);
//        mean.put(0, 3, fakeAvgCount);
//
//        long pcaRun = System.nanoTime();
//        Mat eigenvectors = new Mat(4, 4, CvType.CV_8UC1);
//        Core.PCACompute(data, mean, eigenvectors);
//
//        for (int i = 0; i < 4; i++) {
//            for (int j = 0; j < 4; j++) {
//                Log.d(i + ":" + j, String.valueOf(eigenvectors.get(i,j)[0]));
//
//            }
//        }
//        Log.d("PCA Time", String.valueOf(System.nanoTime() - pcaRun));
//        //scoreAvg = scoreAvg / areas.size();*/
//
//        /*for (int i = 0; i < 10; i++) {
//
//            List<Double> scores = new ArrayList<>();
//            double score = 0.0;
//            for (Double d : areaScore) {
//                score += d / scoreAvg;
//                scores.add(d / scoreAvg);
//            }
//            scoreAvg = score / scores.size();
//            areaScore = scores;
//            Log.d("Score update", String.valueOf(scoreAvg));
//
//        }*/
//        //calculate contour area variance with mean contour area
//        /*double areaVariance = 0.0;
//        for (Double d : areas) {
//            areaVariance = areaVariance + Math.pow(d - avgArea, 2);
//        }
//        areaVariance = areaVariance / areas.size();
//
//        return new double[] {avgArea, areaVariance};*/
//
//        return counts;
//    }
//
//    private Mat process(Mat frame, int alpha, double beta) {
//
//        Mat annotatedFrame = frame.clone();
//
//        long runTime = System.nanoTime();
//
//        final List<Mat> images = new ArrayList<>();
//        images.add(frame);
//
//        Mat hist = new Mat();
//        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0f, 256f));
//
//        double pixels = 0;
//        double hsum = 0.0;
//        final ArrayList<Double> prod = new ArrayList<>();
//        for (int i = 0; i < 256; i++) {
//            final double pix = hist.get(i, 0)[0]; //256x1x1 matrix
//            final double val = pix * i;
//            prod.add(val);
//            hsum = hsum + val;
//            pixels = pixels + pix;
//        }
//
//        int ave = (int) (hsum / pixels);
//        int thresh = 256;
//
//
//        while (thresh > ave) {
//            thresh = ave;
//            hsum = 0;
//            pixels = 0;
//            for (int i = 0; i < ave + 1; i++) {
//                hsum = hsum + prod.get(i);
//                pixels = pixels + hist.get(i, 0)[0];
//            }
//            int aveLow = (int) (hsum / pixels);
//            hsum = 0;
//            pixels = 0;
//            for (int i = ave + 1; i < 256; i++) {
//                hsum = hsum + prod.get(i);
//                pixels = pixels + hist.get(i, 0)[0];
//            }
//            int aveHigh = (int) (hsum / pixels);
//            ave = (int) ((aveLow + aveHigh) * 0.5 + 1);
//
//        }
//
//        Mat binMat = new Mat();
//        Imgproc.threshold(frame, binMat, thresh, 255, Imgproc.THRESH_BINARY_INV);
//        //Log.d("Frame info: ", String.format("width: %s x height: %s", frame.width(), frame.height()));
//
//        //long preRun = System.nanoTime();
//        // threshold BGRA values to help with miaze / corn / transparent seeds
//        //Mat gray = new Mat();
//        Imgproc.cvtColor(binMat, binMat, Imgproc.COLOR_RGB2GRAY);
//        //Core.inRange(frame, new Scalar(0,0,0), new Scalar(128, 128, 128), gray);
//        //Log.d("inRange:", String.valueOf(System.nanoTime() - preRun));
//
//        //long preMorph = System.nanoTime();
//        // classical pre-watershed processing
//        Mat opening = new Mat();
//        Imgproc.morphologyEx(binMat, opening, Imgproc.MORPH_OPEN, Mat.ones(new Size(3,3), CvType.CV_8UC1));
//        //Log.d("Morph:", String.valueOf(System.nanoTime() - preMorph));
//
//        //long dilate = System.nanoTime();
//        Mat sure_bg = new Mat();
//        Imgproc.dilate(opening, sure_bg, Mat.ones(new Size(3,3), CvType.CV_8UC1));
//        //Log.d("dilate:", String.valueOf(System.nanoTime() - dilate));
//
//        //long dstT = System.nanoTime();
//        Mat dt = new Mat();
//        Imgproc.distanceTransform(opening, dt, Imgproc.CV_DIST_L2, 3);
//        //Log.d("dst:", String.valueOf(System.nanoTime() - dstT));
//
//        //long thres = System.nanoTime();
//        Mat sure_fg = new Mat();
//        Imgproc.threshold(dt, sure_fg, 8, 255, 0);
//        //Log.d("thres:", String.valueOf(System.nanoTime() - thres));
//
//        sure_fg.convertTo(sure_fg, CvType.CV_8UC1);
//
//        Core.bitwise_not(sure_fg, sure_fg);
//
//
//        //free up memory from Mats not used
//        binMat.release();
//        sure_bg.release();
//        opening.release();
//        dt.release();
//
//        Mat hierarchy = new Mat();
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Imgproc.findContours(sure_fg, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
//
//        List<Double> areaArray = new ArrayList<>();
//        List<Double> perimeterArray = new ArrayList<>();
//        List<MatOfPoint> contourPoints = new ArrayList<>();
//
//        long startContourLoop = System.nanoTime();
//        /* iterate through all contours, check for image-sized bounding box and
//        any noise under 150 pixels (reasoning below) */
//        for (int i = 0; i < contours.size(); i++) {
//
//            MatOfPoint mp = contours.get(i);
//            double area = Imgproc.contourArea(mp);
//            double perimeter = Imgproc.arcLength(
//                    new MatOfPoint2f(mp.toArray()), true);
//
//            //the smallest seed we tested with at CPS lab was a red seed which had an minimum area of ~160 pixels
//            //with a resolution of 4032 x 3024 at a height of ~18cm
//            //before we calculate statistics, we use this as a metric to filter out anything smaller than a red seed
//            //TODO add database of seed types or coin types to check if the areas are smaller than a given coin, or
//            //TODO (cont) larger than the smallest seed.
//            if (area < 1000000 && area > 150) {
//
//                //we can check the parent hierarchy to try and skip inner contours
//                //where adjacent seeds may create fake contours, but this would skip all
//                //seeds that are completely surrounded by other seeds
//                //double parent = hierarchy.get(0, i)[3];
//                if (BuildConfig.DEBUG) {
//                    //Log.d("parent:", String.valueOf(parent));
//                }
//                //if (parent == -1 || parent == 0) {
//                contourPoints.add(mp);
//                areaArray.add(area);
//                perimeterArray.add(perimeter);
//                //}
//            }
//        }
//
//        hierarchy.release();
//
//        //long startEstCalc = System.nanoTime();
//        //get an estimated count based on cluster sizes
//        List<Integer> c = estimatedClusterCount(areaArray, perimeterArray);
//        //Log.d("EstRun:", String.valueOf(System.nanoTime() - startEstCalc));
//
//
//
//
//
//
//
//
//        long startDraw = System.nanoTime();
//        //draw green contours for count==1 seeds, red otherwise (noise and clusters)
//        for (int i = 0; i < contourPoints.size(); i++) {
//
//            int count = c.get(i);
//
//            MatOfPoint mp = contourPoints.get(i);
//
//            double p = Imgproc.arcLength(new MatOfPoint2f(mp.toArray()), true);
//            double a = Imgproc.contourArea(mp);
//
//            Moments M = Imgproc.moments(mp);
//            double x = (int) (M.get_m10() / M.get_m00());
//            double y = (int) (M.get_m01() / M.get_m00());
//
//            Imgproc.circle(annotatedFrame, new Point(x,y), 10, new Scalar(255,255,255));
//            //Imgproc.putText(frame, String.valueOf(i), new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 2.0, new Scalar(128, 128, 128), 3);
//
//            if (count == 1) {
//                Imgproc.drawContours(frame, contourPoints, i, new Scalar(0,255,0), 2);
//                //mContours.append(mContours.size(),
//                //      new ContourLocation(x, y, count));
//                //Imgproc.putText(frame, String.valueOf(count), new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 5.0, new Scalar(255, 255, 255), 3);
//
//            } else {
//                if (count > 3) {
//                    //Imgproc.line(frame, new Point(2000, 1500), new Point(x, y), new Scalar(0, 255, 0));
//                    Imgproc.putText(frame, String.valueOf(count), new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 5.0, new Scalar(255, 255, 255), 3);
//                }
//                Log.d("COUNT", i + ":" + count);
//                if (count > 1) {
//                    Imgproc.drawContours(frame, contourPoints, i, new Scalar(255, 0, 0), 2);
//                } else {
//                    Imgproc.drawContours(frame, contourPoints, i, new Scalar(255, 255, 0), 2);
//                }
//            }
//        }
//
//        mAnnotated = annotatedFrame;
//
//        contours.clear();
//        //Log.d("EstDrawRun:", String.valueOf(System.nanoTime() - startDraw));
//
//        /*
//        Now we have successfully counted just-one-seeds, and also clusters of two seeds.
//        Clusters of three and more seeds are 'trouble', this loop will attempt to estimate these
//        clusters by first calculating the just-ones-average which is an average area and perimeter
//        of ones-truth seeds. These variables will be used as thresholds to find clusters.
//         */
//        long startFinalCount = System.nanoTime();
//        List<Integer> inflCounts = new ArrayList<>();
//        double[] justOneThresh = justOnesAverage(areaArray, perimeterArray);
//        int n = contourPoints.size();
//        for (int i = 0; i < n; i++) {
//
//            MatOfPoint temp = contourPoints.get(i);
//
//            Moments M = Imgproc.moments(temp);
//            Rect br = Imgproc.boundingRect(temp);
//            //double cx = br.x+br.width/2;
//            //double cy = br.y+br.height/2;
//            double cx = (int) (M.get_m10() / M.get_m00());
//            double cy = (int) (M.get_m01() / M.get_m00());
//
//            double area = Imgproc.contourArea(temp);
//            double perimeter = Imgproc.arcLength(
//                    new MatOfPoint2f(temp.toArray()), true);
//
//            inflCounts.add(0);
//
//            //we know our 1-s and 2-s estimates are fine, so we threshold for anything
//            //bigger than 2.5 seeds.
//
//            if (area > justOneThresh[0] * alpha && perimeter > justOneThresh[1]) {
//
//                double clusterPerimeterEstimate = perimeter / justOneThresh[1];
//                //Log.d("cluster perimeter", String.valueOf(clusterPerimeterEstimate));
//
//                Point[] points = temp.toArray();
//
//                //To estimate the larger clusters we calculate the convex hull and use
//                //confexityDefects to populate an array of inflection points on the cluster.
//                //This is essentially calculating the cross products between arcs along the cluster,
//                //when an inflection point is found it is considered a defect./
//                MatOfInt hull = new MatOfInt();
//                MatOfInt4 convexityDefects = new MatOfInt4();
//                Imgproc.convexHull(temp, hull);
//                Imgproc.convexityDefects(temp, hull, convexityDefects);
//
//                List<Integer> inflectionPoints = convexityDefects.toList();
//
//
//                //the inflection points elements contains a start,end,far point, farthest distance approx
//                //we first start by calculating the distance from start and end. Intuitively, if you think about
//                //drawing a V, the start and end points are where you put the pen down and take the pen off.
//                //The inflection point is the middle point. We now threshold on the very distance between start and
//                //end, we calculate the distance using pythagorean's theorem where the adjacent and opposite edges are
//                //based on a fourth of the just-ones estimate of perimeter intuitively this
//                //is looking for very small (but not small enough for noise) gaps between seeds.
//
//                int inflectionCount = 0;
//                for (int j = 2 ; j < inflectionPoints.size() - 6; j += 4) {
//                    Point inflection = points[inflectionPoints.get(j)];
//                    Point x = points[inflectionPoints.get(j-2)];
//                    Point y = points[inflectionPoints.get(j-1)];
//                    //Log.d("approx dst to infl", String.valueOf(inflectionPoints.get(j+1)));
//                    double dst = Math.sqrt(Math.pow(y.x - x.x, 2) + Math.pow(y.y - x.y, 2)); //Core.norm(new Mat(x), new Mat(y));
//                    //Log.d("DST", String.valueOf(dst));
//                    //Log.d("THRESH", String.valueOf(0.2 * Math.sqrt(Math.pow(justOneThresh[1] / 4.0, 2) + Math.pow(justOneThresh[1] / 4.0, 2))));
//                    double adjOp = Math.pow(justOneThresh[1] / 4.0, 2);
//
//                    //threshold inflection point distance
//                    if (dst > beta * Math.sqrt(2*adjOp)) {
//                        Imgproc.circle(frame, inflection, 10, new Scalar(255, 255, 255));
//                        inflectionCount++;
//                    }
//                }
//
//                int count = (int) Math.round((c.get(i) + inflectionCount) / 3.0);// + clusterPerimeterEstimate) / 3.0);
//
//                //if the above estimate is not equal to our area estimate, we draw a blue contour around
//                //the cluster to show users which clusters may not be counted accurately.
//
//                if (inflectionCount != c.get(i)) {
//                    Imgproc.drawContours(frame, contourPoints, i, new Scalar(0, 0, 255), 3);
//                    Imgproc.putText(frame, String.valueOf(count),
//                            new Point(cx, cy), Core.FONT_HERSHEY_SIMPLEX, 7, new Scalar(255, 255, 255), 3);
//                    mContours.append(mContours.size(),
//                            new ContourLocation(cx, cy, count));
//                }
//
//                //otherwise we set a temporary count to the inflection count + the perimeter estimated count
//                //this will be averaged later.
//                inflCounts.set(i, count);
//            }
//        }
//        //Log.d("EstRun:", String.valueOf(System.nanoTime() - startFinalCount));
//
//        //the final count iteration loop
//        //if the contour is considered a cluster the estimation is formally:
//        //count = Sum ( (Min(area_estimate, perimeter_estimate) + infl_count + round(perimeterJustOnesCount)) / 3 )
//        //otherwise the count is simply Sum ( (Min(area_estimate, perimeter_estimate)) )
//        n = areaArray.size();
//        int ssum = 0;
//        for (int k = 0; k < n; k++) {
//            int count = c.get(k);
//            int infl = inflCounts.get(k);
//            if (infl > 0) ssum = ssum + infl;
//            else ssum = ssum + count;
//        }
//
//        mRuntime = System.nanoTime() - runTime;
//
//        Log.d("SEED OUTPUT COUNT", String.valueOf(ssum));
//        Log.d("actualRun: ", String.valueOf(mRuntime));
//        this.mNumSeeds = ssum;
//
//        return frame;
//
//    }
//
//    public Bitmap process(Bitmap inputBitmap, int alpha, double beta) {
//
//        mContours = new SparseArray<>();
//        Mat frame = new Mat();
//        Utils.bitmapToMat(inputBitmap, frame);
//        //Mat ret = new Mat();
//        //Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2HSV);
//        //Core.inRange(frame, new Scalar(0,0,0), new Scalar(255,255,128), ret);
//        //Imgproc.dilate(ret, ret, Mat.ones(new Size(5,5), CvType.CV_8UC1));
//
//
//        Mat ret = process(frame, alpha, beta);
//        Utils.matToBitmap(ret, inputBitmap);
//
//
//        //mLabels.convertTo(mLabels, CvType.CV_8UC3);
//        //Utils.matToBitmap(mLabels, inputBitmap);
//        return inputBitmap;
//    }
//
//    public long getRuntime() {return this.mRuntime; }
//    public double getSeedCount() { return this.mNumSeeds; }
//    public Mat getAnnotated() { return this.mAnnotated; }
//    public int getNumSeeds() {
//        return this.mNumSeeds;
//    }
//}
