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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.concurrent.Semaphore;


interface onTicketCompleteListener {
    void onComplete(Ticket t);
}

interface onAttachmentCompleteListener {
    void onComplete(byte[] data);
}

public class Ticket implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -3915928655754922097L;
    private final int _ticknr;
    private final Semaphore actionLock = new Semaphore(1, true);
    private JSONObject _velden;
    private JSONArray _history;
    private JSONArray _attachments;
    private JSONArray _actions;
    private boolean _hasdata = false;

    public Ticket(final JSONObject velden) {
        tcLog.d("Ticket = " + velden);

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
            if (actionLock.availablePermits() == 0) {
                actionLock.release();
            }
        }
    }

    public void setActions(JSONArray actions) {
        _actions = actions;
        if (actionLock.availablePermits() == 0) {
            actionLock.release();
        }
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
            tcLog.e("Exception", e);
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

                } catch (final Exception ignored) {
                }
            }
        } catch (final JSONException e) {
            tcLog.e("velden failed", e);
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
                tcLog.e("history failed", e);
            }
        }
        for (int j = 0; j < _attachments.length(); j++) {
            JSONArray bijlage;

            try {
                bijlage = _attachments.getJSONArray(j);
                tekst += "bijlage " + (j + 1) + ": " + toonTijd(bijlage.getJSONObject(3)) + " - " + bijlage.getString(4) + " - "
                        + bijlage.getString(0) + " - " + bijlage.getString(1) + "\n";
            } catch (final JSONException e) {
                tcLog.e("attachment failed", e);
            }
        }
        return tekst;
    }
}