package com.mfvl.trac.client;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

public class TracTitlescreenActivity extends Activity {

	private EasyTracker tracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			tcLog.setContext(this);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			setContentView(R.layout.activity_titlescreen);

			final TextView tv = (TextView) findViewById(R.id.version_content);
			tv.setText(Credentials.buildVersion(this));

		} catch (final Exception e) {
			tcLog.toast("crash: " + e.getMessage());
		}
	}

	@Override
	public void onStart() {
		// tcLog.i(this.getClass().getName(), "onStart");
		super.onStart();
		tracker = EasyTracker.getInstance(this);
		tracker.activityStart(this);
		final Intent intent = getIntent();
		String urlstring = null;
		Integer ticket = -1;
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			final String contentString = intent.getDataString();
			// tcLog.d(getClass().getName(), "View intent data = " +
			// contentString);
			if (contentString != null) {
				final Uri uri = Uri.parse(contentString.replace("trac.client.mfvl.com/", ""));
				final List<String> segments = uri.getPathSegments();
				final String u = uri.getScheme() + "://" + uri.getHost() + "/";
				urlstring = u.replace("tracclient://", "http://").replace("tracclients://", "https://");
				final int count = segments.size();
				final String mustBeTicket = segments.get(count - 2);
				if ("ticket".equals(mustBeTicket)) {
					ticket = Integer.parseInt(segments.get(count - 1));
					for (final String segment : segments.subList(0, count - 2)) {
						urlstring += segment + "/";
					}
					tracker.send(MapBuilder.createEvent("Startup", // Event
																	// category
																	// (required)
							"URI start", // Event action (required)
							urlstring, // Event label
							ticket.longValue()) // Event value
							.build());
				} else {
					tcLog.w(getClass().getName(), "View intent bad Url");
					urlstring = null;
				}
			}
		}
		final Intent launchTrac = new Intent(getApplicationContext(), TracStart.class);
		launchTrac.putExtra("AdMob", true);
		if (urlstring != null) {
			launchTrac.putExtra("url", urlstring);
			launchTrac.putExtra("ticket", (long) ticket);
		}
		final Handler handler = new Handler();
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						startActivity(launchTrac);
						finish();
					}
				});
			}
		}, 3000);
	}

	@Override
	public void onStop() {
		// tcLog.i(this.getClass().getName(), "onStop");
		super.onStop();
		tracker.activityStop(this);
	}
}
