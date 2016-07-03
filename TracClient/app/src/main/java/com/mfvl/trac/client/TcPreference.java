/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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