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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.viewmodels.BarcodeSharedViewModel
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.BleStateListener
import org.wheatgenetics.onekk.toBitmap
import org.wheatgenetics.onekk.toFile
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import org.wheatgenetics.workers.ImageSaveWorker
import java.io.File
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
class CameraFragment : Fragment(), BleStateListener, BleNotificationListener, CoroutineScope by MainScope() {

    private val barcodeViewModel: BarcodeSharedViewModel by activityViewModels()

    private val mPreferences by lazy {
        context?.getSharedPreferences(getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    private val mBluetoothManager by lazy {
        BluetoothUtil(requireContext())
    }

    private lateinit var captureDirectory: File

    //starts camera thread executor, which must be destroyed/stopped in onDestroy
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var mCamera: Camera? = null

    private var mBinding: FragmentCameraBinding? = null

    private var isShowingDialog: Boolean = false

//    //query the relative screen size, used for creating a preview that matches the screen size
//    private val metrics by lazy {
//
//        val metrics = DisplayMetrics().also { mBinding?.viewFinder?.display?.getRealMetrics(it) }
//
//        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
//
//        //val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
//
//        screenSize
//    }

    private val checkPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

            //ensure all permissions are granted
            if (granted.values.all { it }) {

                setupFragment()

            } else {
                //TODO show message saying camera/bluetooth/storage is required to start camera preview
                activity?.let {
                    it.setResult(android.app.Activity.RESULT_CANCELED)
                    it.finish()
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

                initiateDetector(nonNullDoc.toString())
//                initiateDetector(BitmapFactory.decodeStream(requireContext()
//                        .contentResolver.openInputStream(nonNullDoc)), name, save = false)
            }
        }
    }

    private fun Uri.parseName(): String {
        //imported file uris are parsed for their names to use as the sample name
        val path = this.lastPathSegment.toString()

        return if ("/" in path) {
            path.split("/").last()
        } else path
    }

    companion object {

        const val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

    private fun setupFragment() {

        startCameraAnalysis()

        startMacAddressSearch()

//        setHasOptionsMenu(true)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Modify the UI based on the scale input mode. If weight is not taken,
         * make the corresponding views invisible and reconnect the constraints.
         */
        when (mPreferences?.getString(getString(R.string.onekk_preference_mode_key), "1")) {
            "1", "3" -> {
                mBinding?.weightLockButton?.visibility = View.GONE
                mBinding?.weightEditText?.visibility = View.GONE
                val constraintSet = ConstraintSet()
                constraintSet.clone(mBinding?.parent)
                constraintSet.connect(R.id.nameEditText, ConstraintSet.END, R.id.camera_capture_button, ConstraintSet.START)
                constraintSet.connect(R.id.camera_capture_button, ConstraintSet.START, R.id.nameEditText, ConstraintSet.END)
                constraintSet.connect(R.id.camera_capture_button, ConstraintSet.BOTTOM, R.id.parent, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.nameEditText, ConstraintSet.BOTTOM, R.id.parent, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.barcode_scan_button, ConstraintSet.BOTTOM, R.id.parent, ConstraintSet.BOTTOM)
                constraintSet.connect(R.id.progress_bar, ConstraintSet.START, R.id.nameEditText, ConstraintSet.END)
                constraintSet.connect(R.id.progress_bar, ConstraintSet.END, R.id.parent, ConstraintSet.END)

                constraintSet.applyTo(mBinding?.parent)

                val params = mBinding?.nameEditText?.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = context?.resources?.getDimension(R.dimen.default_margin)?.toInt() ?: 8
                mBinding?.nameEditText?.layoutParams = params
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        with(mBinding) {

            getCaptureDirectory()?.let { dir ->

                captureDirectory = dir

            }

            with (this?.weightLockButton) {

                this?.tag = "unlocked"

                this?.setOnClickListener {

                    this.tag = when (tag) {
                        "locked" -> {
                            this.setImageResource(R.drawable.ic_weight_unlock)
                            mBinding?.weightEditText?.isEnabled = true
                            "unlocked"
                        }
                        else -> {
                            this.setImageResource(R.drawable.ic_weight_lock)
                            mBinding?.weightEditText?.isEnabled = false
                            "locked"
                        }
                    }
                }
            }

            barcodeViewModel.lastScan.observe(viewLifecycleOwner, {

                mBinding?.nameEditText?.setText(it)

            })

            this?.barcodeScanButton?.setOnClickListener {
                findNavController().navigate(CameraFragmentDirections.actionToBarcodeScanner())
            }

            val flashButton = this?.toolbar?.findViewById<ImageButton>(R.id.flashButton)
            flashButton?.setOnClickListener {
                mCamera?.let { cam ->
                    cam.cameraControl.enableTorch(cam.cameraInfo.torchState.value == 0)
                    flashButton.setImageResource(when (cam.cameraInfo.torchState.value) {
                        0 -> R.drawable.ic_code_scanner_flash_off
                        else -> R.drawable.ic_code_scanner_flash_on
                    })
                }
            }
        }

        when (mPreferences?.getBoolean(getString(R.string.onekk_first_load_ask_mac_address), true)) {

            true -> {
                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        getString(R.string.camera_fragment_dialog_first_load_ask_address),
                        getString(android.R.string.cancel),
                        getString(R.string.camera_fragment_dialog_first_load_ok)) { theyWantToSetAddress ->

                    if (theyWantToSetAddress) {

                        findNavController().navigate(CameraFragmentDirections.globalActionToSettings())

                    }
                }
            }
        }

        mPreferences?.edit()?.putBoolean(getString(R.string.onekk_first_load_ask_mac_address), false)?.apply()

        checkPermissions.launch(arrayOf(android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

        if (requireArguments().getString("mode") == "import") {
            importFile.launch("image/*")
            arguments?.putString("mode", "default")
        }

        return mBinding?.root

    }

    private fun startMacAddressSearch() {

        val macAddress = mPreferences?.getString(getString(R.string.preferences_enable_bluetooth_key), null)

        if (macAddress != null) {

            mBluetoothManager.establishConnectionToAddress(this, macAddress)

        }
//        else {
//
//            //TODO: Instead of moving to Settings, the service can be automatically found (if it's available)
//            //Toast.makeText(requireContext(), getString(R.string.frag_scale_no_mac_address_found_message), Toast.LENGTH_LONG).show()
//
//            //findNavController().navigate(ScaleFragmentDirections.actionToSettings())
//
//        }
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
     * Save parameter designates whether the file was imported or not. If it was not imported then
     * the source image must be saved to the capture directory. Otherwise, we must use the content
     * resolver to load the image.
     */
    private fun initiateDetector(path: String, name: String? = null) {

        //default is the size for a quarter
        val diameter = mPreferences?.getString(getString(R.string.onekk_coin_pref_key), "24.26")?.toDoubleOrNull() ?: 24.26

        val weight = when (mPreferences?.getString("org.wheatgenetics.onekk.SCALE_STEPS", "1")) {
            "1", "2" -> mBinding?.weightEditText?.text?.toString()?.toDoubleOrNull()
            else -> null
        }

        val key = getString(R.string.onekk_preference_algorithm_mode_key)
        val algorithm = mPreferences?.getString(key, "0") ?: "0"

        //parse the name if its an imported file
        val savedFileName = name ?: path.toUri().parseName()

        val data = workDataOf(
                "algorithm" to algorithm,
                "name" to savedFileName,
                "path" to path,
                "weight" to weight,
                "imported" to (name == null),
                "diameter" to diameter)

        val request = OneTimeWorkRequestBuilder<ImageSaveWorker>()
                .setInputData(data)
                .build()

        val manager = WorkManager.getInstance(requireContext()).also {
            it.enqueue(request)
        }

        activity?.runOnUiThread {

            cameraExecutor.shutdown()

            mBinding?.toggleDetectorProgress(true)

            mBinding?.nameEditText?.setText(name)

            mBinding?.progressBar?.setOnClickListener {

                manager.cancelWorkById(request.id)

            }


            manager.getWorkInfoByIdLiveData(request.id).observe(this@CameraFragment, {

                if (it != null) {

                    if (it.state.isFinished) {

                        cameraExecutor = Executors.newSingleThreadExecutor()

                        when (it.state) {
                            WorkInfo.State.CANCELLED -> {

                                Toast.makeText(requireContext(), R.string.frag_camera_sample_cancelled, Toast.LENGTH_SHORT).show()

                            }
                            WorkInfo.State.SUCCEEDED -> {

                                barcodeViewModel.lastScan.value = ""

                                mBinding?.nameEditText?.setText("")

                                mBinding?.weightEditText?.setText("")

                                mBinding?.weightEditText?.isEnabled = true

                                mBinding?.weightLockButton?.setImageResource(R.drawable.ic_weight_unlock)

                                it.outputData.getString("dst")?.let { outputImage ->

                                    val dst = BitmapFactory.decodeFile(outputImage)

                                    callDialog(dst, it.outputData.getInt("rowid", -1))

                                    Toast.makeText(requireContext(), R.string.frag_camera_sample_saved, Toast.LENGTH_SHORT).show()

                                }
                            }
                            else -> { //WorkInfo.State.FAILED -> {

                                Toast.makeText(requireContext(), R.string.frag_camera_sample_failed, Toast.LENGTH_SHORT).show()

                            }
                        }

                        //in case jni causes memory leaks
                        System.gc()

                        mBinding?.toggleDetectorProgress(false)

                    }
                }
            })
        }
    }

    /**
     * Function that updates the scale measurement UI which can be called from other threads.
     */
    //TODO: use ohaus commands to format the output text to not include newlines.
    private fun scaleTextUpdateUi(value: String) = with(requireActivity()) {

        //format the result
        val weight = value.replace("\n", "").split("g")[0].replace(" ", "")

        val weightText = weight.toDoubleOrNull() ?: 0.0

        if (mBinding?.weightEditText?.tag == "unlocked") {
            runOnUiThread {

                findViewById<TextView>(R.id.weightEditText)?.text = weightText.toString()

            }
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        mBluetoothManager.dispose()

        stopCameraAnalysis()

    }

    private fun stopCameraAnalysis() {

        cameraExecutor.shutdown()

    }

    private fun getCaptureDirectory(): File? {

        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "captures").apply { mkdirs() } }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context?.filesDir
    }

    /**
     * Basic CameraX initialization. This hooks together the image preview and the background
     * running camera analysis. Various camera configurations can happen here s.a torch light enabled.
     *
     * Calling this function with an analyzer will stop the previous analyzer.
     */
    //reference https://developer.android.com/training/camerax/analyze
    private fun startCameraAnalysis() {

        context?.let { ctx ->

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                //Preview
                val preview = Preview.Builder()
//                        .setTargetResolution(metrics)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build()
                        .also {
                            it.setSurfaceProvider(mBinding?.viewFinder?.surfaceProvider)
                        }

                val highResImageCapture = ImageCapture.Builder()
//                        .setTargetResolution(metrics)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                mBinding?.cameraCaptureButton?.setOnClickListener {

                    val name = mBinding?.nameEditText?.text?.toString() ?: String()
                    val weight = mBinding?.weightEditText?.text?.toString()?.toDoubleOrNull()

                    val scaleMode = mPreferences?.getString(getString(R.string.onekk_preference_mode_key), "1") ?: "1"

                    if (name.isNotBlank() && (scaleMode != "2" || weight != null)) {

                        cameraProvider.unbind(preview)

                        mBinding?.toggleDetectorProgress(true)

                        //if something goes wrong with camerax, catch the exception and reset ui
                        try {
                            highResImageCapture.takePicture(cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {

                                    override fun onCaptureSuccess(proxy: ImageProxy) {

                                        val fileName = "${name}_${DateUtil().getTime()}.png"

                                        val file = proxy.toBitmap().toFile(
                                            captureDirectory.path,
                                            fileName
                                        )

                                        proxy.close()

                                        initiateDetector(file.path, name)

                                        super.onCaptureSuccess(proxy)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        super.onError(exception)

                                        exception.printStackTrace()

                                        mBinding?.toggleDetectorProgress(true)

                                        Toast.makeText(context, R.string.frag_camera_camerax_error, Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } catch (e: Exception) {

                            e.printStackTrace()

                            mBinding?.toggleDetectorProgress(false)

                            Toast.makeText(context, R.string.frag_camera_camerax_error, Toast.LENGTH_SHORT).show()

                        }

                    } else {

                        //display input error messages
                        activity?.runOnUiThread {

                            if (name.isBlank()) {

                                Toast.makeText(requireContext(), R.string.frag_camera_no_sample_name_message, Toast.LENGTH_SHORT).show()

                            }

                            if (scaleMode == "2" && weight == null) {

                                Toast.makeText(requireContext(), R.string.frag_camera_no_weight_message, Toast.LENGTH_SHORT).show()

                            }
                        }
                    }
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

//    https://stackoverflow.com/questions/63202209/camerax-how-to-add-pinch-to-zoom-and-tap-to-focus-onclicklistener-and-ontouchl
    @SuppressLint("ClickableViewAccessibility")
    private fun setupCamera(cam: Camera) {

        mCamera = cam

        mBinding?.viewFinder?.let { preview ->

            val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentZoomRatio: Float = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1F
                    val delta = detector.scaleFactor
                    cam.cameraControl.setZoomRatio(currentZoomRatio * delta)
                    return true
                }
            }

            val scaleGestureDetector = ScaleGestureDetector(preview.context, listener)

            preview.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val point = preview.meteringPointFactory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(5, TimeUnit.SECONDS)
                            .build()
                    cam.cameraControl.startFocusAndMetering(action)
                }
                true
            }
        }
    }

    private fun callDialog(dst: Bitmap, rowid: Int) {

        val scaleMode = mPreferences?.getString(getString(R.string.onekk_preference_mode_key), "1") ?: "1"

        if (!isShowingDialog) {

            isShowingDialog = true

            if (mPreferences?.getBoolean("org.wheatgenetics.onekk.DISPLAY_ANALYSIS", true) != false) {

                activity?.runOnUiThread {

                    Dialogs.askAcceptableImage(
                            requireActivity(),
                            AlertDialog.Builder(requireActivity()),
                            getString(R.string.ask_coin_recognition_ok), dst, {

                        isShowingDialog = false

                        if (scaleMode == "3") findNavController().navigate(CameraFragmentDirections.actionToScale(rowid))

                    }) {

                        isShowingDialog = false

                        findNavController().navigate(CameraFragmentDirections.actionToContours(rowid))

                    }
                }

            } else if (scaleMode == "3") {

                findNavController().navigate(CameraFragmentDirections.actionToScale(rowid))

            }

        }
    }

//    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
//        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                if (measuredWidth > 0 && measuredHeight > 0) {
//                    viewTreeObserver.removeOnGlobalLayoutListener(this)
//                    block()
//                }
//            }
//        })
//    }

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
    private fun FragmentCameraBinding.toggleDetectorProgress(toggle: Boolean) = activity?.runOnUiThread {

        when (toggle) {

            true -> {
                progressBar.visibility = View.VISIBLE
                cameraCaptureButton.visibility = View.INVISIBLE
            }

            else -> {
                progressBar.visibility = View.INVISIBLE
                cameraCaptureButton.visibility = View.VISIBLE
                startCameraAnalysis()
            }
        }
    }
}