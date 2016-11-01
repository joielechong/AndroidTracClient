/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.mfvl.mfvllib.MyLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.mfvl.trac.client.Const.*;

final class TracGlobal {
    public static int webzoom = 0;
    static int ticketGroupCount = 0;
    static int timerCorr = 0;
    static int timerStart = 0;
    static int timerPeriod = 0;
    static int large_move = 0;
    static int[] adapterColors = null;
    static String prefFilterKey = "filterstring";
    static String prefSortKey = "sortString";

    private static String versie = null;
    private static String _url = "";
    private static String _username = "";
    private static String _password = "";
    private static boolean _sslHack = false;
    private static boolean _sslHostNameHack = false;
    private static String _profile = null;
    private static SharedPreferences settings = null;
    private static boolean canWriteSD = false;
    private static boolean started = false;
    private static String serviceAction = null;

    static void initialize(final Context context) {
        initialize(context, false);
    }

    static void initialize(final Context context, boolean force) {
        MyLog.d("context = " + context + " force = " + force);
        if (force || !started) {
            Resources res = context.getResources();
            settings = context.getSharedPreferences(PREFS_NAME, 0);
            //MyLog.d(settings.getAll());
            versie = res.getString(R.string.app_version);
            MyLog.i("Started TracClient version " + versie);

            try {
                ticketGroupCount = Integer.parseInt(settings.getString(res.getString(R.string.prefNrItemsKey), "-1"));
            } catch (Exception e) {
                ticketGroupCount = -1;
            }
            if (ticketGroupCount == -1) {
                ticketGroupCount = res.getInteger(R.integer.ticketGroupCount);
            }
            //MyLog.d("final: "+ticketGroupCount);
            serviceAction = res.getString(R.string.serviceAction);

            webzoom = res.getInteger(R.integer.webzoom);
            timerCorr = res.getInteger(R.integer.timerCorr);
            large_move = res.getInteger(R.integer.large_move);
            timerStart = res.getInteger(R.integer.timerStart);
            timerPeriod = res.getInteger(R.integer.timerPeriod);
            prefFilterKey = res.getString(R.string.prefFilterKey);
            prefSortKey = res.getString(R.string.prefSortKey);
            adapterColors = res.getIntArray(R.array.list_col);

            _url = settings.getString(PREF_URL, "");
            _username = settings.getString(PREF_USER, "");
            _password = settings.getString(PREF_PASS, "");
            _sslHack = settings.getBoolean(PREF_HACK, false);
            _sslHostNameHack = settings.getBoolean(PREF_HNH, false);
            _profile = settings.getString(PREF_PROF, null);
            started = true;
        }
    }

    static boolean getCanWriteSD() {
        return canWriteSD;
    }

    static void setCanWriteSD(boolean _canWriteSD) {
        TracGlobal.canWriteSD = _canWriteSD;
    }

    static String getServiceAction() {
        return serviceAction;
    }

    static SharedPreferences getSharedPreferences() {
        return settings;
    }

    /**
     * Store login credentials to shared preferences: server-url, username, password and profile
     */
    static void storeCredentials() {
//        MyLog.logCall();
        settings.edit()
                .putString(PREF_URL, _url)
                .putString(PREF_USER, _username)
                .putString(PREF_PASS, _password)
                .putBoolean(PREF_HACK, _sslHack)
                .putBoolean(PREF_HNH, _sslHostNameHack)
                .putString(PREF_PROF, _profile)
                .apply();
    }

    /**
     * Set login credentials server-url, username, password and profile
     */
    static void setCredentials(final String url, final String username, final String password, final String profile) {
//        MyLog.logCall();
        _url = url;
        _username = username;
        _password = password;
        _profile = profile;
    }

    public static String getUrl() {
        return _url;
    }

    public static String getUsername() {
        return _username;
    }

    public static String getPassword() {
        return _password;
    }

    public static boolean getSslHack() {
        return _sslHack;
    }

    public static void setSslHack(boolean sslHack) {
        _sslHack = sslHack;
    }

    static boolean getSslHostNameHack() {
        return _sslHostNameHack;
    }

    static void setSslHostNameHack(boolean sslHostNameHack) {
        _sslHostNameHack = sslHostNameHack;
    }

    public static String getProfile() {
        return _profile;
    }

    static boolean isFirstRun() {
        // MyLog.d("isFirstRun");
        final String thisRun = versie;
        final String lastRun = settings.getString(PREF_1ST, "");
        settings.edit().putString(PREF_1ST, thisRun).apply();
        return !lastRun.equals(thisRun);
    }

    static String getFilterString() {
        MyLog.logCall();
        return settings.getString(prefFilterKey, "max=500&status!=closed");
    }

    static void storeFilterString(final String filterString) {
        MyLog.d(filterString);
        settings.edit().putString(prefFilterKey, filterString == null ? "" : filterString).apply();
    }

    static String getSortString() {
        // MyLog.logCall();
        final String sortString = settings.getString(prefSortKey,
                "order=priority&order=modified&desc=1");

        MyLog.d("sortString = " + sortString);
        return sortString;
    }

    static void storeSortString(final String sortString) {
        //MyLog.d(sortString);
        settings.edit().putString(prefSortKey, sortString == null ? "" : sortString).apply();
    }

    static boolean isRCVersion() {
        return (versie != null) && (versie.toLowerCase(Locale.US).contains("rc"));
    }

    /**
     * Transform ISO 8601 string to Calendar.
     */
    static Calendar toCalendar(final String iso8601string) throws ParseException {
        final Calendar calendar = Calendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");

        try {
            s = s.substring(0, 22) + s.substring(23);
        } catch (final IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }
        final Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(s);

        calendar.setTime(date);
        return calendar;
    }
}
