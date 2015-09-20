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
import java.util.List;


public class LoginProfile implements Serializable, Cloneable {
    private static final long serialVersionUID = 7810597345256080395L;

    private final String _url;
    private final String _username;
    private final String _password;
    private final boolean _sslHack;
	private boolean _sslHostNameHack = false;
	private List<FilterSpec> filterList = null;
	private List<SortSpec> sortList = null;

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

    public boolean getSslHostNameHack() {
        return _sslHostNameHack;
    }

    public LoginProfile setSslHostNameHack(boolean v) {
        _sslHostNameHack = v;
		return this;
    }
	
	public LoginProfile setFilterList(List<FilterSpec> fl) {
		filterList = fl;
		return this;
	}
	
	public List<FilterSpec> getFilterList() {
		return filterList;
	}

	public LoginProfile setSortList(List<SortSpec> sl) {
		sortList = sl;
		return this;
	}

	public List<SortSpec> getSortList() {
		return sortList;
	}

    @Override
    public String toString() {
        return "url: " + _url + " username: " + _username + " password: " + _password + " sslHack: " + _sslHack + " sslHostNameHack: " + _sslHostNameHack + " filterList: " + filterList + " sortList: " + sortList;
    }
}
