package com.mfvl.trac.client.util;

import java.lang.Throwable;
import android.widget.Toast;
import android.os.Looper;
import java.text.SimpleDateFormat;
import android.app.Activity;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

public class tcLog {
	private static Activity _c = null;
	private static boolean doToast = false;
	private static boolean doBuffer = true;
	private static String debugString="";
	private static SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);

	public static void setContext(Activity c) {
		_c = c;
	}

	public static String getDebug() {
		return debugString;
	}
	
	private static void _toast(Activity c,String string) {
		Toast.makeText(c, string, Toast.LENGTH_SHORT).show();
	}

	public static void toast(final String string) {
		if (_c != null) {
			if (Looper.getMainLooper().equals(Looper.myLooper())) {
				_toast(_c,string);
			} else {
				_c.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						_toast(_c,string);
					}
				});
			}
		} 
	}
	
	private static void myLog(final String tag, final String message) {
		if (doBuffer) {
			Date d =new Date();
			d.setTime(System.currentTimeMillis());
            String date = s.format(new Date());
            debugString += "\n"+date+" "+tag + ": " + message;
		}
		if (doToast) {
			toast(tag + ": " + message);
		}
	} 
	
	public static int d (String tag, String msg) {
		int i = Log.d(tag,msg);
		myLog("D."+tag,msg);
		return i;
	}
	
	public static int d (String tag, String msg, Throwable tr) {
		int i = Log.d(tag,msg,tr);
		myLog("D."+tag,msg);
		return i;
	}
	public static int e (String tag, String msg) {
		int i = Log.e(tag,msg);
		myLog("E."+tag,msg);
		return i;
	}
	
	public static int e (String tag, String msg, Throwable tr) {
		int i = Log.e(tag,msg,tr);
		myLog("E."+tag,msg);
		return i;
	}

	public static int i (String tag, String msg) {
		int i = Log.i(tag,msg);
		myLog("I."+tag,msg);
		return i;
	}
	
	public static int i (String tag, String msg, Throwable tr) {
		int i = Log.i(tag,msg,tr);
		myLog("I."+tag,msg);
		return i;
	}

	public static int v (String tag, String msg) {
		int i = Log.v(tag,msg);
		myLog("V."+tag,msg);
		return i;
	}
	
	public static int v (String tag, String msg, Throwable tr) {
		int i = Log.v(tag,msg,tr);
		myLog("V."+tag,msg);
		return i;
	}

	public static int w (String tag, Throwable tr) {
		int i = Log.w(tag,tr);
		myLog("W."+tag,"");
		return i;
	}

	public static int w (String tag, String msg) {
		int i = Log.w(tag,msg);
		myLog("W."+tag,msg);
		return i;
	}
	
	public static int w (String tag, String msg, Throwable tr) {
		int i = Log.w(tag,msg,tr);
		myLog("W."+tag,msg);
		return i;
	}	
	
	public static int wtf (String tag, Throwable tr) {
		int i = Log.wtf(tag,tr);
		myLog("WTF."+tag,"");
		return i;
	}

	public static int wtf (String tag, String msg) {
		int i = Log.wtf(tag,msg);
		myLog("WTF."+tag,msg);
		return i;
	}
	
	public static int wtf (String tag, String msg, Throwable tr) {
		int i = Log.wtf(tag,msg,tr);
		myLog("WTF."+tag,msg);
		return i;
	}	
	
	public static String getStackTraceString(Throwable tr) {
		return Log.getStackTraceString(tr);
	}
}