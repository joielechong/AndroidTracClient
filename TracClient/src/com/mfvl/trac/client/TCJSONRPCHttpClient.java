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

import org.alexd.jsonrpc.JSONRPCHttpClient;

public class TCJSONRPCHttpClient extends JSONRPCHttpClient {

	private static TCJSONRPCHttpClient _instance = null;
	private static String current_uri = null;
	private static String current_user = null;
	private static String current_pass = null;

	private TCJSONRPCHttpClient(final String uri, final boolean sslHack, final boolean sslHostNameHack, final String username,
			final String password) {
		super(uri, sslHack, sslHostNameHack);
		this.setCredentials(username, password);
		current_uri = uri;
		current_user = username;
		current_pass = password;
	}

	static public TCJSONRPCHttpClient getInstance() {
		boolean reload = _instance == null;

		if (!reload) {
			try {
				reload = !(LoginInfo.url.equals(current_uri) && LoginInfo.username.equals(current_user) && LoginInfo.password
						.equals(current_pass));
			} catch (final Exception e) {
				reload = true;
			}
		}

		if (reload) {
			_instance = new TCJSONRPCHttpClient(LoginInfo.url, LoginInfo.sslHack, LoginInfo.sslHostNameHack, LoginInfo.username,
					LoginInfo.password);
		}
		return _instance;
	}

	static public TCJSONRPCHttpClient getInstance(final String uri, final boolean sslHack, final boolean sslHostNameHack,
			final String username, final String password) {
		_instance = new TCJSONRPCHttpClient(uri, sslHack, sslHostNameHack, username, password);
		return _instance;
	}
}