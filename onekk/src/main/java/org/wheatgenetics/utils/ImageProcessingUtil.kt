package org.wheatgenetics.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
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


        var seedContours = ArrayList<MatOfPoint?>()


        fun quartileRange(values: Array<Double>): Pair<Double, Double> {
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

        /**
         * Computes the circularity of an object using the area and perimeter.
         */
        fun calcCircularity(area: Double, peri: Double):Double {
            return 4.0 * (Math.PI) * (area / (Math.pow(peri, 2.0)))
        }

        fun ComputeMaxContour(contours: ArrayList<MatOfPoint?>): Int{
            var maxArea = 0.0
            var maxContour = 0
            for(i in contours.indices){
                var area = Imgproc.contourArea(contours.get(i))
                if(maxArea < area){
                    maxArea = area
                    maxContour = i
                }
            }
            return maxContour
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

        open class PipelineFunction
        class Identity: PipelineFunction()
        class ConvertGrey: PipelineFunction()
        data class AdaptiveThreshold(var maxValue: Double = 255.0,
                                     var type: Int = Imgproc.ADAPTIVE_THRESH_MEAN_C,
                                     var threshType: Int = Imgproc.THRESH_BINARY_INV,
                                     var blockSize: Int = 15,
                                     var C: Double = 10.0): PipelineFunction()

        data class Threshold(var c: Double = 0.0,
                             var maxValue: Double = 255.0,
                             var type: Int = Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU
        ): PipelineFunction()

        data class DrawContours(var color: Scalar = Scalar(200.0, 200.0, 0.0, 255.0),
                                var thickness: Int = 4
        ): PipelineFunction()

        data class MedianBlur(var ksize: Int = 9): PipelineFunction()

        data class Canny(var maxThresh: Double = 255.0, var minThresh: Double = 255 / 3.0): PipelineFunction()
        data class Dilate(var iterations: Int = 1): PipelineFunction()
        data class Erode(var iterations: Int = 1): PipelineFunction()
        data class DistanceTransform(var distanceType: Int = Imgproc.DIST_L2, var maskSize: Int = Imgproc.DIST_MASK_5): PipelineFunction()

        data class GaussianBlur(var size: Size = Size(3.0, 3.0), val sigmaX: Double = 9.0): PipelineFunction()

        data class Detections(var rect: Rect, var circ: Double, var center: Point, var contour: MatOfPoint, var area: Double)

        data class ContourStats(var x: Double, var y: Double, var count: Int, var area: Double, var minAxis: Int, var maxAxis: Int, var isCluster: Boolean)

        data class AnalysisResult(var images: ArrayList<Bitmap> = ArrayList(),
                                  var detections: ArrayList<Detections> = ArrayList(),
                                  var pipeline: ArrayList<PipelineFunction> = ArrayList(),
                                  var isCompleted: Boolean = false)

        data class ContourResult(var images: ArrayList<Bitmap> = ArrayList(),
                                  var detections: ArrayList<ContourStats> = ArrayList(),
                                  var pipeline: ArrayList<PipelineFunction> = ArrayList(),
                                  var isCompleted: Boolean = false)

        data class UserTunableParams(var circThreshold: Double = 0.8,
                                     var areaThreshold: Double = 8000.0,
                                     var maxContourSizeThreshold: Int = 500000
        )


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
        fun calculateMomentCenter(contour: Mat): Point = with(Imgproc.moments(contour)) {

            Point(m10 / m00, m01 / m00)

        }

        fun euclideanDistance(u: Point, v: Point) =
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