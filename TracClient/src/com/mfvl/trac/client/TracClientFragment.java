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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.support.v4.content.ContextCompat;
import android.view.inputmethod.InputMethodManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;


abstract public class TracClientFragment extends Fragment implements OnGlobalLayoutListener, View.OnClickListener {
 
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
	protected int helpFile = -1;
	protected Bundle fragmentArgs = null;
	
	private void onMyAttach(Context activity) {
        context = (TracStart) activity;
		Credentials.getInstance(context.getApplicationContext());
        listener = (InterFragmentListener) activity;
 		tracStartHandler = listener.getHandler();
		large_move = context.getResources().getInteger(R.integer.large_move);
        extra_large_move = context.getResources().getInteger(R.integer.extra_large_move);
        drawer_border = context.getResources().getInteger(R.integer.drawer_border);
		fragmentArgs = getArguments();
	}

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        tcLog.d( "(C) ");
		onMyAttach(activity);
    }
	
	@SuppressWarnings("deprecation")
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        tcLog.d( "(A) ");
		onMyAttach(activity);
	}

	protected void sendMessageToHandler(int msg,Object o) {
        tcLog.d( "msg = " + msg+ " o = "+o);
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(msg,o));
	}

	protected void showAlertBox(final int titleres, final int message, final String addit){
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(TracStart.MSG_SHOW_DIALOG,titleres,message,addit));
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d( "savedInstanceState = " + savedInstanceState);
        try {
            adUnitId = context.getString(R.string.adUnitId);
            final String t = Credentials.metaDataGetString("com.mfvl.trac.client.testDevices");

            try {
                testDevices = t.split(",");
            } catch (final IllegalArgumentException e) { // only 1
                testDevices = new String[1];
                testDevices[0] = t;
            }
        } catch (final Exception e) {
            tcLog.e( "Problem retrieving Admod information", e);
            listener.setDispAds(false);
            adUnitId = "";
            testDevices = new String[1];
            testDevices[0] = "";
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        tcLog.d( "view = "+view);
        aboveView = view.findViewById(R.id.aboveAdBlock);
		adViewContainer = (LinearLayout) view.findViewById(R.id.adBlock);
 
        if (listener.getDispAds() && adViewContainer != null) {
			adView = new AdView(context);
			adView.setAdUnitId(adUnitId);
			adView.setAdSize(AdSize.SMART_BANNER);

			final AdRequest.Builder arb = new AdRequest.Builder();

			if (adView != null && arb != null) {
				arb.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				if (Credentials.isDebuggable()) {
					for (final String t : testDevices) {
						tcLog.d( "testDevice = " + t);
						arb.addTestDevice(t);
					} 
				}
				arb.setGender(AdRequest.GENDER_UNKNOWN);
				final AdRequest adRequest = arb.build();

				if (adRequest != null) {
					try {
						adView.loadAd(adRequest);
						adView.setLayoutParams(adViewContainer.getLayoutParams());
						// tcLog.d( "adView size = " +adView.getHeight());
						adViewContainer.addView(adView);
					} catch (final Exception e) {
						if (aboveView != null) {
							aboveView.setPadding(0, 0, 0, 0);
						}
						listener.setDispAds(false);
					}
				}
			}
			if (view != null && aboveView != null) {
				padTop = aboveView.getPaddingTop();
				padRight = aboveView.getPaddingRight();
				padBot = aboveView.getPaddingBottom();
				padLeft = aboveView.getPaddingLeft();
				adsVisible = true;
				view.getViewTreeObserver().addOnGlobalLayoutListener(this);
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
            final ActionBar ab = context.getActionBar();
            final Rect r = new Rect();

            // r will be populated with the coordinates of your view that area still visible.
            view.getWindowVisibleDisplayFrame(r);

            final int heightDiff = view.getRootView().getHeight() - (r.bottom - r.top);

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
    public void onPause() {
        tcLog.logCall();
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        tcLog.logCall();
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d( "item=" + item+ " "+ helpFile);
        final int itemId = item.getItemId();

        if (itemId == R.id.help && helpFile != -1) {
			final Intent launchTrac = new Intent(context, TracShowWebPage.class);
			final String filename = context.getString(helpFile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, false);
			startActivity(launchTrac);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        tcLog.logCall();
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
		if (waardes == null) 
			return null;
		
        final List<Object> spinValues = new ArrayList<>();

        if (optional) {
            spinValues.add("");
        }

		for (final Object o: waardes) {
			spinValues.add(o);
		}

        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinValues);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner valSpinner = makeDialogSpinner(context, dialogWanted);

        valSpinner.setAdapter(spinAdapter);
        if (w != null && !"".equals(w)) {
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
        tcLog.d( "ticknr = " + ticknr);
        final Ticket t = listener.getTicket(ticknr);
		if (t != null  && t.hasdata()) {
			listener.onTicketSelected(t);
		}
    }
	
	public static void hideSoftKeyboard(Activity activity) {
		InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	protected void getScreensize(View spin,View but) {
		DisplayMetrics metrics = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int heightPixels = metrics.heightPixels;
		int widthPixels = metrics.widthPixels;
		Drawable drawable = ContextCompat.getDrawable(context,R.drawable.plus);
		spin.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, widthPixels-drawable.getIntrinsicWidth()));
		but.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, drawable.getIntrinsicWidth()));
	}

	protected void setListener(int resid){
		setListener(resid,this.getView(),this);
	}
	
	protected void setListener(int resid,View v,View.OnClickListener c) {
//		tcLog.d( "resid = "+resid+" v = "+v+" c =" + c);
		try {
			v.findViewById(resid).setOnClickListener(c);
		} catch (Exception ignored) {}
	}
	
	@Override
	public void onClick(View v) {
		tcLog.d( "v =" + v);
	}
}
