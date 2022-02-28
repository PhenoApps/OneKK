package org.wheatgenetics.onekk.activities

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import org.wheatgenetics.onekk.R

open class RequireCollectorActivity: AppCompatActivity() {

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    /**
     * Simple function that checks if the collect activity was opened >24hrs ago.
     * If the condition is met, it asks the user to reenter the collector id.
     */
    private fun checkLastOpened() {
        val lastOpen: Long = mPrefs.getLong("LastTimeAppOpened", 0L)
        val systemTime = System.nanoTime()
        val nanosInOneDay = 1e9.toLong() * 3600 * 24
        if (lastOpen != 0L && systemTime - lastOpen > nanosInOneDay) {
            val verify: Boolean = mPrefs.getBoolean("VerifyUserEvery24Hours", true)
            if (verify) {
                val collector = mPrefs.getString("org.wheatgenetics.onekk.COLLECTOR", "") ?: ""
                if (collector.isNotEmpty()) {
                    //person presumably has been set
                    showAskCollectorDialog(
                        "${getString(R.string.activity_dialog_verify_collector, collector)}?",
                        getString(R.string.activity_dialog_verify_yes_button),
                        getString(R.string.activity_dialog_neutral_button),
                        getString(R.string.activity_dialog_verify_no_button)
                    )
                } else {
                    //person presumably hasn't been set
                    showAskCollectorDialog(
                        getString(R.string.activity_dialog_new_collector),
                        getString(R.string.activity_dialog_verify_no_button),
                        getString(R.string.activity_dialog_neutral_button),
                        getString(R.string.activity_dialog_verify_yes_button)
                    )
                }
            }
        }
        updateLastOpenedTime()
    }

    private fun updateLastOpenedTime() {
        mPrefs.edit().putLong("LastTimeAppOpened", System.nanoTime()).apply()
    }

    private fun showAskCollectorDialog(
        message: String,
        positive: String,
        neutral: String,
        negative: String
    ) {
        AlertDialog.Builder(this)
            .setTitle(message) //yes button
            .setPositiveButton(
                positive
            ) { dialog: DialogInterface, which: Int -> dialog.dismiss() } //yes, don't ask again button
            .setNeutralButton(neutral) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                mPrefs.edit().putBoolean("VerifyUserEvery24Hours", false).apply()
            } //no (navigates to the person preference)
            .setNegativeButton(negative) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
//                val controller = Navigation.findNavController(this, R.id.nav_fragment)
//                controller.navigate(R.id.settings_fragment)
                (this as MainActivity).getBottomBar().selectedItemId = R.id.action_nav_settings

            }
            .show()
    }

    override fun onPause() {
        super.onPause()

        updateLastOpenedTime()
    }

    override fun onResume() {
        super.onResume()

        checkLastOpened()
    }
}