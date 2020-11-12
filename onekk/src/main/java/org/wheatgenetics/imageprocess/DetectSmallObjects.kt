package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.utils.ImageProcessingUtil
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.centerDifferentForAllRectangles
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.similarAreasCheck
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
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap

class DetectSmallObjects(private val src: Bitmap) {

    private fun process(original: Mat): AnalysisResult {

        val result = AnalysisResult(ArrayList<Bitmap>(), ArrayList<Detections>(), ArrayList<PipelineFunction>())

        val src = original.clone()
        result.images.add(src.clone().toBitmap())
        result.pipeline.add(Identity())

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        val rectangles = ArrayList<Detections>()

        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(ConvertGrey())

        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 1.0);

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(GaussianBlur())

        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(AdaptiveThreshold())
        //Core.bitwise_not(src, src)

        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 1.0);

        //CHAIN_APPROX_NONE will give more contour points, uses more memory
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
        val hull = MatOfInt()

        //Log.d("DrawContours", "${src.width()}x${src.height()} ${contours.size}")

        val color = original.clone()

        for (i in contours.indices) {

            var contour = contours[i]

            val area = Imgproc.contourArea(contour)

            val approxCurve = MatOfPoint2f();

//            Log.d("Area", "${area}")

            //TODO make these preferences or base of resolution of the image
            if (area > 50 && area < 500) {

                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approxCurve, 0.9, true);
                val peri = Imgproc.arcLength(approxCurve, true)
                val circ = ImageProcessingUtil.calcCircularity(area, peri)
               // Log.d("Circularity", "$circ")

                //if (circ >= 0.5) { //0.9) {

                //Imgproc.convexHull(contours[i], hull);

                val rect = Imgproc.boundingRect(contour)
                //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));

                //moment calculation: Cx = M10/M00 and Cy = M01/M00
                val center = Imgproc.moments(contour)
                val centerPoint = Point(center.m10/center.m00, center.m01/center.m00)

                var topLeft = Point(rect.x.toDouble(), rect.y.toDouble())
                var botRight = Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble())

                val newDetection = Detections(rect, circ, centerPoint, contour, area)

                val areas = result.detections.map { it.area } + area

                if (areas.isNotEmpty() && similarAreasCheck(areas)) {

                    rectangles.add(newDetection)

                } else {

                    val otherContours = centerDifferentForAllRectangles(contour, rectangles)

                    rectangles.add(newDetection)

                    otherContours?.forEach {

                        val contours = (otherContours + newDetection)
                        val max = contours.maxByOrNull { it.area }

                        rectangles.removeAll(contours - max)

                    }
                }
            }
        }

        Imgproc.drawContours(color, result.detections.map { MatOfPoint.fromNativeAddr(it.contour.nativeObjAddr) }, -1, ImageProcessingUtil.CONTOUR_COLOR, -1)

        result.images.add(color.toBitmap())

        result.detections = rectangles

        return result
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
    fun process(): AnalysisResult {
        val frame = Mat()
        val copy = src.copy(src.config, true)
        Utils.bitmapToMat(copy, frame)
        val boxes = process(frame)
        Utils.matToBitmap(frame, src)
        return boxes
    }
}