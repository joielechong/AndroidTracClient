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

public class Tickets {

    public ArrayList<Ticket> ticketList = null;

    private static Map<Integer, Ticket> ticketMap = null;

    private Tickets _instance = null;
    private String _tag = "";
    private int ticketGroupCount;
    private int ticketContentCount;
    private boolean valid = false;

    public Tickets() {
        _tag = getClass().getName();
        tcLog.d(_tag, "Tickets create");
		initList();
        valid = ticketList != null;
    }

    public void initList() {
        tcLog.d(_tag, "initList");
        ticketList = new ArrayList<Ticket>();
        ticketContentCount = 0;
        valid = true;
    }

    public void resetCache() {
        // tcLog.d(_tag,"resetCache voor ticketMap = "+ticketMap);
        ticketMap = new TreeMap<Integer, Ticket>();
        // tcLog.d(_tag,"resetCache na ticketMap = "+ticketMap);
    }

    public Ticket getTicket(final int ticknr) {
        // tcLog.d(_tag, "getTicket ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
        return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
    }

    public void putTicket(Ticket ticket) {
        // tcLog.d(getClass().getName(), "putTicket ticket = "+ticket);
        ticketMap.put(ticket.getTicketnr(), ticket);
    }
	
	public void addTicket(Ticket ticket) {
		ticketList.add(ticket);
		putTicket(ticket);
	}
	
	public void delTicket(int ticknr) {
		tcLog.d(getClass().getName(), "delTicket ticknr = "+ticknr);
		if (ticketMap.containsKey(ticknr)) {
			Ticket removed = ticketMap.remove(ticknr);
			ticketList.remove(removed);
			tcLog.d(getClass().getName(), "delTicket removed = "+removed);
		}
	}
	
    public void setTicketGroupCount(final int v) {
        ticketGroupCount = v;
    }

    public void setTicketContentCount(final int v) {
        ticketContentCount = v;
    }
	
    public void incTicketContentCount() {
        ticketContentCount++;
    }
	
    public int getTicketGroupCount() {
        return ticketGroupCount;
    }

    public int getTicketContentCount() {
        return ticketContentCount;
    }

    public void setInvalid() {
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }

    public int getNextTicket(final int ticket) {
        return getNeighTicket(ticket, 1);
    }

    public int getPrevTicket(final int ticket) {
        return getNeighTicket(ticket, -1);
    }

    private int getNeighTicket(final int ticknr, final int dir) {
        tcLog.d(_tag, "getNeighTicket ticknr = " + ticknr + ", dir = " + dir);
        Ticket t = getTicket(ticknr);

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

    public int getTicketCount() {
        try {
            return ticketList.size();
        } catch (final Exception e) {
//            tcLog.d(_tag, "getTicketCount Exception", e);
            return 0;
        }
    }

}
