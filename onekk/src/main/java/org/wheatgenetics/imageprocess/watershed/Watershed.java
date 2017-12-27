package org.wheatgenetics.imageprocess.watershed;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
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
import org.wheatgenetics.onekk.MainActivity;

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
    Context mContext;
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
        File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/Image_input_101.jpg");
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

    public File process(Context mainActivityContext){
        this.mContext = mainActivityContext;
        //if (cropImage) {
        //    this.cropImage();
        //}
        //File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedLB/WatershedImages", "Image_input_101.jpg");

        //print("Path is :"+file1.getAbsolutePath());
        //Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        //Mat imageMat = new Mat();
        //Utils.bitmapToMat(bitmap,imageMat);
        //Mat originalImage = Imgcodecs.imread(file.getAbsolutePath());

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

        Bitmap trans1Bitmap = Bitmap.createBitmap(image1Mat.cols(), image1Mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image1Mat,trans1Bitmap);
        saveImage1(trans1Bitmap, "trans1Bitmap");

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

        Bitmap trans2Bitmap = Bitmap.createBitmap(mat3.cols(), mat3.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat3, trans2Bitmap);
        return saveImage1(trans2Bitmap, "trans2Bitmap");
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
            File dir = new File(MyDir.getAbsolutePath() + "/WatershedImages");
            dir.mkdirs();

            String fileName = "threshBitmap" + MyDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            //  print("File saved at " + path);
            refreshGallery(path);
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





    public File saveImage1(Bitmap ImageToSave,String fileName) {
        MyDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/WatershedImages");
            dir.mkdirs();

            fileName = fileName + MyDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            print("File saved at " + path);
            refreshGallery(path);
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

    private void refreshGallery(String file) {
        MediaScannerConnection.scanFile(this.mContext,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
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


