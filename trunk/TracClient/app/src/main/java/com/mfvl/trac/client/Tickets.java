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
 */com.mfvl.trac.client;

import com.mfvl.mfvllib.MyLog;

import java.util.Map;
import java.util.TreeMap;

interface TicketListInterface {
    TicketList getTicketList();
}

class Tickets implements TicketListInterface {

    private static Map<Integer, Ticket> ticketMap = null;
    private TicketList ticketList = null;

    public Tickets() {
        MyLog.logCall();
        ticketList = new TicketList();
    }

    public static Ticket getTicket(final int ticknr) {
//        MyLog.d("ticketMap = "+ticketMap);
//        MyLog.d("ticknr = "+ticknr+ " "+ticketMap.containsKey(ticknr));
        return ticketMap.containsKey(ticknr) ? ticketMap.get(ticknr) : null;
    }

    public TicketList getTicketList() {
        return ticketList;
    }

    public void resetCache() {
        MyLog.logCall();
        // MyLog.d("voor: ticketMap = "+ticketMap);
        ticketMap = new TreeMap<>();
        // MyLog.d("na: ticketMap = "+ticketMap);
    }

    public int getTicketCount() {
        try {
            return ticketList.size();
        } catch (final Exception e) {
            MyLog.d("Exception", e);
            return 0;
        }
    }

    public void addTicket(Ticket ticket) {
//		MyLog.d("ticket = "+ticket);
        ticketList.add(ticket);
        putTicket(ticket);
    }

    public void putTicket(Ticket ticket) {
//        MyLog.d("ticketMap = "+ticketMap);
//        MyLog.d("ticket = "+ticket);
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
}
