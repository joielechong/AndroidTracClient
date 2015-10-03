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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;


public class TracTitlescreenActivity extends Activity implements Thread.UncaughtExceptionHandler {
	private int timerVal = 3000;
	private Intent launchTrac = null; 
	private Handler handler = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.setContext(this);
        tcLog.i( "");
		
		Thread.setDefaultUncaughtExceptionHandler (this);
		
        Credentials.getInstance(getApplicationContext());
        boolean doAnalytics = Const.doAnalytics;
		if (doAnalytics) {
			try {
				doAnalytics &= Credentials.metaDataGetBoolean("com.mfvl.trac.client.useAnalytics");
			} catch (final Exception e) {
				tcLog.e( "getApplicationInfo", e);
			}
		}
        tcLog.i( "doAnalytics = " + doAnalytics);
		MyTracker.setDoAnalytics(doAnalytics);
        try {
            setContentView(R.layout.activity_titlescreen);
            final TextView tv = (TextView) findViewById(R.id.version_content);

            tv.setText(Credentials.getVersion());
            // Get a Tracker (should auto-report)
            MyTracker.getInstance(this);
        } catch (final Exception e) {
            tcLog.e( "crash", e);
        }
    }

    @Override
    public void onStart() {
        tcLog.i( "");
        super.onStart();

        boolean adMobAvailable = false;

        try {
            final int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

            tcLog.d( "Google Play Services available? : " + isAvailable);
            if (isAvailable == ConnectionResult.SUCCESS) {
                adMobAvailable = true;
            } else {
                if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
                    GooglePlayServicesUtil.getErrorDialog(isAvailable, this, 123456).show();
                } else {
                    tcLog.d( "Hoe kom je hier");
                }
            }
        } catch (final Exception e) {
            tcLog.e( "Exception while determining Google Play Services", e);
        }

        // Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
        MyTracker.reportActivityStart(this);
		
        launchTrac = new Intent(getApplicationContext(), TracStart.class);

        // adMobAvailable=false;
        launchTrac.putExtra(Const.ADMOB, adMobAvailable);

        String urlstring = null;

        final Intent intent = getIntent();

        // Integer ticket = -1;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final String contentString = intent.getDataString();

            // tcLog.d( "View intent data = " + contentString);
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
                    launchTrac.putExtra(Const.INTENT_URL, urlstring);
                    launchTrac.putExtra(Const.INTENT_TICKET, (long) ticket);
                } else {
                    tcLog.w( "View intent bad Url");
                    urlstring = null;
                }
            }
        }
        handler = new Handler();
		startApp();
//		cookieInform();
	}

    @Override
    public void onStop() {
        tcLog.i( "");
        super.onStop();
        // Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
        MyTracker.reportActivityStop(this);
    }
	private void startApp() {
        tcLog.d( "startApp");
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
		}, timerVal);
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		tcLog.e("Uncaught exception in thread "+ thread,ex);
		tcLog.save();
		finish();
	}
}
