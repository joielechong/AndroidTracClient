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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.res.TypedArray;
import android.content.res.Resources;


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
    private Context _context;

    public class XMLHandler extends DefaultHandler {

        String _appname = null;
        private int state = -1;
        private String profileName;
        private LoginProfile lp;
        private final ProfileDatabaseHelper _pdb;

        public XMLHandler(String appname, ProfileDatabaseHelper pdb) {
            super();
            _appname = appname;
            _pdb = pdb;
            state = 0;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException,           RuntimeException {
			switch (state) {
				case 0:
				if (localName.equals(_appname)) {
					state++;
				}
				break;
				
				case 1:			
				if (localName.equals(ProfileDatabaseHelper.TABLE_NAME)) {
					state++;
					_pdb.beginTransaction();
					if (_pdb.delProfiles() == -1) {
						throw new RuntimeException("delProfiles mislukt");
					}
				}
				break;
				
				case 2:
				if ("profile".equals(localName)) {
					state++;
					lp = new LoginProfile(attributes.getValue(ProfileDatabaseHelper.URL_ID),
                        attributes.getValue(ProfileDatabaseHelper.USERNAME_ID),
                        attributes.getValue(ProfileDatabaseHelper.PASSWORD_ID),
                        "1".equals(attributes.getValue(ProfileDatabaseHelper.SSLHACK_ID)));
					profileName = attributes.getValue(ProfileDatabaseHelper.NAME_ID);
				}
				break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
			switch (state) {	
				case 1:
				if (localName.equals(_appname)) {
					state--;
				}
				break;
				
				case 2:
				if (localName.equals(ProfileDatabaseHelper.TABLE_NAME)) {
					_pdb.endTransaction();
					state--;
				}
				break;
				
				case 3:
				if ("profile".equals(localName)) {
					_pdb.addProfile(profileName, lp);
					state--;
				}
				break;
			}
        }
    }

    public ProfileDatabaseHelper(Context context) {
        super(context, Credentials.makeDbPath(DATABASE_NAME), null, DATABASE_VERSION);
        _context = context;
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
        if (oldVersion == 1 && newVersion == 2) {} else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
        upgrade = true;
    }

    public void open() {
        db = getWritableDatabase();
        if (upgrade) {
            Resources res = _context.getResources();
            TypedArray ta = res.obtainTypedArray(R.array.profiles);

            for (int i = 0; i < ta.length(); ++i) {
                int resId = ta.getResourceId(i, 0);
                String[] values = res.getStringArray(resId);

                addProfile(values[0], new LoginProfile(values[1], values[2], values[3], values[4] == "true"));
                // tcLog.d(getClass().getName(),"i = "+i+" values = "+Arrays.asList(values));
            }
            ta.recycle();
            upgrade = false;
        }
    }

    @Override
    public void close() {
        db.close();
    }

    public void beginTransaction() {
        if (db == null) {
            open();
        }
        db.beginTransaction();
    }

    public void endTransaction() {
        if (db == null) {
            open();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public void addProfile(String name, LoginProfile profile) throws SQLException {
        final ContentValues values = new ContentValues();

        values.put(NAME_ID, name);
        values.put(URL_ID, profile.getUrl());
        values.put(USERNAME_ID, profile.getUsername());
        values.put(PASSWORD_ID, profile.getPassword());
        values.put(SSLHACK_ID, profile.getSslHack());

        if (db == null) {
            open();
        }
        try {
            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (final SQLException e) {
            db.replaceOrThrow(TABLE_NAME, null, values);
        }
    }

    public Cursor getProfiles() {
        if (db == null) {
            open();
        }
        final Cursor c = db.rawQuery("SELECT rowid as _id,name from " + TABLE_NAME + " ORDER BY name", null);

        return c;
    }

    public Cursor getAllProfiles() {
        if (db == null) {
            open();
        }
        final Cursor c = db.rawQuery(
                "SELECT " + NAME_ID + "," + URL_ID + "," + USERNAME_ID + "," + PASSWORD_ID + "," + SSLHACK_ID + " from "
                + TABLE_NAME + " WHERE " + NAME_ID + " !=''",
                null);

        return c;
    }

    public LoginProfile getProfile(String name) {
        LoginProfile profile = null;

        if (db == null) {
            open();
        }
        final Cursor c = db.query(TABLE_NAME, new String[] { URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID }, NAME_ID + "=?",
                new String[] { name }, null, null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            profile = new LoginProfile(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1);
        }
		c.close();
        return profile;
    }

    public LoginProfile findProfile(String url) {
        LoginProfile profile = null;

        if (db == null) {
            open();
        }
        final Cursor c = db.query(TABLE_NAME, new String[] { URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID }, URL_ID + "=?",
                new String[] { url }, null, null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            profile = new LoginProfile(c.getString(0), c.getString(1), c.getString(2), c.getInt(3) == 1);
        }
		c.close();
        return profile;
    }

    public void delProfile(String name) {
        if (db == null) {
            open();
        }
        final String values[] = new String[] { name };

        db.delete(TABLE_NAME, "name=?", values);
    }

    public int delProfiles() {
        if (db == null) {
            open();
        }
        final String values[] = new String[] { "" };

        return db.delete(TABLE_NAME, "name!=?", values);
    }

    public void readXML(final String appname) throws Exception {
        if (db == null) {
            open();
        }
        final String fileName = Credentials.makeExtFilePath(appname + ".xml");
        final InputStream in = new BufferedInputStream(new FileInputStream(fileName));
        final XMLReader xmlR = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

        /**
         * Create the Handler to handle each of the XML tags.
         **/
        xmlR.setContentHandler(new XMLHandler(appname, this));
        xmlR.parse(new InputSource(in));
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
