package org.wheatgenetics.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.wheatgenetics.onekk.R;
import org.wheatgenetics.onekk.SettingsFragment;

/**
 * Created by sid on 2/14/18.
 */

public class PersonDialogPreference extends DialogPreference {

    private View personView = null;
    private EditText fName = null;
    private EditText lName = null;

    public PersonDialogPreference(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);
        setDialogLayoutResource(R.layout.person);
        setPersistent(false);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setTitle(R.string.user_name);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    public void onBindDialogView(View view) {
        personView = view;
        fName = personView.findViewById(R.id.firstName);
        lName = personView.findViewById(R.id.lastName);

        fName.setText(getSharedPreferences().getString(SettingsFragment.FIRST_NAME, ""));
        lName.setText(getSharedPreferences().getString(SettingsFragment.LAST_NAME, ""));

        super.onBindDialogView(personView);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            String firstName = fName.getText().toString().trim();
            String lastName = lName.getText().toString().trim();

            if (firstName.length() == 0 | lastName.length() == 0) {
                Toast.makeText(getContext(), R.string.no_blank, Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(), "Person set as: " + firstName + " " + lastName, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor ed = getSharedPreferences().edit();
            ed.putString(SettingsFragment.FIRST_NAME, firstName);
            ed.putString(SettingsFragment.LAST_NAME, lastName);
            ed.apply();
        }
    }
}
