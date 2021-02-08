package org.wheatgenetics.onekk.interfaces

import org.wheatgenetics.imageprocess.DetectWithReferences
import java.io.File

interface DetectorListener {
    fun onDetectorCompleted(result: DetectorAlgorithm.Result, imported: File? = null)
}