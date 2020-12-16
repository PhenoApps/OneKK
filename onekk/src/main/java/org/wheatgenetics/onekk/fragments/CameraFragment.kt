package org.wheatgenetics.onekk.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.android.synthetic.main.fragment_image.view.*
import kotlinx.coroutines.*
import org.wheatgenetics.imageprocess.DetectWithReferences
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.analyzers.Detector
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import org.wheatgenetics.onekk.database.models.embedded.Contour
import org.wheatgenetics.onekk.database.models.embedded.Image
import org.wheatgenetics.onekk.database.viewmodels.BarcodeSharedViewModel
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.BleStateListener
import org.wheatgenetics.onekk.interfaces.DetectorListener
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import org.wheatgenetics.utils.toBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This fragment uses the CameraX analysis API. The use-cases used are preview and image capture.
 * Image capture is used to take high resolution images. The capture mode prioritizes image quality over latency.
 * When the image capture button is pressed, an image is saved and the detector is run. Interfaces are implemented to
 * listen to detector results. When results occur, the user is asked to accept the detection. If it is accepted, the image
 * along with its results are saved to the local database.
 */
class CameraFragment : Fragment(), DetectorListener, BleStateListener, BleNotificationListener, CoroutineScope by MainScope() {

    private val db by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
    }

    private val barcodeViewModel: BarcodeSharedViewModel by activityViewModels()

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    private val mBluetoothManager by lazy {
        BluetoothUtil(requireContext()).also {
//            it.deviceStateListener(this)
        }
    }

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var mCamera: Camera? = null

    private var mBinding: FragmentCameraBinding? = null

    private var isShowingDialog: Boolean = false

    //query the relative screen size, used for creating a preview that matches the screen size
    private val metrics by lazy {

        val metrics = DisplayMetrics().also { mBinding?.viewFinder?.display?.getRealMetrics(it) }

        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        //val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        screenSize
    }

    private val checkPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

            //ensure all permissions are granted
            if (granted.values.all { it }) {

                setupFragment()

            } else {
                //TODO show message saying camera/bluetooth/storage is required to start camera preview
                with(requireActivity()) {
                    setResult(android.app.Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    /**
     * Start the detector given a user-chosen file.
     */
    private val importFile by lazy {

        registerForActivityResult(ActivityResultContracts.GetContent()) { doc ->

            doc?.let { nonNullDoc ->

                initiateDetector(BitmapFactory.decodeStream(requireContext()
                        .contentResolver.openInputStream(nonNullDoc.normalizeScheme())))
            }
        }
    }

    companion object {

        final val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private fun setupFragment() {

        startCameraAnalysis()

        startMacAddressSearch()

        setHasOptionsMenu(true)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        with(mBinding) {

            this?.weightEditText?.visibility = when(mPreferences.getString("org.wheatgenetics.onekk.SCALE_STEPS", "1")) {
                "1" -> View.VISIBLE
                else -> View.GONE
            }

            getOutputDirectory()?.let { output ->

                outputDirectory = output

            }

            barcodeViewModel.lastScan.observe(viewLifecycleOwner, {

                mBinding?.nameEditText?.setText(it)

            })
        }

        checkPermissions.launch(arrayOf(android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

        return mBinding?.root

    }

    private fun startMacAddressSearch() {

        val macAddress = mPreferences.getString(getString(R.string.preferences_enable_bluetooth_key), null)

        if (macAddress != null) {

            mBluetoothManager.establishConnectionToAddress(this, macAddress)

        } else {

            //TODO: Instead of moving to Settings, the service can be automatically found (if it's available)
            //Toast.makeText(requireContext(), getString(R.string.frag_scale_no_mac_address_found_message), Toast.LENGTH_LONG).show()

            //findNavController().navigate(ScaleFragmentDirections.actionToSettings())

        }
    }

    /**
     * The interface implementation which is sent to setupDeviceComms
     * This will read any notification that is received from the device.
     */
    override fun onNotification(bytes: ByteArray) {

        val stringResult = ValueInterpreter.getStringValue(bytes, 0)

        if (stringResult.isNotBlank()) {

            scaleTextUpdateUi(stringResult)

        }
    }

    /**
     * Disposables are destroyed so BLE connections are lost when the app is sent to background.
     */
    override fun onPause() {
        super.onPause()

        mBluetoothManager.dispose()

        stopCameraAnalysis()
    }

    /**
     * Reads the preferences for the current selected reference and runs the detector.
     */
    private fun initiateDetector(image: Bitmap) {

        mBinding?.toggleDetectorProgress(true)

        //default is the size for a quarter
        val diameter = mPreferences.getString(getString(R.string.onekk_coin_pref_key), "24.26")?.toDoubleOrNull() ?: 24.26

        try {

            Detector(this@CameraFragment, diameter).scan(image)

        } catch (e: Exception) {

            e.printStackTrace()

            mBinding?.toggleDetectorProgress(false)

        }
    }

    /**
     * Function that updates the scale measurement UI which can be called from other threads.
     */
    //TODO: use ohaus commands to format the output text to not include newlines.
    private fun scaleTextUpdateUi(value: String) = with(requireActivity()) {

        //format the result
        val weight = value.replace("\n", "").split("g")[0].replace(" ", "")

        runOnUiThread {

            findViewById<TextView>(R.id.weightEditText)?.text = weight

        }
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

        mBluetoothManager.dispose()

        stopCameraAnalysis()

    }

    private fun stopCameraAnalysis() {

        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()

    }

    /**
     * Basic CameraX initialization. This hooks together the image preview and the background
     * running camera analysis. Various camera configurations can happen here s.a torch light enabled.
     *
     * Calling this function with an analyzer will stop the previous analyzer.
     */
    //reference https://developer.android.com/training/camerax/analyze
    private fun startCameraAnalysis() {

        //starts camera thread executor, which must be destroyed/stopped in onDestroy
        cameraExecutor = Executors.newSingleThreadExecutor()

        requireContext().let { ctx ->

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

                val highResImageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                mBinding?.cameraCaptureButton?.setOnClickListener {

                    highResImageCapture.takePicture(cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    image.use {

                                        initiateDetector(it.toBitmap())

                                    }
                                    super.onCaptureSuccess(image)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    super.onError(exception)
                                    println("error")
                                }
                            })

                }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    val cam = cameraProvider.bindToLifecycle(this as LifecycleOwner,
                            cameraSelector, preview, highResImageCapture)

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

        mCamera = cam

        enableOneTapFocus(cam)

        enableAutoFocus(cam)

    }

    /**
     * Creates a Dialog that asks the user to accept or decline the image.
     */
    private fun callCoinRecognitionDialog(result: DetectWithReferences.Result) {

        if (!isShowingDialog) {

            isShowingDialog = true

            mBinding?.let { ui ->

                try {

                    this@CameraFragment.activity?.let { activity ->

                        activity.runOnUiThread {

                            val src = result.src
                            val dst = result.dst

                            if (mPreferences.getBoolean("org.wheatgenetics.onekk.DISPLAY_ANALYSIS", true)) {

                                Dialogs.askAcceptableImage(
                                        activity,
                                        AlertDialog.Builder(activity),
                                        getString(R.string.ask_coin_recognition_ok),
                                        srcBitmap = src, dstBitmap = dst, { success ->

                                    savePipelineToDatabase(result)

                                    isShowingDialog = false

                                }) {

                                    isShowingDialog = false

                                }

                            } else {

                                savePipelineToDatabase(result)
                            }
                        }
                    }

                } catch (e: IOException) {

                    e.printStackTrace()

                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_camera_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            //toggle the torch when the option is clicked
            R.id.action_flash_enable -> {

                mCamera?.let { cam ->

                    cam.cameraControl.enableTorch(cam.cameraInfo.torchState.value == 0)
                }

            }

            R.id.action_import_file -> {

                importFile.launch("image/*")

            }

            R.id.action_barcode_scan -> {

                findNavController().navigate(CameraFragmentDirections.actionToBarcodeScanner())

            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
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
     * When the coin recognition is accepted, the idea is to save all the pipeline images
     * as an analysis row. Images are saved in the externalMedia directory, but their uri's are
     * stored in the local database.
     *
     * When the one-step process is used, the weight is also stored into the analysis row.
     */
    private fun savePipelineToDatabase(result: DetectWithReferences.Result) {

        launch(Dispatchers.IO) {

            val weight = when(mPreferences.getString("org.wheatgenetics.onekk.SCALE_STEPS", "1")) {
                "1" -> mBinding?.weightEditText?.text?.toString()?.toDoubleOrNull()
                else -> null
            }

            val name = mBinding?.nameEditText?.text?.toString() ?: String()

            if (name.isNotBlank()) {

                val rowid = viewModel.insert(AnalysisEntity(name = name,
                        date = DateUtil().getTime(), weight = weight)).toInt()

                launch {

                    with(viewModel) {

                        var count = 0.0

                        result.contours.forEach { contour ->

                            insert(ContourEntity(
                                    Contour(contour.x,
                                            contour.y,
                                            contour.count,
                                            contour.area,
                                            contour.minAxis,
                                            contour.maxAxis),
                                    selected = true,
                                    aid = rowid))

                            count += contour.count
                        }

                        updateAnalysisCount(rowid, count.toInt())

                        result.dst.let { image ->

                            val file = File(outputDirectory.path.toString(), "${UUID.randomUUID()}.png")

                            FileOutputStream(file).use { stream ->

                                image.compress(Bitmap.CompressFormat.PNG, 100, stream)

                            }

                            Glide.with(requireContext()).asBitmap().load(file.toUri()).fitCenter().preload()

                            viewModel.insert(ImageEntity(Image(Uri.fromFile(file).path.toString(), DateUtil().getTime()), rowid.toInt()))
                        }
                    }
                }

                requireActivity().runOnUiThread {

                    val mode = mPreferences.getString(getString(R.string.onekk_preference_mode_key), "1")
                    when(mode) {
                        "2", "3" -> findNavController().navigate(CameraFragmentDirections.actionToContours(rowid))
                    }

                }

            } else {

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), R.string.frag_camera_no_sample_name_message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * BLE listener implementation for reading the bluetooth adapter state.
     * Uses RxBleClient in BluetoothUtil.
     */
    override fun onStateChanged(state: RxBleClient.State) {

        when (state) {

            RxBleClient.State.READY -> {

                startMacAddressSearch()

            }

            else -> {

                //display a message if bt disconnects
                mBluetoothManager.dispose()
            }
        }
    }

    /**
     * Simple UI method for toggling the progress bar and button.
     */
    private fun FragmentCameraBinding.toggleDetectorProgress(toggle: Boolean) = requireActivity().runOnUiThread {

        when (toggle) {

            true -> {
                progressBar.visibility = View.VISIBLE
                cameraCaptureButton.visibility = View.GONE
            }

            else -> {
                progressBar.visibility = View.GONE
                cameraCaptureButton.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Listener method that is called from the analyzer class. This result holds
     * the output of the detection algorithm including src/dst images, and detected contours with stats.
     */
    override fun onDetectorCompleted(result: DetectWithReferences.Result) {

        mBinding?.toggleDetectorProgress(false)

        callCoinRecognitionDialog(result)

    }
}