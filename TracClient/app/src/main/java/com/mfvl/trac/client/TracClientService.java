/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.mfvl.mfvllib.MyLog;

import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import static com.mfvl.trac.client.TracGlobal.*;

interface OnTicketModelListener {
    void onTicketModelLoaded(TicketModel tm);
}

interface DataChangedListener {
    void onDataChanged();

    void onFase1Completed(Tickets mTickets);

    void onFase2Completed();

    void showAlertBox(int titleres, CharSequence message);

    void loadAborted();

    void newTicketModel(TicketModel tm);

}

interface TicketCountInterface {

    void requestTicketCount();
}

public class TracClientService extends Service {

    public static final String refreshAction = "LIST_REFRESH";
    private final static String TICKET_GET = "GET";
    private final static String TICKET_CHANGE = "CHANGE";
    private final static String TICKET_ATTACH = "ATTACH";
    private final static String TICKET_ACTION = "ACTION";
    private static final int notifId = 1234;
    private final IBinder mBinder = new TcBinder();
    private final Collection<DataChangedListener> dcList = new ArrayList<>();
    private Timer monitorTimer = null;
    private LoginProfile mLoginProfile = null;
    private TracHttpClient tracClient = null;
    private NotificationManager mNotificationManager = null;
    private Tickets mTickets = null;
    private boolean invalid = true;
    private boolean profileChanged = false;
    private ReentrantLock loadLock = null;
    private TicketModel ticketModel = null;

    public boolean isProfileChanged() {
        return profileChanged;
    }

    public void resetProfileChanged() {
        this.profileChanged = false;
    }

