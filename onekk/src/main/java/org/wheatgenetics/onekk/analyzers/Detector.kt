package org.wheatgenetics.onekk.analyzers

import android.graphics.Bitmap
import androidx.work.Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.imageprocess.DetectWithReferences
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.onekk.interfaces.DetectorListener
import java.io.File

class Detector(referenceDiameter: Double, private val imported: File? = null, private val algorithm: String? = null): CoroutineScope by MainScope() {

//    private var frames = 0.0
//    private var startAnalysisTime = System.currentTimeMillis()

    val detector: DetectorAlgorithm = DetectWithReferences(referenceDiameter)

    fun scan(src: Bitmap): DetectWithReferences.Result? {

//        val startTime = System.currentTimeMillis()

        detector.process(src).let { result ->

//            println("${System.currentTimeMillis() - startTime}")

            //listener.onDetectorCompleted(result, imported)

            return result
        }
    }
}