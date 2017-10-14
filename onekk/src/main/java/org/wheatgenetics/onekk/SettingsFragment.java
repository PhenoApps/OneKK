package org.wheatgenetics.onekk;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

/************************************************************************************
 * this class initiates all the settings fragments and adds any additional preferences
 ************************************************************************************/

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static String FIRST_NAME = "org.wheatgenetics.onekk.FIRST_NAME";
    public static String LAST_NAME = "org.wheatgenetics.onekk.LAST_NAME";
    public static String DISPLAY_ANALYSIS = "org.wheatgenetics.onekk.DISPLAY_ANALYSIS";
    public static String DISPLAY_SEED_COUNT = "org.wheatgenetics.onekk.DISPLAY_SEED_COUNT";
    public static String AUTO_CROP = "org.wheatgenetics.onekk.AUTO_CROP";
    public static String MIN_SEED_VALUE = "org.wheatgenetics.onekk.MIN_SEED_VALUE";
    public static String MAX_SEED_VALUE = "org.wheatgenetics.onekk.MAX_SEED_VALUE";
    public static String MIN_HUE_VALUE = "org.wheatgenetics.onekk.MIN_HUE_VALUE";
    public static String MAX_HUE_VALUE = "org.wheatgenetics.onekk.MAX_HUE_VALUE";
    public static String THRESHOLD = "org.wheatgenetics.onekk.THRESHOLD";
    public static String COIN_SIZE = "org.wheatgenetics.onekk.COIN_SIZE";
    public static String ASK_PROCESSING_TECHNIQUE = "org.wheatgenetics.onekk.ASK_PROCESSING_TECHNIQUE";
    public static String PROCESSING_TECHNIQUE = "org.wheatgenetics.onekk.PROCESSING_TECHNIQUE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        //additional setup for MIN/MAX checks in Range settings
        additionalPreferenceSetup(findPreference(MIN_SEED_VALUE), "Minimum seed value");
        additionalPreferenceSetup(findPreference(MAX_SEED_VALUE), "Maximum seed value");
        additionalPreferenceSetup(findPreference(MIN_HUE_VALUE), "Minimum hue value");
        additionalPreferenceSetup(findPreference(MAX_HUE_VALUE), "Maximum hue value");
        additionalPreferenceSetup(findPreference(THRESHOLD), "Threshold");
    }

    //adding additional preferences to MIN/MAX range sliders

    private void additionalPreferenceSetup(Preference preference, String text) {
        bindPreferenceToValue(preference);
        preference.setTitle(text + " - " + preference.getSharedPreferences().getInt(preference.getKey(), 0));
    }

    // Set the listener onPreferenceChange to watch for MIN/MAX value changes.
    private void bindPreferenceToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);

    }

    //returns true if the range seek bar value is within the
    //compared value(MIN<MAX or MAX>MIN), else returns false and restores the original value

    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        int tempVal;
        String text = "";
        if (preference.getKey().equals(MIN_SEED_VALUE)) {
            text = "Minimum seed value";
            tempVal = findPreference(MAX_SEED_VALUE).getSharedPreferences().getInt(MAX_SEED_VALUE, 0);
            if (Integer.parseInt(stringValue) > tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Minimum cannot be greater than Maximum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(MAX_SEED_VALUE)) {
            text = "Maximum seed value";
            tempVal = findPreference(MIN_SEED_VALUE).getSharedPreferences().getInt(MIN_SEED_VALUE, 0);
            if (Integer.parseInt(stringValue) < tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Maximum cannot be less than Minimum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(MIN_HUE_VALUE)) {
            text = "Minimum hue value";
            tempVal = findPreference(MAX_HUE_VALUE).getSharedPreferences().getInt(MAX_HUE_VALUE, 0);
            if (Integer.parseInt(stringValue) > tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Minimum cannot be greater than Maximum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(MAX_HUE_VALUE)) {
            text = "Maximum hue value";
            tempVal = findPreference(MIN_HUE_VALUE).getSharedPreferences().getInt(MIN_HUE_VALUE, 0);
            if (Integer.parseInt(stringValue) < tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Maximum cannot be less than Minimum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(THRESHOLD)) {
            text = "Threshold";
        }
        preference.setTitle(text + " - " + stringValue);
        return true;
    }
}