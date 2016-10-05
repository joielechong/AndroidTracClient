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


import com.mfvl.mfvllib.MyLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Semaphore;

import static com.mfvl.trac.client.TracGlobal.*;

interface onTicketCompleteListener {
    void onComplete();
}

interface onAttachmentCompleteListener {
    void onComplete(byte[] data);
}

interface Ticket {
    int getTicketnr();

    JSONObject getVelden();

    String getString(final String veld) throws JSONException;

    JSONObject getJSONObject(final String veld) throws JSONException;

    void setFields(JSONObject velden);

    JSONArray getHistory();

    void setHistory(JSONArray history);

    JSONArray getActions();

    void setActions(JSONArray actions);

    JSONArray getAttachments();

    void setAttachments(JSONArray attachments);

    String getAttachmentFile(int nr) throws JSONException;

    boolean hasdata();

    String toText();
}

class NormalTicket implements Ticket {
    private final int _ticknr;
    private final Semaphore actionLock = new TcSemaphore(1, true);
    private JSONObject _velden;
    private JSONArray _history;
    private JSONArray _attachments;
    private JSONArray _actions;
    private boolean _hasdata = false;

    NormalTicket(final JSONObject velden) {
        MyLog.d("NormalTicket = " + velden);
        _ticknr = -1;
        init(velden);
    }

    NormalTicket(final int ticknr) {
        _ticknr = ticknr;
        init(null);
    }

    private void init(JSONObject velden) {
        _velden = velden;
        _history = null;
        _attachments = null;
        _actions = null;
        actionLock.acquireUninterruptibly();
        _hasdata = (velden != null);
    }

    @Override
    public int getTicketnr() {
        return _ticknr;
    }

    @Override
    public JSONObject getVelden() {
        return _velden;
    }

    @Override
    public String toString() {
        if (_velden == null) {
            return _ticknr + "";
        }
        try {
            return _ticknr + (_attachments != null && _attachments.length() > 0 ? "+" : "") + " - " + _velden.getString(
                    "status") + " - "
                    + _velden.getString("summary");
        } catch (final JSONException e) {
            return _ticknr + "";
        }
    }

    @Override
    public String getString(final String veld) throws JSONException {
        try {
            return _velden.getString(veld);
        } catch (final NullPointerException e) {
            return "";
        }
    }

    @Override
    public JSONObject getJSONObject(final String veld) throws JSONException {
        return _velden.getJSONObject(veld);
    }

    @Override
    public void setFields(JSONObject velden) {
        _velden = velden;
        _hasdata = true;
    }

    @Override
    public JSONArray getHistory() {
        return _history;
    }

    @Override
    public void setHistory(JSONArray history) {
        _history = history;
    }

    @Override
    public JSONArray getActions() {
        if (actionLock.availablePermits() == 0) {
            actionLock.acquireUninterruptibly();
            actionLock.release();
        }
        return _actions;
    }

    @Override
    public void setActions(JSONArray actions) {
        _actions = actions;
        if (actionLock.availablePermits() == 0) {
            actionLock.release();
        }
    }

    @Override
    public JSONArray getAttachments() {
        return _attachments;
    }

    @Override
    public void setAttachments(JSONArray attachments) {
        _attachments = attachments;
    }

    @Override
    public String getAttachmentFile(int nr) throws JSONException {
        return _attachments.getJSONArray(nr).getString(0);
    }

    @Override
    public boolean hasdata() {
        return _hasdata;
    }

    @Override
    public String toText() {
        String tekst = "Ticket: " + _ticknr;

        try {
            final JSONArray fields = _velden.names();

            tekst += " " + _velden.getString("summary") + "\n\n";
            final int count = fields.length();

            for (int i = 0; i < count; i++) {

                try {
                    String veld = fields.getString(i);
                    switch (veld) {
                        case "summary":
                        case "_ts":
                            break;

                        case "time":
                        case "changetime":
                            tekst += veld + ":\t" + toonTijd(_velden.getJSONObject(veld)) + "\n";
                            break;

                        default:
                            tekst += veld + ":\t" + _velden.getString(veld) + "\n";
                    }
                } catch (final Exception ignored) {
                }
            }
        } catch (final JSONException e) {
            MyLog.e("velden failed", e);
        }
        for (int j = 0; j < _history.length(); j++) {

            try {
                JSONArray cmt = _history.getJSONArray(j);
                if ("comment".equals(cmt.getString(2)) && cmt.getString(4).length() > 0) {
                    tekst += "comment: " + toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(
                            1) + " - " + cmt.getString(4)
                            + "\n";
                }
            } catch (final JSONException e) {
                MyLog.e("history failed", e);
            }
        }
        for (int j = 0; j < _attachments.length(); j++) {

            try {
                JSONArray bijlage = _attachments.getJSONArray(j);
                tekst += "bijlage " + (j + 1) + ": " + toonTijd(
                        bijlage.getJSONObject(3)) + " - " + bijlage.getString(4) + " - "
                        + bijlage.getString(0) + " - " + bijlage.getString(1) + "\n";
            } catch (final JSONException e) {
                MyLog.e("attachment failed", e);
            }
        }
        return tekst;
    }

    private String toonTijd(final JSONObject v) {
        try {
            return toCalendar(
                    v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
        } catch (final Exception e) {
            MyLog.e("Exception", e);
            return "";
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NormalTicket && (_ticknr == ((NormalTicket) obj)._ticknr);
    }

    @Override
    public int hashCode() {
        int result = _ticknr;
        result = 31 * result + (_velden != null ? _velden.toString().hashCode() : 0);
        result = 31 * result + (_history != null ? _history.toString().hashCode() : 0);
        result = 31 * result + (_attachments != null ? _attachments.toString().hashCode() : 0);
        result = 31 * result + (_actions != null ? _actions.toString().hashCode() : 0);
        result = 31 * result + (_hasdata ? 1 : 0);
        return result;
    }
}
