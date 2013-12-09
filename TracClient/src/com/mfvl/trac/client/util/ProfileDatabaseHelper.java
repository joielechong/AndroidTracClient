package com.mfvl.trac.client.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mfvl.trac.client.saxparser.XMLHandler;

public class ProfileDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "profile.db";
	private static final int DATABASE_VERSION = 2;
	public static final String TABLE_NAME = "profiles";
	public static final String NAME_ID = "name";
	public static final String URL_ID = "url";
	public static final String USERNAME_ID = "username";
	public static final String PASSWORD_ID = "password";
	public static final String SSLHACK_ID = "sslhack";
	public SQLiteDatabase db = null;
	public boolean upgrade = false;

	public ProfileDatabaseHelper(Context context) {
		super(context, Credentials.makeDbPath(context, DATABASE_NAME), null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		final String CREATE_PROFILE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + NAME_ID + " TEXT PRIMARY KEY," + URL_ID + " TEXT,"
				+ USERNAME_ID + " TEXT," + PASSWORD_ID + " TEXT," + SSLHACK_ID + " BOOLEAN" + ")";
		db.execSQL(CREATE_PROFILE_TABLE);
		db.execSQL("insert into " + TABLE_NAME + "(" + NAME_ID + ") VALUES ('')");
		upgrade = true;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
		} else {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
		upgrade = true;
	}

	public void open() {
		db = this.getWritableDatabase();
		if (upgrade) {
			final LoginProfile ex1 = new LoginProfile("http://van-loon.xs4all.nl/TracClient/rpc", "", "", false);
			final LoginProfile ex2 = new LoginProfile("https://van-loon.xs4all.nl/TracClient/login/rpc", "demo", "demo", true);
			addProfile("TracClient-RO", ex1);
			addProfile("TracClient-login", ex2);
			upgrade = false;
		}
	}

	@Override
	public void close() {
		db.close();
	}

	public void beginTransaction() {
		Log.d(this.getClass().getName(), "beginTransaction db = " + db);
		if (db == null) {
			this.open();
		}
		db.beginTransaction();
	}

	public void endTransaction() {
		Log.d(this.getClass().getName(), "endTransaction db = " + db);
		if (db == null) {
			this.open();
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	public void addProfile(String name, LoginProfile profile) throws SQLException {
		Log.d(this.getClass().getName(), "addProfile name = " + name + " profile = " + profile + " db = " + db);

		final ContentValues values = new ContentValues();
		values.put(NAME_ID, name);
		values.put(URL_ID, profile.getUrl());
		values.put(USERNAME_ID, profile.getUsername());
		values.put(PASSWORD_ID, profile.getPassword());
		values.put(SSLHACK_ID, profile.getSslHack());

		if (db == null) {
			this.open();
		}
		db.insertOrThrow(TABLE_NAME, null, values);
	}

	public Cursor getProfiles() {
		if (db == null) {
			this.open();
		}
		final Cursor c = db.rawQuery("SELECT rowid as _id,name from " + TABLE_NAME, null);
		return c;
	}

	public Cursor getAllProfiles() {
		if (db == null) {
			this.open();
		}
		final Cursor c = db.rawQuery("SELECT " + NAME_ID + "," + URL_ID + "," + USERNAME_ID + "," + PASSWORD_ID + "," + SSLHACK_ID
				+ " from " + TABLE_NAME + " WHERE " + NAME_ID + " !=''", null);
		return c;
	}

	public LoginProfile getProfile(String name) {
		LoginProfile profile = null;

		if (db == null) {
			this.open();
		}
		final Cursor c = db.query(TABLE_NAME, new String[] { URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID }, "name=?",
				new String[] { name }, null, null, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			profile = new LoginProfile(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1);
		}
		return profile;
	}

	public void delProfile(String name) {
		if (db == null) {
			this.open();
		}
		final String values[] = new String[] { name };
		db.delete(TABLE_NAME, "name=?", values);
	}

	public int delProfiles() {
		Log.d(this.getClass().getName(), "delProfiles db = " + db);
		if (db == null) {
			this.open();
		}
		final String values[] = new String[] { "" };
		return db.delete(TABLE_NAME, "name!=?", values);
	}

	public void readXML(final String appname) throws Exception {
		Log.d(this.getClass().getName(), "readXML appname = " + appname + " db = " + db);
		if (db == null) {
			this.open();
		}
		final String fileName = Credentials.makeExtFilePath(appname + ".xml");
		final InputStream in = new BufferedInputStream(new FileInputStream(fileName));
		try {

			/**
			 * Create a new instance of the SAX parser
			 **/
			final SAXParserFactory saxPF = SAXParserFactory.newInstance();
			final SAXParser saxP = saxPF.newSAXParser();
			final XMLReader xmlR = saxP.getXMLReader();

			/**
			 * Create the Handler to handle each of the XML tags.
			 **/
			final XMLHandler myXMLHandler = new XMLHandler(appname, this);
			xmlR.setContentHandler(myXMLHandler);
			xmlR.parse(new InputSource(in));

		} catch (final Exception e) {
			System.out.println(e);
		}

	}

	public void writeXML(final String appname) throws Exception {
		final String fileName = Credentials.makeExtFilePath(appname + ".xml");
		final OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

		String xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n";
		xmlString += "<" + appname + ">\n";
		xmlString += "<" + TABLE_NAME + ">\n";
		final Cursor c = getAllProfiles();
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			xmlString += "<profile " + NAME_ID + "=\"" + c.getString(0) + "\" " + URL_ID + "=\"" + c.getString(1) + "\" "
					+ USERNAME_ID + "=\"" + c.getString(2) + "\" " + PASSWORD_ID + "=\"" + c.getString(3) + "\" " + SSLHACK_ID
					+ "=\"" + c.getInt(4) + "\" />\n";
		}
		c.close();
		xmlString += "</" + TABLE_NAME + ">\n";
		xmlString += "</" + appname + ">\n";

		final byte[] bytes = xmlString.getBytes("UTF-8");
		out.write(bytes, 0, bytes.length);
		out.close();
	}
}
