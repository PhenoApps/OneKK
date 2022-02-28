package org.wheatgenetics.imageprocess

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.utils.ImageProcessingUtil
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.center
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.estimateMillis
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.estimateSeedArea
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.findCoins
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.quartileRange
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toMatOfPoint2f
import kotlin.collections.ArrayList
import kotlin.math.*


class DetectWithReference(private val context: Context, private val coinReferenceDiameter: Double, private val measure: String): OpenCVTransformation(context), DetectorAlgorithm {

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
        val avgCoinDiameter = coins.map { it.minMaxAxis().second }.reduceRight { x, y -> x + y } / coins.size

        //from the original contours, filter out the found seeds
        //filter out any duplicates. Sometimes shadows or object features can have distinct continous gradients which causes multiple contours per object.
        //this filter simply checks if the center points are too close and eliminates one.
        val seeds = filterInsideRoi(coins, ImageProcessingUtil.filterDuplicatesByCenter(contours - coins))

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

        val clusterAxis = estimateAxis(clusters, coins, coinReferenceDiameter)

        //val outputData = ArrayList<Double>()
        //transform the seed/cluster contours to counted/estimated results
        val objects = singles.map {

            val center = it.center()

            val (minAxis, maxAxis) = if (measure == "0") {
                val rect = Imgproc.minAreaRect(it.toMatOfPoint2f())
                val points = Array(4) { Point () }
                rect.points(points)
                Imgproc.drawContours(dst, listOf(MatOfPoint(*points)), -1, Scalar(255.0, 255.0, 0.0), 3)
                axisEstimates[it] ?: 0.0 to 0.0

            } else if (measure == "1") {

                val lw = ImageProcessingUtil.measureSmartGrain(dst, it)
                val maxAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, lw.first)
                val minAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, lw.second)

                minAxis to maxAxis
            } else {

                val l = ImageProcessingUtil.findLongestLine(it.toArray(), arrayOf())
                Imgproc.line(dst, l.first, l.second, Scalar(0.0, 255.0, 0.0), 2); // length

                val d = sqrt((l.first.x-l.second.x).pow(2.0) + (l.first.y-l.second.y).pow(2.0))
                val w = ImageProcessingUtil.carrotMethod(dst, it, d, 0.0)

                val maxAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, d)
                val minAxis = estimateMillis(avgCoinDiameter, coinReferenceDiameter, w)

                minAxis to maxAxis
            }

            DetectorAlgorithm.Contour(center.x, center.y, minAxis, maxAxis, singleEstimates[it] ?: error(""), 1)

        } + clusters.map {

            val center = it.center()

            val box = Imgproc.boundingRect(it)
            val mask = Mat.zeros(original.size(), original.type())
            Imgproc.drawContours(mask, listOf(it), -1, Scalar.all(255.0), -1)
            val count = watershed(Mat(mask, box))
            mask.release()

            if (count == 1) {

                val (minAxis, maxAxis) = clusterAxis[it] ?: 0.0 to 0.0

                DetectorAlgorithm.Contour(center.x, center.y, minAxis, maxAxis, clusterEstimates[it] ?: error(""), 1)

            } else DetectorAlgorithm.Contour(center.x, center.y, null, null, clusterEstimates[it] ?: error(""), count)

        }

//        val exampleContour = singles.first()
//        val mask = Mat.zeros(dst.size(), dst.type())
//        Imgproc.drawContours(mask, listOf(exampleContour), -1, Scalar.all(255.0), -1)
//
//        val rect = Imgproc.minAreaRect(exampleContour.toMatOfPoint2f())
//
//        val points = arrayOfNulls<Point>(4)
//
//        rect.points(points)
//
//        val roi = Mat()
//
//        original.copyTo(roi, mask)

//draw axes
//        Imgproc.line(roi, points[0], points[1], Scalar(255.0, 0.0, 255.0, 255.0), 3)
//
//        Imgproc.line(roi, points[1], points[2], Scalar(0.0, 0.0, 255.0, 255.0), 3)

//        val example = Mat(roi, rect.boundingRect())

        val originalOutput = original.toBitmap()
        original.release()

        val dstOutput = dst.toBitmap()
        dst.release()

        original.release()
//        roi.release()
//        mask.release()
        src.release()

        return DetectorAlgorithm.Result(originalOutput, dstOutput, null, objects)

    }

    private fun filterInsideRoi(coins: List<MatOfPoint>, contours: List<MatOfPoint>): List<MatOfPoint> {

        val rect = calculateRoi(coins);

        rect?.let {

            return contours.filter { rect.contains(it.center()) }

        }

        return listOf()
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

            //return a bounding rectangle between the top left and bottom right point
            val pointA = Point(tl.x, tl.y)

            val pointB = Point(br.x, br.y)

            Rect(pointA, pointB)

        } else null
    }

    //function that returns the min/max axis stats
    private fun MatOfPoint.minMaxAxis(): Pair<Double, Double> {

        val rect = Imgproc.minAreaRect(this.toMatOfPoint2f())

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

    private fun watershed(original: Mat): Int {

        val contours: List<MatOfPoint> = ArrayList()
        val hierarchy = Mat()

        //val cropped = original //]Mat(original, rect)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/cropped.png", cropped)

        val gray = Mat()

        Imgproc.cvtColor(original, gray, Imgproc.COLOR_BGR2GRAY)

        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 15.0)

        val thresh = Mat()

        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/first_thresh.png", thresh)

        val kernel = Mat.ones(Size(3.0, 3.0), CvType.CV_8U)

        //remove noise
        val opening = Mat()
        Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_ELLIPSE, kernel, Point(-1.0, -1.0), 2)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/opening.png", opening)

        val sureBg = Mat()
        Imgproc.dilate(opening, sureBg, kernel, Point(-1.0, -1.0), 3)

        val dt = Mat()
        Imgproc.distanceTransform(opening, dt, Imgproc.DIST_L2, Imgproc.CV_DIST_MASK_5)
        Core.normalize(dt, dt, 0.0, 1.0, Core.NORM_MINMAX)

        val sureFg = Mat()
        //val maxValue = Core.minMaxLoc(dt).maxVal
        Imgproc.threshold(dt, sureFg, 0.7, 255.0, Imgproc.THRESH_BINARY)
        sureFg.convertTo(sureFg, CvType.CV_8U)

        val unknown = Mat()
        Core.subtract(sureBg, sureFg, unknown)

//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/dt.png", dt)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/unknown.png", unknown)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/sure_bg.png", sure_bg)
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/sure_fg.png", sure_fg)

        //Imgproc.GaussianBlur(sure_fg, sure_fg, Size(), 5.0)

        Imgproc.findContours(sureFg, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        unknown.release()
        sureBg.release()
        sureFg.release()
        dt.release()
        opening.release()
        hierarchy.release()
        gray.release()
        thresh.release()
        //cropped.release()
//        org.opencv.imgcodecs.Imgcodecs.imwrite(dir.path + "/${contours.size}COUNTED${UUID.randomUUID()}output.png", sure_fg)

        //subtract the background label and border label
        return contours.size

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