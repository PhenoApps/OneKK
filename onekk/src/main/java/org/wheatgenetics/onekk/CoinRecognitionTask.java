package org.wheatgenetics.onekk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TimingLogger;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.wheatgenetics.onekkUtils.Constants;
import org.wheatgenetics.ui.guideBox;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by sid on 3/6/18.
 */

public class CoinRecognitionTask /*extends AsyncTask<byte[],AsyncTask.Status,ArrayList<Point>>*/ {

    private static double COIN_CIRCULARITY = 0.95;
    private static double COIN_SIZE_THRESHOLD = 2.5;

    /* declared the ArrayList as android Point for ease of use to display them on the preview
    *
    *  centroidArrayList is the array list consisting of the centroids of the coins
    *  cornerArrayList is the array list consisting of the calculated corners for cropping
    */

    private ArrayList<Coin> coinArrayList;
    private ArrayList<Point> centroidArrayList;
    private ArrayList<Point> cornerArrayList;
    private ArrayList<Double> radiusArrayList;
    private String STATUS = "";
    private ArrayList<Point> coinCoordsList;
    private int[] textSize = new int[1];
    private org.opencv.core.Rect boundingBox = null;
    private Mat processedMat = null;
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private int previousSize = 0;
    private double coinSize = 0;
    private guideBox gb = null;
    private TimingLogger timingLogger = null;

    /** The following four methods are only used in case of real time coin recognition
     *  else these four methods can be removed and the coin recognition class need not
     *  extend the AsyncTask
     * */
    /* ================== REAL TIME COIN RECOGNITION - START =========================*/

    /* used in static processing *//*
    public CoinRecognitionTask(int cameraWidth, int cameraHeight){
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
    }

    *//* used in real time processing *//*
    public CoinRecognitionTask(int cameraWidth, int cameraHeight, guideBox gb){
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.gb = gb;
    }

    @Override
    protected ArrayList<Point> doInBackground(byte[]... bytes) {
        Log.d("Background processing","Started");
        byte[] data = bytes[0];
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, cameraWidth, cameraHeight, null);
        Rect rectangle = new Rect(0, 0, cameraWidth, cameraHeight);

        Log.d("Background processing","Creating bitmap");
        ByteArrayOutputStream output_stream = new ByteArrayOutputStream();

        Log.d("Background processing","byte stream created");
        yuvImage.compressToJpeg(rectangle, 100, output_stream);

        Log.d("Background processing","compressed");
        Bitmap bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size()), cameraWidth , cameraHeight, true);

        Log.d("Background processing","converting bitmap to mat");
        Mat m = new Mat();
        Utils.bitmapToMat(bmp,m);
        //Core.flip(m.t(),m,1);

        Log.d("Background processing","Starting hueProcess");
        //coinCoordsList = hueProcess(m);
        //Log.d("Centroid Coordinates",coinCoordsList.toString());

        cornerArrayList = coinCoordsList;
        return coinCoordsList;
    }

    @Override
    protected void onPostExecute(ArrayList<Point> coinCoordsList){
        if(coinCoordsList.size() != previousSize && gb != null){
            gb.setImageWidth(cameraWidth);
            gb.setImageHeight(cameraHeight);
            //gb.setCoinCoordsList(coinCoordsList);
            gb.invalidate();
        }

        previousSize = coinCoordsList.size();

    }*/

    /* ===================== REAL TIME COIN RECOGNITION - END =========================*/

    /* default constructor */
    public CoinRecognitionTask(double coinSize){
        this.coinSize = coinSize;
        timingLogger = new TimingLogger("CoreProcessing","Coin Recognition");
    }

