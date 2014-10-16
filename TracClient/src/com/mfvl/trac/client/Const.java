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
	 * Constanten voor communicatie met de service
	 */
	static final int MSG_START_TIMER = 1;
	static final int MSG_REQUEST_TICKET_COUNT = 2;
	static final int MSG_SEND_TICKET_COUNT = 3;
	static final int MSG_REQUEST_NEW_TICKETS = 4;
	static final int MSG_SEND_NEW_TICKETS = 5;
	static final int MSG_REQUEST_REFRESH = 6;
	static final int MSG_STOP_TIMER = 7;
	static final int MSG_REMOVE_NOTIFICATION = 8;

	/*
	 * Constanten voor opslaan van variablen
	 */
	static final String CURRENT_URL = "currentURL";
	static final String CURRENT_USERNAME = "currentUsername";
	static final String CURRENT_PASSWORD = "currentPassword";
	static final String CURRENT_TICKET = "ticknr";
	static final String CURRENT_SSLHACK = "sslHack";
	static final String CURRENT_SSLHOSTNAMEHACK = "sslHostNameHack";
	
	static final String CURRENT_FILTER = "currentFilter";
	static final String CURRENT_PROFILE = "currentProfile";
	static final String CURRENT_SORTORDER = "currectSortOrder";

	static final String NEW_URL = "newURL";
	static final String NEW_USERNAME = "newUsername";
	static final String NEW_PASSWORD = "newPassword";

	static final String ADMOB = "Admob";
	static final String UPDATE_CURRENT_BUTTON = "currentButton";
	static final String UPDATE_SPIN_POSITION = "spinPosition";
	static final String UPDATE_OPTION_VAL = "optionVal";

	static final String INTENT_URL = "url";
	static final String INTENT_TICKET = "ticket";

	static final String HELP_FILE = "file";
	static final String HELP_VERSION = "version";

	static final String EMPTYFIELDS = "emptyfields";
	static final String MODVELD = "modveld";
	/*
	 * Shared preferences
	 */

	public static final String PREFS_NAME = "Trac";
	public final static String PREF_URL = "tracUrl";
	public final static String PREF_USER = "tracUsername";
	public final static String PREF_PASS = "tracPassword";
	public final static String PREF_HACK = "sslHack";
	public final static String PREF_HNH = "sslHostNameHack";
	public final static String PREF_PROF = "profile";
	public final static String PREF_1ST = "firstRun";
	public final static String PREF_FILTER = "filterstring";
	public final static String PREF_SORT = "sortString";

	/*
	 * Overige constanten
	 */
	static final int UPARROW = android.R.drawable.arrow_up_float;
	static final int DOWNARROW = android.R.drawable.arrow_down_float;
	final static String mimeUnknown = "application/unknown";

	public enum TrackerName {
		APP_TRACKER, // Tracker used only in this app.
		GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
		ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
	}
 
}
