package com.mfvl.trac.client;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;
import java.util.Map;

import static com.mfvl.trac.client.Const.*;

public abstract class TcBaseActivity extends AppCompatActivity implements Handler.Callback, InterFragmentListener {
    Handler tracStartHandler = null;
    Messenger mMessenger = null;
    MyHandlerThread mHandlerThread = null;
    TicketModel tm = null;
    private ProgressDialog progressBar = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.logCall();
        mHandlerThread = new MyHandlerThread("IncomingHandler");
        mHandlerThread.start();
        tracStartHandler = new Handler(mHandlerThread.getLooper(), this);
        mMessenger = new Messenger(tracStartHandler);
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
        MyLog.d("msg = " + msg.what);
        switch (msg.what) {
            case MSG_START_PROGRESSBAR:
                final String message = (String) msg.obj;
                synchronized (this) {
                    //MyLog.d("handleMessage msg = START_PROGRESSBAR string = "+message);
                    if (progressBar == null) {
                        progressBar = new ProgressDialog(this) {
                            @Override
                            public void onStop() {
                                super.onStop();
                                MyLog.logCall();
                                stopProgressBar();
                            }
                        };
                        progressBar.setCancelable(true);
                        if (message != null) {
                            progressBar.setMessage(message);
                        }
                        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        if (!isFinishing()) {
                            progressBar.show();
                        }
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
                filter.add(new FilterSpec(f, operators));
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
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onChooserSelected(OnFileSelectedListener oc) {
        throw new RuntimeException("not implemented");

    }

    @Override
    public void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile, boolean bewaren) {
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
