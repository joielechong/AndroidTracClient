package com.mfvl.trac.client;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

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
            EditTextPreference pref = (EditTextPreference)findPreference(getString(R.string.prefNrItemsKey));
            String val = pref.getText();
            pref.setSummary(val);
        }
		
		public void onStart() {
			super.onStart();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}
		
		public void onStop() {
			super.onStop();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            MyLog.d(key);
            Preference pref = findPreference(key);

            if (pref instanceof EditTextPreference) {
                EditTextPreference editPref = (EditTextPreference) pref;
                String val = editPref.getText();
                pref.setSummary(val);
                TracGlobal.ticketGroupCount = Integer.parseInt(val);
                MyLog.d("val = "+val);
            }
        }
	}

}