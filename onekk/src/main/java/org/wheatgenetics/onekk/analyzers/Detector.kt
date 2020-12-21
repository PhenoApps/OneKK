package org.wheatgenetics.onekk.analyzers

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.onekk.interfaces.DetectorListener
import java.io.File

class Detector(private val algorithm: String, private val dir: File, private val listener: DetectorListener, referenceDiameter: Double, private val imported: File? = null): CoroutineScope by MainScope() {

//    private var frames = 0.0
//    private var startAnalysisTime = System.currentTimeMillis()

    val detector: DetectorAlgorithm = if (algorithm == "1") {
        org.wheatgenetics.imageprocess.DetectWithReferences(File(dir, "Temp").also { it.mkdir() }, referenceDiameter)
    } else {
        org.wheatgenetics.imageprocess.WatershedWithReferences(File(dir, "Temp").also { it.mkdir() }, referenceDiameter)
    }

    fun scan(src: Bitmap) {

        //val startTime = System.currentTimeMillis()

        detector.process(src).let { result ->

            //Log.d(CameraFragment.TAG, "CoinAnalyzer: ${System.currentTimeMillis() - startTime}")

            listener.onDetectorCompleted(result, imported)
        }
    }
}