package com.mfvl.trac.client;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

public class TracTitlescreenActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			tcLog.setContext(this);
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	            WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.activity_titlescreen);

			final String versie = Credentials.buildVersion(this, true);
			final TextView tv = (TextView) findViewById(R.id.version_content);
			tv.setText(versie);

		} catch (final Exception e) {
			tcLog.toast("crash: " + e.getMessage());
		}
	}

	@Override
	public void onStart() {
		tcLog.i(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		final Handler handler = new Handler();
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						final Intent launchTrac = new Intent(getApplicationContext(), TracStart.class);
						launchTrac.putExtra("AdMob", true);
						startActivity(launchTrac);
						finish();
					}
				});
			}
		}, 3000);
	}

	@Override
	public void onStop() {
		tcLog.i(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
}
