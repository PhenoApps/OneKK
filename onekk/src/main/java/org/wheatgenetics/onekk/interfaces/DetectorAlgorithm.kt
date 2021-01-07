package org.wheatgenetics.onekk.interfaces

import android.graphics.Bitmap
import org.wheatgenetics.imageprocess.DetectWithReferences

interface DetectorAlgorithm {
    fun process(bitmap: Bitmap?): DetectWithReferences.Result
}