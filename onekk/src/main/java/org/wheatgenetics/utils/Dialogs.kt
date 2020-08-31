package org.wheatgenetics.utils

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import org.wheatgenetics.imageprocess.EnhancedWatershed
import org.wheatgenetics.imageprocess.renderscript.ExampleRenderScript
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.DialogCoinRecognitionBinding

class Dialogs {

    companion object {

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

                    function(false)

                }

                setNegativeButton(negativeText) { _, _ ->

                }

                show()
            }
        }

        /**
         * Simple alert dialog to notify the user of a message.
         */
        fun notify(builder: AlertDialog.Builder, title: String) {

            builder.apply {

                setPositiveButton("OK") { _, _ ->

                }
            }

            builder.setTitle(title)
            builder.show()
        }

        /**
         * Simple alert dialog to notify the user of a message.
         */
        fun largeNotify(builder: AlertDialog.Builder, title: String) {

            builder.apply {

                setPositiveButton("OK") { _, _ ->

                }
            }

            builder.setMessage(title)
            builder.show()
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

        fun askForExportType(builder: AlertDialog.Builder, title: String, options: Array<String>, function: (String) -> Unit) {

            builder.setTitle(title)

            builder.setSingleChoiceItems(options, 0) { dialog, choice ->

                function(options[choice])

                dialog.dismiss()

            }

            builder.create()

            builder.show()
        }

        fun askAcceptableCoinRecognition(activity: Activity, builder: AlertDialog.Builder, title: String, srcBitmap: Bitmap?, function: (Bitmap?) -> Unit) {

            val binding = DataBindingUtil.inflate<DialogCoinRecognitionBinding>(activity.layoutInflater, R.layout.dialog_coin_recognition, null, false)

//            val bitmap = activity.contentResolver?.openInputStream(savedUri)?.let { stream ->
//
//                val bitmap: Bitmap? = BitmapFactory.decodeStream(stream)
//
//                stream.close()
//
//                bitmap
//
//            }

            binding.visible = true

            binding.resultView.rotation = 90f

            binding.resultView.setImageBitmap(srcBitmap)

            builder.setTitle(title)

            builder.setPositiveButton(R.string.accept) { dialog, which ->

                srcBitmap?.let { src ->

//                    val detect = EnhancedWatershed(EnhancedWatershed.DetectSeedsParams(5000))
//
//                    val result = detect.process(src)
//
//                    val src = result.first

                    val dst = Bitmap.createBitmap(src.width, src.height, src.config)

//                    ExampleRenderScript().testScript(activity, src)
//
//                    binding.resultView.setImageBitmap(dst)

                    function(dst)

                    dst

                }

                //dialog.dismiss()

            }

            builder.setNegativeButton(R.string.decline) { dialog, which ->

                function(srcBitmap)

                dialog.dismiss()

            }

            builder.setView(binding.root)

            builder.setCancelable(false)

            builder.show()

        }
    }
}