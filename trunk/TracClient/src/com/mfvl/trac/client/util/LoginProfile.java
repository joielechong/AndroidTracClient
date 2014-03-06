package com.mfvl.trac.client.util;

import java.io.Serializable;

public class LoginProfile extends Object implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7810597433987080395L;
	/**
	 * 
	 */
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
		return (o == null ? 0 : o.hashCode());
	}
	
	@Override 
	public int hashCode() {
     // Start with a non-zero constant.
		int result = 17;

     // Include a hash for each field.
		result = 31 * result + (_sslHack ? 1 : 0);
		result = 31 * result + hc(_url);
		result = 31 * result + hc(_username);
		result = 31 * result + hc(_password);
		return result;
	}
}
