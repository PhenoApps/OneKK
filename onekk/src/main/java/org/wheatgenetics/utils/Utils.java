package org.wheatgenetics.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.wheatgenetics.onekk.R;
import org.wheatgenetics.ui.TouchImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sid on 1/22/18.
 */

public class Utils {

    /**
     * Writes the passed Mat with the file name provided
     * <p>
     * {@link Utils#writeMat2File(Mat, String)}.
     * </p>
     */
    public static void writeMat2File(Mat inputMat, String filename) {
        Log.d("", "Writing file with filename " + filename);
        Imgcodecs.imwrite(filename, inputMat);
    }

    /**
     * Makes the saved file discoverable in Gallery/File Manager
     * <p>
     * {@link Utils#makeFileDiscoverable(File, Context)}.
     * </p>
     */
    public static void makeFileDiscoverable(File file, Context context) {
        MediaScannerConnection.scanFile(context,
                new String[]{file.getPath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)));
    }

    /**
     * Generates a Media file with specified type and name
     * <p>
     * {@link Utils#getOutputMediaFile(int, String)}.
     * </p>
     */
    public static File getOutputMediaFile(int type, String fileName) {
        File mediaFile = new File(Constants.PHOTO_PATH, fileName + "IMG_" + getDate() + ".jpg");
        return mediaFile;
    }

    /**
     * Returns a string of current date and time in the format yyyy-MM-dd-hh-mm-ss
     * <p>
     * {@link Utils#getDate()}.
     * </p>
     */
    public static String getDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat date = new SimpleDateFormat(
                "yyyy-MM-dd-hh-mm-ss", Locale.getDefault());
        return date.format(cal.getTime());
    }

    /**
     * Returns a double formatted string
     * <p>
     * {@link Utils#stringDecimal(String)}.
     * </p>
     */
    public static String stringDecimal(String input) {
        if (!input.equals("null")) {
            if (!input.equals("NA"))
                return String.format("%.2f", Double.parseDouble(input));
        }

        return "null";
    }

    public static Float adjustFontSize(String text) {
        int fontSize = text.length();
        if (fontSize > 15) {
            float size = ((float) (fontSize) - 10f) / 2;
            return (20.0f - size);
        } else
            return (15.0f);
    }

    public static void postImageDialog(Context context, String imageName, int numberOfSeeds) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        final View personView = inflater.inflate(R.layout.post_image, new LinearLayout(context), false);
        final TextView tv = (TextView) personView.findViewById(R.id.tvSeedCount);
        Typeface myTypeFace = Typeface.createFromAsset(context.getAssets(), "AllerDisplay.ttf");
        tv.setTypeface(myTypeFace);
        tv.setText("Seed Count : " + numberOfSeeds);
        File imgFile = new File(Constants.ANALYZED_PHOTO_PATH, imageName + "_new.jpg");

        if (imgFile.exists()) {
            TouchImageView imgView = (TouchImageView) personView.findViewById(R.id.postImage);
            Bitmap bmImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rbmImg = Bitmap.createBitmap(bmImg, 0, 0, bmImg.getWidth(), bmImg.getHeight(), matrix, true); //TODO change preview size to avoid out of memory errors

            imgView.setImageBitmap(rbmImg);
        }

        alert.setCancelable(true);
        alert.setView(personView);
        alert.setNegativeButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }
}
