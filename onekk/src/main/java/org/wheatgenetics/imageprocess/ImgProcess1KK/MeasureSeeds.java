package org.wheatgenetics.imageprocess.ImgProcess1KK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.wheatgenetics.imageprocess.Seed.RawSeed;

public class MeasureSeeds {

    private Mat image;
    List<MatOfPoint> hullMop = new ArrayList<>();

    private List<MatOfPoint> refContours = new ArrayList<>();
    private List<MatOfPoint> goodContours = new ArrayList<>();

    private int seedCount = 0;


    private double pixelSize = 0; // pixel size in mm
    private double minimumSize = 0.0;
    private double maximumSize = 0.0;

    private ArrayList<RawSeed> rawSeedArray = new ArrayList<>();

    ArrayList<Double> seedMatL = new ArrayList<>(); // major axis
    ArrayList<Double> seedMatW = new ArrayList<>(); // minor axis
    ArrayList<Double> seedMatP = new ArrayList<>(); // perimeter
    ArrayList<Double> seedMatA = new ArrayList<>(); // area

    List<MatOfInt> hull = null;

    public MeasureSeeds(){}

    public MeasureSeeds(Mat image, double minSize, double maxSize, List<MatOfPoint> ref) {
        this.image = image;
        minimumSize = minSize;
        maximumSize = maxSize;
        refContours = ref;
    }

    //TODO minimum number option

    /**
     * Count seeds.  Median contour size is used to estimate seed count.
     * Image must be initialized and converted to HSV
     */
    public void identifySingleSeeds(double pixelSize) {
        System.out.println("Counting seeds...");

        /**
         * UW nonparametric approach
         */

        //Mat imgNG = imageProcess.filterGreen(image);
        Mat imgNG = image;
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgNG, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE, new Point(0, 0));

        if (contours.size() >= 1500) {
            System.out.println("Too many objects detected. Use fewer seeds or check background colors");
            System.out.println(contours.size());
            return;
        }
        System.out.println("CONTOURS: " + contours.size());

        double[] dims = new double[2];

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f tmp = new MatOfPoint2f(contours.get(i).toArray());
            RotatedRect test = Imgproc.minAreaRect(tmp);
            Size testsz = test.size;
            dims[0] = testsz.width;
            dims[1] = testsz.height;

            double area = Imgproc.contourArea(contours.get(i));

            if (findMin(dims) / findMax(dims) == 0.0 || area == 0.0 || findMax(dims) / findMin(dims) >= 10.0 || findMax(dims) <= 10 || findMin(dims) <= 10) {
                contours.remove(i);
                i = i - 1;
                continue;
            }

