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
    private String url;
    private String username;
    private String password;
    private boolean sslHack;
    private boolean sslHostNameHack = false;
    private List<FilterSpec> filterList = null;
    private List<SortSpec> sortList = null;
    private String _profile = null;

    LoginProfileImpl(String _url, String _username, String _password, boolean _sslHack) {
        this(_url, _username, _password, _sslHack, false);
    }

    LoginProfileImpl(String _url, String _username, String _password, boolean _sslHack, boolean _sslHostNameHack) {
        url = _url;
        username = _username;
        password = _password;
        sslHack = _sslHack;
        sslHostNameHack = _sslHostNameHack;
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
        return equalFields(url, f.getUrl())
                && equalFields(username, f.getUsername())
                && equalFields(password, f.getPassword())
                && equalFields(filterList, f.getFilterList())
                && equalFields(sortList, f.getSortList())
                && (sslHack == f.getSslHack())
                && (sslHostNameHack == f.getSslHostNameHack());
    }

    @SuppressWarnings({"MethodReturnOfConcreteClass", "LocalVariableOfConcreteClass"})
    @Override
    public LoginProfileImpl clone() throws CloneNotSupportedException {
        LoginProfileImpl lp = (LoginProfileImpl) super.clone();
        lp.url = url;
        lp.username = username;
        lp.password = password;
        lp.sslHack = sslHack;
        lp.sslHostNameHack = sslHostNameHack;
        lp.filterList = filterList;
        lp.sortList = sortList;
        lp._profile = _profile;
        return lp;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean getSslHack() {
        return sslHack;
    }

    @Override
    public boolean getSslHostNameHack() {
        return sslHostNameHack;
    }

    @Override
    public LoginProfile setSslHostNameHack(boolean v) {
        sslHostNameHack = v;
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
        return "url: " + url + " username: " + username + " password: " + password + " sslHack: " + sslHack + " sslHostNameHack: " + sslHostNameHack + " filterList: " + filterList + " sortList: " + sortList;
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
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (sslHack ? 1 : 0);
        result = 31 * result + (sslHostNameHack ? 1 : 0);
        result = 31 * result + (filterList != null ? filterList.hashCode() : 0);
        result = 31 * result + (sortList != null ? sortList.hashCode() : 0);
        result = 31 * result + (_profile != null ? _profile.hashCode() : 0);
        return result + super.hashCode();
    }
}
