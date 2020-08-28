package org.wheatgenetics.imageprocess;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import androidx.test.filters.SmallTest;
import android.util.Log;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.Arrays;

import static junit.framework.Assert.assertTrue;

/**
 * Created by sid on 2/11/18.
 */
@RunWith(Parameterized.class)
@SmallTest
public class SoybeansParameterizedTest {
    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"soy_50.jpg", 50},
                {"soy_100.jpg", 100},
                {"soy_222.jpg", 222},
                {"soy_332.jpg", 332},
                {"soy_416.jpg", 416}
        });
    }


    private int areaLow = Integer.valueOf("400");
    private int areaHigh = Integer.valueOf("160000");
    private int defaultRate = Integer.valueOf("34");
    private double sizeLowerBoundRatio = Double.valueOf("0.25");
    private double newSeedDistRatio = Double.valueOf("4.0");
    private int seedCount = 0;
    private double expectedSeedCount = 0;
    private WatershedLB mSeedCounter;
    private String photoPath;
    private String imageName;
    private WatershedLB.WatershedParams params;
    private Bitmap inputBitmap;
    private Bitmap outputBitmap;
    private Mat outputMat;

    public SoybeansParameterizedTest(String imageName, double expectedSeedCount) {
        this.imageName = imageName;
        this.expectedSeedCount = expectedSeedCount;
    }

    @Before
    public void setup() {
        if (!OpenCVLoader.initDebug()) {
            Log.d("Test Case", "Open CV Setup: FAILED");
            System.exit(0);
        } else {
            Log.d("Test Case", "Open CV Setup: SUCCESS");
            outputMat = new Mat();
        }

        params = new WatershedLB.WatershedParams(areaLow, areaHigh, defaultRate, sizeLowerBoundRatio, newSeedDistRatio);
        mSeedCounter = new WatershedLB(params);
        photoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/";
    }

    @org.junit.Test
    public void watershedLB_soybeans_test() {
        inputBitmap = BitmapFactory.decodeFile(photoPath + imageName);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap, outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/WatershedImages/" + imageName + "analyzed_test_new.jpg", outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == expectedSeedCount);
    }

}
