/*
 * Copyright (C) 2014 Michiel van Loon
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

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Base64;


public class TracHttpClient extends JSONRPCHttpClient {

    final static String TICKET_QUERY = "ticket.query";
    final static String TICKET_CREATE = "ticket.create";
    final static String TICKET_UPDATE = "ticket.update";
    final static String TICKET_GETTICKETFIELDS = "ticket.getTicketFields";
    final static String TICKET_GETATTACHMENT = "ticket.getAttachment";
	final static String TICKET_PUTATTACHMENT = "ticket.putAttachment";
    final static String SYSTEM_GETAPIVERSION = "system.getAPIVersion";

    final static String _JSONCLASS = "__jsonclass__";

    private String current_url = null;
    private String current_user = null;
    private String current_pass = null;
	private boolean current_sslHack = false;
	private boolean current_sslHostNameHack = false;
	
    public TracHttpClient(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
        super(url, sslHack, sslHostNameHack);
        setCredentials(username, password);
        current_url = url;
        current_user = username;
        current_pass = password;
		current_sslHack = sslHack;
		current_sslHostNameHack = sslHostNameHack;
    }

    public TracHttpClient(final LoginProfile lp) {
        super(lp.getUrl(), lp.getSslHack(), lp.getSslHostNameHack());
        setCredentials(lp.getUsername(), lp.getPassword());
        current_url = lp.getUrl();
        current_user = lp.getUsername();
        current_pass = lp.getPassword();
		current_sslHack = lp.getSslHack();
		current_sslHostNameHack = lp.getSslHostNameHack();
    }

    public JSONArray Query(String reqString) throws JSONRPCException {
        return callJSONArray(TICKET_QUERY, reqString);
    }

    public int createTicket(final String s, final String d, final JSONObject _velden) throws JSONRPCException {
        return callInt(TICKET_CREATE, s, d, _velden);
    }

    public JSONArray updateTicket(final int _ticknr, final String cmt, final JSONObject _velden, final boolean notify) throws JSONRPCException {
        // tcLog.d( "_velden call = " + _velden);
        return callJSONArray(TICKET_UPDATE, _ticknr, cmt, _velden, notify);
    }

    public String verifyHost() throws Exception {
        return callJSONArray(SYSTEM_GETAPIVERSION).toString();
    }

    public JSONArray getModel() throws Exception {
        if (current_url == null) {
            return null;
        } else {
            return callJSONArray(TICKET_GETTICKETFIELDS);
        }
    }

    public byte[] getAttachment(int ticknr, String filename) throws JSONException, JSONRPCException {
        return Base64.decode(callJSONObject(TICKET_GETATTACHMENT, ticknr, filename).getJSONArray(_JSONCLASS).getString(1),
                Base64.DEFAULT);
    }
}
