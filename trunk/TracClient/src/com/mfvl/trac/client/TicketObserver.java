package com.mfvl.trac.client;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

public class TicketObserver extends ContentObserver {
	
	private TicketCursor c;

	TicketObserver(Handler h) {
		super(h);
		tcLog.d(getClass().getName(), "TicketObserver construction");
	}
	
	public boolean deliverSelfNotifications() {
		return true;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		tcLog.d(getClass().getName(), "onChange selfChange = " + selfChange );
		onChange(selfChange, null);
	}

	// Implement the onChange(boolean, Uri) method to take advantage of the new Uri argument.
	@Override
	public void onChange(boolean selfChange, Uri uri) {
		tcLog.d(getClass().getName(), "onChange selfChange = " + selfChange+" "+ uri );
		// Handle change.
	}
	
	public void setCursor(TicketCursor c) {
		this.c=c;
	}

}
