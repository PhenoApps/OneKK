package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.bumptech.glide.request.transition.BitmapContainerTransitionFactory
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */
class DetectRectangles {
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

    private fun calcCircularity(area: Double, peri: Double): Double {
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

    data class GaussianBlur(var size: Size = Size(3.0, 3.0), val sigmaX: Double = 9.0): PipelineFunction()

    data class Detections(var rect: Rect, var circ: Double)

    data class AnalysisResult(var images: ArrayList<Bitmap>,
                              var detections: ArrayList<Detections>,
                              var pipeline: ArrayList<PipelineFunction>)

    private fun process(src: Mat): AnalysisResult {

        val result = AnalysisResult(ArrayList<Bitmap>(), ArrayList<Detections>(), ArrayList<PipelineFunction>())

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(Identity())

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        val rectangles = ArrayList<Detections>()

//        Core.bitwise_not(src, src)

//        rectangles.add(Detections(Rect(0, 0, src.width(), src.height()), 0.0))
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(ConvertGrey())

        Imgproc.GaussianBlur(src, src,  Size(3.0, 3.0), 9.0);

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(GaussianBlur())

//        var blur = Mat()
//        Imgproc.GaussianBlur(src, blur, Size(1.0, 1.0), 3.0)
//        Core.addWeighted(src, 1.5, blur, -0.5, 0.0, src)
//
//        result.images.add(src.clone().toBitmap())
//
//        Imgproc.GaussianBlur(src, blur, Size(1.0, 1.0), 3.0)
//        Core.addWeighted(src, 1.5, blur, -0.5, 0.0, src)
//
//        result.images.add(src.clone().toBitmap())

//        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2Lab)
//
//        result.images.add(src.clone().toBitmap())
//
//        var mats = mutableListOf<Mat>()
//
//        Core.split(src, mats)

//        Imgproc.adaptiveThreshold(mats[0], src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)

        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)

        result.images.add(src.clone().toBitmap())
        result.pipeline.add(AdaptiveThreshold())
        //Core.bitwise_not(src, src)

        //FLOOD FILL
//        var flood = src.clone()
//        var size = src.size()
//        var mask = Mat.zeros(Size(size.width+2, size.height+2), src.type())
//
//        Imgproc.floodFill(flood, mask, Point(0.0,0.0), Scalar(255.0,255.0,255.0))
//
//        Core.bitwise_not(flood, flood)
//
//        Core.bitwise_or(src, flood, src)
        //FLOOD FILL END



       // Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 3.0);

        //CHAIN_APPROX_NONE will give more contour points, uses more memory
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
        val hull = MatOfInt()

        Log.d("DrawContours", "${src.width()}x${src.height()} ${contours.size}")

        for (i in contours.indices) {

            var contour = contours[i]

            val area = Imgproc.contourArea(contour)

            val approxCurve = MatOfPoint2f();

//            Log.d("Area", "${area}")

            if (area > 1000) {

                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approxCurve, 0.9, true);
                val peri = Imgproc.arcLength(approxCurve, true)
                val circ = calcCircularity(area, peri)
    //                Log.d("Circularity", "${circ}")

                if (circ >= 0.9) {

                    //Imgproc.convexHull(contours[i], hull);

                    val rect = Imgproc.boundingRect(contour)
                    //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));

                    var topLeft = Point(rect.x.toDouble(), rect.y.toDouble())
                    var botRight = Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble())

                    rectangles.add(Detections(rect, circ))

//                    Imgproc.rectangle(src, topLeft, botRight, RECTANGLE_COLOR, 2)
//
//                    result.images.add(src.clone().toBitmap())


                    //                    Imgproc.putText(src,
    //                            "Circularity $circ",
    //                            Point(rect.x.toDouble(), rect.y.toDouble()),
    //                            Imgproc.FONT_HERSHEY_SIMPLEX, 4.0, TEXT_COLOR, 3);
                }
            }

            if (rectangles.size == 4) break

        }

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


    fun Mat.toBitmap() = Bitmap.createBitmap(this.width(), this.height(), Bitmap.Config.ARGB_8888).also {
        Utils.matToBitmap(this@toBitmap, it)
    }

    /*
    OpenCV Debug version, output frame is the opencv result.
     */
    fun process(inputBitmap: Bitmap?): AnalysisResult {
        val frame = Mat()
        val copy = inputBitmap?.copy(inputBitmap.config, true)
        Utils.bitmapToMat(copy, frame)
        val boxes = process(frame)
        Utils.matToBitmap(frame, inputBitmap)
        return boxes
    }
}