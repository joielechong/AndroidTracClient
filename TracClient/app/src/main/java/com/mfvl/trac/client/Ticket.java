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


import java.io.Serializable;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private final int _ticknr;
    private boolean _hasdata = false;
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
                String veld;

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
