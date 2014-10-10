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

package com.mfvl.trac.client.util;

import java.io.Serializable;

public class LoginProfile extends Object implements Serializable, Cloneable {
	private static final long serialVersionUID = 7810597433987080395L;

	private final String _url;
	private final String _username;
	private final String _password;
	private final boolean _sslHack;

	public LoginProfile(String url, String username, String password, boolean sslHack) {
		_url = url;
		_username = username;
		_password = password;
		_sslHack = sslHack;
	}

	public String getUrl() {
		return _url;
	}

	public String getUsername() {
		return _username;
	}

	public String getPassword() {
		return _password;
	}

	public boolean getSslHack() {
		return _sslHack;
	}

	@Override
	public String toString() {
		return "url: " + _url + " username: " + _username + " password: " + _password + " sslHack: " + _sslHack;
	}

	private int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		// Start with a non-zero constant.
		int result = 17;

		// Include a hash for each field.
		result = 31 * result + hc(_sslHack);
		result = 31 * result + hc(_url);
		result = 31 * result + hc(_username);
		result = 31 * result + hc(_password);
		return 31 * result + super.hashCode();
	}
}
