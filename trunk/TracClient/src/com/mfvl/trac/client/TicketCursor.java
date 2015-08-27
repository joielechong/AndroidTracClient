/*
 * Copyright (C) 2015 Michiel van Loon
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


import android.database.AbstractCursor;


public class TicketCursor extends AbstractCursor {
	
    public static final int FIELD_ID = 0;
    public static final int FIELD_TICKET = 1;
	
    public static final String STR_FIELD_ID = "_id";
    public static final String STR_FIELD_TICKET = "ticket";
	
    final String[] columns = new String[] { STR_FIELD_ID, STR_FIELD_TICKET}; 
	
	private Tickets ticketList;
	
    TicketCursor(Tickets ticketList) {
        super();
		this.ticketList = ticketList;
//        this.ticketList.initList();
        tcLog.d(getClass().getName(), "TicketCursor constructed " + this);
    }
	
	TicketCursor(Ticket ticket) {
		super();
		this.ticketList = new Tickets();
		this.ticketList.initList();
		this.ticketList.putTicket(ticket);
	}
		
	@Override 
	public void close() {
		tcLog.d(getClass().getName(),"close "+this);
		super.close();
	}
	
	public Tickets getTicketList() {
		return ticketList;
	}

    @Override
    public boolean isNull(int column) {
        // tcLog.d(getClass().getName(),"isNull "+ column+" "+getPosition()+ " "+this);
        Ticket t = ticketList.ticketList.get(this.getPosition());

        return t == null;
    }
	
    @Override
    public String[] getColumnNames() {
        // tcLog.d(getClass().getName(),"getColumnNames "+this);
        return columns;
    }
	
    @Override
    public int getCount() {
        // tcLog.d(getClass().getName(),"getCount -->"+ ticketList.getTicketCount()+" "+this);
        return ticketList.getTicketCount();
    }

    @Override
    public double getDouble(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getDouble "+ column+" "+getPosition());
        throw new IllegalArgumentException("No double field");
    }

    @Override
    public float getFloat(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getFloat "+ column+" "+getPosition());
        throw new IllegalArgumentException("No float field");
    }

    @Override
    public int getInt(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getInt "+ column+" "+getPosition());
        if (column != FIELD_ID) {
            throw new IllegalArgumentException("No int field");
        }
        return ticketList.ticketList.get(this.getPosition()).getTicketnr();
    }
	
    @Override
    public long getLong(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getLong "+ column+" "+getPosition());
        if (column != FIELD_ID) {
            throw new IllegalArgumentException("No long field");
        }
        Ticket t = ticketList.ticketList.get(this.getPosition());

        // tcLog.d(getClass().getName(),"getLong ticket = "+ t);
        return t.getTicketnr();
    }

    @Override
    public short getShort(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getShort "+ column+" "+getPosition());
        throw new IllegalArgumentException("No short field");
    }

    @Override
    public String getString(int column) {
        // tcLog.d(getClass().getName(),"getString "+ column+" "+getPosition());
        Ticket t = ticketList.ticketList.get(this.getPosition());
        String retval = null;

        if (t != null) {
            switch (column) {
            case FIELD_ID:
                retval = "" + t.getTicketnr();
                break;
			
            case FIELD_TICKET:
                retval = t.toString();
                break;
            }
        }
        return retval;
    }

	public Ticket getTicket(int column) throws IllegalArgumentException {
        // tcLog.d(getClass().getName(),"getTicket "+ column+" "+getPosition());
        if (column != FIELD_TICKET) {
            throw new IllegalArgumentException("No Ticket field");
        }
        Ticket t = ticketList.ticketList.get(this.getPosition());
//        tcLog.d(getClass().getName(),"getTicket ticket = "+t);
		return t;
	}
	
}
