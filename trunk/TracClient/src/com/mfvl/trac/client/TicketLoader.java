/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.support.v4.content.LocalBroadcastManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.alexd.jsonrpc.JSONRPCException;

public class TicketLoader extends AsyncTaskLoader<Tickets> {
    private final static String TICKET_GET = "GET";
    private final static String TICKET_CHANGE = "CHANGE";
    private final static String TICKET_ATTACH = "ATTACH";
    private final static String TICKET_ACTION = "ACTION";

    final ForceLoadContentObserver mObserver;

	LoginProfile mLoginProfile;

    CancellationSignal mCancellationSignal;
	private TicketModel tm = null;
	private TracHttpClient tracClient;
	int mode;
	String isoTijd;
	private TicketList ticketList;
	Tickets mTickets = null;

    /**
     * Creates an empty unspecified TicketListLoader.  You must follow this with
     * calls to {@link #setUri(Uri)}, {@link #setSelection(String)}, etc
     * to specify the query to perform.
     */
    public TicketLoader(Context context) {
        super(context);
        tcLog.d(getClass().getName(), "TicketLoader " + context);
        mObserver = new ForceLoadContentObserver();
		tracClient = null;
		mode = 1;
    }

   public TicketLoader(Context context, LoginProfile lp) {
		super(context);
        tcLog.d(getClass().getName(), "TicketLoader " + context+" "+lp);
		mObserver = new ForceLoadContentObserver();
		mLoginProfile = lp;
		tracClient = null;
		mode = 1;
    }

   public TicketLoader(Context context, LoginProfile lp, TicketList tl) {
		super(context);
        tcLog.d(getClass().getName(), "TicketLoader " + context+" "+lp+"  "+tl);
		mObserver = new ForceLoadContentObserver();
		mLoginProfile = lp;
		tracClient = null;
		mode = 2;
		ticketList = tl;
    }

   public TicketLoader(Context context, LoginProfile lp, Ticket t) {
		super(context);
        tcLog.d(getClass().getName(), "TicketLoader " + context+" "+lp+"  "+t);
		mObserver = new ForceLoadContentObserver();
		mLoginProfile = lp;
		tracClient = null;
		mode = 2;
		ticketList = new TicketList();
		ticketList.add(t);
    }

   public TicketLoader(Context context, LoginProfile lp, String tijd) {
		super(context);
        tcLog.d(getClass().getName(), "TicketLoader " + context+" "+lp+"  "+tijd);
		mObserver = new ForceLoadContentObserver();
		mLoginProfile = lp;
		tracClient = null;
		mode = 3;
		isoTijd = tijd;
    }

    /* Runs on a worker thread */
    @Override
    public Tickets loadInBackground() {
        tcLog.d(getClass().getName(), "loadInBackground this = "+this);
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
		
		tracClient = new TracHttpClient(mLoginProfile);
		tm=TicketModel.getInstance(tracClient);
		switch (mode) {
			case 1:
			return loadTickets();

			case 2:
			Tickets tl = new Tickets();
			for (Ticket t: ticketList) {
				tl.addTicket(t);
			}
			loadTicketContent(tl);
			return tl;
			
			case 3:
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
					loadTicketContent(t);
				}
				return t;
			} catch (Exception e) {
				tcLog.d(getClass().getName(),"getChanges exception",e);
			}
			
		}
		return null;
   }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();
        tcLog.d(getClass().getName(), "cancelLoadInBackground this = "+this);

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Tickets tl) {
        tcLog.d(getClass().getName(), "deliverResult this = "+this+" " + tl);
        if (isReset()) {
            // An async query came in while the loader is stopped
			super.deliverResult(null);;
            return;
        }
		
		if (mode == 1) {
			mTickets = tl;
		}
		
        if (isStarted() && tl != null) {
            super.deliverResult(tl);
        }
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        tcLog.d(getClass().getName(), "onStartLoading this = "+this);
        if (mTickets != null) {
            deliverResult(mTickets);
        }
        if (takeContentChanged() || mTickets == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        tcLog.d(getClass().getName(), "onStopLoading this = "+this);
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Tickets tl) {
        tcLog.d(getClass().getName(), "onCanceled this = "+this+"  "+tl);
		/*
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
		*/
		tl = null;
    }

    @Override
    protected void onReset() {
        tcLog.d(getClass().getName(), "onReset this = "+this);
        super.onReset();
        
        // Ensure the loader is stopped
        onStopLoading();
/*
        if (mTicketList != null && !mTicketList.isClosed()) {
            mTicketList.close();
        }
 */
		mTickets = null;
		}

    public LoginProfile getLoginProfile() {
        tcLog.d(getClass().getName(), "getLoginProfile this = "+this);
        return mLoginProfile;
    }

    public void setLoginProfile(LoginProfile lp) {
        tcLog.d(getClass().getName(), "setLoginProfile this = "+this+"  "+ lp);
		mLoginProfile = lp;
		mTickets = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        tcLog.d(getClass().getName(), "dump "+prefix);
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("this="); writer.println(this);
        writer.print(prefix); writer.print("mLoginProfile="); writer.println(mLoginProfile);
        writer.print(prefix); writer.print("mTickets="); writer.println(mTickets);
//        writer.print(prefix); writer.print("mContentChanged="); writer.println(mContentChanged);
    }
	
	private Tickets loadTickets() {

		tcLog.d(getClass().getName(), "loadTickets  " + mLoginProfile );

		final Tickets mTickets = new Tickets();
		mTickets.resetCache();
		String reqString="";
		List<FilterSpec> fl = mLoginProfile.getFilterList();
		if (fl != null) {
			reqString = Credentials.joinList(fl.toArray(),"&");
		}
		List<SortSpec> sl = mLoginProfile.getSortList();
		if (sl != null) {
			if (fl != null) {
				reqString += "&";
			}
			reqString += joinList(sl.toArray(),"&");
		}
		if (reqString.length() == 0) {
			reqString = "max=0";
		}
		tcLog.d(getClass().getName(), "loadTickets reqString = " + reqString);
		try {
			final JSONArray jsonTicketlist = tracClient.Query(reqString);

			tcLog.d(getClass().getName(), jsonTicketlist.toString());
			final int count = jsonTicketlist.length();

			if (count > 0) {
				int tickets[] = new int[count];
				for (int i = 0; i < count; i++) {
					Ticket t = null;
					try {
						tickets[i] = jsonTicketlist.getInt(i);
						t = new Ticket(tickets[i]); 
						mTickets.putTicket(t);
					} catch (JSONException e) {
						tickets[i] = -1;
					} finally {
						mTickets.ticketList.add(i, t);
					}
				}
				tcLog.d(getClass().getName(), "loadTicketList ticketlist loaded");
				new Thread() {
					@Override
					public void run() {
						try {
							loadTicketContent(mTickets);
						} catch (Exception e) {
							tcLog.e(getClass().getName(), "Exception in ticketContentLoad", e);
						} finally {
							tcLog.d(getClass().getName(), "loadTicketList content loaded");
						}
					}
				}.start();
			}
		} catch (JSONRPCException e) {
			popup_warning(R.string.connerr,e.getMessage());
		}		
		tcLog.e(getClass().getName(), "In main thread again count = "+mTickets.getTicketCount());
		if (mTickets.getTicketCount() == 0) {
			popup_warning(R.string.notickets,null);
		}
		return mTickets;
	}
	
	 private void loadTicketContent(Tickets tl) throws TicketLoadException {
		tcLog.d(getClass().getName(), "loadTicketContent");
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
			}
			notify_datachanged();
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
}