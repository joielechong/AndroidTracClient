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

import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.content.Context;

public class TicketListAdapter extends SimpleCursorAdapter {
	private CursorWrapper cursor;
	public static String[] fields = new String[] {TicketCursor.STR_FIELD_TICKET};
	public static int[] adapres = new int[] {R.id.ticket_list};
	Context context;

	public TicketListAdapter(TracStart context, int resource, TicketCursor c) {
		super(context, resource, c,fields,adapres,0);
		tcLog.d(getClass().getName(), "TicketListAdapter construction "+c);
		cursor = new CursorWrapper(c);
		this.context=context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return ColoredLines.getView(context,super.getView(position, convertView, parent),position, convertView, parent);
	}
	
	public void changeCursor(Cursor c) {
		tcLog.d(getClass().getName(), "changeCursor "+c);
//		super.changeCursorAndColumns(c,fields,adapres);
		super.changeCursor(c);
		if (! (c instanceof CursorWrapper)) {
			cursor = new CursorWrapper(c);
		} else {
			cursor = (CursorWrapper)c;
		}
	}
	
	@Override
	public Cursor swapCursor(Cursor c) {
		Cursor c1 = super.swapCursor(c);
		tcLog.d(getClass().getName(), "swapCursor new = "+c+ " old = "+c1);
		return c1;
	}
		
	public boolean moveToFirst() {
		tcLog.d(getClass().getName(), "moveToFirst "+cursor);
		boolean b =cursor.moveToFirst();
		tcLog.d(getClass().getName(), "_id = "+cursor.getLong(0));
		return b;
	}
		
	public boolean moveToNext() {
		tcLog.d(getClass().getName(), "moveToNext "+cursor);
		return cursor.moveToNext();
	}
		
	public boolean isAfterLast() {
		tcLog.d(getClass().getName(), "isAfterLast "+cursor);
		return cursor.isAfterLast();
	}
	
	@Override
	public boolean hasStableIds() {
		tcLog.d(getClass().getName(), "hasStableIds");
		return true;
	}
	
	@Override
	public void onContentChanged() {
		tcLog.d(getClass().getName(), "onContentChanged");
		super.onContentChanged();
	}

}
