/*
 * Copyright (C) 2013-2015 Michiel van Loon 
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
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class tcLog {
    private static final SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private static Activity _c = null;
    private static boolean doToast = false;
    private static String debugString = "";

    public static void setContext(Activity c) {
        _c = c;
    }

    private static String getCaller(int index) {
        String retval;
        try {
            throw new Exception("debug");
        } catch (Exception e) {
            StackTraceElement[] s = e.getStackTrace();
            retval = s[index].getClassName() + "." + s[index].getMethodName();
        }
        return retval;
    }

    public static String getDebug() {
        return debugString;
    }

    public static void setToast(boolean value) {
        doToast = value;
    }

    private static void _toast(String string) {
        Toast.makeText(_c, string, Toast.LENGTH_SHORT).show();
    }

    public static void toast(final String string) {
        if (_c != null) {
            _c.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _toast(string);
                }
            });
        }
    }

    private static void myLog(final String tag, final String message) {
        myLog(tag, message, null);
    }

    private static void myLog(final String tag, final String message, Throwable tr) {
        final Date dt = new Date();
        int tid = android.os.Process.myTid();
        int pid = android.os.Process.myPid();

        dt.setTime(System.currentTimeMillis());
        final String date = s.format(new Date());

        debugString += "\n" + date + " " + pid + " " + tid + " " + tag
                + ("".equals(message) ? "" : ": " + message)
                + (tr != null ? "\nException thrown: " + tr.getMessage() + "\n" + getStackTraceString(tr) : "");
        if (doToast) {
            toast(tag + ": " + message);
        }
    }

    public static void logCall() {
        String caller = getCaller(2);
        Log.d(caller, "logCall");
        myLog("D." + caller, "");
    }

    public static void d(Object msg) {
        String caller = getCaller(2);
        Log.d(caller, msg.toString());
        myLog("D." + caller, msg.toString());
    }

    public static void d(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.d(caller, msg, tr);
        myLog("D." + caller, msg, tr);
    }

    public static void e(String msg) {
        String caller = getCaller(2);
        Log.e(caller, msg);
        myLog("E." + caller, msg);
    }

    public static void e(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.e(caller, msg, tr);
        myLog("E." + caller, msg, tr);
    }

    public static void e(Throwable tr) {
        String caller = getCaller(2);
        Log.w(caller, tr);
        myLog("E." + caller, "", tr);
    }

    public static void i(String msg) {
        String caller = getCaller(2);
        Log.i(caller, msg);
        myLog("I." + caller, msg);
    }

    public static void i(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.i(caller, msg, tr);
        myLog("I." + caller, msg, tr);
    }

    public static void v(String msg) {
        String caller = getCaller(2);
        Log.v(caller, msg);
        myLog("V." + caller, msg);
    }

    public static void v(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.v(caller, msg, tr);
        myLog("V." + caller, msg);
    }

    public static void w(Throwable tr) {
        String caller = getCaller(2);
        Log.w(caller, tr);
        myLog("W." + caller, "", tr);
    }

    public static void w(String msg) {
        String caller = getCaller(2);
        Log.w(caller, msg);
        myLog("W." + caller, msg);
    }

    public static void w(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.w(caller, msg, tr);
        myLog("W." + caller, msg, tr);
    }

    public static void wtf(Throwable tr) {
        String caller = getCaller(2);
        Log.wtf(caller, tr);
        myLog("WTF." + caller, "");
    }

    public static void wtf(String msg) {
        String caller = getCaller(2);
        Log.wtf(caller, msg);
        myLog("WTF." + caller, msg);
    }

    public static void wtf(String msg, Throwable tr) {
        String caller = getCaller(2);
        Log.wtf(caller, msg, tr);
        myLog("WTF." + caller, msg, tr);
    }

    private static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    public static void save() {
        String caller = getCaller(2);
        String logFilename = _c.getResources().getString(R.string.logfile);
        File file = null;
        try {
            file = TracGlobal.makeExtFilePath(logFilename, false);
            final OutputStream os = new FileOutputStream(file);

            os.write(getDebug().getBytes());
            os.close();
            Log.d(caller, "File saved  =  " + file);
            myLog("D." + caller, "File saved  =  " + file);
        } catch (final Exception e) {
            Log.e(caller, "Exception while saving logfile on " + file, e);
            myLog("E." + caller, "Exception while saving logfile on " + file, e);
        }
    }
}
