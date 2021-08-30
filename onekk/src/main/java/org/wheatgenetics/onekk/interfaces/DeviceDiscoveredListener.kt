package org.wheatgenetics.onekk.interfaces

import android.bluetooth.BluetoothDevice

interface DeviceDiscoveredListener {
    fun onDiscovered(device: BluetoothDevice)
}