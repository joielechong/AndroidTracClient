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

import android.database.Cursor;
import android.database.CursorWrapper;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

public class TicketListAdapter extends SimpleCursorAdapter /* implements OnTicketsChangeListener */ {
    private CursorWrapper cursor;
    public static String[] fields = new String[] { TicketCursor.STR_FIELD_TICKET};
    public static int[] adapres = new int[] { R.id.ticket_list};
	private Tickets ticketList = null;
    Context context;

    public TicketListAdapter(TracStart context, int resource, TicketCursor c) {
        super(context, resource, c, fields, adapres, 0);
        tcLog.d(getClass().getName(), "TicketListAdapter construction " + c);
        this.context = context;

		cursor = (c == null ? null : new CursorWrapper(c));

		try {
			ticketList = c.getTicketList();
		} catch (Exception e) {
			ticketList = null;
		}
    }
	
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //tcLog.d(getClass().getName(), "getView position = " + position + " "+ convertView + " "+ parent);
        return ColoredLines.getView(context, super.getView(position, convertView, parent), position, convertView, parent);
    }
	
	@Override
	public Cursor getCursor() {
		Cursor c = super.getCursor();
        return (c instanceof CursorWrapper ?((CursorWrapper)c).getWrappedCursor() : c);
	  }

    public void changeCursor(Cursor c) {
        tcLog.d(getClass().getName(), "changeCursor " + c);
        // super.changeCursorAndColumns(c,fields,adapres);
        super.changeCursor(c);
        cursor = (c instanceof CursorWrapper ? (CursorWrapper) c : new CursorWrapper(c));
		setTicketList(c);
    }

	private void setTicketList(Cursor c) {
		Tickets oldTicketList = ticketList;
		if (c == null) {
			ticketList = null;
		} else {
			Cursor cursor = c;
			if (c instanceof CursorWrapper) {
				cursor = ((CursorWrapper)c).getWrappedCursor();
			}
			if (!(cursor instanceof TicketCursor)) {
			} else {
				ticketList = ((TicketCursor)cursor).getTicketList();
				if (ticketList == null || !ticketList.equals(oldTicketList)) {
				}
			}
		}
	}
	
	@Override
    public Cursor swapCursor(Cursor c) {
        Cursor c1 = super.swapCursor(c);

        tcLog.d(getClass().getName(), "swapCursor new = " + c + " old = " + c1);
		setTicketList(c);
        return c1;
    }
		
    @Override
    public boolean hasStableIds() {
//        tcLog.d(getClass().getName(), "hasStableIds");
        return true;
    }
		
    @Override
    public Object getItem(int position) {
        tcLog.d(getClass().getName(), "getItem " + position);
		try {
			Object o = ticketList.ticketList.get(position);
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
}
