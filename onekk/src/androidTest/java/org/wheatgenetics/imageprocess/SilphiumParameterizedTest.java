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
public class SilphiumParameterizedTest {
    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"silphium_100.jpg", 100},
                {"silphium_200.jpg", 200},
                {"silphium_300.jpg", 300},
                {"silphium_500.jpg", 500},
                {"silphium_800.jpg", 800},
                {"silphium_1200.jpg", 1200},
                {"silphium_1600.jpg", 1600},
                {"silphium_2000.jpg", 2000}
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
    private ColorThresholding.ColorThresholdParams htParams;
    private WatershedLB.WatershedParams params;
    private Bitmap inputBitmap;
    private Bitmap outputBitmap;
    private Mat outputMat;

    public SilphiumParameterizedTest(String imageName, double expectedSeedCount) {
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
    public void watershedLB_silphium_test() {
        inputBitmap = BitmapFactory.decodeFile(photoPath + imageName);
        outputBitmap = mSeedCounter.process(inputBitmap);
        Utils.bitmapToMat(outputBitmap, outputMat);
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/WatershedImages/" + imageName + "analyzed_test_new.jpg", outputMat);
        seedCount = (int) mSeedCounter.getNumSeeds();
        assertTrue(seedCount == expectedSeedCount);
    }

}
