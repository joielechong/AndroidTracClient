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

public class TicketListAdapter extends ColoredArrayAdapter<Ticket>  {
    Context context;
	final Tickets mTickets;

    public TicketListAdapter(TracStart context, int resource, Tickets tl) {
        super(context, resource,tl!=null?tl.ticketList:null);
        tcLog.d(tl != null ? tl.toString() : null);
        this.context = context;

		mTickets = tl;
    }
	
//	public View getView(int position, View convertView, ViewGroup parent) {
//		tcLog.d("position = "+position+" convertView = "+convertView+" parent = "+parent);
//		return super.getView(position,convertView,parent);
//	}
	
	public void clear() {
		tcLog.logCall();
		super.clear();
		mTickets.clear();
		notifyDataSetChanged();
	}
	
	public void addAll(Tickets tl) {
        tcLog.d(tl != null ? tl.ticketList.toString() : null);
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
//        tcLog.d( "getTicket i = " + i);
		return (mTickets!= null?mTickets.getTicket(i):null);
	}
	
	public int getTicketContentCount() {
		return mTickets.getTicketContentCount();
	}
}