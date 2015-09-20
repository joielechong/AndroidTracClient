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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;

public class TicketListAdapter extends ColoredArrayAdapter<Ticket> /* implements OnTicketsChangeListener */ {
    public static String[] fields = new String[] { TicketCursor.STR_FIELD_TICKET};
    public static int[] adapres = new int[] { R.id.ticket_list};
    Context context;
	final Tickets ticketList;

    public TicketListAdapter(TracStart context, int resource, Tickets tl) {
        super(context, resource,(tl==null?null:tl.ticketList));
        tcLog.d(getClass().getName(), "TicketListAdapter construction " + tl);
        this.context = context;

		ticketList = tl;
    }
	
	public TicketList getTicketList() {
		return ticketList.ticketList;
	}
	
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //tcLog.d(getClass().getName(), "getView position = " + position + " "+ convertView + " "+ parent);
        return ColoredLines.getView(context, super.getView(position, convertView, parent), position, convertView, parent);
    }
	
    @Override
    public boolean hasStableIds() {
//        tcLog.d(getClass().getName(), "hasStableIds");
        return true;
    }
		
    @Override
    public Ticket getItem(int position) {
        tcLog.d(getClass().getName(), "getItem " + position);
		try {
			Ticket o = ticketList.ticketList.get(position);
			tcLog.d(getClass().getName(), "getItem o = " + o);
			return o;
		} catch (Exception e) {
			return null;
		}
    }
	
	public int getNextTicket(int pos) {
		return ticketList.getNextTicket(pos);
	}

	public int getPrevTicket(int pos) {
		return ticketList.getPrevTicket(pos);
	}

	public Ticket getTicket(int i) {
        tcLog.d(getClass().getName(), "getTicket i = " + i);
		return (ticketList!= null?ticketList.getTicket(i):null);
	}
	
	public int getTicketContentCount() {
		return ticketList.getTicketContentCount();
	}
}