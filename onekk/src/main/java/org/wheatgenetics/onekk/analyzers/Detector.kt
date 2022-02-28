package org.wheatgenetics.onekk.analyzers

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.imageprocess.DetectWithReference
import org.wheatgenetics.imageprocess.PotatoDetector
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import java.io.File

class Detector(context: Context, referenceDiameter: Double, algorithm: String? = null, measure: String): CoroutineScope by MainScope() {

//    private var frames = 0.0
//    private var startAnalysisTime = System.currentTimeMillis()

    val detector: DetectorAlgorithm = when (algorithm?.toIntOrNull() ?: 0) {

        DetectorAlgorithm.LSS -> PotatoDetector(context, referenceDiameter, measure)

        else -> DetectWithReference(context, referenceDiameter, measure)
    }

    fun scan(src: Bitmap): DetectorAlgorithm.Result? {

//        val startTime = System.currentTimeMillis()

        detector.process(src).let { result ->

//            println("${System.currentTimeMillis() - startTime}")

            //listener.onDetectorCompleted(result, imported)

            return result
        }
    }
}