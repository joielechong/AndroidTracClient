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


public final class Const {
	
    /*
     * Constanten voor opslaan van variablen
     */
    static final String CURRENT_URL = "currentURL";
    static final String CURRENT_USERNAME = "currentUsername";
    static final String CURRENT_PASSWORD = "currentPassword";
    static final String CURRENT_TICKET = "ticknr";
    static final String CURRENT_SSLHACK = "sslHack";
    static final String CURRENT_SSLHOSTNAMEHACK = "sslHostNameHack";

    static final String ADMOB = "Admob";

    static final String INTENT_URL = "url";
    static final String INTENT_TICKET = "ticket";

    static final String HELP_FILE = "file";
    static final String HELP_VERSION = "version";

    /*
     * Shared preferences
     */

    static final String PREFS_NAME = "Trac";
    static final String PREF_URL = "tracUrl";
    static final String PREF_USER = "tracUsername";
    static final String PREF_PASS = "tracPassword";
    static final String PREF_HACK = "sslHack";
    static final String PREF_HNH = "sslHostNameHack";
    static final String PREF_PROF = "profile";
    static final String PREF_1ST = "firstRun";
    static final String PREF_FILTER = "filterstring";
    static final String PREF_SORT = "sortString";
    static final String PREF_DISCLAIM = "disclaimer";
    static final String PREF_COOKIEINFORM = "CookieInform";
	
	static final String DisclaimerVersion = "1.0";

    /*
     * Overige constanten
     */
    static final String mimeUnknown = "application/unknown";
	
    static final boolean doAnalytics = true;
    static public int ticketGroupCount = 50;
}
