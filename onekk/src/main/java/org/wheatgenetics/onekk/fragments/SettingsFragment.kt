package org.wheatgenetics.onekk.fragments

import android.bluetooth.BluetoothDevice
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.interfaces.DeviceDiscoveredListener
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.Dialogs
import kotlin.math.absoluteValue

class SettingsFragment : PreferenceFragmentCompat(), DeviceDiscoveredListener {

    //global list of devices to populate from bluetooth le search
    //the mac address is saved which is used to make a connection in ScaleFragment
    private val mDevices = ArrayList<BluetoothDevice>()

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), MODE_PRIVATE)
    }

    private val mDeviceFinder by lazy { BluetoothUtil(requireContext()) }

    /**
     * Bluetooth device discovery callback that is updated whenever BluetoothUtil finds a new device.
     */
    override fun onDiscovered(device: BluetoothDevice) {

        if (device.name != null && mDevices.find { it.name == device.name } == null) {

            mDevices.add(device)

            //crash if this is called before this preference is created
            findPreference<Preference>(getString(R.string.preferences_enable_bluetooth_key))
                    ?.summary = "${mDevices.size} ${getString(R.string.devices)}"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        /**
         * Ensure that the diameter preference is positive and can be represented as a Double.
         */
        findPreference<EditTextPreference>("org.wheatgenetics.onekk.REFERENCE_DIAMETER")?.setOnPreferenceChangeListener { preference, newValue ->

            newValue?.toString()?.toDoubleOrNull()?.absoluteValue != null

        }

        //crash if this is called before this preference is created
        findPreference<Preference>(getString(R.string.preferences_enable_bluetooth_key))!!
                .setOnPreferenceClickListener {

                    if (mDevices.isNotEmpty()) {
                        Dialogs.chooseBleDevice(AlertDialog.Builder(requireContext()),
                                getString(R.string.dialog_choose_ble_device_title),
                                mDevices.toTypedArray()) { device ->

                            mPreferences.edit().apply {

                                putString(getString(R.string.preferences_enable_bluetooth_key), device)

                            }.apply()
                        }
                    }

                    true
                }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        mDeviceFinder.observeBleDevices(this)

    }
}
