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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import static com.mfvl.trac.client.Const.HELP_FILE;
import static com.mfvl.trac.client.Const.HELP_VERSION;

public class TracShowWebPage extends Activity implements View.OnClickListener {
    private String filename;
    private WebView wv;
    private TextView cv;
    private View sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TracGlobal.getInstance(getApplicationContext());
        tcLog.d("savedInstanceState = " + savedInstanceState);
        final Intent i = getIntent();
        final boolean toonVersie = i.getBooleanExtra(HELP_VERSION, true);

        setContentView(R.layout.trac_about);
        filename = "file:///android_asset/" + i.getStringExtra(HELP_FILE) + ".html";

        final View tv = findViewById(R.id.versionblock);
        tcLog.d(filename + " " + toonVersie + " " + tv);
        wv = (WebView) findViewById(R.id.webfile);
        cv = (TextView) findViewById(R.id.textAbout);
        sv = findViewById(R.id.scrollAbout);

        if (!toonVersie) {
            tv.setVisibility(View.GONE);
        } else {
            final TextView tv1 = (TextView) findViewById(R.id.about_version_text);
            tv1.setText(TracGlobal.getVersion());
            boolean cookies = TracGlobal.getCookieInform();
            View kb = findViewById(R.id.keuzeblock);
            if (!cookies) {
                kb.setVisibility(View.GONE);
            } else {
                kb.setVisibility(View.VISIBLE);
                TextView sch = (TextView) findViewById(R.id.showchanges);
                sch.setOnClickListener(this);
                if (cookies) {
                    TextView v = (TextView) findViewById(R.id.showcookies);
                    v.setOnClickListener(this);
                }
            }
        }
        showWebpage();
    }

    private void showWebpage() {
        wv.setVisibility(View.VISIBLE);
        sv.setVisibility(View.GONE);
        // wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.getSettings().setTextZoom(getResources().getInteger(R.integer.webzoom));
        wv.loadUrl(filename);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.showchanges:
                showWebpage();
                break;

            case R.id.showcookies:
                showCookies();
                break;
        }
    }

    private void showCookies() {
        sv.setVisibility(View.VISIBLE);
        wv.setVisibility(View.GONE);
        cv.setText(R.string.cookieInform);
//		TracGlobal.setCookieInform();
    }
}
