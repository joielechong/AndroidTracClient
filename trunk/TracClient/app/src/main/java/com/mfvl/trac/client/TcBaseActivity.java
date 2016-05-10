package com.mfvl.trac.client;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.mfvl.mfvllib.MyLog;

import java.util.Map;

public abstract class TcBaseActivity extends AppCompatActivity implements Handler.Callback, InterFragmentListener {
    protected Handler tracStartHandler = null;
    protected Messenger mMessenger = null;
    protected MyHandlerThread mHandlerThread = null;
    protected TicketModel tm = null;

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

    public boolean handleMessage(Message msg) {
        MyLog.logCall();
        return false;
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
    public void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile) {
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
        throw new RuntimeException("not implemented");

    }

    @Override
    public void stopProgressBar() {
        throw new RuntimeException("not implemented");

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
