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


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.app.Fragment;
import android.app.ActionBar;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;


abstract public class TracClientFragment extends Fragment implements OnGlobalLayoutListener {
 
    public Ticket _ticket = null;
    public TracStart context;
    private AdView adView = null;
    private View aboveView;
    private LinearLayout adViewContainer = null;
    public InterFragmentListener listener = null;
    private boolean adsVisible = true;
    private int padTop;
    private int padRight;
    private int padBot;
    private int padLeft;
    private String adUnitId;
    private String[] testDevices;
    protected int large_move;
    protected int extra_large_move;
    protected int drawer_border;
	protected Handler tracStartHandler;
	
	private void onMyAttach(Context activity) {
        context = (TracStart) activity;
        listener = (InterFragmentListener) activity;
 		tracStartHandler = listener.getHandler();
		large_move = context.getResources().getInteger(R.integer.large_move);
        extra_large_move = context.getResources().getInteger(R.integer.extra_large_move);
        drawer_border = context.getResources().getInteger(R.integer.drawer_border);
	}

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        tcLog.d(getClass().getName() + ".super", "onAttach(C) ");
		onMyAttach(activity);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        tcLog.d(getClass().getName() + ".super", "onAttach(A) ");
		onMyAttach(activity);
	}

	protected void sendMessageToHandler(int msg) {
        tcLog.d(getClass().getName() + ".super", "sendMessageToHandler msg = " + msg);
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(msg));
	}

	protected void sendMessageToHandler(int msg,Object o) {
        tcLog.d(getClass().getName() + ".super", "sendMessageToHandler msg = " + msg+ " o = "+o);
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(msg,o));
	}

	protected void showAlertBox(final int titleres, final int message, final String addit){
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(TracStart.MSG_SHOW_DIALOG,titleres,message,addit));
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d(getClass().getName() + ".super", "onCreate savedInstanceState = " + savedInstanceState);
        try {
            adUnitId = Credentials.metaDataGetString("com.mfvl.trac.client.adUnitId");
            final String t = Credentials.metaDataGetString("com.mfvl.trac.client.testDevices");

            try {
                testDevices = t.split("\\,");
            } catch (final IllegalArgumentException e) { // only 1
                testDevices = new String[1];
                testDevices[0] = t;
            }
        } catch (final Exception e) {
            tcLog.e(getClass().getName(), "Problem retrieving Admod information", e);
            listener.setDispAds(false);
            adUnitId = "";
            testDevices = new String[1];
            testDevices[0] = "";
        }

        // Get a Tracker (should auto-report)
        MyTracker.getInstance(context);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tcLog.d(getClass().getName() + ".super", "onViewCreated");
        aboveView = view.findViewById(R.id.aboveAdBlock);
        adViewContainer = (LinearLayout) view.findViewById(R.id.adBlock);

        final View activityRootView = view.findViewById(R.id.updateTop);

        if (listener != null && listener.getDispAds()) {
            if (adViewContainer != null) {
                adView = new AdView(context);
                adView.setAdUnitId(adUnitId);
                adView.setAdSize(AdSize.SMART_BANNER);

                final AdRequest.Builder arb = new AdRequest.Builder();

                if (adView != null && arb != null) {
                    arb.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
                    if (Credentials.isDebuggable()) {
                        for (final String t : testDevices) {
                            tcLog.d(getClass().getName() + ".super", "onViewCreated testDevice = " + t);
                            arb.addTestDevice(t);
                        }
                    }
                    arb.setGender(AdRequest.GENDER_UNKNOWN);
                    final AdRequest adRequest = arb.build();

                    if (adRequest != null) {
                        try {
                            adView.loadAd(adRequest);
                            adView.setLayoutParams(adViewContainer.getLayoutParams());
                            // tcLog.d(getClass().getName(), "adView size = " +adView.getHeight());
                            adViewContainer.addView(adView);
                        } catch (final Exception e) {
                            listener.setDispAds(false);
                        }
                    }
                }
                if (activityRootView != null && aboveView != null) {
                    padTop = aboveView.getPaddingTop();
                    padRight = aboveView.getPaddingRight();
                    padBot = aboveView.getPaddingBottom();
                    padLeft = aboveView.getPaddingLeft();
                    adsVisible = true;
                    activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
                }
            }
        } else {
            if (aboveView != null) {
                aboveView.setPadding(0, 0, 0, 0);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        final View view = getView();

        if (view != null) {
            final View activityRootView = view.findViewById(R.id.updateTop);
            final ActionBar ab = context.getActionBar();
            final Rect r = new Rect();

            // r will be populated with the coordinates of your view that area still visible.
            activityRootView.getWindowVisibleDisplayFrame(r);

            final int heightDiff = activityRootView.getRootView().getHeight() - (r.bottom - r.top);

            if (heightDiff > 100) { // if more than 100 pixels,
                // its probably a keyboard...
                if (adsVisible) {
                    adViewContainer.setVisibility(View.GONE);
                    aboveView.setPadding(padLeft, padTop, padRight, 0);
                    adsVisible = false;
                }
                ab.hide();
            } else {
                if (!adsVisible) {
                    adViewContainer.setVisibility(View.VISIBLE);
                    aboveView.setPadding(padLeft, padTop, padRight, padBot);
                    adsVisible = true;
                }
                ab.show();
            }
        }
    }

    @Override
    public void onStart() {
        tcLog.d(getClass().getName() + ".super", "onStart");
        super.onStart();
         
        // Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
		MyTracker.getInstance(context);
        MyTracker.reportActivityStart(context);
        MyTracker.hitScreen(getClass().getName());
    }

    @Override
    public void onStop() {
        tcLog.d(getClass().getName() + ".super", "onStop");
        super.onStop();
        MyTracker.reportActivityStop(context);
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        tcLog.d(getClass().getName() + ".super", "onDestroy");
        if (getView() != null && adView != null) {
            adView.destroy();
        }
        adView = null;
        super.onDestroy();
    }

    private Spinner makeDialogSpinner(Context context, boolean dialogWanted) {
        if (dialogWanted) {
            return new Spinner(context, Spinner.MODE_DIALOG);
        } else {
            return new Spinner(context);
        }
    }

    private Spinner _makeComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w, boolean dialogWanted) {
        final List<Object> spinValues = new ArrayList<Object>();

        if (optional) {
            spinValues.add("");
        }

        if (waardes != null) {
            for (int i = 0; i < waardes.size(); i++) {
                spinValues.add(waardes.get(i));
            }
        }

        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_item, spinValues);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner valSpinner = makeDialogSpinner(context, dialogWanted);

        valSpinner.setAdapter(spinAdapter);
        if (waardes != null && w != null && !"".equals(w)) {
            valSpinner.setSelection(waardes.indexOf(w) + (optional ? 1 : 0), true);
        }
        return valSpinner;
    }

    protected Spinner makeDialogComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w) {
        return _makeComboSpin(context, veldnaam, waardes, optional, w, true);
    }

    protected Spinner makeComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w) {
        return _makeComboSpin(context, veldnaam, waardes, optional, w, false);
    }

    protected void selectTicket(int ticknr) {
        tcLog.d(getClass().getName()+".super", "selectTicket = " + ticknr);
        final Ticket t = listener.getTicket(ticknr);
		if (t != null  && t.hasdata()) {
			listener.onTicketSelected(t);
		}
    }
	
	public static void hideSoftKeyboard(Activity activity) {
		InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	abstract public void showHelp();
	
	public void showHelpFile(int resId) {
        tcLog.d(getClass().getName()+".super", "showHelp");
        final Intent launchTrac = new Intent(context, TracShowWebPage.class);
        final String filename = context.getString(resId);

        launchTrac.putExtra(Const.HELP_FILE, filename);
        launchTrac.putExtra(Const.HELP_VERSION, false);
        startActivity(launchTrac);
	}
	
}