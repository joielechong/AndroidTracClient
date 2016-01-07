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
    final Tickets mTickets;

    public TicketListAdapter(TracStart context, int resource, Tickets tl) {
        super(context, resource, tl != null ? tl.ticketList : null);
        tcLog.logCall();
//		tcLog.d("context = "+context+" resource = "+ resource+" tl = "+tl);
//		tcLog.d(tl != null ? tl.toString() : null);

        mTickets = tl;
    }

    public void clear() {
        tcLog.logCall();
        super.clear();
        mTickets.clear();
        notifyDataSetChanged();
    }

    public void addAll(Tickets tl) {
        tcLog.d("tl = " + tl + " " + (tl != null ? tl.ticketList.toString() : null));
//		tcLog.d("mTickets = "+mTickets);
        if (tl != null) {
            super.addAll(tl.ticketList);
            mTickets.add(tl);
//			tcLog.d("count = "+super.getCount());
            notifyDataSetChanged();
        }
    }

    public TicketList getTicketList() {
        return mTickets.ticketList;
    }

    @Override
    public boolean hasStableIds() {
//        tcLog.d( "hasStableIds");
        return true;
    }

    @Override
    public Ticket getItem(int position) {
//        tcLog.d( "getItem " + position);
        try {
            //			tcLog.d( "getItem o = " + o);
            return mTickets.ticketList.get(position);
        } catch (Exception e) {
            return null;
        }
    }

    public int getNextTicket(int pos) {
        return mTickets.getNextTicket(pos);
    }

    public int getPrevTicket(int pos) {
        return mTickets.getPrevTicket(pos);
    }

    public Ticket getTicket(int i) {
        tcLog.d("mTickets = " + mTickets);
        tcLog.d("getTicket i = " + i);
        return (mTickets != null ? mTickets.getTicket(i) : null);
    }

    public int getCount() {
        return mTickets.getTicketCount();
    }

    public int getTicketContentCount() {
        return mTickets.getTicketContentCount();
    }
}