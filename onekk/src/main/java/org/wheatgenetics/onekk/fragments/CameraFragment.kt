package org.wheatgenetics.onekk.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.FragmentCameraBinding
import org.wheatgenetics.utils.Dialogs
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    companion object {

        final val TAG = "Onekk.CameraFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var mBinding: FragmentCameraBinding? = null

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                startCamera()

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate<FragmentCameraBinding>(inflater, R.layout.fragment_camera, container, false)

        checkCamPermissions.launch(android.Manifest.permission.CAMERA)

        with(mBinding) {

            this?.cameraCaptureButton?.setOnClickListener {

                takePhoto()

            }

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

                    callCoinRecognitionDialog(savedUri)

                }
            })
        }
    }

    private fun callCoinRecognitionDialog(savedUri: Uri) {

        mBinding?.let { ui ->

            try {

                this@CameraFragment.activity?.let { activity ->

                    Dialogs.askAcceptableCoinRecognition(
                            activity,
                            AlertDialog.Builder(activity),
                            getString(R.string.ask_coin_recognition_ok),
                            savedUri) {

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

                // Preview
                val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(mBinding?.viewFinder?.createSurfaceProvider())
                        }

                imageCapture = ImageCapture.Builder()
                        .build()

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture)

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))
        }
    }
}