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
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import java.util.Set;
import java.util.concurrent.Semaphore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.alexd.jsonrpc.JSONRPCException;


public class TicketProvider extends ContentProvider {
	public static final String GET_TICKETMODEL = "getTicketModel";	
	
    public static final String RESULT = "rv";
    public static final String ERROR = "error";

    public static final String AUTHORITY = "com.mfvl.trac.client.provider.TicketProvider";
    public static final String URI = "content://" + AUTHORITY;
	
    private static final String LIST_QUERY_PATH = "tickets";
	private static final String GET_QUERY_PATH = "ticket/";
    private static final String QUERY_CHANGES_PATH = "ticket/getRecentChanges/";
    private static final String CONFIG_QUERY_PATH = "config";
    private static final String VERIFY_QUERY_PATH = "verify";
    private static final String RESET_QUERY_PATH = "reset";
	private static final String ATTACHMENT_QUERY_PATH = "attachment/";
    
    public static final Uri AUTH_URI = Uri.parse(URI);
    public static final Uri LIST_QUERY_URI = Uri.withAppendedPath(AUTH_URI, LIST_QUERY_PATH);
    public static final Uri GET_QUERY_URI = Uri.withAppendedPath(AUTH_URI, GET_QUERY_PATH);
    public static final Uri QUERY_CHANGES_URI = Uri.withAppendedPath(AUTH_URI, QUERY_CHANGES_PATH);
    public static final Uri CONFIG_QUERY_URI = Uri.withAppendedPath(AUTH_URI, CONFIG_QUERY_PATH);
    public static final Uri VERIFY_QUERY_URI = Uri.withAppendedPath(AUTH_URI, VERIFY_QUERY_PATH);
    public static final Uri RESET_QUERY_URI = Uri.withAppendedPath(AUTH_URI, RESET_QUERY_PATH);
    public static final Uri ATTACHMENT_QUERY_URI = Uri.withAppendedPath(AUTH_URI, ATTACHMENT_QUERY_PATH);
    
    public static final String DIR_CONTENT_TYPE = "vnd.android.cursor.dir/" + AUTHORITY;
    public static final String ITEM_CONTENT_TYPE = "vnd.android.cursor.item/" + AUTHORITY;
    
    private static final int LIST_TICKETS = 1;
    private static final int GET_CHANGES = 3;
    private static final int CONFIG = 4;
    private static final int VERIFY = 5;
	private static final int GET_TICKET = 6;
	private static final int RESET = 7;
	private static final int GET_ATTACHMENT = 8;
	private static final int PUT_ATTACHMENT = 9;
	
    private final static String TICKET_GET = "GET";
    private final static String TICKET_CHANGE = "CHANGE";
    private final static String TICKET_ATTACH = "ATTACH";
    private final static String TICKET_ACTION = "ACTION";

    private static final UriMatcher sURIMatcher;
	
	private TracHttpClient tracClient = null;
    private String currentUrl;
    private String currentName;
    private String currentPass;
    private boolean currentSslHack;
    private boolean currentSslHostNameHack;
	private Uri currentUri = null;
	private String[] currentProjection = null;
	private String currentReqString = null;

	private Tickets ticketList = null;
	private TicketModel tm = null;

    private static final int MAXPERMITS = 1;
    private static Semaphore accessAllowed = new Semaphore(MAXPERMITS, true);	
	
