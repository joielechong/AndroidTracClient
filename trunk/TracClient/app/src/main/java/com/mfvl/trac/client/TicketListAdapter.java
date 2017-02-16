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
 */

package com.mfvl.trac.client;

import android.content.Context;

import com.mfvl.mfvllib.MyLog;

import java.util.Collection;

class TicketListAdapter extends ColoredArrayAdapter<Ticket> {

    TicketListAdapter(Context context, Tickets tl) {
        super(context, tl != null ? tl.getTicketList() : null);
        MyLog.logCall();
        setNotifyOnChange(true);
    }

    public void addAll(Tickets tl) {

        //MyLog.d("tl = " + tl + " " + (tl != null ? tl.ticketList.toString() : null));
        super.addAll(tl.getTicketList());
    }

    @Override
    public boolean hasStableIds() {
        //MyLog.logCall();
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

    public Iterable<Ticket> getTicketList() {
        Collection<Ticket> tl = new TicketList();
        for (int i = 0; i < getCount(); i++) {
            tl.add(getItem(i));
        }
        return tl;
    }

    public int getTicketContentCount() {
        int count = 0;
        for (int i = 0; i < getCount(); i++) {
            if ((getItem(i) != null) && getItem(i).hasdata()) {
                count++;
            }
        }
        //MyLog.d("count = " + count);
        return count;
    }
}
