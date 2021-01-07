package org.wheatgenetics.imageprocess

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.euclideanDistance
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.quartileRange
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toMatOfPoint2f
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*


class DetectWithReferences(private val dir: File, private val coinReferenceDiameter: Double): DetectorAlgorithm {

    data class Contour(val x: Double, val y: Double, val minAxis: Double?, val maxAxis: Double?, val area: Double, val count: Int)
    data class Result(val src: Bitmap, val dst: Bitmap, val example: Bitmap, val contours: List<Contour>)

    private fun process(original: Mat): Result {

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

        //convert image to greyscale and blur, this reduces the natural noise in images
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)

        Imgproc.threshold(src, src, 0.0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY_INV)

        Imgproc.morphologyEx(src, src, Imgproc.MORPH_ELLIPSE, kernel, Point(-1.0, -1.0), 3)
//        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 15.0)
//
//        //dilate the image to fill any tiny holes that make contours discontinuous
//        Imgproc.dilate(src, src, kernel, Point(-1.0, -1.0), 6)
//
//        //threshold the image, adaptive might be overkill here because the lightbox ensures a uniform background
//        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)
//
//        //blur to smooth threshed contours, otherwise contours can be jagged
//        Imgproc.GaussianBlur(src, src, Size(3.0, 3.0), 15.0)
//
//        //final dilate to fill holes formed by adaptive threshing/blurring
//        Imgproc.dilate(src, src, kernel, Point(-1.0, -1.0), 3)

        //CHAIN_APPROX_NONE will give more contour points, uses more memory
        //uses RETR_EXTERNAL, skip any hierarchy parsing.
        /**
         * TODO: we can improve this by parsing the hierarchy, sometimes seeds might be 'split'
         *       into multiple contours which are siblings. Look at opencv findcontours docs for more info.
         **/
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        //finds the four contours closest to the corners
        val coins: List<MatOfPoint> = findCoins(contours, imageWidth, imageHeight)

        //from the original contours, filter out the found seeds
        //filter out any duplicates. Sometimes shadows or object features can have distinct continous gradients which causes multiple contours per object.
        //this filter simply checks if the center points are too close and eliminates one.
        val seeds = filterDuplicatesByCenter(contours - coins)

        val dst = original.clone()

        //draw the coins as filled contours green
        Imgproc.drawContours(dst, coins, -1, Scalar(0.0, 255.0, 0.0, 255.0), -1)

        //use interquartile range to partition single seeds from clusters and noise
        val areas = seeds.map { Imgproc.contourArea(it) }
        val perimeters = seeds.map { Imgproc.arcLength(it.toMatOfPoint2f(), true) }

        val perimeterThresh = quartileRange(perimeters.toTypedArray())
        val areaThresh = quartileRange(areas.toTypedArray())

        val singles = arrayListOf<MatOfPoint>()
        val clusters = arrayListOf<MatOfPoint>()

        val maxThresh = (imageHeight*imageWidth*0.05)

        seeds.forEach {

            val area = (Imgproc.contourArea(it))
            val perimeter = Imgproc.arcLength(it.toMatOfPoint2f(), true)

            //interquartile threshing
            if (area > areaThresh.first && area < areaThresh.second
                    && perimeter > perimeterThresh.first && perimeter < perimeterThresh.second) {

                //red
                Imgproc.drawContours(dst, listOf(it), -1, Scalar(255.0, 0.0, 0.0, 255.0), -1)

                singles.add(it)

            } else if (area >= areaThresh.second && area < maxThresh) {

                //blue
                Imgproc.drawContours(dst, listOf(it), -1, Scalar(0.0, 0.0, 255.0, 255.0), -1)

                clusters.add(it)
            }
        }

//        val avgArea = singles.map { Imgproc.contourArea(it) }.reduceRight { x, y -> x + y } / seeds.size

        val coinAreaMilli = Math.PI * (coinReferenceDiameter / 2).pow(2)

        val singleEstimates = estimateSeedArea(singles, coins, coinAreaMilli)
        val clusterEstimates = estimateSeedArea(clusters, coins, coinAreaMilli)

        val axisEstimates = estimateAxis(singles, coins, coinReferenceDiameter)

        //transform the seed/cluster contours to counted/estimated results
        val objects = singles.map {

            val center = it.center()

            val (minAxis, maxAxis) = axisEstimates[it] ?: 0.0 to 0.0

            Contour(center.x, center.y, minAxis, maxAxis, singleEstimates[it] ?: error(""), 1)

        } + clusters.map {

            val center = it.center()

            val box = Imgproc.boundingRect(it)
            val mask = Mat.zeros(original.size(), original.type())
            Imgproc.drawContours(mask, listOf(it), -1, Scalar.all(255.0), -1)
            val count = watershed(Mat(mask, box))

            Contour(center.x, center.y, null, null, clusterEstimates[it] ?: error(""), count)

        }