    static {
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, LIST_QUERY_PATH, LIST_TICKETS);
        sURIMatcher.addURI(AUTHORITY, GET_QUERY_PATH+"#", GET_TICKET);
        sURIMatcher.addURI(AUTHORITY, QUERY_CHANGES_PATH+"*", GET_CHANGES);
        sURIMatcher.addURI(AUTHORITY, CONFIG_QUERY_PATH, CONFIG);
        sURIMatcher.addURI(AUTHORITY, VERIFY_QUERY_PATH, VERIFY);
        sURIMatcher.addURI(AUTHORITY, RESET_QUERY_PATH, RESET);
        sURIMatcher.addURI(AUTHORITY, ATTACHMENT_QUERY_PATH+"#/#", GET_ATTACHMENT);
        sURIMatcher.addURI(AUTHORITY, ATTACHMENT_QUERY_PATH+"#", PUT_ATTACHMENT);
    }	
    
    @Override
    public boolean onCreate() {
        tcLog.d(getClass().getName(), "onCreate");
        clearConfig();
        return true;
    }
    
    @Override
    public String getType(Uri uri) {
        tcLog.d(getClass().getName(), "getType + " + uri + " " + sURIMatcher.match(uri));
        switch (sURIMatcher.match(uri)) {
			case LIST_TICKETS:
			case GET_CHANGES:
            return DIR_CONTENT_TYPE;
			
			case GET_TICKET:
			return ITEM_CONTENT_TYPE;
			
			case GET_ATTACHMENT:
			case PUT_ATTACHMENT:
			return null;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
		Uri newUri = null;
        tcLog.d(getClass().getName(), "insert + " + uri + " " + values + " " + sURIMatcher.match(uri));
        switch (sURIMatcher.match(uri)) {
	    
			case CONFIG:
            setConfig(cv2b(values));
            newUri = uri;
			break;
			
			case RESET:
			ticketList = null;
			clearConfig();
			break;

			case VERIFY:
            JSONObject b = verifyHost(cv2b(values));
            newUri = Uri.withAppendedPath(uri, b.toString());
            break;
			
			case GET_TICKET:
			int ticknr = Integer.parseInt(uri.getLastPathSegment());
			String s = values.getAsString("summary");
			String d = values.getAsString("description");
			try {
				JSONObject _velden = new JSONObject(values.getAsString("velden"));
				final int newticknr = tracClient.createTicket(s, d, _velden);
				if (newticknr != -1) {
					reloadTicketData(new Ticket(newticknr));
					newUri = Uri.withAppendedPath(GET_QUERY_URI,""+newticknr);
				} else {
					popup_message(R.string.storerr,R.string.noticketUnk,"");
				}
			} catch (final Exception e) {
				try {
					final JSONObject o = new JSONObject(e.getMessage());
					popup_message(R.string.storerr,R.string.storerrdesc,o.getString("message"));
				} catch (final JSONException e1) {
					tcLog.e(getClass().getName(), "create failed", e1);
					popup_message(R.string.storerr,R.string.invalidJson,e1.getMessage());
				}
			}
			notifyChange(newUri);
			break;
        }
        return newUri;
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        tcLog.d(getClass().getName(), "delete + " + uri + " " + selection + " " + selectionArgs);
        return 0;
    }
	
    
    @Override
    public int update(Uri uri, final ContentValues cv, final String selection, final String[] selectionArgs) {
        tcLog.d(getClass().getName(),
                "update + " + uri + " " + cv + " " + selection + " " + selectionArgs + " " + sURIMatcher.match(uri));
        switch (sURIMatcher.match(uri)) {
	    
			case GET_TICKET:
			final int ticknr = Integer.parseInt(uri.getLastPathSegment());
			if (ticknr == -1) {
				popup_message(R.string.storerr,R.string.invtick,"Ticket = "+ticknr);
				return 0;
			}
			final String cmt = cv.getAsString("comment");
			final boolean notify = cv.getAsBoolean("notify");
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						JSONObject _velden = new JSONObject(cv.getAsString("velden"));
						JSONArray retTick = tracClient.updateTicket(ticknr, cmt, _velden, notify); // TODO zou nieuwe ticket retourneren en is volgende dus niet nodig
						reloadTicketData(new Ticket(ticknr));
					} catch (final Exception e) {
						tcLog.e(getClass().getName(), "JSONRPCException", e);
						popup_message(R.string.storerr,R.string.storerrdesc,e.getMessage());
					}
			}};
			t.start();
			try {
				t.join();
			} catch (Exception e) {
				tcLog.e(getClass().getName(),"Exception during join in update",e);
				popup_message(R.string.storerr,R.string.storerrdesc,e.getMessage());
			}
			notifyChange(uri);
			return 1;
			
			case PUT_ATTACHMENT:
			return 0;
		}
        return 0;
    }
	
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        tcLog.d(getClass().getName(),
                "query + entry " + uri + " " + projection + " " + selection + " " + selectionArgs + " " + sortOrder + " "
                + sURIMatcher.match(uri));
