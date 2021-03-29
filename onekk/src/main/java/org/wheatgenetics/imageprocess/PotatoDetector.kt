package org.wheatgenetics.imageprocess

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.euclideanDistance
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toMatOfPoint2f
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*


class PotatoDetector(context: Context, private val coinReferenceDiameter: Double): OpenCVTransformation(context), DetectorAlgorithm {

    private fun process(original: Mat): DetectorAlgorithm.Result {

        val kernel = Mat.ones(Size(3.0, 3.0), CvType.CV_8U)

        //upscale small images
        while (original.height() <= 1920) {
            Imgproc.pyrUp(original, original)
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
        val coin = coins.sortedByDescending { Imgproc.contourArea(it) }.first()

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

            val (minAxis, maxAxis) = axisEstimates[it] ?: 0.0 to 0.0

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

        val TOP_LEFT = 0
        val TOP_RIGHT = 1
        val BOTTOM_LEFT = 2
        val BOTTOM_RIGHT = 3

        return if (coins.size == 4) {

            val tl = coins[TOP_LEFT].center()
            val br = coins[BOTTOM_RIGHT].center()

            //return a bounding rectangle between the top left and bottom right points
            //make a small adjustment for the diameter of the coins
            val rA = sqrt(Imgproc.contourArea(coins[TOP_LEFT]))
            val rB = sqrt(Imgproc.contourArea(coins[BOTTOM_RIGHT]))

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

    //function that returns the center point of a contour
    private fun MatOfPoint.center() = with(Imgproc.moments(this@center)) {
        Point(m10 / m00, m01 / m00)
    }

    /**
     * Uses cross multiplication to convert a measurement in px to a millimeters.
     */
    private fun estimateMillis(groundTruthPixels: Double, groundTruthMillis: Double, unknown: Double) = unknown * groundTruthMillis / groundTruthPixels

    /**
     * Uses the known reference measurements (e.g a penny is 19.05 mm in diameter) to estimate the found contour areas.
     */
    private fun estimateSeedArea(contours: List<MatOfPoint>, coins: List<MatOfPoint>, coinAreaMilli: Double): Map<MatOfPoint, Double> {

        val avgCoinArea = coins.map { Imgproc.contourArea(it) }.reduceRight { x, y -> x + y } / coins.size

        return contours.associateWith { estimateMillis(avgCoinArea, coinAreaMilli, Imgproc.contourArea(it)) }

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

    private fun MatOfPoint.approximate(e: Double) = MatOfPoint().apply {
        //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
        //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
        //epsilon is the maximum distance from the contour and the approximation
        val preciseContour = MatOfPoint2f(*this@approximate.toArray())
        val epsilon = e * Imgproc.arcLength(preciseContour, true)

        val approx = MatOfPoint2f()

        Imgproc.approxPolyDP(preciseContour, approx, epsilon, true)

        approx.convertTo(this, CvType.CV_32S)
    }

    /**
     * Greedy algorithm that takes the contours of an image and partitions them into coins and seed classes.
     * Coins are defined as the four contours closest to the the corners of the image. There will always be four.
     * Seeds are everything other than the coins, these could be clusters of seeds still.
     */
    private fun findCoins(contours: List<MatOfPoint>, imageWidth: Double, imageHeight: Double): List<MatOfPoint> {

        //initialize the greedy parameters with null values
        val coins = arrayOfNulls<Pair<MatOfPoint, Point>>(4)
        val TOP_LEFT = 0
        val TOP_RIGHT = 1
        val BOTTOM_LEFT = 2
        val BOTTOM_RIGHT = 3

        //set the starting assumptions to be opposite corners.
        //e.g TOP_LEFT is set to the bottom right corner, any other contour should be closer to the TOP_LEFT corner
        if (contours.size >= 4) {

            coins[TOP_LEFT] = contours[0] to Point(imageWidth, imageHeight)
            coins[TOP_RIGHT] = contours[1] to Point(0.0, imageHeight)
            coins[BOTTOM_LEFT] = contours[2] to Point(imageWidth, 0.0)
            coins[BOTTOM_RIGHT] = contours[3] to Point(0.0, 0.0)

            //search through and approximate all contours,
            //update the coins based on how close they are to the respective corners
            for (i in contours.indices) {

                val contour = contours[i]// sortedContours[i]

                //TODO: helps ignore grid/noise but this shouldn't be constant preferably.
                if (Imgproc.contourArea(contour) > 500) {

                    val approxCurve = MatOfPoint2f()

                    //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
                    //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
                    //epsilon is the maximum distance from the contour and the approximation
                    val preciseContour = MatOfPoint2f(*contour.toArray())
                    val epsilon = 0.009 * Imgproc.arcLength(preciseContour, true)

                    Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true)

                    //TODO put center point function in ImageProcessingUtils
                    //moment calculation: Cx = M10/M00 and Cy = M01/M00
                    val center = Imgproc.moments(approxCurve)
                    val centerPoint = Point(center.m10 / center.m00, center.m01 / center.m00)

                    //start of the four greedy search updates, checks if this contour's center point
                    //is closer to the corner than the previous
                    coins[TOP_RIGHT]?.let {
                        if (euclideanDistance(centerPoint, Point(imageWidth, 0.0)) <
                                euclideanDistance(it.second, Point(imageWidth, 0.0))) {
                            coins[TOP_RIGHT] = contour to centerPoint
                        }
                    }

                    coins[TOP_LEFT]?.let {
                        if (euclideanDistance(centerPoint, Point(0.0, 0.0)) <
                                euclideanDistance(it.second, Point(0.0, 0.0))) {
                            coins[TOP_LEFT] = contour to centerPoint
                        }
                    }

                    coins[BOTTOM_LEFT]?.let {
                        if (euclideanDistance(centerPoint, Point(0.0, imageHeight)) <
                                euclideanDistance(it.second, Point(0.0, imageHeight))) {
                            coins[BOTTOM_LEFT] = contour to centerPoint
                        }
                    }

                    coins[BOTTOM_RIGHT]?.let {
                        if (euclideanDistance(centerPoint, Point(imageWidth, imageHeight)) <
                                euclideanDistance(it.second, Point(imageWidth, imageHeight))) {
                            coins[BOTTOM_RIGHT] = contour to centerPoint
                        }
                    }
                }
            }

            return coins.mapNotNull { it?.first }
        }

        return listOf()
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