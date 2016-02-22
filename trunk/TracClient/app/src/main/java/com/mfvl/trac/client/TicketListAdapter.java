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

public class TicketListAdapter extends ColoredArrayAdapter<Ticket> {
    //private final Tickets mTickets;

    public TicketListAdapter(TracStart context, Tickets tl) {
        super(context, tl != null ? tl.ticketList : null);
        tcLog.logCall();
        //mTickets = tl;
    }

    public void clear() {
        tcLog.logCall();
        super.clear();
        //mTickets.clear();
        notifyDataSetChanged();
    }

    public void addAll(Tickets tl) {
        //tcLog.d("tl = " + tl + " " + (tl != null ? tl.ticketList.toString() : null));
        if (tl != null) {
            for (Ticket t:tl.ticketList) {
                super.add(t);
            }
            //mTickets.add(tl);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean hasStableIds() {
        //tcLog.logCall();
        return true;
    }

    public int getNextTicket(int ticknr) {
        Ticket t = Tickets.getTicket(ticknr);
        int retVal = -1;
        if (t != null) {
            int pos = super.getPosition(t);
            if (pos >= 0) {
                try {
                    t = super.getItem(pos + 1);
                    retVal = t.getTicketnr();
                } catch (IndexOutOfBoundsException ignored) {
                    //  retVal remains -1
                }
            }
        }
        return retVal;
    }

    public int getPrevTicket(int ticknr) {
        Ticket t = Tickets.getTicket(ticknr);
        int retVal = -1;
        if (t != null) {
            int pos = super.getPosition(t);
            if (pos > 0) {
                try {
                    t = super.getItem(pos - 1);
                    retVal = t.getTicketnr();
                } catch (IndexOutOfBoundsException ignored) {
                    //  retVal remains -1
                }
            }
        }
        return retVal;
    }

    public TicketList getTicketList() {
        TicketList tl = new TicketList();
        for (int i = 0;i<getCount();i++) {
            tl.add(getItem(i));
        }
        return tl;
    }

    public int getTicketContentCount() {
        int count = 0;
        for (int i = 0;i<getCount();i++) {
            if (getItem(i).hasdata()) {
                count++;
            }
        }
        tcLog.d("count = "+count);
        return count;
    }

    public int getCount() {
        int count = super.getCount();
        tcLog.d("count = "+count);
        return count;
    }
}
