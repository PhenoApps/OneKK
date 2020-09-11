package org.wheatgenetics.onekk.analyzers

import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.imageprocess.DetectRectangles
import org.wheatgenetics.imageprocess.renderscript.ExampleRenderScript
import org.wheatgenetics.onekk.activities.AnalysisListener
import org.wheatgenetics.onekk.activities.BitmapListener
import org.wheatgenetics.onekk.fragments.CameraFragment
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class CoinAnalyzer(private val context: Context, private var metrics: Size? = null, private var width: Int = 1080, private var height: Int = 1920, private val listener: AnalysisListener) : ImageAnalysis.Analyzer, CoroutineScope by MainScope() {

    private var frames = 0.0
    private var startAnalysisTime = System.currentTimeMillis()

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }


//    private suspend fun coinRecognition(startTime: Long, src: Bitmap): Deferred<ArrayList<DetectRectangles.Detections>?> = withContext(Dispatchers.IO) {
//
//
//        val imageProcessingJob = async {
//
//           // mCoinRecognitionResultBitmap = src.copy(src.config, true)
//
//            //mCoinRecognitionResultBitmap = ExampleRenderScript().yuvToRgb(mCoinRecognitionResultBitmap, requireContext())!!
//
//            //ExampleRenderScript().resizeScript(mCoinRecognitionResultBitmap, requireContext())
//
//            //ExampleRenderScript().testScript(mCoinRecognitionResultBitmap, requireContext())!!
//
//            //mCoinRecognitionResultBitmap = ExampleRenderScript().mandelbrotBitmap(mCoinRecognitionResultBitmap, requireContext())!!
//
////            context?.let { ctx ->
////
////                with(ExampleRenderScript(requireContext())) {
////
////                    histogramEqualization(mCoinRecognitionResultBitmap)
////
////                    blur(mCoinRecognitionResultBitmap, 5f)
////
////                    convolveLaplaceBitmap(mCoinRecognitionResultBitmap)
////
////                    Blur().process(mCoinRecognitionResultBitmap)
//////
//////                    blur(mCoinRecognitionResultBitmap, 1f)
//////
//////                    convolveLaplaceBitmap(mCoinRecognitionResultBitmap)
////
////                    return@async arrayListOf<DetectRectangles.Detections>() //DetectRectangles().process(mCoinRecognitionResultBitmap)
////
////                }
////
//            }
//
//        }
//        Log.d(CameraFragment.TAG, "Time: ${System.currentTimeMillis()-startTime}")
//
//        return@withContext imageProcessingJob
//
//
////        val detect = Blur()
////
////        detect.process(mCoinRecognitionResultBitmap)
////
//
//    }


    val detector = DetectRectangles()
    val renderContext = ExampleRenderScript(context)
    override fun analyze(proxy: ImageProxy) {

//        val buffer = image.planes[0].buffer
//        val data = buffer.toByteArray()
//        val pixels = data.map { it.toInt() and 0xFF }
//        val luma = pixels.average()

//        val rotationDegrees = proxy.imageInfo.rotationDegrees

        proxy.toBitmap()
                //.scale(width, height)
                .let { src ->

//            launch {
//
//                coinRecognition(System.currentTimeMillis(), src)
//
//                    .await()?.let { detections ->
//
//
//                    }
//            }


            val startTime = System.currentTimeMillis()

            val result = with(renderContext) {

                //histogramEqualization(src)

                //blur(src, 1f)

                //convolveLaplaceBitmap(src)

                //resize(src)

                //Thresh().process(src)

                //blur(src, 1f)

                //convolveLaplaceBitmap(src)

                //val boxes = arrayListOf<DetectRectangles.Detections>()//DetectRectangles().process(src)
                var boxes = arrayListOf<DetectRectangles.Detections>()

                var result = detector.process(src)

//                Log.d(CameraFragment.TAG, "RenderScript: ${boxes.size} objects ${src.width}x${src.height}")

                return@with result

            }

            Log.d(CameraFragment.TAG, "RenderScriptT: ${System.currentTimeMillis() - startTime}")

            listener(result)

        }

        proxy.close()

    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

}