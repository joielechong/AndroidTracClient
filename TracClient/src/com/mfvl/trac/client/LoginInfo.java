package com.mfvl.trac.client;

public class LoginInfo {

	public static String url;
	public static String username;
	public static String password;
	public static String profile;
	public static boolean sslHack;
	public static boolean sslHostNameHack;

	private static LoginInfo _instance = null;

	private LoginInfo() {
	}

	public static LoginInfo getInstance() {
		if (_instance == null) {
			_instance = new LoginInfo();
		}
		return _instance;
	}
}
