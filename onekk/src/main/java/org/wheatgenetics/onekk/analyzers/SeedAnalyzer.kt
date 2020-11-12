package org.wheatgenetics.onekk.analyzers

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.imageprocess.DetectRectangles
import org.wheatgenetics.imageprocess.DetectSmallObjects
import org.wheatgenetics.onekk.activities.SeedAnalysisListener

class SeedAnalyzer(private val src: Bitmap, private val listener: SeedAnalysisListener) : ImageAnalysis.Analyzer, CoroutineScope by MainScope() {

    private var frames = 0.0
    private var startAnalysisTime = System.currentTimeMillis()

    val detector = DetectSmallObjects(src)

    /**
     * Skips the image proxy and runs the analysis on the source image.
     */
    override fun analyze(proxy: ImageProxy) {

        val startTime = System.currentTimeMillis()

        var result = detector.process()

        //Log.d(CameraFragment.TAG, "RenderScript: ${boxes.size} objects ${src.width}x${src.height}")
        // Log.d(CameraFragment.TAG, "SeedAnalyzer: ${System.currentTimeMillis() - startTime}")

        listener(result)

        proxy.close()

    }
}