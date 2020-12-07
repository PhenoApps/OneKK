package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.utils.ImageProcessingUtil
import kotlin.math.pow
import kotlin.math.sqrt
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.AnalysisResult
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Detections
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.PipelineFunction
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Identity
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.ConvertGrey
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.AdaptiveThreshold
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Dilate
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.GaussianBlur
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Erode
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.similarAreasCheck
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toMatOfPoint2f

class DrawSelectedContour {

    private val sSelectedColor = Scalar(0.0, 255.0, 0.0)

    private fun process(original: Mat, x: Double, y: Double, minAxis: Double, maxAxis: Double): Bitmap {

        //original image, center point, radius, perimeter color, perimeter draw thickness
        Imgproc.circle(original, Point(x,y), maxAxis.toInt(), sSelectedColor, 5)

        return Mat(original, Rect(x.toInt()-minAxis.toInt()*2, y.toInt()-minAxis.toInt()*2, minAxis.toInt()+minAxis.toInt()*2, minAxis.toInt()+minAxis.toInt()*2)).toBitmap()

    }

    fun process(inputBitmap: Bitmap?, x: Double, y: Double, minAxis: Double, maxAxis: Double): Bitmap {

        val frame = Mat()

        val copy = inputBitmap?.copy(inputBitmap.config, true)

        Utils.bitmapToMat(copy, frame)

        return process(frame, x, y, minAxis, maxAxis)
    }
}