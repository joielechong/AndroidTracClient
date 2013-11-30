package com.mfvl.trac.client.util;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

public class ProfileDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "profile.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TABLE_NAME = "profiles";
	private static final String NAME_ID = "name";
	private static final String URL_ID = "url";
	private static final String USERNAME_ID = "username";
	private static final String PASSWORD_ID = "password";
	private static final String SSLHACK_ID = "sslhack";
	private SQLiteDatabase db = null;

	private static File path = Environment.getExternalStorageDirectory();

	public ProfileDatabaseHelper(Context context, boolean publ) {
		super(context, publ && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? path.toString()
				+ "/TracClient/" + DATABASE_NAME : DATABASE_NAME, null, DATABASE_VERSION);
		// super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		final String CREATE_PROFILE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + NAME_ID + " TEXT PRIMARY KEY," + URL_ID + " TEXT,"
				+ USERNAME_ID + " TEXT," + PASSWORD_ID + " TEXT," + SSLHACK_ID + " BOOLEAN" + ")";
		db.execSQL(CREATE_PROFILE_TABLE);
		db.execSQL("insert into " + TABLE_NAME + "(" + NAME_ID + ") VALUES ('')");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public void open() {
		db = this.getWritableDatabase();
	}

	@Override
	public void close() {
		db.close();
	}

	public void addProfile(String name, LoginProfile profile) {

		final ContentValues values = new ContentValues();
		values.put(NAME_ID, name);
		values.put(URL_ID, profile.getUrl());
		values.put(USERNAME_ID, profile.getUsername());
		values.put(PASSWORD_ID, profile.getPassword());
		values.put(SSLHACK_ID, profile.getSslHack());

		if (db == null) {
			this.open();
		}
		db.insert(TABLE_NAME, null, values);
	}

	public Cursor getProfiles() {
		if (db == null) {
			this.open();
		}
		final Cursor c = db.rawQuery("SELECT rowid as _id,name from " + TABLE_NAME, null);
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
}
