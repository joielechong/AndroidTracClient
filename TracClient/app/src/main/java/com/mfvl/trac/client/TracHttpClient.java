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

import android.util.Base64;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


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

    public TracHttpClient(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
	super(url, sslHack, sslHostNameHack);
	setCredentials(username, password);
	current_url = url;
    }

    public TracHttpClient(final LoginProfile lp) {
	super(lp.getUrl(), lp.getSslHack(), lp.getSslHostNameHack());
	setCredentials(lp.getUsername(), lp.getPassword());
	current_url = lp.getUrl();
    }

    public JSONArray Query(String reqString) throws JSONRPCException {
	return callJSONArray(TICKET_QUERY, reqString);
    }

    public int createTicket(final String s, final String d, final JSONObject _velden, boolean notify) throws JSONRPCException {
	return callInt(TICKET_CREATE, s, d, _velden, notify);
    }

    public JSONArray updateTicket(final int _ticknr, final String cmt, final JSONObject _velden, final boolean notify) throws JSONRPCException {
	// tcLog.d( "_velden call = " + _velden);
	return callJSONArray(TICKET_UPDATE, _ticknr, cmt, _velden, notify);
    }

    public String verifyHost() throws JSONRPCException {
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

    public void putAttachment(final int ticknr, String filename, String base64Content) throws JSONException, JSONRPCException {
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
	final String retfile = callString(TICKET_PUTATTACHMENT, ar);

	tcLog.i("putAttachment " + retfile);
    }
}
