package org.wheatgenetics.onekk.fragments

import android.content.Context.MODE_PRIVATE
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.android.synthetic.main.fragment_contour_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentScaleBinding
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.observeOnce
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.Dialogs
import kotlin.properties.Delegates

//TODO rework scale message queue / handler
/**
 * This fragment was developed under the guidance of the following reference:
 * https://developer.android.com/guide/topics/connectivity/bluetooth-le
 * and http://polidea.github.io/RxAndroidBle/
 * The scale fragment's purpose is to interface with Ohaus scales to weigh seed samples
 *  that have been counted in OneKK.
 */
class ScaleFragment : Fragment(), CoroutineScope by MainScope(), BleNotificationListener {

    private val viewModel by viewModels<ExperimentViewModel> {
        with(OnekkDatabase.getInstance(requireContext())) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    private var aid by Delegates.notNull<Int>()

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), MODE_PRIVATE)
    }

    private val mBluetoothManager by lazy {
        BluetoothUtil(requireContext())
    }

    companion object {

        const val OHAUS_BLUETOOTH_GATT_SERVICE_UUID = "2456e1b9-26e2-8f83-e744-f34f01e9d701"

        //val OHAUS_BLUETOOTH_GATT_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

        //val OHAUS_BLUETOOTH_GATT_CHARACTERISTIC_UUID = "2456e1b9-26e2-8f83-e744-f34f01e9d703"

        //val OHAUS_BLUETOOTH_GATT_CHARACTERISTIC_UUID_B = "2456e1b9-26e2-8f83-e744-f34f01e9d704"
        
        const val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss.SSS"
    }

    private var mBinding: FragmentScaleBinding? = null


    /**
     * The interface implementation which is sent to setupDeviceComms
     * This will read any notification that is received from the device.
     */
    override fun onNotification(bytes: ByteArray) {

        val stringResult = ValueInterpreter.getStringValue(bytes, 0)

        if (stringResult.isNotBlank()) {

            scaleTextUpdateUi(stringResult)

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aid = requireArguments().getInt("analysis", -1)

        //if a device is already chosen, then start the service search/connection
        if (mPreferences.getString(getString(R.string.preferences_enable_bluetooth_key), String())?.isEmpty() == true) {

            //if a device is not already chosen and its the first time loading this fragment, ask if the user wants to connect to a device
            when (mPreferences.getBoolean(getString(R.string.onekk_first_scale_fragment_ask_mac_address), true)) {

                true -> {

                    callAskConnectDialog()

                    //set first time dialog to false so it is only ever asked once
                    mPreferences.edit().putBoolean(getString(R.string.onekk_first_scale_fragment_ask_mac_address), false).apply()

                }
                else ->  {

                    //finally, if it's not the first time, check the ask connect preference to ask anyways
                    if (mPreferences.getBoolean("org.wheatgenetics.onekk.ASK_CONNECT", true)) {

                        callAskConnectDialog()

                    }
                }
            }
        } else {

            //if a device is already preferred, try connecting to it
            startMacAddressSearch()

        }
    }

    //when the view is loaded, update the edit text with the saved weight
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAnalysis(aid).observeOnce(viewLifecycleOwner, { analysis ->
            activity?.runOnUiThread {
                analysis?.weight?.let {
                    mBinding?.scaleFragmentEditText?.setText(it.toString())
                }
            }
        })
    }

    private fun callAskConnectDialog() {

        Dialogs.onOk(AlertDialog.Builder(requireContext()),
                getString(R.string.camera_fragment_dialog_first_load_ask_address),
                getString(R.string.cancel),
                getString(R.string.camera_fragment_dialog_first_load_ok)) { theyWantToSetAddress ->

            if (theyWantToSetAddress) {

                startMacAddressSearch()

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_scale, container, false)

        with(mBinding) {

            this?.scaleCaptureButton?.setOnClickListener {

                val weight = this.scaleFragmentEditText.text?.toString()?.toDoubleOrNull()

                launch {

                    viewModel.updateAnalysisWeight(aid, weight)

                    //When the save button is pressed, go to the analysis fragment or the camera fragment
                    activity?.runOnUiThread {

                        with (findNavController()) {
                            if (previousBackStackEntry?.destination?.id == R.id.analysis_fragment) {
                                popBackStack()
                            } else navigate(ScaleFragmentDirections.actionToCamera())
                        }
                    }
                }
            }

            viewModel.getSourceImage(aid).observeForever { url ->

                imageView?.setImageBitmap(BitmapFactory.decodeFile(url))

            }

            //custom support action toolbar
            with(mBinding?.toolbar) {
                this?.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
                    findNavController().popBackStack()
                }
                this?.findViewById<ImageButton>(R.id.connectButton)?.setOnClickListener {
                    startMacAddressSearch()
                }
            }
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun startMacAddressSearch() {

        val macAddress = mPreferences.getString(getString(R.string.preferences_enable_bluetooth_key), null)

        if (macAddress != null) {

            mBluetoothManager.establishConnectionToAddress(this, macAddress)

        } else {

            //TODO: Instead of moving to Settings, the service can be automatically found (if it's available)
            Toast.makeText(requireContext(), getString(R.string.frag_scale_no_mac_address_found_message), Toast.LENGTH_LONG).show()

//            findNavController().navigate(ScaleFragmentDirections.actionToSettings())

        }
    }

    /**
     * Disposables are destroyed so BLE connections are lost when the app is sent to background.
     */
    override fun onPause() {
        super.onPause()

        mBluetoothManager.dispose()

    }

    /**
     * Function that updates the scale measurement UI which can be called from other threads.
     */
    //TODO: use ohaus commands to format the output text to not include newlines.
    private fun scaleTextUpdateUi(value: String) {

        activity?.let {

            it.runOnUiThread {

                it.findViewById<EditText>(R.id.scaleFragmentEditText)?.setText(formatWeightText(value).toString())

            }
        }
    }

    private fun formatWeightText(text: String): Double = text.replace("\n", "")
            .split("g")[0]
            .replace(" ", "").toDoubleOrNull() ?: 0.0

}