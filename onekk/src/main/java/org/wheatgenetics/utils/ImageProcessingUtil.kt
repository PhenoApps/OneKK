package org.wheatgenetics.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.imageprocess.DetectRectangles
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class ImageProcessingUtil {

    companion object {

        /**
         * Scalar colors require an alpha channel to be opaque
         * Fourth parameter is the alpha channel
         * Not required for Android version < 9
         */
        val COIN_FILL_COLOR = Scalar(0.0, 0.0, 0.0, 255.0)
        val CONTOUR_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
        private val RECTANGLE_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
        private val TEXT_COLOR = Scalar(0.0, 0.0, 0.0, 255.0)

        private fun quartileRange(values: Array<Double>): Pair<Double, Double> {
            val size = values.size
            Arrays.sort(values)
            val Q2 = values[values.size / 2]
            val lowerArray = values.copyOfRange(0, values.size / 2)
            val upperArray = values.copyOfRange(values.size / 2, values.size)
            val Q1 = lowerArray[lowerArray.size / 2]
            val Q3 = upperArray[upperArray.size / 2]
            val IQR = Q3 - Q1
            val firstValue = Q1 - 1.5 * IQR
            val secondValue = Q3 + 1.5 * IQR
            return Pair(firstValue, secondValue)
        }

        fun interquartileReduce(pairs: Array<Detections>): ArrayList<Detections> {
            if (pairs.size > 1) {
                val range = quartileRange(pairs.mapNotNull { it.area }.toTypedArray())
                val output = ArrayList<Detections>()
                for (p in pairs) {
                    if (p.area >= range.first && p.area <= range.second) {
                        output.add(p)
                    }
                }
                return output
            } else return ArrayList(pairs.toList())
        }

        fun calcCircularity(area: Double, peri: Double): Double {
            return 4 * Math.PI * (area / peri.pow(2.0))
        }

        open class PipelineFunction
        class Identity: PipelineFunction()
        class ConvertGrey: PipelineFunction()
        data class AdaptiveThreshold(var maxValue: Double = 255.0,
                                     var type: Int = Imgproc.ADAPTIVE_THRESH_MEAN_C,
                                     var threshType: Int = Imgproc.THRESH_BINARY_INV,
                                     var blockSize: Int = 15,
                                     var C: Double = 10.0): PipelineFunction()

        data class Canny(var maxThresh: Double = 255.0, var minThresh: Double = 255 / 3.0): PipelineFunction()
        data class Dilate(var iterations: Int = 1): PipelineFunction()
        data class Erode(var iterations: Int = 1): PipelineFunction()

        data class GaussianBlur(var size: Size = Size(3.0, 3.0), val sigmaX: Double = 9.0): PipelineFunction()

        data class Detections(var rect: Rect, var circ: Double, var center: Point, var contour: MatOfPoint, var area: Double)

        data class AnalysisResult(var images: ArrayList<Bitmap>,
                                  var detections: ArrayList<Detections>,
                                  var pipeline: ArrayList<PipelineFunction>,
                                  var isCompleted: Boolean = false)
        
        fun measureArea(groundTruthPixel: Double, groundTruthmm: Double, kernelPx: Double): Double {
            return kernelPx * groundTruthmm / groundTruthPixel
        }

        fun similarAreasCheck(areas: List<Double>): Boolean {

            var nextArea = areas.firstOrNull() ?: 0.0

            areas.drop(0).forEach {

                val denom = max(nextArea, it)
                val numer = min(nextArea, it)

                val quot = numer / denom

                if (quot < 0.9) {

                    return false

                }

                nextArea = it
            }

            return true
        }

        //moment calculation: Cx = M10/M00 and Cy = M01/M00
        private fun calculateMomentCenter(contour: Mat): Point = with(Imgproc.moments(contour)) {

            Point(m10 / m00, m01 / m00)

        }

        private fun euclideanDistance(u: Point, v: Point) =
                sqrt((u.x - v.x).pow(2) + (u.y - v.y).pow(2))

        fun centerDifferentForAllRectangles(contour: Mat, rectangles: List<Detections>): List<Detections>? =

                rectangles.filter {

                    euclideanDistance(it.center, calculateMomentCenter(contour)) < 100.0

                }


        fun Mat.toBitmap(): Bitmap = Bitmap.createBitmap(this.width(), this.height(), Bitmap.Config.ARGB_8888).also {
            Utils.matToBitmap(this@toBitmap, it)
        }

        fun MatOfPoint.toMatOfPoint2f(): MatOfPoint2f {

            return MatOfPoint2f().also {

                this.convertTo(it, CvType.CV_32F)

            }
        }
    }
}