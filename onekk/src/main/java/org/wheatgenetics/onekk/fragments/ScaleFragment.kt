package org.wheatgenetics.onekk.fragments

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentScaleBinding
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.utils.BluetoothUtil
import java.util.*


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

    private val mScaleQueue = ArrayList<String>()

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), MODE_PRIVATE)
    }

    private val mBluetoothManager by lazy {
        BluetoothUtil(requireContext())
    }

    companion object {

        val OHAUS_BLUETOOTH_GATT_SERVICE_UUID = "2456e1b9-26e2-8f83-e744-f34f01e9d701"

        val OHAUS_BLUETOOTH_GATT_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

        val OHAUS_BLUETOOTH_GATT_CHARACTERISTIC_UUID = "2456e1b9-26e2-8f83-e744-f34f01e9d703"

        val OHAUS_BLUETOOTH_GATT_CHARACTERISTIC_UUID_B = "2456e1b9-26e2-8f83-e744-f34f01e9d704"
        
        final val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss.SSS"
    }

    //global references to the menu, allows the swapping of icons when connecting/disconnecting from bt
    private var mMenu: Menu? = null

    private var mBinding: FragmentScaleBinding? = null

    /**
     * The interface implementation which is sent to setupDeviceComms
     * This will read any notification that is received from the device.
     */
    override fun onNotification(bytes: ByteArray) {

        startScaleTextUpdaterUi()

        val stringResult = ValueInterpreter.getStringValue(bytes, 0)

        if (stringResult.isNotBlank()) {

            mScaleQueue.add(stringResult)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_scale, container, false)

        with(mBinding) {

//            viewModel.analysis(ExperimentEntity(Experiment("Test"), 1))
//                    .observe(viewLifecycleOwner, {
//
//                        mBinding?.imageView?.setImageBitmap(it.first())
//
//                    })

        }

        val macAddress = mPreferences.getString(getString(R.string.preferences_enable_bluetooth_key), null)

        if (macAddress != null) {

            BluetoothUtil(requireContext()).establishConnectionToAddress(this, macAddress)

        } else {

            //TODO: Instead of moving to Settings, the service can be automatically found (if it's available)
            Toast.makeText(requireContext(), getString(R.string.frag_scale_no_mac_address_found_message), Toast.LENGTH_LONG).show()

            findNavController().navigate(ScaleFragmentDirections.actionToSettings())

        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.scale_toolbar, menu)

        mMenu = menu

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        with(mBinding) {

            when(item.itemId) {

                R.id.action_print -> {

                    Log.d(TAG, "BluetoothLe scanning initiated.")

                }
                else -> null
            }
        }

        return super.onOptionsItemSelected(item)
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
     *
     *
     */
    //TODO: use ohaus commands to format the output text to not include newlines.
    private fun scaleTextUpdateUi(value: String) = with(requireActivity()) {

        runOnUiThread {
            findViewById<TextView>(R.id.scaleEditText)
                    .text = value.replace("\n", "")
        }
    }


    /**
     * Function that schedules the scale message queue to be consumed and displayed to the UI.
     */
    private fun startScaleTextUpdaterUi() {

        Timer().scheduleAtFixedRate(object : TimerTask() {

            override fun run() {

                if (mScaleQueue.isNotEmpty()) {

                    val nextWeightValue = mScaleQueue.removeAt(0)

                    scaleTextUpdateUi(nextWeightValue)
                }

            }

        }, 0L, 500L)
    }
}