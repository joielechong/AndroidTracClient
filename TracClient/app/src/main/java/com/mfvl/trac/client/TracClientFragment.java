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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.mfvl.trac.client.Const.*;

abstract public class TracClientFragment extends Fragment implements View.OnClickListener {

    public Ticket _ticket = null;
    public TracStart context;
    public InterFragmentListener listener = null;
    protected int large_move;
    protected int extra_large_move;
    protected Handler tracStartHandler = null;
    protected int helpFile = -1;
    protected Bundle fragmentArgs = null;

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        tcLog.d("(C) ");
        onMyAttach(activity);
    }

    private void onMyAttach(Context activity) {
        context = (TracStart) activity;
        TracGlobal.getInstance(context.getApplicationContext());
        listener = (InterFragmentListener) activity;
        tracStartHandler = listener.getHandler();
        large_move = context.getResources().getInteger(R.integer.large_move);
        extra_large_move = context.getResources().getInteger(R.integer.extra_large_move);
        fragmentArgs = getArguments();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tcLog.d("(A) ");
        onMyAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d("savedInstanceState = " + savedInstanceState);

        tracStartHandler = listener.getHandler();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d("item=" + item + " " + helpFile);
        final int itemId = item.getItemId();

        if (itemId == R.id.help && helpFile != -1) {
            final Intent launchTrac = new Intent(context, TracShowWebPage.class);
            final String filename = context.getString(helpFile);
            launchTrac.putExtra(HELP_FILE, filename);
            launchTrac.putExtra(HELP_VERSION, false);
            startActivity(launchTrac);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    protected void sendMessageToHandler(int msg, Object o) {
        tcLog.d("msg = " + msg + " o = " + o);
        tracStartHandler.obtainMessage(msg, o).sendToTarget();
    }

    protected void showAlertBox(final int titleres, final int message, final String addit) {
        tracStartHandler.obtainMessage(MSG_SHOW_DIALOG, titleres, message, addit).sendToTarget();
    }

    protected SpinnerAdapter makeComboAdapter(Context context, List<Object> waardes, boolean optional) {
        if (waardes == null) {
            return null;
        }

        final List<Object> spinValues = new ArrayList<>();

        if (optional) {
            spinValues.add("");
        }

        spinValues.addAll(waardes);

        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(context,
                                                                    android.R.layout.simple_spinner_item,
                                                                    spinValues);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        return spinAdapter;
    }

    protected void selectTicket(int ticknr) {
        tcLog.d("ticknr = " + ticknr);
        final Ticket t = listener.getTicket(ticknr);
        if (t != null && t.hasdata()) {
            listener.onTicketSelected(t);
        }
    }

    protected void getScreensize(View spin, View but) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.plus);
        spin.setLayoutParams(
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                                              widthPixels - drawable.getIntrinsicWidth()));
        but.setLayoutParams(
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                                              drawable.getIntrinsicWidth()));
    }

    protected void setListener(int resid) {
        setListener(resid, this.getView(), this);
    }

    protected void setListener(int resid, View v, View.OnClickListener c) {
//		tcLog.d( "resid = "+resid+" v = "+v+" c =" + c);
        try {
            v.findViewById(resid).setOnClickListener(c);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClick(View v) {
        tcLog.d("v =" + v);
    }
}