//		try {
//			throw new Exception("Debug");
//		} catch (Exception e) {
//			tcLog.d(getClass().getName(),"Debug exception",e);
//		}
				
        switch (sURIMatcher.match(uri)) {
			case LIST_TICKETS:
            accessAllowed.acquireUninterruptibly(1);
            return loadTickets(uri, projection, Credentials.joinList(new String[] { selection, sortOrder}, "&"));
			
			case GET_CHANGES:
			return getChanges(uri.getLastPathSegment());
			
			case GET_TICKET:
			return getSingleTicket(uri.getLastPathSegment());
			
			case GET_ATTACHMENT:
			return null;
			
        default:
			return null;
        }
	
    }
	
	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		tcLog.d(getClass().getName(),"call method = "+method);
		if (GET_TICKETMODEL.equals(method)) {
			if (tm != null) {
				tm.wacht();
				//tcLog.d(getClass().getName(),"call tm = "+ tm);
				Bundle b = new Bundle();
				b.putSerializable(Const.TICKETMODELNAME,tm);
				return b;
			}
		}
		return null;
	}
	
	/**
	  *  Internal methods
	  *
	  */
	  
	private Cursor getSingleTicket(final String ticknrstr) {
		tcLog.d(getClass().getName(),"getSingleTicket ticknr = "+ticknrstr);
		if (ticketList != null) {
			int ticknr = Integer.parseInt(ticknrstr);
			final Tickets tl = new Tickets();
			tl.addTicket(new Ticket(ticknr));
			final Semaphore active = new Semaphore(1, true);	
			active.acquireUninterruptibly ();
			new Thread() {
				@Override
				public void run() {
					try {
						loadTicketContent(null,tl);
					} catch (Exception e) {
						tcLog.e(getClass().getName(), "Exception in ticketContentLoad", e);
					} finally {
						active.release(1);
					}
				}
			}.start();
			active.acquireUninterruptibly ();
			active.release();
			Ticket t1 = tl.getTicket(ticknr);
			if (t1== null) {
				return null;
			}
			if (!t1.hasdata()) {
				tl.delTicket(ticknr);
			}
			return new TicketCursor(tl);
		}
		return null;
	}
	
	private Cursor getChanges(final String isoTijd) {
		tcLog.d(getClass().getName(),"getChanges ticknr = "+isoTijd);

        try {
            final JSONArray datum = new JSONArray();

            datum.put("datetime");
            datum.put(isoTijd);
            final JSONObject ob = new JSONObject();

            ob.put("__jsonclass__", datum);
            final JSONArray param = new JSONArray();

            param.put(ob);
            final JSONArray jsonTicketlist = tracClient.callJSONArray("ticket.getRecentChanges", param);
			
			Tickets t = new Tickets();
			
			if (jsonTicketlist.length() > 0) {
				for (int i = 0;i<jsonTicketlist.length();i++) {
					int ticknr = jsonTicketlist.getInt(i);
					t.addTicket(new Ticket(ticknr));
				}
				loadTicketContent(null,t);
			}
            return new TicketCursor(t);
		} catch (Exception e) {
			tcLog.d(getClass().getName(),"getChanges exception",e);
		}

		return null;
	}

	private void notify_datachanged() {
        Intent intent = new Intent(TracStart.DATACHANGED_MESSAGE);
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
	}

	private void popup_warning(int messString, String addit) {
		popup_message(R.string.warning,messString,addit);
	}
	
	private void popup_message(int title,int messString,String addit) {
		/* 
			Since we are in a Content Provider we only have an Application context. This means we cannot do a runOnUIthread call here.
			For that reason we send a Broadcast within the app to the receiver in TracStart. There the popup will be serviced.
		*/
        Intent intent = new Intent(TracStart.PROVIDER_MESSAGE);
        intent.putExtra("title", title );
        intent.putExtra("message", messString );
 		intent.putExtra("additonal",addit);
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
	}
    
    private void buildCall(JSONArray multiCall, int ticknr) throws JSONException {
        multiCall
			.put(new TracJSONObject().makeComplexCall(TICKET_GET + "_" + ticknr, "ticket.get", ticknr))
			.put(new TracJSONObject().makeComplexCall(TICKET_CHANGE + "_" + ticknr, "ticket.changeLog", ticknr))
			.put(new TracJSONObject().makeComplexCall(TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr))
			.put(new TracJSONObject().makeComplexCall(TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
    }
	
	private void notifyChange(Uri uri) {
        tcLog.d(getClass().getName(), "notifyChange uri = "+uri);
		if (uri != null) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
	}
 
    private void loadTicketContent(Uri uri,Tickets tl) throws TicketLoadException {
        tcLog.d(getClass().getName(), "loadTicketContent uri = "+uri);
        int count = tl.getTicketCount();
        tcLog.d(getClass().getName(), "loadTicketContent count = "+count+ " "+ tl);
		

        for (int j = 0; j < count; j += Const.ticketGroupCount) {
            final JSONArray mc = new JSONArray();

            for (int i = j; i < (j + Const.ticketGroupCount < count ? j + Const.ticketGroupCount : count); i++) {
                try {
                    buildCall(mc, tl.ticketList.get(i).getTicketnr());
                } catch (final Exception e) {
                    throw new TicketLoadException("loadTicketContent Exception during buildCall",e);
                }
            }
            try {
                final JSONArray mcresult = tracClient.callJSONArray("system.multicall", mc);
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
                            t = tl.getTicket(thisTicket);
                        }
                        if (t != null) {
                            if (id.equals(TICKET_GET + "_" + thisTicket)) {
                                t.setFields(result.getJSONObject(3));
                                tl.incTicketContentCount();
                            } else if (id.equals(TICKET_CHANGE + "_" + thisTicket)) {
                                t.setHistory(result);
                            } else if (id.equals(TICKET_ATTACH + "_" + thisTicket)) {
                                t.setAttachments(result);
                            } else if (id.equals(TICKET_ACTION + "_" + thisTicket)) {
                                t.setActions(result);
                            } else {
                                tcLog.d(getClass().getName(), "loadTickets, unexpected response = " + result);
                            }
                        }
                    } catch (final Exception e1) {
                        throw new TicketLoadException("loadTicketContent Exception thrown innerloop j=" + j + " k=" + k, e1);
                    }
                }
            } catch (final TicketLoadException e) {
                throw new TicketLoadException("loadTicketContent TicketLoadException thrown outerloop j=" + j, e);
            } catch (final Exception e) {
                throw new TicketLoadException("loadTicketContent Exception thrown outerloop j=" + j, e);
            }  finally {
				tcLog.d(getClass().getName(), "loadTicketContent loop " + tl.getTicketContentCount());
				notify_datachanged();
				notifyChange(uri);
//				tl.notifyChange();
			}
        }
    }
	
    private Cursor loadTickets(final Uri uri, final String[] projection, String reqString) {
		
        tcLog.d(getClass().getName(), "loadTickets  " + uri + " " + projection + " " + reqString);

		TicketCursor cTickets = new TicketCursor(ticketList);
		
		if (!uri.equals(currentUri) || !projection.equals(currentProjection) || !reqString.equals(currentReqString)) {
//			initCursor();
			accessAllowed.acquireUninterruptibly(1); // initCursor claims all so  we know it is ready when we get the lock
			accessAllowed.release(1); // No further need
	 
			if (reqString.length() == 0) {
				reqString = "max=0";
			}
			final String rs = reqString;

			tcLog.d(getClass().getName(), "loadTickets reqString = " + reqString);
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						tcLog.d(getClass().getName(), "loadTickets start thread reqString = " + rs);
						final JSONArray jsonTicketlist = tracClient.Query(rs);

						tcLog.d(getClass().getName(), jsonTicketlist.toString());
						final int count = jsonTicketlist.length();

						if (count > 0) {
							int tickets[] = new int[count];
							for (int i = 0; i < count; i++) {
								Ticket t = null;
								try {
									tickets[i] = jsonTicketlist.getInt(i);
									t = new Ticket(tickets[i]); 
									ticketList.putTicket(t);
								} catch (JSONException e) {
									tickets[i] = -1;
								} finally {
									ticketList.ticketList.add(i, t);
								}
							}
							tcLog.d(getClass().getName(), "loadTicketList ticketlist loaded");
							new Thread() {
								@Override
								public void run() {
									try {
										loadTicketContent(uri,ticketList);
									} catch (Exception e) {
										tcLog.e(getClass().getName(), "Exception in ticketContentLoad", e);
									} finally {
										accessAllowed.release(1);
										tcLog.d(getClass().getName(), "loadTicketList content loaded");
									}
								}
							}.start();
						}
					} catch (JSONRPCException e) {
						popup_warning(R.string.connerr,e.getMessage());
						accessAllowed.release(1);
					}		
				}
			};
			t.start();

			try {
				t.join();
//				tcLog.e(getClass().getName(), "In main thread again count = "+ticketList.getTicketCount()+" errmsg = "+loadTicketErrorMsg);
				if (ticketList.getTicketCount() == 0) {
					popup_warning(R.string.notickets,null);
					accessAllowed.release(1);
				}
			} catch (Exception e) {
				accessAllowed.release(1);
			}
		}
		return cTickets;
    }
    
    private boolean equalBundles(Bundle one, Bundle two) {
        if (one.size() != two.size()) {
            return false;
        }
        Set<String> setOne = one.keySet();
        Object valueOne;
        Object valueTwo;
	
        for (String key : setOne) {
            if (!two.containsKey(key)) {
                return false;
            }
            valueOne = one.get(key);
            valueTwo = two.get(key);
            if (valueOne instanceof Bundle && valueTwo instanceof Bundle && !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            } else if (valueOne == null) {
                if (valueTwo != null) {
                    return false;
                }
            } else if (!valueOne.equals(valueTwo)) {
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
                //tcLog.d(getClass().getName() + ".initCursor.run " + this, "starting thread");
				tracClient = new TracHttpClient(currentUrl, currentSslHack, currentSslHostNameHack, currentName, currentPass);
                tm=TicketModel.getInstance(tracClient);
                accessAllowed.release(MAXPERMITS);
                //tcLog.d(getClass().getName() + ".initCursor.run " + this, "ending thread");
            }
        }.start();
        //tcLog.d(getClass().getName() + ".initCursor.run " + this, "wait for thread");
		accessAllowed.acquireUninterruptibly(MAXPERMITS);
		accessAllowed.release(MAXPERMITS);
        //tcLog.d(getClass().getName() + ".initCursor.run " + this, "thread finished");
    }
    
    private void cv2bs(ContentValues cv, Bundle b, String f) {
        if (cv.containsKey(f)) {
            b.putString(f, cv.getAsString(f));
        }	
    }
    
    private void cv2bb(ContentValues cv, Bundle b, String f) {
        if (cv.containsKey(f)) {
            b.putBoolean(f, cv.getAsBoolean(f));
        }	
    }

    private Bundle cv2b(final ContentValues cv) {
        final Bundle values = new Bundle();

        cv2bs(cv, values, Const.CURRENT_URL);
        cv2bs(cv, values, Const.CURRENT_USERNAME);
        cv2bs(cv, values, Const.CURRENT_PASSWORD);
        cv2bb(cv, values, Const.CURRENT_SSLHACK);
        cv2bb(cv, values, Const.CURRENT_SSLHOSTNAMEHACK);
        return values;
    }
    
    private Bundle setConfig(final Bundle values) {
        final Bundle b = getConfig();

        if (!equalBundles(values, b) || ticketList == null) {
			currentUrl = values.getString(Const.CURRENT_URL);
			currentName = values.getString(Const.CURRENT_USERNAME);
			currentPass = values.getString(Const.CURRENT_PASSWORD);
			currentSslHack = values.getBoolean(Const.CURRENT_SSLHACK);
			currentSslHostNameHack = values.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
			ticketList = new Tickets();
			ticketList.resetCache();
			initCursor();
		}
		accessAllowed.release(MAXPERMITS);
		return getConfig();
    }
    
    private Bundle clearConfig() {
        currentUrl = null;
        currentName = null;
        currentPass = null;
        currentSslHack = false;
        currentSslHostNameHack = false;
		
		currentUri = null;
		currentProjection = null;
		currentReqString = null;
		
		tm = null;
    
       return null;
    }
    
    private Bundle getConfig() {
        Bundle b = new Bundle();

        b.putString(Const.CURRENT_URL, currentUrl);
        b.putString(Const.CURRENT_USERNAME, currentName);
        b.putString(Const.CURRENT_PASSWORD, currentPass);
        b.putBoolean(Const.CURRENT_SSLHACK, currentSslHack);
        b.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, currentSslHostNameHack);
        return b;
    }
    
    private JSONObject verifyHost(final Bundle values) {
        JSONObject b = new JSONObject();

        try {
			TracHttpClient verifyClient = new TracHttpClient(values.getString(Const.CURRENT_URL), values.getBoolean(Const.CURRENT_SSLHACK),
                    values.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK), values.getString(Const.CURRENT_USERNAME),
                    values.getString(Const.CURRENT_PASSWORD));
            String rv = verifyClient.verifyHost();

            b.put(RESULT, rv);
        } catch (Exception e) {
            try {
                b.put(ERROR, e.getMessage());
            } catch (JSONException e1) {}
        }
        return b;
    }

	private void reloadTicketData(Ticket t) {
		Tickets tl = new Tickets();
		tl.addTicket(t);
		loadTicketContent(null,tl);
	}

}
