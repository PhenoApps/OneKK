package org.wheatgenetics.onekk.interfaces

interface CoinValueChangedListener {
    fun onCoinValueChanged(country: String, name: String, value: Double)
}