package org.wheatgenetics.onekk.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.FORMAT_SFLOAT
import android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16
import android.bluetooth.BluetoothGattDescriptor
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.ParcelUuid
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import com.polidea.rxandroidble2.utils.StandardUUIDsParser
import io.reactivex.Flowable.combineLatest
import io.reactivex.Observable
import io.reactivex.Observable.combineLatest
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import org.opencv.core.MatOfPoint
import org.wheatgenetics.imageprocess.DetectRectangles
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.analyzers.CoinAnalyzer
import org.wheatgenetics.onekk.analyzers.NoopAnalyzer
import org.wheatgenetics.onekk.analyzers.SeedAnalyzer
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import org.wheatgenetics.onekk.database.models.embedded.Contour
import org.wheatgenetics.onekk.database.models.embedded.Image
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import org.wheatgenetics.utils.ImageProcessingUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This fragment uses the CameraX analysis API. Two different analyzers, Coin and Seed, are used
 * for recognizing and counting/measuring coins respectively.
 *
 * Analysis:
 * The coin analyzer is a quick adaptiveThresholding approach which, when four coins are recognized,
 * will enable the camera capture button. The camera capture button switches the analysis to the
 * Seed Analyzer, which requires the source image of the coin recognition task.
 */
class CameraFragment : Fragment(), CoroutineScope by MainScope() {

