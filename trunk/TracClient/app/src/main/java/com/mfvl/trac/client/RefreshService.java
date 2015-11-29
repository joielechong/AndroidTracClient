/*
 * Copyright (C) 2014,2015 Michiel van Loon
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.mfvl.trac.client.Const.*;

public class RefreshService extends Service implements Handler.Callback {

    private final static String TICKET_GET = "GET";
    private final static String TICKET_CHANGE = "CHANGE";
    private final static String TICKET_ATTACH = "ATTACH";
    private final static String TICKET_ACTION = "ACTION";

    private static int timerStart;
    private static int timerPeriod;
    public static final String refreshAction = "LIST_REFRESH";
    private Timer monitorTimer = null;
    private static final int notifId = 1234;

    private MyHandlerThread mHandlerThread = null;
    private Handler mServiceHandler;
    private LoginProfile mLoginProfile = null;
    private TracHttpClient tracClient = null;
    private NotificationManager mNotificationManager;
    private TicketModel tm = null;
    private Tickets mTickets = null;
    private boolean invalid = true;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    private final IBinder mBinder = new RefreshBinder();

    public class RefreshBinder extends Binder {
        RefreshService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RefreshService.this;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(final Message msg) {
        tcLog.d("msg = " + msg);

        switch (msg.what) {
            case MSG_START_TIMER:
                stopTimer();
                startTimer();
                break;

            case MSG_STOP_TIMER:
                stopTimer();
                break;

            case MSG_REMOVE_NOTIFICATION:
                mNotificationManager.cancel(notifId);
                stopTimer();
                startTimer();
                break;

            case MSG_SEND_TICKET_COUNT:
                if (msg.arg1 > 0) {
                    Tickets tl = changedTickets((String) msg.obj);
                    if (tl != null && tl.ticketList.size() > 0) {
                        try {
                            final Intent launchIntent = new Intent(RefreshService.this, Refresh.class);

                            launchIntent.setAction(refreshAction);
                            final PendingIntent pendingIntent = PendingIntent.getActivity(RefreshService.this, -1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                            final Notification notification = new NotificationCompat.Builder(RefreshService.this)
                                    .setSmallIcon(R.drawable.traclogo)
                                    .setAutoCancel(true)
                                    .setContentTitle(RefreshService.this.getString(R.string.notifmod))
                                    .setTicker(RefreshService.this.getString(R.string.foundnew))
                                    .setContentText(RefreshService.this.getString(R.string.foundnew))
                                    .setContentIntent(pendingIntent)
                                    .setSubText(tl.ticketList.toString())
                                    .build();
                            mNotificationManager.notify(notifId, notification);
                            // tcLog.d( "Notification sent");
                        } catch (final IllegalArgumentException e) {
                            tcLog.e("IllegalArgumentException in notification", e);
                        }
                    }
                }
                break;

            case MSG_SEND_TICKETS:
                List<Integer> newTickets = (List<Integer>) msg.obj;
                if (newTickets == null) {
                    newTickets = new ArrayList<>();
                    newTickets.add(msg.arg1);
                }
                tcLog.d("newTickets = " + newTickets);

                if (newTickets.size() > 0) {
                    Tickets tl = new Tickets();
                    for (Integer i : newTickets) {
                        tl.addTicket(new Ticket(i));
                    }
                    try {
                        loadTicketContent(tl);
                        if (msg.arg2 != 0) {
                            sendMessageToUI(msg.arg2, tl.getTicket(msg.arg1));
                        }
                    } catch (Exception e) {
                        tcLog.e("MSG_SEND_TICKETS exception", e);
                        popup_warning(R.string.ticketnotfound, "" + tl.ticketList);
                    }
                }
                break;

            case MSG_GET_TICKET_MODEL:
                tm = getTicketModel();
                break;

            case MSG_REFRESH_LIST:
                msg.obj = null;
            case MSG_LOAD_TICKETS:
                LoginProfile lp = (LoginProfile) msg.obj;
                tcLog.d("lp = " + lp);
                tcLog.d("mLoginProfile = " + mLoginProfile);
                if (lp != null) {
                    invalid = !lp.equals(mLoginProfile);
                    mLoginProfile = lp;
                    tracClient = new TracHttpClient(mLoginProfile);
                    tm = TicketModel.getInstance(tracClient);
                } else {
                    invalid = true;
                }
                loadTickets();
                break;

            case MSG_SET_FILTER:
                if (mLoginProfile != null) {
                    mLoginProfile.setFilterList((List<FilterSpec>) msg.obj);
                }
                break;

            case MSG_SET_SORT:
                if (mLoginProfile != null) {
                    mLoginProfile.setSortList((List<SortSpec>) msg.obj);
                }
                break;

            default:
                return false;
        }
        return true;
    }

    private void dispatchMessage(Message msg) {
        msg.setTarget(TracStart.tracStartHandler);
        tcLog.d(msg.toString());
        msg.sendToTarget();
    }

    private void sendMessageToUI(int message) {
        dispatchMessage(Message.obtain(null, message));
    }

    private void sendMessageToUI(int message,Object o) {
        dispatchMessage(Message.obtain(null, message,o));
    }

    private void sendMessageToUI(int message,int arg1,int arg2,Object o) {
        dispatchMessage(Message.obtain(null, message,arg1,arg2,o));
    }

    public TicketModel getTicketModel() {
        tm = TicketModel.getInstance();
//        tcLog.d("tm = "+tm);
        return tm;
    }

    @Override
    public void onCreate() {
        tcLog.logCall();

        Resources res = getResources();
        timerStart = res.getInteger(R.integer.timerStart);
        timerPeriod = res.getInteger(R.integer.timerPeriod);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mHandlerThread = new MyHandlerThread("ServiceHandler");
        mHandlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new Handler(mHandlerThread.getLooper(),this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tcLog.d("intent = " + intent);
        return START_STICKY;
    }

    @Override
    public void onRebind(Intent intent) {
        tcLog.d("intent = " + intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        tcLog.d("intent = " + intent);
/*
        String action = intent.getAction();
        if (action != null) {
            Message msg = mServiceHandler.obtainMessage();
            switch (action) {
                case ACTION_LOAD_TICKETS:
                    msg.what = MSG_LOAD_TICKETS;
                    msg.obj = (intent.getSerializableExtra("LoginProfile"));
                    mServiceHandler.sendMessage(msg);
                    break;

                case ACTION_START_TIMER:
                    msg.what = MSG_START_TIMER;
                    mServiceHandler.sendMessage(msg);
                    break;
            }
        }
*/
        return mBinder;
    }

    public void send(Message msg) {
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        tcLog.d("intent = " + intent);
        return false;
    }

    @Override
    public void onDestroy() {
        tcLog.logCall();
        stopTimer();
        mHandlerThread.interrupt();
        mHandlerThread.quit();
        mNotificationManager.cancel(notifId);
        super.onDestroy();
    }

    private void startTimer() {
        //tcLog.logCall();
        monitorTimer = new Timer("monitorTickets");
        monitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //tcLog.d("timertask started");
                sendMessageToUI(MSG_REQUEST_TICKET_COUNT);
            }
        }, timerStart, timerPeriod);
    }

    private void stopTimer() {
        if (monitorTimer != null) {
            monitorTimer.cancel();
        }
        monitorTimer = null;
    }

    private void loadTickets() {
        tcLog.d(mLoginProfile.toString());
        if (invalid) {
            mTickets = new Tickets();
            mTickets.resetCache();
            String reqString = "";
            List<FilterSpec> fl = mLoginProfile.getFilterList();
            if (fl != null) {
                reqString = Credentials.joinList(fl.toArray(), "&");
            }
            List<SortSpec> sl = mLoginProfile.getSortList();
            if (sl != null) {
                if (fl != null) {
                    reqString += "&";
                }
                reqString += Credentials.joinList(sl.toArray(), "&");
            }
            if (reqString.length() == 0) {
                reqString = "max=0";
            }
            tcLog.d("reqString = " + reqString);
            try {
                final JSONArray jsonTicketlist = tracClient.Query(reqString);

                tcLog.d(jsonTicketlist.toString());
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
                    tcLog.d("ticketlist loaded");
                    sendMessageToUI(MSG_LOAD_FASE1_FINISHED, mTickets);
                    try {
                        loadTicketContent(mTickets);
                    } catch (Exception e) {
                        tcLog.e("Exception in ticketContentLoad", e);
                        popup_warning(R.string.connerr, e.getMessage());
                    } finally {
                        sendMessageToUI(MSG_LOAD_FASE2_FINISHED, mTickets);
                    }
                }
            } catch (JSONRPCException e) {
                popup_warning(R.string.connerr, e.getMessage());
            }
            if (mTickets.getTicketCount() == 0) {
                popup_warning(R.string.notickets, null);
            }
        } else{
            sendMessageToUI(MSG_LOAD_FASE1_FINISHED, mTickets);
            sendMessageToUI(MSG_LOAD_FASE2_FINISHED, mTickets);
        }
        invalid = false;
    }

    private void loadTicketContent(Tickets tl) throws RuntimeException {
        tcLog.logCall();
        int count = tl.getTicketCount();
        tcLog.d("count = " + count + " " + tl);

        try {
            for (int j = 0; j < count; j += ticketGroupCount) {
                final JSONArray mc = new JSONArray();

                for (int i = j; i < (j + ticketGroupCount < count ? j + ticketGroupCount : count); i++) {
                    buildCall(mc, tl.ticketList.get(i).getTicketnr());
                }
                try {
                    final JSONArray mcresult = tracClient.callJSONArray("system.multicall", mc);
                    // tcLog.d( "mcresult = " + mcresult);
                    Ticket t = null;

                    for (int k = 0; k < mcresult.length(); k++) {
                        try {
                            final JSONObject res = mcresult.getJSONObject(k);
                            final String id = res.getString("id");
                            final JSONArray result = res.getJSONArray("result");
                            final int startpos = id.indexOf("_") + 1;
                            final int thisTicket = Integer.parseInt(id.substring(startpos));

                            if (t == null || t.getTicketnr() != thisTicket)
                                t = tl.getTicket(thisTicket);
                            if (t != null) {
                                if ((TICKET_GET + "_" + thisTicket).equals(id)) {
                                    t.setFields(result.getJSONObject(3));
                                } else if ((TICKET_CHANGE + "_" + thisTicket).equals(id)) {
                                    t.setHistory(result);
                                } else if ((TICKET_ATTACH + "_" + thisTicket).equals(id)) {
                                    t.setAttachments(result);
                                } else if ((TICKET_ACTION + "_" + thisTicket).equals(id)) {
                                    t.setActions(result);
                                } else {
                                    tcLog.d( "unexpected response = " + result);
                                }
                            }
                        } catch (final JSONException e1) {
                            tcLog.e("JSONException thrown innerloop j=" + j + " k=" + k,e1);
                        }
                    }
                } catch (final JSONRPCException e) {
                    tcLog.e("JSONRPCException thrown outerloop j=" + j,e);
                }  finally {
                    tcLog.d("loop " + tl.getTicketContentCount());
                }
                notify_datachanged();
            }
        } catch (Exception e) {
            tcLog.e("Exception",e);
        }
    }

    private void notify_datachanged() {
        sendMessageToUI(MSG_DATA_CHANGED);
    }

    private void popup_warning(int messString, String addit) {
        sendMessageToUI(MSG_SHOW_DIALOG, R.string.warning, messString, addit);
    }

    private void buildCall(JSONArray multiCall, int ticknr) throws JSONException {
        multiCall
                .put(new TracJSONObject().makeComplexCall(TICKET_GET + "_" + ticknr, "ticket.get", ticknr))
                .put(new TracJSONObject().makeComplexCall(TICKET_CHANGE + "_" + ticknr, "ticket.changeLog", ticknr))
                .put(new TracJSONObject().makeComplexCall(TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr))
                .put(new TracJSONObject().makeComplexCall(TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
    }

    public Tickets changedTickets(String isoTijd) {
        try {
            final JSONArray datum = new JSONArray();

            datum.put("datetime");
            datum.put(isoTijd);
            final JSONObject ob = new JSONObject();

            ob.put("__jsonclass__", datum);
            final JSONArray param = new JSONArray();

            param.put(ob);
            final JSONArray jsonTicketlist = tracClient.callJSONArray("ticket.getRecentChanges", param);

            Tickets t = null;

            if (jsonTicketlist.length() > 0) {
                t = new Tickets();
                for (int i = 0;i<jsonTicketlist.length();i++) {
                    int ticknr = jsonTicketlist.getInt(i);
                    t.addTicket(new Ticket(ticknr));
                }
                loadTicketContent(t);
            }
            return t;
        } catch (Exception e) {
            tcLog.d("getChanges exception",e);
        }
        return null;
    }
}