        val exampleContour = singles.first()
        val mask = Mat.zeros(dst.size(), dst.type())
        Imgproc.drawContours(mask, listOf(exampleContour), -1, Scalar.all(255.0), -1)

        val rect = Imgproc.minAreaRect(exampleContour.toMatOfPoint2f())

        val points = arrayOfNulls<Point>(4)

        rect.points(points)

        val roi = Mat()

        original.copyTo(roi, mask)

//draw axes
//        Imgproc.line(roi, points[0], points[1], Scalar(255.0, 0.0, 255.0, 255.0), 3)
//
//        Imgproc.line(roi, points[1], points[2], Scalar(0.0, 0.0, 255.0, 255.0), 3)

        val example = Mat(roi, rect.boundingRect())

        return Result(original.toBitmap(), dst.toBitmap(), example.toBitmap(), objects)

    }

    //function that returns the min/max axis stats
    private fun MatOfPoint.minMaxAxis(): Pair<Double, Double> {

        val rect = Imgproc.minAreaRect(this.toMatOfPoint2f())

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

    private fun watershed(original: Mat): Int {

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()

        val cropped = original //]Mat(original, rect)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/cropped.png", cropped)

        val gray = Mat()

        Imgproc.cvtColor(cropped, gray, Imgproc.COLOR_BGR2GRAY)

        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 15.0)

        val thresh = Mat()

        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/first_thresh.png", thresh)

        val kernel = Mat.ones(Size(3.0, 3.0), CvType.CV_8U)

        //remove noise
        val opening = Mat()
        Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_ELLIPSE, kernel, Point(-1.0, -1.0), 2)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/opening.png", opening)

        val sure_bg = Mat()
        Imgproc.dilate(opening, sure_bg, kernel, Point(-1.0, -1.0), 3)

        val dt = Mat()
        Imgproc.distanceTransform(opening, dt, Imgproc.DIST_L2, Imgproc.CV_DIST_MASK_5)
        Core.normalize(dt, dt, 0.0, 1.0, Core.NORM_MINMAX)

        val sure_fg = Mat()
        val maxValue = Core.minMaxLoc(dt).maxVal
        Imgproc.threshold(dt, sure_fg, 0.7, 255.0, Imgproc.THRESH_BINARY)
        sure_fg.convertTo(sure_fg, CvType.CV_8U)

        val unknown = Mat()
        Core.subtract(sure_bg, sure_fg, unknown)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/dt.png", dt)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/unknown.png", unknown)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/sure_bg.png", sure_bg)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/sure_fg.png", sure_fg)

        //Imgproc.GaussianBlur(sure_fg, sure_fg, Size(), 5.0)

        Imgproc.findContours(sure_fg, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/${contours.size}COUNTED${UUID.randomUUID()}output.png", sure_fg)

        //subtract the background label and border label
        return contours.size

    }

    /**
     * A simple max-area filtering algorithm that finds and replaces duplicates with whichever has the largest area.
     * Contours with center points within 100px of eachother are considered duplicates.
     */
    private fun filterDuplicatesByCenter(duplicates: List<MatOfPoint>): List<MatOfPoint> {

        val contours = arrayListOf<MatOfPoint>()

        duplicates.forEach { dup ->

            val x = Imgproc.moments(dup)
            val u = Point(x.m10 / x.m00, x.m01 / x.m00)

            val others = contours.filter {

                val y = Imgproc.moments(it)
                val v = Point(y.m10 / y.m00, y.m01 / y.m00)

                euclideanDistance(u, v) < 10
            }

            if (others.isEmpty()) {

                contours.add(dup)

            } else {

                contours.removeAll(others)

                (others + dup).maxByOrNull { Imgproc.contourArea(it) }?.let { bigDup ->

                    contours.add(bigDup)

                }
            }
        }

        return contours

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

                val approxCurve = MatOfPoint2f()

                //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
                //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
                //epsilon is the maximum distance from the contour and the approximation
                val preciseContour = MatOfPoint2f(*contour.toArray())
                val epsilon = 0.009*Imgproc.arcLength(preciseContour, true)

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
    override fun process(inputBitmap: Bitmap?): Result {
        val frame = Mat()
//        val copy = inputBitmap?.copy(inputBitmap.config, true)
        Utils.bitmapToMat(inputBitmap, frame)
        val result = process(frame)
        //Utils.matToBitmap(frame, inputBitmap)
        return result
    }
}