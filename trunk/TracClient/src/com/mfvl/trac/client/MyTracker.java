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
    private static int delay = 500;
	private static boolean doAnalytics = Const.doAnalytics;

    private MyTracker(final Activity context) {
        try {
            delay = context.getResources().getInteger(R.integer.waitAnalytics);
        } catch (Exception e) {}
        Thread anathread = new Thread("AnalyticsStartUp") {
            @Override
            public void run() {
                try {
                    String PROPERTY_ID = Credentials.metaDataGetString("com.mfvl.trac.client.propertyId");

                    if (mTracker == null) {
                        Tracker t = null;

                        analytics = GoogleAnalytics.getInstance(context);
                        if (analytics != null) {
                            tcLog.d(getClass().getName(), "Initialize analytics"); 
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
						doAnalytics &= (t!=null);
//                        tcLog.d(getClass().getName()+"."+this.getName(), "tracker = " + t); 
                        mTracker = t;
                    }
                } catch (final NameNotFoundException e) {
                    tcLog.e(getClass().getName()+"."+this.getName(), "getApplicationInfo", e);
					doAnalytics = false;
                }
            }
        };

        anathread.start();
        try {
            anathread.join(delay);
        } catch (InterruptedException e) {
            tcLog.e(getClass().getName(), anathread.getName()+ " exception", e);
        }
    }

    public static MyTracker getInstance(Activity context) {
        if (doAnalytics && _instance == null) {
            _instance = new MyTracker(context);
        }
        return _instance;
    }
	
	public static void setDoAnalytics(final boolean newDoAnalytics) {
		doAnalytics = newDoAnalytics;
	}
	
    static void report(String cat, String action, String label) {
        if (doAnalytics && mTracker != null) {
            mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(cat)
				.setAction(action)
				.setLabel(label)
				.build());
        }
    }
	
	static void hitScreen(final String screenName) {
        if (doAnalytics && mTracker != null) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
		}
	}

    public static void reportActivityStart(final Activity acContext) {
        if (doAnalytics && analytics != null) {
            analytics.reportActivityStart(acContext);
        }
    }
	
    public static void reportActivityStop(final Activity acContext) {
        if (doAnalytics && analytics != null) {
            analytics.reportActivityStop(acContext);
        }
    }
}
