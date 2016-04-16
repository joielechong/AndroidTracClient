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

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import static com.mfvl.trac.client.Const.*;

public class TracShowWebPageDialogFragment extends DialogFragment implements View.OnClickListener {
    private String filename;
    private WebView wv;
    private TextView cv;
    private View sv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.logCall();
        final Bundle args = getArguments();
        final boolean toonVersie = args.getBoolean(HELP_VERSION);

		View ll = inflater.inflate(R.layout.trac_about,container);
        filename = "file:///android_asset/" + args.getString(HELP_FILE) + ".html";

        final View tv = ll.findViewById(R.id.versionblock);
        //tcLog.d(filename + " " + toonVersie + " " + tv);
        wv = (WebView) ll.findViewById(R.id.webfile);
        cv = (TextView) ll.findViewById(R.id.textAbout);
        sv = ll.findViewById(R.id.scrollAbout);

        if (!toonVersie) {
            tv.setVisibility(View.GONE);
        } else {
            final TextView tv1 = (TextView) ll.findViewById(R.id.about_version_text);
            tv1.setText(TracGlobal.getVersion());
            boolean cookies = TracGlobal.getCookieInform();
            View kb = ll.findViewById(R.id.keuzeblock);
            if (!cookies) {
                kb.setVisibility(View.GONE);
            } else {
                kb.setVisibility(View.VISIBLE);
                TextView sch = (TextView) ll.findViewById(R.id.showchanges);
                sch.setOnClickListener(this);
                if (cookies) {
                    TextView v = (TextView) ll.findViewById(R.id.showcookies);
                    v.setOnClickListener(this);
                }
            }
        }
        showWebpage();
		return ll;
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
    
    private void showWebpage() {
        tcLog.logCall();
        wv.setVisibility(View.VISIBLE);
        sv.setVisibility(View.GONE);
        // wv.getSettings().setJavaScriptEnabled(true);
        // wv.setWebViewClient(new WebViewClient());
        wv.getSettings().setTextZoom(getResources().getInteger(R.integer.webzoom));
        wv.loadUrl(filename);
    }

    private void showCookies() {
        tcLog.logCall();
        sv.setVisibility(View.VISIBLE);
        wv.setVisibility(View.GONE);
        cv.setText(R.string.cookieInform);
		//TracGlobal.setCookieInform(false);
    }
}
