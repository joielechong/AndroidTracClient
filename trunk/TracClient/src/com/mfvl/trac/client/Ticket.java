/*
 * Copyright (C) 2013,2014 Michiel van Loon
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


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Base64;

/*
interface onTicketCompleteListener {
    void onComplete(Ticket t);
}
*/
interface onAttachmentCompleteListener {
    void onComplete(byte[] data);
}


public class Ticket implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -3915928655754922097L;

    private JSONObject _velden;
    private JSONArray _history;
    private JSONArray _attachments;
    private JSONArray _actions;
    private int _ticknr;
    private boolean _hasdata = false;
    private boolean _isloading = false;
    private static Semaphore available = new Semaphore(1, true);
    private final Semaphore actionLock = new Semaphore(1, true);
    private String rpcerror = null;

    /* static */private TracHttpClient req = null;

	
    public Ticket(final JSONObject velden) {
        tcLog.d( "Ticket = " + velden);

        _ticknr = -1;
        _velden = velden;
        _history = null;
        _attachments = null;
        _actions = null;
        actionLock.acquireUninterruptibly();
        _hasdata = true;
    }

	public Ticket(final int ticknr) {
        _ticknr = ticknr;
        _velden = null;
        _history = null;
        _attachments = null;
        _actions = null;
        actionLock.acquireUninterruptibly();
        _hasdata = false;
	}
/*	
    private void refresh(TracStart context, onTicketCompleteListener oc) {
        tcLog.i( "refresh Ticketnr = " + _ticknr);
        actionLock.release();
        available.release();
        loadTicketData(context, oc);
    }
*/
    public int getTicketnr() {
        return _ticknr;
    }
	
	public JSONObject getVelden() {
		return _velden;
	}

    @Override
    public String toString() {
        if (_velden == null) {
            return _ticknr + "";
        }
        try {
            return _ticknr + (_attachments.length() > 0 ? "+" : "") + " - " + _velden.getString("status") + " - "
                    + _velden.getString("summary");
        } catch (final JSONException e) {
            return _ticknr + "";
        }
    }
