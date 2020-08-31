package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
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

    data class Detections(var rect: Rect, var circ: Double)
    private fun process(src: Mat): ArrayList<Detections> {
        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        val rectangles = ArrayList<Detections>()

        rectangles.add(Detections(Rect(0, 0, src.width(), src.height()), 0.0))
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)
        //Imgproc.GaussianBlur(src, src,  Size(3.0, 3.0), 9.0);
        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 10.0)
       // Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 3.0);


        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        val hull = MatOfInt()

        Log.d("DrawContours", "${src.width()}x${src.height()} ${contours.size}")

        for (i in contours.indices) {
            val area = Imgproc.contourArea(contours[i])
            val approxCurve = MatOfPoint2f();

            Log.d("Area", "${area}")

            if (area > 500) {

                Imgproc.approxPolyDP(MatOfPoint2f(*contours[i].toArray()), approxCurve, 0.1, true);
                val peri = Imgproc.arcLength(approxCurve, true)

                val circ = calcCircularity(area, peri)
                Log.d("Circularity", "${circ}")

                if (circ >= 0.8) {

                    //Imgproc.convexHull(contours[i], hull);

                    val rect = Imgproc.boundingRect(contours[i])
                    //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));

                    var topLeft = Point(rect.x.toDouble(), rect.y.toDouble())
                    var botRight = Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble())

                    rectangles.add(Detections(rect, circ))

                    Imgproc.rectangle(src, topLeft, botRight, RECTANGLE_COLOR, 2)

                    Imgproc.putText(src,
                            "Circularity $circ",
                            Point(rect.x.toDouble(), rect.y.toDouble()),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 4.0, TEXT_COLOR, 3);
                }
            }
        }

            //if (i == 100) break

            //ToDo: Verify that the rows()  is what needs to be used.
       // }
        return rectangles
    }

    fun process(inputBitmap: Bitmap?): ArrayList<Detections> {
        val frame = Mat()
        val copy = inputBitmap?.copy(inputBitmap.config, true)
        Utils.bitmapToMat(copy, frame)
        val boxes = process(frame)
        //Utils.matToBitmap(frame, inputBitmap)
        return boxes
    }


    /*
    OpenCV Debug version, output frame is the opencv result.
     */
//    fun process(inputBitmap: Bitmap?): ArrayList<Detections> {
//        val frame = Mat()
//        val copy = inputBitmap?.copy(inputBitmap.config, true)
//        Utils.bitmapToMat(copy, frame)
//        val boxes = process(frame)
//        Utils.matToBitmap(frame, inputBitmap)
//        return boxes
//    }
}