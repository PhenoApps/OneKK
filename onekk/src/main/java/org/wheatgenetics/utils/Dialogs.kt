package org.wheatgenetics.utils

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import org.wheatgenetics.imageprocess.DetectWithReferences
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.DialogCoinRecognitionBinding

class Dialogs {

    companion object {

        fun chooseBleDevice(builder: AlertDialog.Builder, title: String, devices: Array<BluetoothDevice>, function: (BluetoothDevice?) -> Unit) {

            if (devices.isNotEmpty()) {

                builder.setTitle(title)

                //bluetooth devices might have a null name
                builder.setSingleChoiceItems(devices.map { it.name }.toTypedArray() + "None", -1) { dialog, choice ->

                    if (choice > -1 && choice < devices.size) {

                        function(devices[choice])

                    } else if (choice >= devices.size) {

                        function(null)
                    }

                    dialog.dismiss()

                }

                builder.create()

                builder.show()
            }

        }

        /***
         * Generic dialog to run a function if the OK/Neutral button are pressed.
         * If the ok button is pressed the boolean parameter to the function is set to true, false otherwise.
         */
        fun booleanOption(builder: AlertDialog.Builder, title: String,
                          positiveText: String, negativeText: String,
                          neutralText: String, function: (Boolean) -> Unit) {

            builder.apply {

                setTitle(title)

                setPositiveButton(positiveText) { _, _ ->

                    function(true)

                }

                setNeutralButton(neutralText) { _, _ ->
                    
                }

                setNegativeButton(negativeText) { _, _ ->

                    function(false)
                }

                show()
            }
        }

        fun onOk(builder: AlertDialog.Builder, title: String, cancel: String, ok: String, function: (Boolean) -> Unit) {

            builder.apply {

                setCancelable(false)

                setNegativeButton(cancel) { _, _ ->

                    function(false)

                }

                setPositiveButton(ok) { _, _ ->

                    function(true)

                }

                setTitle(title)

                create()

                show()
            }
        }

        fun askAcceptableImage(activity: Activity, builder: AlertDialog.Builder, title: String,
                               dst: Bitmap, function: (() -> Unit)? = null, onDecline: (() -> Unit)? = null) {

            val binding = DataBindingUtil.inflate<DialogCoinRecognitionBinding>(activity.layoutInflater, R.layout.dialog_coin_recognition, null, false)

            binding.visible = true

            binding.resultView.rotation = 90f

            binding.resultView.setImageBitmap(dst)

            builder.setTitle(title)

            builder.setPositiveButton(R.string.frag_camera_dialog_post_analysis_ok) { dialog, which ->

                if (function != null) {
                    function()
                }

                dialog.dismiss()

            }

            builder.setNegativeButton(R.string.frag_camera_dialog_post_analysis_details) { dialog, which ->

                if (onDecline != null) {
                    onDecline()
                }

                dialog.dismiss()

            }

            builder.setView(binding.root)

            builder.setCancelable(false)

            builder.show()

        }
    }
}