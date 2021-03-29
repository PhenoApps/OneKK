package org.wheatgenetics.imageprocess

import android.content.Context
import android.util.Log
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

/**
 * base class that loads opencv libraries
 * image processing algorithms can extend this class to automatically link necessary jnis
 */
open class OpenCVTransformation(appContext: Context?) {

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(appContext) {

        override fun onManagerConnected(status: Int) {

            when (status) {

                LoaderCallbackInterface.SUCCESS -> {

                    Log.i("OpenCV", "OpenCV loaded successfully")

                }

                else -> {

                    super.onManagerConnected(status)

                }
            }
        }
    }

    init {

        appContext?.let {

            if (!OpenCVLoader.initDebug()) {

                Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization")

                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, it, mLoaderCallback)

            } else {

                Log.d("OpenCV", "OpenCV library found inside package. Using it!")

                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)

            }
        }
    }
}