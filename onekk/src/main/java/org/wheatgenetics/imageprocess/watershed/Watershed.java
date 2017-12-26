package org.wheatgenetics.imageprocess.watershed;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
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
import org.wheatgenetics.onekk.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Watershed {
    private static final String TAG = "Watershed";
    public static String MyDate;
    String path;
    public double thresh;
    public int h1, h2, mh1, mh2, mh3, mh4;
    int black = -16777216;
    int seeds = 0;
    boolean multiHue = false;
    Mat mat5gray2 = new Mat();
    private String fileName;
    Mat originalImage;
    public boolean cropImage = true;

    public void setThresh(double threshold){
        this.thresh = threshold;
    }

    public void setHue(int h1, int h2 ){
        this.h1 = h1;
        this.h2 = h2;
        print("h1, h2 are: "+h1+" "+h2);
    }

    public void setMultipleHue(int h3, int h4, int h5, int h6 ){
        this.mh1 = h3;
        this.mh2 = h4;
        this.mh3 = h5;
        this.mh4 = h6;
        print("mh1, mh2, mh3, mh4 are: "+mh1+" "+mh2+" "+mh3+" "+mh4);
    }

    public void checkMultiHue(boolean multiHue){
        this.multiHue = multiHue;
    }

    public void setMultiHue(boolean multiHue, String text){
        this.multiHue = multiHue;
    }

    public void setCropImage(boolean cropImage){
        this.cropImage = cropImage;
    }

    public  Watershed(String fileName){
        File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/0815171641.jpg");
        File file = new File(Constants.PHOTO_PATH.toString() + "/" + fileName);
        print("Path is :"+file1.getAbsolutePath());
        this.originalImage   = Imgcodecs.imread(file1.getAbsolutePath());
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

    private void cropImage() {
        System.out.println("Cropping image...");

        Mat uncropped = filterBackground(this.originalImage);

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

        Mat mask_image = new Mat(this.originalImage.size(), CvType.CV_8U);

        Imgproc.drawContours(mask_image, contours, largest_index, new Scalar(255, 255, 255), Core.FILLED);

        this.originalImage.copyTo(mask_image, mask_image);

        this.originalImage = mask_image;

        MatOfPoint2f mat = new MatOfPoint2f(contours.get(largest_index).toArray());

        RotatedRect rect = Imgproc.minAreaRect(mat);
        Size rectSize = rect.size;

        System.out.println("PHOTO CROPPED FROM " + this.originalImage.rows() + " x " + this.originalImage.cols() + " TO " + Math.round(rectSize.height) + " x " + Math.round(rectSize.width) + "\n");
    }

    public void process(){
        //if (cropImage) {
        //    this.cropImage();
        //}
        //File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedLB/WatershedImages", "Image_input_101.jpg");

        //print("Path is :"+file1.getAbsolutePath());
        //Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        //Mat imageMat = new Mat();
        //Utils.bitmapToMat(bitmap,imageMat);
        //Mat originalImage = Imgcodecs.imread(file.getAbsolutePath());

        if(!this.originalImage.empty()){
            print("Image available for processing");


        //thresh = myThreshold;
        //Bitmap originalBitmap = Bitmap.createBitmap(originalImage.cols(), originalImage.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(originalImage, originalBitmap);
        //print("Mat read");
        Mat image1Mat = new Mat();
        //Mat image1Mat = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8UC1);
        //Imgproc.cvtColor(originalImage,image1Mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(this.originalImage,image1Mat, Imgproc.COLOR_BGR2HSV);
        //print("Mat converted to HSV");
        //Bitmap abc = Bitmap.createBitmap(image1Mat.cols(), image1Mat.rows(), Bitmap.Config.ARGB_8888);
        // Utils.matToBitmap(image1Mat,abc);

        Mat mat3 = new Mat();
        //print("new mat3 created");
        if(multiHue) {
            mat3 = preProcess2(image1Mat);
        }
        else
        {
            mat3 = preProcess1(image1Mat);
        }
        //**********Core.inRange(image1Mat, new Scalar(0, 0, 0), new Scalar(50, 255, 255), mat3);

        //print("core.inrange performed");
        //Bitmap abcd = Bitmap.createBitmap(mat3.cols(), mat3.rows(), Bitmap.Config.ARGB_8888);
        // Utils.matToBitmap(mat3, abcd);

        Mat mat4 = new Mat();
        //print("new mat4 created");
        Imgproc.threshold(mat3, mat4, 0, 255, Imgproc.THRESH_BINARY);
        //print("Binary performed");

        //Bitmap saveBitmap = Bitmap.createBitmap(mat4.cols(), mat4.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mat4, saveBitmap);
        //saveImage1(saveBitmap);


        Bitmap threshBitmap = Bitmap.createBitmap(mat4.cols(), mat4.rows(), Bitmap.Config.ARGB_8888);
        //print("created bitmap threshBitmap");
        Utils.matToBitmap(mat4, threshBitmap);
        //print("converted mat to bitmap");
        print("width is "+threshBitmap.getWidth()+" height is "+threshBitmap.getHeight());
        //saveImage(threshBitmap);
        EDM e= new EDM();
        e.setThreshold(thresh);
        e.setup(threshBitmap);
        e.setOutputType(EDM.BYTE_OVERWRITE);
        //saveImage(threshBitmap);
        Mat mat5 = new Mat();
        Utils.bitmapToMat(threshBitmap, mat5);
        Mat mat5gray = new Mat();
        Imgproc.cvtColor(mat5, mat5gray, Imgproc.COLOR_BGR2GRAY);
        int delta = 3;
        /**
         List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
         Mat hierarchy = new Mat();
         Imgproc.findContours(mat5gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);


         double total_area = 0.0;
         double average_area = 0.0;
         int nsize = contours.size();


         for (int i=0; i<nsize; i++)
         {
         double area = Imgproc.contourArea(contours.get(i));
         total_area += area;
         }
         if (nsize > 0)
         average_area = total_area/nsize;
         //print("Average contour area is " + average_area);
         double area_threshold = 1.5 * average_area; // consider splitting contours > 1.5*average_area
         double area_threshold2 = 2.5 * average_area;

         List<Point> slist = new ArrayList<Point>();
         List<Point> dlist = new ArrayList<Point>();


         for (int i=0; i<nsize; i++)
         {
         double area = Imgproc.contourArea(contours.get(i));

         //  System.out.println("Contour " + i + " area is " + Imgproc.contourArea(contours.get(i)));
         Imgproc.drawContours(mat5gray, contours, i, new Scalar(255,255,255));

         if (area > area_threshold)
         {
         List<Point> clist = contours.get(i).toList();

         //    System.out.println("Checking Large Contour " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
         int n = clist.size();
         double crossProd = 0.0;
         boolean foundOne = false;
         int max_j = -1;
         double maxProd = 0.0;
         for (int j=0; j<n; j++)
         {
         crossProd = (clist.get(j).x - clist.get((j+n-4)%n).x)*(clist.get((j+3)%n).y - clist.get(j).y) -
         (clist.get(j).y - clist.get((j+n-4)%n).y)*(clist.get((j+3)%n).x - clist.get(j).x);
         if (crossProd > 4.0) {
         if (foundOne == false) {
         foundOne = true;
         max_j = j;
         maxProd = crossProd;
         }
         else if (maxProd < crossProd) {
         max_j = j;
         maxProd = crossProd;
         }
         if (j==n-1) {
         //              System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
         foundOne = false;
         }
         }
         else
         {
         if (foundOne == true)
         {
         //            System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
         foundOne = false;
         maxProd = 0;
         max_j = -1;
         }
         }
         }
         }
         }

         for (int i=0; i<nsize; i++)
         {
         double area = Imgproc.contourArea(contours.get(i));

         //System.out.println("Contour " + i + " area is " + Imgproc.contourArea(contours.get(i)));
         Imgproc.drawContours(mat5gray, contours, i, new Scalar(255,255,255));

         if (area > area_threshold)
         {
         List<Point> clist = contours.get(i).toList();
         List<CPoint> plist = new ArrayList<CPoint>();

         //  System.out.println("Checking Large Contour " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
         int n = clist.size();
         double crossProd = 0.0;
         boolean foundOne = false;
         int max_j = -1;
         double maxProd = 0.0;
         for (int j=0; j<n; j++)
         {
         crossProd = (clist.get(j).x - clist.get((j+n-4)%n).x)*(clist.get((j+3)%n).y - clist.get(j).y) -
         (clist.get(j).y - clist.get((j+n-4)%n).y)*(clist.get((j+3)%n).x - clist.get(j).x);
         if (crossProd > 4.0) {
         if (foundOne == false) {
         foundOne = true;
         max_j = j;
         maxProd = crossProd;
         }
         else if (maxProd < crossProd) {
         max_j = j;
         maxProd = crossProd;
         }
         if (j==n-1) {
         //            System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
         foundOne = false;
         int xVal = (int) Math.round(clist.get(max_j).x);
         int yVal = (int) Math.round(clist.get(max_j).y);
         CPoint ePoint = new CPoint(xVal, yVal, maxProd);
         plist.add(ePoint);
         }
         }
         else
         {
         if (foundOne == true)
         {
         //          System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
         foundOne = false;
         int xVal = (int) Math.round(clist.get(max_j).x);
         int yVal = (int) Math.round(clist.get(max_j).y);
         CPoint ePoint = new CPoint(xVal, yVal, maxProd);
         plist.add(ePoint);
         maxProd = 0;
         max_j = -1;
         }

         }
         }
         Collections.sort(plist);
         //for (int ii=0; ii<plist.size(); ii++)
         //  System.out.println("Sorted: " + plist.get(ii).getX() + ", " + plist.get(ii).getY() + " -> " + plist.get(ii).getXProd());
         if ((plist.size() > 1) && (plist.get(0).getXProd()>=20) && (plist.get(1).getXProd()>=10.0))  // MLN both cross products big enough
         {
         Point src = new Point(plist.get(0).getX(), plist.get(0).getY());
         Point dst = new Point(plist.get(1).getX(), plist.get(1).getY());
         slist.add(src);
         dlist.add(dst);
         if ((area > area_threshold2)&&(plist.size() > 3))
         {
         src = new Point(plist.get(2).getX(), plist.get(2).getY());
         dst = new Point(plist.get(3).getX(), plist.get(3).getY());
         slist.add(src);
         dlist.add(dst);

         }
         }

         }
         }
         **/

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mat5gray, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        double total_area = 0.0;
        double average_area = 0.0;
        int nsize = contours.size();



        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            total_area += area;
        }
        if (nsize > 0)
            average_area = total_area/nsize;
        //System.out.println("Original average area = " + average_area);



        average_area *= 1.5;
        // size based on single elements only...
        int single_count = 0;
        total_area = 0.0;
        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < average_area)
            {
                total_area += area;
                single_count++;
            }
        }

        average_area = total_area/single_count;
        //System.out.println("New average area = " + average_area);
        //System.out.println("Average contour area is " + average_area);
/*
        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours.get(i));
            if(area > 1.3* average_area){
                List<Point> mycList = contours.get(i).toList();
                System.out.println("coords are... x is "+mycList.get(0).x+"! y is "+mycList.get(0).y);
                System.out.println("Found a large Contour " + i + " area is " + Imgproc.contourArea(contours.get(i)));
                Imgproc.drawContours(mat5gray, contours, i, new Scalar(255,255,255));
            }
        }
        Bitmap threshBitmap11 = Bitmap.createBitmap(mat5gray.cols(), mat5gray.rows(), Bitmap.Config.ARGB_8888);
        //print("created bitmap threshBitmap");
        Utils.matToBitmap(mat5gray, threshBitmap11);
        saveImage(threshBitmap11);
*/
        double area_threshold = 1.5 * average_area; // consider splitting contours > 1.5*average_area
        double area_threshold2 = 2.5 * average_area;
        double small_size = 0.1 * average_area;

        List<Point> slist = new ArrayList<Point>();
        List<Point> dlist = new ArrayList<Point>();

        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours.get(i));

            //   System.out.println("Contour " + i + " area is " + area);
            if (area < small_size)
            {
                List<Point> clist = contours.get(i).toList();
                contours.remove(i);
                //     System.out.println("Small contour " + i + " at "+ clist.get(0).x + ", " + clist.get(0).y + " removed");
                i--;
                nsize--;
            }
            else
            {
                Imgproc.drawContours(mat5gray, contours, i, new Scalar(255,255,255));

                if (area > area_threshold)
                {
                    List<Point> clist = contours.get(i).toList();

                    //                  System.out.println("Checking Large Contour A " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
                    int n = clist.size();
                    double crossProd = 0.0;
                    double slope = 0.0;
                    double maxSlope = 0.0; // slope corresponding to max prod...
                    boolean foundOne = false;
                    int max_j = -1;
                    double maxProd = 0.0;
                    for (int j=0; j<n; j++)
                    {
                        // MLN NEW was -5 +4
                        Point p = clist.get(j);
                        Point pup = clist.get((j+delta)%n);
                        Point pdwn = clist.get((j+n-delta)%n);
                        double xavg = (pup.x + pdwn.x)*0.5;
                        double yavg = (pup.y + pdwn.y)*0.5;

                        if (Math.abs(p.x - xavg) > 0.0001)
                            slope = (p.y - yavg)/(p.x - xavg);
                        else
                            slope = 1000000.0;

                        crossProd = (p.x - pdwn.x)*(pup.y - p.y) - (p.y - pdwn.y)*(pup.x - p.x);
                        if (crossProd > 4.0) {
                            if (foundOne == false) {
                                foundOne = true;
                                max_j = j;
                                maxProd = crossProd;
                                maxSlope = slope;
                            }
                            else if (maxProd < crossProd) {
                                max_j = j;
                                maxProd = crossProd;
                                maxSlope = slope;
                            }
                            if (j==n-1) {
                                //                            System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                                foundOne = false;
                            }
                        }
                        else
                        {
                            if (foundOne == true)
                            {
                                //                          System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                                foundOne = false;
                                maxProd = 0;
                                max_j = -1;
                            }
                        }
                    }
                }
            }
        }

        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours.get(i));

            //System.out.println("Contour " + i + " area is " + area);
            if (area < small_size)
            {
                List<Point> clist = contours.get(i).toList();
                contours.remove(i);
                //        System.out.println("Small contour " + i + " at "+ clist.get(0).x + ", " + clist.get(0).y + " removed");
                i--;
                nsize--;
            }
            else
            {
                //      System.out.println("Contour " + i + " area is " + Imgproc.contourArea(contours.get(i)));
                Imgproc.drawContours(mat5gray, contours, i, new Scalar(255,255,255));

                if (area > area_threshold)
                {
                    List<Point> clist = contours.get(i).toList();
                    List<CPoint> plist = new ArrayList<CPoint>();

                    //        System.out.println("Checking Large Contour B " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
                    int n = clist.size();
                    double crossProd = 0.0;
                    boolean foundOne = false;
                    int max_j = -1;
                    double maxProd = 0.0;
                    double slope = 0.0;
                    double maxSlope = 0.0;

                    for (int j=0; j<n; j++)
                    {
                        // MLN NEW was -5 +4..
                        Point p = clist.get(j);

                        System.out.println(p.x + " " + p.y);

                        Point pup = clist.get((j+delta)%n);
                        Point pdwn = clist.get((j+n-delta)%n);
                        double xavg = (pup.x + pdwn.x)*0.5;
                        double yavg = (pup.y + pdwn.y)*0.5;

                        if (Math.abs(p.x - xavg) > 0.0001)
                            slope = (p.y - yavg)/(p.x - xavg);
                        else
                            slope = 1000000.0;

                        crossProd = (p.x - pdwn.x)*(pup.y - p.y) - (p.y - pdwn.y)*(pup.x - p.x);
                        if (crossProd > 4.0) {
                            if (foundOne == false) {
                                foundOne = true;
                                max_j = j;
                                maxProd = crossProd;
                                maxSlope = slope;
                            }
                            else if (maxProd < crossProd) {
                                max_j = j;
                                maxProd = crossProd;
                                maxSlope = slope;
                            }
                            if (j==n-1) {
                                //                  System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                                foundOne = false;
                                int xVal = (int) Math.round(clist.get(max_j).x);
                                int yVal = (int) Math.round(clist.get(max_j).y);
                                CPoint ePoint = new CPoint(xVal, yVal, maxProd, maxSlope);
                                plist.add(ePoint);
                            }
                        }
                        else
                        {
                            if (foundOne == true)
                            {
                                //                System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                                foundOne = false;
                                int xVal = (int) Math.round(clist.get(max_j).x);
                                int yVal = (int) Math.round(clist.get(max_j).y);
                                CPoint ePoint = new CPoint(xVal, yVal, maxProd, maxSlope);
                                plist.add(ePoint);
                                maxProd = 0.0;
                                maxSlope = 0.0;
                                max_j = -1;
                            }
                        }
                    }

                    Collections.sort(plist);
                    CPoint[] cp = new CPoint[4];
                    Point[] p = new Point[4];

                    //  for (int ii=0; ii<plist.size(); ii++)
                    //    System.out.println("Sorted: " + plist.get(ii).getX() + ", " + plist.get(ii).getY() + " -> " + plist.get(ii).getXProd());
                    if ((plist.size() > 1) && (plist.get(0).getXProd()>=20) && (plist.get(1).getXProd()>=10.0))  // MLN both cross products big enough
                    {
                        cp[0] = plist.get(0);
                        p[0] = new Point(cp[0].getX(), cp[0].getY());
                        cp[1] = plist.get(1);
                        p[1] = new Point(cp[1].getX(), cp[1].getY());
                        //  System.out.println("Slopes: "+ cp[0].slope + ", " + cp[1].slope);
                        int minIndex = 1;

                        if ((area > area_threshold2)&&(plist.size() > 3))
                        {
                            cp[2] = plist.get(2);
                            p[2] = new Point(cp[2].getX(), cp[2].getY());
                            cp[3] = plist.get(3);
                            p[3] = new Point(cp[3].getX(), cp[3].getY());
                            double minError;
                            if (cp[0].slope >= 1000000.0)
                            {
                                minError = Math.abs(p[1].x - p[0].x);
                                for (int jj=2; jj<=3; jj++)
                                {
                                    double error = Math.abs(p[jj].x - p[0].x);
                                    if (error < minError)
                                    {
                                        minError = error;
                                        minIndex = jj;
                                    }
                                }
                            }
                            else
                            {
                                minError = Math.abs(p[1].y - (p[0].y + cp[0].slope*(p[1].x-p[0].x)));

                                //        System.out.println("minError from " + p[0].x + ", " + p[0].y + " to " + p[1].x + ", " + p[1].y + " is " + minError);

                                for (int jj=2; jj<=3; jj++)
                                {
                                    double err = Math.abs(p[jj].y - (p[0].y + cp[0].slope*(p[jj].x-p[0].x)));

                                    //          System.out.println("minError from " + p[0].x + ", " + p[0].y + " to " + p[jj].x + ", " + p[jj].y + " is " + err);


                                    if (err < minError)
                                    {
                                        minError = err;
                                        minIndex = jj;
                                    }
                                }
                            }



                            slist.add(p[0]);
                            dlist.add(p[minIndex]);


/** don't automagically add the next two... MLN HACK
 *  maybe recursively call the algorithm again ???
 *
 if (minIndex == 1)
 {
 slist.add(p[2]);
 dlist.add(p[3]);
 }
 else if (minIndex == 2)
 {
 slist.add(p[1]);
 dlist.add(p[3]);
 }
 if (minIndex == 3)
 {
 slist.add(p[1]);
 dlist.add(p[2]);
 }
 **/

                        }
                        else
                        {
                            slist.add(p[0]);
                            dlist.add(p[1]);
                        }
                    }
                }
            }
        }
        e = null; // MLN HACK
        //Bitmap binaryBitmap = Bitmap.createBitmap(mat4.cols(), mat4.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mat4, binaryBitmap);
        //int[] pixels = new int[mat4.rows() * mat4.cols()];
        //for(int i=0; i< ((mat4.rows())*(mat4.cols())); i++){
        //    pixels[i] = black;
        //}

        //binaryBitmap.setPixels(pixels, 0, binaryBitmap.getWidth(), 0, 0, binaryBitmap.getWidth(), binaryBitmap.getHeight());
        Bitmap threshBitmap1 = Bitmap.createBitmap(mat4.cols(), mat4.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat4, threshBitmap1);
        Canvas canvas = new Canvas(threshBitmap1);
        Paint paint  = new Paint();
        paint.setColor(Color.BLACK);

        for (int i=0; i<slist.size(); i++)
        {
            Point src = slist.get(i);
            Point dst = dlist.get(i);
            int x1 = (int) Math.round(src.x);
            int x2 = (int) Math.round(dst.x);
            int y1 = (int) Math.round(src.y);
            int y2 = (int) Math.round(dst.y);
            //ip2.drawLine(x1, y1, x2, y2);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        //  saveImage1(binaryBitmap);
        EDM e2 = new EDM();
        e2.setup(threshBitmap1);
        e2.setOutputType(EDM.BYTE_OVERWRITE);

        Mat mat52 = new Mat();
        Utils.bitmapToMat(threshBitmap1, mat52);

        Imgproc.cvtColor(mat52, mat5gray2, Imgproc.COLOR_BGR2GRAY);

        List<MatOfPoint> contours2 = new ArrayList<MatOfPoint>();
        Mat hierarchy2 = new Mat();
        Imgproc.findContours(mat5gray2, contours2, hierarchy2, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        /*
        for (int i=0; i<contours2.size(); i++)
        {
            double area = Imgproc.contourArea(contours2.get(i));

          //  System.out.println("Contour " + i + " area is " + Imgproc.contourArea(contours2.get(i)));
            Imgproc.drawContours(mat5gray2, contours2, i, new Scalar(255,255,255));

            if (area > area_threshold)
            {
                List<Point> clist = contours2.get(i).toList();

              //  System.out.println("Checking Large Contour " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
                int n = clist.size();
                double crossProd = 0.0;
                boolean foundOne = false;
                int max_j = -1;
                double maxProd = 0.0;
                for (int j=0; j<n; j++)
                {
                    crossProd = (clist.get(j).x - clist.get((j+n-2)%n).x)*(clist.get((j+2)%n).y - clist.get(j).y) -
                            (clist.get(j).y - clist.get((j+n-2)%n).y)*(clist.get((j+2)%n).x - clist.get(j).x);
                    if (crossProd > 4.0) {
                        if (foundOne == false) {
                            foundOne = true;
                            max_j = j;
                            maxProd = crossProd;
                        }
                        else if (maxProd < crossProd) {
                            max_j = j;
                            maxProd = crossProd;
                        }
                        if (j==n-1) {
                //            System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                            foundOne = false;
                        }
                    }
                    else
                    {
                        if (foundOne == true)
                        {
                  //          System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                            foundOne = false;
                        }
                    }
                }
            }
        }*/

        nsize = contours2.size();
        double total_area2 = 0.0;

        for (int i=0; i<nsize; i++)
        {
            double area = Imgproc.contourArea(contours2.get(i));
            total_area2 += area;

            System.out.println("Contour " + i + " area is " + area);
            if (area < small_size)
            {
                List<Point> clist = contours2.get(i).toList();
                contours2.remove(i);
                System.out.println("Small contour " + i + " at "+ clist.get(0).x + ", " + clist.get(0).y + " removed");
                i--;
                nsize--;
            }
            else
            {
                Imgproc.drawContours(mat5gray2, contours2, i, new Scalar(255,255,255));
//  	  			Imgproc.drawContours(mat, contours2, i, new Scalar(0,255,0));
                // Imgproc.drawContours(mat, contours2, i, new Scalar(0,0,0));
                /**if (area > area_threshold)
                 {
                 List<Point> clist = contours2.get(i).toList();

                 System.out.println("Checking Large Contour C " + i + " at " + clist.get(0).x + ", " + clist.get(0).y);
                 int n = clist.size();
                 double crossProd = 0.0;
                 boolean foundOne = false;
                 int max_j = -1;
                 double maxProd = 0.0;
                 double slope = 0.0;
                 double maxSlope = 0.0;

                 for (int j=0; j<n; j++)
                 {
                 // MLN NEW was -2 +2 ??
                 Point p = clist.get(j);
                 Point pup = clist.get((j+4)%n);
                 Point pdwn = clist.get((j+n-5)%n);
                 double xavg = (pup.x + pdwn.x)*0.5;
                 double yavg = (pup.y + pdwn.y)*0.5;

                 if (Math.abs(p.x - xavg) > 0.0001)
                 slope = (p.y - yavg)/(p.x - xavg);
                 else
                 slope = 1000000.0;

                 crossProd = (p.x - pdwn.x)*(pup.y - p.y) - (p.y - pdwn.y)*(pup.x - p.x);
                 if (crossProd > 4.0) {
                 if (foundOne == false) {
                 foundOne = true;
                 max_j = j;
                 maxProd = crossProd;
                 maxSlope = slope;
                 }
                 else if (maxProd < crossProd) {
                 max_j = j;
                 maxProd = crossProd;
                 maxSlope = slope;
                 }
                 if (j==n-1) {
                 System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                 foundOne = false;
                 }
                 }
                 else
                 {
                 if (foundOne == true)
                 {
                 System.out.println("Local Max Point: " + clist.get(max_j).x + ", " + clist.get(max_j).y + ", crossProd = " + maxProd);
                 foundOne = false;
                 }
                 }
                 }
                 }**/
            }
        }
        double ave_area2 = total_area2/(double) nsize;
        System.out.println("Average area of contours2: " + ave_area2);

        //print("Total number of contours is "+contours2.size());

        setSeedCount(contours2.size()-1);
        //Bitmap mat5gray2Bitmap = Bitmap.createBitmap(mat5gray2.cols(), mat5gray2.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mat5gray2, mat5gray2Bitmap);

        //return mat5gray2Bitmap;
        //return threshBitmap;
    }
    }

    public void writeProcessedImg(String filename) {
        System.out.println(String.format("\nWriting %s", filename));
        Imgcodecs.imwrite(filename, this.mat5gray2);
    }

    public Mat preProcess1(Mat imageMat){
        print("pre process 1 called");
        Mat mat3 = new Mat();
        //print("new mat3 created");
        Core.inRange(imageMat, new Scalar(h1, 0, 0), new Scalar(h2, 255, 255), mat3);
        return mat3;
    }

    public Mat preProcess2(Mat imageMat){
        print("pre process 2 called");

        Mat mat5=new Mat();
        Core.inRange(imageMat, new Scalar(mh1,0,0), new Scalar(mh2,255,255), mat5);

        Mat mat6=new Mat();
        Core.inRange(imageMat, new Scalar(mh3,0,0), new Scalar(mh4,255,255), mat6);

        Mat finalMat = new Mat();
        Core.bitwise_or(mat5, mat6, finalMat);

        //Mat mat4=new Mat();
        //Imgproc.threshold(finalMat, mat4, 0, 255, Imgproc.THRESH_BINARY);

        return finalMat;
    }

    public void setSeedCount(int num){
        seeds = num;
    }

    public int getSeedCount(){
        return seeds;
    }

    public File saveImage(Bitmap ImageToSave) {
        MyDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/Watershed/WatershedImages");
            dir.mkdirs();

            String fileName = "threshBitmap" + MyDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            //  print("File saved at " + path);
            //refreshGallery(outFile);
            //return outFile;

        } catch (FileNotFoundException e) {
            print("FileNotFound exception");
            e.printStackTrace();
        } catch (IOException e) {
            print("IOException");
            e.printStackTrace();
        } finally {

        }return outFile;
    }





    public File saveImage1(Bitmap ImageToSave) {
        MyDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/Watershed/WatershedImages");
            dir.mkdirs();

            String fileName = "TEST" + MyDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            print("File saved at " + path);
            //refreshGallery(outFile);
            //return outFile;

        } catch (FileNotFoundException e) {
            print("FileNotFound exception");
            e.printStackTrace();
        } catch (IOException e) {
            print("IOException");
            e.printStackTrace();
        } finally {

        }return outFile;
    }

    private String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }


    public Boolean checkBinary(Bitmap bitmap){
        int[] pixels = new int[bitmap.getHeight()*bitmap.getWidth()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0,0,bitmap.getWidth(), bitmap.getHeight());
        for(int i=0; i< (bitmap.getWidth()*bitmap.getHeight()); i++){
            if(!((pixels[i] == -16777216) ||(pixels[i]==-1))){
                return false;
            }
        }return true;
    }

    public Bitmap loadImage(){
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Watershed", "Mat4.jpg");
        print(file.getAbsolutePath());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        return bitmap;
    }

    private void print(String s){
        Log.d(TAG, s);
    }
}


