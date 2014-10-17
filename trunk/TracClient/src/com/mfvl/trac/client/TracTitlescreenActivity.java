/*
 * Copyright (C) 2013,2014 Michiel van Loon
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

public class TracTitlescreenActivity extends Activity {

	Tracker t;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			tcLog.setContext(this);
			Credentials.getInstance(this);
			LoginInfo.getInstance();
			Tickets.getInstance();
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.activity_titlescreen);

			final TextView tv = (TextView) findViewById(R.id.version_content);
			tv.setText(Credentials.buildVersion());

		} catch (final Exception e) {
			tcLog.toast("crash: " + e.getMessage());
		}
		// Get a Tracker (should auto-report)
		t = ((TracClient) getApplication()).getTracker(Const.TrackerName.APP_TRACKER);
		t.setScreenName(getClass().getName());
	}

	@Override
	public void onStart() {
		// tcLog.i(getClass().getName(), "onStart");
		super.onStart();

		boolean adMobAvailable = false;
		try {
			final int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			tcLog.d(getClass().getName(), "Google Play Services available? : " + isAvailable);
			if (isAvailable == ConnectionResult.SUCCESS) {
				adMobAvailable = true;
			} else {
				if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
					final Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, 123456);
					dialog.show();
				} else {
					tcLog.d(getClass().getName(), "Hoe kom je hier");
				}
			}
		} catch (final Exception e) {
			tcLog.e(getClass().getName(), "Exception while determining Google Play Services", e);
		}

		// Get an Analytics tracker to report app starts &amp; uncaught
		// exceptions etc.
		final GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		analytics.reportActivityStart(this);

		final Intent launchTrac = new Intent(getApplicationContext(), TracStart.class);

		// adMobAvailable=false;
		launchTrac.putExtra(Const.ADMOB, adMobAvailable);

		String urlstring = null;

		final Intent intent = getIntent();
		// Integer ticket = -1;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			final String contentString = intent.getDataString();
			// tcLog.d(getClass().getName(), "View intent data = " +
			// contentString);
			if (contentString != null) {
				final Uri uri = Uri.parse(contentString.replace("trac.client.mfvl.com/", ""));
				final List<String> segments = uri.getPathSegments();
				final String u = uri.getScheme() + "://" + uri.getHost() + "/";
				urlstring = u.replace("tracclient://", "http://").replace("tracclients://", "https://");
				final int count = segments.size();
				final String mustBeTicket = segments.get(count - 2);
				if ("ticket".equals(mustBeTicket)) {
					final int ticket = Integer.parseInt(segments.get(count - 1));
					for (final String segment : segments.subList(0, count - 2)) {
						urlstring += segment + "/";
					}
					// Build and send an Event.
					t.send(new HitBuilders.EventBuilder().setCategory("Startup").setAction("URI start").setLabel(urlstring)
							.setValue(ticket).build());
					launchTrac.putExtra(Const.INTENT_URL, urlstring);
					launchTrac.putExtra(Const.INTENT_TICKET, (long) ticket);
				} else {
					tcLog.w(getClass().getName(), "View intent bad Url");
					urlstring = null;
				}
			}
		}
		final Handler handler = new Handler();
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						startActivity(launchTrac);
						finish();
					}
				});
			}
		}, 3000);
	}

	@Override
	public void onStop() {
		// tcLog.i(getClass().getName(), "onStop");
		super.onStop();
		// Get an Analytics tracker to report app starts &amp; uncaught
		// exceptions etc.
		final GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		analytics.reportActivityStop(this);

	}
}
