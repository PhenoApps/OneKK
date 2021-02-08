package org.wheatgenetics.onekk.fragments

import android.bluetooth.BluetoothDevice
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.interfaces.DeviceDiscoveredListener
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.Dialogs

class SettingsFragment : CoroutineScope by MainScope(), PreferenceFragmentCompat(), DeviceDiscoveredListener {

    private val db by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
    }

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
//            findPreference<Preference>(getString(R.string.preferences_enable_bluetooth_key))
//                    ?.summary = "${mDevices.size} ${getString(R.string.devices)}"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val countryPreference = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_COUNTRY")
        val namePreference = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_NAME")

        val country = mPreferences.getString(getString(R.string.onekk_country_pref_key), "USA")
        countryPreference?.summary = country

        val coin = mPreferences.getString(getString(R.string.onekk_coin_pref_key), "1 Cent")
        namePreference?.summary = coin

        updateCoinList(country!!)

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                if (!resources.configuration.locales.isEmpty) {
                    updateCoinList(resources.configuration.locales[0].country)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        launch {

            viewModel.countries().observeForever {

                countryPreference?.entries = it.toTypedArray()
                countryPreference?.entryValues = it.toTypedArray()

                countryPreference?.setOnPreferenceChangeListener { preference, newValue ->

                    val countryName = (newValue as? String) ?: "USA"

                    countryPreference.summary = countryName

                    updateCoinList(countryName)

                    mPreferences.edit().putString(getString(R.string.onekk_country_pref_key), countryName).apply()

                    true

                }
            }
        }

        findPreference<Preference>("org.wheatgenetics.onekk.ASK_CONNECT")!!
                .setOnPreferenceChangeListener { preference, newValue ->
                    mPreferences.edit().apply {
                        putBoolean("org.wheatgenetics.onekk.ASK_CONNECT", (newValue as? Boolean) ?: true)
                    }.apply()

                    true
                }

        findPreference<Preference>("org.wheatgenetics.onekk.DISPLAY_ANALYSIS")!!
                .setOnPreferenceChangeListener { preference, newValue ->
                    mPreferences.edit().apply {
                        putBoolean("org.wheatgenetics.onekk.DISPLAY_ANALYSIS", (newValue as? Boolean) ?: true)
                    }.apply()

                    true
                }

        findPreference<Preference>(getString(R.string.onekk_coin_manager_key))!!
                .setOnPreferenceClickListener {

                    findNavController().navigate(SettingsFragmentDirections.actionToCoinManager())

                    true
                }

        findPreference<Preference>(getString(R.string.onekk_preference_collector_key))!!
                .setOnPreferenceChangeListener { preference, newValue ->
                    mPreferences.edit().apply {
                        putString(getString(R.string.onekk_preference_collector_key), (newValue as? String) ?: "1")
                    }.apply()

                    true
                }

        findPreference<Preference>(getString(R.string.onekk_preference_mode_key))!!
                .setOnPreferenceChangeListener { preference, newValue ->

                    preference.summary = when(newValue.toString()) {
                        "1" -> getString(R.string.frag_setting_scale_mode_1)
                        "2" -> getString(R.string.frag_setting_scale_mode_2)
                        else -> getString(R.string.frag_setting_scale_mode_3)

                    }

                    mPreferences.edit().apply {
                        putString(getString(R.string.onekk_preference_mode_key), (newValue as? String) ?: "1")
                    }.apply()

                    true
                }

        findPreference<Preference>(getString(R.string.onekk_preference_algorithm_mode_key))!!
                .setOnPreferenceChangeListener { preference, newValue ->
                    mPreferences.edit().apply {
                        putString(getString(R.string.onekk_preference_algorithm_mode_key), (newValue as? String) ?: "0")
                    }.apply()

                    true
                }

        //crash if this is called before this preference is created
        findPreference<Preference>(getString(R.string.preferences_enable_bluetooth_key)).apply {

            this?.summary = mPreferences.getString(getString(R.string.preferences_saved_device_name_key), getString(R.string.preferences_device_searching))

            this?.setOnPreferenceClickListener {

                if (mDevices.isNotEmpty()) {
                    Dialogs.chooseBleDevice(AlertDialog.Builder(requireContext()),
                            getString(R.string.dialog_choose_ble_device_title),
                            mDevices.toTypedArray()) { device ->

                        it.summary = device?.name ?: "None"

                        mPreferences.edit().apply {

                            putString(getString(R.string.preferences_enable_bluetooth_key), device?.address ?: String())

                            putString(getString(R.string.preferences_saved_device_name_key), device?.name ?: String())

                        }.apply()
                    }
                } else {

                    it.summary = getString(R.string.frag_settings_no_devices_found)
                }

                true
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun updateCoinList(name: String) {

        val namePreference = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_NAME")

        viewModel.coinModels(name).observeForever { coins ->

            val names = coins.map { it.name }
            namePreference?.entries = names.toTypedArray()
            namePreference?.entryValues = names.toTypedArray()

            namePreference?.setOnPreferenceChangeListener { preference, newValue ->

                val coinName = (newValue as? String) ?: "25 cents"

                namePreference.summary = coinName

                val coinDiameter = coins.find { it.name == coinName }?.diameter ?: "24.26"

                mPreferences.edit().putString(getString(R.string.onekk_coin_pref_key), coinDiameter).apply()

                true

            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        mDeviceFinder.observeBleDevices(this)

    }

    private fun List<String>.toEntryValues() = indices.toList().map { it.toString() }.toTypedArray()
}
