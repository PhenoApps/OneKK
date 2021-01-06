package org.wheatgenetics.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.ParcelUuid
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.wheatgenetics.onekk.fragments.ScaleFragment
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.BleStateListener
import org.wheatgenetics.onekk.interfaces.DeviceDiscoveredListener
import java.util.*

/**
 * A class for managing bluetooth connections/communications with Ohaus BLE scales.
 */
class BluetoothUtil(private val context: Context) {

    private val client by lazy { RxBleClient.create(context) }

    private var notificationDisposable: Disposable? = null

    private var scanDisposable: Disposable? = null

    private var stateDisposable: Disposable? = null

    private var writeDisposable: Disposable? = null

    fun deviceStateListener(listener: BleStateListener) {

        try {
            stateDisposable?.dispose()

            stateDisposable = client.observeStateChanges()
                    .doOnNext { }
                    .doOnError { }
                    .subscribe({
                        listener.onStateChanged(it)
                    }) { error -> error.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Uses a device with an established connection to setup GATT notifications for OHAUS devices.
     * Currently, only the known service weight ID (given in OHAUS documentation) is used.
     * TODO: we could make this more generic by using the GATT/BLE spec's standardized UUID for weight scales
     *
     * This function begins notifications from the given characteristic. This will send messages back from
     * the OHAUS device whenever the weight measurement changes. Measurements are saved to a queue.
     * Finally, a write characteristic happens to send an OHAUS command to the scale.
     */
    private fun setupDeviceComms(listener: BleNotificationListener, device: RxBleDevice?, connection: RxBleConnection) {

        println("Connection established with ${device!!.name} @ ${device!!.macAddress}")

        //ohaus defined GATT characteristic id that has the notify property
        val uuid = UUID.fromString("2456e1b9-26e2-8f83-e744-f34f01e9d703")

        try {
            //destroy any previous disposable before recreating
            notificationDisposable?.dispose()

            notificationDisposable = connection.setupNotification(uuid)
                    .doOnNext { _ ->
                        //println("Notifications setup.")
                    }
                    .doOnError {  }
                    .flatMap { x -> x }
                    .subscribeOn(Schedulers.newThread())
                    .doOnError {

                    }
                    .subscribe({ bytes ->

                        listener.onNotification(bytes)

                    })
                    { error -> error.printStackTrace() }

            //ohaus commands https://www.novatech-usa.com/pdf/Ohaus%2030296497%20Bluetooth%20Interface%20Manual.pdf


            //writes a single characteristic, in this case CP which is continuous print
            writeDisposable?.dispose()

            writeDisposable = connection.writeCharacteristic(uuid, "CP\r\n".toByteArray())
                    .doOnError { }
                    .subscribe({ success ->

                        //    println("$success")
                    }) { error -> error.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Takes a macAddress and uses RxBleClient to establish a connection with the device.
     * Uses a scan filter that requires the macAddress and that the OHAUS service is available.
     * Calls setupDeviceComms when a connection is established.
     */
    fun establishConnectionToAddress(listener: BleNotificationListener, address: String) {

        var device: RxBleDevice? = null

        try {
            scanDisposable?.dispose()

            scanDisposable = client.scanBleDevices(ScanSettings.Builder().build(),
                    ScanFilter.Builder()
                            .setDeviceAddress(address)
                            .setServiceUuid(ParcelUuid.fromString(ScaleFragment.OHAUS_BLUETOOTH_GATT_SERVICE_UUID)).build())
                    .doOnError {  }
                    .switchMap { scan ->

                        device = scan?.bleDevice

                        scan?.bleDevice?.establishConnection(false)

                    }
                    .doOnError { }
                    .subscribe({ success ->

                        setupDeviceComms(listener, device, success)

                    }) { error -> error.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun observeBleDevices(listener: DeviceDiscoveredListener) {

        try {

            stateDisposable?.dispose()

            stateDisposable = client.scanBleDevices(ScanSettings.Builder().build(), ScanFilter.empty())
                    .doOnError {  }
                    .subscribe({
                        listener.onDiscovered(it.bleDevice.bluetoothDevice)
                    }) { error -> error.printStackTrace() }
        } catch (ble: BleScanException) {
            ble.printStackTrace()
        }
    }

    fun dispose() {

        scanDisposable?.dispose()

        notificationDisposable?.dispose()

        stateDisposable?.dispose()

        writeDisposable?.dispose()

    }
}