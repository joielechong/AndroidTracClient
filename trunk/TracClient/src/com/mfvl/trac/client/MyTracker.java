package com.mfvl.trac.client;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.HitBuilders;

public class MyTracker {
	private static Tracker mTracker = null;
	private static MyTracker _instance = null;
	private static GoogleAnalytics analytics = null;
	private int delay;

	private MyTracker(final Activity context) {
		try {
			delay = context.getResources().getInteger(R.integer.waitAnalytics);
		} catch (Exception e) {
			tcLog.e(getClass().getName(),"no delay",e);
			delay = 500;
		}
		Thread anathread = new Thread ("AnayticsStartUp") {
			@Override
			public void run() {
				try {
					String PROPERTY_ID = Credentials.metaDataGetString("com.mfvl.trac.client.propertyId");
					if (mTracker == null) {
						Tracker t = null;
						analytics = GoogleAnalytics.getInstance(context);
						if (analytics != null) {
							tcLog.d(getClass().getName(),"Initialize analytics"); 
							analytics.enableAutoActivityReports(context.getApplication());
							t = analytics.newTracker(PROPERTY_ID);
							if (t != null) {
								t.enableExceptionReporting(true);
								t.setAnonymizeIp(true);
								t.setSessionTimeout(600);
								t.enableAutoActivityTracking(true);
								t.setSampleRate(100.0);
							}
						}
						tcLog.d(getClass().getName(),"propertyId = " + t.get("ga_trackingId")); 
						mTracker = t;
					}
				} catch (final NameNotFoundException e) {
					tcLog.e(getClass().getName(), "getApplicationInfo", e);
				}
			}
		};
		try {
			anathread.start();
			anathread.join(delay);
		} catch (InterruptedException e) {
			tcLog.e(getClass().getName(),"anathread error",e);
		}
	}

	public static MyTracker getInstance(Activity context) {
		if (_instance == null) {
			_instance = new MyTracker(context);
		}
		return _instance;
	}

	static Tracker getTracker(String screenName) {
		if (mTracker != null) { 
			mTracker.setScreenName(screenName);
		}
		return mTracker;
	}
	
	static void report(String cat, String action, String label) {
		if (mTracker != null) {
			mTracker.send(new HitBuilders.EventBuilder().setCategory(cat).setAction(action).setLabel(label).build());
		}
	}

	public static void reportActivityStart(final Activity acContext) {
		if (analytics != null) {
			analytics.reportActivityStart(acContext);
		}
	}
	
	public static void reportActivityStop(final Activity acContext) {
		if (analytics != null) {
			analytics.reportActivityStop(acContext);
		}
	}
}
