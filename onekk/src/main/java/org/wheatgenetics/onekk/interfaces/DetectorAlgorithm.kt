package org.wheatgenetics.onekk.interfaces

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.wheatgenetics.imageprocess.DetectWithReferences

interface DetectorAlgorithm {
    fun process(bitmap: Bitmap?): DetectWithReferences.Result
}