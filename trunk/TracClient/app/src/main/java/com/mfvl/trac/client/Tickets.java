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

import java.util.Map;
import java.util.TreeMap;

public class Tickets {

    private static Map<Integer, Ticket> ticketMap = null;
    public TicketList ticketList = null;

    public Tickets() {
        tcLog.d("create");
        initList();
    }

    public void initList() {
        tcLog.logCall();
        ticketList = new TicketList();
    }

    public void resetCache() {
        tcLog.logCall();
        // tcLog.d("voor: ticketMap = "+ticketMap);
        ticketMap = new TreeMap<>();
        // tcLog.d("na: ticketMap = "+ticketMap);
    }

    public void clear() {
        tcLog.logCall();
        ticketList.clear();
    }

    public void add(Tickets tl) {
        tcLog.d("this = " + this + " size voor = " + getTicketCount() + " tl = " + tl);
        this.ticketList.addAll(tl.ticketList);
//		for(Ticket t: tl.ticketList) {
//			addTicket(t);
//		}
        tcLog.d("size na = " + getTicketCount());
    }

    public int getTicketCount() {
        try {
            return ticketList.size();
        } catch (final Exception e) {
            tcLog.d("Exception", e);
            return 0;
        }
    }

    public void addTicket(Ticket ticket) {
//		tcLog.d("ticket = "+ticket);
        ticketList.add(ticket);
        putTicket(ticket);
    }

    public void putTicket(Ticket ticket) {
//        tcLog.d("ticketMap = "+ticketMap);
//        tcLog.d("ticket = "+ticket);
        ticketMap.put(ticket.getTicketnr(), ticket);
    }

    public int getTicketContentCount() {
        int c = 0;
        for (Ticket t : ticketList) {
            if (t.hasdata()) {
                c = c + 1;
            }
        }
        return c;
    }

    public int getNextTicket(final int ticket) {
        return getNeighTicket(ticket, 1);
    }

    private int getNeighTicket(final int ticknr, final int dir) {
        tcLog.d("ticknr = " + ticknr + ", dir = " + dir);
        Ticket t = getTicket(ticknr);

        // tcLog.d("t = " + t);
        if (t == null) {
            return -1;
        } else {
            final int pos = ticketList.indexOf(t);
            final int newpos = pos + dir;

            // tcLog.d("pos = "+pos+", newpos = "+newpos+", count = "+ticketList.size());
            if (pos < 0 || newpos < 0 || newpos >= ticketList.size()) {
                return -1;
            } else {
                t = ticketList.get(newpos);
                tcLog.d("new ticket = " + t);
                return t != null && t.hasdata() ? t.getTicketnr() : ticknr;
            }
        }
    }

    public Ticket getTicket(final int ticknr) {
//        tcLog.d("ticketMap = "+ticketMap);
//        tcLog.d("ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
        return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
    }

    public int getPrevTicket(final int ticket) {
        return getNeighTicket(ticket, -1);
    }
}
