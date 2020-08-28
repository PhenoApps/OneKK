//package org.wheatgenetics.imageprocess;
//
//import org.opencv.core.Core;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Rect;
//import org.opencv.core.RotatedRect;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;
//import org.wheatgenetics.utils.Constants;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by sid on 2/22/18.
// */
//
//public class ImageProcess {
//
//    private String imageFile;
//    private Mat image;
//    private Mat image2;
//    private Mat labels;
//    private Rect roi;
//
//    private MeasureSeeds measureSeeds;
//    private List<MatOfPoint> refContours = new ArrayList<>();
//    private static String OS = System.getProperty("os.name").toLowerCase();
//    private Scalar filterGreen[] = {new Scalar(0, 0, 0), new Scalar(40, 255, 255)};
//    private Scalar filterBlue[] = {new Scalar(100, 0, 0), new Scalar(179, 255, 255)};
//    private double pixelSize = 0; // pixel size in mm
//    private boolean cropImage = false;
//    private double refDiam = 1;
//
//    public ImageProcess(String inputFILE, String photoName, double refDiameter, boolean crop, double minSize, double maxSize) {
//
//        double start = System.currentTimeMillis();
//
//        refDiam = refDiameter;
//        imageFile = inputFILE;
//
//        cropImage = crop;
//
//        this.initialize();
//        this.processImage(minSize, maxSize);
//
//        double time = (System.currentTimeMillis() - start) / 1000;
//        System.out.println("\nProcessed in : " + time + " sec");
//
//        Imgproc.cvtColor(image, image, Imgproc.COLOR_HSV2BGR);
//
//        Imgcodecs.imwrite(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg", image);
//    }
//
//    private void initialize() {
//        image = Imgcodecs.imread(imageFile);
//        System.out.println(String.format("Processing %s", imageFile));
//        //HSV: Hue, Saturation, value
//        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2HSV);
//    }
//
//    private void processImage(double min, double max) {
//
//        if (cropImage) {
//            //crop image to the minimum bounding box of 4 coins
//            image = cropImage(image);
//        }
//
//        Mat imageNG = filterGreen(image);
//        measureSeeds = new MeasureSeeds(imageNG, min, max, getRefContours());
//
//        boolean setRefs = setReference(image);
//
//        if (setRefs) {
//            measureSeeds.identifySingleSeeds(pixelSize);
//        }
//    }
//
//    /**
//     * Filters the image based on the green background
//     * Crops image using largest observable contour
//     * Removes extra 1.5% of image to ensure all edges removed
//     */
//    public Mat cropImage(Mat image) {
//        System.out.println("Cropping image...");
//
//        Mat uncropped = filterBackground(image);
//
//        if (Core.countNonZero(uncropped.row(1)) > uncropped.rows() * .6 &&
//                Core.countNonZero(uncropped.col(1)) > uncropped.cols() * .6 &&
//                Core.countNonZero(uncropped.row(uncropped.rows() - 1)) > uncropped.rows() * .6 &&
//                Core.countNonZero(uncropped.col(uncropped.cols() - 1)) > uncropped.cols() * .6) {
//
//            System.out.println("IMAGE DOES NOT NEED TO BE CROPPED" + "\n");
//            return image;
//        }
//
//        Mat hierarchy = new Mat();
//        List<MatOfPoint> contours = new ArrayList<>();
//
//        Imgproc.findContours(uncropped, contours, hierarchy,
//                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS);
//        double largest_area = 0;
//        int largest_index = 0;
//
//        for (int i = 0; i < contours.size(); i++) {
//            MatOfPoint cnt = new MatOfPoint(contours.get(i).toArray());
//            double area = Imgproc.contourArea(cnt);
//            if (area > largest_area) {
//                largest_area = area;
//                largest_index = i;
//            }
//        }
//
//        Mat mask_image = new Mat(image.size(), CvType.CV_8U);
//
//        Imgproc.drawContours(mask_image, contours, largest_index, new Scalar(255, 255, 255), Core.FILLED);
//
//        image.copyTo(mask_image, mask_image);
//
//        image = mask_image;
//
//        MatOfPoint2f mat = new MatOfPoint2f(contours.get(largest_index).toArray());
//
//        RotatedRect rect = Imgproc.minAreaRect(mat);
//        Size rectSize = rect.size;
//
//        System.out.println("PHOTO CROPPED FROM " + image.rows() + " x " + image.cols() + " TO " + Math.round(rectSize.height) + " x " + Math.round(rectSize.width) + "\n");
//
//        return image;
//    }
//
//    /**
//     * Set reference circle diameter and pixel abs size
//     * Image must be initialized and converted to HSV
//     */
//    public boolean setReference(Mat image) {
//        System.out.println("Setting references...");
//        Mat imgBLUE = filterBlue(image);
//        Mat hierarchy = new Mat();
//        List<MatOfPoint> contours = new ArrayList<>();
//        Imgproc.findContours(imgBLUE, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
//
//        for (int i = 0; i < contours.size(); i++) {
//            MatOfPoint tmp = new MatOfPoint(contours.get(i).toArray());
//            MatOfPoint2f tmp2f = new MatOfPoint2f(contours.get(i).toArray());
//
//            if (tmp.toArray().length > 10) {
//                RotatedRect rec = Imgproc.minAreaRect(tmp2f);
//                RotatedRect elp = Imgproc.fitEllipse(tmp2f);
//                double circ = 4 * Math.PI * Imgproc.contourArea(tmp) / Math.pow(Imgproc.arcLength(tmp2f, true), 2); // calculate circularity
//                double h = rec.boundingRect().height;
//                double w = rec.boundingRect().width;
//                double h2w = Math.max(h, w) / Math.min(h, w);
//                if (circ > 0.85 & h2w < 1.1) {
//                    refContours.add(tmp);
//                }
//            }
//        }
//
//        // find the average width and height // divide by reference circle size
//        double sum = 0;
//        for (int i = 0; i < refContours.size(); i++) {
//            MatOfPoint2f ref2f = new MatOfPoint2f();
//            refContours.get(i).convertTo(ref2f, CvType.CV_32FC2);
//            RotatedRect rec = Imgproc.minAreaRect(ref2f);
//            sum += rec.boundingRect().height + rec.boundingRect().width;
//        }
//        double avgRef = sum / refContours.size() / 2;
//        pixelSize = refDiam / avgRef;  // TODO check if this is calculated correctly
//
//        if (refContours.size() > 0) {
//            System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
//            System.out.println("REFERENCE PIXEL SIZE: " + pixelSize + "\n");
//
//            return true;
//        } else {
//            System.out.println("MEASURED " + refContours.size() + " REFERENCE CIRCLES");
//            System.out.println("REPOSITION CAMERA TO CAPTURE REFERENCE CIRCLES" + "\n");
//            return false;
//        }
//    }
//
//    /**
//     * Filters input image for blue
//     * Scalar reference in HSB color space.
//     */
//    public Mat filterBlue(Mat img) {
//        //Threshold the image
//        Mat imgFilter = img.clone();
//        Core.inRange(img, filterBlue[0], filterBlue[1], imgFilter);
//        return imgFilter;
//    }
//
//    /**
//     * Filters input image for green
//     * Scalar reference in HSB color space.
//     */
//    public Mat filterGreen(Mat img) {
//        //Threshold the image
//        Mat imageFilter = img.clone();
//        Core.inRange(img, filterGreen[0], filterGreen[1], imageFilter);
//        return imageFilter;
//    }
//
//    /**
//     * Filters input image for the background
//     * Scalar reference in HSB color space.
//     */
//    public Mat filterBackground(Mat img) {
//        Scalar filterBackground[] = {new Scalar(40, 0, 0), new Scalar(85, 255, 255)};
//
//        Mat imageFilter = img.clone();
//        Core.inRange(img, filterBackground[0], filterBackground[1], imageFilter);
//        return imageFilter;
//    }
//
//    public List<MatOfPoint> getRefContours() {
//        return refContours;
//    }
//
//    public double getPixelSize() {
//        return pixelSize;
//    }
//
//    public double getRefDiam() {
//        return refDiam;
//    }
//
//    /**
//     * Sets the blue level filter for masking image.
//     * Scalar reference in HSB color space.
//     *
//     * @param low  three value Scalar giving HSB values for lower filter threshold
//     * @param high three value Scalar giving HSB values for upper filter threshold
//     */
//    public void setBlueFilter(Scalar low, Scalar high) {
//        this.filterBlue[0] = low;
//        this.filterBlue[1] = high;
//    }
//
//    /**
//     * Sets the green level filter for masking image.
//     * Scalar reference in HSB color space.
//     *
//     * @param low  three value Scalar giving HSB values for lower filter threshold
//     * @param high three value Scalar giving HSB values for upper filter threshold
//     */
//    public void setGreenFilter(Scalar low, Scalar high) {
//        this.filterGreen[0] = low;
//        this.filterGreen[1] = high;
//    }
//
//    public int getSeedCount() {
//        return measureSeeds.getSeedCount();
//    }
//
//    public ArrayList<RawSeed> getSeedList() {
//        return measureSeeds.getList();
//    }
//
//}