    @Override
    public void onCreate() {
        MyLog.logCall();

        mLoginProfile = null;
        tracClient = null;
        loadLock = new TicketLoaderLock();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * This is to start the Service. It will remain inactive until bound from TracStart.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyLog.d("intent = " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        MyLog.logCall();
        stopTimer();
        removeNotification();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        MyLog.d("intent = " + intent);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MyLog.d("intent = " + intent);
        return false;
    }

    private void stopTimer() {
//        MyLog.logCall();
        if (monitorTimer != null) {
            monitorTimer.cancel();
            MyLog.d("timertask stopped");
        }
        monitorTimer = null;
    }

    private void startTimer() {
//        MyLog.logCall();
        stopTimer();
        monitorTimer = new Timer("monitorTickets");
        monitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                MyLog.d("timertask started");
                requestTicketCount();
            }
        }, timerStart, timerPeriod);
    }

    private void startLoadTickets() {
//        MyLog.d("loadLock = " + loadLock);
        stopTimer();
        new Thread() {
            @Override
            public void run() {
                MyLog.logCall();
                if (!loadLock.tryLock()) {
                    ((TicketLoaderLock) loadLock).killOwner();
                    loadLock.lock();
                }
//                MyLog.d("locked: " + loadLock);
                try {
                    loadTickets();
                } catch (Exception e) {
                    MyLog.d("Exception", e);
                } finally {
                    loadLock.unlock();
//                    MyLog.d("unlock: " + loadLock);
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
        MyLog.d(mLoginProfile + "\ninvalid = " + invalid);
        if (invalid) {
            mTickets = new Tickets();
            mTickets.resetCache();
            String reqString = "";
            List<FilterSpec> fl = mLoginProfile.getFilterList();
            if (fl != null) {
                reqString = TextUtils.join("&", fl);
            }
            List<SortSpec> sl = mLoginProfile.getSortList();
            if (sl != null) {
                if (fl != null) {
                    reqString += "&";
                }
                reqString += TextUtils.join("&", sl.toArray());
            }
            if (reqString.length() == 0) {
                reqString = "max=0";
            }
            MyLog.d("reqString = " + reqString);
            try {
                final JSONArray jsonTicketlist = tracClient.Query(reqString);
                check_interrupt();
//                MyLog.d(jsonTicketlist.toString());
                final int count = jsonTicketlist.length();
                MyLog.d("ticketlist loaded");

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
                            mTickets.getTicketList().add(i, t);
                        }
                    }
                    reportFase1Completed(mTickets);
                    loadTicketContent(mTickets);
                    startTimer();
                    reportFase2Completed();
                } else {
                    reportFase1Completed(mTickets);
                    reportFase2Completed();
                    popup_warning(getString(R.string.notickets));
                }
            } catch (JSONRPCException e) {
                MyLog.d("JSONRPCException", e);
                popup_warning(getString(R.string.connerr, e.getMessage()));
            } catch (InterruptedException e) {
                MyLog.d("InterruptedException");
                MyLog.toast(getString(R.string.interrupted));
                reportLoadAborted();
            } catch (Exception e) {
                MyLog.d("Exception", e);
                reportLoadAborted();
                popup_warning(getString(R.string.connerr, e.getMessage()));
            }
        } else {
            reportFase1Completed(mTickets);
            reportFase2Completed();
        }
//        invalid = false;
    }

    private void loadTicketContent(Tickets tl) throws Exception {
        //MyLog.d(tl);
        int count = tl.getTicketCount();
//        MyLog.d("count = " + count + " " + tl);

        for (int j = 0; j < count; j += ticketGroupCount) {
            final JSONArray mc = new JSONArray();

            for (int i = j; i < Math.min(j + ticketGroupCount, count); i++) {
                buildCall(mc, tl.getTicketList().get(i).getTicketnr());
            }
            try {
                final JSONArray mcresult = tracClient.callJSONArray("system.multicall", mc);
                //MyLog.d("mcresult = " + mcresult);
                check_interrupt();
                Ticket t = null;

                for (int k = 0; k < mcresult.length(); k++) {
                    try {
                        final JSONObject res = mcresult.getJSONObject(k);
                        //MyLog.d(res);
                        final String id = res.getString("id");
                        final int startpos = id.indexOf("_") + 1;
                        final int thisTicket = Integer.parseInt(id.substring(startpos));
                        if (t == null || t.getTicketnr() != thisTicket) {
                            t = Tickets.getTicket(thisTicket);
                        }
                        JSONObject error = null;
                        try {
                            error = res.getJSONObject("error");
                        } catch (JSONException ignore) {
                        }
                        if (error != null) {
                            throw (new Resources.NotFoundException(id.substring(startpos)));
                        }
                        final JSONArray result = res.getJSONArray("result");

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
                                MyLog.d("unexpected response = " + result);
                            }
                        }
                        check_interrupt();
                    } catch (final JSONException e1) {
                        MyLog.e("JSONException thrown innerloop j=" + j + " k=" + k, e1);
                    }
                }
            } catch (final JSONRPCException e) {
                MyLog.e("JSONRPCException thrown outerloop j=" + j, e);
            } finally {
                MyLog.d("loop " + tl.getTicketContentCount());
            }
            notify_datachanged();
        }
    }

    public void registerDataChangedListener(DataChangedListener oc) {
        MyLog.d(oc);
        if (!dcList.contains(oc)) {
            dcList.add(oc);
        }
    }

    public void unregisterDataChangedListener(DataChangedListener oc) {
        MyLog.d(oc);
        if (dcList.contains(oc)) {
            dcList.remove(oc);
        }
    }

    private void reportNewTicketModel(TicketModel tm) {
        MyLog.logCall();
        for (DataChangedListener oc : dcList) {
            oc.newTicketModel(tm);
        }
    }

    private void requestTicketCount() {
        for (DataChangedListener oc : dcList) {
            if (oc instanceof TicketCountInterface) {
                ((TicketCountInterface) oc).requestTicketCount();
            }
        }
    }

    private void reportLoadAborted() {
        for (DataChangedListener oc : dcList) {
            oc.loadAborted();
        }
    }

    private void reportFase1Completed(Tickets tl) {
        for (DataChangedListener oc : dcList) {
            oc.onFase1Completed(tl);
        }
    }

    private void reportFase2Completed() {
        for (DataChangedListener oc : dcList) {
            oc.onFase2Completed();
        }
    }

    private void notify_datachanged() {
        MyLog.logCall();
        for (DataChangedListener oc : dcList) {
            oc.onDataChanged();
        }
    }

    private void popup_warning(CharSequence message) {
        MyLog.d(message);
        for (DataChangedListener oc : dcList) {
            oc.showAlertBox(R.string.warning, message);
        }
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
                //loadTicketContent(t);
            }
            return t;
        } catch (Exception e) {
            MyLog.d("getChanges exception", e);
        }
        return null;
    }

    public void removeNotification() {
        MyLog.logCall();
        mNotificationManager.cancel(notifId);
    }

    public void sendTicketCount(int count, String tijdstip) {
        if (count > 0) {
            Tickets tl = changedTickets(tijdstip);
            if (tl != null && tl.getTicketList().size() > 0) {
                mNotificationManager.notify(notifId, new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.traclogo)
                        .setAutoCancel(true)
                        .setContentTitle(TracClientService.this.getString(R.string.notifmod))
                        .setTicker(TracClientService.this.getString(R.string.foundnew))
                        .setContentText(TracClientService.this.getString(R.string.foundnew))
                        .setContentIntent(PendingIntent.getActivity(this, -1,
                                new Intent(this, Refresh.class).setAction(refreshAction), PendingIntent.FLAG_UPDATE_CURRENT))
                        .setSubText(tl.getTicketList().toString())
                        .build());
                // MyLog.d( "Notification sent");
            }
        }
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Nullable
    public Ticket requestTicket(int ticket) {
        MyLog.d(ticket);

        Tickets tl = new Tickets();
        tl.addTicket(new Ticket(ticket));
        try {
            loadTicketContent(tl);
            return Tickets.getTicket(ticket);
        } catch (Resources.NotFoundException e) {
            popup_warning(getString(R.string.ticketnotfound, tl.getTicketList()));
        } catch (Exception e) {
            MyLog.e("Exception", e);
        }
        return null;
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    public TicketModel getTicketModel() {
        return ticketModel;
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    public LoginProfile getLoginProfile() {
        return mLoginProfile;
    }

    public void msgLoadTickets(LoginProfile lp) {
        MyLog.d("lp = " + lp);
        MyLog.d("mLoginProfile = " + mLoginProfile);
        if (lp != null) {
            invalid = !lp.equals(mLoginProfile);
            mLoginProfile = lp;
            tracClient = new TracHttpClient(mLoginProfile);
            TicketModel.getInstance(tracClient, new OnTicketModelListener() {
                @Override
                public void onTicketModelLoaded(TicketModel tm) {
                    MyLog.logCall();
                    ticketModel = tm;
                    reportNewTicketModel(tm);
                }
            });
        } else {
            invalid = true;
        }
        profileChanged |= invalid;
        startLoadTickets();
    }

    public void setFilter(List<FilterSpec> items) {
        MyLog.d(items);
        String filterString = TextUtils.join("&", items);
        storeFilterString(filterString);
        if (mLoginProfile != null) {
            mLoginProfile.setFilterList(items);
            invalid = true;
            startLoadTickets();
        }
    }

    public void setSort(List<SortSpec> items) {
        MyLog.d(items);
        String sortString = TextUtils.join("&", items);
        storeSortString(sortString);
        if (mLoginProfile != null) {
            mLoginProfile.setSortList(items);
            invalid = true;
            startLoadTickets();
        }
    }

    private class TicketLoaderLock extends ReentrantLock {
        TicketLoaderLock() {
            super();
//            MyLog.logCall();
        }

        void killOwner() {
//            MyLog.logCall();
            Thread t = super.getOwner();
            t.interrupt();
            MyLog.toast(TracClientService.this.getString(R.string.tryinterrupt));
        }
    }

    public class TcBinder extends Binder {
        @SuppressWarnings("MethodReturnOfConcreteClass")
        TracClientService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TracClientService.this;
        }
    }
}
