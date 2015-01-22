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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Environment;

import com.mfvl.trac.client.Const;

public class Credentials {
	private static String versie = null;
	private static String _url = "";
	private static String _username = "";
	private static String _password = "";
	private static boolean _sslHack = false;
	private static boolean _sslHostNameHack = false;
	private static String _profile = null;
	private static SharedPreferences settings = null;
	private static Credentials _instance = null;
	private static Context _context = null;
	private static String _tag = "";

	private Credentials(final Context context) {
		settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
		_context = context;
		_tag = getClass().getName();
	}

	public static Credentials getInstance(final Context context) {
		if (_instance == null) {
			_instance = new Credentials(context);
		}
		return _instance;
	}

	/**
	 * Load login credentials from shared preferences: server-url, username, password and profile
	 */
	public static void loadCredentials() {
		// tcLog.d(_tag, "loadCredentials");
		_url = settings.getString(Const.PREF_URL, "");
		_username = settings.getString(Const.PREF_USER, "");
		_password = settings.getString(Const.PREF_PASS, "");
		_sslHack = settings.getBoolean(Const.PREF_HACK, false);
		_sslHostNameHack = settings.getBoolean(Const.PREF_HNH, false);
		_profile = settings.getString(Const.PREF_PROF, null);
	}

	/**
	 * Store login credentials to shared preferences: server-url, username, password and profile
	 */
	public static void storeCredentials() {
		// tcLog.d(_tag, "storeCredentials");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_URL, _url);
		editor.putString(Const.PREF_USER, _username);
		editor.putString(Const.PREF_PASS, _password);
		editor.putBoolean(Const.PREF_HACK, _sslHack);
		editor.putBoolean(Const.PREF_HNH, _sslHostNameHack);
		editor.putString(Const.PREF_PROF, _profile);

		// apply the edits!
		editor.apply();
	}

	/** Set login credentials server-url, username, password and profile */
	public static void setCredentials(final String url, final String username, final String password, final String profile) {
		// tcLog.d(_tag, "setCredentials");
		_url = url;
		_username = username;
		_password = password;
		_profile = profile;
	}

	public static String getUrl() {
		return _url;
	}

	public static String getUsername() {
		return _username;
	}

	public static String getPassword() {
		return _password;
	}

	public static void setSslHack(boolean sslHack) {
		_sslHack = sslHack;
	}

	public static boolean getSslHack() {
		return _sslHack;
	}

	public static void setSslHostNameHack(boolean sslHostNameHack) {
		_sslHostNameHack = sslHostNameHack;
	}

	public static boolean getSslHostNameHack() {
		return _sslHostNameHack;
	}

	public static void setProfile(String profile) {
		_profile = profile;
	}

	public static String getProfile() {
		return _profile;
	}

	public static boolean getFirstRun() {
		// tcLog.d("Credentials", "getFirstRun");
		final String thisRun = buildVersion();
		final String lastRun = settings.getString(Const.PREF_1ST, "");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_1ST, thisRun);
		editor.apply();

		return !lastRun.equals(thisRun);
	}

	public static void storeFilterString(final String filterString) {
		tcLog.d("Credentials", "storeFilterString: " + filterString);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_FILTER, filterString == null ? "" : filterString);
		editor.apply();
	}

	public static String getFilterString() {
		// tcLog.d("Credentials", "getFilterString");
		final String filterString = settings.getString(Const.PREF_FILTER, "max=500&status!=closed");
		tcLog.d("Credentials", "getFilterString filterString = " + filterString);
		return filterString;
	}

	public static void removeFilterString() {
		tcLog.d("Credentials", "removeFilterString");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_FILTER, "max=500&status!=closed");
		editor.apply();
	}

	public static void storeSortString(final String sortString) {
		tcLog.d("Credentials", "storeSortString: " + sortString);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_SORT, sortString == null ? "" : sortString);
		editor.apply();
	}

	public static String getSortString() {
		// tcLog.d("Credentials", "getSortString");
		final String sortString = settings.getString(Const.PREF_SORT, "order=priority&order=modified&desc=1");
		tcLog.d("Credentials", "getSortString sortString = " + sortString);
		return sortString;
	}

	public static void removeSortString() {
		tcLog.d(_tag, "removeSortString");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_SORT, "order=priority&order=modified&desc=1");
		editor.apply();
	}

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	public static boolean isDebuggable() {
		boolean debuggable = false;

		try {
			final PackageInfo pinfo = _context.getPackageManager().getPackageInfo(_context.getPackageName(),
					PackageManager.GET_SIGNATURES);
			final Signature signatures[] = pinfo.signatures;

			final CertificateFactory cf = CertificateFactory.getInstance("X.509");

			for (final Signature signature : signatures) {
				final ByteArrayInputStream stream = new ByteArrayInputStream(signature.toByteArray());
				final X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
				debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
				if (debuggable) {
					break;
				}
			}
		} catch (final NameNotFoundException e) {
			tcLog.i(_tag, "isDebuggable", e);
		} catch (final CertificateException e) {
			tcLog.i(_tag, "isDebuggable", e);
		}
		return debuggable;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static String buildVersion() {
		PackageInfo info;
		if (versie == null) {
			try {
				info = _context.getPackageManager().getPackageInfo(_context.getPackageName(), 0);
				versie = "V" + info.versionName;
				final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
				// tcLog.d(_tag, "buildVersion versie = " + versie + " api = " + currentapiVersion);
				if (isDebuggable() && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
					versie += "/" + info.lastUpdateTime / (1000 * 60);
				}
			} catch (final NameNotFoundException e) {
				tcLog.i(_tag, "buildVersion", e);
				if (versie == null) {
					versie = "V0.6.x";
				}
			}
		}
		// tcLog.d(_tag, "buildVersion versie = " + versie);
		return versie;
	}

	public static boolean isRCVersion() {
		buildVersion();
		if (versie == null) {
			return false;
		}
		return versie.toLowerCase(Locale.US).contains("rc");
	}

	public static String makeDbPath(String dbname) {
		final File extpath = Environment.getExternalStorageDirectory();

		String dbpath = dbname;

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			final String p1 = extpath.toString() + "/TracClient/" + dbname;
			if (!isDebuggable()) {
				if (new File(p1).exists()) {
					dbpath = p1;
				}
			} else {
				dbpath = p1;
			}
		}
		// tcLog.d(context.getClass().getName(), "makeDbPath dbpath = " +
		// dbpath);
		return dbpath;
	}

	public static String makeExtFilePath(String filename) throws FileNotFoundException {
		final File extpath = Environment.getExternalStorageDirectory();

		final String filePath = extpath.toString() + "/TracClient/" + filename;

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new FileNotFoundException(filePath);
		}
		return filePath;
	}
	
	public static Boolean metaDataGetBoolean (String metaId) throws NameNotFoundException {
		return (_context != null ?_context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA).metaData.getBoolean(metaId) : null);
	}
	
	public static String metaDataGetString (String metaId) throws NameNotFoundException {
		return (_context != null ?_context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA).metaData.getString(metaId) : null);
	}
}
