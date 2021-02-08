package org.wheatgenetics.onekk.interfaces

import android.graphics.Bitmap
import androidx.work.Data
import org.wheatgenetics.imageprocess.DetectWithReferences

interface DetectorAlgorithm {
    companion object {
        val DEFAULT = 0
        val LSS = 1 //large single sample
    }
    data class Contour(val x: Double, val y: Double, val minAxis: Double?, val maxAxis: Double?, val area: Double, val count: Int)
    data class Result(val src: Bitmap, val dst: Bitmap, val example: Bitmap?, val contours: List<Contour>)
    fun process(bitmap: Bitmap?): Result?
}