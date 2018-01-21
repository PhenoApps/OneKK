package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.runner.RunWith;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.wheatgenetics.imageprocess.HueThreshold.HueThreshold;
import org.wheatgenetics.imageprocess.watershedLB.*;

import java.io.File;

import static junit.framework.Assert.assertTrue;

/**
 * Created by sid on 1/10/18.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class watershedLBAndroidUnitTest{

    protected int areaLow = Integer.valueOf("400");
    protected int areaHigh = Integer.valueOf("160000");
    protected int defaultRate = Integer.valueOf("34");
    protected double sizeLowerBoundRatio = Double.valueOf("0.25");
    protected double newSeedDistRatio = Double.valueOf("4.0");
    protected double threshold = Double.valueOf("1");
    protected int min_hueValue = Integer.valueOf("30");
    protected int max_hueValue = Integer.valueOf("255");
    protected int seedCount = 0;
    public boolean setupDoneFlag = false;

    protected int SEEDCOUNT_SOYBEANS = 31;
    protected int SEEDCOUNT_REDSEEDS = 114;
    protected int SEEDCOUNT_WHEAT = 260; //267
    protected int SEEDCOUNT_MAIZE = 30;

    protected WatershedLB mSeedCounter;
    protected String photoPath;
    protected String photoName;
    protected HueThreshold.HueThresholdParams htParams;
    protected WatershedLB.WatershedParams params;
    protected Bitmap inputBitmap;
    protected Bitmap outputBitmap;
    protected File htFile;
    protected Mat outputMat;

    protected void setup(){
        if (!OpenCVLoader.initDebug()) {
            Log.d("Test Case", "Open CV Setup: FAILED");
            System.exit(0);
        } else {
            Log.d("Test Case", "Open CV Setup: SUCCESS");
            setupDoneFlag = true;
            outputMat = new Mat();
        }

        params = new WatershedLB.WatershedParams(areaLow, areaHigh, defaultRate, sizeLowerBoundRatio, newSeedDistRatio);
        mSeedCounter = new WatershedLB(params);
    }


    @org.junit.Test
    public void ht_watershedLB_wheat_test(){
        if(!setupDoneFlag)
            setup();

        /** Setup the parameters required for Hue Thresholding
         */

        htParams = new HueThreshold.HueThresholdParams(threshold,min_hueValue,max_hueValue);
        final HueThreshold hueThreshold = new HueThreshold(htParams);
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/wheat.jpg";
        photoName = "wheat.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        htFile = hueThreshold.process(inputBitmap);

        /** Convert the file to URI to pass to Watershed Light Box to keep the process
         * same in other tests*/

        Uri imageUri = Uri.fromFile(htFile);

        /** Perform Watershed Light box using the image post hue thresholding
         */

        photoPath = imageUri.getPath();
        photoName = imageUri.getLastPathSegment();
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        //Imgproc.cvtColor(outputMat,outputMat,Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/analyzed_test_new.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_WHEAT);
    }

    @org.junit.Test
    public void watershedLB_soybeans_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/soybeans.jpg";
        photoName = "soybeans.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        //Imgproc.cvtColor(outputMat,outputMat,Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/analyzed_test_new.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_SOYBEANS);
    }

    @org.junit.Test
    public void watershedLB_redseeds_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/redseeds.jpg";
        photoName = "redseeds.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        //Imgproc.cvtColor(outputMat,outputMat,Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/analyzed_test_new.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_REDSEEDS);
    }

    @org.junit.Test
    public void watershedLB_maize_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/NOT_MASK.jpg";
        photoName = "NOT_MASK.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        //Imgproc.cvtColor(outputMat,outputMat,Imgproc.COLOR_BGR2RGB);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/analyzed_test_new.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_MAIZE);
    }
}