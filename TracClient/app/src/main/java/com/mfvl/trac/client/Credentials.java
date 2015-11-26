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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Environment;

import static com.mfvl.trac.client.Const.*;

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

    private Credentials(final Context context) {
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        _context = context;
 		versie = context.getString(R.string.app_version);
    }

    public static Credentials getInstance(final Context context) {
        if (_instance == null ) {
            _instance = new Credentials(context);
			loadCredentials();
        }
        return _instance;
    }

    /**
     * Load login credentials from shared preferences: server-url, username, password and profile
     */
    private static void loadCredentials() {
//        tcLog.logCall();
        _url = settings.getString(PREF_URL, "");
        _username = settings.getString(PREF_USER, "");
        _password = settings.getString(PREF_PASS, "");
        _sslHack = settings.getBoolean(PREF_HACK, false);
        _sslHostNameHack = settings.getBoolean(PREF_HNH, false);
        _profile = settings.getString(PREF_PROF, null);
    }

    /**
     * Store login credentials to shared preferences: server-url, username, password and profile
     */
    public static void storeCredentials() {
//        tcLog.logCall();
        settings.edit()
			.putString(PREF_URL, _url)
			.putString(PREF_USER, _username)
			.putString(PREF_PASS, _password)
			.putBoolean(PREF_HACK, _sslHack)
			.putBoolean(PREF_HNH, _sslHostNameHack)
			.putString(PREF_PROF, _profile)
			.apply();
    }

    /** Set login credentials server-url, username, password and profile */
    public static void setCredentials(final String url, final String username, final String password, final String profile) {
//        tcLog.logCall();
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

    public static boolean checkDisclaimer() {
//        tcLog.logCall();
        final String thisRun = DisclaimerVersion;
        final String lastRun = settings.getString(PREF_DISCLAIM, "");
	
        return !lastRun.equals(thisRun);
    }
	
	public static void setDisclaimer() {
		settings.edit().putString(PREF_DISCLAIM, DisclaimerVersion).apply();
	}

    public static boolean getFirstRun() {
        // tcLog.d("getFirstRun");
        final String thisRun = versie;
        final String lastRun = settings.getString(PREF_1ST, "");
        settings.edit().putString(PREF_1ST, thisRun).apply();
        return !lastRun.equals(thisRun);
    }

    public static boolean getCookieInform() {
//        tcLog.logCall();
		return settings.getBoolean(PREF_COOKIEINFORM,true);
	}
	
	public static void setCookieInform(boolean val) {
//        tcLog.logCall();
		settings.edit().putBoolean(PREF_COOKIEINFORM, val).apply();
	}

    public static void storeFilterString(final String filterString) {
        //tcLog.d(filterString);
        settings.edit().putString(PREF_FILTER, filterString == null ? "" : filterString).apply();
    }

    public static String getFilterString() {
        // tcLog.logCall();
        return settings.getString(PREF_FILTER, "max=500&status!=closed");
    }

    public static void removeFilterString() {
        // tcLog.logCall();
		storeFilterString("max=500&status!=closed");
    }

    public static void storeSortString(final String sortString) {
        //tcLog.d(sortString);
        settings.edit().putString(PREF_SORT, sortString == null ? "" : sortString).apply();
    }

    public static String getSortString() {
        // tcLog.logCall();
        final String sortString = settings.getString(PREF_SORT, "order=priority&order=modified&desc=1");

        tcLog.d("sortString = " + sortString);
        return sortString;
    }

    public static void removeSortString() {
        // tcLog.logCall();
        storeSortString("order=priority&order=modified&desc=1");
    }

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");

    public static boolean isDebuggable() {
        boolean debuggable = false;

        try {
            @SuppressLint("PackageManagerGetSignatures")
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
        } catch (final NameNotFoundException | CertificateException e) {
            tcLog.w(e);
        }
        return debuggable;
    }

	public static String getVersion() {
		return versie;
	}

    public static boolean isRCVersion() {
		return (versie != null) && (versie.toLowerCase(Locale.US).contains("rc"));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String makeDbPath(String dbname) {

        String dbpath = dbname;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			final File dirPath = _context.getExternalFilesDir(null);
//			tcLog.d("dirpath = "+dirPath);
			final File filePath = new File(dirPath,dbname);
//			tcLog.d("filePath = "+filePath);
            final String p1 = filePath.toString();

            if (!isDebuggable() && !isRCVersion()) {
                if (new File(p1).exists()) {
                    dbpath = p1;
                }
            } else {
				dbpath = p1;
            }
        }
        tcLog.d("dbpath = " + dbpath);
        return dbpath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File makeExtFilePath(String filename,boolean visible) throws FileNotFoundException {
		File dirPath;
//		tcLog.d("filename = "+filename);
		if (visible) {
			final File extPath = Environment.getExternalStorageDirectory();
//			tcLog.d("extpath = "+extPath);
			dirPath = new File(extPath,"TracClient");
		} else {
			dirPath = _context.getExternalFilesDir(null);
			dirPath.mkdirs();
		}
//		tcLog.d("dirpath = "+dirPath);
		if (!dirPath.isDirectory()) {
            throw new FileNotFoundException("Not a directory: "+dirPath.toString());
		}
        final File filePath = new File(dirPath,filename);
//		tcLog.d("filepath = "+filePath);
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new FileNotFoundException(filePath.toString());
        }
        return filePath;
    }

    public static File makeCacheFilePath(final String filename) {
        final File extPath = _context.getExternalCacheDir();
        final File filePath = new File(extPath,filename);
		tcLog.d("filepath = "+filePath);
        return filePath;
    }
	
    public static Boolean metaDataGetBoolean(String metaId) throws NameNotFoundException {
        return (_context != null
                ? _context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA).metaData.getBoolean(metaId): null);
    }
	
    public static String metaDataGetString(String metaId) throws NameNotFoundException {
        return (_context != null
                ? _context.getPackageManager().getApplicationInfo(_context.getPackageName(), PackageManager.GET_META_DATA).metaData.getString(metaId): null);
    }
	
	public static String joinList(Object list[], final String sep) {
		String reqString = "";

		for (final Object fs : list) {
			if (fs != null) {
				if (reqString.length() > 0) {
					reqString += sep;
				}
				reqString += fs.toString();
			}
		}
		return reqString;
	} 
}
