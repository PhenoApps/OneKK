package org.wheatgenetics.onekk.interfaces

import android.bluetooth.BluetoothDevice

interface BleNotificationListener {
    fun onNotification(bytes: ByteArray)
}