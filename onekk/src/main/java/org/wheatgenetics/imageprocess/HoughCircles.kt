package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.os.Environment
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */
class HoughCircles {
    /**
     * Scalar colors require an alpha channel to be opaque
     * Fourth parameter is the alpha channel
     * Not required for Android version < 9
     */
    private val CONTOUR_COLOR = Scalar(255.0, 0.0, 0.0, 255.0)
    private val RECTANGLE_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
    private val TEXT_COLOR = Scalar(0.0, 0.0, 0.0, 255.0)
    private fun writeFile(name: String, img: Mat) {
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().absolutePath + "/OneKK/" + name + ".jpg", img)
    }

    data class Detections(var center: Point, var radius: Int)
    private fun process(src: Mat): ArrayList<Detections> {
        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)
        val circles = Mat()
        val circlesOutput = ArrayList<Detections>()
        Imgproc.HoughCircles(src, circles, Imgproc.CV_HOUGH_GRADIENT, Math.PI / 180, 150.0)
        for (i in 0 until circles.cols()) {
            val data = circles[0, i]
            val center = Point(Math.round(data[0]).toDouble(), Math.round(data[1]).toDouble())
            // circle center
            Imgproc.circle(src, center, 1, Scalar(0.0, 0.0, 255.0), 3, 8, 0)
            // circle outline
            val radius = Math.round(data[2]).toInt()
            Imgproc.circle(src, center, radius, Scalar(0.0, 0.0, 255.0), 3, 8, 0)
            circlesOutput.add(Detections(center, radius))
        }
        return circlesOutput
    }

    fun process(inputBitmap: Bitmap?): ArrayList<Detections> {
        val frame = Mat()
        Utils.bitmapToMat(inputBitmap, frame)
        return process(frame)
        //Utils.matToBitmap(frame, inputBitmap)
    }
}