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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.lang.StackTraceElement;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class tcLog {
    private static Activity _c = null;
    private static boolean doToast = false;
    private static boolean doBuffer = true;
    private static String debugString = "";
    private static SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    public static void setContext(Activity c) {
        _c = c;
    }

	private static String getCaller(int index) {
		String retval = "No trace";
		try {
			throw new Exception("debug");
		} catch (Exception e) {
//			tcLog.d(getClass().getName(),"Debug trace:\n"+Arrays.asList(e.getStackTrace()));
			StackTraceElement[] s = e.getStackTrace();
			retval = s[index].getClassName()+"."+s[index].getMethodName();
		}
		return retval;
    }



    public static String getDebug() {
        return debugString;
    }
	
    public static void setToast(boolean value) {
	doToast = value;
    }
    
    private static void _toast(Activity c, String string) {
        Toast.makeText(c, string, Toast.LENGTH_SHORT).show();
    }

    public static void toast(final String string) {
        if (_c != null) {
            _c.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _toast(_c, string);
                }
            });
        }
    }

    private static void myLog(final String tag, final String message) {
        if (doBuffer) {
            final Date d = new Date();
			int tid = android.os.Process.myTid();
			int pid = android.os.Process.myPid();

            d.setTime(System.currentTimeMillis());
            final String date = s.format(new Date());

            debugString += "\n" + date + " " +pid +" "+tid+" "+ tag + ": " + message;
        }
        if (doToast) {
            toast(tag + ": " + message);
        }
    }

    private static void myLog(final String tag, final String message, Throwable tr) {
        if (doBuffer) {
            final Date dt = new Date();
			int tid = android.os.Process.myTid();
			int pid = android.os.Process.myPid();

            dt.setTime(System.currentTimeMillis());
            final String date = s.format(new Date());

            debugString += "\n" + date + " " +pid +" "+tid + " " + tag + ": " + message;
            debugString += "\nException thrown: " + tr.getMessage();
            debugString += "\n" + getStackTraceString(tr);
        }
        if (doToast) {
            toast(tag + ": " + message);
        }
    }

    public static int d(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.d(caller, msg);

        myLog("D." + caller, msg);
        return i;
    }

    public static int d(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.d(caller, msg, tr);

        myLog("D." + caller, msg, tr);
        return i;
    }

    public static int e(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.e(caller, msg);

        myLog("E." + caller, msg);
        return i;
    }

    public static int e(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.e(caller, msg, tr);

        myLog("E." + caller, msg, tr);
        return i;
    }

    public static int e(String tag, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.w(caller, tr);

        myLog("E." + caller, "", tr);
        return i;
    }

    public static int i(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.i(caller, msg);

        myLog("I." + caller, msg);
        return i;
    }

    public static int i(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.i(caller, msg, tr);

        myLog("I." + caller, msg, tr);
        return i;
    }

    public static int v(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.v(caller, msg);

        myLog("V." + caller, msg);
        return i;
    }

    public static int v(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.v(caller, msg, tr);

        myLog("V." + caller, msg);
        return i;
    }

    public static int w(String tag, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.w(caller, tr);

        myLog("W." + caller, "", tr);
        return i;
    }

    public static int w(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.w(caller, msg);

        myLog("W." + caller, msg);
        return i;
    }

    public static int w(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.w(caller, msg, tr);

        myLog("W." + caller, msg, tr);
        return i;
    }

    public static int wtf(String tag, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.wtf(caller, tr);

        myLog("WTF." + caller, "");
        return i;
    }

    public static int wtf(String tag, String msg) {
		String caller = getCaller(2);
        final int i = Log.wtf(caller, msg);

        myLog("WTF." + caller, msg);
        return i;
    }

    public static int wtf(String tag, String msg, Throwable tr) {
		String caller = getCaller(2);
        final int i = Log.wtf(caller, msg, tr);

        myLog("WTF." + caller, msg, tr);
        return i;
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

	public static void save(String tag) {
		String caller = getCaller(2);
		File file = null;
		try {
			file = Credentials.makeExtFilePath("tc-log.txt");
			final OutputStream os = new FileOutputStream(file);

			os.write(tcLog.getDebug().getBytes());
			os.close();
			Log.d(tag, "File saved  =  " + file);
			myLog("D."+tag, "File saved  =  " + file);
		} catch (final Exception e) {
			Log.e(caller, "Exception while saving logfile on " + file, e);
			myLog("E."+caller, "Exception while saving logfile on " + file, e);
		}
	}
}
