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

interface onTicketsChanged {
	void onChanged();
}

public class Tickets extends TcObject {

	public static String url = null;
	public static String username = null;
	public static String password = null;
	public static String profile = null;
	public static boolean sslHack = false;
	public static boolean sslHostNameHack = false;

	public static ArrayList<Ticket> ticketList = null;
	public static ArrayList<SortSpec> sortList = null;
	public static ArrayList<FilterSpec> filterList = null;
	public static int tickets[] = null;

	private static Map<Integer, Ticket> ticketMap = null;

	private static Tickets _instance = null;
	private static String _tag = "";
	private static int ticketGroupCount;
	private static int ticketContentCount;
	
	private static onTicketsChanged saveTc = null;

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
		getInstance();
		tcLog.d(_tag, "Tickets initList");
		ticketList = new ArrayList<Ticket>();
		ticketContentCount = 0;
		valid = true;
		resetCache();
	}

	public static void clear() {
		getInstance();
		ticketList = null;
		tickets = null;
		ticketMap = null;
		ticketContentCount = 0;
		TicketModel.getInstance();
	}
	
	public static void setOnChanged(onTicketsChanged tc) {
		saveTc = tc;
	}
	
	public static void notifyChange() {
		if (saveTc != null) {
			saveTc.onChanged();
		}
	}
	

	public static Ticket getTicket(final int ticknr) {
		getInstance();
//		tcLog.d(_tag, "getTicket ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
		return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
	}

	public static void putTicket(Ticket ticket) {
		getInstance();
//		tcLog.d(getClass().getName(), "putTicket ticket = "+ticket);
		ticketMap.put(ticket.getTicketnr(), ticket);
	}
	
	public static void setTicketGroupCount(final int v) {
		getInstance();
		ticketGroupCount = v;
	}

	public static void setTicketContentCount(final int v) {
		getInstance();
		ticketContentCount = v;
	}
	
	public static void incTicketContentCount() {
		ticketContentCount++;
	}
	
	public static int getTicketGroupCount() {
		return ticketGroupCount;
	}

	public static int getTicketContentCount() {
		return ticketContentCount;
	}

	public static void resetCache() {
		getInstance();
		// tcLog.d(_tag,"resetCache voor ticketMap = "+ticketMap);
		ticketMap = new TreeMap<Integer, Ticket>();
		// tcLog.d(_tag,"resetCache na ticketMap = "+ticketMap);
	}

	public static void setInvalid() {
		getInstance();
		valid = false;
	}

	public static boolean isValid() {
		getInstance();
		return valid;
	}

	public static int getNextTicket(final int ticket) {
		return getNeighTicket(ticket, 1);
	}

	public static int getPrevTicket(final int ticket) {
		return getNeighTicket(ticket, -1);
	}

	private static int getNeighTicket(final int ticknr, final int dir) {
		getInstance();
		tcLog.d(_tag, "getNeighTicket ticknr = " + ticknr + ", dir = " + dir);
		Ticket t = Tickets.getTicket(ticknr);
		// tcLog.d(_tag, "t = " + t);
		if (t == null) {
			return -1;
		} else {
			final int pos = ticketList.indexOf(t);
			final int newpos = pos + dir;
			// tcLog.d(_tag,"pos = "+pos+", newpos = "+newpos+", count = "+ticketList.size());
			if (pos < 0 || newpos < 0 || newpos >= ticketList.size()) {
				return -1;
			} else {
				t = ticketList.get(newpos);
				tcLog.d(_tag, "new ticket = " + t);
				return t != null && t.hasdata() ? t.getTicketnr() : ticknr;
			}
		}
	}

	public static int getTicketCount() {
		getInstance();
		try {
			return ticketList.size();
		} catch (final Exception e) {
			tcLog.e(_tag, "getTicketCount Exception", e);
			return 0;
		}
	}

}
