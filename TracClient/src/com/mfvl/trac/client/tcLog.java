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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
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
            final Date d = new Date();
			int tid = android.os.Process.myTid();
			int pid = android.os.Process.myPid();

            d.setTime(System.currentTimeMillis());
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

    public static int e(String tag, Throwable tr) {
        final int i = Log.w(tag, tr);

        myLog("E." + tag, "", tr);
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

	public static void save(String tag) {
		File path = null;
		File file = null;
		try {
			path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			path.mkdirs();

			file = new File(path, "tc-log.txt");
			final OutputStream os = new FileOutputStream(file);

			os.write(tcLog.getDebug().getBytes());
			os.close();
			d(tag, "File saved  =  " + file);
		} catch (final Exception e) {
			tcLog.e(tag, "Exception while saving logfile on " + path + " " + file, e);
		}
	}
}
