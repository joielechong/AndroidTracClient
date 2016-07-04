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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

import static com.mfvl.trac.client.Const.*;

public class TracShowWebPageDialogFragment extends TcDialogFragment implements TabLayout.OnTabSelectedListener {
    private String fileUrl;
    private View mainView = null;
    private int webzoom = 0;

    @SuppressLint("InflateParams")
    public void preLoad(LayoutInflater inflater, Bundle args) {
        MyLog.logCall();
        final String fileName = args.getString(HELP_FILE);
        webzoom = args.getInt(HELP_ZOOM);

        mainView = inflater.inflate(R.layout.trac_about, null);
        fileUrl = "file:///android_asset/" + fileName + ".html";
        MyLog.d(fileUrl);
    }

    private void startFragment(Fragment frag) {
        MyLog.d(frag);
        if (frag != null) {
            getChildFragmentManager().beginTransaction().replace(R.id.about_frame, frag).commit();
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        MyLog.d(tab);
        switch (tab.getPosition()) {
            case 0:
                startFragment(new AboutFragment());
                break;
            case 1:
                Fragment frag = new ChangeFragment();
                Bundle args = new Bundle();
                args.putString(HELP_FILE, fileUrl);
                args.putInt(HELP_ZOOM, webzoom);
                frag.setArguments(args);
                startFragment(frag);
                break;
            case 2:
                startFragment(new CookiesFragment());
                break;
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MyLog.logCall();
        if (mainView == null) {
            preLoad(inflater, getArguments());
        }
        TabLayout tl = (TabLayout) mainView.findViewById(R.id.tabs);
        tl.setOnTabSelectedListener(this);
        return mainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        startFragment(new AboutFragment());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MyLog.logCall();
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    public static class AboutFragment extends Fragment {
        @SuppressLint("InflateParams")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            return inflater.inflate(R.layout.trac_version, null);
        }
    }

    public static class ChangeFragment extends Fragment {
        @SuppressLint("InflateParams")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            final View v = inflater.inflate(R.layout.trac_help, null);
            final WebView wv = (WebView) v.findViewById(R.id.webfile);
            final Bundle args = getArguments();
            if (args != null) {
                final String fileUrl = args.getString(HELP_FILE);
                final int webzoom = args.getInt(HELP_ZOOM);
                wv.loadUrl(fileUrl);
                wv.getSettings().setTextZoom(webzoom);
            }
            return v;
        }
    }

    public static class CookiesFragment extends Fragment {
        @SuppressLint("InflateParams")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            return inflater.inflate(R.layout.cookies, null);
        }
    }
}
