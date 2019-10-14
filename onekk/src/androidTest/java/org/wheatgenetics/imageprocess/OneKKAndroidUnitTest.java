package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.runner.RunWith;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

import static junit.framework.Assert.assertTrue;

/**
 * Created by sid on 1/10/18.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class OneKKAndroidUnitTest{

    private final int areaLow = Integer.valueOf("400");
    private final int areaHigh = Integer.valueOf("160000");
    private final int defaultRate = Integer.valueOf("34");
    private final double sizeLowerBoundRatio = Double.valueOf("0.25");
    private final double seedSize = 1;
    private final double newSeedDistRatio = Double.valueOf("4.0");

    private int seedCount = 0;
    private boolean setupDoneFlag = false;

    private final int SEEDCOUNT_SOYBEANS = 50;
    private final int SEEDCOUNT_SOYBEANS_OVERLAPPING = 50;
    private int SEEDCOUNT_REDSEEDS = 114;
    private final int SEEDCOUNT_WHEAT = 173;
    private final int SEEDCOUNT_WHEAT_COLORED = 269;
    private final int SEEDCOUNT_MAIZE = 62;
    private final int SEEDCOUNT_SILPHIUM = 45;

    private WatershedLB mSeedCounter;
    private String photoPath;
    private String photoName;
    private String[] imageName;
    private WatershedLB.WatershedParams params;
    private Bitmap inputBitmap;
    private Bitmap outputBitmap;
    private File htFile;
    private Mat outputMat;

    private void setup(){
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
    public void imgprocess1kk_wheat_test(){

        if(!setupDoneFlag)
            setup();

        double refDiam = seedSize; // Wheat default
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/gwheat.jpg";
        photoName = "gwheat.jpg";
        ImageProcess imgP = new ImageProcess(photoPath, photoName,refDiam, true, 0.0, 2.0); //TODO the min/max sizes are bad

        seedCount = imgP.getSeedCount();

        assertTrue(seedCount == SEEDCOUNT_WHEAT_COLORED);
    }

    @org.junit.Test
    public void watershedLB_soybeans_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/soybeans-cropped.jpg";
        photoName = "soybeans-cropped.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/test_soybeans_analyzed.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_SOYBEANS);
    }

    @org.junit.Test
    public void watershedLB_soybeans_overlapping_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/soy_50~2.jpg";
        photoName = "soy_50~2.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/test_soybeans_overlapping_analyzed.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_SOYBEANS_OVERLAPPING);
    }

    @org.junit.Test
    public void watershedLB_maize_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/maize-cropped.jpg";
        photoName = "maize-cropped.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/test_maize_analyzed.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_MAIZE);
    }

    @org.junit.Test
    public void watershedLB_wheat_test(){
        if(!setupDoneFlag)
            setup();
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/wheat-cropped.jpg";
        photoName = "wheat-cropped.jpg";
        inputBitmap = BitmapFactory.decodeFile(photoPath);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap,outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath()+"/WatershedImages/test_wheat_analyzed.jpg",outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == SEEDCOUNT_WHEAT);
    }
}