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

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.alexd.jsonrpc.JSONRPCException;

public class TicketProvider extends ContentProvider {
	public static final String SET_CONFIG = "setConfig";
	public static final String GET_CONFIG = "getConfig";
	public static final String CLEAR_CONFIG = "clearConfig";
	public static final String VERIFY_HOST = "verifyHost";
	public static final String RESULT = "rv";
	public static final String ERROR = "error";
	
	
	public static final String AUTHORITY = "com.mfvl.trac.client.provider.TicketProvider";
	public static final String URI = "content://"+AUTHORITY;
	private static final String LIST_QUERY_PATH = "ticket/query";
	private static final String TICKET_QUERY_PATH = "ticket/get/#";
	private static final String QUERY_CHANGES_PATH = "ticket/getRecentChanges/#";
	
	public static final Uri AUTH_URI = Uri.parse(URI);
	public static final Uri LIST_QUERY_URI = Uri.withAppendedPath(AUTH_URI, LIST_QUERY_PATH);
	public static final Uri TICKET_QUERY_URI = Uri.withAppendedPath(AUTH_URI, TICKET_QUERY_PATH);
	public static final Uri QUERY_CHANGES_URI = Uri.withAppendedPath(AUTH_URI, QUERY_CHANGES_PATH);
	
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/"+AUTHORITY ;
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/"+AUTHORITY ;
	
    private static final int LIST_TICKETS= 1;
    private static final int GET_TICKET = 2;
	private static final int GET_CHANGES = 3;

    private static final UriMatcher sURIMatcher;
	private String currentUrl;
	private String currentName;
	private String currentPass;
	private boolean currentSslHack;
	private boolean currentSslHostNameHack;
	
	private TracHttpClient hClient = null; // if null than not connected to host
	private TicketCursor cTickets = null;
	
	private static final int MAXPERMITS = 1;
	private static Semaphore accessAllowed = new Semaphore(MAXPERMITS, true);	

