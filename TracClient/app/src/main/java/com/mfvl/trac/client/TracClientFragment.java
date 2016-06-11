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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.List;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

abstract public class TracClientFragment extends Fragment implements View.OnClickListener {

    Ticket _ticket = null;
    Activity context;
    InterFragmentListener listener = null;
    Bundle fragmentArgs = null;
    TicketModel tm = null;
    private Handler tracStartHandler = null;

    static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        MyLog.d("(C) ");
        onMyAttach(activity);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        MyLog.d("(A) ");
        onMyAttach(activity);
    }

    void onMyAttach(Context activity) {
        context = (Activity) activity;
        TracGlobal.getInstance(context.getApplicationContext());
        listener = (InterFragmentListener) activity;
        fragmentArgs = getArguments();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyLog.logCall();
        tracStartHandler = listener.getHandler();
    }

    abstract int getHelpFile();

    public void showHelp() {
        final String filename = context.getString(getHelpFile());
		final TracShowWebPageDialogFragment about =  new TracShowWebPageDialogFragment();
		final Bundle aboutArgs = new Bundle();
        aboutArgs.putString(HELP_FILE, filename);
        aboutArgs.putBoolean(HELP_VERSION, false);
        aboutArgs.putBoolean(HELP_COOKIES, false);
        aboutArgs.putInt(HELP_ZOOM, webzoom);
        about.preLoad(context.getLayoutInflater(), aboutArgs);
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

    void sendMessageToHandler(int msg, Object o) {
        MyLog.d("msg = " + msg + " o = " + o);
        tracStartHandler.obtainMessage(msg, o).sendToTarget();
    }

    void showAlertBox(final int titleres, final int message, final String addit) {
        tracStartHandler.obtainMessage(MSG_SHOW_DIALOG, titleres, message, addit).sendToTarget();
    }

    SpinnerAdapter makeComboAdapter(Context context, List<Object> waardes, boolean optional) {
//        MyLog.d("waardes = "+waardes+" optional = "+optional);
        if (waardes == null) {
            return null;
        }

        final List<Object> spinValues = new ArrayList<>();

        if (optional) {
            spinValues.add("");
        }

        spinValues.addAll(waardes);

        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinValues);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        return spinAdapter;
    }

    void selectTicket(int ticknr) {
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
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.plus);
        spin.setLayoutParams(
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, widthPixels - drawable.getIntrinsicWidth()));
        but.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, drawable.getIntrinsicWidth()));
    }

    void setListener(int resid) {
        setListener(resid, this.getView(), this);
    }

    void setListener(int resid, View v, View.OnClickListener c) {
//		MyLog.d( "resid = "+resid+" v = "+v+" c =" + c);
        try {
            v.findViewById(resid).setOnClickListener(c);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClick(View v) {
        MyLog.d("v =" + v);
    }

    void onNewTicketModel(TicketModel newTm) {
        MyLog.d("newTm == null " + (newTm == null));
        tm = newTm;
    }
}
