package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.core.util.Pair
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.util.*

/**
 * Created by chaneylc and venkat on 1/16/2020.
 */
class Thresh {

    private fun calcCircularity(area: Double, peri: Double): Double {
        return 4 * Math.PI * (area / Math.pow(peri, 2.0))
    }

    private fun <T : Number?> calcStdDev(values: List<T>): Double {
        var mean = 0.0
        for (x in values) mean += x!!.toDouble()
        mean = mean / values.size
        Log.d("CoinRecMean", mean.toString())
        var variance = 0.0
        for (x in values) {
            Log.d("CoinRecVal", x.toString())
            variance += Math.pow(x!!.toDouble() - mean, 2.0)
        }
        variance /= values.size.toDouble()
        Log.d("CoinRecVar", variance.toString())
        return Math.sqrt(variance)
    }

    private fun measureArea(groundTruthAreaPixel: Double, groundTruthAreamm: Double, kernelAreaPx: Double): Double {
        return kernelAreaPx * groundTruthAreamm / groundTruthAreaPixel
    }

    private fun findMax(mat: Mat): Double {
        // Initializing max element as INT_MIN
        var maxElement = Int.MIN_VALUE.toDouble()

        // checking each element of matrix
        // if it is greater than maxElement,
        // update maxElement
        for (i in 0 until mat.rows()) {
            for (j in 0 until mat.cols()) {
                if (mat[i, j][0] > maxElement) {
                    maxElement = mat[i, j][0]
                }
            }
        }

        // finally return maxElement
        return maxElement
    }

    private fun toDoubleArray(vals: DoubleArray): Array<Double?> {
        val output = arrayOfNulls<Double>(vals.size)
        for (i in vals.indices) {
            output[i] = vals[i]
        }
        return output
    }

    private fun <T : Number?> Mean(values: Array<T>): Float {
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]!!.toDouble()
        }
        return (sum / values.size).toFloat()
    }

    private fun quartileRange(values: ArrayList<Double>): Pair<Double, Double> {
        val size = values.size
        return if (size > 0) {
            val doubleValues = values.toArray(arrayOf<Double>())
            Arrays.sort(doubleValues)
            val Q2 = doubleValues[doubleValues.size / 2]
            val lowerArray = Arrays.copyOfRange(doubleValues, 0, doubleValues.size / 2)
            val upperArray = Arrays.copyOfRange(doubleValues, doubleValues.size / 2, doubleValues.size)
            if (lowerArray.size > 0 && upperArray.size > 0) {
                val Q1 = lowerArray[lowerArray.size / 2]
                val Q3 = upperArray[upperArray.size / 2]
                val IQR = Q3 - Q1
                val firstValue = Q1 - 1.5 * IQR
                val secondValue = Q3 + 1.5 * IQR
                return Pair(firstValue, secondValue)
            }
            Pair(doubleValues[0], doubleValues[doubleValues.size - 1])
        } else {
            Pair(0.0, 0.0)
        }
    }

    private fun InterquartileReduce(pairs: List<Pair<Double, MatOfPoint>?>): ArrayList<Pair<Double, MatOfPoint>?> {
        val areas = ArrayList<Double>()
        for (p in pairs) {
            areas.add(p!!.first)
        }
        val range = quartileRange(areas)
        val output = ArrayList<Pair<Double, MatOfPoint>?>()
        for (p in pairs) {
            if (p!!.first >= range.first && p.first <= range.second) {
                output.add(p)
            }
        }
        return output
    }

    private inner class GroundTruths internal constructor(var contours: List<MatOfPoint>, var stdev: Double, var defectMean: Int, var coins: ArrayList<MatOfPoint>)

    private fun logMat(m: Mat) {
        for (r in 0 until m.rows()) {
            for (c in 0 until m.cols()) {
                Log.d("PIXEL", m[r, c][0].toString())
            }
        }
    }

    private fun writeFile(name: String, img: Mat) {
        Imgcodecs.imwrite(Environment.getExternalStorageDirectory().absolutePath + "/OneKK/" + name + ".jpg", img)
    }

    private fun process(src: Mat): Mat {
        val start = System.currentTimeMillis()

        //Mat copy = src.clone();
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)
        //Imgproc.GaussianBlur(copy, copy, new Size(3, 3), 5);
        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 35, 8.0)
        //Imgproc.threshold(src, src, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        // Imgproc.threshold(src, src, 0, 128, Imgproc.THRESH_BINARY_INV);


//        Mat mask = Mat.zeros(copy.size(), CvType.CV_8U);
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//
//
//        Imgproc.findContours(copy, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        MatOfInt hull = new MatOfInt();
//        for(int i = 0; i < contours.size(); i++){
//
//            Imgproc.convexHull(contours.get(i), hull);
//
//            //ToDo: Verify that the rows()  is what needs to be used.
//            if(contours.get(i).rows()/ hull.rows() <= 3){
//                Imgproc.drawContours(src, contours, i, new Scalar(128, 128, 128, 255), 1);
//            }
//
//        }

        //Imgproc.medianBlur(mask, mask, 9);

        //Imgproc.Canny(mask, mask, 200, 255);

        //writeFile("canny", mask);

//        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        //Imgproc.cvtColor(gray, gray, Imgproc.COLOR_BGR2GRAY);
//
//        for (int i = 0; i < contours.size(); i++) {
//
//            Imgproc.drawContours(src, contours, i, new Scalar(255, 255, 255), -1);
//
//        }
////
//        return gray;

        //Imgproc.cvtColor(mask, mask, Imgproc.COLOR_GRAY2BGR);
        return src
    }

    fun process(inputBitmap: Bitmap?) {
        val frame = Mat()
        Utils.bitmapToMat(inputBitmap, frame)
        process(frame)
        Utils.matToBitmap(frame, inputBitmap)
    }
}