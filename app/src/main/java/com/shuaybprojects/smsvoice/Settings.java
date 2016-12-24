package com.shuaybprojects.smsvoice;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;

public class Settings extends AppCompatActivity {

    public static final String EXC_LIST_KEY = "exclusion_list_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            String summary = "";

            if (MainActivity.contactNames != null) {
                //Create all the checkboxes inside of the PreferenceScreen
                for (int i = 0; i < MainActivity.contactNames.length; i++) {
                    CheckBoxPreference checkbox = new CheckBoxPreference(getActivity());
                    checkbox.setTitle(MainActivity.contactNames[i][0]);
                    checkbox.setKey(MainActivity.contactNames[i][2]);
                    checkbox.setSummary(MainActivity.contactNames[i][1]);
                    checkbox.setDefaultValue(false);
                    ((PreferenceScreen) findPreference(EXC_LIST_KEY)).addPreference(checkbox);
                    if (checkbox.isChecked()) {
                        summary = summary + checkbox.getTitle() + "\n";
                    }
                }
            }
            findPreference(EXC_LIST_KEY).setSummary(summary);
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //Checkbox keys all start with 'key_'
            if (key.startsWith("key_")) {
                String summary = "";
                for (int i = 0; i < MainActivity.contactNames.length; i++) {
                    CheckBoxPreference checkbox = (CheckBoxPreference) findPreference(MainActivity.contactNames[i][2]);
                    if (checkbox.isChecked()) {
                        summary = summary + checkbox.getTitle() + "\n";
                    }
                }
                findPreference(EXC_LIST_KEY).setSummary(summary);
            }
        }
    }
}
