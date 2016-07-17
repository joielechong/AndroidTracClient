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
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;

import com.mfvl.mfvllib.MyLog;

import static com.mfvl.trac.client.Const.*;

public class TracShowWebPageDialogFragment extends TcDialogFragment implements TabLayout.OnTabSelectedListener {
    private final static String TABSELECTED = "tabselected";
    private String fileUrl;
    private int webzoom = 0;
    private int tabSelected = 0;
    private TabLayout tl;

    private void startFragment(Fragment frag) {
        MyLog.d(frag);
        if (frag != null) {
            getChildFragmentManager().beginTransaction().replace(R.id.about_frame, frag).commit();
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        MyLog.d(tab);
        tabSelected = tab.getPosition();
        selectFragment(tabSelected);
    }

    private void selectFragment(int tab) {
        switch (tab) {
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
        onTabSelected(tab);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MyLog.logCall();
        if (savedInstanceState != null) {
            tabSelected = savedInstanceState.getInt(TABSELECTED);
        }
        final String fileName = getArguments().getString(HELP_FILE);
        webzoom = getArguments().getInt(HELP_ZOOM);

        View mainView = inflater.inflate(R.layout.trac_about, container, false);
        fileUrl = "file:///android_asset/" + fileName + ".html";
        tl = (TabLayout) mainView.findViewById(R.id.tabs);
        MyLog.d(fileUrl);
        return mainView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TABSELECTED,tabSelected);
    }

    @Override
    public void onStart() {
        super.onStart();
        TabLayout.Tab tab = tl.getTabAt(tabSelected);
        if (tab != null) {
            tab.select();
            selectFragment(tabSelected);
        }
    }

    public void onResume() {
        super.onResume();
        tl.addOnTabSelectedListener(this);
    }

    public void onPause() {
        super.onPause();
        tl.removeOnTabSelectedListener(this);
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
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            return inflater.inflate(R.layout.trac_version,  container,false);
        }
    }

    public static class ChangeFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            final View v = inflater.inflate(R.layout.trac_help, container,false);
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
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            MyLog.logCall();
            return inflater.inflate(R.layout.cookies, container,false);
        }
    }
}
