/*
 * Copyright (C) 2014-2016 Michiel van Loon
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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
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
import java.util.concurrent.locks.ReentrantLock;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

interface OnTicketModelListener {
    void onTicketModelLoaded(TicketModel tm);
}

public class RefreshService extends Service implements Handler.Callback {


    public static final String refreshAction = "LIST_REFRESH";
    private final static String TICKET_GET = "GET";
    private final static String TICKET_CHANGE = "CHANGE";
    private final static String TICKET_ATTACH = "ATTACH";
    private final static String TICKET_ACTION = "ACTION";
    private static final int notifId = 1234;
    private static int timerStart;
    private static int timerPeriod;
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    private final IBinder mBinder = new RefreshBinder();
    private Timer monitorTimer = null;
    private MyHandlerThread mHandlerThread = null;
    private Handler mServiceHandler;
    private LoginProfile mLoginProfile = null;
    private TracHttpClient tracClient = null;
    private NotificationManager mNotificationManager;
    private Tickets mTickets = null;
    private boolean invalid = true;
    private TicketLoaderLock loadLock = null;
    private Handler tracStartHandler = null;

    @Override
    public void onCreate() {
        tcLog.logCall();

        Resources res = getResources();
        timerStart = res.getInteger(R.integer.timerStart);
        timerPeriod = res.getInteger(R.integer.timerPeriod);

        loadLock = new TicketLoaderLock();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mHandlerThread = new MyHandlerThread("ServiceHandler");
        mHandlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new Handler(mHandlerThread.getLooper(), this);
    }

    /**
     * This is to start the Service. It will remain inactive until bound from TracStart.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tcLog.d("intent = " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        tcLog.logCall();
        stopTimer();
        mHandlerThread.interrupt();
        mHandlerThread.quit();
        mHandlerThread = null;
        mNotificationManager.cancel(notifId);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        int cmd = intent.getIntExtra(INTENT_CMD, -1);
        tcLog.d("intent = " + intent + " cmd = " + cmd);
        if (cmd != -1) {
            int arg1 = 0;
            int arg2 = 0;
            Object obj = null;
            if (intent.hasExtra(INTENT_ARG1)) {
                arg1 = intent.getIntExtra(INTENT_ARG1, -1);
            }
            if (intent.hasExtra(INTENT_ARG2)) {
                arg2 = intent.getIntExtra(INTENT_ARG2, -1);
            }
            if (intent.hasExtra(INTENT_OBJ)) {
                obj = intent.getSerializableExtra(INTENT_OBJ);
            }
            send(Message.obtain(null, cmd, arg1, arg2, obj));
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        tcLog.d("intent = " + intent);
        return false;
    }

    public void send(Message msg) {
//        tcLog.d(msg);
        mServiceHandler.sendMessage(msg);
    }

    private void stopTimer() {
//        tcLog.logCall();
        if (monitorTimer != null) {
            monitorTimer.cancel();
            tcLog.d("timertask stopped");
        }
        monitorTimer = null;
    }

    private void startTimer() {
//        tcLog.logCall();
        stopTimer();
        monitorTimer = new Timer("monitorTickets");
        monitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                tcLog.d("timertask started");
                sendMessageToUI(MSG_REQUEST_TICKET_COUNT);
            }
        }, timerStart, timerPeriod);
    }

    private void startLoadTickets() {
//        tcLog.d("loadLock = " + loadLock);
        stopTimer();
        new Thread() {
            @Override
            public void run() {
                if (!loadLock.tryLock()) {
                    loadLock.killOwner();
                    loadLock.lock();
                }
//                tcLog.d("locked: " + loadLock);
                try {
                    loadTickets();
                } catch (Exception e) {
                    tcLog.d("Exception", e);
                } finally {
                    loadLock.unlock();
//                    tcLog.d("unlock: " + loadLock);
                }

            }
        }.start();
    }

    private void check_interrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void loadTickets() {
        tcLog.d(mLoginProfile + "\ninvalid = " + invalid);
        if (invalid) {
            mTickets = new Tickets();
            mTickets.resetCache();
            String reqString = "";
            List<FilterSpec> fl = mLoginProfile.getFilterList();
            if (fl != null) {
                reqString = TracGlobal.joinList(fl.toArray(), "&");
            }
            List<SortSpec> sl = mLoginProfile.getSortList();
            if (sl != null) {
                if (fl != null) {
                    reqString += "&";
                }
                reqString += TracGlobal.joinList(sl.toArray(), "&");
            }
            if (reqString.length() == 0) {
                reqString = "max=0";
            }
            tcLog.d("reqString = " + reqString);
            try {
                final JSONArray jsonTicketlist = tracClient.Query(reqString);
                check_interrupt();
//                tcLog.d(jsonTicketlist.toString());
                final int count = jsonTicketlist.length();
                tcLog.d("ticketlist loaded");

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
                    sendMessageToUI(MSG_LOAD_FASE1_FINISHED, mTickets);
                    loadTicketContent(mTickets);
                    mServiceHandler.obtainMessage(MSG_START_TIMER).sendToTarget();
                    sendMessageToUI(MSG_LOAD_FASE2_FINISHED, mTickets);
                } else {
                    sendMessageToUI(MSG_LOAD_FASE1_FINISHED, mTickets);
                    sendMessageToUI(MSG_LOAD_FASE2_FINISHED, mTickets);
                    popup_warning(R.string.notickets, null);
                }
            } catch (JSONRPCException e) {
                tcLog.d("JSONRPCException", e);
                popup_warning(R.string.connerr, e.getMessage());
            } catch (InterruptedException e) {
                tcLog.d("InterruptedException", e);
                tcLog.toast(getString(R.string.interrupted));
                sendMessageToUI(MSG_LOAD_ABORTED, mTickets);
            } catch (Exception e) {
                tcLog.d("Exception", e);
                sendMessageToUI(MSG_LOAD_ABORTED, mTickets);
                popup_warning(R.string.connerr, e.getMessage());
            }
        } else {
            sendMessageToUI(MSG_LOAD_FASE1_FINISHED, mTickets);
            sendMessageToUI(MSG_LOAD_FASE2_FINISHED, mTickets);
        }
//        invalid = false;
    }

    private void loadTicketContent(Tickets tl) throws Exception {
//        tcLog.logCall();
        int count = tl.getTicketCount();
//        tcLog.d("count = " + count + " " + tl);

        for (int j = 0; j < count; j += ticketGroupCount) {
            final JSONArray mc = new JSONArray();

            for (int i = j; i < (j + ticketGroupCount < count ? j + ticketGroupCount : count); i++) {
                buildCall(mc, tl.ticketList.get(i).getTicketnr());
            }
            try {
                final JSONArray mcresult = tracClient.callJSONArray("system.multicall", mc);
                // tcLog.d("mcresult = " + mcresult);
                check_interrupt();
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
                            if ((TICKET_GET + "_" + thisTicket).equals(id)) {
                                t.setFields(result.getJSONObject(3));
                            } else if ((TICKET_CHANGE + "_" + thisTicket).equals(id)) {
                                t.setHistory(result);
                            } else if ((TICKET_ATTACH + "_" + thisTicket).equals(id)) {
                                t.setAttachments(result);
                            } else if ((TICKET_ACTION + "_" + thisTicket).equals(id)) {
                                t.setActions(result);
                            } else {
                                tcLog.d("unexpected response = " + result);
                            }
                        }
                        check_interrupt();
                    } catch (final JSONException e1) {
                        tcLog.e("JSONException thrown innerloop j=" + j + " k=" + k, e1);
                    }
                }
            } catch (final JSONRPCException e) {
                tcLog.e("JSONRPCException thrown outerloop j=" + j, e);
            } finally {
                tcLog.d("loop " + tl.getTicketContentCount());
            }
            notify_datachanged();
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

    private Tickets changedTickets(final String isoTijd) {
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
                for (int i = 0; i < jsonTicketlist.length(); i++) {
                    int ticknr = jsonTicketlist.getInt(i);
                    t.addTicket(new Ticket(ticknr));
                }
                loadTicketContent(t);
            }
            return t;
        } catch (Exception e) {
            tcLog.d("getChanges exception", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(final Message msg) {
        tcLog.d("msg = " + msg.what);

        switch (msg.what) {
            case MSG_REMOVE_NOTIFICATION:
                mNotificationManager.cancel(notifId);
            case MSG_START_TIMER:
                startTimer();
                break;

            case MSG_STOP_TIMER:
                stopTimer();
                break;

            case MSG_SEND_TICKET_COUNT:
                if (msg.arg1 > 0) {
                    Tickets tl = changedTickets((String) msg.obj);
                    if (tl != null && tl.ticketList.size() > 0) {
                        mNotificationManager.notify(notifId, new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.traclogo)
                                .setAutoCancel(true)
                                .setContentTitle(RefreshService.this.getString(R.string.notifmod))
                                .setTicker(RefreshService.this.getString(R.string.foundnew))
                                .setContentText(RefreshService.this.getString(R.string.foundnew))
                                .setContentIntent(PendingIntent.getActivity(this, -1,
                                        new Intent(this, Refresh.class).setAction(refreshAction),PendingIntent.FLAG_UPDATE_CURRENT))
                                .setSubText(tl.ticketList.toString())
                                .build());
                        // tcLog.d( "Notification sent");
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
                    TicketModel.getInstance(tracClient, new OnTicketModelListener() {
                        @Override
                        public void onTicketModelLoaded(TicketModel tm) {
                            dispatchMessage(Message.obtain(null, MSG_SET_TICKET_MODEL, tm));
                        }
                    });
                } else {
                    invalid = true;
                }
                startLoadTickets();
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

    private void dispatchMessage(final Message msg) {
        msg.setTarget(tracStartHandler);
        tcLog.d("msg = " + msg.what);
        msg.sendToTarget();
    }

    public void setTracStartHandler(final Handler tsh) {
        tracStartHandler = tsh;
    }

    private void sendMessageToUI(final int message) {
        dispatchMessage(Message.obtain(null, message));
    }

    private void sendMessageToUI(final int message, Object o) {
        dispatchMessage(Message.obtain(null, message, o));
    }

    private void sendMessageToUI(final int message, int arg1, int arg2, Object o) {
        dispatchMessage(Message.obtain(null, message, arg1, arg2, o));
    }

    private class TicketLoaderLock extends ReentrantLock {
        TicketLoaderLock() {
            super();
//            tcLog.logCall();
        }

        public void killOwner() {
//            tcLog.logCall();
            Thread t = super.getOwner();
            t.interrupt();
            tcLog.toast(RefreshService.this.getString(R.string.tryinterrupt));
        }
    }

    public class RefreshBinder extends Binder {
        RefreshService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RefreshService.this;
        }
    }
}