/*
    private void loadTicketData_x(TracStart context, final onTicketCompleteListener oc) {
        tcLog.i( "ticketnr = " + _ticknr);
        actionLock.acquireUninterruptibly();
        _isloading = true;
        new Thread("loadTicketData") {
            @Override
            public void run() {
                available.acquireUninterruptibly();
                req = TracHttpClient.getInstance();

                try {
                    final JSONArray mc = new JSONArray();

                    mc.put(new TracJSONObject().makeComplexCall(TICKET_GET, "ticket.get", _ticknr));
                    mc.put(new TracJSONObject().makeComplexCall(TICKET_CHANGE, "ticket.changeLog", _ticknr));
                    mc.put(new TracJSONObject().makeComplexCall(TICKET_ATTACH, "ticket.listAttachments", _ticknr));
                    mc.put(new TracJSONObject().makeComplexCall(TICKET_ACTION, "ticket.getActions", _ticknr));
                    final JSONArray mcresult = req.callJSONArray("system.multicall", mc);

                    _hasdata = false;
                    _velden = null;
                    _history = null;
                    _attachments = null;
                    _actions = null;
                    for (int i = 0; i < mcresult.length(); i++) {
                        try {
                            final JSONObject res = mcresult.getJSONObject(i);
                            final String id = res.getString("id");
                            final JSONArray result = res.getJSONArray("result");

                            if (id.equals(TICKET_GET)) {
                                _velden = result.getJSONObject(3);
                            } else if (id.equals(TICKET_CHANGE)) {
                                _history = result;
                            } else if (id.equals(TICKET_ATTACH)) {
                                _attachments = result;
                            } else if (id.equals(TICKET_ACTION)) {
                                _actions = result;
                                actionLock.release();
                            } else {
                                tcLog.i( "loadTicketData, unexpected response = " + result);
                            }
                        } catch (final Exception e1) {
                            tcLog.e( "loadTicketData error while reading response", e1);
                        }
                    }
                    _hasdata = _velden != null && _history != null && _actions != null;
                    _isloading = false;
                    if (oc != null) {
                        available.release();
                        oc.onComplete(Ticket.this);
                    }
                } catch (final Exception e) {
                    tcLog.i( "loadTicketData exception", e);
                } finally {
                    available.release();
                }
            }
        }.start();
    }

    public void getAttachment(final String filename, final onAttachmentCompleteListener oc) {
        final Thread networkThread = new Thread("getAttachment") {
            @Override
            public void run() {
                available.acquireUninterruptibly();
                if (oc != null) {
                    try {
                        oc.onComplete(TracHttpClient.getAttachment(_ticknr, filename));
                    } catch (final Exception e) {
                        tcLog.e(getClass().getName() + "getAttachment", e.toString());
                    } finally {
                        available.release();
                    }
                }
            }
        };

        networkThread.start();
        try {
            networkThread.join();
            if (rpcerror != null) {
                throw new RuntimeException(rpcerror);
            }
        } catch (final Exception ignored) {}
    }

    public void addAttachment(final String filename, final TracStart context, final onTicketCompleteListener oc) {
        tcLog.i(this.getClass().getName() + ".addAttachment", filename);
        new Thread() {
            @Override
            public void run() {
                available.acquireUninterruptibly();
                req = TracHttpClient.getInstance();
                final File file = new File(filename);
                final int bytes = (int) file.length();
                final byte[] data = new byte[bytes];

                try {
                    final InputStream is = new FileInputStream(file);

                    is.read(data);
                    is.close();
                    final String b64 = Base64.encodeToString(data, Base64.DEFAULT);
                    final JSONArray ar = new JSONArray();

                    ar.put(_ticknr);
                    ar.put(file.getName());
                    ar.put("");
                    final JSONArray ar1 = new JSONArray();

                    ar1.put("binary");
                    ar1.put(b64);
                    final JSONObject ob = new JSONObject();

                    ob.put("__jsonclass__", ar1);
                    ar.put(ob);
                    ar.put(true);
                    final String retfile = req.callString("ticket.putAttachment", ar);

                    tcLog.i(this.getClass().getName() + ".putAttachment", retfile);
                    actionLock.release();
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadTicketData(context, null);
                            if (oc != null) {
                                oc.onComplete(Ticket.this);
                            }
                        }
                    });
                } catch (final Exception e) {
                    tcLog.i(this.getClass().getName() + ".addAttachment", e.toString());
                } finally {
                    actionLock.release();
                    available.release();
                }
            }
        }.start();
    }
*/
    public String getString(final String veld) throws JSONException {
        try {
            return _velden.getString(veld);
        } catch (final NullPointerException e) {
            return "";
        }
    }

    public JSONObject getJSONObject(final String veld) throws JSONException {
        return _velden.getJSONObject(veld);
    }

    public JSONArray getFields() {
        return _velden.names();
    }

    public void setFields(JSONObject velden) {
        _velden = velden;
        _hasdata = true;
    }

    public JSONArray getHistory() {
        return _history;
    }

    public void setHistory(JSONArray history) {
        _history = history;
    }

    public JSONArray getActions() {
        actionLock.acquireUninterruptibly();
        try {
            return _actions;
        } finally {
            actionLock.release();
        }
    }

    public void setActions(JSONArray actions) {
        _actions = actions;
        actionLock.release();
    }

    public JSONArray getAttachments() {
        return _attachments;
    }

    public void setAttachments(JSONArray attachments) {
        _attachments = attachments;
    }

    public String getAttachmentFile(int nr) throws JSONException {
        return _attachments.getJSONArray(nr).getString(0);
    }

    public boolean hasdata() {
        return _hasdata;
    }

    public boolean isloading() {
        return _isloading;
    }
/*
    public int create(final TracStart context, final boolean notify) throws Exception {
        if (_ticknr != -1) {
            throw new RuntimeException("Call create ticket not -1");
        }
        tcLog.i( "create: " + _velden.toString());
        final String s = _velden.getString("summary");
        final String d = _velden.getString("description");

        _velden.remove("summary");
        _velden.remove("description");

        final Thread networkThread = new Thread() {
            @Override
            public void run() {
                available.acquireUninterruptibly();
                try {
                    final int newticknr = TracHttpClient.createTicket(s, d, _velden);

                    _ticknr = newticknr;
                    actionLock.release();
                    loadTicketData(context, null);
                } catch (final JSONRPCException e) {
                    try {
                        final JSONObject o = new JSONObject(e.getMessage());

                        rpcerror = o.getString("message");
                    } catch (final JSONException e1) {
                        tcLog.e( "create failed", e1);
                        rpcerror = context.getString(R.string.invalidJson);
                    }
                }
                available.release();
            }
        };

        networkThread.start();
        try {
            networkThread.join();
            if (rpcerror != null) {
                throw new RuntimeException(rpcerror);
            }
        } catch (final Exception e) {
            throw e;
        }
        if (_ticknr == -1) {
            throw new RuntimeException(context.getString(R.string.noticketUnk));
        }

        return _ticknr;
    }
*/
    // update is called from within a non UI thread
