package org.wheatgenetics.imageprocess.ColorThreshold;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2Lab;
import static org.opencv.imgproc.Imgproc.COLOR_Lab2BGR;

public class ColorThresholding {

    private ColorThresholdParams mparams;
    private static final String TAG = "Color Thresholding";
    private Bitmap processedBitmap;

    /**
     * This class consists of variables and methods used to setup hue thresholding parameters
     *
     */
    public static class ColorThresholdParams {
        protected double threshold;
        protected int lowerBound, upperBound;

        /**
         * Constructor to initialize the hue thresholding parameters before processing
         * <p>
         *  This is a convenience for calling
         * {@link ColorThresholdParams#ColorThresholdParams(int, int, int)}.
         * </p>
         *
         *  @param lowerBound minimum hue value to be used for thresholding
         *  @param upperBound maximum hue value to be used for thresholding
         */
        public ColorThresholdParams(int thresh, int lowerBound, int upperBound){
            this.threshold = thresh;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        public double getThreshold() {
            return threshold;
        }

        public int getLowerBound() {
            return lowerBound;
        }

        public int getUpperBound() {
            return upperBound;
        }

        public void setThreshold(double threshold){
            this.threshold = threshold;
        }

        public void setHue(int lowerBound, int upperBound){
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }
    }

    /**
     * Color Threshold constructor to setup the color thresholding parameters
     * <p>
     *  This is a convenience for calling
     * {@link ColorThresholding#ColorThresholding(ColorThresholdParams)}.
     * </p>
     *
     */
    public ColorThresholding(ColorThresholdParams params){
        this.mparams = params;
    }

    /**
     * Method to invoke the hue thresholding before passing the file to Watershed light box processing
     * <p>
     *  This is a convenience for calling
     * {@link ColorThresholding#hueProcess(Bitmap)}.
     * </p>
     *
     *
     * @return a processed file after performing the hue thresholding with the user specified parameters
     */
    public File hueProcess(Bitmap inputBitmap){

        Log.d("Process","Image available for processing");

        Mat frame = new Mat();
        Utils.bitmapToMat(inputBitmap,frame);
        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_BGR2RGB);

        Mat image1Mat = new Mat();
        Imgproc.cvtColor(frame,image1Mat, Imgproc.COLOR_BGR2HSV);
        Log.d("Process","Original Image Mat converted to HSV");

        Mat mat3 = new Mat();
        Core.inRange(image1Mat, new Scalar(mparams.getLowerBound(), 0, 0), new Scalar(mparams.getUpperBound(), 255, 255), mat3);
        Log.i(TAG,"Hue thresholding completed");

        Bitmap hueThreshold = Bitmap.createBitmap(mat3.cols(), mat3.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat3, hueThreshold);

        return saveImage(hueThreshold, "hueThreshold");
    }

    /**
     * Method to invoke the lab thresholding before passing the file to Watershed light box processing
     * <p>
     *  This is a convenience for calling
     * {@link ColorThresholding#labProcess(Bitmap)}.
     * </p>
     *
     *
     * @return a processed file after performing the lab thresholding with the user specified parameters
     */
    public File labProcess(Bitmap inputBitmap){

        Mat inputMat = new Mat();
        Mat processedMat = new Mat();
        Utils.bitmapToMat(inputBitmap,inputMat);

        Mat labMat = new Mat();
        Imgproc.cvtColor(inputMat,labMat,COLOR_BGR2Lab);

        Mat mask = new Mat();
        Core.inRange(labMat,new Scalar(0,0,116), new Scalar(255,255,255),mask);
        Core.bitwise_and(labMat,labMat,processedMat,mask);

        Imgproc.cvtColor(processedMat,processedMat,COLOR_Lab2BGR);
        Imgproc.cvtColor(processedMat,processedMat,Imgproc.COLOR_BGR2GRAY);

        Imgproc.threshold(processedMat,processedMat,62,255,Imgproc.THRESH_BINARY);
        //Imgproc.cvtColor(processedMat,processedMat,COLOR_BGR2RGB);
        Log.d("Process","Original Image Mat converted to Lab");

        processedBitmap = Bitmap.createBitmap(processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(processedMat, processedBitmap);
        return saveImage(processedBitmap, "colorThreshold");
    }

    public Bitmap getProcessedBitmap() {
        return processedBitmap;
    }

    /**
     * Method to save a bitmap as an image with a specified filename
     *
     * @param ImageToSave - bitmap to be saved
     * @param fileName - name of the file in string format
     *
     * @return saved image file
     */
    public File saveImage(Bitmap ImageToSave,String fileName) {
        String myDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/WatershedImages");
            dir.mkdirs();

            fileName = fileName + myDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            String path = outFile.getAbsolutePath();
            Log.i("Save Image","Image saved at " + path);
            //refreshGallery(path);
            //return outFile;
        } catch (FileNotFoundException e) {
            Log.e("Save Image","File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Save Image","Unable to perform IO operation");
            e.printStackTrace();
        } finally {

        }

        return outFile;
    }

    /** Method to get the current date and time in
     * string format used in naming and saving the generated images */
    private String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }
}