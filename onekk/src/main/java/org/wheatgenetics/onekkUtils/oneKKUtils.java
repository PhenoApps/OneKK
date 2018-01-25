package org.wheatgenetics.onekkUtils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.wheatgenetics.onekk.Constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sid on 1/22/18.
 */

public class oneKKUtils {

    /**
     * Writes the passed Mat with the file name provided
     * <p>
     * {@link oneKKUtils#writeMat2File(Mat, String)}.
     * </p>
     */
    public static void writeMat2File(Mat inputMat, String filename) {
        Log.d("","Writing file with filename " + filename);
        Imgcodecs.imwrite(filename, inputMat);
    }

    /**
     * Makes the saved file discoverable in Gallery/File Manager
     * <p>
     * {@link oneKKUtils#makeFileDiscoverable(File, Context)}.
     * </p>
     */
    public static void makeFileDiscoverable(File file, Context context) {
        MediaScannerConnection.scanFile(context,
                new String[]{file.getPath()}, null, null);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)));
    }

    /**
     * Generates a Media file with specified type and name
     * <p>
     * {@link oneKKUtils#getOutputMediaFile(int, String)}.
     * </p>
     */
    public static File getOutputMediaFile(int type,String fileName) {
        File mediaFile = new File(Constants.PHOTO_PATH, fileName + "IMG_" + getDate() + ".jpg");
        return mediaFile;
    }

    /**
     * Returns a string of current date and time in the format yyyy-MM-dd-hh-mm-ss
     * <p>
     * {@link oneKKUtils#getDate()}.
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
     * {@link oneKKUtils#stringDecimal(String)}.
     * </p>
     */
    public static String stringDecimal(String input) {
        if (!input.equals("null")) {
            return String.format("%.2f", Double.parseDouble(input));
        }

        return "null";
    }
}
