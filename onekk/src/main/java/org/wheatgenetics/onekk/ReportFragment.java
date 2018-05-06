package org.wheatgenetics.onekk;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import org.wheatgenetics.database.CoinRecord;
import org.wheatgenetics.database.MySQLiteHelper;

import java.util.ArrayList;
import java.util.List;

/************************************************************************************
 * this class initiates all the settings fragments and adds any additional preferences
 ************************************************************************************/

public class ReportFragment extends PreferenceFragment{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            addPreferencesFromResource(R.xml.report_preferences);
        }
        catch (Exception ex) {
            //Log.e("Settings Fragment","Preferences setup failed");
        }
    }
}