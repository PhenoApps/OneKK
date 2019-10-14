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

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static String USER_NAME = "org.wheatgenetics.onekk.USER_NAME";
    public static String FIRST_NAME = "org.wheatgenetics.onekk.FIRST_NAME";
    public static String LAST_NAME = "org.wheatgenetics.onekk.LAST_NAME";
    public static String DISPLAY_ANALYSIS = "org.wheatgenetics.onekk.DISPLAY_ANALYSIS";
    public static String COLOR_THRESHOLD = "org.wheatgenetics.onekk.COLOR_THRESHOLD";
    public static String MIN_SEED_VALUE = "org.wheatgenetics.onekk.MIN_SEED_VALUE";
    public static String MAX_SEED_VALUE = "org.wheatgenetics.onekk.MAX_SEED_VALUE";
    public static String MIN_VALUE = "org.wheatgenetics.onekk.MIN_HUE_VALUE";
    public static String MAX_VALUE = "org.wheatgenetics.onekk.MAX_HUE_VALUE";
    public static String THRESHOLD = "org.wheatgenetics.onekk.THRESHOLD";
    public static String COIN_DB = "org.wheatgenetics.onekk.COIN_DB";
    public static String COIN_SIZE = "org.wheatgenetics.onekk.COIN_SIZE";
    public static String COIN_COUNTRY = "org.wheatgenetics.onekk.COIN_COUNTRY";
    public static String COIN_NAME = "org.wheatgenetics.onekk.COIN_NAME";
    public static String PROCESSING_TECHNIQUE = "org.wheatgenetics.onekk.PROCESSING_TECHNIQUE";
    public static String ASK_BACKGROUND_PROCESSING = "org.wheatgenetics.onekk.ASK_BACKGROUND_PROCESSING";
    public static String ASK_MULTI_PROCESSING = "org.wheatgenetics.onekk.ASK_MULTI_PROCESSING";
    public static String PARAM_AREA_LOW = "edu.ksu.wheatgenetics.seedcounter.AREA_LOW";
    public static String PARAM_AREA_HIGH = "edu.ksu.wheatgenetics.seedcounter.AREA_HIGH";
    public static String PARAM_DEFAULT_RATE = "edu.ksu.wheatgenetics.seedcounter.DEFAULT_RATE";
    public static String PARAM_SIZE_LOWER_BOUND_RATIO =
            "edu.ksu.wheatgenetics.seedcounter.SIZE_LOWER_BOUND_RATIO";
    public static String PARAM_NEW_SEED_DIST_RATIO =
            "edu.ksu.wheatgenetics.seedcounter.NEW_SEED_DIST_RATIO";

    private MySQLiteHelper mySQLiteHelper = null;
    private List<CoinRecord> coinRecordList;
    private ArrayList<String> arrayList;
    private ListPreference listPreference;
    private String[] selectColumns;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            addPreferencesFromResource(R.xml.preferences);
        } catch (Exception ex) {
            //Log.e("Settings Fragment","Preferences setup failed");
        }

        mySQLiteHelper = new MySQLiteHelper(getPreferenceScreen().getContext());

        //additional setup for REFERENCE COIN SIZE settings
        additionalPreferenceSetup(findPreference(COIN_COUNTRY));

        //additional setup for MIN/MAX checks in Range settings
        additionalPreferenceSetup(findPreference(MIN_SEED_VALUE), "Minimum seed value");
        additionalPreferenceSetup(findPreference(MAX_SEED_VALUE), "Maximum seed value");
        //additionalPreferenceSetup(findPreference(MIN_VALUE), "Minimum hue value");
        //additionalPreferenceSetup(findPreference(MAX_VALUE), "Maximum hue value");
        //additionalPreferenceSetup(findPreference(THRESHOLD), "Threshold");
    }

    /**
     * Adding additional preferences to REFERENCE COIN SIZE
     * adds a listener for Country and based on the value, populates the
     * list values for coin name list
     */
    private void additionalPreferenceSetup(Preference preference) {
        listPreference = (ListPreference) preference;
        arrayList = new ArrayList<>();
        selectColumns = new String[]{"country"};
        coinRecordList = mySQLiteHelper.getFromCoins(selectColumns, null, null, true);
        int itemCount = coinRecordList.size();
        if (itemCount != 0) {
            for (int i = 0; i < itemCount; i++) {
                String temp = coinRecordList.get(i).getCountry();
                arrayList.add(temp);
            }
        }

        listPreference.setEntries(arrayList.toArray(new String[arrayList.size()])); //COIN COUNTRY
        listPreference.setEntryValues(arrayList.toArray(new String[arrayList.size()])); // COIN COUNTRY

        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                listPreference = (ListPreference) findPreference(COIN_NAME);
                ArrayList<String> anotherArrayList = new ArrayList<>();
                arrayList = new ArrayList<>();
                listPreference.setEnabled(true);
                //String country = preference.getSharedPreferences().getString(COIN_COUNTRY,null);
                String country = newValue.toString();
                coinRecordList = mySQLiteHelper.getFromCoins(null, selectColumns, new String[]{country}, false);
                int itemCount = coinRecordList.size();
                if (itemCount != 0) {
                    for (int i = 0; i < itemCount; i++) {
                        String temp = coinRecordList.get(i).getName();
                        arrayList.add(temp);
                        temp = coinRecordList.get(i).getDiameter();
                        anotherArrayList.add(temp);
                    }
                }

                // Adding COIN NAME as the entries to List Preference
                listPreference.setEntries(arrayList.toArray(new String[arrayList.size()]));

                // Adding COIN DIAMETERS as the entry values to List Preference
                listPreference.setEntryValues(anotherArrayList.toArray(new String[anotherArrayList.size()]));

                return true;
            }
        });

        //If the country preference value is already set then populate the list for coin names
        if (preference.getSharedPreferences().contains(COIN_COUNTRY))
            listPreference.getOnPreferenceChangeListener().onPreferenceChange(preference, ((ListPreference) preference).getValue());
    }

    /**
     * Adding additional preferences to MIN/MAX range sliders
     */
    private void additionalPreferenceSetup(Preference preference, String text) {
        bindPreferenceToValue(preference);
        preference.setTitle(text + " - " + preference.getSharedPreferences().getInt(preference.getKey(), 0));
    }

    /**
     * Set the listener onPreferenceChange to watch for MIN/MAX value changes.
     */
    private void bindPreferenceToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
    }

    /**
     * Returns true if the range seek bar value is within the
     * compared value(MIN<MAX or MAX>MIN), else returns false and restores the original value
     */
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
        } /*else if (preference.getKey().equals(MIN_VALUE)) {
            text = "Minimum hue value";
            tempVal = findPreference(MAX_VALUE).getSharedPreferences().getInt(MAX_VALUE, 0);
            if (Integer.parseInt(stringValue) > tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Minimum cannot be greater than Maximum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(MAX_VALUE)) {
            text = "Maximum hue value";
            tempVal = findPreference(MIN_VALUE).getSharedPreferences().getInt(MIN_VALUE, 0);
            if (Integer.parseInt(stringValue) < tempVal) {
                Toast.makeText(getPreferenceScreen().getContext(), "Maximum cannot be less than Minimum", Toast.LENGTH_LONG).show();
                return false;
            }
        } else if (preference.getKey().equals(THRESHOLD)) {
            text = "Threshold";
        }*/
        preference.setTitle(text + " - " + stringValue);
        return true;
    }
}