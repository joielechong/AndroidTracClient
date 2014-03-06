package com.mfvl.trac.client.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class tcLog {
	private static Activity _c = null;
	private static boolean doToast = false;
	private static boolean doBuffer = true;
	private static String debugString = "";
	@SuppressLint("SimpleDateFormat")
	private static SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void setContext(Activity c) {
		_c = c;
	}

	public static String getDebug() {
		return debugString;
	}

	private static void _toast(Activity c, String string) {
		Toast.makeText(c, string, Toast.LENGTH_SHORT).show();
	}

	public static void toast(final String string) {
		if (_c != null) {
			if (Looper.getMainLooper().equals(Looper.myLooper())) {
				_toast(_c, string);
			} else {
				_c.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						_toast(_c, string);
					}
				});
			}
		}
	}

	private static void myLog(final String tag, final String message) {
		if (doBuffer) {
			final Date d = new Date();
			d.setTime(System.currentTimeMillis());
			final String date = s.format(new Date());
			debugString += "\n" + date + " " + tag + ": " + message;
		}
		if (doToast) {
			toast(tag + ": " + message);
		}
	}

	private static void myLog(final String tag, final String message, Throwable tr) {
		if (doBuffer) {
			final Date d = new Date();
			d.setTime(System.currentTimeMillis());
			final String date = s.format(new Date());
			debugString += "\n" + date + " " + tag + ": " + message;
			debugString += "\n    Exception thrown: " + tr.getMessage();
		}
		if (doToast) {
			toast(tag + ": " + message);
		}
	}

	public static int d(String tag, String msg) {
		final int i = Log.d(tag, msg);
		myLog("D." + tag, msg);
		return i;
	}

	public static int d(String tag, String msg, Throwable tr) {
		final int i = Log.d(tag, msg, tr);
		myLog("D." + tag, msg, tr);
		return i;
	}

	public static int e(String tag, String msg) {
		final int i = Log.e(tag, msg);
		myLog("E." + tag, msg);
		return i;
	}

	public static int e(String tag, String msg, Throwable tr) {
		final int i = Log.e(tag, msg, tr);
		myLog("E." + tag, msg, tr);
		return i;
	}

	public static int i(String tag, String msg) {
		final int i = Log.i(tag, msg);
		myLog("I." + tag, msg);
		return i;
	}

	public static int i(String tag, String msg, Throwable tr) {
		final int i = Log.i(tag, msg, tr);
		myLog("I." + tag, msg, tr);
		return i;
	}

	public static int v(String tag, String msg) {
		final int i = Log.v(tag, msg);
		myLog("V." + tag, msg);
		return i;
	}

	public static int v(String tag, String msg, Throwable tr) {
		final int i = Log.v(tag, msg, tr);
		myLog("V." + tag, msg);
		return i;
	}

	public static int w(String tag, Throwable tr) {
		final int i = Log.w(tag, tr);
		myLog("W." + tag, "", tr);
		return i;
	}

	public static int w(String tag, String msg) {
		final int i = Log.w(tag, msg);
		myLog("W." + tag, msg);
		return i;
	}

	public static int w(String tag, String msg, Throwable tr) {
		final int i = Log.w(tag, msg, tr);
		myLog("W." + tag, msg, tr);
		return i;
	}

	public static int wtf(String tag, Throwable tr) {
		final int i = Log.wtf(tag, tr);
		myLog("WTF." + tag, "");
		return i;
	}

	public static int wtf(String tag, String msg) {
		final int i = Log.wtf(tag, msg);
		myLog("WTF." + tag, msg);
		return i;
	}

	public static int wtf(String tag, String msg, Throwable tr) {
		final int i = Log.wtf(tag, msg, tr);
		myLog("WTF." + tag, msg, tr);
		return i;
	}

	public static String getStackTraceString(Throwable tr) {
		return Log.getStackTraceString(tr);
	}
}