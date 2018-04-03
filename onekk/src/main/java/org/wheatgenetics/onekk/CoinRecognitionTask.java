package org.wheatgenetics.onekk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sid on 3/6/18.
 */

public class CoinRecognitionTask extends AsyncTask<byte[],AsyncTask.Status,ArrayList<Point>> {

    /* declared the ArrayList as android Point for ease of use to display them on the preview
    *
    *  centroidArrayList is the array list consisting of the centroids of the coins
    *  cornerArrayList is the array list consisting of the calculated corners for cropping
    */

    private ArrayList<Coin> coinArrayList;
    private ArrayList<Point> centroidArrayList;
    private ArrayList<Point> cornerArrayList;
    private ArrayList<Point> boundingBoxArrayList;
    private ArrayList<Point> coinCoordsList;
    private int[] textSize = new int[1];
    private org.opencv.core.Rect boundingBox = null;
    private Mat processedMat = null;
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private int previousSize = 0;
    private double coinSize = 0;
    private guideBox gb = null;

    /* default constructor */
    public CoinRecognitionTask(double coinSize){
        this.coinSize = coinSize;
    }

    /* used in static processing */
    public CoinRecognitionTask(int cameraWidth, int cameraHeight){
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
    }

    /* used in real time processing */
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

        Log.d("Background processing","Starting process");
        //coinCoordsList = process(m);
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

    }

    /** The function starts the coin recognition process triggered from
     * MainActivity{@link org.wheatgenetics.onekk.MainActivity}
     *
     * @param initialMat takes the mat of the frame captured from the camera preview at that moment
     *
     * @return an ArrayList containing the centroids of the coins
     *
     */
    public Mat process(Mat initialMat){
        centroidArrayList = new ArrayList<>();

        // Check if image is loaded fine
        if( initialMat.empty() ) {
            Log.e("Coin recognition","Empty image");
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(initialMat, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(gray, gray, 5);
        Mat circles = new Mat();
        int radius = 0;

        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                (double)gray.rows()/8, //16 change this value to detect circles with different distances to each other
                100.0, 30.0, 100, 300); // change the last two parameters
        // (min_radius & max_radius) to detect larger circles

        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));

            // circle center
            Imgproc.circle(initialMat, center, 1, new Scalar(0,100,100), 3, 8, 0 );

            // circular mask 157,178,195
            radius = (int) Math.round(c[2]);
            Imgproc.circle(initialMat, center, radius + 20, maskColor(initialMat), -1, 8, 0 );

            centroidArrayList.add(new Point((int)center.x,(int)center.y));
        }

        Imgproc.cvtColor(initialMat,initialMat,Imgproc.COLOR_BGR2RGB);

        /* Saving the mat consisting of the circles with mask */
        Imgcodecs.imwrite(Constants.PHOTO_PATH + "/houghCoinRecog.jpg",initialMat);

        Imgproc.cvtColor(initialMat,initialMat,Imgproc.COLOR_RGB2BGR);

        if(centroidArrayList.size() == 4) {
        /* determining the corners of the coins */
            cornerArrayList = findCorners(centroidArrayList, radius);

        /* cropping the image to limit the search space by bounding it with the farthest
        *  corners of each coin
        */
            initialMat = cropImage(initialMat, cornerArrayList);
        }
        return initialMat;
    }

    private Mat cropImage(Mat initialMat, ArrayList<Point> cornerArrayList) {

        /* determine the top-left corner top-left coin which is the 1st in the ArrayList
        *  and the bottom-right corner of the bottom-right coin which is 4th in ArrayList
        */
        Point tl = new org.opencv.core.Point(cornerArrayList.get(0).x, cornerArrayList.get(0).y);
        Point br = new org.opencv.core.Point(cornerArrayList.get(3).x, cornerArrayList.get(3).y);

        /* create a rectangle based on the top-left and bottom-right points to crop the image */
        org.opencv.core.Rect cropBox = new org.opencv.core.Rect(tl, br);

        Log.d("Crop Box dimensions", cropBox.height + " " + cropBox.width);

        /* crop the original image based on the crop box by creating a submat*/
        Mat croppedMat = initialMat.submat(cropBox);

        Imgcodecs.imwrite(Constants.PHOTO_PATH + "/croppedCoinRecog.jpg",croppedMat);

        return croppedMat;
    }

    private Scalar maskColor(Mat initialMat){
        org.opencv.core.Rect touchedRect = new org.opencv.core.Rect();
        Scalar mBlobColorHsv;

        touchedRect.x = initialMat.rows()/2;
        touchedRect.y = initialMat.cols()/2;

        touchedRect.width = 5;
        touchedRect.height = 5;

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
     * @return an ArrayList consisting of four corner points corresponding to the four centroids using
     *         which the image is cropped before processing
     */
    private ArrayList<Point> findCorners
    (ArrayList<Point> pointArrayList, int radius){

        Point tempPt;

        Log.d("Corners - pre sort", pointArrayList.toString());

        /* sort the ArrayList of centroids starting from top-left to bottom-right of the screen
        *  using just the x-coordinate
        */
        Collections.sort(pointArrayList, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                if(o1.x > o2.x)
                    //if( o1.y > o2.y)
                     //   return 0;
                    //else
                        return 1;
                else
                //if( o1.y > o2.y)
                    return -1;
                //else
                    //return 1;
            }
        });

        /* sort the ArrayList of centroids starting from top-left and bottom-left of the screen
        *  using the y-coordinate
        */
        Point pt1 = pointArrayList.get(0);
        Point pt2 = pointArrayList.get(1);

        if(pt1.y > pt2.y){
            tempPt = pt1;
            pointArrayList.set(0,pt2);
            pointArrayList.set(1,tempPt);
        }

        /* sort the ArrayList of centroids starting from top-right and bottom-right of the screen
        *  using the y-coordinate
        */
        Point pt3 = pointArrayList.get(2);
        Point pt4 = pointArrayList.get(3);

        if(pt3.y > pt4.y){
            tempPt = pt3;
            pointArrayList.set(2,pt4);
            pointArrayList.set(3,tempPt);
        }

        Log.d("Corners - post sort", pointArrayList.toString());

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

        Log.d("Corners", pointArrayList.toString());
        return pointArrayList;
    }

    public ArrayList<Point> getCentroidArrayList() {
        return centroidArrayList;
    }

    public ArrayList<Point> getCornerArrayList() {
        return cornerArrayList;
    }

    public Mat getProcessedMat() {
        return processedMat;
    }

    public double getPixelMetric(){
        return coinSize / coinArrayList.get(0).getRadius();
    }
}

class Coin {
    private Point center;
    private int radius;
    
    private Point tl;
    private Point tr;
    private Point bl;
    private Point br;
    private org.opencv.core.Rect boundingBox;
    private double coinArea;

    public Coin(Point center, int radius){
        this.center = center;
        this.radius = radius;
        tl = new Point(center.x - radius,center.y - radius);
        tr = new Point(center.x + radius,center.y - radius);
        bl = new Point(center.x - radius,center.y + radius);
        br = new Point(center.x + radius,center.y + radius);
        boundingBox = new org.opencv.core.Rect(tl,br);
        coinArea = (double)(22* radius * radius)/7;
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

    public int getRadius() {
        return radius;
    }

    public org.opencv.core.Rect getBoundingBox() {
        return boundingBox;
    }

    public double getCoinArea() {
        return coinArea;
    }
}
