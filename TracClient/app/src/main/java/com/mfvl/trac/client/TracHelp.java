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

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings.TextSize;
import android.webkit.WebView;

import com.mfvl.mfvllib.MyLog;

import static com.mfvl.trac.client.Const.*;

public class TracHelp extends TcDialogFragment {
    private String fileUrl = null;
    private WebView webfile = null;
    private int webzoom = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MyLog.logCall();
        final String fileName = getArguments().getString(HELP_FILE);
        webzoom = getArguments().getInt(HELP_ZOOM);

        View mainView = inflater.inflate(R.layout.trac_help, container, false);
        fileUrl = "file:///android_asset/" + fileName + ".html";

        MyLog.d(fileUrl);
        webfile = (WebView) mainView.findViewById(R.id.webfile);
        showWebpage();
        return mainView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MyLog.logCall();
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    @SuppressWarnings("deprecation")
    private void showWebpage() {
        MyLog.logCall();
        webfile.loadUrl(fileUrl);
        webfile.setVisibility(View.VISIBLE);
        // webfile.getSettings().setJavaScriptEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            webfile.getSettings().setTextZoom(webzoom);
        } else {
            webfile.getSettings().setTextSize((webzoom > 100 ? TextSize.LARGER : (webzoom == 100 ? TextSize.NORMAL : TextSize.SMALLER)));
        }
        MyLog.d(webfile.getContentHeight());
    }
}
