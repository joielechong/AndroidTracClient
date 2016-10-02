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

import java.io.Serializable;
import java.util.List;

interface LoginProfile {
    String getUrl();

    String getUsername();

    String getPassword();

    boolean getSslHack();

    boolean getSslHostNameHack();

    LoginProfile setSslHostNameHack(boolean v);

    List<FilterSpec> getFilterList();

    LoginProfile setFilterList(List<FilterSpec> fl);

    List<SortSpec> getSortList();

    LoginProfile setSortList(List<SortSpec> sl);

    String getProfile();

    void setProfile(String profile);

}

class LoginProfileImpl extends TcObject implements Serializable, Cloneable, LoginProfile {
    private String _url;
    private String _username;
    private String _password;
    private boolean _sslHack;
    private boolean _sslHostNameHack = false;
    private List<FilterSpec> filterList = null;
    private List<SortSpec> sortList = null;
    private String _profile = null;

    LoginProfileImpl(String url, String username, String password, boolean sslHack) {
        this(url, username, password, sslHack, false);
    }

    LoginProfileImpl(String url, String username, String password, boolean sslHack, boolean sslHostNameHack) {
        _url = url;
        _username = username;
        _password = password;
        _sslHack = sslHack;
        _sslHostNameHack = sslHostNameHack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoginProfile)) {
            return false;
        }
        LoginProfile f = (LoginProfile) o;
//			MyLog.d("this = "+this+" f = "+f + " "+retVal);
        return equalFields(_url, f.getUrl())
                && equalFields(_username, f.getUsername())
                && equalFields(_password, f.getPassword())
                && equalFields(filterList, f.getFilterList())
                && equalFields(sortList, f.getSortList())
                && (_sslHack == f.getSslHack())
                && (_sslHostNameHack == f.getSslHostNameHack());
    }

    @SuppressWarnings({"MethodReturnOfConcreteClass", "LocalVariableOfConcreteClass"})
    @Override
    public LoginProfileImpl clone() throws CloneNotSupportedException {
        LoginProfileImpl lp = (LoginProfileImpl) super.clone();
        lp._url=_url;
        lp._username = _username;
        lp._password=_password;
        lp._sslHack = _sslHack;
        lp._sslHostNameHack = _sslHostNameHack;
        lp.filterList = filterList;
        lp.sortList = sortList;
        lp._profile = _profile;
        return lp;
    }

    @Override
    public String getUrl() {
        return _url;
    }

    @Override
    public String getUsername() {
        return _username;
    }

    @Override
    public String getPassword() {
        return _password;
    }

    @Override
    public boolean getSslHack() {
        return _sslHack;
    }

    @Override
    public boolean getSslHostNameHack() {
        return _sslHostNameHack;
    }

    @Override
    public LoginProfile setSslHostNameHack(boolean v) {
        _sslHostNameHack = v;
        return this;
    }

    @Override
    public List<FilterSpec> getFilterList() {
        return filterList;
    }

    @Override
    public LoginProfile setFilterList(List<FilterSpec> fl) {
        filterList = fl;
        return this;
    }

    @Override
    public List<SortSpec> getSortList() {
        return sortList;
    }

    @Override
    public LoginProfile setSortList(List<SortSpec> sl) {
        sortList = sl;
        return this;
    }

    @Override
    public String toString() {
        return "url: " + _url + " username: " + _username + " password: " + _password + " sslHack: " + _sslHack + " sslHostNameHack: " + _sslHostNameHack + " filterList: " + filterList + " sortList: " + sortList;
    }

    @Override
    public String getProfile() {
        return _profile;
    }

    @Override
    public void setProfile(String profile) {
        _profile = profile;
    }

    @Override
    public int hashCode() {
        int result = _url != null ? _url.hashCode() : 0;
        result = 31 * result + (_username != null ? _username.hashCode() : 0);
        result = 31 * result + (_password != null ? _password.hashCode() : 0);
        result = 31 * result + (_sslHack ? 1 : 0);
        result = 31 * result + (_sslHostNameHack ? 1 : 0);
        result = 31 * result + (filterList != null ? filterList.hashCode() : 0);
        result = 31 * result + (sortList != null ? sortList.hashCode() : 0);
        result = 31 * result + (_profile != null ? _profile.hashCode() : 0);
        return result +super.hashCode();
    }
}