/*
    public void update(String action, String comment, String veld, String waarde, final boolean notify, final TracStart context,Map<String, String> modVeld) throws Exception {
        tcLog.d( "update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "' " + modVeld);
        // tcLog.d( "_velden voor = " + _velden);
        if (_ticknr == -1) {
            throw new IllegalArgumentException(context.getString(R.string.invtick) + " " + _ticknr);
        }
        if (action == null) {
            throw new NullPointerException(context.getString(R.string.noaction));
        }
        _velden.put("action", action);
        if (waarde != null && veld != null && !"".equals(veld) && !"".equals(waarde)) {
            _velden.put(veld, waarde);
        }
        if (modVeld != null) {
            final Iterator<Entry<String, String>> i = modVeld.entrySet().iterator();

            while (i.hasNext()) {
                final Entry<String, String> e = i.next();
                // tcLog.d( e.toString());
                final String v = e.getKey();
                final String w = e.getValue();

                _velden.put(v, w);
            }
        }

        final String cmt = comment == null ? "" : comment;

        _velden.remove("changetime");
        _velden.remove("time");

        available.acquireUninterruptibly();
        try {
            if (Tickets.url != null) {
                TracHttpClient.updateTicket(_ticknr, cmt, _velden, notify);
                actionLock.release();
                loadTicketData(context, null);
            }
        } catch (final JSONRPCException e) {
            tcLog.e( "JSONRPCException", e);
            rpcerror = e.getMessage();
        } finally {
            available.release();
        }

        if (rpcerror != null) {
            throw new RuntimeException(rpcerror);
        }
    }
*/
    private String toonTijd(final JSONObject v) {
        try {
            return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
        } catch (final Exception e) {
            tcLog.e( e.toString());
            return "";
        }
    }

    public String toText() {
        String tekst = "Ticket: " + _ticknr;

        try {
            final JSONArray fields = _velden.names();

            tekst += " " + _velden.getString("summary") + "\n\n";
            final int count = fields.length();

            for (int i = 0; i < count; i++) {
                String veld = "veld " + i;

                try {
                    veld = fields.getString(i);
                    if ("summary".equals(veld) || "_ts".equals(veld)) {// skip
                    } else if ("time".equals(veld) || "changetime".equals(veld)) {
                        tekst += veld + ":\t" + toonTijd(_velden.getJSONObject(veld)) + "\n";
                    } else if (_velden.getString(veld).length() > 0) {
                        tekst += veld + ":\t" + _velden.getString(veld) + "\n";
                    }

                } catch (final Exception ignored) {}
            }
        } catch (final JSONException e) {
            tcLog.e( "toText velden failed", e);
        }
        for (int j = 0; j < _history.length(); j++) {
            JSONArray cmt;

            try {
                cmt = _history.getJSONArray(j);
                if ("comment".equals(cmt.getString(2)) && cmt.getString(4).length() > 0) {
                    tekst += "comment: " + toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(1) + " - " + cmt.getString(4)
                            + "\n";
                }
            } catch (final JSONException e) {
                tcLog.e( "toText history failed", e);
            }
        }
        for (int j = 0; j < _attachments.length(); j++) {
            JSONArray bijlage;

            try {
                bijlage = _attachments.getJSONArray(j);
                tekst += "bijlage " + (j + 1) + ": " + toonTijd(bijlage.getJSONObject(3)) + " - " + bijlage.getString(4) + " - "
                        + bijlage.getString(0) + " - " + bijlage.getString(1) + "\n";
            } catch (final JSONException e) {
                tcLog.e( "toText atachment failed", e);
            }
        }
        return tekst;
    }
}