    static
    {
		sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, LIST_QUERY_PATH, LIST_TICKETS);
        sURIMatcher.addURI(AUTHORITY, TICKET_QUERY_PATH, GET_TICKET);
        sURIMatcher.addURI(AUTHORITY, QUERY_CHANGES_PATH, GET_CHANGES);
    }	
	
	@Override
	public boolean onCreate() {
		tcLog.d(getClass().getName(),"onCreate");
		clearConfig();
		return true;
	}
	
	private boolean equalBundles(Bundle one, Bundle two) {
		if(one.size() != two.size()) {
			return false;
		}
		Set<String> setOne = one.keySet();
		Object valueOne;
		Object valueTwo;

		for(String key : setOne) {
			if (!two.containsKey(key)) {
				return false;
			}
			valueOne = one.get(key);
			valueTwo = two.get(key);
			if(valueOne instanceof Bundle && valueTwo instanceof Bundle && !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
				return false;
			} else if(valueOne == null) {
				if (valueTwo != null) {
					return false;
				}
			} else if(!valueOne.equals(valueTwo)) {
				return false;
			}
		}
		return true;
	}
	
	private void initCursor() {
		accessAllowed.acquireUninterruptibly(MAXPERMITS);
		new Thread() {
			@Override
			public void run() {
				tcLog.d(getClass().getName()+".initCursor.run "+this,"starting thread");
				hClient = TracHttpClient.getInstance(currentUrl,currentSslHack,currentSslHostNameHack,currentName,currentPass);
				TicketModel.getInstance();
				cTickets = new TicketCursor();
				tcLog.d(getClass().getName()+".initCursor.run "+this,"hClient = "+hClient+" cTickets = "+cTickets);
				accessAllowed.release(MAXPERMITS);
				tcLog.d(getClass().getName()+".initCursor.run "+this,"columns = "+cTickets.getColumnNames());
			}
		}.start();
	}

	private Bundle setConfig(final Bundle values) {
		final Bundle b = getConfig();
		if (!equalBundles(values,b)) {
			currentUrl = values.getString(Const.CURRENT_URL);
			currentName = values.getString(Const.CURRENT_USERNAME);
			currentPass = values.getString(Const.CURRENT_PASSWORD);
			currentSslHack = values.getBoolean(Const.CURRENT_SSLHACK);
			currentSslHostNameHack = values.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
			hClient=null;
			cTickets = null;
			initCursor();
		}
		return getConfig();
	}
	
	private Bundle clearConfig() {
		currentUrl = null;
		currentName = null;
		currentPass = null;
		currentSslHack = false;
		currentSslHostNameHack = false;
		hClient = null;
		cTickets = null;
		return null;
	}
	
	private Bundle getConfig() {
		Bundle b = new Bundle();
		b.putString(Const.CURRENT_URL,currentUrl);
		b.putString(Const.CURRENT_USERNAME,currentName);
		b.putString(Const.CURRENT_PASSWORD,currentPass);
		b.putBoolean(Const.CURRENT_SSLHACK,currentSslHack);
		b.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK,currentSslHostNameHack);
		return b;
	}
	
	private Bundle verifyHost(final Bundle values) {
		Bundle b = new Bundle();
		try {
			String rv = TracHttpClient.verifyHost(values.getString(Const.CURRENT_URL),
				values.getBoolean(Const.CURRENT_SSLHACK),
				values.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK),
				values.getString(Const.CURRENT_USERNAME),
				values.getString(Const.CURRENT_PASSWORD));
			b.putString(RESULT,rv);
		} catch (Exception e) {
			b.putString(ERROR,e.getMessage());
		}
		return b;
	}
	
	@Override
	public Bundle call(final String method,final String arg,final Bundle values) {
		tcLog.d(getClass().getName(),"call entry "+method+" "+arg+" "+values);
		Bundle b = null;
		if (SET_CONFIG.equals(method)) {
			b = setConfig(values);
		} else if (GET_CONFIG.equals(method)) {
			b = getConfig();
		} else if (VERIFY_HOST.equals(method)) {
			b = verifyHost(values);
		}
		tcLog.d(getClass().getName(),"call exit "+b);
		return b;
	}
	
	@Override
	public  String getType(Uri uri) {
		tcLog.d(getClass().getName(),"getType + "+uri+ " " + sURIMatcher.match(uri));
		switch (sURIMatcher.match(uri)) {
			case LIST_TICKETS:
			case GET_CHANGES:
			return CONTENT_TYPE;
			
			case GET_TICKET:
			return CONTENT_ITEM_TYPE;

			default:
		}
		return null;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		tcLog.d(getClass().getName(),"insert + "+uri+" "+values+ " " + sURIMatcher.match(uri));
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		tcLog.d(getClass().getName(),"delete + "+uri+ " "+ selection+" " +selectionArgs);
		return 0;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs){
		tcLog.d(getClass().getName(),"update + "+uri+ " " + values+ " "+ selection+" " +selectionArgs+ " " + sURIMatcher.match(uri));
		return 0;
	}

	private void buildCall(JSONArray mc, int ticknr) throws JSONException {
		mc.put(new TracJSONObject().makeComplexCall(Ticket.TICKET_GET + "_" + ticknr, "ticket.get", ticknr));
		mc.put(new TracJSONObject().makeComplexCall(Ticket.TICKET_CHANGE + "_" + ticknr, "ticket.changeLog", ticknr));
		mc.put(new TracJSONObject().makeComplexCall(Ticket.TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr));
		mc.put(new TracJSONObject().makeComplexCall(Ticket.TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
	}

	private void loadTicketContent(final Uri uri,final String[] projection) throws TicketLoadException {
		tcLog.d(getClass().getName(),"loadTicketContent");
		int count = Tickets.getTicketCount();
		for (int j = 0; j < count; j += Tickets.getTicketGroupCount()) {
			final JSONArray mc = new JSONArray();
			for (int i = j; i < (j + Tickets.getTicketGroupCount() < count ? j + Tickets.getTicketGroupCount() : count); i++) {
				try {
					buildCall(mc, Tickets.tickets[i]);
				} catch (final Exception e) {
					throw new TicketLoadException("loadTicketContent Exception during buildCall");
				}
			}
			try {
				final JSONArray mcresult = TracHttpClient.getInstance().callJSONArray("system.multicall", mc);
				// tcLog.d(getClass().getName(), "mcresult = " + mcresult);
				Ticket t = null;
				for (int k = 0; k < mcresult.length(); k++) {
					try {
						final JSONObject res = mcresult.getJSONObject(k);
						final String id = res.getString("id");
						final JSONArray result = res.getJSONArray("result");
						final int startpos = id.indexOf("_") + 1;
						final int thisTicket = Integer.parseInt(id.substring(startpos));
						if (t == null || t.getTicketnr() != thisTicket) {
							t = Tickets.getTicket(thisTicket);
						}
						if (t != null) {
							if (id.equals(Ticket.TICKET_GET + "_" + thisTicket)) {
								final JSONObject v = result.getJSONObject(3);
								t.setFields(v);
								Tickets.incTicketContentCount();
							} else if (id.equals(Ticket.TICKET_CHANGE + "_" + thisTicket)) {
								final JSONArray h = result;
								t.setHistory(h);
							} else if (id.equals(Ticket.TICKET_ATTACH + "_" + thisTicket)) {
								final JSONArray at = result;
								t.setAttachments(at);
							} else if (id.equals(Ticket.TICKET_ACTION + "_" + thisTicket)) {
								final JSONArray ac = result;
								t.setActions(ac);
							} else {
								tcLog.d(getClass().getName(), "loadTickets, onverwachte respons = " + result);
							}
						}
					} catch (final Exception e1) {
						throw new TicketLoadException(
								"loadTicketContent Exception thrown innerloop j=" + j + " k=" + k, e1);
					}
				}
				Tickets.notifyChange();
			} catch (final TicketLoadException e) {
				throw new TicketLoadException("loadTicketContent TicketLoadException thrown outerloop j=" + j, e);
			} catch (final Exception e) {
				throw new TicketLoadException("loadTicketContent Exception thrown outerloop j=" + j, e);
			}
			tcLog.d(getClass().getName(), "loadTicketContent loop " + Tickets.getTicketContentCount());
			Tickets.notifyChange();
		}
	}
	
	private void loadTickets(final Uri uri,final String[] projection, String reqString) {
		tcLog.d(getClass().getName(),"loadTickets  "+uri+ " "+ projection+ " "+ reqString);
		if (cTickets ==null || cTickets.getCount() != 0) {
			initCursor();
		}
		cTickets.setNotificationUri(getContext().getContentResolver(),uri);

		TracHttpClient.getInstance();
		if (reqString.length() == 0) {
			reqString = "max=0";
		}
		final String rs = reqString;
		tcLog.d(getClass().getName(),"loadTickets reqString = "+reqString);
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					tcLog.d(getClass().getName(),"loadTickets start thread reqString = "+rs);
					final JSONArray jsonTicketlist = TracHttpClient.Query(rs);
					tcLog.d(getClass().getName(), jsonTicketlist.toString());
					final int count = jsonTicketlist.length();
					Tickets.tickets = new int[count];
					for (int i = 0; i < count; i++) {
						try {
							Tickets.tickets[i] = jsonTicketlist.getInt(i);
							final Ticket t = new Ticket(Tickets.tickets[i], null, null, null, null);
							Tickets.ticketList.add(i,t);
							Tickets.putTicket(t);
						} catch (JSONException e) {
							Tickets.tickets[i] = -1;
							Tickets.ticketList.add(Tickets.tickets[i],null);				}
					}
					tcLog.d(getClass().getName(), "loadTicketList ticketlist loaded");
					new Thread() {
						@Override
						public void run() {
							try {
								loadTicketContent(uri,projection);
							} catch (Exception e) {
								tcLog.e(getClass().getName(),"Exception in ticketContentLoad",e);
							} finally {
								tcLog.d(getClass().getName(),"loadTickets-thread before accsessAllowed permits = "+accessAllowed.availablePermits());
								accessAllowed.release(1);
								tcLog.d(getClass().getName(),"loadTickets-thread after accsessAllowed permits = "+accessAllowed.availablePermits());
							}
						}
					}.start();
				} catch (JSONRPCException e) {
					tcLog.d(getClass().getName(),"loadTickets-thread before accsessAllowed permits = "+accessAllowed.availablePermits());
					accessAllowed.release(1);
					tcLog.d(getClass().getName(),"loadTickets-thread after accsessAllowed permits = "+accessAllowed.availablePermits());
				}		
			}
		};
		t.start();
		try {
			t.join();
		} catch (Exception e) {
			tcLog.d(getClass().getName(),"loadTickets before accsessAllowed permits = "+accessAllowed.availablePermits());
			accessAllowed.release(1);
			tcLog.d(getClass().getName(),"loadTickets after accsessAllowed permits = "+accessAllowed.availablePermits());
		}
	}
	
	private String joinList(Object list[], final String sep) {
		String reqString = "";
		for (final Object fs : list) {
			if (fs != null) {
				if (reqString.length() > 0) {
					reqString += sep;
				}
				reqString += fs.toString();
			}
		}
		return reqString;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		tcLog.d(getClass().getName(),"query + entry "+uri+ " "+ projection+ " "+ selection+" " +selectionArgs+" "+sortOrder+ " " + sURIMatcher.match(uri));
		switch (sURIMatcher.match(uri)) {
			case LIST_TICKETS:
			tcLog.d(getClass().getName(),"query before accsessAllowed permits = "+accessAllowed.availablePermits());
			accessAllowed.acquireUninterruptibly(1);
			tcLog.d(getClass().getName(),"query after accsessAllowed permits = "+accessAllowed.availablePermits());
			loadTickets(uri,projection,joinList(new String[] {selection,sortOrder}, "&"));
			break;
			
			case GET_CHANGES:
			break;
			
			case GET_TICKET:
			break;

			default:
		}
		
		tcLog.d(getClass().getName(),"query + exit "+cTickets);
		return cTickets;
	}
}