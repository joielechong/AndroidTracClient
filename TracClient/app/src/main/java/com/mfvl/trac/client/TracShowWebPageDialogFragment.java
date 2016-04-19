/*
 * Copyright (C) 2013-2016 Michiel van Loon
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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;

import static com.mfvl.trac.client.Const.*;

public class TracShowWebPageDialogFragment extends DialogFragment implements View.OnClickListener {
    private String fileUrl;
    private WebView webfile;
    private TextView textAbout;
    private View scrollAbout;
    private View mainView = null;
    private int webzoom = 0;

    @SuppressLint("InflateParams")
    public void preLoad(LayoutInflater inflater, Bundle args) {

        final boolean toonVersie = args.getBoolean(HELP_VERSION);
        final String fileName = args.getString(HELP_FILE);
        final boolean cookieInform = TracGlobal.getCookieInform();
        webzoom = args.getInt(HELP_ZOOM);

        mainView = inflater.inflate(R.layout.trac_about, null);
        fileUrl = "file:///android_asset/" + fileName + ".html";

        final View versionblock = mainView.findViewById(R.id.versionblock);
        tcLog.d(fileUrl + " " + toonVersie + " " + versionblock);
        webfile = (WebView) mainView.findViewById(R.id.webfile);
        textAbout = (TextView) mainView.findViewById(R.id.textAbout);
        scrollAbout = mainView.findViewById(R.id.scrollAbout);

        if (!toonVersie) {
            versionblock.setVisibility(View.GONE);
        } else {
            final TextView about_version_text = (TextView) mainView.findViewById(R.id.about_version_text);
            about_version_text.setText(TracGlobal.getVersion());
            View keuzeblock = mainView.findViewById(R.id.keuzeblock);
            if (!cookieInform) {
                keuzeblock.setVisibility(View.GONE);
            } else {
                keuzeblock.setVisibility(View.VISIBLE);
                mainView.findViewById(R.id.showchanges).setOnClickListener(this);
                mainView.findViewById(R.id.showcookies).setOnClickListener(this);
            }
        }
        showWebpage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.logCall();
        if (mainView == null) {
            preLoad(inflater, getArguments());
        }
        return mainView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        tcLog.logCall();
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    public void onClick(View v) {
        tcLog.d(v.getId());
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
//        scrollAbout.setVisibility(View.GONE);
        webfile.loadUrl(fileUrl);
        webfile.setVisibility(View.VISIBLE);
        // webfile.getSettings().setJavaScriptEnabled(true);
        webfile.getSettings().setTextZoom(webzoom);
        tcLog.d(webfile.getContentHeight());
        scrollAbout.setVisibility(View.VISIBLE);
        textAbout.setText(null);
    }

    private void showCookies() {
        tcLog.logCall();
        webfile.setVisibility(View.GONE);
        scrollAbout.setVisibility(View.VISIBLE);
        textAbout.setText(R.string.cookieInform);
//        webfile.loadData(getResources().getString(R.string.cookieInform),"text/html",null);
        //TracGlobal.setCookieInform(false);
    }
}
