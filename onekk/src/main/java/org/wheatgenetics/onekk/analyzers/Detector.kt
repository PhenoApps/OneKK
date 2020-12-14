package org.wheatgenetics.onekk.analyzers

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.interfaces.DetectorListener

class Detector(private val listener: DetectorListener, referenceDiameter: Double): CoroutineScope by MainScope() {

//    private var frames = 0.0
//    private var startAnalysisTime = System.currentTimeMillis()

    val detector = org.wheatgenetics.imageprocess.DetectWithReferences(referenceDiameter)

    fun scan(src: Bitmap) {

        //val startTime = System.currentTimeMillis()

        detector.process(src).let { result ->

            //Log.d(CameraFragment.TAG, "CoinAnalyzer: ${System.currentTimeMillis() - startTime}")

            listener.onDetectorCompleted(result)
        }
    }
}