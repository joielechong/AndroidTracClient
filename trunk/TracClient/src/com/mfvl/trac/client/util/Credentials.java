package com.mfvl.trac.client.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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

public class Credentials {
	public static final String PREFS_NAME = "Trac";
	private static String _url = "";
	private static String _username = "";
	private static String _password = "";
	private static boolean _sslHack = false;
	private static String _profile = null;
	private static SharedPreferences settings = null;

	/** Set login credentials server-url, username, password and profile */
	public static void setCredentials(final String url, final String username, final String password, final String profile) {
		// tcLog.d("Credentials", "setCredentials");
		_url = url;
		_username = username;
		_password = password;
		_profile = profile;
	}

	/**
	 * Load login credentials from shared preferences: server-url, username,
	 * password and profile
	 */
	public static void loadCredentials(Context context) {
		// tcLog.d("Credentials", "loadCredentials");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		_url = settings.getString("tracUrl", "");
		_username = settings.getString("tracUsername", "");
		_password = settings.getString("tracPassword", "");
		_sslHack = settings.getBoolean("sslHack", false);
		_profile = settings.getString("profile", null);
	}

	/**
	 * Store login credentials to shared preferences: server-url, username,
	 * password and profile
	 */
	public static void storeCredentials(Context context) {
		// tcLog.d("Credentials", "storeCredentials");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("tracUrl", _url);
		editor.putString("tracUsername", _username);
		editor.putString("tracPassword", _password);
		editor.putBoolean("sslHack", _sslHack);
		editor.putString("profile", _profile);

		// Commit the edits!
		editor.commit();
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

	public static void setProfile(String profile) {
		_profile = profile;
	}

	public static String getProfile() {
		return _profile;
	}

	public static boolean getFirstRun(Context context) {
		// tcLog.d("Credentials", "getFirstRun");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final String thisRun = buildVersion(context, false);
		final String lastRun = settings.getString("firstRun", "");
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("firstRun", thisRun);
		editor.commit();

		return !lastRun.equals(thisRun);
	}

	public static void storeFilterString(Context context, final String filterString) {
		// tcLog.d("Credentials", "storeFilterString: " + filterString);
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("filterString", filterString == null ? "" : filterString);
		editor.commit();
	}

	public static String getFilterString(Context context) {
		// tcLog.d("Credentials", "getFilterString");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final String filterString = settings.getString("filterString", "max=500&status!=closed");
		tcLog.d("Credentials", "getFilterString filterString = " + filterString);
		return filterString;
	}

	public static void removeFilterString(Context context) {
		// tcLog.d("Credentials", "removeFilterString");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("filterString", "max=500&status!=closed");
		editor.commit();
	}

	public static void storeSortString(Context context, final String sortString) {
		// tcLog.d("Credentials", "storeSortString: " + sortString);
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("sortString", sortString == null ? "" : sortString);
		editor.commit();
	}

	public static String getSortString(Context context) {
		// tcLog.d("Credentials", "getSortString");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final String sortString = settings.getString("sortString", "order=priority&order=modified&desc=1");
		tcLog.d("Credentials", "getSortString sortString = " + sortString);
		return sortString;
	}

	public static void removeSortString(Context context) {
		// tcLog.d("Credentials", "removeSortString");
		if (settings == null) {
			settings = context.getSharedPreferences(PREFS_NAME, 0);
		}
		final SharedPreferences.Editor editor = settings.edit();
		editor.putString("sortString", "order=priority&order=modified&desc=1");
		editor.commit();
	}

	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

	public static boolean isDebuggable(Context ctx) {
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
		} catch (final CertificateException e) {
		}
		return debuggable;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static String buildVersion(Context context, boolean extra) {
		String versie = null;
		final PackageManager manager = context.getPackageManager();
		PackageInfo info;
		try {
			info = manager.getPackageInfo(context.getPackageName(), 0);
			versie = "V" + info.versionName;
			final int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			// tcLog.d(context.getClass().getName(), "buildVersion versie = " +
			// versie + " api = " + currentapiVersion);
			if (isDebuggable(context) && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
				versie += "/" + info.lastUpdateTime / (1000 * 60);
			}
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
			if (versie == null) {
				versie = "V0.3x";
			}
		}
		// tcLog.d(context.getClass().getName(), "buildVersion versie = " +
		// versie);
		return versie;
	}

	public static String makeDbPath(Context context, String dbname) {
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

	public static String makeExtFilePath(String filename) throws FileNotFoundException {
		final File extpath = Environment.getExternalStorageDirectory();

		final String filePath = extpath.toString() + "/TracClient/" + filename;

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			throw new FileNotFoundException(filePath);
		}
		return filePath;
	}
}
