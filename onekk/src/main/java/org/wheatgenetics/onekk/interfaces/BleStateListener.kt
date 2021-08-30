package org.wheatgenetics.onekk.interfaces

import com.polidea.rxandroidble2.RxBleClient

interface BleStateListener {
    fun onStateChanged(state: RxBleClient.State)
}