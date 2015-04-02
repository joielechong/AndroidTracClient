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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class TracClientFragment extends Fragment implements OnGlobalLayoutListener {
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
	protected int fast_move;
	protected int drawer_border;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		tcLog.d(getClass().getName() + ".super", "onAttach ");
		context = (TracStart) activity;
		listener = context;
		large_move = context.getResources().getInteger(R.integer.large_move);
		extra_large_move = context.getResources().getInteger(R.integer.extra_large_move);
		fast_move = context.getResources().getInteger(R.integer.fast_move);
		drawer_border = context.getResources().getInteger(R.integer.drawer_border);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(getClass().getName() + ".super", "onCreate savedInstanceState = " + savedInstanceState);
		if (savedInstanceState != null) {
			Tickets.url = savedInstanceState.getString(Const.CURRENT_URL);
			Tickets.username = savedInstanceState.getString(Const.CURRENT_USERNAME);
			Tickets.password = savedInstanceState.getString(Const.CURRENT_PASSWORD);
			Tickets.sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK, false);
			Tickets.sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK, false);
		}
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
			tcLog.e(getClass().getName(),"Problem retrieving Admod information",e);
			listener.setDispAds(false);
			adUnitId = "";
			testDevices = new String[1];
			testDevices[0] = "";
		}

		if (Const.doAnalytics) {
			// Get a Tracker (should auto-report)
			MyTracker.getInstance(context);
		}
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
            final ActionBar ab = context.getSupportActionBar();
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
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(getClass().getName() + ".super", "onSaveInstanceState");
		savedState.putString(Const.CURRENT_URL, Tickets.url);
		savedState.putString(Const.CURRENT_USERNAME, Tickets.username);
		savedState.putString(Const.CURRENT_PASSWORD, Tickets.password);
		savedState.putBoolean(Const.CURRENT_SSLHACK, Tickets.sslHack);
		savedState.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, Tickets.sslHostNameHack);
		tcLog.d(getClass().getName() + ".super", "onSaveInstanceState = " + savedState);
	}

	@Override
	public void onStart() {
		tcLog.d(getClass().getName() + ".super", "onStart");
		super.onStart();
         
		if (Const.doAnalytics) {
			// Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
			MyTracker.getInstance(context);
			// Get tracker.
			final Tracker t = MyTracker.getTracker(getClass().getName());
			MyTracker.reportActivityStart(context);

			if (t != null) {
				// Send a screen view.
				t.send(new HitBuilders.ScreenViewBuilder().build());
			}
		}
    }

	@Override
	public void onStop() {
		tcLog.d(getClass().getName() + ".super", "onStop");
		super.onStop();
		if (Const.doAnalytics) {
			// Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
			MyTracker.reportActivityStop(context);
		}
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

	public void setHost() {
		tcLog.d(getClass().getName() + ".super", "setHost");
		_ticket = null;
		Tickets.setInvalid();
	}

	public ProgressDialog startProgressBar(String message) {
		ProgressDialog progressBar = null;
		progressBar = new ProgressDialog(context);
		progressBar.setCancelable(true);
		if (message != null) {
			progressBar.setMessage(message);
		}
		progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		if (!context.isFinishing()) {
			progressBar.show();
			return progressBar;
		} else {
			return null;
		}
	}

	public ProgressDialog startProgressBar(int resid) {
		return startProgressBar(context.getString(resid));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private Spinner makeDialogSpinner(Context context, boolean dialogWanted) {
		if (dialogWanted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			return new Spinner(context, Spinner.MODE_DIALOG);
		} else {
			return new Spinner(context);
		}
	}

	private Spinner _makeComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w,
			boolean dialogWanted) {
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
		if (waardes != null && w != null && !w.equals("")) {
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
		tcLog.d(getClass().getName(), "selectTicket = " + ticknr);
		final Ticket t = Tickets.getTicket(ticknr);
		if (t != null && t.hasdata()) {
			listener.onTicketSelected(t);
		} else {
			final ProgressDialog pb = startProgressBar(R.string.downloading);
			new Ticket(ticknr, context, new onTicketCompleteListener() {

				@Override
				public void onComplete(Ticket t2) {
					if (pb != null && !context.isFinishing()) {
						pb.dismiss();
					}
					if (t2.hasdata()) {
						Tickets.putTicket(t2);
						listener.onTicketSelected(t2);
					} else {
						context.runOnUiThread(new Runnable() {

							@Override
							public void run() {
							new AlertDialog.Builder(context)
												.setTitle(R.string.notfound)
												.setMessage(R.string.ticketnotfound)
												.setCancelable(false)
												.setPositiveButton(R.string.oktext, null)
												.create()
												.show();
							}
						});
					}
				}
			});
		}
	}
}
