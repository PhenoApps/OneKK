package org.wheatgenetics.onekk.interfaces

import org.wheatgenetics.imageprocess.DetectWithReferences
import java.io.File

interface DetectorListener {
    fun onDetectorCompleted(result: DetectWithReferences.Result, imported: File? = null)
}