package org.wheatgenetics.onekk.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.activities.MainActivity
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
import org.wheatgenetics.onekk.observeOnce
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

    private val viewModel by viewModels<ExperimentViewModel> {
        with(OnekkDatabase.getInstance(requireContext())) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    private val mPreferences by lazy {
        context?.getSharedPreferences(getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    private val mBluetoothManager by lazy {
        BluetoothUtil(requireContext())
    }

    private lateinit var captureDirectory: File
    private lateinit var analyzedDirectory: File

    //starts camera thread executor, which must be destroyed/stopped in onDestroy
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var mCamera: Camera? = null

    private var mBinding: FragmentCameraBinding? = null

    private var isShowingDialog: Boolean = false

    //global variable to track when weight capture button is locked
    private var isLocked: Boolean = false

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

        val mPreferences = context?.getSharedPreferences(getString(R.string.onekk_preference_key),
            AppCompatActivity.MODE_PRIVATE
        )

        val weighAndCapture = mPreferences?.getString(getString(R.string.onekk_preference_mode_key),
            getString(R.string.frag_setting_scale_mode_1))

        when (weighAndCapture) {
            "2" -> {

                //if a device is already chosen, then start the service search/connection
                if (mPreferences.getString(getString(R.string.preferences_enable_bluetooth_key), String())?.isEmpty() == true) {

                    //if a device is not already chosen and its the first time loading this fragment, ask if the user wants to connect to a device
                    when (mPreferences.getBoolean(getString(R.string.onekk_first_scale_fragment_ask_mac_address), true)) {

                        true -> {

                            callAskConnectDialog()

                            //set first time dialog to false so it is only ever asked once
                            mPreferences.edit().putBoolean(getString(R.string.onekk_first_scale_fragment_ask_mac_address), false).apply()

                        }
                        else ->  {

                            //finally, if it's not the first time, check the ask connect preference to ask anyways
                            if (mPreferences.getBoolean("org.wheatgenetics.onekk.ASK_CONNECT", true)) {

                                callAskConnectDialog()

                            }
                        }
                    }
                } else {

                    //if a device is already preferred, try connecting to it
                    startMacAddressSearch()

                }
            }
        }

        when (mPreferences?.getBoolean(getString(R.string.onekk_first_sample_load), true)) {

            true -> {

                askLoadSample()

                mPreferences.edit().putBoolean(getString(R.string.onekk_first_sample_load), false).apply()

            }
        }
    }

    private fun callAskConnectDialog() {

        Dialogs.askWithNeutral(AlertDialog.Builder(requireContext()),
            getString(R.string.camera_fragment_dialog_first_load_ask_address),
            getString(R.string.cancel),
            getString(R.string.camera_fragment_dialog_first_load_ok),
            getString(R.string.dialog_pair_dont_ask_again)) { theyWantToSetAddress ->

            if (theyWantToSetAddress == 1) {

                startMacAddressSearch()

            } else if (theyWantToSetAddress == 0) {

                mPreferences?.edit()
                    ?.putBoolean("org.wheatgenetics.onekk.ASK_CONNECT", false)
                    ?.apply()
            }
        }
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

            getAnalyzedDirectory()?.let { dir ->

                analyzedDirectory = dir

            }

            with (this?.weightLockButton) {

                this?.setOnClickListener {

                    isLocked = when (isLocked) {
                        true -> {
                            this.setImageResource(R.drawable.ic_weight_unlock)
                            mBinding?.weightEditText?.isEnabled = true
                            false
                        }
                        else -> {
                            this.setImageResource(R.drawable.ic_weight_lock)
                            mBinding?.weightEditText?.isEnabled = false
                            true
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
            (activity as? MainActivity)?.invalidateMenu()
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
     * Loads/inserts data from assets/samples into the database on cold load.
     */
    private fun askLoadSample() {
        context?.let { ctx ->

            launch(Dispatchers.IO) {

                //parse sample export file to get stats
                val sampleTokens = ctx.assets.open("samples/sample_sample.csv")
                    .bufferedReader().readLines().drop(1).first().split(",").map { x -> x.trim() }

                //decode and save capture and analyzed photos to the respective directories
                val src = BitmapFactory.decodeStream(context?.assets?.open("samples/sample_original.jpg"))
                    .toFile(captureDirectory.path, "sample.jpg")

                val analyzedSample = BitmapFactory.decodeStream(context?.assets?.open("samples/sample_analyzed.png"))
                    .toFile(analyzedDirectory.path, "sample.png")

                //insert the analysis into the database, count is not in the export file format, so it is defined statically here
                val aid = viewModel.insert(AnalysisEntity(
                    name = "sample",
                    date = DateUtil().getTime(),
                    uri = analyzedSample.toUri().path,
                    src = src.toUri().path,
                    count = 74,
                    maxAxisAvg = sampleTokens[4].toDoubleOrNull(),
                    maxAxisVar = sampleTokens[5].toDoubleOrNull(),
                    maxAxisCv = sampleTokens[6].toDoubleOrNull(),
                    minAxisAvg = sampleTokens[7].toDoubleOrNull(),
                    minAxisVar = sampleTokens[8].toDoubleOrNull(),
                    minAxisCv = sampleTokens[9].toDoubleOrNull())).toInt()

                //insert the image paths into the database, allowing it to be viewed in the contour viewer
                viewModel.insert(ImageEntity(Image(analyzedSample.path, example = null), aid))

                //parse and insert each contour from the seed_sample file
                ctx.assets.open("samples/seed_sample.csv")
                    .bufferedReader().readLines().drop(1).forEach {
                        val tokens = it.split(",").map { x -> x.trim() }
                        viewModel.insert(ContourEntity(Contour(
                            area = tokens[4].toDouble(),
                            count = tokens[1].toInt(),
                            maxAxis = tokens[2].toDoubleOrNull(),
                            minAxis = tokens[3].toDoubleOrNull(),
                            x = tokens[6].toDouble(),
                            y = tokens[7].toDouble(),
                        ), aid = aid))
                    }
            }
        }
    }

    /**
     * Reads the preferences for the current selected reference and runs the detector.
     * Save parameter designates whether the file was imported or not. If it was not imported then
     * the source image must be saved to the capture directory. Otherwise, we must use the content
     * resolver to load the image.
     */
    private fun initiateDetector(path: String, name: String? = null) {

        //default is the size for a quarter
        val coinDiameter = mPreferences?.getString(getString(R.string.onekk_coin_pref_diameter_key), "19.05")?.toDoubleOrNull() ?: 19.05
        val manualDiameter = mPreferences?.getString("org.wheatgenetics.onekk.REFERENCE_MANUAL_DIAMETER", "19.05")?.toDoubleOrNull() ?: 19.05
        val referenceType = mPreferences?.getInt("org.wheatgenetics.onekk.REFERENCE_TYPE", 1)
        val diameter = if (referenceType == 1) coinDiameter else manualDiameter
        val measure = mPreferences?.getString("org.wheatgenetics.onekk.MEASURE_TYPE", "0")

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
                "measure" to measure,
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

                                resetUi()

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

    private fun resetUi() {

        this.activity?.runOnUiThread {

            barcodeViewModel.lastScan.value = ""

            mBinding?.nameEditText?.setText("")

            mBinding?.weightEditText?.setText("")

            mBinding?.weightEditText?.isEnabled = true

            mBinding?.weightLockButton?.setImageResource(R.drawable.ic_weight_unlock)

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

        val weightText = weight.toDoubleOrNull() ?: 0.0

        if (!isLocked) {
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

    private fun getAnalyzedDirectory(): File? {

        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "analyzed").apply { mkdirs() } }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context?.filesDir
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
                    val algorithm = mPreferences?.getString(getString(R.string.onekk_preference_algorithm_mode_key), "0") ?: "0"
                    val collector = mPreferences?.getString(getString(R.string.onekk_preference_collector_key), "") ?: ""

                    if (name.isNotBlank() && (scaleMode != "2" || weight != null)) {

                        cameraProvider.unbind(preview)

                        mBinding?.toggleDetectorProgress(true)

                        //if something goes wrong with camerax, catch the exception and reset ui
                        try {
                            highResImageCapture.takePicture(cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {

                                    override fun onCaptureSuccess(proxy: ImageProxy) {

                                        val date = DateUtil().getTime()
                                        val fileName = "${name}_$date.png"

                                        val file = proxy.toBitmap().toFile(
                                            captureDirectory.path,
                                            fileName
                                        )

                                        proxy.close()

                                        if (algorithm != "2") {
                                            initiateDetector(file.path, name)
                                        } else {

                                            //if no algorithm is run, just insert a blank analysis/image entity
                                            launch {
                                                val rowid = viewModel.insert(
                                                    AnalysisEntity(
                                                        name = name,
                                                        collector = collector,
                                                        uri = file.path,
                                                        src = file.path,
                                                        date = date,
                                                        weight = weight)
                                                ).toInt()

                                                viewModel.insert(ImageEntity(Image(file.path, null, date), rowid))

                                            }

                                            resetUi()
                                        }

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

                    }, {

                        isShowingDialog = false

                        findNavController().navigate(CameraFragmentDirections.actionToContours(rowid))

                    }) {

                        isShowingDialog = false

                        //get the saved analysis to delete the photo from analysis&captured directory
                        viewModel.getAnalysis(rowid).observeOnce(viewLifecycleOwner) { analysis ->
                            analysis.uri?.let { uri ->
                                File(uri).delete()
                            }
                            analysis.src?.let { src ->
                                File(src).delete()
                            }
                            //cascade delete everything related to this analysis
                            viewModel.deleteAnalysis(rowid)

                            mBinding?.nameEditText?.setText(analysis.name)
                        }
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