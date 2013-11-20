package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;

public class TracClientFragment extends Fragment {
	public Ticket _ticket = null;
	public String _url = null;
	public String _username = null;
	public String _password = null;
	public boolean _sslHack = false;
	public TracStart context;
	private AdView adView = null;
	public InterFragmentListener listener = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.i(this.getClass().getName() + ".super", "onAttach ");
		context = (TracStart) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName() + ".super", "onCreate");
		Log.i(this.getClass().getName() + ".super", "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			_url = savedInstanceState.getString("currentURL");
			_username = savedInstanceState.getString("currentUsername");
			_password = savedInstanceState.getString("currentPassword");
			_sslHack = savedInstanceState.getBoolean("sslHack", false);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.i(this.getClass().getName() + ".super", "onActivityCreated");
		Log.i(this.getClass().getName() + ".super", "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		listener = context;
		if (listener != null && listener.dispAds()) {
			final LinearLayout ll = (LinearLayout) getView().findViewById(R.id.adBlock);
			if (ll != null) {
				adView = new AdView(context, AdSize.BANNER, "ca-app-pub-3154118785616242/7091928539");
				final AdRequest adRequest = new AdRequest();
				if (adView != null && adRequest != null) {
					if (Credentials.isDebuggable(context)) {
						adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
						adRequest.addTestDevice("9A306D880ED517968FD50C3A2340839E");
					}
					adView.loadAd(adRequest);
					ll.addView(adView);
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.i(this.getClass().getName() + ".super", "onSaveInstanceState");
		savedState.putString("currentURL", _url);
		savedState.putString("currentUsername", _username);
		savedState.putString("currentPassword", _password);
		savedState.putBoolean("sslHack", _sslHack);
	}

	@Override
	public void onStart() {
		Log.i(this.getClass().getName() + ".super", "onStart");
		super.onStart();
		EasyTracker.getInstance(context).activityStart(context);
	}

	@Override
	public void onStop() {
		Log.i(this.getClass().getName() + ".super", "onStop");
		super.onStop();
		EasyTracker.getInstance(context).activityStop(context);
	}

	@Override
	public void onDestroy() {
		Log.i(this.getClass().getName() + ".super", "onDestroy");
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}

	public void setHost(final String url, final String username, final String password, boolean sslHack) {
		Log.i(this.getClass().getName() + ".super", "setHost");
		if (_url != url) {
			_url = url;
			_username = username;
			_password = password;
			_sslHack = sslHack;
			_ticket = null;
		}
	}

	public int findValueInArray(List<Object> a, Object w) {
		if (w != null) {
			for (int i = 0; i < a.size(); i++) {
				if (w.equals(a.get(i))) {
					return i;
				}
			}
		}
		return 0; // if not found choose the first
	}

	public Spinner makeComboSpin(final String veldnaam, List<Object> waardes, boolean optional, Object w) {
		final List<Object> spinValues = new ArrayList<Object>();

		if (optional) {
			spinValues.add(null);
		}

		for (int i = 0; i < waardes.size(); i++) {
			spinValues.add(waardes.get(i));
		}

		final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_item, spinValues);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner valSpinner = new Spinner(context);
		valSpinner.setAdapter(spinAdapter);
		if (w != null && !w.equals("")) {
			valSpinner.setSelection(waardes.indexOf(w), true);
		}
		return valSpinner;
	}

	public void makeRow(TableLayout tl, final String veldnaam, View tv2, final int id) {
		if (veldnaam != null) {
			final TableRow tr1 = new TableRow(context);
			tr1.setId(id + 100);
			final TextView tv1 = new TextView(context, null, android.R.attr.textAppearanceMedium);
			tv1.setId(id + 200);
			tr1.addView(tv1);
			tv1.setText(veldnaam);
			tl.addView(tr1);
		}
		final TableRow tr2 = new TableRow(context);

		tv2.setId(id + 300);
		tr2.addView(tv2);
		tl.addView(tr2);
	}

}
