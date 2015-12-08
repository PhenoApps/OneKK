package org.wheatgenetics.onekk;

import android.os.Environment;

import java.io.File;

public class Constants {

    public static File MAIN_PATH = new File(Environment.getExternalStorageDirectory(), "1KK");

    public static File EXPORT_PATH = new File(Environment.getExternalStorageDirectory(), "1KK/Export");

    public static File PHOTO_PATH = new File(Environment.getExternalStorageDirectory(), "1KK/Photos");

    public static File ANALYZED_PHOTO_PATH = new File(Environment.getExternalStorageDirectory(), "1KK/AnalyzedPhotos");

}