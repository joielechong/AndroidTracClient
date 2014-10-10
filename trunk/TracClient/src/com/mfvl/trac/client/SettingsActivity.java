/*
 * Copyright (C) 2014 Michiel van Loon
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

import android.preference.PreferenceActivity;
import android.os.Bundle;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.mfvl.trac.client.util.tcLog;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
		//Get a Tracker (should auto-report)
		((TracClient) getApplication()).getTracker(Const.TrackerName.APP_TRACKER);

    }
	
	@Override
	public void onStart() {
		super.onStart();
		tcLog.d(this.getClass().getName(), "onStart");
		//Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		tcLog.d(getClass().getName(), "onStop");
		//Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
	}
	
}