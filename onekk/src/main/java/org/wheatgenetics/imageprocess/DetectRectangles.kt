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

class DetectRectangles {

    companion object {

        val EXPECTED_NUMBER_OF_COINS = 4
    }

    private fun process(original: Mat): AnalysisResult? {

        var epsilon: Double?

        val result = AnalysisResult(ArrayList<Bitmap>(), ArrayList<Detections>(), ArrayList<PipelineFunction>())

        val src = original.clone()
        result.images.add(src.clone().toBitmap())
        result.pipeline.add(Identity())

        //calculate the area of the image to estimate the seed min/max thresholds
        val width = src.width()
        val height = src.height()
        val pictureArea = width * height

        val maxCoinAreaThresh = pictureArea*0.025

        //Log.d("MINIMUM COIN THRESH", minCoinAreaThresh.toString())
       // Log.d("MAX COIN THRESH", maxCoinAreaThresh.toString())

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        var rectangles = ArrayList<Detections>()

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(ConvertGrey())

        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(AdaptiveThreshold())

        //CHAIN_APPROX_NONE will give more contour points, uses more memory
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
        val hull = MatOfInt()

        //Log.d("DrawContours", "${src.width()}x${src.height()} ${contours.size}")

        val color = original.clone()

        val sortedContours = contours.sortedByDescending { Imgproc.contourArea(it) }
        for (i in sortedContours.indices) {

            var contour = sortedContours[i]

            val area = Imgproc.contourArea(contour)

            val approxCurve = MatOfPoint2f();

            //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
            //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
            //epsilon is the maximum distance from the contour and the approximation
            val preciseContour = MatOfPoint2f(*contour.toArray())
            epsilon = 0.009*Imgproc.arcLength(preciseContour, true)

            Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true);
            val peri = Imgproc.arcLength(approxCurve, true)
            val circ = ImageProcessingUtil.calcCircularity(area, peri)
            //Log.d("Circularity", "$circ")

            if (circ >= 0.9 && area > 1000) {

                //Imgproc.convexHull(contours[i], hull);

                val rect = Imgproc.boundingRect(approxCurve)
                //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));

                //TODO put center point function in ImageProcessingUtils
                //moment calculation: Cx = M10/M00 and Cy = M01/M00
                val center = Imgproc.moments(approxCurve)
                val centerPoint = Point(center.m10/center.m00, center.m01/center.m00)

//                var topLeft = Point(rect.x.toDouble(), rect.y.toDouble())
//                var botRight = Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble())

                val otherContours = ImageProcessingUtil.centerDifferentForAllRectangles(approxCurve, rectangles)

                val newDetection = Detections(rect, circ, centerPoint, MatOfPoint(*approxCurve.toArray()), area)

                rectangles.add(newDetection)

                otherContours?.forEach { other ->

                    val choices = (otherContours + newDetection)
                    val max = choices.maxByOrNull { other.area }

                    rectangles.removeAll(choices - max)

                }
            }

            rectangles = ImageProcessingUtil.interquartileReduce(rectangles.toTypedArray())

            val areas = rectangles.map { it.area }
            
            if (rectangles.size == EXPECTED_NUMBER_OF_COINS) {

                val dst = color.clone()
                //draw contours documentation, fill in all detected coins
                //https://docs.opencv.org/3.4/d6/d6e/group__imgproc__draw.html#ga746c0625f1781f1ffc9056259103edbc

                Imgproc.drawContours(dst, rectangles.map { it.contour }, -1, ImageProcessingUtil.COIN_FILL_COLOR, -1)

                result.images.add(dst.toBitmap())

//                val ellipses = color.clone()
//
//                rectangles.map { it.contour }.forEach {
//                    Imgproc.fitEllipse(MatOfPoint2f(*it.toArray())).also { rotatedRect ->
//                        Imgproc.circle(ellipses, rotatedRect.center, 5, ImageProcessingUtil.COIN_FILL_COLOR)
//                    }
//                }
//
//                result.images.add(ellipses.toBitmap())
                println("epsilon: $epsilon")

                result.isCompleted = true

                result.detections = rectangles

                break
            }

        }

        return if (result.isCompleted) {

            result

        } else null

    }

//    fun process(inputBitmap: Bitmap?): ArrayList<Detections> {
//        val frame = Mat()
//        //val copy = inputBitmap?.copy(inputBitmap.config, true)
//        Utils.bitmapToMat(inputBitmap, frame)
//        val boxes = process(frame)
//        //Utils.matToBitmap(frame, inputBitmap)
//        return boxes
//    }

    /*
    OpenCV Debug version, output frame is the opencv result.
     */
    fun process(inputBitmap: Bitmap?): AnalysisResult? {
        val frame = Mat()
        val copy = inputBitmap?.copy(inputBitmap.config, true)
        Utils.bitmapToMat(copy, frame)
        val boxes = process(frame)
        Utils.matToBitmap(frame, inputBitmap)
        return boxes
    }
}