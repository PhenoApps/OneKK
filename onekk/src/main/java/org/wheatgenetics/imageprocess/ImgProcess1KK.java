package org.wheatgenetics.imageprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class ImgProcess1KK {

    private String imageFile;
    private Mat image;
    private Mat image2;
    private Mat labels;
    private Rect roi;

    private Scalar filterGreen[] = {new Scalar(0, 0, 0), new Scalar(40, 255, 255)};
    private Scalar filterBlue[] = {new Scalar(100, 0, 0), new Scalar(179, 255, 255)};

    List<MatOfPoint> hullMop = new ArrayList<>();

    private List<MatOfPoint> refContours = new ArrayList<>();
    private List<MatOfPoint> goodContours = new ArrayList<>();
    private double refDiam = 1;

    private int seedCount = 0;

    private double pixelSize = 0; // pixel size in mm
    public boolean cropImage = true;

    private ArrayList<Seed> seedArray = new ArrayList<>();

    private static String OS = System.getProperty("os.name").toLowerCase();

    ArrayList<Double> seedMatL = new ArrayList<>(); // major axis
    ArrayList<Double> seedMatW = new ArrayList<>(); // minor axis
    ArrayList<Double> seedMatP = new ArrayList<>(); // perimeter
    ArrayList<Double> seedMatA = new ArrayList<>(); // area

    List<MatOfInt> hull = new ArrayList<>();

    public ImgProcess1KK(String inputFILE, double refDiameter, boolean crop, double minSize) {
        double start = System.currentTimeMillis();

        refDiam = refDiameter;
        imageFile = inputFILE;
        this.initialize();
        this.processImage();

        double time = (System.currentTimeMillis() - start) / 1000;
        System.out.println("\nProcessed in : " + time + " sec");
    }

    private void initialize() {
        checkOS();
        image = Highgui.imread(imageFile);
        System.out.println(String.format("Processing %s", imageFile));
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2HSV);
    }

    public void processImage() {
        if (cropImage) {
            this.cropImage();
        }

        if (this.setReference()) {
            this.countSeeds();
        }
    }

    /**
     * Check which operating system this is running on
     */
    private void checkOS() {
        if (isWindows() || isMac()) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load the native library.
        }
    }

    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMac() {
        return (OS.contains("mac"));
    }

    /**
     * Filters the image based on the green background
     * Crops image using largest observable contour
     * Removes extra 1.5% of image to ensure all edges removed
     */
    private void cropImage() {
        System.out.println("Cropping image...");

        Mat uncropped = filterBackground(image);

        if (Core.countNonZero(uncropped.row(1)) > uncropped.rows() * .6 &&
                Core.countNonZero(uncropped.col(1)) > uncropped.cols() * .6 &&
                Core.countNonZero(uncropped.row(uncropped.rows() - 1)) > uncropped.rows() * .6 &&
                Core.countNonZero(uncropped.col(uncropped.cols() - 1)) > uncropped.cols() * .6) {

            System.out.println("IMAGE DOES NOT NEED TO BE CROPPED" + "\n");
            return;
        }

        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(uncropped, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS);
        double largest_area = 0;
        int largest_index = 0;

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint cnt = new MatOfPoint(contours.get(i).toArray());
            double area = Imgproc.contourArea(cnt);
            if (area > largest_area) {
                largest_area = area;
                largest_index = i;
            }
        }

        Mat mask_image = new Mat(image.size(), CvType.CV_8U);

        Imgproc.drawContours(mask_image, contours, largest_index, new Scalar(255, 255, 255), Core.FILLED);

        image.copyTo(mask_image, mask_image);

        image = mask_image;

        MatOfPoint2f mat = new MatOfPoint2f(contours.get(largest_index).toArray());

        RotatedRect rect = Imgproc.minAreaRect(mat);
        Size rectSize = rect.size;

        System.out.println("PHOTO CROPPED FROM " + image.rows() + " x " + image.cols() + " TO " + Math.round(rectSize.height) + " x " + Math.round(rectSize.width) + "\n");
    }


    /**
     * Set reference circle diameter and pixel abs size
     * Image must be initialized and converted to HSV
     */
    private boolean setReference() {
        System.out.println("Setting references...");
        Mat imgBLUE = this.filterBlue(image);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(imgBLUE, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint tmp = new MatOfPoint(contours.get(i).toArray());
            MatOfPoint2f tmp2f = new MatOfPoint2f(contours.get(i).toArray());

            if (tmp.toArray().length > 10) {
                RotatedRect rec = Imgproc.minAreaRect(tmp2f);
                RotatedRect elp = Imgproc.fitEllipse(tmp2f);
                double circ = 4 * Math.PI * Imgproc.contourArea(tmp) / Math.pow(Imgproc.arcLength(tmp2f, true), 2); // calculate circularity
                double h = rec.boundingRect().height;
                double w = rec.boundingRect().width;
                double h2w = Math.max(h, w) / Math.min(h, w);
                if (circ > 0.85 & h2w < 1.1) {
                    refContours.add(tmp);
                }
            }
        }

        // find the average width and height // divide by reference circle size
        double sum = 0;
        for (int i = 0; i < refContours.size(); i++) {
            MatOfPoint2f ref2f = new MatOfPoint2f();
            refContours.get(i).convertTo(ref2f, CvType.CV_32FC2);
            RotatedRect rec = Imgproc.minAreaRect(ref2f);
            sum += rec.boundingRect().height + rec.boundingRect().width;
        }
        double avgRef = sum / refContours.size() / 2;
        pixelSize = refDiam / avgRef;  // TODO check if this is calculated correctly

        if (refContours.size() > 0) {
            System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
            System.out.println("REFERENCE PIXEL SIZE: " + pixelSize + "\n");

            return true;
        } else {
            System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
            System.out.println("REPOSITION CAMERA TO CAPTURE REFERENCE CIRCLES" + "\n");
            return false;
        }

    }

    //TODO minimum size option
    //TODO minimum number option

    /**
     * Count seeds.  Median contour size is used to estimate seed count.
     * Image must be initialized and converted to HSV
     */
    private void countSeeds() {
        System.out.println("Counting seeds...");

        /**
         * UW nonparametric approach
         */

        Mat imgNG = this.filterGreen(image);
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
            System.out.println("YOUR SAMPLE IS LIKELY HETEROGENOUS OR THE IMAGE IS NOISY");
            System.out.println("USING ALL OBJECTS INSTEAD");
        }


        // median counting approach

        Mat pixels = imgNG.clone();
        double seedSizeMedian = findMedian(contours);
        int i = Double.valueOf(seedSizeMedian).intValue();
        int medCount = Core.countNonZero(pixels) / i;
        System.out.println("NUMBER OF SEEDS COUNTED: " + medCount);
        seedCount = medCount;

        measureSeeds();
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
    private void measureSeeds() {
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

            if (findMin(dims) / findMax(dims) == 0.0 || area == 0.0 || findMax(dims) / findMin(dims) >= 10.0 || findMax(dims) <= 10 || findMin(dims) <= 10) {
                goodContours.remove(i);
                i = i - 1;
            }
        }

        convexHull();

        for (int i = 0; i < goodContours.size(); i++) {
            MatOfPoint2f tmp = new MatOfPoint2f(goodContours.get(i).toArray());
            MatOfPoint2f tmp2 = new MatOfPoint2f(hullMop.get(i).toArray());

            if (tmp.toArray().length > 10) {

                //Imgproc.drawContours(labels, goodContours, i,new Scalar(255, 255, 255), -1); // TODO add switch for analyzing color
                //roi = Imgproc.boundingRect(goodContours.get(i)); // TODO add switch for analyzing color

                Seed s = new Seed(tmp, tmp2);
                seedArray.add(s);
            }
        }

        System.out.println("NUMBER OF SEEDS MEASURED: " + seedArray.size());
    }


    /**
     * Filters input image for blue
     * Scalar reference in HSB color space.
     */
    private Mat filterBlue(Mat img) {
        //Threshold the image
        Mat imgFilter = img.clone();
        Core.inRange(img, filterBlue[0], filterBlue[1], imgFilter);
        return imgFilter;
    }

    /**
     * Filters input image for green
     * Scalar reference in HSB color space.
     */
    private Mat filterGreen(Mat img) {
        //Threshold the image
        Mat imageFilter = img.clone();
        Core.inRange(img, filterGreen[0], filterGreen[1], imageFilter);
        return imageFilter;
    }

    /**
     * Filters input image for the background
     * Scalar reference in HSB color space.
     */
    private Mat filterBackground(Mat img) {
        Scalar filterBackground[] = {new Scalar(40, 0, 0), new Scalar(85, 255, 255)};

        Mat imageFilter = img.clone();
        Core.inRange(img, filterBackground[0], filterBackground[1], imageFilter);
        return imageFilter;
    }


    /**
     * Sets the blue level filter for masking image.
     * Scalar reference in HSB color space.
     *
     * @param low  three value Scalar giving HSB values for lower filter threshold
     * @param high three value Scalar giving HSB values for upper filter threshold
     */
    public void setBlueFilter(Scalar low, Scalar high) {
        this.filterBlue[0] = low;
        this.filterBlue[1] = high;
    }

    /**
     * Sets the green level filter for masking image.
     * Scalar reference in HSB color space.
     *
     * @param low  three value Scalar giving HSB values for lower filter threshold
     * @param high three value Scalar giving HSB values for upper filter threshold
     */
    public void setGreenFilter(Scalar low, Scalar high) {
        this.filterGreen[0] = low;
        this.filterGreen[1] = high;
    }

    /**
     * Convex hull testing
     */
    public void convexHull() {
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

    public Mat getProcessedImg() {
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
        for (int i = 0; i < seedArray.size(); i++) {
            seeds.add(seedArray.get(i).perm);
        }

        Imgproc.drawContours(pImg, refContours, -1, white, 3);
        Imgproc.drawContours(pImg, hullMop, -1, white, 2); //From convex hull
        Imgproc.drawContours(pImg, seeds, -1, red, 2); //From contours

        for (int i = 0; i < seedArray.size(); i++) {
            Core.line(pImg, seedArray.get(i).ptsW[0], seedArray.get(i).ptsW[1], blue, 2); // width
            Core.line(pImg, seedArray.get(i).ptsL[0], seedArray.get(i).ptsL[1], green, 2); // length
            Core.circle(pImg, seedArray.get(i).centgrav, 2, white, 2); // center gravity
        }

        return pImg;
    }

    public int getSeedCount() {
        return seedCount;
    }

    /**
     * Writes a false color image with the reference and seeds colored to the specified image path
     */

    public void writeProcessedImg(String filename) {
        System.out.println(String.format("\nWriting %s", filename));
        Highgui.imwrite(filename, this.getProcessedImg());
    }

    public class Seed {
        private double length;
        private double width;
        private double circ;
        private double area;
        private double perimeter;
        private Scalar color;
        private double lwr; // length to width ratio
        private Point centgrav = new Point(); // center of gravity
        private Point intLW = new Point(); //intersection of lenght and width vector
        private double ds; // distance between centgrav and intLW;
        private double tolerance = 0.2;
        private boolean isCanonical = false;

        // TODO add factors for expected size and filter accordingly
        private MatOfPoint2f seedMat = new MatOfPoint2f();
        private MatOfPoint perm = new MatOfPoint();
        private MatOfPoint perm2 = new MatOfPoint();
        private RotatedRect rec = new RotatedRect();
        private RotatedRect elp = new RotatedRect();
        private Point[] ptsL = new Point[2];
        private Point[] ptsW = new Point[2];

        /**
         * Getter classes
         */
        public double getLength() {
            return length * pixelSize;
        }

        public double getWidth() {
            return width * pixelSize;
        }

        public double getCirc() {
            return circ * pixelSize;
        }

        public double getArea() {
            return area * pixelSize;
        }

        public double getPerim() {
            return perimeter * pixelSize;
        }

        public Scalar getColor() {
            return color;
        }

        /**
         * Class to hold seed matrix array and descriptors
         */
        public Seed(MatOfPoint2f mat, MatOfPoint2f matHull) {
            seedMat = mat;

            mat.convertTo(perm, CvType.CV_32S);
            matHull.convertTo(perm2, CvType.CV_32S);

            rec = Imgproc.minAreaRect(mat);
            elp = Imgproc.fitEllipse(mat);
            circ = 4 * Math.PI * Imgproc.contourArea(mat) / Math.pow(Imgproc.arcLength(mat, true), 2); // calculate circularity
            area = Imgproc.contourArea(mat);
            perimeter = Imgproc.arcLength(mat, true);
            //color = Core.mean(image2.submat(roi),labels.submat(roi)); // TODO add switch for analyzing color

            this.calcCG();
            this.findMaxVec();
            //this.findIS();
        }

        /**
         * Find the center of gravity by averaging all points in the perimeter
         * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
         */
        private void calcCG() {
            double sumX = 0;
            double sumY = 0;
            Point[] permArray = perm2.toArray();
            for (Point aPermArray : permArray) {
                sumX += aPermArray.x;
                sumY += aPermArray.y;

            }
            centgrav.x = Math.round(sumX / permArray.length);
            centgrav.y = Math.round(sumY / permArray.length);
        }

        /**
         * Find the end-point coordinates of the maxium vector in the seed
         * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
         */
        private void findMaxVec() {
            Point[] permArray = perm2.toArray();

            for (int i = 0; i < permArray.length; i++) {
                for (int j = i; j < permArray.length; j++) {
                    Point p1 = permArray[i];
                    Point p2 = permArray[j];
                    double l = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
                    if (l > length) {
                        length = l;
                        ptsL[0] = p1;
                        ptsL[1] = p2;
                    }
                }
            }

            permArray = perm.toArray();

            double slopeL = ((ptsL[1].y - ptsL[0].y) / (ptsL[1].x - ptsL[0].x));  // TODO not sure this works for infinite slope

            //TODO use perm2 to calculate width

            for (Point aPermArray : permArray) {
                double d = 1;
                Point p2 = aPermArray;
                for (Point aPermArray1 : permArray) {
                    double s = slopeL * ((aPermArray.y - aPermArray1.y) / (aPermArray.x - aPermArray1.x));
                    if (Math.abs(s + 1) < d) {
                        d = Math.abs(s + 1);
                        p2 = aPermArray1;
                    }
                }

                double w = Math.sqrt(Math.pow(aPermArray.x - p2.x, 2) + Math.pow(aPermArray.y - p2.y, 2));
                if (w > width) {
                    width = w;
                    ptsW[0] = aPermArray;
                    ptsW[1] = p2;
                }
            }

            lwr = length / width;
        }

        /**
         * Find the intersection of max length and max width vector
         * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
         */
        private void findIS() {
            if (ptsW[0] == null) {
                return;
            }
            ; // exit method if width is null
            double S1 = 0.5 * ((ptsW[0].x - ptsW[1].x) * (ptsL[0].y - ptsW[1].y) - (ptsW[0].y - ptsW[1].y) * (ptsL[0].x - ptsW[1].x));
            double S2 = 0.5 * ((ptsW[0].x - ptsW[1].x) * (ptsW[1].y - ptsL[1].y) - (ptsW[0].y - ptsW[1].y) * (ptsW[1].x - ptsL[1].x));
            intLW.x = ptsL[0].x + S1 * (ptsL[1].x - ptsL[0].x) / (S1 + S2);
            intLW.y = ptsL[0].y + S1 * (ptsL[1].y - ptsL[0].y) / (S1 + S2);

            ds = Math.sqrt(Math.pow(intLW.x - centgrav.x, 2) + Math.pow(intLW.y - centgrav.y, 2));

        }

        /**
         * Returns the maximum vector length
         * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
         *
         * @return double giving the maximum vector length
         */
        private double getMaxVec() {
            return Math.sqrt(Math.pow(ptsL[0].x - ptsL[1].x, 2) + Math.pow(ptsL[0].y - ptsL[1].y, 2));
        }

        /**
         * Runs multiple checks to determine if the shape blob represents a canonical seed shape
         */
        private boolean checkCanonical() {
            double minSize = 30;
            return this.length >= minSize && this.checkCirc() && this.checkElp();
        }

        /**
         * Checks and expected circularity value is within the expected circularity range
         */
        private boolean checkCirc() {
            double minCirc = 0.6;
            return minCirc < circ;
        }

        /**
         * Checks the object is roughly an eliptical shape.  Will filter blobs of two or more seeds
         * Not sure this is working correctly due to approximation formula for circumference of ellipse.
         */
        private boolean checkElp() {
            double a = elp.boundingRect().height / 2;
            double b = elp.boundingRect().width / 2;
            double c = 2 * Math.PI * Math.sqrt((Math.pow(a, 2) + Math.pow(b, 2)) / 2); // TODO this is the approximation formula for circumference of an ellipse - FIGURE OUT IF IT IS WORKING
            double p = Imgproc.arcLength(seedMat, true);

            return p < 1.1 * c;
        }


        /**
         * Checks and expected length to width ratio is within a tolerance limit // DEFAULT SET TO 30%
         */
        private boolean checkL2W() {
            double expLWR = 1.2;
            return lwr > expLWR * (1 - tolerance) & lwr < expLWR * (1 + tolerance);
        }

        public void setTolerance(double t) {
            tolerance = t;
        }
    }

    public ArrayList<Seed> getList() {
        return seedArray;
    }
}
