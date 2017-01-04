package com.shuaybprojects.smsvoice;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.widget.BaseAdapter;


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
            addPreferencesFromResource(R.xml.preferences);

            PreferenceScreen excList = (PreferenceScreen) findPreference(EXC_LIST_KEY);
            String summary = "";

            if (MainActivity.contactNames != null) {
                //Create all the checkboxes inside of the PreferenceScreen
                for (int i = 0; i < MainActivity.contactNames.length; i++) {
                    CheckBoxPreference checkbox = new CheckBoxPreference(getActivity());
                    checkbox.setTitle(MainActivity.contactNames[i][0]);
                    checkbox.setKey(MainActivity.contactNames[i][2]);
                    checkbox.setSummary(MainActivity.contactNames[i][1]);
                    checkbox.setDefaultValue(false);
                    excList.addPreference(checkbox);
                    if (checkbox.isChecked()) {
                        summary = summary + checkbox.getTitle() + "\n";
                    }
                }
            }
            excList.setSummary(summary);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //Checkbox keys all start with 'key_'
            if (key.startsWith("key_")) {
                PreferenceScreen excList = (PreferenceScreen)findPreference(EXC_LIST_KEY);

                String summary = "";
                for (int i = 0; i < MainActivity.contactNames.length; i++) {
                    CheckBoxPreference checkbox = (CheckBoxPreference) findPreference(MainActivity.contactNames[i][2]);
                    if (checkbox.isChecked()) {
                        summary = summary + checkbox.getTitle() + "\n";
                    }
                }
                excList.setSummary(summary);
            }
            ((BaseAdapter)getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
