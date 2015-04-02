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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class TracShowWebPage extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		tcLog.d(getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);
		super.onCreate(savedInstanceState);
		final Intent i = this.getIntent();
		final boolean toonVersie = i.getBooleanExtra(Const.HELP_VERSION, true);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.trac_about);
		final String filename = "file:///android_asset/" + i.getStringExtra(Const.HELP_FILE) + ".html";
		tcLog.d(getClass().getName(), filename + " " + toonVersie);
		final View tv = findViewById(R.id.versionblock);
		if (!toonVersie) {
			tv.setVisibility(View.GONE);
		} else {
			final TextView tv1 = (TextView) findViewById(R.id.about_version_text);
			tv1.setText(Credentials.buildVersion());
		}
		if (Const.doAnalytics) {
			MyTracker.report("Normal","WebView",filename);
		}
		final WebView wv = (WebView) findViewById(R.id.webfile);
		// wv.getSettings().setJavaScriptEnabled(true);
		wv.setWebViewClient(new WebViewClient());
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			final int webzoom = getResources().getInteger(R.integer.webzoom);
			wv.getSettings().setTextZoom(webzoom);
		}
		wv.loadUrl(filename);
		tcLog.d(getClass().getName(), "webview = " + wv);
	}

	@Override
	public void onStart() {
		tcLog.d(getClass().getName(), "onStart");
		super.onStart();
		if (Const.doAnalytics) {
			MyTracker.reportActivityStart(TracShowWebPage.this);
		}
	}

	@Override
	public void onStop() {
		tcLog.d(getClass().getName(), "onStop");
		super.onStop();
		if (Const.doAnalytics) {
			MyTracker.reportActivityStop(TracShowWebPage.this);
		}
	}

}