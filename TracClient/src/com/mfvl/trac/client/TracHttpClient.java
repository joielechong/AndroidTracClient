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
	final static String TICKET_GETATTACHMENT ="ticket.getAttachment";
	final static String SYSTEM_GETAPIVERSION = "system.getAPIVersion";

	final static String _JSONCLASS = "__jsonclass__";

	private static TracHttpClient _instance = null;
	private static String current_uri = null;
	private static String current_user = null;
	private static String current_pass = null;

	private TracHttpClient(final String uri, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
		super(uri, sslHack, sslHostNameHack);
		this.setCredentials(username, password);
		current_uri = uri;
		current_user = username;
		current_pass = password;
	}

	static public TracHttpClient getInstance() {
		boolean reload = _instance == null;

		if (!reload) {
			try {
				reload = !(Tickets.url.equals(current_uri) && Tickets.username.equals(current_user) && Tickets.password
						.equals(current_pass));
			} catch (final Exception e) {
				reload = true;
			}
		}

		if (reload) {
			_instance = new TracHttpClient(Tickets.url, Tickets.sslHack, Tickets.sslHostNameHack, Tickets.username, Tickets.password);
		}
		return _instance;
	}

	static public TracHttpClient getInstance(final String uri, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
		_instance = new TracHttpClient(uri, sslHack, sslHostNameHack, username, password);
		return _instance;
	}
	
	static public JSONArray Query(String reqString) throws JSONRPCException {
		return _instance.callJSONArray(TICKET_QUERY, reqString);
	}

	static public int createTicket(final String s, final String d, final JSONObject _velden) throws JSONRPCException {
		getInstance();
		return _instance.callInt(TICKET_CREATE, s, d, _velden);
	}

	static public void updateTicket(final int _ticknr, final String cmt, final JSONObject _velden, final boolean notify) throws JSONRPCException {
		getInstance();
		// tcLog.d(this.getClass().getName(), "_velden call = " + _velden);
		_instance.callJSONArray(TICKET_UPDATE, _ticknr, cmt, _velden, notify);
	}

	static public String verifyHost(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username,
			final String password) throws Exception {
		getInstance(url, sslHack, sslHostNameHack, username, password);
		return _instance.callJSONArray(SYSTEM_GETAPIVERSION).toString();
	}

	static public JSONArray getModel() throws Exception {
		getInstance();
		if (current_uri == null) {
			return null;
		} else {
			return _instance.callJSONArray(TICKET_GETTICKETFIELDS);
		}
	}

	static public byte[] getAttachment(int ticknr,String filename) throws JSONException,JSONRPCException{
		getInstance();
		return Base64.decode(_instance.callJSONObject(TICKET_GETATTACHMENT, ticknr, filename).getJSONArray(_JSONCLASS).getString(1), Base64.DEFAULT);
	}
}