package org.wheatgenetics.imageprocess

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.utils.ImageProcessingUtil
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.center
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.estimateMillis
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.estimateSeedArea
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.findCoins
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toMatOfPoint2f
import kotlin.collections.ArrayList
import kotlin.math.*


class PotatoDetector(private val context: Context, private val coinReferenceDiameter: Double, private val measure: String): OpenCVTransformation(context), DetectorAlgorithm {

    private fun process(original: Mat): DetectorAlgorithm.Result {

        val kernel = Mat.ones(Size(3.0, 3.0), CvType.CV_8U)

        //upscale small images
        while (original.height() <= 1920) {
            Imgproc.pyrUp(original, original)
        }

        while (original.height() >= 1920) {
            Imgproc.pyrDown(original, original)
        }

        val imageWidth = original.width().toDouble()
        val imageHeight = original.height().toDouble()

        val src = original.clone()

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()

        val dst = original.clone()

        //convert image to greyscale and blur, this reduces the natural noise in images
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2GRAY)

        Imgproc.threshold(dst, dst, 0.0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY_INV)

        //better results with 8 iterations but much slower
        Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_ELLIPSE, kernel, Point(-1.0, -1.0), 5)

        /**
         * TODO: we can improve this by parsing the hierarchy, sometimes seeds might be 'split'
         *       into multiple contours which are siblings. Look at opencv findcontours docs for more info.
         **/
        Imgproc.findContours(dst, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        //finds the four contours closest to the corners
        val coins = findCoins(contours, imageWidth, imageHeight)
        val coin = coins.asSequence().sortedByDescending { Imgproc.contourArea(it) }.first()
        val avgCoinDiameter = coins.map { it.minMaxAxis().second }.reduceRight { x, y -> x + y } / coins.size

        //draw the coins as filled contours green
        Imgproc.drawContours(src, listOf(coin), 0, Scalar(0.0, 255.0, 0.0, 255.0), -1)

        val largestSample = findLargestCenter(coins, contours-coins)

        val approxSample = listOf(largestSample)

        //red
        Imgproc.drawContours(src, approxSample, 0, Scalar(255.0, 0.0, 0.0, 255.0), -1)

//        val avgArea = singles.map { Imgproc.contourArea(it) }.reduceRight { x, y -> x + y } / seeds.size

        val coinAreaMilli = Math.PI * (coinReferenceDiameter / 2).pow(2)

        val singleEstimates = estimateSeedArea(approxSample, listOf(coin), coinAreaMilli)

        val axisEstimates = estimateAxis(approxSample, listOf(coin), coinReferenceDiameter)

        //transform the seed/cluster contours to counted/estimated results
        val objects = approxSample.map {

            val center = it.center()

            val (minAxis, maxAxis) = if (measure == "0") {
                val rect = Imgproc.minAreaRect(it.toMatOfPoint2f())
                val points = Array(4) { Point () }
                rect.points(points)
                Imgproc.drawContours(src, listOf(MatOfPoint(*points)), -1, Scalar(255.0, 255.0, 0.0), 3)
                axisEstimates[it] ?: 0.0 to 0.0

            } else if (measure == "1") {

                val lw = ImageProcessingUtil.measureSmartGrain(src, it)
                val maxAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, lw.first)
                val minAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, lw.second)

                minAxis to maxAxis
            } else {

                val l = ImageProcessingUtil.findLongestLine(it.toArray(), arrayOf())
                Imgproc.line(src, l.first, l.second, Scalar(0.0, 255.0, 0.0), 2) // length

                val d = sqrt((l.first.x-l.second.x).pow(2.0) + (l.first.y-l.second.y).pow(2.0))
                val w = ImageProcessingUtil.carrotMethod(src, it, d, 50.0)

                val maxAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, d)
                val minAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, w)

                minAxis to maxAxis
            }

            DetectorAlgorithm.Contour(center.x, center.y, minAxis, maxAxis, singleEstimates[it]
                    ?: error(""), 1)

        }

        val originalOutput = original.toBitmap()
        original.release()

        val dstOutput = src.toBitmap()
        dst.release()

        original.release()
        src.release()

        return DetectorAlgorithm.Result(originalOutput, dstOutput, null, objects)

    }

    private fun findLargestCenter(coins: List<MatOfPoint>, list: List<MatOfPoint>): MatOfPoint {

        list.sortedByDescending { Imgproc.contourArea(it) }.forEach { contour ->

            if (calculateRoi(coins)?.contains(contour.center()) == true) {

                return contour
            }

        }

        return list.first() //if no contours are found just return the first
    }

    /**
     * Function that takes as input a list of 4 coins (indices defined in function)
     * A bounding box is created from the edges of the coins s.a no coins are visible.
     */
    private fun calculateRoi(coins: List<MatOfPoint>): Rect? {

        val topLeft = 0
        //val topRight = 1
        //val bottomLeft = 2
        val bottomRight = 3

        return if (coins.size == 4) {

            val tl = coins[topLeft].center()
            val br = coins[bottomRight].center()

            //return a bounding rectangle between the top left and bottom right points
            //make a small adjustment for the diameter of the coins
            val rA = sqrt(Imgproc.contourArea(coins[topLeft]))
            val rB = sqrt(Imgproc.contourArea(coins[bottomRight]))

            val pointA = Point(tl.x + rA, tl.y + rA)

            val pointB = Point(br.x - rB, br.y - rB)

            Rect(pointA, pointB)

        } else null
    }

    //function that returns the min/max axis stats
    private fun MatOfPoint.minMaxAxis(ellipse: Boolean = false): Pair<Double, Double> {

        val rect = if (!ellipse) Imgproc.minAreaRect(this.toMatOfPoint2f())
            else Imgproc.fitEllipse(this.toMatOfPoint2f())

        val minAxis = min(rect.size.width, rect.size.height)

        val maxAxis = max(rect.size.width, rect.size.height)

        return minAxis to maxAxis
    }

    /**
     * Uses the known reference measurements (e.g a penny is 19.05 mm in diameter) to estimate the found contour areas.
     */
    private fun estimateAxis(contours: List<MatOfPoint>, coins: List<MatOfPoint>, coinDiameterMilli: Double): Map<MatOfPoint, Pair<Double, Double>> {

        val avgCoinDiameter = coins.map { it.minMaxAxis().second }.reduceRight { x, y -> x + y } / coins.size

        return contours.associateWith {
            estimateMillis(avgCoinDiameter, coinDiameterMilli, it.minMaxAxis().first) to
                    estimateMillis(avgCoinDiameter, coinDiameterMilli, it.minMaxAxis().second)
        }
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
    override fun process(bitmap: Bitmap?): DetectorAlgorithm.Result {

        val frame = Mat()

        Utils.bitmapToMat(bitmap, frame)

        val result = process(frame)

        frame.release()

        return result
    }
}