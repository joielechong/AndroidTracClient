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
	
	private Credentials() {
	}
	
    public static Credentials getInstance()
    {
        if (_instance == null)
        {
            _instance = new Credentials();
        }
        return _instance;
    }

	private void getSettings(final Context context) {
		if (settings == null) {
			settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
		}
	}

	/**
	 * Load login credentials from shared preferences: server-url, username,
	 * password and profile
	 */
	public void loadCredentials(final Context context) {
		// tcLog.d("Credentials", "loadCredentials");
		getSettings(context);
		_url = settings.getString(Const.PREF_URL, "");
		_username = settings.getString(Const.PREF_USER, "");
		_password = settings.getString(Const.PREF_PASS, "");
		_sslHack = settings.getBoolean(Const.PREF_HACK, false);
		_sslHostNameHack = settings.getBoolean(Const.PREF_HNH, false);
		_profile = settings.getString(Const.PREF_PROF, null);
	}

	public  void reloadCredentials(final Context context) {
		if (settings == null) {
			loadCredentials(context);
		}
	}

	/**
	 * Store login credentials to shared preferences: server-url, username,
	 * password and profile
	 */
	public  void storeCredentials(final Context context) {
		// tcLog.d("Credentials", "storeCredentials");
		getSettings(context);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_URL, _url);
		editor.putString(Const.PREF_USER, _username);
		editor.putString(Const.PREF_PASS, _password);
		editor.putBoolean(Const.PREF_HACK, _sslHack);
		editor.putBoolean(Const.PREF_HNH, _sslHostNameHack);
		editor.putString(Const.PREF_PROF, _profile);

		// Commit the edits!
		editor.commit();
	}

	/** Set login credentials server-url, username, password and profile */
	public void setCredentials(final String url, final String username, final String password, final String profile) {
		// tcLog.d("Credentials", "setCredentials");
		_url = url;
		_username = username;
		_password = password;
		_profile = profile;
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

	public void setSslHack(boolean sslHack) {
		_sslHack = sslHack;
	}

	public boolean getSslHack() {
		return _sslHack;
	}

	public void setSslHostNameHack(boolean sslHostNameHack) {
		_sslHostNameHack = sslHostNameHack;
	}

	public boolean getSslHostNameHack() {
		return _sslHostNameHack;
	}

	public void setProfile(String profile) {
		_profile = profile;
	}

	public String getProfile() {
		return _profile;
	}

	public boolean getFirstRun(Context context) {
		// tcLog.d("Credentials", "getFirstRun");
		getSettings(context);
		final String thisRun = buildVersion(context);
		final String lastRun = settings.getString(Const.PREF_1ST, "");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_1ST, thisRun);
		editor.commit();

		return !lastRun.equals(thisRun);
	}

	public void storeFilterString(Context context, final String filterString) {
		tcLog.d("Credentials", "storeFilterString: " + filterString);
		getSettings(context);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_FILTER, filterString == null ? "" : filterString);
		editor.commit();
	}

	public String getFilterString(Context context) {
		// tcLog.d("Credentials", "getFilterString");
		getSettings(context);
		final String filterString = settings.getString(Const.PREF_FILTER, "max=500&status!=closed");
		tcLog.d("Credentials", "getFilterString filterString = " + filterString);
		return filterString;
	}

	public void removeFilterString(Context context) {
		tcLog.d("Credentials", "removeFilterString");
		getSettings(context);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_FILTER, "max=500&status!=closed");
		editor.commit();
	}

	public void storeSortString(Context context, final String sortString) {
		tcLog.d("Credentials", "storeSortString: " + sortString);
		getSettings(context);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_SORT, sortString == null ? "" : sortString);
		editor.commit();
	}

	public String getSortString(Context context) {
		// tcLog.d("Credentials", "getSortString");
		getSettings(context);
		final String sortString = settings.getString(Const.PREF_SORT, "order=priority&order=modified&desc=1");
		tcLog.d("Credentials", "getSortString sortString = " + sortString);
		return sortString;
	}

	public void removeSortString(Context context) {
		tcLog.d("Credentials", "removeSortString");
		getSettings(context);
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString(Const.PREF_SORT, "order=priority&order=modified&desc=1");
		editor.commit();
	}

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	public boolean isDebuggable(Context ctx) {
		boolean debuggable = false;

		try {
			final PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
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
			tcLog.i(ctx.getClass().getName(), "isDebuggable", e);
		} catch (final CertificateException e) {
			tcLog.i(ctx.getClass().getName(), "isDebuggable", e);
		}
		return debuggable;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public String buildVersion(Context context) {
		final PackageManager manager = context.getPackageManager();
		PackageInfo info;
		if (versie == null) {
			try {
				info = manager.getPackageInfo(context.getPackageName(), 0);
				versie = "V" + info.versionName;
				final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
				// tcLog.d(context.getClass().getName(),
				// "buildVersion versie = " +
				// versie + " api = " + currentapiVersion);
				if (isDebuggable(context) && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
					versie += "/" + info.lastUpdateTime / (1000 * 60);
				}
			} catch (final NameNotFoundException e) {
				tcLog.i(context.getClass().getName(), "buildVersion", e);
				if (versie == null) {
					versie = "V0.6.x";
				}
			}
		}
		// tcLog.d(context.getClass().getName(), "buildVersion versie = " +
		// versie);
		return versie;
	}

	public boolean isRCVersion(Context context) {
		buildVersion(context);
		if (versie == null) {
			return false;
		}
		return versie.toLowerCase(Locale.US).contains("rc");
	}

	public String makeDbPath(Context context, String dbname) {
		final File extpath = Environment.getExternalStorageDirectory();

		String dbpath = dbname;

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			final String p1 = extpath.toString() + "/TracClient/" + dbname;
			if (!isDebuggable(context)) {
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

	public String makeExtFilePath(String filename) throws FileNotFoundException {
		final File extpath = Environment.getExternalStorageDirectory();

		final String filePath = extpath.toString() + "/TracClient/" + filename;

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new FileNotFoundException(filePath);
		}
		return filePath;
	}
}
