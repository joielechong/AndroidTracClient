package com.mfvl.trac.client;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

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
		tcLog.d(this.getClass().getName() + ".super", "onAttach ");
		context = (TracStart) activity;
		listener = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName() + ".super", "onCreate savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
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
		tcLog.d(this.getClass().getName() + ".super", "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
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
					adView.setLayoutParams(ll.getLayoutParams());
					tcLog.d(getClass().getName(), "adView size = "+adView.getHeight());
					ll.addView(adView);
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName() + ".super", "onSaveInstanceState");
		savedState.putString("currentURL", _url);
		savedState.putString("currentUsername", _username);
		savedState.putString("currentPassword", _password);
		savedState.putBoolean("sslHack", _sslHack);
	}

	@Override
	public void onStart() {
		tcLog.d(this.getClass().getName() + ".super", "onStart");
		super.onStart();
		EasyTracker.getInstance(context).activityStart(context);
	}

	@Override
	public void onStop() {
		tcLog.d(this.getClass().getName() + ".super", "onStop");
		super.onStop();
		EasyTracker.getInstance(context).activityStop(context);
	}

	@Override
	public void onDestroy() {
		tcLog.d(this.getClass().getName() + ".super", "onDestroy");
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		tcLog.d(this.getClass().getName() + ".super", "onDetach");
		super.onDetach();
	}

	public void setHost(final String url, final String username, final String password, boolean sslHack) {
		tcLog.d(this.getClass().getName() + ".super", "setHost");
		if (_url != url) {
			_url = url;
			_username = username;
			_password = password;
			_sslHack = sslHack;
			_ticket = null;
		}
	}

	public ProgressDialog startProgressBar(String message) {
		ProgressDialog progressBar = null;
		progressBar = new ProgressDialog(context);
		progressBar.setCancelable(true);
		progressBar.setMessage(message);
		progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressBar.show();
		return progressBar;
	}

	public ProgressDialog startProgressBar(int resid) {
		return startProgressBar(context.getString(resid));
	}

}
