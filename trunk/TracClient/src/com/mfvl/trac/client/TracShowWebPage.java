package com.mfvl.trac.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;

public class TracShowWebPage extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trac_about);
		final Intent i = this.getIntent();
		final boolean toonVersie = i.getBooleanExtra("version", true);
		final String filename = "file:///android_asset/" + i.getStringExtra("file") + ".html";
		Log.i(this.getClass().getName(), filename + " " + toonVersie);
		final TextView tv = (TextView) findViewById(R.id.about_version_text);
		final TextView tv1 = (TextView) findViewById(R.id.v1);
		final TextView tv2 = (TextView) findViewById(R.id.v2);
		if (!toonVersie) {
			tv.setVisibility(View.GONE);
			tv1.setVisibility(View.GONE);
			tv2.setVisibility(View.GONE);
		} else {
			final String versie = Credentials.buildVersion(this, true);
			tv.setText(versie);
		}
		final WebView wv = (WebView) findViewById(R.id.webfile);
		wv.loadUrl(filename);
	}

	@Override
	public void onStart() {
		Log.i(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		Log.i(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

}