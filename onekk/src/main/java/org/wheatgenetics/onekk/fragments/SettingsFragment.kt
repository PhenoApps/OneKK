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
import androidx.preference.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.interfaces.DeviceDiscoveredListener
import org.wheatgenetics.onekk.observeOnce
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

        val country = mPreferences.getString(getString(R.string.onekk_country_pref_key), "USA") ?: "USA"
        countryPreference?.summary = country

        val coin = mPreferences.getString(getString(R.string.onekk_coin_pref_key), "1 Cent") ?: "1 Cent"
        namePreference?.summary = coin

        updateCoinList(country)

//        TODO: need a mapping between our locally defined country names and Android's configuration locales
//        try {
//            if (!resources.configuration.locales.isEmpty) {
//                updateCoinList(resources.configuration.locales[0].country)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        viewModel.countries().observeOnce(viewLifecycleOwner, {

            countryPreference?.entries = it.toTypedArray()
            countryPreference?.entryValues = it.toTypedArray()

            countryPreference?.setOnPreferenceChangeListener { _, newValue ->

                val countryName = (newValue as? String) ?: "USA"

                countryPreference.summary = countryName

                updateCoinList(countryName)

                mPreferences.edit().putString(getString(R.string.onekk_country_pref_key), countryName).apply()

                true

            }
        })

        findPreference<Preference>("org.wheatgenetics.onekk.ASK_CONNECT")!!
                .setOnPreferenceChangeListener { _, newValue ->
                    mPreferences.edit().apply {
                        putBoolean("org.wheatgenetics.onekk.ASK_CONNECT", (newValue as? Boolean) ?: true)
                    }.apply()

                    true
                }

        findPreference<Preference>("org.wheatgenetics.onekk.DISPLAY_ANALYSIS")!!
                .setOnPreferenceChangeListener { _, newValue ->
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

        with(findPreference<Preference>(getString(R.string.onekk_preference_collector_key))!!) {
            setOnPreferenceChangeListener { preference, newValue ->
                mPreferences.edit().apply {
                    putString(getString(R.string.onekk_preference_collector_key), (newValue as? String) ?: "1")
                }.apply()

                preference.summary = newValue.toString()

                true
            }

            this.summary = mPreferences.getString(getString(R.string.onekk_preference_collector_key), "")
        }

        with(findPreference<Preference>(getString(R.string.onekk_preference_mode_key))!!) {
            setOnPreferenceChangeListener { preference, newValue ->

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

            this.summary = when(mPreferences.getString(getString(R.string.onekk_preference_mode_key),
                getString(R.string.frag_setting_scale_mode_1))) {
                "2" -> getString(R.string.frag_setting_scale_mode_2)
                "3" -> getString(R.string.frag_setting_scale_mode_3)
                else -> getString(R.string.frag_setting_scale_mode_1)
            }
        }

        with(findPreference<Preference>(getString(R.string.onekk_preference_algorithm_mode_key))) {
            this?.setOnPreferenceChangeListener { preference, newValue ->
                mPreferences.edit().apply {
                    putString(getString(R.string.onekk_preference_algorithm_mode_key), (newValue as? String) ?: "0")
                }.apply()

                if (newValue.toString() == "1") {
                    val measurePref = findPreference<ListPreference>("org.wheatgenetics.onekk.MEASURE_TYPE")

                    if (mPreferences.getString("org.wheatgenetics.onekk.MEASURE_TYPE", "0") != "2") {
                        Dialogs.onOk(AlertDialog.Builder(context),
                            getString(R.string.dialog_preference_update_measurement),
                            getString(android.R.string.cancel),
                            getString(android.R.string.ok),
                            getString(R.string.dialog_preference_update_measurement_message)) {

                            if (it) {

                                measurePref?.setValueIndex(2)

                            }
                        }
                    }
                }

                preference.summary = when (newValue.toString()) {
                    "1" -> getString(R.string.large_single_sample_algorithm)
                    else -> getString(R.string.default_algorithm)
                }
                true
            }
            this?.summary = when(mPreferences.getString(getString(R.string.onekk_preference_algorithm_mode_key), "0") ?: "0") {
                "1" -> getString(R.string.large_single_sample_algorithm)
                else -> getString(R.string.default_algorithm)
            }
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

        updateReferenceTypeVis()

        val refTypePref = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_TYPE")

        refTypePref?.setOnPreferenceChangeListener { preference, newValue ->

            mPreferences.edit().putInt("org.wheatgenetics.onekk.REFERENCE_TYPE",
                ((newValue as? String) ?: "1").toInt()).apply()

            updateReferenceTypeVis()

            true
        }

        val refManualPref = findPreference<EditTextPreference>("org.wheatgenetics.onekk.REFERENCE_MANUAL")
        refManualPref?.setOnPreferenceChangeListener { preference, newValue ->

            val diameter = newValue as String
            mPreferences.edit().putString("org.wheatgenetics.onekk.REFERENCE_MANUAL_DIAMETER", diameter).apply()

            true

        }

        val measurePref = findPreference<ListPreference>("org.wheatgenetics.onekk.MEASURE_TYPE")
        measurePref?.setOnPreferenceChangeListener { preference, newValue ->

            mPreferences.edit().putString("org.wheatgenetics.onekk.MEASURE_TYPE", newValue as String).apply()

            true

        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun updateReferenceTypeVis() {
        val refType = mPreferences.getInt("org.wheatgenetics.onekk.REFERENCE_TYPE", 1)
        val manualPref = findPreference<EditTextPreference>("org.wheatgenetics.onekk.REFERENCE_MANUAL")
        val countryPref = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_COUNTRY")
        val coinPref = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_NAME")
        val managerPref = findPreference<Preference>("org.wheatgenetics.onekk.COIN_MANAGER_NAVIGATE")

        if (refType == 1) {

            countryPref?.isVisible = true
            coinPref?.isVisible = true
            managerPref?.isVisible = true
            manualPref?.isVisible = false

        } else {

            countryPref?.isVisible = false
            coinPref?.isVisible = false
            managerPref?.isVisible = false
            manualPref?.isVisible = true
        }
    }

    private fun updateCoinList(name: String) {

        val namePreference = findPreference<ListPreference>("org.wheatgenetics.onekk.REFERENCE_NAME")

        viewModel.coinModels(name).observe(viewLifecycleOwner, { coins ->

            val names = coins.map { it.name }
            namePreference?.entries = names.toTypedArray()
            namePreference?.entryValues = names.toTypedArray()

            namePreference?.setOnPreferenceChangeListener { _, newValue ->

                val coinName = (newValue as? String) ?: "1 Cent"

                val coinDiameter = coins.find { it.name == coinName }?.diameter ?: "19.05"

                namePreference.summary = "$coinName $coinDiameter"

                mPreferences.edit().putString(getString(R.string.onekk_coin_pref_key), coinName).apply()
                mPreferences.edit().putString(getString(R.string.onekk_coin_pref_diameter_key), coinDiameter).apply()

                true

            }
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        mDeviceFinder.observeBleDevices(this)

    }

    override fun onResume() {
        super.onResume()

        updateReferenceTypeVis()
    }

    //private fun List<String>.toEntryValues() = indices.toList().map { it.toString() }.toTypedArray()
}
