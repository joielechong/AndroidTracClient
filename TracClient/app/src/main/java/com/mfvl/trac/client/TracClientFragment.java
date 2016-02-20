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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

import static com.mfvl.trac.client.Const.*;

abstract public class TracClientFragment extends Fragment implements View.OnClickListener {

    Ticket _ticket = null;
    TracStart context;
    InterFragmentListener listener = null;
    int large_move;
    int helpFile = -1;
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
        tcLog.d("(C) ");
        onMyAttach(activity);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        tcLog.d("(A) ");
        onMyAttach(activity);
    }

    void onMyAttach(Context activity) {
        context = (TracStart) activity;
        TracGlobal.getInstance(context.getApplicationContext());
        listener = (InterFragmentListener) activity;
        tracStartHandler = listener.getHandler();
        large_move = context.getResources().getInteger(R.integer.large_move);
        fragmentArgs = getArguments();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.logCall();
        tracStartHandler = listener.getHandler();
    }
	
	public void showHelp() {
		if (helpFile != -1) {
			final Intent launchTrac = new Intent(context, TracShowWebPage.class);
			final String filename = context.getString(helpFile);
			launchTrac.putExtra(HELP_FILE, filename);
			launchTrac.putExtra(HELP_VERSION, false);
			startActivity(launchTrac);	
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d("item=" + item + " " + helpFile);
        final int itemId = item.getItemId();

        if (itemId == R.id.help && helpFile != -1) {
			showHelp();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    void sendMessageToHandler(int msg, Object o) {
        tcLog.d("msg = " + msg + " o = " + o);
        tracStartHandler.obtainMessage(msg, o).sendToTarget();
    }

    void showAlertBox(final int titleres, final int message, final String addit) {
        tracStartHandler.obtainMessage(MSG_SHOW_DIALOG, titleres, message, addit).sendToTarget();
    }

    SpinnerAdapter makeComboAdapter(Context context, List<Object> waardes, boolean optional) {
//        tcLog.d("waardes = "+waardes+" optional = "+optional);
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
        tcLog.d("ticknr = " + ticknr);
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
//		tcLog.d( "resid = "+resid+" v = "+v+" c =" + c);
        try {
            v.findViewById(resid).setOnClickListener(c);
        } catch (Exception ignored) {
        }
    }

    /*
        void waitForTicketModel() {
            while (tm == null) {
                tcLog.d("tm is still null");
                try {
                    Thread.sleep(100);
                } catch (Exception ignored){
                }
            }
            tm.wacht();
        }
    */
    @Override
    public void onClick(View v) {
        tcLog.d("v =" + v);
    }

    void onNewTicketModel(TicketModel newTm) {
        tcLog.d("newTm == null " + (newTm == null));
        tm = newTm;
    }
}
