package org.wheatgenetics.onekk.analyzers

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * A blank analyzer that does nothing.
 * Applications switch to this analyzer when others are paused.
 */
class NoopAnalyzer() : ImageAnalysis.Analyzer {

    override fun analyze(proxy: ImageProxy) {

        proxy.close()

    }
}