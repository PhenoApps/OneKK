package org.wheatgenetics.onekk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

const val DB_NAME = "OneKK.db"

fun ImageProxy.toBitmap(): Bitmap {

    val buffer: ByteBuffer = this.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())

    buffer.get(bytes)

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Bitmap.toFile(parent: String, name: String) = this.let { image ->

    val file = File(parent, name)

    FileOutputStream(file).use { stream ->

        image.compress(Bitmap.CompressFormat.JPEG, 90, stream)

    }

    file
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

//simple function that takes a string expecting decimal places and shortens to 2 after the decimal.
fun shortenString(long: Double): String {

    val decimalPlaces = 2

    val longNumber = long.toString()

    val last = longNumber.indexOf(".") + decimalPlaces

    return longNumber.padEnd(last).substring(0, last)

}