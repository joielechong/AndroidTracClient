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

final class Const {

    static final public boolean DEBUG_MANAGERS = false;
    /*
     * Constanten voor opslaan van variablen
     */
    static final String CURRENT_URL = "currentURL";
    static final String CURRENT_USERNAME = "currentUsername";
    static final String CURRENT_PASSWORD = "currentPassword";
    static final String CURRENT_TICKET = "ticknr";
    static final String CURRENT_SSLHACK = "sslHack";
    static final String CURRENT_SSLHOSTNAMEHACK = "sslHostNameHack";
    static final String CURRENT_PROFILE = "profile";
    static final String ADMOB = "Admob";
    static final String FILTERLISTNAME = "filterlist";
    static final String SORTLISTNAME = "sortlist";
    static final String INTENT_URL = "url";
    static final String INTENT_CMD = "cmd";
    static final String INTENT_ARG1 = "arg1";
    static final String INTENT_ARG2 = "arg2";
    static final String INTENT_OBJ = "obj";
    static final String INTENT_TICKET = "ticket";
    static final String HELP_FILE = "file";
    static final String HELP_VERSION = "version";
    static final String HELP_ZOOM = "zoom";
    static final String HELP_COOKIES = "cookies";
    static final String PREFS_NAME = "Trac";
    static final String PREF_URL = "tracUrl";
    static final String PREF_USER = "tracUsername";
    static final String PREF_PASS = "tracPassword";
    static final String PREF_HACK = "sslHack";
    static final String PREF_HNH = "sslHostNameHack";
    static final String PREF_PROF = "profile";
    static final String PREF_1ST = "firstRun";

    /*
     * Messages
    */
    static final int MSG_START_TIMER = 1;
    static final int MSG_STOP_TIMER = 2;
    static final int MSG_REQUEST_TICKET_COUNT = 3;
    static final int MSG_SEND_TICKET_COUNT = 4;
    static final int MSG_SEND_TICKETS = 6;
    static final int MSG_REMOVE_NOTIFICATION = 7;
    static final int MSG_START_PROGRESSBAR = 21;
    static final int MSG_STOP_PROGRESSBAR = 22;
    static final int MSG_SET_SORT = 23;
    static final int MSG_SET_FILTER = 24;
    static final int MSG_SHOW_DIALOG = 25;
    static final int MSG_DISPLAY_TICKET = 26;
    static final int MSG_DATA_CHANGED = 27;
    static final int MSG_START_LISTLOADER = 28;
    static final int MSG_GET_PERMISSIONS = 29;
    static final int MSG_REFRESH_LIST = 30;
    static final int MSG_LOAD_TICKETS = 42;
    static final int MSG_LOAD_FASE1_FINISHED = 43;
    static final int MSG_LOAD_FASE2_FINISHED = 44;
    static final int MSG_LOAD_ABORTED = 46;
    static final int MSG_SET_TICKET_MODEL = 47;
    static final int MSG_PERFORM_LOGIN = 48;
    static final int MSG_DONE = 49;

    static final String DATABASE_NAME = "profile.db";
    static final String DB_UPDATED = "db_update";

    static final String PERFORM_LOGIN = "perform_login";
	static final String PERFORM_FILTER = "perform_filter";
	static final String PERFORM_SORT = "perform_sort";
    static final String BEWAREN = "bewaren";
}
