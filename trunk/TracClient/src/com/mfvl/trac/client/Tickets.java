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
	private static String _tag = "";
	
	private static boolean valid = false;

	public static final int INVALID_URL = 1;
	public static final int LIST_NOT_LOADED = 2;
	public static final int CONTENT_NOT_LOADED = 3;
	

	private Tickets() {
		_tag = getClass().getName();
		tcLog.d(_tag, "Tickets create");
		valid = ticketList != null;
	}

	public static Tickets getInstance() {
		if (_instance == null) {
			_instance = new Tickets();
		}
		return _instance;
	}

	public static void initList() {
		ticketList = new ArrayList<Ticket>();
		valid = true;
		resetCache();
	}

	public static void clear() {
		ticketList = null;
		tickets = null;
		TicketModel.getInstance();
	}

	private static void loadTicketList() throws TicketLoadException {
	}

	private static void loadTicketContent(final onLoadTicketCompleteListener oc) throws TicketLoadException {
	}

	public static void load(final onLoadTicketCompleteListener oc) {
		if (LoginInfo.url == null) {
			tcLog.e(_tag, "URL == null");
			oc.onError(INVALID_URL);
		}
		clear();
		Thread loadThread = new Thread() {
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

	public static Ticket getTicket(int ticknr) {
		// tcLog.d(getClass().getName(), "getTicket ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
		return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
	}

	public static void putTicket(Ticket ticket) {
		// tcLog.d(getClass().getName(), "putTicket ticket = "+ticket);
		ticketMap.put(ticket.getTicketnr(), ticket);
	}

	public static void resetCache() {
		// tcLog.d(getClass().getName(),"resetCache voor ticketMap = "+ticketMap);
		ticketMap = new TreeMap<Integer, Ticket>();
		// tcLog.d(getClass().getName(),"resetCache na ticketMap = "+ticketMap);
	}

	public static void setInvalid() {
		valid = false;
	}

	public static boolean isValid() {
		return valid;
	}
}
