package com.mfvl.trac.client;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Button;

import com.mfvl.mfvllib.MyLog;

import java.util.List;

public class TcPreference extends PreferenceActivity {
    @Override
    protected boolean isValidFragment(String frag) {
        MyLog.d(frag);
        return true;
    }

   @SuppressWarnings("unused")
   public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            MyLog.i("Arguments: " + getArguments());
            addPreferencesFromResource(R.xml.preferences);
        }
    }

}