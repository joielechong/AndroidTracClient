/*
 * Copyright (C) 2014 Michiel van Loon
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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.SortSpec;
import com.mfvl.trac.client.util.tcLog;

interface onLoadTicketCompleteListener {
	void onListComplete(int count);

	void onContentLoaded(int progress);

	void onContentComplete(int count);

	void onError(int code);
}

public class Tickets {

	public static ArrayList<Ticket> ticketList = null;
	public static ArrayList<SortSpec> sortList = null;
	public static ArrayList<FilterSpec> filterList = null;
	public static int tickets[] = null;

	private static Map<Integer, Ticket> ticketMap = null;

	private static Tickets _instance = null;
	private static Credentials cred = null;
	private static TicketModel tm = null;

	private static Thread loadThread = null;

	private static String _url = null;
	private static String _username = null;
	private static String _password = null;
	private static Boolean _sslHack = false;
	private static Boolean _sslHostNameHack = false;

	public static final int INVALID_URL = 1;
	public static final int LIST_NOT_LOADED = 2;
	public static final int CONTENT_NOT_LOADED = 3;

	private Tickets() {
		tcLog.d(this.getClass().getName(), "Tickets create");
		// cred = Credentials.getInstance();
		// tm = TicketModel.getInstance();
	}

	public static Tickets getInstance() {
		if (_instance == null) {
			_instance = new Tickets();
		}
		return _instance;
	}

	public void clear() {
		ticketList = null;
		tickets = null;
		tm = TicketModel.getInstance();
		cred = Credentials.getInstance();
	}

	private boolean equals(Object s1, Object s2) {
		if (s1 == null && s2 == null) {
			return true;
		}
		if (s1 == null && s2 != null) {
			return false;
		}
		if (s1 != null && s2 == null) {
			return false;
		}
		return s1.equals(s2);
	}

	public void setHost(final String url, final String username, final String password, boolean sslHack, boolean sslHostNameHack) {
		tcLog.d(this.getClass().getName(), "setHost");

		boolean validList = _url != null;

		try {
			validList &= equals(url, _url) && equals(username, _username) && equals(password, _password)
					&& equals(sslHack, _sslHack) && equals(sslHostNameHack, _sslHostNameHack);
		} catch (final Exception e) {
			tcLog.e(this.getClass().getName(), "setHost validList", e);
			validList = false;
		}

		_url = url;
		_username = username;
		_password = password;
		_sslHack = sslHack;
		_sslHostNameHack = sslHostNameHack;

		if (!validList) {
			// clear();
		}
	}

	private void loadTicketList() throws TicketLoadException {
	}

	private void loadTicketContent(final onLoadTicketCompleteListener oc) throws TicketLoadException {
	}

	public void load(final onLoadTicketCompleteListener oc) {
		if (_url == null) {
			tcLog.e(getClass().getName(), "URL == null");
			oc.onError(INVALID_URL);
		}
		clear();
		loadThread = new Thread() {
			@Override
			public void run() {
				try {
					loadTicketList();
					oc.onListComplete(0);
				} catch (final TicketLoadException e) {
					tcLog.e(getClass().getName(), "load problem loading list", e);
					tickets = null;
					ticketList = null;
					oc.onError(LIST_NOT_LOADED);
				}
				try {
					loadTicketContent(oc);
					oc.onContentComplete(0);
				} catch (final TicketLoadException e) {
					tcLog.e(getClass().getName(), "load problem loading content", e);
					tickets = null;
					ticketList = null;
					oc.onError(CONTENT_NOT_LOADED);
				}
			}
		};
		loadThread.start();
	}

	public Ticket getTicket(int ticknr) {
		// tcLog.d(getClass().getName(), "getTicket ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
		return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
	}

	public void putTicket(Ticket ticket) {
		// tcLog.d(getClass().getName(), "putTicket ticket = "+ticket);
		ticketMap.put(ticket.getTicketnr(), ticket);
	}

	public void resetCache() {
		// tcLog.d(getClass().getName(),"resetCache voor ticketMap = "+ticketMap);
		ticketMap = new TreeMap<Integer, Ticket>();
		// tcLog.d(getClass().getName(),"resetCache na ticketMap = "+ticketMap);
	}

}
