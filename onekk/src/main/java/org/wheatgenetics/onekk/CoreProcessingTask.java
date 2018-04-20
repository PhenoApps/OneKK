package org.wheatgenetics.onekk;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TimingLogger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.wheatgenetics.database.Data;
import org.wheatgenetics.imageprocess.ColorThreshold.ColorThresholding;
import org.wheatgenetics.imageprocess.ImgProcess1KK.MeasureSeeds;
import org.wheatgenetics.imageprocess.WatershedLB.WatershedLB;
import org.wheatgenetics.onekkUtils.Constants;
import org.wheatgenetics.onekkUtils.NotificationHelper;
import org.wheatgenetics.onekkUtils.oneKKUtils;

import java.io.File;

import static org.wheatgenetics.onekkUtils.oneKKUtils.makeFileDiscoverable;
import static org.wheatgenetics.onekkUtils.oneKKUtils.postImageDialog;

/**
 * Created by sid on 1/28/18.
 */

public class CoreProcessingTask extends AsyncTask<Bitmap,AsyncTask.Status,Bitmap> {

    private Context context;
    private Data data = null;
    private final Mat finalMat = new Mat();
    private ProgressDialog progressDialog;
    private int seedCount = 0;
    private String sampleName;
    private String firstName;
    private String lastName;
    private String weight;
    private String photoName;
    private Boolean showAnalysis;
    private Boolean backgroundProcessing;
    private int notificationCounter;
    private double coinSize;
    private CoinRecognitionTask coinRecognitionTask;
    private ColorThresholding.ColorThresholdParams ctParams;
    private WatershedLB watershedLB;

    public CoreProcessingTask(Context context, ColorThresholding.ColorThresholdParams ctParams, String photoName,
                              Boolean showAnalysis, String sampleName, String firstName,
                              String lastName, String weight, int notificationCounter,
                              boolean backgroundProcessing, double coinSize){
        this.context = context;
        this.ctParams = ctParams;
        this.photoName = photoName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.showAnalysis = showAnalysis;
        this.sampleName = sampleName;
        this.weight = weight;

        this.progressDialog = new ProgressDialog(context);
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setCancelable(false);

        this.notificationCounter = notificationCounter;
        this.backgroundProcessing = backgroundProcessing;
        this.coinSize = coinSize;

        // TODO : add unique identification to implement "cancel processing" feature

        //context.registerReceiver(broadcastReceiver,new IntentFilter("CANCEL"));
    }

    /* written for cancel feature */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("WatershedLB Task","Cancel message received");

            if(!isCancelled())
                displayAlert("Cancelling...",true);

            if(intent.getBooleanExtra("CANCEL",false))
                cancel(true);

