package org.wheatgenetics.onekk.fragments

import android.graphics.*
import android.hardware.display.DisplayManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.core.impl.ImageAnalysisConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.util.Pair
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import org.wheatgenetics.imageprocess.*
import org.wheatgenetics.imageprocess.renderscript.ExampleRenderScript
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.analyzers.CoinAnalyzer
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.onekk.views.CanvasView
import org.wheatgenetics.utils.Dialogs
import java.io.File
import java.io.IOException
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.asKotlinRandom

class CameraFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var mCoinRecognitionResultBitmap: Bitmap

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private val screenAspectRatio by lazy {

        var rect: Rect = Rect()

        var point: Point = Point()

        // pull the metrics from our TextureView
        // textuewView size : height=match_parent, width=match_parent
        val metrics = DisplayMetrics().also { mBinding?.viewFinder?.display?.getRealMetrics(it) }

//        var m: DisplayMetrics = DisplayMetrics()
//        var rm = DisplayMetrics()
//        var real = Point()
//        var r = Rect()
//        var s = Point()
//        var smallest = Point()
//        var largest = Point()
//        mBinding?.viewFinder?.display?.getCurrentSizeRange(largest, smallest)
//
//        mBinding?.viewFinder?.display?.getMetrics(m)
//        mBinding?.viewFinder?.display?.getRealMetrics(rm)
//        mBinding?.viewFinder?.display?.getRealSize(real)
//        mBinding?.viewFinder?.display?.getRectSize(r)
//        mBinding?.viewFinder?.display?.getSize(s)


        // define the screen size
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val ratio = Rational(metrics.widthPixels, metrics.heightPixels)

//        DisplayManagerCompat.getInstance(requireContext())
//
//
//                .displays.first()?.let { display ->
//
//                    display.getRealSize(point)
//                    display.getRectSize(rect)
//
//                }
//
//        //rect
//        point
        ratio
    }

    private val metrics by lazy {

        var rect: Rect = Rect()

        var point: Point = Point()

        // pull the metrics from our TextureView
        // textuewView size : height=match_parent, width=match_parent
        val metrics = DisplayMetrics().also { mBinding?.viewFinder?.display?.getRealMetrics(it) }
        // define the screen size
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

//        DisplayManagerCompat.getInstance(requireContext())
//
//
//                .displays.first()?.let { display ->
//
//                    display.getRealSize(point)
//                    display.getRectSize(rect)
//
//                }
//
//        //rect
//        point
        screenSize
    }

    private val sScreenScale by lazy {
        var smallest = Point()
        var largest = Point()
        mBinding?.viewFinder?.display?.getCurrentSizeRange(smallest, largest)
        largest
    }

    companion object {

        final val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var mBinding: FragmentCameraBinding? = null

    private val sAnalyzer by lazy {
        CoinAnalyzer(requireContext()) { bmp, boxes ->

            val bitmap = bmp!!//.copy(bmp.config, false)

//            Log.d("Metrics", "${bmp?.width}x${bmp?.height}")

            //val overlay = Bitmap.createBitmap(metrics.x, metrics.y, Bitmap.Config.ARGB_8888)

            val overlay = bitmap.copy(bitmap.config, true)//Bitmap.createBitmap(bmp!!.width, bmp.height, bmp.config)

            //overlay.scale(sScreenScale.x, sScreenScale.y)

            requireActivity().runOnUiThread {
                mBinding?.canvasImageView?.setImageBitmap(overlay)
                mBinding?.canvasImageView?.rotation = 90f
            }

            if (boxes.isNotEmpty()) {

                val paint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    color = Color.GREEN
                    strokeWidth = 5f
                }

                val textPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    textSize = 36f
                    color = Color.GREEN
                    isLinearText = true
                    strokeWidth = 1f
                }

                var canvas = Canvas(overlay)


                canvas.drawRect(Rect(0, 0, bmp.width, bmp.height), paint)

                for (b in boxes) {

                    var rect = b.rect

                    canvas.drawRect(Rect(rect.x, rect.y,
                            rect.x + rect.width,
                            rect.y + rect.height),
                            paint
                    )

                    val circText = if (b.circ.toString().isNotBlank() && b.circ.toString().length > 5) {
                        b.circ.toString().substring(0,5)
                    } else ""

                    canvas.drawText("circularity: $circText", rect.x.toFloat(), rect.y - 50f, textPaint)

                    //canvas.rotate(-90f)
                    //canvas.scale(3.0f, 3f)

                }

                mBinding?.cameraCaptureButton?.setOnClickListener {

                    callCoinRecognitionDialog(overlay)

                }
            }


        }
    }

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                startCameraAnalysis()

                //startCamera()

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
//
//        val cv = CanvasView(requireContext(), mCoinRecognitionResultBitmap)
//        mBinding?.group?.addView(cv)

        checkCamPermissions.launch(android.Manifest.permission.CAMERA)

        with(mBinding) {

            this?.coin1?.visibility = View.GONE
            this?.coin2?.visibility = View.GONE
            this?.coin3?.visibility = View.GONE
            this?.coin4?.visibility = View.GONE

//            Timer().scheduleAtFixedRate(object : TimerTask() {
//
//                override fun run() {
//
//                    requireActivity().runOnUiThread {
//
//                        if (::mCoinRecognitionResultBitmap.isInitialized) {
//                            with(mCoinRecognitionResultBitmap) {
////                                mBinding?.surfaceView?.bitmap = this.copy(config, false)
////                                mBinding?.surfaceView?.invalidate()
//                                //cv.invalidate()
//                            }
//                        }
//                    }
//                }
//
//            }, 0L, 1L)



            getOutputDirectory()?.let { output ->

                outputDirectory = output

            }

            cameraExecutor = Executors.newSingleThreadExecutor()

        }

        return mBinding?.root
    }

    private fun getOutputDirectory(): File? {

        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context?.filesDir
    }

    override fun onDestroy() {

        super.onDestroy()

        cameraExecutor.shutdown()

    }

    private fun startCameraAnalysis() {

        this@CameraFragment.context?.let { ctx ->

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                 //Preview
                val preview = Preview.Builder()
                        .setTargetResolution(metrics)
                        .build()
                        .also {
                            it.setSurfaceProvider(mBinding?.viewFinder?.createSurfaceProvider())
                        }

                val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(metrics)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, sAnalyzer)
                        }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val cam = cameraProvider.bindToLifecycle(this as LifecycleOwner,
                            cameraSelector, preview, imageAnalyzer)

                    //cam.cameraControl.enableTorch(true)


                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    private fun takePhoto() {

        with(mBinding) {

            // Get a stable reference of the modifiable image capture use case
            val imageCapture = imageCapture ?: return

            // Create time-stamped output file to hold the image
            val photoFile = File(
                    outputDirectory,
                    SimpleDateFormat(FILENAME_FORMAT, Locale.US
                    ).format(System.currentTimeMillis()) + ".jpg")

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Set up image capture listener, which is triggered after photo has
            // been taken
            imageCapture.takePicture(
                    outputOptions, ContextCompat.getMainExecutor(this@CameraFragment.context), object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {

                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)

                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val savedUri = Uri.fromFile(photoFile)

                    val msg = "Photo capture succeeded: $savedUri"

                    Toast.makeText(this@CameraFragment.context, msg, Toast.LENGTH_SHORT).show()

                    Log.d(TAG, msg)

                    //callCoinRecognitionDialog(savedUri)

                }
            })
        }
    }

    /**
     * Creates a Dialog that asks the user to accept or decline the image.
     * In this case, if the coin recognition step is accepted, the watershed algorithm begins.
     */
    private fun callCoinRecognitionDialog(bmp: Bitmap?) {

        mBinding?.let { ui ->

            try {

                this@CameraFragment.activity?.let { activity ->

                    /*
                    coinRecResult -> Boolean from Dialogs acceptance
                    bmp -> result image of coin recognition
                     */

                    Dialogs.askAcceptableCoinRecognition(
                            activity,
                            AlertDialog.Builder(activity),
                            getString(R.string.ask_coin_recognition_ok),
                            bmp) { bmp ->


                    }

                }

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }

    private fun startCamera() {

        this@CameraFragment.context?.let { ctx ->

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener(Runnable {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

//                // Preview
//                val preview = Preview.Builder()
//                        .build()
//                        .also {
//                            it.setSurfaceProvider(mBinding?.viewFinder?.createSurfaceProvider())
//                        }

                imageCapture = ImageCapture.Builder()
                        .build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            this, cameraSelector, imageCapture)

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    private fun draw(canvas: Canvas) {

        if (::mCoinRecognitionResultBitmap.isInitialized) {
            val random = Random()
            canvas.drawRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255))
            canvas.drawBitmap(mCoinRecognitionResultBitmap, 1080f, 1920f, Paint().also {
                it.color = Color.BLACK
                it.isAntiAlias = true
                it.isDither = true
            })
            canvas.drawText("Coin Recognition", 100f, 200f, Paint().also {
                it.color = Color.BLACK
                it.isAntiAlias = true
                it.isDither = true
                it.isLinearText = true
            })
        }

    }
}