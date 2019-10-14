package org.wheatgenetics.utils;

import android.os.Environment;

import java.io.File;

public class Constants {

    public static File MAIN_PATH = new File(Environment.getExternalStorageDirectory(), "OneKK");

    public static File EXPORT_PATH = new File(Environment.getExternalStorageDirectory(), "OneKK/Export");

    public static File PHOTO_PATH = new File(Environment.getExternalStorageDirectory(), "OneKK/Photos");

    public static File PHOTO_SAMPLES_PATH = new File(Environment.getExternalStorageDirectory(), "OneKK/Photos/Samples");

    public static File ANALYZED_PHOTO_PATH = new File(Environment.getExternalStorageDirectory(), "OneKK/AnalyzedPhotos");

}