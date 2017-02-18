/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;


public abstract class TracClientFragment extends Fragment implements View.OnClickListener {
    Ticket _ticket = null;
    InterFragmentListener listener = null;
    Bundle fragmentArgs = null;

    static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    abstract int getHelpFile();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listener = (InterFragmentListener) getActivity();
        fragmentArgs = (savedInstanceState == null ? getArguments() : null);
        TracGlobal.initialize(getActivity());
    }

    public void showHelp() {
        final String filename = getString(getHelpFile());
        final DialogFragment about = new TracHelp();
        final Bundle aboutArgs = new Bundle();
        aboutArgs.putString(HELP_FILE, filename);
        aboutArgs.putInt(HELP_ZOOM, webzoom);
        about.setArguments(aboutArgs);
        about.show(getFragmentManager(), "help");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MyLog.d("item=" + item);
        final int itemId = item.getItemId();

        if (itemId == R.id.help) {
            showHelp();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    void showAlertBox(final int titleres, int message) {
        listener.showAlertBox(titleres, getString(message));
    }

    void showAlertBox(final int titleres, CharSequence message) {
        listener.showAlertBox(titleres, message);
    }

    @Nullable
    SpinnerAdapter makeComboAdapter(Collection<Object> waardes, boolean optional) {
//        MyLog.d("waardes = "+waardes+" optional = "+optional);
        if (waardes == null) {
            return null;
        }

        final List<Object> spinValues = new ArrayList<>();

        if (optional) {
            spinValues.add("");
        }

        spinValues.addAll(waardes);

        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinValues);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        return spinAdapter;
    }

    public void selectTicket(int ticknr) {
        MyLog.d("ticknr = " + ticknr);
        listener.getTicket(ticknr, new OnTicketLoadedListener() {
            @Override
            public void onTicketLoaded(Ticket t) {
                if (t != null && t.hasdata()) {
                    listener.onTicketSelected(t);
                }
            }
        });
    }

    void getScreensize(View spin, View but) {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        Drawable drawable = ContextCompat.getDrawable(getActivity(), R.drawable.plus);
        spin.setLayoutParams(
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, widthPixels - drawable.getIntrinsicWidth()));
        but.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, drawable.getIntrinsicWidth()));
    }

    void setOnClickListener(int resid, View v, View.OnClickListener c) {
//		MyLog.d( "resid = "+resid+" v = "+v+" c =" + c);
        try {
            v.findViewById(resid).setOnClickListener(c);
        } catch (Exception ignored) {
        }
    }

    void runOnUiThread(Runnable r) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(r);
        }
    }

    @Override
    public void onClick(View v) {
        MyLog.d("v =" + v);
    }

    public void onServiceConnected() {
        MyLog.logCall();
    }

    public void onServiceDisconnected() {
        MyLog.logCall();
    }

    public void onTicketModelChanged(TicketModel tm) {
        MyLog.logCall();
    }
}
