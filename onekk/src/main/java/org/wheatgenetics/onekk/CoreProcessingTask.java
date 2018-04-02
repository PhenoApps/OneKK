package org.wheatgenetics.onekk;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.wheatgenetics.imageprocess.WatershedLB.WatershedLB;
import org.wheatgenetics.onekkUtils.NotificationHelper;

import java.io.File;

import static org.wheatgenetics.onekkUtils.oneKKUtils.makeFileDiscoverable;
import static org.wheatgenetics.onekkUtils.oneKKUtils.postImageDialog;

/**
 * Created by sid on 1/28/18.
 */

public class CoreProcessingTask extends AsyncTask<Bitmap,AsyncTask.Status,Bitmap> {

    private Context context;
    private final WatershedLB watershedLB;
    private final Mat finalMat = new Mat();
    private ProgressDialog progressDialog;
    private int seedCount = 0;
    private String sampleName;
    private String weight;
    private String photoName;
    private Boolean showAnalysis;
    private Boolean backgroundProcessing;
    private int notificationCounter;

    public CoreProcessingTask(Context context, WatershedLB watershedLB, String photoName, Boolean showAnalysis, String sampleName, String weight, int notificationCounter, boolean backgroundProcessing){
        this.context = context;
        this.watershedLB = watershedLB;
        this.photoName = photoName;
        this.showAnalysis = showAnalysis;
        this.sampleName = sampleName;
        this.weight = weight;

        this.progressDialog = new ProgressDialog(context);
        this.progressDialog.setIndeterminate(false);
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressDialog.setCancelable(false);

        this.notificationCounter = notificationCounter;
        this.backgroundProcessing = backgroundProcessing;

        // TODO : add unique identification to implement "cancel processing" feature

        //context.registerReceiver(broadcastReceiver,new IntentFilter("CANCEL"));
    }

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
        displayAlert("Processing...",true);
        Bitmap outputBitmap = this.watershedLB.process(bitmaps[0]);
        seedCount = (int) watershedLB.getNumSeeds();
        System.gc();
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

        if (sampleName.equals("")) {
            displayAlert("Processing finished. \nSeed Count : " + seedCount,false);

            if (showAnalysis) {
                postImageDialog(context,photoName,seedCount);
            }
            return;
        }
        else{
            displayAlert("Adding sample to database...",true);
            Data data = new Data(context);
            data.addSimpleRecord(sampleName,seedCount,weight);
            displayAlert("Processing finished. \nSeed Count : " + seedCount,false);
        }

        if(showAnalysis)
            postImageDialog(context,photoName,seedCount);

        //context.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCancelled(Bitmap bitmap){

        Log.d("WatershedLB Activity", "Cancelled");

        displayAlert("Processing Cancelled",false);

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
