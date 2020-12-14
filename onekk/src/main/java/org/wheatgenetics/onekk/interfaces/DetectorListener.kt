package org.wheatgenetics.onekk.interfaces

import org.wheatgenetics.imageprocess.DetectWithReferences

interface DetectorListener {
    fun onDetectorCompleted(result: DetectWithReferences.Result)
}