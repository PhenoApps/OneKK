package org.wheatgenetics.onekk.analyzers

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.activities.CoinAnalysisListener
import org.wheatgenetics.onekk.fragments.CameraFragment
import org.wheatgenetics.utils.toBitmap

class CoinAnalyzer(private val referenceDiameter: Double, private val src: Bitmap? = null, private val listener: CoinAnalysisListener) : ImageAnalysis.Analyzer, CoroutineScope by MainScope() {

    private var frames = 0.0
    private var startAnalysisTime = System.currentTimeMillis()

    val detector = org.wheatgenetics.imageprocess.DetectWithReferences(referenceDiameter)

    override fun analyze(proxy: ImageProxy) {

        if (src == null) {

            proxy.toBitmap().let { bmp ->

                run(bmp)

            }

            proxy.close()

        } else run(src)

    }

    private fun run(src: Bitmap) {

        val startTime = System.currentTimeMillis()

        try {

            detector.process(src)?.let { result ->

                Log.d(CameraFragment.TAG, "CoinAnalyzer: ${System.currentTimeMillis() - startTime}")

                listener(result)
            }

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }
}