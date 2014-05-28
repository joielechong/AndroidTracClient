package com.mfvl.trac.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;

public class TracShowWebPage extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " +
		// savedInstanceState);
		super.onCreate(savedInstanceState);
		final Intent i = this.getIntent();
		final boolean toonVersie = i.getBooleanExtra("version", true);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.trac_about);
		final String filename = "file:///android_asset/" + i.getStringExtra("file") + ".html";
		// tcLog.d(this.getClass().getName(), filename + " " + toonVersie);
		final TextView tv = (TextView) findViewById(R.id.about_version_text);
		final TextView tv1 = (TextView) findViewById(R.id.v1);
		final TextView tv2 = (TextView) findViewById(R.id.v2);
		if (!toonVersie) {
			tv.setVisibility(View.GONE);
			tv1.setVisibility(View.GONE);
			tv2.setVisibility(View.GONE);
		} else {
			final String versie = Credentials.buildVersion(this);
			tv.setText(versie);
		}
		final WebView wv = (WebView) findViewById(R.id.webfile);
		wv.setWebViewClient(new WebViewClient());
		wv.loadUrl(filename);
	}

	@Override
	public void onStart() {
		// tcLog.d(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		// tcLog.d(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

}