    private val db by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
    }

    private var mExperimentId: Int = -1

    private var mChosenResult: ImageProcessingUtil.Companion.AnalysisResult? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var mBinding: FragmentCameraBinding? = null

    private var isShowingDialog: Boolean = false

    //query the relative screen size
    private val metrics by lazy {

        val metrics = DisplayMetrics().also { mBinding?.viewFinder?.display?.getRealMetrics(it) }

        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        //val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        screenSize
    }

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                startCameraAnalysis(sCoinAnalyzer)

            }
        }
    }

    companion object {

        final val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    val sCanvasPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 5f
    }

    val sCanvasTextPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 36f
        color = Color.GREEN
        isLinearText = true
        strokeWidth = 1f
    }

    //the no-operation analyzer that's used between coin recognition and seed phenotyping
    private val sNoopAnalysis by lazy { NoopAnalyzer() }

    /**
     * This is the first analysis that runs when the app begins. Images are constantly
     * processed in the background from the camera. Coin Analyzer is required to return a result that includes:
     * the pipeline array of images, the transformations between each image, and the detections array which includes
     * contours, contour areas, contour centers, and contour bounding boxes.
     *
     * If four coins are detected, the analysis switches to the Seed Analysis. The Seed Analysis is slightly different, as
     * it does not continuously process new images, but runs the Onekk algorithm on the source image of the coin recognition result.
     *
     */
    private val sCoinAnalyzer by lazy {
        CoinAnalyzer { result ->

            val boxes = result.detections

            val bmp = result.images.last()

            val bitmap = bmp!!//.copy(bmp.config, false)

//            println("Rectangles: ${boxes.size}")

            val overlay = bitmap.copy(bitmap.config, true)//Bitmap.createBitmap(bmp!!.width, bmp.height, bmp.config)

            requireActivity().runOnUiThread {
                mBinding?.canvasImageView?.setImageBitmap(overlay)
                mBinding?.canvasImageView?.rotation = 90f
            }

            /**
             * Draw bounding boxes of the accepted coins.
             */
            if (boxes.isNotEmpty()) {

                var canvas = Canvas(overlay)

                canvas.rotate(-90f)
                canvas.drawText("${boxes.size} ${requireContext().getString(R.string.frag_camera_coins_maybe_plural)}", 1000f, 500f, sCanvasTextPaint)
                canvas.rotate(90f)

                canvas.drawRect(Rect(0, 0, bmp.width, bmp.height), sCanvasPaint)

                for (b in boxes) {

                    var rect = b.rect

                    canvas.drawText("${b.circ}", rect.x.toFloat(), rect.y.toFloat(), sCanvasTextPaint)

                    canvas.drawRect(Rect(rect.x, rect.y,
                            rect.x + rect.width,
                            rect.y + rect.height),
                            sCanvasPaint
                    )
                }
            }

            updateCurrentAnalysisResult(result)

        }
    }

    /**
     * Runs a UI update of the camera capture button. When this button is enabled, the user can capture
     * a picture. When a picture is captured, a dialog asks the user if the result of the coin detection
     * is acceptable.
     */
    private fun updateCurrentAnalysisResult(result: ImageProcessingUtil.Companion.AnalysisResult) = requireActivity().runOnUiThread {

        if (result.isCompleted && result.detections.size == DetectRectangles.EXPECTED_NUMBER_OF_COINS) {

            if (!isShowingDialog) {

                mChosenResult = result

                callCoinRecognitionDialog()
            }
        }
    }

    /**
     * Setup the image analysis listener for seed phenotyping. This analyzer takes an input image
     * and runs the algorithm within SeedAnalyzer.
     */
    private fun seedAnalyzer(src: Bitmap, coins: List<MatOfPoint>) = SeedAnalyzer(outputDirectory, src, coins) { result ->

        //savePipelineToDatabase(result)

        //on the first result, switch to the noop analyzer
        startCameraAnalysis(NoopAnalyzer())

        val boxes = result.detections

        val bmp = result.images.last()

        val bitmap = bmp!!//.copy(bmp.config, false)

        val overlay = bitmap.copy(bitmap.config, true)//Bitmap.createBitmap(bmp!!.width, bmp.height, bmp.config)

        requireActivity().runOnUiThread {
            mBinding?.canvasImageView?.setImageBitmap(overlay)
            mBinding?.canvasImageView?.rotation = 90f
        }

        if (boxes.isNotEmpty()) {

            var canvas = Canvas(overlay)

            canvas.drawRect(Rect(0, 0, bmp.width, bmp.height), sCanvasPaint)

            //var penny = boxes.minByOrNull { it.rect.width }!!

//            for (b in boxes) {
//
//                var rect = b.rect
//
//                //val diameter = ImageProcessingUtil.measureArea(penny.rect.width.toDouble(), 19.05, rect.width.toDouble())
//
//                canvas.drawRect(Rect(rect.x, rect.y,
//                        rect.x + rect.width,
//                        rect.y + rect.height),
//                        sCanvasTextPaint
//                )
//
////                    val circText = if (b.circ.toString().isNotBlank() && b.circ.toString().length > 5) {
////                        b.circ.toString().substring(0, 5)
////                    } else ""
////
////                    //canvas.drawText("circularity: $circText", rect.x.toFloat(), rect.y - 50f, textPaint)
////                    canvas.drawText("diameter: $diameter", rect.x.toFloat(), rect.y - 50f, textPaint)
//
//            }

            if (isShowingDialog == false) {
                callSeedCountDialog(src, result)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mExperimentId = requireArguments().getInt("experiment", -1)

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        checkCamPermissions.launch(android.Manifest.permission.CAMERA)

        with(mBinding) {

            getOutputDirectory()?.let { output ->

                outputDirectory = output

            }

            //starts camera thread executor, which must be destroyed/stopped in onDestroy
            cameraExecutor = Executors.newSingleThreadExecutor()

//            //viewModel.insert(ExperimentEntity(Experiment("test"), 1))

            this?.cameraCaptureButton?.setOnClickListener {

                if (mChosenResult?.isCompleted == true && !isShowingDialog) {

                    callCoinRecognitionDialog()

                } else Toast.makeText(requireContext(), R.string.frag_camera_not_enough_coins_found, Toast.LENGTH_LONG).show()

            }

        }

        return mBinding?.root
    }

    //externalMediaDirs pictures are located in /storage/primary/Android/media/org.wheatgenetics.onekk/OneKK
    private fun getOutputDirectory(): File? {

        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context?.filesDir
    }

    override fun onDestroy() {

        super.onDestroy()

        stopCameraAnalysis()

    }

    private fun stopCameraAnalysis() {

        cameraExecutor.shutdown()

    }

    /**
     * Basic CameraX initialization. This hooks together the image preview and the background
     * running camera analysis. Various camera configurations can happen here s.a torch light enabled.
     *
     * Calling this function with an analyzer will stop the previous analyzer.
     */
    //reference https://developer.android.com/training/camerax/analyze
    private fun startCameraAnalysis(analyzer: ImageAnalysis.Analyzer) {

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
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val cam = cameraProvider.bindToLifecycle(this as LifecycleOwner,
                            cameraSelector, preview, imageAnalyzer)

                    setupCamera(cam)


                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableOneTapFocus(cam: Camera) {

        mBinding?.viewFinder?.afterMeasured {

            mBinding?.viewFinder?.let { preview ->
                preview.setOnTouchListener { _, event ->
                    return@setOnTouchListener when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                    preview.width.toFloat(), preview.height.toFloat()
                            )
                            val autoFocusPoint = factory.createPoint(event.x, event.y)
                            try {
                                cam.cameraControl.startFocusAndMetering(
                                        FocusMeteringAction.Builder(
                                                autoFocusPoint,
                                                FocusMeteringAction.FLAG_AF
                                        ).apply {
                                            //focus only when the user tap the preview
                                            disableAutoCancel()
                                        }.build()
                                )
                            } catch (e: CameraInfoUnavailableException) {
                                Log.d("ERROR", "cannot access camera", e)
                            }
                            true
                        }
                        else -> false // Unhandled event.
                    }
                }
            }
        }
    }

    private fun enableAutoFocus(cam: Camera) {

        mBinding?.viewFinder?.let { preview ->
            preview.afterMeasured {
                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                        preview.width.toFloat(), preview.height.toFloat())
                val centerWidth = preview.width.toFloat() / 2
                val centerHeight = preview.height.toFloat() / 2
                //create a point on the center of the view
                val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
                try {
                    cam.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(
                                    autoFocusPoint,
                                    FocusMeteringAction.FLAG_AF
                            ).apply {
                                //auto-focus every 1 seconds
                                setAutoCancelDuration(1, TimeUnit.SECONDS)
                            }.build()
                    )
                } catch (e: CameraInfoUnavailableException) {
                    Log.d("ERROR", "cannot access camera", e)
                }
            }
        }
    }

    /**
     * Sets up auto focus and tap-to-focus functionality for the current camera instance.
     * https://stackoverflow.com/questions/58159891/how-to-auto-focus-with-android-camerax
     */
    private fun setupCamera(cam: Camera) {

        requireActivity().runOnUiThread {

            mBinding?.cameraTorchButton?.setOnClickListener {

                cam.cameraControl.enableTorch(cam.cameraInfo.torchState.value == 0)

            }
        }

        enableOneTapFocus(cam)

        enableAutoFocus(cam)

    }

    private fun callSeedCountDialog(src: Bitmap, result: ImageProcessingUtil.Companion.ContourResult) {

        isShowingDialog = true

        startCameraAnalysis(sNoopAnalysis)

        mBinding?.let { ui ->

            try {

                this@CameraFragment.activity?.let { activity ->

                    activity.runOnUiThread {

                        Dialogs.askAcceptableImage(
                                activity,
                                AlertDialog.Builder(activity),
                                getString(R.string.ask_coin_recognition_ok),
                                srcBitmap = src, dstBitmap = result.images.last(), { success ->

                            savePipelineToDatabase(result)
    //
                            isShowingDialog = false

                            startCameraAnalysis(sCoinAnalyzer)

                        }) {

                            isShowingDialog = false

                            startCameraAnalysis(sCoinAnalyzer)

                        } //restart coin analyzer on decline

                    }

                }

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }

    /**
     * Creates a Dialog that asks the user to accept or decline the image.
     * In this case, if the coin recognition step is accepted, the analyzer switches to the watershed algorithm.
     */
    private fun callCoinRecognitionDialog() {

        isShowingDialog = true

        startCameraAnalysis(sNoopAnalysis)

        mBinding?.let { ui ->

            try {

                this@CameraFragment.activity?.let { activity ->

                    val source = mChosenResult!!.images.first()
                    val result = mChosenResult!!.images.last()

                    Dialogs.askAcceptableImage(
                            activity,
                            AlertDialog.Builder(activity),
                            getString(R.string.ask_coin_recognition_ok),
                            srcBitmap = source, dstBitmap = result, { success ->

//                        savePipelineToDatabase(result)
//
//                        findNavController().navigate(CameraFragmentDirections.actionToScale(1))

                        isShowingDialog = false

                        /**
                         * begin the seed analyzer on the first image of the pipeline,
                         * TODO: send a modified image with coins masked out or use the result data as a coin detection heuristic
                         */
                        startCameraAnalysis(seedAnalyzer(source, mChosenResult!!.detections.map { it.contour }))

                    }) {

                        isShowingDialog = false

                        startCameraAnalysis(sCoinAnalyzer)

                    } //restart coin analyzer on decline

                }

            } catch (e: IOException) {

                e.printStackTrace()

            }
        }
    }

    inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }

    /**
     * TODO: for development, currently images are not saved
     */
    private fun savePipelineToDatabase(result: ImageProcessingUtil.Companion.ContourResult) {

        /**
         * When the coin recognition is accepted, the idea is to save all the pipeline images
         * as an analysis row. Images are saved in the externalMedia directory, but their uri's are
         * stored in the local database.
         */

        with(viewModel) {

            insert(AnalysisEntity(mExperimentId)).observeForever { rowid ->

                result.detections.forEach { contour ->

                    insert(ContourEntity(
                            Contour(contour.x,
                                    contour.y,
                                    contour.area,
                                    contour.minAxis.toDouble(),
                                    contour.maxAxis.toDouble(),
                                    contour.isCluster),
                            rowid))
                }

                result.images.first().let { image ->

                    val file = File(outputDirectory.path.toString(), "${UUID.randomUUID()}.png")

                    FileOutputStream(file).use { stream ->

                        image.compress(Bitmap.CompressFormat.PNG, 100, stream)

                    }

                    viewModel.insert(ImageEntity(Image(Uri.fromFile(file).path.toString(), DateUtil().getTime()), rowid.toInt()))
                }

                findNavController().navigate(CameraFragmentDirections.actionToContours(mExperimentId, rowid))

            }
        }
    }
}