package org.wheatgenetics.utils

import android.graphics.Bitmap
import android.util.Log
import math.geom2d.Point2D
import math.geom2d.line.Line2D
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

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
            //val size = values.size
            Arrays.sort(values)
            //val Q2 = values[values.size / 2]
            val lowerArray = values.copyOfRange(0, values.size / 2)
            val upperArray = values.copyOfRange(values.size / 2, values.size)
            val Q1 = lowerArray[lowerArray.size / 2]
            val q3 = upperArray[upperArray.size / 2]
            val iqr = q3 - Q1
            val firstValue = Q1 - 1.5 * iqr
            val secondValue = q3 + 1.5 * iqr
            return Pair(firstValue.coerceAtLeast(50.0), secondValue)
        }

        /**
         * Computes the circularity of an object using the area and perimeter.
         */
        fun calcCircularity(area: Double, peri: Double):Double {
            return 4.0 * (Math.PI) * (area / (peri.pow(2.0)))
        }

        fun interquartileReduce(pairs: Array<Detections>): ArrayList<Detections> {
            return if (pairs.size > 1) {
                val range = quartileRange(pairs.map { it.area }.toTypedArray())
                val output = ArrayList<Detections>()
                for (p in pairs) {
                    if (p.area >= range.first && p.area <= range.second) {
                        output.add(p)
                    }
                }
                output
            } else ArrayList(pairs.toList())
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

        fun centerDifferentForAllRectangles(contour: Mat, rectangles: List<Detections>): List<Detections> =

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

        fun findLongestLine(contour: Array<Point>, ignore: Array<Line2D>): Pair<Point, Point> {
            var length = 0.0
            var A = Point()
            var B = Point()
            for (i in contour.indices) {
                for (j in i until contour.size) {
                    val p1 = contour[i]
                    val p2 = contour[j]
                    val l = sqrt((p1.x - p2.x).pow(2.0) + (p1.y - p2.y).pow(2.0))
                    if (l > length && !ignore.any { it.p1 == p1 && it.p2 == p2 }) {
                        length = l
                        A = p1
                        B = p2
                    }
                }
            }

            return A to B
        }

        /**
         * Skeletonizes the object's contour and finds the min axis based on
         * perpendicular lines to the skeleton.
         */
        fun carrotMethod(dst: Mat, contour: MatOfPoint, maxAxis: Double, minAxisThresh: Double): Double {

            //approximate the contour slightly
            val approxCurve = MatOfPoint2f()
            val preciseContour = MatOfPoint2f(*contour.toArray())
            val epsilon = 0.0005*Imgproc.arcLength(preciseContour, true)
            Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true)

            //draw object contour onto temporary mat
            val copy = Mat.zeros(dst.size(), CvType.CV_8UC1)
            Imgproc.drawContours(copy, listOf(MatOfPoint(*approxCurve.toArray())), -1, Scalar(255.0), -1)

            //iteratively thin the contour
            val element = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, Size(3.0, 3.0))
            val eroded = Mat()
            val temp = Mat(copy.rows(), copy.cols(), CvType.CV_8UC1, Scalar(0.0))
            val skel = Mat(temp.rows(), temp.cols(), CvType.CV_8UC1, Scalar(0.0))
            val size = dst.cols() * dst.rows()
            var zeros = 0
            var done = false
            while (!done) {
                Imgproc.erode(copy, eroded, element)
                Imgproc.dilate(eroded, temp, element)
                Core.subtract(copy, temp, temp)
                Core.bitwise_or(skel, temp, skel)
                eroded.copyTo(copy)
                zeros = size - Core.countNonZero(copy)
                if (zeros == size) done = true
            }

            //dilate and close the skeleton a bit
            val kernel = Mat.ones(Size(2.0, 2.0), CvType.CV_8U)
            Imgproc.dilate(skel, skel, kernel)
            Imgproc.morphologyEx(skel, skel, Imgproc.MORPH_CLOSE, kernel)
            Imgproc.blur(skel, skel, Size(3.0, 3.0))

            return findMinAxis(dst, skel, preciseContour, maxAxis, minAxisThresh)
        }

        fun distanceTransformMethod(dst: Mat, contour: MatOfPoint, maxAxis: Double, minAxisThresh: Double): Double {

            val approxCurve = MatOfPoint2f()
            val preciseContour = MatOfPoint2f(*contour.toArray())
            val epsilon = 0.005 * Imgproc.arcLength(preciseContour, true)
            Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true)

            val copy = Mat.zeros(dst.size(), CvType.CV_8UC1)

            Imgproc.drawContours(
                copy,
                listOf(MatOfPoint(*approxCurve.toArray())),
                -1,
                Scalar(255.0),
                -1
            )

            Imgproc.distanceTransform(copy, copy, Imgproc.DIST_L2, Imgproc.CV_DIST_MASK_3)
            copy.convertTo(copy, CvType.CV_8UC1)
            val sureFg = Mat()
            val maxValue = Core.minMaxLoc(copy).maxVal
            Imgproc.threshold(copy, sureFg, maxValue - 10, 255.0, Imgproc.THRESH_BINARY)

            sureFg.convertTo(sureFg, CvType.CV_8UC1)

            return findMinAxis(dst, sureFg, preciseContour, maxAxis, minAxisThresh)
        }

        /**
         * Find min axis will find and draw on the dst image the min axis of the inputted contour.
         * The function takes the original image to draw on, the 'skeleton' of the contour,
         * the approximated object's contour, and the max axis length.
         * The function uses a line detector to find lines on the skeleton Mat, from here
         * perpendicular lines are maximized across the lines, but are only maximized within the object's contour.
         *
         * @param dst is the destination image to draw on
         * @param contour is the Mat to find lines on
         * @param approx is the original object contour
         * @param maxAxis is the length of the longest line between any points on the contour
         */
        private fun findMinAxis(dst: Mat, contour: Mat, approx: MatOfPoint2f, maxAxis: Double, minAxisThresh: Double): Double {

            //use the center of mass to filter out detected lines that are forks on edges of contour
            val center = MatOfPoint(*approx.toArray()).center()
            val cx = center.x
            val cy = center.y

            //detect lines along the skeleton
            val lineMat = Mat()
            val detector = Imgproc.createLineSegmentDetector()
            detector.detect(contour, lineMat)

            //start greedy algorithm, track the longest minimum axis using A -> B
            //maximize the length of AB and minimize distance to the center
            val A = Point()
            val B = Point()
            var width = 0.0
            var minDstToCenter = Double.MAX_VALUE
            for (i in 0 until lineMat.rows()) {

                //grab lines from the result matrix of the lsd
                val coords = lineMat.get(i, 0)
                val a1 = coords[0]
                val a2 = coords[1]
                val b1 = coords[2]
                val b2 = coords[3]

                //create a line, scale defines the number of points to iterate across
                //slope is the opposite reciprocal slope to find perpendicular lines with
                val line = Line2D(Point2D(a1, a2), Point2D(b1, b2))
                val scale = line.length().toInt()
                val slope = -1 / ((b2-a2)/(b1-a1))

                val midPoint = Point2D.midPoint(Point2D(a1, a2), Point2D(b1, b2))
                val distToCenter = midPoint.distance(Point2D(cx, cy))

                //filter out small lines
                if (line.length() > minAxisThresh) {

                    //iterate over all points on the max axis
                    for (j in 1 until scale) {

                        //track whether or not the two contour boundaries for the
                        //perpendicular line have been found
                        var foundA = false
                        var foundB = false
                        val next = line.point(j / scale.toDouble())

                        //debug plot each of the line centers
                        //Imgproc.circle(dst, Point(next.x(), next.y()), 3, Scalar(0.0,0.0,255.0))

                        //next loop gradually grows the perpendicular line and tries to find contour boundary
                        //if the edge jumps out the contour the last point is used instead
                        val x = next.x()
                        val pointA = Point()
                        val pointB = Point()
                        val u = Point()
                        val v = Point()
                        var iterations = 0
                        //build a line to the right and left of the max axis
                        do {
                            //positive and negative sides of the max axis
                            val x2 = x + iterations/10.0
                            val x3 = x - iterations/10.0

                            //use point slope formula to find b for perpendicular line
                            val b = next.y()-slope*x

                            //use point slope formula to find y coordinate of left and right point
                            val y = slope*x2+b
                            val y2 = slope*x3+b

                            //Imgproc.circle(dst, Point(x2, y), 3, Scalar(0.0,0.0,255.0))
                            //Imgproc.circle(dst, Point(x3, y2), 3, Scalar(0.0,0.0,255.0))

                            //check if positive side is on a boundary yet
                            if (!foundA) {
                                pointA.x = x2
                                pointA.y = y

                                //The function returns +1 , -1 , or 0 to indicate if a point is inside, outside, or on the contour,
                                val result = Imgproc.pointPolygonTest(approx, Point(x2, y), false)
                                if (result == 0.0) foundA = true
                                if (result == -1.0) {
                                    pointA.x = u.x
                                    pointA.y = u.y
                                    break
                                }
                                if (result == 1.0) {
                                    u.x = x2
                                    u.y = y
                                }
                            }

                            //check if negative side is on a boundary yet
                            if (!foundB) {
                                pointB.x = x3
                                pointB.y = y2
                                val result2 = Imgproc.pointPolygonTest(approx, Point(x3, y2), false)
                                if (result2 == 0.0) foundB = true
                                if (result2 == -1.0) {
                                    pointB.x = v.x
                                    pointB.y = v.y
                                    break
                                }
                                if (result2 == 1.0) {
                                    v.x = x3
                                    v.y = y2
                                }
                            }

                            //debug draw the potential min axis line
                            //Imgproc.line(dst, pointA, pointB, Scalar(255.0, 255.0, 255.0), 1)

                            iterations++

                        } while (!(foundA && foundB))

                        //ensure a point was actually found
                        if (!(pointA.x == 0.0 && pointA.y == 0.0) && !(pointB.x == 0.0 && pointB.y == 0.0)) {

                            //greedily select the longest line that is closest to the center of mass
                            val length = sqrt((pointA.x-pointB.x).pow(2) + (pointA.y-pointB.y).pow(2))
                            if (length < maxAxis*0.75 && length > width && distToCenter < minDstToCenter) {
                                minDstToCenter = distToCenter
                                width = length
                                A.x = pointA.x
                                A.y = pointA.y
                                B.x = pointB.x
                                B.y = pointB.y
                            }
                        }
                    }
                }
            }

            //finally draw the actual min axis onto the destination image
            Imgproc.line(dst, A, B, Scalar(0.0, 255.0, 255.0), 3)

            return width
        }

        fun measureSmartGrain(dst: Mat, contour: MatOfPoint): Pair<Double, Double> {

            val approxCurve = MatOfPoint2f()

            //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
            //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
            //epsilon is the maximum distance from the contour and the approximation
            val preciseContour = MatOfPoint2f(*contour.toArray())
            val epsilon = 0.0005*Imgproc.arcLength(preciseContour, true)

            Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true)

            val ptsW = Array(2) { Point() }
            var width = 0.0
            val c = approxCurve.toArray()

            val ab = findLongestLine(c, arrayOf())

            val length = sqrt((ab.first.x - ab.second.x).pow(2.0) + (ab.first.y - ab.second.y).pow(2.0))

            val slopeL = (ab.second.y - ab.first.y) / (ab.second.x - ab.first.x)

            for (a in c) {

                var d = 1.0
                var p2 = a

                for (b in c) {
                    val s = slopeL * ((a.y - b.y) / (a.x - b.x))
                    if (abs(s+1) < d) {
                        d = abs(s+1)
                        p2 = b
                    }
                }

                val w = sqrt((a.x - p2.x).pow(2.0) + (a.y - p2.y).pow(2.0))
                if (w > width) {
                    width = w
                    ptsW[0] = a
                    ptsW[1] = p2
                }
            }

            Imgproc.line(dst, ptsW[0], ptsW[1], Scalar(0.0, 0.0, 255.0), 2); // width
            Imgproc.line(dst, ab.first, ab.second, Scalar(0.0, 255.0, 0.0), 2); // length

            return length to width
        }

        //function that returns the center point of a contour
        fun MatOfPoint.center() = with(Imgproc.moments(this@center)) {
            Point(m10 / m00, m01 / m00)
        }

        /**
         * A simple max-area filtering algorithm that finds and replaces duplicates with whichever has the largest area.
         * Contours with center points within 100px of eachother are considered duplicates.
         */
        fun filterDuplicatesByCenter(duplicates: List<MatOfPoint>): List<MatOfPoint> {

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
         * Uses cross multiplication to convert a measurement in px to a millimeters.
         */
        fun estimateMillis(groundTruthPixels: Double, groundTruthMillis: Double, unknown: Double) = unknown * groundTruthMillis / groundTruthPixels

        /**
         * Uses the known reference measurements (e.g a penny is 19.05 mm in diameter) to estimate the found contour areas.
         */
        fun estimateSeedArea(contours: List<MatOfPoint>, coins: List<MatOfPoint>, coinAreaMilli: Double): Map<MatOfPoint, Double> {

            val avgCoinArea = coins.map { Imgproc.contourArea(it) }.reduceRight { x, y -> x + y } / coins.size

            return contours.associateWith { estimateMillis(avgCoinArea, coinAreaMilli, Imgproc.contourArea(it)) }

        }

        /**
         * Greedy algorithm that takes the contours of an image and partitions them into coins and seed classes.
         * Coins are defined as the four contours closest to the the corners of the image. There will always be four.
         * Seeds are everything other than the coins, these could be clusters of seeds still.
         */
        fun findCoins(contours: List<MatOfPoint>, imageWidth: Double, imageHeight: Double): List<MatOfPoint> {

            //initialize the greedy parameters with null values
            val coins = arrayOfNulls<Pair<MatOfPoint, Point>>(4)
            val topLeft = 0
            val topRight = 1
            val bottomLeft = 2
            val bottomRight = 3

            //set the starting assumptions to be opposite corners.
            //e.g TOP_LEFT is set to the bottom right corner, any other contour should be closer to the TOP_LEFT corner
            if (contours.size >= 4) {

                coins[topLeft] = contours[0] to Point(imageWidth, imageHeight)
                coins[topRight] = contours[1] to Point(0.0, imageHeight)
                coins[bottomLeft] = contours[2] to Point(imageWidth, 0.0)
                coins[bottomRight] = contours[3] to Point(0.0, 0.0)

                //search through and approximate all contours,
                //update the coins based on how close they are to the respective corners
                for (i in contours.indices) {

                    val contour = contours[i]// sortedContours[i]

                    val approxCurve = MatOfPoint2f()


                    //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
                    //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
                    //epsilon is the maximum distance from the contour and the approximation
                    val preciseContour = MatOfPoint2f(*contour.toArray())
                    val area = Imgproc.contourArea(preciseContour)
                    val peri = Imgproc.arcLength(preciseContour, true)
                    val circularity = calcCircularity(area, peri)

                    if (circularity > 0.5) {

                        val epsilon = 0.009*peri

                        Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true)

                        //TODO put center point function in ImageProcessingUtils
                        //moment calculation: Cx = M10/M00 and Cy = M01/M00
                        val center = Imgproc.moments(approxCurve)
                        val centerPoint = Point(center.m10 / center.m00, center.m01 / center.m00)

                        //start of the four greedy search updates, checks if this contour's center point
                        //is closer to the corner than the previous
                        coins[topRight]?.let {
                            if (euclideanDistance(centerPoint, Point(imageWidth, 0.0)) <
                                euclideanDistance(it.second, Point(imageWidth, 0.0))) {
                                coins[topRight] = contour to centerPoint
                            }
                        }

                        coins[topLeft]?.let {
                            if (euclideanDistance(centerPoint, Point(0.0, 0.0)) <
                                euclideanDistance(it.second, Point(0.0, 0.0))) {
                                coins[topLeft] = contour to centerPoint
                            }
                        }

                        coins[bottomLeft]?.let {
                            if (euclideanDistance(centerPoint, Point(0.0, imageHeight)) <
                                euclideanDistance(it.second, Point(0.0, imageHeight))) {
                                coins[bottomLeft] = contour to centerPoint
                            }
                        }

                        coins[bottomRight]?.let {
                            if (euclideanDistance(centerPoint, Point(imageWidth, imageHeight)) <
                                euclideanDistance(it.second, Point(imageWidth, imageHeight))) {
                                coins[bottomRight] = contour to centerPoint
                            }
                        }
                    }
                }

                return coins.mapNotNull { it?.first }
            }

            return listOf()
        }
    }
}