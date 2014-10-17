package com.mfvl.trac.client;

import java.util.HashMap;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.mfvl.trac.client.util.tcLog;

public class TracClient extends Application {

	private class MyLogger extends tcLog implements Logger {
		private int _l = 0;
		private String _tag = "";

		public void setTag(String tag) {
			_tag = tag;
		}

		@Override
		public void error(String s) {
			super.e(_tag, s);
		}

		@Override
		public void error(Exception e) {
			super.e(_tag, e);
		}

		@Override
		public void info(String s) {
			super.i(_tag, s);
		}

		@Override
		public void verbose(String s) {
			super.v(_tag, s);
		}

		@Override
		public void warn(String s) {
			super.w(_tag, s);
		}

		@Override
		public int getLogLevel() {
			return _l;
		}

		@Override
		public void setLogLevel(int l) {
			super.d(getClass().getName(), "LogLevel set to " + l);
			_l = l;
		}

	}

	// The following line should be changed to include the correct property id.
	private static final String PROPERTY_ID = "UA-44748801-1";

	public static int GENERAL_TRACKER = 0;
	HashMap<Const.TrackerName, Tracker> mTrackers = new HashMap<Const.TrackerName, Tracker>();

	public TracClient() {
		super();
	}

	synchronized Tracker getTracker(Const.TrackerName trackerId) {
		if (!mTrackers.containsKey(trackerId)) {
			final GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			final MyLogger ml = new MyLogger();
			ml.setTag("GAV4-TracClient");
			analytics.setLogger(ml);
			final Tracker t = trackerId == Const.TrackerName.APP_TRACKER ? analytics.newTracker(R.xml.app_tracker)
					: trackerId == Const.TrackerName.GLOBAL_TRACKER ? analytics.newTracker(PROPERTY_ID) : null;
					if (t == null) {
						return null;
					}
					mTrackers.put(trackerId, t);
		}
		return mTrackers.get(trackerId);
	}
}