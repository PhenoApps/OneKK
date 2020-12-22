package org.wheatgenetics.onekk

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

const val DB_NAME = "OneKK.db"

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