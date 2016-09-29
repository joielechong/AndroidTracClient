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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.mfvl.mfvllib.MyLog;
import com.mfvl.mfvllib.MyProgressBar;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;

import static com.mfvl.trac.client.Const.*;

interface TcBaseInterface {
    Deque<Message> getMessageQueue();
}

@SuppressLint("Registered")
class TcBaseActivity extends AppCompatActivity implements Handler.Callback, InterFragmentListener {
    static boolean debug = false; // disable menuoption at startup
    Handler tracStartHandler = null;
    Messenger mMessenger = null;
    MyHandlerThread mHandlerThread = null;
    TicketModel tm = null;
    private MyProgressBar progressBar = null;
    private boolean isPaused;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.logCall();
        mHandlerThread = new MyHandlerThread("IncomingHandler");
        mHandlerThread.start();
        isPaused = false;
        tracStartHandler = new Handler(mHandlerThread.getLooper(), this);
        mMessenger = new Messenger(tracStartHandler);
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
        MyLog.logCall();
        isPaused = false;
        Deque<Message> msgQueue = ((TcBaseInterface) this).getMessageQueue();
        MyLog.d(msgQueue);
        while (!msgQueue.isEmpty()) {
            Message m = msgQueue.poll();
            if (m != null) {
                tracStartHandler.sendMessage(m);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MyLog.logCall();
        isPaused = true;
    }

    @Override
    protected void onDestroy() {
        MyLog.d("isFinishing = " + isFinishing());
        if (isFinishing()) {
            mHandlerThread.quit();
        }
        super.onDestroy();
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        MyLog.d(frag + " this = " + this);

        if (frag instanceof TracClientFragment) {
            ((TracClientFragment) frag).onNewTicketModel(tm);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        MyLog.d("msg = " + msg);
        return isPaused && msg.what != MSG_REQUEST_TICKET_COUNT ? queueMessage(msg) : processMessage(msg);
    }

    private synchronized boolean queueMessage(Message msg) {
        MyLog.d("msg = " + msg);
        Deque<Message> msgQueue = ((TcBaseInterface) this).getMessageQueue();
        Message m = Message.obtain(msg);
        msgQueue.add(m);
        MyLog.d(msgQueue);
        return true;
    }

    boolean processMessage(Message msg) {
        MyLog.d("msg = " + msg.what);
        switch (msg.what) {
            case MSG_START_PROGRESSBAR:
                final String message = (String) msg.obj;
                synchronized (this) {
                    //MyLog.d("handleMessage msg = START_PROGRESSBAR string = "+message);
                    if (progressBar == null) {
                        progressBar = new MyProgressBar(this,message);
                    }
                }
                break;

            case MSG_STOP_PROGRESSBAR:
                synchronized (this) {
                    // MyLog.d("handleMessage msg = STOP_PROGRESSBAR "+progressBar+" "+TracStart.this.isFinishing());
                    if (progressBar != null) {
                        if (!isFinishing()) {
                            progressBar.dismiss();
                        }
                        progressBar = null;
                    }
                }
                break;

            default:
                return false;
        }
        return true;
    }

    Bundle makeArgs() {
        return new Bundle();
    }

    ArrayList<FilterSpec> parseFilterString(String filterString) {
        final ArrayList<FilterSpec> filter = new ArrayList<>();

        if (filterString.length() > 0) {
            String[] fs;

            try {
                fs = filterString.split("&");
            } catch (final IllegalArgumentException e) {
                fs = new String[1];
                fs[0] = filterString;
            }
            final String[] operators = getResources().getStringArray(R.array.filter2_choice);

            for (final String f : fs) {
                filter.add(new FilterSpecImpl(f, operators));
            }
        }
        return filter;
    }

    ArrayList<SortSpec> parseSortString(String sortString) {
        final ArrayList<SortSpec> sl = new ArrayList<>();

        if (sortString.length() > 0) {
            String[] sort;

            try {
                sort = sortString.split("&");
            } catch (final IllegalArgumentException e) {
                sort = new String[1];
                sort[0] = sortString;
            }
            for (int i = 0; i < sort.length; i++) {
                final String s = sort[i];

                if (s.startsWith("order=")) {
                    final String veld = s.substring(6);
                    boolean richting = true;

                    if (i + 1 < sort.length) {
                        final String s1 = sort[i + 1];

                        if ("desc=1".equalsIgnoreCase(s1)) {
                            richting = false;
                            i++;
                        }
                    }
                    sl.add(new SortSpec(veld, richting));
                }
            }
        }
        return sl;
    }

    @Override
    public void enableDebug() {
        // MyLog.d("enableDebug");
        debug = true;
        invalidateOptionsMenu();
        MyLog.toast("Debug enabled");
    }

	@Override
	public boolean debugEnabled() {
		return debug;
	}

    @Override
    public void onChooserSelected(OnFileSelectedListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onUpdateTicket(Ticket ticket) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void refreshOverview() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void startProgressBar(int resid) {
        startProgressBar(getString(resid));
    }

    void startProgressBar(String message) {
        MyLog.d(message);
        try {
            tracStartHandler.obtainMessage(MSG_START_PROGRESSBAR, message).sendToTarget();
        } catch (NullPointerException e) {
            MyLog.e("NullPointerException", e);
        }
    }

    @Override
    public void stopProgressBar() {
        MyLog.logCall();
        try {
            tracStartHandler.obtainMessage(MSG_STOP_PROGRESSBAR).sendToTarget();
        } catch (Exception e) {
            MyLog.e("Exception", e);
        }
    }

    @Override
    public TicketListAdapter getAdapter() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void getTicket(int ticknr, OnTicketLoadedListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void refreshTicket(int ticknr) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getNextTicket(int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getPrevTicket(int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getTicketCount() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getTicketContentCount() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void updateTicket(Ticket t, String action, String comment, String veld, String waarde, boolean notify, Map<String, String> modVeld) throws Exception {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int createTicket(Ticket t, boolean notify) throws Exception {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void listViewCreated() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Handler getHandler() {
        return tracStartHandler;
    }

    @Override
    public boolean getCanWriteSD() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void getAttachment(Ticket t, String filename, onAttachmentCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addAttachment(Ticket ticket, Uri uri, onTicketCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }
}
