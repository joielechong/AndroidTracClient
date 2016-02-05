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
    private final Tickets mTickets;

    public TicketListAdapter(TracStart context, int resource, Tickets tl) {
        super(context, resource, tl != null ? tl.ticketList : null);
        tcLog.logCall();
        mTickets = tl;
    }

    public void clear() {
        tcLog.logCall();
        super.clear();
        mTickets.clear();
        notifyDataSetChanged();
    }

    public int getCount() {
        return mTickets.getTicketCount();
    }

    @Override
    public Ticket getItem(int position) {
//        tcLog.d( "getItem " + position);
        try {
            return mTickets.ticketList.get(position);
        } catch (Exception e) {
            return null;
        }
    }

    public void addAll(Tickets tl) {
        tcLog.d("tl = " + tl + " " + (tl != null ? tl.ticketList.toString() : null));
        if (tl != null) {
            super.addAll(tl.ticketList);
            mTickets.add(tl);
            notifyDataSetChanged();
        }
    }

    public TicketList getTicketList() {
        return mTickets.ticketList;
    }

    @Override
    public boolean hasStableIds() {
//        tcLog.logCall();
        return true;
    }

    public int getNextTicket(int pos) {
        return mTickets.getNextTicket(pos);
    }

    public int getPrevTicket(int pos) {
        return mTickets.getPrevTicket(pos);
    }

    public Ticket getTicket(int i) {
        tcLog.d("getTicket i = " + i + " mTickets = " + mTickets);
        return (mTickets != null ? mTickets.getTicket(i) : null);
    }

    public int getTicketContentCount() {
        return mTickets.getTicketContentCount();
    }
}