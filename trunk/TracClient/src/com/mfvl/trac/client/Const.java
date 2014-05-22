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

	/*
 * 
 */
	static final int UPARROW = android.R.drawable.arrow_up_float;
	static final int DOWNARROW = android.R.drawable.arrow_down_float;

}
