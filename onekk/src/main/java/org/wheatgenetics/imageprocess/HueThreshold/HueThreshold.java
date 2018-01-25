package org.wheatgenetics.imageprocess.HueThreshold;

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

import static org.wheatgenetics.onekk.BuildConfig.DEBUG;

public class HueThreshold {

    private HueThresholdParams mparams;
    private static final String TAG = "Hue Thresholding";
    public static String MyDate;
    String path;

    /**
     * This class consists of variables and methods used to setup hue thresholding parameters
     *
     */
    public static class HueThresholdParams{
        protected double threshold;
        protected int min_hue, max_hue;

        /**
         * Constructor to initialize the hue thresholding parameters before processing
         * <p>
         *  This is a convenience for calling
         * {@link org.wheatgenetics.imageprocess.HueThreshold.HueThreshold.HueThresholdParams#HueThresholdParams(double, int, int)}.
         * </p>
         *
         *  @param min_hue minimum hue value to be used for thresholding
         *  @param max_hue maximum hue value to be used for thresholding
         */
        public HueThresholdParams(double thresh, int min_hue, int max_hue){
            this.threshold = thresh;
            this.min_hue = min_hue;
            this.max_hue = max_hue;
        }

        public double getThreshold() {
            return threshold;
        }

        public int getMin_hue() {
            return min_hue;
        }

        public int getMax_hue() {
            return max_hue;
        }

        public void setThreshold(double threshold){
            this.threshold = threshold;
        }

        public void setHue(int min_hue, int max_hue){
            this.min_hue = min_hue;
            this.max_hue = max_hue;
        }
    }

    /**
     * Hue Threshold constructor to setup the hue thresholding parameters
     * <p>
     *  This is a convenience for calling
     * {@link org.wheatgenetics.imageprocess.HueThreshold.HueThreshold#HueThreshold(HueThresholdParams)}.
     * </p>
     *
     */
    public HueThreshold(HueThresholdParams params){
        this.mparams = params;
    }

    /**
     * Method to invoke the hue thresholding before passing the file to Watershed light box processing
     * <p>
     *  This is a convenience for calling
     * {@link org.wheatgenetics.imageprocess.HueThreshold.HueThreshold#process(Bitmap)}.
     * </p>
     *
     *
     * @return a processed file after performing the hue thresholding with the user specified parameters
     */
    public File process(Bitmap inputBitmap){

        Log.d("Process","Image available for processing");
        Mat frame = new Mat();
        Utils.bitmapToMat(inputBitmap,frame);
        Imgproc.cvtColor(frame,frame,Imgproc.COLOR_BGR2RGB);
        Mat image1Mat = new Mat();
        Imgproc.cvtColor(frame,image1Mat, Imgproc.COLOR_BGR2HSV);
        Log.d("Process","Original Image Mat converted to HSV");

        if(DEBUG) {
            Bitmap trans1Bitmap = Bitmap.createBitmap(image1Mat.cols(), image1Mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image1Mat, trans1Bitmap);
            saveImage(trans1Bitmap, "trans1Bitmap");
        }

        Mat mat3 = preProcess(image1Mat);

        Log.i(TAG,"Hue thresholding completed");
        Bitmap hueThreshold = Bitmap.createBitmap(mat3.cols(), mat3.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat3, hueThreshold);
        return saveImage(hueThreshold, "hueThreshold");
    }

    public Mat preProcess(Mat imageMat){
        Mat mat3 = new Mat();
        Core.inRange(imageMat, new Scalar(mparams.getMin_hue(), 0, 0), new Scalar(mparams.getMax_hue(), 255, 255), mat3);
        Log.d(TAG,"PreProcess1 : Mat3 with user hue values created");
        return mat3;
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


