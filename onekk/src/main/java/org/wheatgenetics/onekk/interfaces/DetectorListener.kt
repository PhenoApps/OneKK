package org.wheatgenetics.onekk.interfaces

import java.io.File

interface DetectorListener {
    fun onDetectorCompleted(result: DetectorAlgorithm.Result, imported: File? = null)
}