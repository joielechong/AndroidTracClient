/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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

import android.util.Base64;

import com.mfvl.mfvllib.MyLog;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.mfvl.trac.client.Const.*;


class TracHttpClient extends JSONRPCHttpClient {

    private final static String _JSONCLASS = "__jsonclass__";
    private final boolean current_sslHack;
    private final boolean current_sslHostNameHack;
    private final String current_url;
    private final String current_username;
    private final String current_password;

    TracHttpClient(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
        super(url, sslHack, sslHostNameHack);
        current_url = url;
        current_username = username;
        current_password = password;
        current_sslHack = sslHack;
        current_sslHostNameHack = sslHostNameHack;
        setCredentials(username, password);
    }

    TracHttpClient(final LoginProfile lp) {
        this(lp.getUrl(), lp.getSslHack(), lp.getSslHostNameHack(), lp.getUsername(), lp.getPassword());
    }

    TracHttpClient(final JSONObject b) throws JSONException {
        this(b.getString(CURRENT_URL), b.getBoolean(CURRENT_SSLHACK), b.getBoolean(CURRENT_SSLHOSTNAMEHACK), b.getString(CURRENT_USERNAME),
                b.getString(CURRENT_PASSWORD));
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject a = new JSONObject();
        a.put(CURRENT_URL, current_url);
        a.put(CURRENT_USERNAME, current_username);
        a.put(CURRENT_PASSWORD, current_password);
        a.put(CURRENT_SSLHACK, current_sslHack);
        a.put(CURRENT_SSLHOSTNAMEHACK, current_sslHostNameHack);
        return a;
    }

    public JSONArray Query(String reqString) throws JSONRPCException {
        String TICKET_QUERY = "ticket.query";
        return callJSONArray(TICKET_QUERY, reqString);
    }

    public int createTicket(final String s, final String d, final JSONObject _velden, boolean notify) throws JSONRPCException {
        String TICKET_CREATE = "ticket.create";
        return callInt(TICKET_CREATE, s, d, _velden, notify);
    }

    public JSONArray updateTicket(final int _ticknr, final String cmt, final JSONObject _velden, final boolean notify) throws JSONRPCException {
        // MyLog.d( "_velden call = " + _velden);
        String TICKET_UPDATE = "ticket.update";
        return callJSONArray(TICKET_UPDATE, _ticknr, cmt, _velden, notify);
    }

    public String verifyHost() throws JSONRPCException {
        String SYSTEM_GETAPIVERSION = "system.getAPIVersion";
        return callJSONArray(SYSTEM_GETAPIVERSION).toString();
    }

    public JSONArray getModel() throws JSONRPCException {
        String TICKET_GETTICKETFIELDS = "ticket.getTicketFields";
        return callJSONArray(TICKET_GETTICKETFIELDS);
    }

    public byte[] getAttachment(int ticknr, String filename) throws JSONException, JSONRPCException {
        String TICKET_GETATTACHMENT = "ticket.getAttachment";
        return Base64.decode(callJSONObject(TICKET_GETATTACHMENT, ticknr, filename).getJSONArray(_JSONCLASS).getString(1), Base64.DEFAULT);
    }

    void putAttachment(final int ticknr, String filename, String base64Content) throws JSONException, JSONRPCException {
        final JSONArray ar = new JSONArray();

        ar.put(ticknr);
        ar.put(filename);
        ar.put("");
        final JSONArray ar1 = new JSONArray();

        ar1.put("binary");
        ar1.put(base64Content);
        final JSONObject ob = new JSONObject();

        ob.put("__jsonclass__", ar1);
        ar.put(ob);
        ar.put(true);
        String TICKET_PUTATTACHMENT = "ticket.putAttachment";
        final String retfile = callString(TICKET_PUTATTACHMENT, ar);

        MyLog.i("putAttachment " + retfile);
    }

    @SuppressWarnings("LocalVariableOfConcreteClass")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof TracHttpClient) {
            TracHttpClient t = (TracHttpClient) o;
            return current_url.equals(t.current_url) && current_username.equals(t.current_username);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = current_url != null ? current_url.hashCode() : 0;
        result = 31 * result + (current_username != null ? current_username.hashCode() : 0);
        return result;
    }
}
