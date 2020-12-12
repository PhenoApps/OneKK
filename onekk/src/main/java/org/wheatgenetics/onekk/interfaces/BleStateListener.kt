package org.wheatgenetics.onekk.interfaces

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble2.RxBleClient

interface BleStateListener {
    fun onStateChanged(state: RxBleClient.State)
}