            seedMatL.add(findMax(dims));
            seedMatW.add(findMin(dims));
            seedMatA.add(area);
            seedMatP.add(Imgproc.arcLength(tmp, true));
        }

        //System.out.println(seedMatW.size());
        //store all measurements in single variable
        double[][] seedMatValues = new double[4][seedMatW.size()];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < seedMatW.size(); j++) {
                if (i == 0) {
                    seedMatValues[i][j] = seedMatL.get(j);
                }
                if (i == 1) {
                    seedMatValues[i][j] = seedMatW.get(j);
                }
                if (i == 2) {
                    seedMatValues[i][j] = seedMatA.get(j);
                }
                if (i == 3) {
                    seedMatValues[i][j] = seedMatP.get(j);
                }
            }
        }

        // Divide each element by the other element

        double[][][] seedMatCalc = new double[4][seedMatW.size()][seedMatW.size()];

        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < seedMatW.size(); i++) {
                for (int j = 0; j < seedMatW.size(); j++) {
                    seedMatCalc[k][i][j] = Math.round(seedMatValues[k][j] / seedMatValues[k][i]);
                }
            }
        }

        // Count number of 1s

        int[][] seedMatCalcMax = new int[4][seedMatW.size()];
        int count = 0;

        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < seedMatW.size(); i++) {
                for (int j = 0; j < seedMatW.size(); j++) {
                    if (seedMatCalc[k][j][i] == 1) {
                        count++;
                    }
                }

                seedMatCalcMax[k][i] = count;
                count = 0;
            }
        }

        // find mode for true seed count
        int[] trueSeedCount = new int[4];
        for (int i = 0; i < 4; i++) {
            trueSeedCount[i] = findMode(seedMatCalcMax[i]);
            //System.out.println(trueSeedCount[i]);
        }

        double seedSize = 0;
        double[] avgSeedSize = new double[4];
        int[] numClusters = new int[4];

        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < seedMatW.size(); i++) {
                if (seedMatCalcMax[j][i] == trueSeedCount[j]) {
                    seedSize = seedSize + seedMatValues[j][i];
                    numClusters[j]++;
                }
            }
            avgSeedSize[j] = seedSize;
            //System.out.println(avgSeedSize[j]);
            seedSize = 0;
        }

        for (int i = 0; i < 4; i++) {
            avgSeedSize[i] = avgSeedSize[i] / numClusters[i];
            //System.out.println(avgSeedSize[i]);
        }


        // Count number of seeds using avg seed size
        double[][] seedMatCount = new double[4][seedMatW.size()];

        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < seedMatW.size(); i++) {
                seedMatCount[j][i] = Math.round(seedMatValues[j][i] / avgSeedSize[j]);
                //System.out.println(seedMatCount[j][i]);
            }
            //System.out.println("break");
        }

        // identify all contours that only go to one for all four characteristics
        for (int i = 0; i < contours.size(); i++) {
            if (seedMatCount[0][i] == seedMatCount[1][i] && seedMatCount[1][i] == seedMatCount[2][i] && seedMatCount[2][i] == seedMatCount[3][i] && seedMatCount[0][i] == 1) {
                goodContours.add(contours.get(i));
            }
        }

        if (goodContours.size() <= 12) {
            goodContours = contours;
            System.out.println("\n" + "UNABLE TO IDENTIFY FIVE INDIVIDUAL OBJECTS");
            System.out.println("YOUR SAMPLE IS VERY HETEROGENOUS OR THE IMAGE IS TOO NOISY");
            System.out.println("USING ALL OBJECTS INSTEAD");
        }

        // median counting approach
        Mat pixels = imgNG.clone();
        double seedSizeMedian = findMedian(contours);
        int i = Double.valueOf(seedSizeMedian).intValue();
        int medCount = Core.countNonZero(pixels) / i;
        System.out.println("NUMBER OF SEEDS COUNTED: " + medCount);
        seedCount = medCount;

        measureSeeds(goodContours,pixelSize);
    }

    private double findMax(double[] values) {
        double maxValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > maxValue) {
                maxValue = values[i];
            }
        }
        return maxValue;
    }

    private double findMin(double[] values) {
        double minValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < minValue) {
                minValue = values[i];
            }
        }
        return minValue;
    }

    private int findMode(int a[]) {
        int maxValue = 0;
        int maxCount = 0;

        for (int anA : a) {
            int count = 0;
            for (int anA1 : a) {
                if (anA1 == anA) ++count;
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = anA;
            }
        }
        return maxValue;
    }

    private double findMedian(List<MatOfPoint> contours) {
        double[] contSize = new double[contours.size()];

        if (!contours.isEmpty()) {
            for (int i = 0; i < contours.size(); i++) {
                contSize[i] = Imgproc.contourArea(contours.get(i));
            }
        }

        Arrays.sort(contSize);

        double median;
        if (contSize.length % 2 == 0) {
            median = (contSize[contSize.length / 2] + contSize[contSize.length / 2 - 1]) / 2;
        } else {
            median = contSize[contSize.length / 2];
        }

        return median;
    }

    /**
     * Find seeds based on color filter and expected shape
     * Image must be initialized and converted to HSV
     */
    public void measureSeeds(List<MatOfPoint> goodContours, double pixelSize) {
        System.out.println("\n" + "Measuring seeds...");

        if (goodContours.size() >= 1500) {
            System.out.println("Too many objects detected. Use fewer seeds or check background colors");
            return;
        }

        // additional contour clean up
        for (int i = 0; i < goodContours.size(); i++) {
            MatOfPoint2f tmp = new MatOfPoint2f(goodContours.get(i).toArray());

            double[] dims = new double[2];
            RotatedRect test = Imgproc.minAreaRect(tmp);
            Size testsz = test.size;
            dims[0] = testsz.width;
            dims[1] = testsz.height;
            double area = Imgproc.contourArea(goodContours.get(i));

            //TODO check these
            if (findMin(dims) / findMax(dims) == 0.0 || area == 0.0 || findMax(dims) / findMin(dims) >= 10.0 || findMax(dims) <= 10 || findMin(dims) <= 10) {
                goodContours.remove(i);
                i = i - 1;
            }
        }

        convexHull(goodContours);

        for (int i = 0; i < goodContours.size(); i++) {
            MatOfPoint2f tmp = new MatOfPoint2f(goodContours.get(i).toArray());
            MatOfPoint2f tmp2 = new MatOfPoint2f(hullMop.get(i).toArray());

            if (tmp.toArray().length > 10) {

                //Imgproc.drawContours(labels, goodContours, i,new Scalar(255, 255, 255), -1); // TODO add switch for analyzing color
                //roi = Imgproc.boundingRect(goodContours.get(i)); // TODO add switch for analyzing color

                RawSeed s = new RawSeed(tmp, tmp2);
                s.setSeedPixelSize(pixelSize);
                //TODO check this
                //Size filtering - add seed if minimum or maximum are set and seed length falls within bounds
                if((minimumSize!=0.0 && s.getLength()>=minimumSize*pixelSize)||(maximumSize!=0.0 && s.getLength()<=maximumSize*pixelSize)) {
                    rawSeedArray.add(s);
                }

                // Add if size parameters not set
                if((minimumSize==0.0)||(maximumSize==0.0)){
                    rawSeedArray.add(s);
                }
            }
        }

        System.out.println("NUMBER OF SEEDS MEASURED: " + rawSeedArray.size());
    }

    /**
     * Convex hull testing
     */
    private void convexHull(List<MatOfPoint> goodContours) {
        hull = new ArrayList<>();
        for (int i = 0; i < goodContours.size(); i++) {
            hull.add(new MatOfInt());
            Imgproc.convexHull(goodContours.get(i), hull.get(i));
        }

        List<Point[]> hullPoints = new ArrayList<>();

        for (int i = 0; i < hull.size(); i++) {
            Point[] points = new Point[hull.get(i).rows()];

            // Loop over all points that need to be hulled in current contour
            for (int j = 0; j < hull.get(i).rows(); j++) {
                int index = (int) hull.get(i).get(j, 0)[0];
                points[j] = new Point(goodContours.get(i).get(index, 0)[0], goodContours.get(i).get(index, 0)[1]);
            }

            hullPoints.add(points);
        }

        for (int i = 0; i < hullPoints.size(); i++) {
            MatOfPoint mop = new MatOfPoint();
            mop.fromArray(hullPoints.get(i));
            hullMop.add(mop);
        }
    }

    /**
     * Returns a false color image with the reference and seeds colored
     */
    public Mat getProcessedMat() {

        Mat pImg = image.clone();
        Imgproc.cvtColor(pImg, pImg, Imgproc.COLOR_HSV2BGR);

        Scalar blue = new Scalar(255, 0, 0);
        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(50, 0, 255);
        Scalar white = new Scalar(255, 255, 255);
        Scalar black = new Scalar(0, 0, 0);
        Scalar purple = new Scalar(255, 0, 155);
        Scalar orange = new Scalar(0, 50, 255);

        List<MatOfPoint> seeds = new ArrayList<>();
        for (int i = 0; i < rawSeedArray.size(); i++) {
            seeds.add(rawSeedArray.get(i).getPerm());
        }

        Imgproc.drawContours(pImg, refContours, -1, white, 3);
        Imgproc.drawContours(pImg, hullMop, -1, white, 2); //From convex hull
        Imgproc.drawContours(pImg, seeds, -1, red, 2); //From contours

        for (int i = 0; i < rawSeedArray.size(); i++) {
            Imgproc.line(pImg, rawSeedArray.get(i).getPtsW()[0], rawSeedArray.get(i).getPtsW()[1], blue, 2); // width
            Imgproc.line(pImg, rawSeedArray.get(i).getPtsL()[0], rawSeedArray.get(i).getPtsL()[1], green, 2); // length
            Imgproc.circle(pImg, rawSeedArray.get(i).getCentgrav(), 2, white, 2); // center gravity
            Imgproc.putText(pImg,String.valueOf(i+1), rawSeedArray.get(i).getCentgrav(),Core.FONT_HERSHEY_PLAIN,2,black,2);
        }

        return pImg;
    }

    public int getSeedCount() {
        return seedCount;
    }

    public ArrayList<RawSeed> getList() {
        return rawSeedArray;
    }
}
