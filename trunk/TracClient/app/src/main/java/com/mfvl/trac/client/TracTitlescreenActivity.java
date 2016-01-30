/*
 * Copyright (C) 2013,2014,2015 Michiel van Loon
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


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.mfvl.trac.client.Const.*;

public class TracTitlescreenActivity extends Activity implements Thread.UncaughtExceptionHandler {
    private Intent launchTrac = null;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        tcLog.logCall();
        super.onCreate(savedInstanceState);
        tcLog.setContext(this);
        TracGlobal.getInstance(getApplicationContext());
        setContentView(R.layout.activity_titlescreen);
        startService(new Intent(this, RefreshService.class));
    }

    @Override
    public void onStart() {
//        tcLog.logCall();
        super.onStart();

//        boolean adMobAvailable = false;
        boolean adMobAvailable = true;
        launchTrac = new Intent(getApplicationContext(), TracStart.class);

        // adMobAvailable=false;
        launchTrac.putExtra(ADMOB, adMobAvailable);

        final Intent intent = getIntent();

        // Integer ticket = -1;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final String contentString = intent.getDataString();

            // tcLog.d("View intent data = " + contentString);
            if (contentString != null) {
                final Uri uri = Uri.parse(contentString.replace("trac.client.mfvl.com/", ""));
                final List<String> segments = uri.getPathSegments();
                final String u = uri.getScheme() + "://" + uri.getHost() + "/";

                String urlstring = u.replace("tracclient://", "http://").replace("tracclients://",
                                                                                 "https://");
                final int count = segments.size();
                final String mustBeTicket = segments.get(count - 2);

                if ("ticket".equals(mustBeTicket)) {
                    final int ticket = Integer.parseInt(segments.get(count - 1));

                    for (final String segment : segments.subList(0, count - 2)) {
                        urlstring += segment + "/";
                    }
                    launchTrac.putExtra(INTENT_URL, urlstring)
                            .putExtra(INTENT_TICKET, (long) ticket);
                } else {
                    tcLog.w("View intent bad Url");
                }
            }
        }
        handler = new Handler();
        startApp();
    }

    private void startApp() {
//        tcLog.logCall();
        int timerVal = getResources().getInteger(R.integer.startupTimer);
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tcLog.logCall();
                        startActivity(launchTrac);
                        finish();
                    }
                });
            }
        }, timerVal);
    }

    public void onResume() {
        tcLog.logCall();
        super.onResume();
    }

    public void onPause() {
        tcLog.logCall();
        super.onPause();
    }

    public void onStop() {
        tcLog.logCall();
        super.onStop();
    }

    public void onDestroy() {
        tcLog.logCall();
        super.onDestroy();
    }


    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        tcLog.e("Uncaught exception in thread " + thread, ex);
        tcLog.save();
        finish();
    }
}