            goAsync();
        }
    };

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        displayAlert("In queue...",true);
    }

    @Override
    protected Bitmap doInBackground(Bitmap... bitmaps){
        /* the first bitmap consists of the image */
        Bitmap outputBitmap = bitmaps[0];

        /*
        *  adb shell setprop log.tag.<TAGNAME> VERBOSE
        *
        *  TAGNAME = CoreProcessing
        *
        *  To see the timings in the console make sure you run this command
        *
        *  adb shell setprop log.tag.CoreProcessing VERBOSE
        *
        */
        TimingLogger timingLogger = new TimingLogger("CoreProcessing", sampleName);
        Mat tempMat = new Mat();

        displayAlert("Coin Recognition...",true);

        /* convert the bitmap to a mat and start coin recognition */
        Utils.bitmapToMat(bitmaps[0],tempMat);
        coinRecognitionTask = new CoinRecognitionTask(coinSize);
        coinRecognitionTask.process(tempMat);

        /* once the hueProcess is complete check for coin constraints */
        boolean coinsRecognized = coinRecognitionTask.checkConstraints();

        timingLogger.addSplit("Coin Recognition");

        /* if all the 4 coins are recognized and satisfy the constraints then start image processing */
        if(coinsRecognized) {

            /* get the coins masked mat from coin recognition */
            tempMat = coinRecognitionTask.getProcessedMat();

            /* as the new mat is cropped bounding the image to the coins, we create a new bitmap */
            Bitmap croppedBitmap = Bitmap.createBitmap(tempMat.cols(),tempMat.rows(),bitmaps[0].getConfig());
            Utils.matToBitmap(tempMat,croppedBitmap);

            /* if the color threshold parameters object is null, indicates that color thresholding
            * is not required, else uses the parameters to perform the thresholding */
            if(ctParams != null) {
                final ColorThresholding colorThresholding = new ColorThresholding(ctParams);

                colorThresholding.labProcess(croppedBitmap);

                croppedBitmap = colorThresholding.getProcessedBitmap();

                timingLogger.addSplit("Thresholding");
            }

            /* start the watershed light box processing */
            watershedLB = new WatershedLB();
            displayAlert("Processing...", true);
            outputBitmap = this.watershedLB.process(croppedBitmap);

            timingLogger.addSplit("Image Analysis");
            seedCount = (int) watershedLB.getNumSeeds();

            /* once the processing is complete we characterize the seeds */

            /* Open CV based approach*/

            /*MSeeds mSeeds = new MSeeds(coinRecognitionTask.getPixelMetric(), watershedLB.getProcessedMat());
            mSeeds.process(watershedLB.getSeedArrayList());
            Utils.matToBitmap(mSeeds.getmSeedsProcessedMat(),outputBitmap);*/

            /* Trevor's implementation */
            MeasureSeeds measureSeeds = new MeasureSeeds();
            measureSeeds.measureSeeds(watershedLB.getSeedContours(),coinRecognitionTask.getPixelMetric());

            Log.d("Seed Measurements", measureSeeds.getList().toString());
            timingLogger.addSplit("Measure Seeds");

            /* if the sample name is provided the data is added to the database */
            if (!(sampleName.equals(""))) {
                displayAlert("Adding sample to database...", true);

                data = new Data(context);
                //data.addRecords(sampleName, photoName, firstName, lastName, seedCount, weight, mSeeds.getmSeedsArrayList());

                data.addRecords(sampleName,photoName,firstName,lastName,seedCount,weight,measureSeeds.getList());
            }
            timingLogger.addSplit("Store data");

            System.gc();
        }

        /* if all the coins are not detected or fail any of the constraint checks then that particular
        * processing is cancelled
        */
        else{
            cancel(true);
        }
        timingLogger.dumpToLog();
        Log.d("CoreProcessing : End", oneKKUtils.getDate());

        return outputBitmap;
    }

    protected void onPostExecute(Bitmap bitmap){
        super.onPostExecute(bitmap);

        displayAlert("Saving processed image...",true);
        //if(isCancelled())
        //    onCancelled(null);
        Utils.bitmapToMat(bitmap,finalMat);

        Imgcodecs.imwrite(Constants.ANALYZED_PHOTO_PATH.toString() + "/analyzed_new.jpg",finalMat);
        Imgcodecs.imwrite(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg",finalMat);

        makeFileDiscoverable(new File(Constants.ANALYZED_PHOTO_PATH.toString() + "/" + photoName + "_new.jpg"), context);

        if(!(sampleName.equals(""))){
            data.createNewTableEntry(sampleName,String.valueOf(seedCount));
        }
        displayAlert("Processing finished. \nSeed Count : " + seedCount,false);

        if (showAnalysis) {
                postImageDialog(context,photoName,seedCount);
        }
        data.getLastData();
        //context.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCancelled(Bitmap bitmap){

        Log.d("WatershedLB Activity", "Cancelled");

        displayAlert("Processing Cancelled : " + coinRecognitionTask.getSTATUS(),false);

        //context.unregisterReceiver(broadcastReceiver);
    }

    private void displayAlert(String text, boolean showAlert){
        if(backgroundProcessing)
            NotificationHelper.notify(context, sampleName,text,this.notificationCounter,showAlert);
        else
            if(showAlert)
                if(!progressDialog.isShowing()){
                    progressDialog.setMessage(text);
                    progressDialog.show();
                }
                else
                    progressDialog.setMessage(text);
            else
                progressDialog.dismiss();
    }
}
