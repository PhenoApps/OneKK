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
import androidx.core.net.toUri
import androidx.core.util.Pair
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import org.wheatgenetics.imageprocess.*
import org.wheatgenetics.imageprocess.renderscript.ExampleRenderScript
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.analyzers.CoinAnalyzer
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.models.embedded.Image
import org.wheatgenetics.onekk.database.viewmodels.AnalysisViewModel
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.onekk.views.CanvasView
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.asKotlinRandom

class CameraFragment : Fragment(), CoroutineScope by MainScope() {

    private val db by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
    }

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

    private fun measureArea(groundTruthPixel: Double, groundTruthmm: Double, kernelPx: Double): Double {
        return kernelPx * groundTruthmm / groundTruthPixel
    }

    companion object {

        final val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var mBinding: FragmentCameraBinding? = null

    private val sAnalyzer by lazy {
        CoinAnalyzer(requireContext()) { result ->

            val boxes = result.detections

            updateCoinUi(boxes)

            val bmp = result.images.last()

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

                var penny = boxes.minByOrNull { it.rect.width }!!

                for (b in boxes) {

                    var rect = b.rect

                    val diameter = measureArea(penny.rect.width.toDouble(), 19.05, rect.width.toDouble())

                    canvas.drawRect(Rect(rect.x, rect.y,
                            rect.x + rect.width,
                            rect.y + rect.height),
                            paint
                    )

                    val circText = if (b.circ.toString().isNotBlank() && b.circ.toString().length > 5) {
                        b.circ.toString().substring(0,5)
                    } else ""

                    //canvas.drawText("circularity: $circText", rect.x.toFloat(), rect.y - 50f, textPaint)
                    canvas.drawText("diameter: $diameter", rect.x.toFloat(), rect.y - 50f, textPaint)

                    //canvas.rotate(-90f)
                    //canvas.scale(3.0f, 3f)

                }


            }

            mBinding?.cameraCaptureButton?.setOnClickListener {

                callCoinRecognitionDialog(result)

            }
        }
    }

    private fun updateCoinUi(boxes: ArrayList<DetectRectangles.Detections>) {
        requireActivity().runOnUiThread {
            when (val size = boxes.size) {
                1 -> {
                    mBinding?.coin1?.visibility = View.VISIBLE
                    mBinding?.coin2?.visibility = View.GONE
                    mBinding?.coin3?.visibility = View.GONE
                    mBinding?.coin4?.visibility = View.GONE

                }
                2 -> {
                    mBinding?.coin1?.visibility = View.VISIBLE
                    mBinding?.coin2?.visibility = View.VISIBLE
                    mBinding?.coin3?.visibility = View.GONE
                    mBinding?.coin4?.visibility = View.GONE
                }
                3 -> {
                    mBinding?.coin1?.visibility = View.VISIBLE
                    mBinding?.coin2?.visibility = View.VISIBLE
                    mBinding?.coin3?.visibility = View.VISIBLE
                    mBinding?.coin4?.visibility = View.GONE
                }
                4 -> {
                    mBinding?.coin1?.visibility = View.VISIBLE
                    mBinding?.coin2?.visibility = View.VISIBLE
                    mBinding?.coin3?.visibility = View.VISIBLE
                    mBinding?.coin4?.visibility = View.VISIBLE
                }
                else -> {
                    mBinding?.coin1?.visibility = View.GONE
                    mBinding?.coin2?.visibility = View.GONE
                    mBinding?.coin3?.visibility = View.GONE
                    mBinding?.coin4?.visibility = View.GONE
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

        viewModel.deleteAll()
        viewModel.dropAll()
        //analysisViewModel.dropAll()

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

            Timer().scheduleAtFixedRate(object : TimerTask() {

                override fun run() {

                    requireActivity().runOnUiThread {

                        updateCoinUi(arrayListOf())

                    }
                }

            }, 0L, 2000L)

            getOutputDirectory()?.let { output ->

                outputDirectory = output

            }

            cameraExecutor = Executors.newSingleThreadExecutor()

//            //viewModel.insert(ExperimentEntity(Experiment("test"), 1))
//            Dialogs.askAcceptableCoinRecognition(
//                    activity,
//                    AlertDialog.Builder(activity),
//                    getString(R.string.ask_coin_recognition_ok),
//                    bmp) { bmp ->
//
//                analysis.images.forEach {
//
//                    var url = try {
//
//                        val file = File(outputDirectory.path.toString(), "test.png")
//
//                        FileOutputStream(file).use { stream ->
//
//                            bmp!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
//
//                        }
//
//                        file.toUri()
//
//                    } catch (e: IOException) {
//
//                        e.printStackTrace()
//
//                        null
//                    }
//
//                    url?.let {
//
//                        val row = AnalysisEntity(
//                                Image(it.toString()), 1, 1)
//                                .insert(row)
//
//                    }
//
//                }
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

                }
            })
        }
    }

    /**
     * Creates a Dialog that asks the user to accept or decline the image.
     * In this case, if the coin recognition step is accepted, the watershed algorithm begins.
     */
    private fun callCoinRecognitionDialog(result: DetectRectangles.AnalysisResult) {

        mBinding?.let { ui ->

            try {

                this@CameraFragment.activity?.let { activity ->

                    val bmp = result.images.last()
                    /*
                    coinRecResult -> Boolean from Dialogs acceptance
                    bmp -> result image of coin recognition
                     */

                    Dialogs.askAcceptableCoinRecognition(
                            activity,
                            AlertDialog.Builder(activity),
                            getString(R.string.ask_coin_recognition_ok),
                            bmp) { bmp ->

                        viewModel.dropAll()
                        viewModel.deleteAll()
                        viewModel.insert(ExperimentEntity(Experiment("Test", DateUtil().getTime()), 1))
                        viewModel.insert(AnalysisEntity(1, 1))

                        result.images.forEachIndexed { index, image ->
                            val file = File(outputDirectory.path.toString(), "${UUID.randomUUID()}.png")

                            FileOutputStream(file).use { stream ->

                                image.compress(Bitmap.CompressFormat.PNG, 100, stream)

                            }

                            viewModel.insert(ImageEntity(Image(Uri.fromFile(file).path.toString(), DateUtil().getTime()), 1, 1))
                        }

                        findNavController().navigate(CameraFragmentDirections.actionToAnalysis(1))
                    }

                }

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }
}