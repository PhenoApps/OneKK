package org.wheatgenetics.onekk.database.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BarcodeSharedViewModel : ViewModel() {

    val lastScan: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
}