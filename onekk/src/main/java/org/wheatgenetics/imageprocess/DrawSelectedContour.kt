package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap

class DrawSelectedContour {

    private val boundary = 256

    private val sSelectedColor = Scalar(0.0, 255.0, 0.0)

    private fun process(original: Mat, x: Double, y: Double, cluster: Boolean, minAxis: Double, maxAxis: Double): Bitmap {

        //original image, center point, radius, perimeter color, perimeter draw thickness
        Imgproc.circle(original, Point(x,y), boundary/2, sSelectedColor, 5)

        return when (cluster) {
            false -> Mat(original, Rect(x.toInt()-boundary/2, y.toInt()-boundary/2, boundary, boundary)).toBitmap()
            true -> Mat(original, Rect(x.toInt()-boundary*2, y.toInt()-boundary*2, boundary*4, boundary*4)).toBitmap()
        }

    }

    fun process(inputBitmap: Bitmap?, x: Double, y: Double, cluster: Boolean, minAxis: Double, maxAxis: Double): Bitmap {

        val frame = Mat()

        val copy = inputBitmap?.copy(inputBitmap.config, true)

        Utils.bitmapToMat(copy, frame)

        return process(frame, x, y, cluster, minAxis, maxAxis)
    }
}