    /** The function starts the coin recognition hueProcess.
     *
     * @param initialMat mat of the image captured or frame mat from the camera preview
     *
     */
    public void process(Mat initialMat) {
        radiusArrayList = new ArrayList<>();
        centroidArrayList = new ArrayList<>();
        STATUS = "";
        processedMat = initialMat;
        // Check if image is loaded fine
        if (initialMat.empty()) {
            Log.e("Coin recognition", "Empty image");
        }

        Mat gray = new Mat();
        Mat hierarchy = new Mat();
        Imgproc.cvtColor(initialMat, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(gray, gray, 5);
        Mat circles = new Mat();
        int radius;

        /* minDist = (double) gray.rows() / 8
         * '8' - change this value to detect circles with different distances to each other
         * change min_radius & max_radius to detect larger circles
         */

        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                (double) gray.rows() / 8,
                100.0, 30.0, 150, 300);

        timingLogger.addSplit("Process");

        Scalar maskColor = maskColor(initialMat);

        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            radius = (int) Math.round(c[2]);

            /* get the background color using maskColor method and mask the coins */
            Imgproc.circle(processedMat, center, radius + 20, maskColor, -1, 8, 0);

            radiusArrayList.add(c[2]);
            centroidArrayList.add(center);
        }
        Imgcodecs.imwrite(Constants.PHOTO_PATH + "/houghCoinRecog.jpg",processedMat);
    }

    /** used to perform various checks on the detected coins
     *  1. checks if only 4 coins are detected
     *  2. triggers the circularity check
     *  3. triggers the pixel size check
     *
     *  @return true if all the checks are true else false
     *
     */
    protected boolean checkConstraints(){
        if(centroidArrayList.size() == 4) {

            if(checkCircularity()) {

                if (checkPixelSize()) {
                    /* determining the corners of the coins */
                    cornerArrayList = findCorners(centroidArrayList, radiusArrayList.get(0));

                    /* cropping the image to limit the search space by bounding it with the farthest
                    *  corners of each coin
                    */
                    processedMat = cropImage(processedMat, cornerArrayList);

                    /* uncomment to get the timing values in the log*/
                    //timingLogger.dumpToLog();

                    return true;
                }
                STATUS = "Coin size check failed";
                return false;
            }
            STATUS = "Coin circularity failed";
            return false;
        }
        else {
            STATUS = "All the coins not detected";
            return false;
        }
    }

    /** Used to check the circularity of the coins.
     *  The threshold value is defined as a class level variable to easily change it
     *
     *  @return true if the circularity is within the threshold else returns false
     * */
    private boolean checkCircularity(){
        boolean circular = false;
        for(double r : radiusArrayList)
        {
            double circ = 4 * Math.PI * Math.PI * r * r / Math.pow(2 * Math.PI * r, 2); // calculate circularity

            /* check circularity > 0.95 */
            circular = circ > COIN_CIRCULARITY;
        }
        timingLogger.addSplit("checkCircularity");
        return circular;
    }

    /** Used to check the pixel size of the coins, whether all the coins are same or not
     *  The threshold value is defined as a class level variable to easily change it
     *
     *  @return true if the pixel size is within the threshold else returns false
     * */
    private boolean checkPixelSize(){
        boolean pixelSize = false;
        ArrayList<Double> areaArrayList = new ArrayList<>();

        //Log.d("checkPixelSize: Radius",radiusArrayList.toString());

        for(double r : radiusArrayList){
            areaArrayList.add(Math.PI * r * r);
        }

        //Log.d("checkPixelSize: Areas",areaArrayList.toString());

        for(double a : areaArrayList) {
             /* check that all four are within 2.5% of each other in pixel size */
            double threshold = (a * COIN_SIZE_THRESHOLD)/100;
            double min = a - threshold;
            double max = a + threshold;

            //Log.d("checkPixelSize: Min-Max",String.valueOf(min) + " - " + String.valueOf(max));

            for (double ar : areaArrayList) {
                pixelSize = min >= ar && a <= max;
            }
        }
        timingLogger.addSplit("checkPixelSize");
        //TODO : return the pixelSize variable value
        return true;
    }

    /** Used to crop the sample image bounded by the four coin corners
     *
     * @param initialMat mat of the sample image
     *
     * @param cornerArrayList array list consisting of the four coin corners
     *
     * @return cropped Mat
     * */
    private Mat cropImage(Mat initialMat, ArrayList<Point> cornerArrayList) {

        /* determine the top-left corner top-left coin which is the 1st in the ArrayList
        *  and the bottom-right corner of the bottom-right coin which is 4th in ArrayList
        */
        Point tl = new org.opencv.core.Point(cornerArrayList.get(0).x, cornerArrayList.get(0).y);
        Point br = new org.opencv.core.Point(cornerArrayList.get(3).x, cornerArrayList.get(3).y);

        /* create a rectangle based on the top-left and bottom-right points to crop the image */
        org.opencv.core.Rect cropBox = new org.opencv.core.Rect(tl, br);

        timingLogger.addSplit("cropImage");

        /* uncomment the below to save the coin recognition cropped mat for debugging */
         Log.d("Crop Box dimensions", cropBox.height + " " + cropBox.width);
        Mat croppedMat = initialMat.submat(cropBox);
        Imgproc.cvtColor(croppedMat,croppedMat,Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(Constants.PHOTO_PATH + "/croppedCoinRecog.jpg",croppedMat);
        Imgproc.cvtColor(croppedMat,croppedMat,Imgproc.COLOR_RGB2BGR);

        /* crop and return the original image based on the crop box by creating a submat*/
        return initialMat.submat(cropBox);

    }

    /** This method is used to get the background color from the image to mask the detected coins
     *
     * @param initialMat mat of the sample image
     *
     * @return a Scalar consisting of the RGB values of the background color used to mask
     * */
    private Scalar maskColor(Mat initialMat){
        org.opencv.core.Rect touchedRect = new org.opencv.core.Rect();
        Scalar mBlobColorHsv;

        touchedRect.x = 200;
        touchedRect.y = initialMat.cols()/2;

        touchedRect.width = 2;
        touchedRect.height = 2;

        Mat touchedRegionRgba = initialMat.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, mBlobColorHsv);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB, 4);

        timingLogger.addSplit("maskColor");

        return new Scalar(pointMatRgba.get(0, 0));
    }

    /**
     * This function is used to sort and return the respective corners of the four coin centroids
     * It also creates a {@link org.wheatgenetics.onekk.Coin} object for each of the coins and
     * adds them to the coin ArrayList
     *
     * @param pointArrayList takes an array list consisting of the four coin centroids
     * @param radius radius of the coin
     *
     * @return an ArrayList consisting of four corners corresponding to the four centroids using
     *         which the image is cropped before processing
     */
    private ArrayList<Point> findCorners
    (ArrayList<Point> pointArrayList, double radius){

        Point tempPt;

        /* sort the ArrayList of centroids starting from top-left to bottom-right of the screen
        *  using just the x-coordinate
        */
        Collections.sort(pointArrayList, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return o1.x > o2.x ? 1 : -1;
            }
        });

        /* sort the ArrayList of centroids, top-left and bottom-left of the screen
        *  using the y-coordinate
        */
        Point pt1 = pointArrayList.get(0);
        Point pt2 = pointArrayList.get(1);

        if(pt1.y > pt2.y){
            tempPt = pt1;
            pointArrayList.set(0,pt2);
            pointArrayList.set(1,tempPt);
        }

        /* sort the ArrayList of centroids, top-right and bottom-right of the screen
        *  using the y-coordinate
        */
        Point pt3 = pointArrayList.get(2);
        Point pt4 = pointArrayList.get(3);

        if(pt3.y > pt4.y){
            tempPt = pt3;
            pointArrayList.set(2,pt4);
            pointArrayList.set(3,tempPt);
        }

        coinArrayList = new ArrayList<>();

        /* TOP LEFT COIN CORNER */
        tempPt = pointArrayList.get(0);
        Coin tlCoin = new Coin(tempPt,radius);
        coinArrayList.add(tlCoin);
        pointArrayList.set(0,tlCoin.getTl());

        /* BOTTOM LEFT COIN CORNER */
        tempPt = pointArrayList.get(1);
        Coin blCoin = new Coin(tempPt,radius);
        coinArrayList.add(blCoin);
        pointArrayList.set(1,blCoin.getBl());

        /* TOP RIGHT COIN CORNER */
        tempPt = pointArrayList.get(2);
        Coin trCoin = new Coin(tempPt,radius);
        coinArrayList.add(trCoin);
        pointArrayList.set(2,trCoin.getTr());

        /* BOTTOM RIGHT COIN CORNER */
        tempPt = pointArrayList.get(3);
        Coin brCoin = new Coin(tempPt,radius);
        coinArrayList.add(brCoin);
        pointArrayList.set(3,brCoin.getBr());

        //Log.d("Corners", pointArrayList.toString());

        timingLogger.addSplit("findCorners");

        return pointArrayList;
    }

    /** getter method to get the array list consisting of the coin objects for each
     *  of the detected coins
     *
     *  @return array list of the coin objects
     * */
    public ArrayList<Coin> getCoinArrayList() {
        return coinArrayList;
    }

    /** getter method to get the processed mat which is the cropped mat
     *
     *  @return cropped mat
     * */
    public Mat getProcessedMat() {
        return processedMat;
    }

    /** Calculates the scale factor to relate pixel and physical dimensions of the coin
     *  used to measure the size of the seeds
     *
     * @return scale factor
     * */
    public double getPixelMetric(){
        double sumRadius = 0;

        for(double r : radiusArrayList)
            sumRadius = sumRadius + r;

        double avgRadius = sumRadius / radiusArrayList.size();

        return coinSize / avgRadius;
    }

    /** To get the STATUS of the coin recognition hueProcess, used to display in the Notifications
     *
     *  POSSIBLE VALUES : Coin size check failed
     *                    Coin circularity failed
     *                    All the coins not detected
     *
     *  @return a String with the corresponding status
     *
     * */
    public String getSTATUS() {
        return STATUS;
    }
}

/** a separate class to define the Coins that are detected as part of the Coin Recognition
 * */
class Coin {
    private final Point center;
    private final double radius;
    
    private final Point tl;
    private final Point tr;
    private final Point bl;
    private final Point br;
    private final org.opencv.core.Rect boundingBox;
    private final double coinArea;

    public Coin(Point center, double radius){
        this.center = center;
        this.radius = radius;
        tl = new Point(center.x - radius,center.y - radius);
        tr = new Point(center.x + radius,center.y - radius);
        bl = new Point(center.x - radius,center.y + radius);
        br = new Point(center.x + radius,center.y + radius);
        boundingBox = new org.opencv.core.Rect(tl,br);
        coinArea = 22* radius * radius /7;
    }

    public Point getCenter() {
        return center;
    }

    public Point getTl() {
        return tl;
    }

    public Point getTr() {
        return tr;
    }

    public Point getBl() {
        return bl;
    }

    public Point getBr() {
        return br;
    }

    public double getRadius() {
        return radius;
    }

    public org.opencv.core.Rect getBoundingBox() {
        return boundingBox;
    }

    public double getCoinArea() {
        return coinArea;
    }
}
