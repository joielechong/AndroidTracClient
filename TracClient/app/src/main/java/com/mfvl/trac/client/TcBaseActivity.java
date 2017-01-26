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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import com.mfvl.mfvllib.MyLog;
import com.mfvl.mfvllib.MyProgressBar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.setCanWriteSD;

abstract class TcBaseActivity extends AppCompatActivity implements Handler.Callback, ActivityCompat.OnRequestPermissionsResultCallback,
        InterFragmentListener {
    private static final int REQUEST_CODE_WRITE_EXT = 175;
    static boolean debug = false; // disable menuoption at startup
    Handler tracStartHandler = null;
    Messenger mMessenger = null;
    TicketModel tm = null;
    private HandlerThread mHandlerThread = null;
    private ProgressDialog progressBar = null;
    private boolean isPaused = false;

    private void createHandler() {
        MyLog.logCall();
        mHandlerThread = new MyHandlerThread("IncomingHandler");
        mHandlerThread.start();
        tracStartHandler = new Handler(mHandlerThread.getLooper(), this);
        mMessenger = new Messenger(tracStartHandler);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.logCall();
        TracGlobal.initialize(getApplicationContext());
        createHandler();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
        MyLog.logCall();
        isPaused = false;
        Deque<Message> msgQueue = MsgQueueHolder.msgQueue;
        MyLog.d(msgQueue);
        while (!msgQueue.isEmpty()) {
            Message m = msgQueue.poll();
            if (m != null) {
                tracStartHandler.sendMessage(m);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            TracGlobal.setCanWriteSD(true);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permissiontitle)
                        .setMessage(R.string.permissiontext)
                        .setCancelable(false)
                        .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                tracStartHandler.obtainMessage(MSG_GET_PERMISSIONS).sendToTarget();
                            }
                        }).show();
            } else {
                tracStartHandler.obtainMessage(MSG_GET_PERMISSIONS).sendToTarget();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MyLog.d("requestCode = " + requestCode + " permissions = " + Arrays.asList(
                permissions) + " grantResults = " + Arrays.asList(grantResults));
        if (requestCode == REQUEST_CODE_WRITE_EXT) {
            // If request is cancelled, the result arrays are empty.
            setCanWriteSD((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED));
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
            tracStartHandler = null;
            mHandlerThread = null;
        }
        super.onDestroy();
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        MyLog.d(frag + " this = " + this);

        if (frag instanceof TcFragment) {
            ((TcFragment) frag).onNewTicketModel(tm);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        MyLog.d("msg = " + msg);
        return isPaused && msg.what != MSG_REQUEST_TICKET_COUNT ? queueMessage(msg) : processMessage(msg);
    }

    private synchronized boolean queueMessage(Message msg) {
        MyLog.d("msg = " + msg);
        Deque<Message> msgQueue = MsgQueueHolder.msgQueue;
        Message m = Message.obtain(msg);
        boolean retval = msgQueue.add(m);
        MyLog.d(msgQueue);
        return retval;
    }

    @SuppressLint("InlinedApi")
    boolean processMessage(Message msg) {
        MyLog.d("msg = " + msg.what);
        switch (msg.what) {
            case MSG_START_PROGRESSBAR:
                final String message = (String) msg.obj;
                synchronized (this) {
                    //MyLog.d("handleMessage msg = START_PROGRESSBAR string = "+message);
                    if (progressBar == null) {
                        progressBar = new MyProgressBar(this, message);
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

            case MSG_GET_PERMISSIONS:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_EXT);
                break;

            default:
                return false;
        }
        return true;
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
                String veld = null;
                for (int i = operators.length - 1; i >= 0 && veld == null; i--) {
                    final String op = operators[i];
                    final int index = f.indexOf(op);

                    if (index > 0) {
                        veld = f.substring(0, index);
                        final String waarde = f.substring(index + op.length());
                        filter.add(new FilterSpec(veld, op, waarde));
                    }
                }
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
    public ArrayAdapter<Ticket> getAdapter() {
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
    public void getAttachment(Ticket t, String filename, onAttachmentCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addAttachment(Ticket ticket, Uri uri, onTicketCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }

    static class MsgQueueHolder {
        static final Deque<Message> msgQueue = new ArrayDeque<>();
    }
}
