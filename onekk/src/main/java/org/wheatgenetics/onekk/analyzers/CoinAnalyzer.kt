package org.wheatgenetics.onekk.analyzers

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.imageprocess.DetectRectangles
import org.wheatgenetics.onekk.activities.CoinAnalysisListener
import org.wheatgenetics.onekk.fragments.CameraFragment
import org.wheatgenetics.utils.toBitmap

class CoinAnalyzer(private val listener: CoinAnalysisListener) : ImageAnalysis.Analyzer, CoroutineScope by MainScope() {

    private var frames = 0.0
    private var startAnalysisTime = System.currentTimeMillis()

    val detector = DetectRectangles()

    override fun analyze(proxy: ImageProxy) {

        proxy.toBitmap().let { src ->

            val startTime = System.currentTimeMillis()

            detector.process(src)?.let { result ->

                Log.d(CameraFragment.TAG, "CoinAnalyzer: ${System.currentTimeMillis() - startTime}")

                listener(result)
            }
        }

        proxy.close()

    }

}