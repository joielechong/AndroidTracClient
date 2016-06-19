package com.mfvl.trac.client;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.mfvl.mfvllib.MyLog;

public class TcPreference extends PreferenceActivity {

    @Override
    protected boolean isValidFragment(String frag) {
        MyLog.d(frag);
        return true;
    }

    @SuppressWarnings("unused")
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            MyLog.i("Arguments: " + getArguments());
            getPreferenceManager().setSharedPreferencesName(Const.PREFS_NAME);
            addPreferencesFromResource(R.xml.preferences);
            EditTextPreference editPref = (EditTextPreference) findPreference(getString(R.string.prefNrItemsKey));
            String val = editPref.getText();
            editPref.setSummary(val);
            PreferenceScreen filterPref = (PreferenceScreen) findPreference(TracGlobal.prefFilterKey);
            val = TracGlobal.getFilterString();
            filterPref.setSummary(val);
            PreferenceScreen sortPref = (PreferenceScreen) findPreference(TracGlobal.prefSortKey);
            val = TracGlobal.getSortString();
            sortPref.setSummary(val);
            MyLog.logCall();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        public void onDestroy() {
            super.onDestroy();
            MyLog.logCall();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            MyLog.d(key);
            String val;
            Preference pref = findPreference(key);
            MyLog.d(pref);

            if (pref instanceof EditTextPreference && getString(R.string.prefNrItemsKey).equals(key)) {
                EditTextPreference editPref = (EditTextPreference) pref;
                val = editPref.getText();
                pref.setSummary(val);
                TracGlobal.ticketGroupCount = Integer.parseInt(val);
                MyLog.d("val = " + val);
            } else if (TracGlobal.prefFilterKey.equals(key)) {
                val = TracGlobal.getFilterString();
                pref.setSummary(val);
            } else if (TracGlobal.prefSortKey.equals(key)) {
                val = TracGlobal.getSortString();
                pref.setSummary(val);
            }
        }
    }

}