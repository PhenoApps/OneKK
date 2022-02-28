package org.wheatgenetics.onekk.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import org.wheatgenetics.onekk.R

class ExportDialog(private val activity: Activity,
                   private val onPositiveClick: (ExportChoice) -> Unit) : AlertDialog(activity, R.style.Dialog) {

    data class ExportChoice(val seeds: Boolean, val samples: Boolean, val analyzed: Boolean, val captures: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.dialog_ask_export)

        setCanceledOnTouchOutside(true)

        setTitle(R.string.dialog_export_title)

        val cancelButton = findViewById<Button>(R.id.dialog_export_negative_button)
        val positiveButton = findViewById<Button>(R.id.dialog_export_positive_button)
        val seedsCb = findViewById<CheckBox>(R.id.dialog_export_seeds_cb)
        val samplesCb = findViewById<CheckBox>(R.id.dialog_export_samples_cb)
        val capturesCb = findViewById<CheckBox>(R.id.dialog_export_bundle_captures_cb)
        val analyzedCb = findViewById<CheckBox>(R.id.dialog_export_bundle_analyzed_cb)

        cancelButton.setOnClickListener {
            dismiss()
        }

        positiveButton.setOnClickListener {

            onPositiveClick(ExportChoice(
                seedsCb.isChecked,
                samplesCb.isChecked,
                analyzedCb.isChecked,
                capturesCb.isChecked))

            dismiss()
        }
    }
}