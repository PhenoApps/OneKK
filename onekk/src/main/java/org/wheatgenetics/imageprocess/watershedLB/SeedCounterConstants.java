package org.wheatgenetics.imageprocess.watershedLB;

import android.Manifest;

/**
 * Created by Chaney on 8/9/2017.
 */

class SeedCounterConstants {

    final static String[] permissions = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    //requests
    final static int PERM_REQUEST = 101;
    final static int LOAD_REQUEST = 100;

    //extras
    final static String FILE_PATH_EXTRA = "edu.ksu.wheatgenetics.seedcounter.FILE_URI_EXTRA";
}
