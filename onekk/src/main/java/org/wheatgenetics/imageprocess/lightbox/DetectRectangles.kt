package org.wheatgenetics.imageprocess.lightbox

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.wheatgenetics.utils.ImageProcessingUtil
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.AnalysisResult
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Detections
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.Identity
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.ConvertGrey
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.GaussianBlur
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.seedContours
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.calcCircularity
import org.wheatgenetics.utils.ImageProcessingUtil.Companion.toBitmap

class DetectRectangles {

    companion object {

        val EXPECTED_NUMBER_OF_COINS = 4
    }

    /**
     * Class to hold the values of contour areas and circularities.
     */
    inner class ContourAreaCircularityTriple(area: Double, contour: MatOfPoint, circularity: Double) {
        internal var contour:MatOfPoint
        internal var area:Double
        internal var circularity: Double

        init{
            this.contour = contour
            this.area = area
            this.circularity = circularity
        }
    }

    /**
     * Recognizes 'n' largest contours on an image based on a circularity measure where n is a predefined parameter.
     * This helps in identifying the coins on images containing both seeds and coins. A user defined circularity threshold is used to filter out
     * contours initially and from the remaining contours, the four largest contours i.e. the contours assumed to be coins and their circularities are returned.
     *
     * This works because we assume that the coins make the largest contours based on a circularity threshold.
     *
     */
    private fun coinRecognition(contours: ArrayList<MatOfPoint?>):Pair<ArrayList<MatOfPoint?>, ArrayList<Double>> {
        val userParams = ImageProcessingUtil.Companion.UserTunableParams()
        val areas = ArrayList<Double>()
        val circus = ArrayList<Double>()
        val circularities = ArrayList<Double>()

        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour?.toArray()), true)
            val area = Imgproc.contourArea(contour)
            areas.add(area)
            circus.add(calcCircularity(area, peri))
        }
        val contourMap = ArrayList<ContourAreaCircularityTriple>()
        for (i in contours.indices) {
            if (circus.get(i) >= userParams.circThreshold) {
                contours.get(i)?.let { ContourAreaCircularityTriple(areas.get(i), it, circus.get(i)) }?.let { contourMap.add(it) }
            }
        }

        var contourArray = Array(contourMap.size) { ContourAreaCircularityTriple(0.0, MatOfPoint(), 0.0) }
        for (i in contourMap.indices) {
            contourArray[i] = contourMap.get(i)
        }

        val sortedContours = contourArray.sortedByDescending({ it.area }).filterIndexed{index, s -> index < EXPECTED_NUMBER_OF_COINS}

        val coins = ArrayList<MatOfPoint?>()

        for(i in sortedContours.indices){
            coins.add(sortedContours.get(i).contour)
            circularities.add(sortedContours.get(i).circularity)
        }

        return Pair(coins, circularities)

    }

    /**
     * The process method helps detect the coins from an image containing seeds and coins.
     * The input image is initially put through a series of pre-processing steps such as grayscale conversion, gaussian blurring, and binary thresholding
     * before contours are found on the image.
     * From the set of contours identified on the image, the contours deemed coins are detected and returned.
     */
    private fun process(original: Mat): AnalysisResult? {

        val gtImg = original.clone()
        val coinCopy = original.clone()
        val cannyContours = original.clone()

        val parentlessContourMask = Mat.zeros(gtImg.size(), CvType.CV_8U)
        var rectangles = ArrayList<Detections>()
        var parentlessContours = ArrayList<MatOfPoint?>()
        var gtCoins: ArrayList<MatOfPoint?>

        val result = ImageProcessingUtil.Companion.AnalysisResult(ArrayList<Bitmap>(), ArrayList<ImageProcessingUtil.Companion.Detections>(), ArrayList<ImageProcessingUtil.Companion.PipelineFunction>())

        result.images.add(original.clone().toBitmap())
        result.pipeline.add(Identity());

        Imgproc.cvtColor(gtImg, gtImg, Imgproc.COLOR_BGR2GRAY)

        result.images.add(gtImg.clone().toBitmap())
        result.pipeline.add(ConvertGrey())

        Imgproc.GaussianBlur(gtImg, gtImg, ImageProcessingUtil.Companion.GaussianBlur().size, GaussianBlur().sigmaX)

        result.images.add(gtImg.clone().toBitmap())
        result.pipeline.add(GaussianBlur())

        Imgproc.threshold(gtImg, gtImg, ImageProcessingUtil.Companion.Threshold().c, ImageProcessingUtil.Companion.Threshold().maxValue, ImageProcessingUtil.Companion.Threshold().type)

        result.images.add(gtImg.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.Threshold())

        val mask = Mat.zeros(gtImg.size(), CvType.CV_8U)
        var coinContours = ArrayList<MatOfPoint?>()
        val hierarchy = Mat()
        Imgproc.findContours(gtImg, coinContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        val hull = MatOfInt()
        for (i in coinContours.indices)
        {
            Imgproc.convexHull(coinContours.get(i), hull)
            if ((coinContours.get(i)?.rows() ?: 0) / hull.rows() <= 3) {
                Imgproc.drawContours(mask, coinContours, i, ImageProcessingUtil.Companion.DrawContours().color, ImageProcessingUtil.Companion.DrawContours().thickness)
            }
        }

        result.images.add(mask.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.DrawContours())

        Imgproc.GaussianBlur(mask, mask, ImageProcessingUtil.Companion.GaussianBlur().size, ImageProcessingUtil.Companion.GaussianBlur().sigmaX)

        result.images.add(mask.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.MedianBlur())

        Imgproc.Canny(mask, cannyContours, ImageProcessingUtil.Companion.Canny().maxThresh, ImageProcessingUtil.Companion.Canny().minThresh)

        result.images.add(cannyContours.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.Canny())

        Imgproc.findContours(cannyContours, coinContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val maxContour = ImageProcessingUtil.ComputeMaxContour(coinContours)

        for(i in 0 until hierarchy.cols()){
            if(hierarchy.get(0, i)[3] == -1.0 || hierarchy.get(0, i)[3] == maxContour.toDouble()){
                parentlessContours.add(coinContours.get(i))
            }
        }

        Imgproc.drawContours(parentlessContourMask, parentlessContours, -1, ImageProcessingUtil.Companion.DrawContours().color, ImageProcessingUtil.Companion.DrawContours().thickness)

        result.images.add(parentlessContourMask.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.DrawContours())

        // coins is a pair that consists of coin contours and circularities.
        val coins = coinRecognition(parentlessContours)

        gtCoins = coins.first

        seedContours = parentlessContours.minus(gtCoins) as ArrayList<MatOfPoint?>

        Imgproc.drawContours(coinCopy, gtCoins, -1, ImageProcessingUtil.Companion.DrawContours().color, ImageProcessingUtil.Companion.DrawContours().thickness)

        result.images.add(coinCopy.clone().toBitmap())
        result.pipeline.add(ImageProcessingUtil.Companion.DrawContours())

        for(i in gtCoins.indices){
            val rect = Imgproc.boundingRect(gtCoins.get(i))
            val center = Imgproc.moments(gtCoins.get(i))
            val centerPoint = Point(center.m10/center.m00, center.m01/center.m00)
            val area = Imgproc.contourArea(gtCoins.get(i))
            val detection = Detections(rect, coins.second.get(i), centerPoint, MatOfPoint(*gtCoins.get(i)?.toArray()), area)
            rectangles.add(detection)
        }

        result.detections = rectangles
        result.isCompleted = true

        return result

        //coinContours = coins.first
//        var areas = ArrayList<androidx.core.util.Pair<Double, MatOfPoint>>()
//        for (i in contours.indices)
//        {
//            val area = Imgproc.contourArea(contours.get(i))
//            val moments = Imgproc.moments(contours.get(i))
//            if (moments.m00 > 0) {
//                val cX = (moments.m10 / moments.m00).roundToInt()
//                val cY = (moments.m01 / moments.m00).roundToInt()
//                val avgVal = Mean<Double>(toDoubleArray(mask.get(cY, cX)))
//                if (avgVal < 170)
//                {
//                    areas.add(androidx.core.util.Pair<Double, MatOfPoint>(area, contours.get(i)))
//                }
//            }
//        }


//        var epsilon: Double?
//
//        val result = AnalysisResult(ArrayList<Bitmap>(), ArrayList<Detections>(), ArrayList<PipelineFunction>())
//
//        val src = original.clone()
//        result.images.add(src.clone().toBitmap())
//        result.pipeline.add(Identity())
//
//        //calculate the area of the image to estimate the seed min/max thresholds
//        val width = src.width()
//        val height = src.height()
//        val pictureArea = width * height
//
//        val maxCoinAreaThresh = pictureArea*0.025
//
//        //Log.d("MINIMUM COIN THRESH", minCoinAreaThresh.toString())
//       // Log.d("MAX COIN THRESH", maxCoinAreaThresh.toString())
//
//        val contours: List<MatOfPoint> = ArrayList()
//        val hierarchy = Mat()
//
//        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)
//
//        result.images.add(src.clone().toBitmap())
//        result.pipeline.add(ConvertGrey())
//
//        Imgproc.adaptiveThreshold(src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10.0)
//
//        result.images.add(src.clone().toBitmap())
//        result.pipeline.add(AdaptiveThreshold())
//
//        //CHAIN_APPROX_NONE will give more contour points, uses more memory
//        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
//        val hull = MatOfInt()
//
//        //Log.d("DrawContours", "${src.width()}x${src.height()} ${contours.size}")
//
//        val color = original.clone()
//
//        val sortedContours = contours.sortedByDescending { Imgproc.contourArea(it) }
//        for (i in sortedContours.indices) {
//
//            var contour = sortedContours[i]
//
//            val area = Imgproc.contourArea(contour)
//
//            val approxCurve = MatOfPoint2f();
//
//            //contour approximation: https://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_imgproc/py_contours/py_contour_features/py_contour_features.html
//            //Douglas-Peucker algorithm that approximates a contour with a polygon with less vertices
//            //epsilon is the maximum distance from the contour and the approximation
//            val preciseContour = MatOfPoint2f(*contour.toArray())
//            epsilon = 0.009*Imgproc.arcLength(preciseContour, true)
//
//            Imgproc.approxPolyDP(preciseContour, approxCurve, epsilon, true);
//            val peri = Imgproc.arcLength(approxCurve, true)
//            val circ = ImageProcessingUtil.calcCircularity(area, peri)
//            //Log.d("Circularity", "$circ")
//
//            if (circ >= 0.9 && area > 1000) {
//
//                //Imgproc.convexHull(contours[i], hull);
//
//                val rect = Imgproc.boundingRect(approxCurve)
//                //RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i)));
//
//                //TODO put center point function in ImageProcessingUtils
//                //moment calculation: Cx = M10/M00 and Cy = M01/M00
//                val center = Imgproc.moments(approxCurve)
//                val centerPoint = Point(center.m10/center.m00, center.m01/center.m00)
//
////                var topLeft = Point(rect.x.toDouble(), rect.y.toDouble())
////                var botRight = Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble())
//
//                val otherContours = ImageProcessingUtil.centerDifferentForAllRectangles(approxCurve, rectangles)
//
//                val newDetection = Detections(rect, circ, centerPoint, MatOfPoint(*approxCurve.toArray()), area)
//
//                rectangles.add(newDetection)
//
//                otherContours?.forEach { other ->
//
//                    val choices = (otherContours + newDetection)
//                    val max = choices.maxByOrNull { other.area }
//
//                    rectangles.removeAll(choices - max)
//
//                }
//            }
//
//            rectangles = ImageProcessingUtil.interquartileReduce(rectangles.toTypedArray())
//
//            val areas = rectangles.map { it.area }
//
//            if (rectangles.size == EXPECTED_NUMBER_OF_COINS) {
//
//                val dst = color.clone()
//                //draw contours documentation, fill in all detected coins
//                //https://docs.opencv.org/3.4/d6/d6e/group__imgproc__draw.html#ga746c0625f1781f1ffc9056259103edbc
//
//                Imgproc.drawContours(dst, rectangles.map { it.contour }, -1, ImageProcessingUtil.COIN_FILL_COLOR, -1)
//
//                result.images.add(dst.toBitmap())

//                val ellipses = color.clone()
//
//                rectangles.map { it.contour }.forEach {
//                    Imgproc.fitEllipse(MatOfPoint2f(*it.toArray())).also { rotatedRect ->
//                        Imgproc.circle(ellipses, rotatedRect.center, 5, ImageProcessingUtil.COIN_FILL_COLOR)
//                    }
//                }
//
//                result.images.add(ellipses.toBitmap())
//                println("epsilon: $epsilon")
//
//                result.isCompleted = true
//
//                result.detections = rectangles
//
//                break
//            }
//
//        }
//
//        return if (result.isCompleted) {
//
//            result
//
//        } else null
//        return null
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