package org.wheatgenetics.onekk.interfaces

interface BleNotificationListener {
    fun onNotification(bytes: ByteArray)
}