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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Xml;

import com.mfvl.mfvllib.FileOps;
import com.mfvl.mfvllib.MyLog;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.SAXParserFactory;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

interface PDHelper {
    void open();

    void close();

    void beginTransaction();

    void endTransaction();

    void addProfile(String name, LoginProfile lp);

    LoginProfile getProfile(String naam);

    Cursor getProfiles(boolean addBlank);

    LoginProfile findProfile(String url);

    int delProfiles();

    void readXML(String s) throws Exception;

    void writeXML(String s) throws Exception;
}

class ProfileDatabaseHelper extends SQLiteOpenHelper implements PDHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "profiles";
    private static final String NAME_ID = "name";
    private static final String URL_ID = "url";
    private static final String USERNAME_ID = "username";
    private static final String PASSWORD_ID = "password";
    private static final String SSLHACK_ID = "sslhack";
    private final Context context;
    private SQLiteDatabase sqlDb = null;
    private boolean upgrade = false;

    ProfileDatabaseHelper(Context ctx) {
        super(ctx, FileOps.makeDbPath(ctx, null, DATABASE_NAME, (isRCVersion() ? FileOps.VISIBLE : 0)), null, DATABASE_VERSION);
        context = ctx;
        sendNotification("class creation");
    }

    @Override
    public void open() {
        if (sqlDb == null) {
            sqlDb = getWritableDatabase();
            if (upgrade) {
                Resources res = context.getResources();
                TypedArray ta = res.obtainTypedArray(R.array.profiles);
                try {

                    for (int i = 0; i < ta.length(); ++i) {
                        int resId = ta.getResourceId(i, 0);
                        String[] values = res.getStringArray(resId);

                        addProfile(values[0], new LoginProfileImpl(values[1], values[2], values[3],
                                "true".equals(values[4])));
                        // MyLog.d("i = "+i+" values = "+Arrays.asList(values));
                    }
                } finally {
                    ta.recycle();
                }
                upgrade = false;
            }
        }
    }

    @Override
    public void close() {
        sqlDb.close();
        sqlDb = null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_PROFILE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + NAME_ID + " TEXT PRIMARY KEY," + URL_ID + " TEXT,"
                + USERNAME_ID + " TEXT," + PASSWORD_ID + " TEXT," + SSLHACK_ID + " BOOLEAN" + ")";

        db.execSQL(CREATE_PROFILE_TABLE);
        db.execSQL("insert into " + TABLE_NAME + "(" + NAME_ID + ") VALUES ('')");
        upgrade = true;
        sendNotification("Databae opened");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //noinspection StatementWithEmptyBody
        if (oldVersion == 1 && newVersion == 2) {
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
        upgrade = true;
    }

    @Override
    public void beginTransaction() {
        open();
        sqlDb.beginTransaction();
    }

    @Override
    public void endTransaction() {
        sqlDb.setTransactionSuccessful();
        sqlDb.endTransaction();
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(DB_UPDATED);
        intent.putExtra(DB_UPDATED, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void addProfile(String name, LoginProfile profile) throws SQLException {
        final ContentValues values = new ContentValues();

        values.put(NAME_ID, name);
        values.put(URL_ID, profile.getUrl());
        values.put(USERNAME_ID, profile.getUsername());
        values.put(PASSWORD_ID, profile.getPassword());
        values.put(SSLHACK_ID, profile.getSslHack());

        open();
        try {
            sqlDb.insertOrThrow(TABLE_NAME, null, values);
        } catch (final SQLException e) {
            sqlDb.replaceOrThrow(TABLE_NAME, null, values);
        } finally {
            sendNotification("add profile");
        }
    }

    @Override
    public Cursor getProfiles(boolean addBlank) {
        MyLog.d("addBlank = " + addBlank);
        open();
        return sqlDb.rawQuery(
                "SELECT rowid as _id,name from " + TABLE_NAME + (!addBlank ? " WHERE " + NAME_ID + " !=''" : "") + " ORDER BY name",
                null);
    }

    private Cursor getAllProfiles() {
        open();
        return sqlDb.query(TABLE_NAME, new String[]{NAME_ID, URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID}, NAME_ID + " !=''", null, null, null, null);
    }

    @Override
    public LoginProfile getProfile(String name) {

        open();
        final Cursor c = sqlDb.query(TABLE_NAME,
                new String[]{URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID},
                NAME_ID + "=?",
                new String[]{name}, null, null, null);

        LoginProfile profile = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            profile = new LoginProfileImpl(c.getString(0), c.getString(1), c.getString(2),
                    c.getInt(3) == 1);
        }
        c.close();
        return profile;
    }

    @Override
    public LoginProfile findProfile(String url) {

        open();
        final Cursor c = sqlDb.query(TABLE_NAME,
                new String[]{URL_ID, USERNAME_ID, PASSWORD_ID, SSLHACK_ID},
                URL_ID + "=?",
                new String[]{url}, null, null, null);

        LoginProfile profile = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            profile = new LoginProfileImpl(c.getString(0), c.getString(1), c.getString(2),
                    c.getInt(3) == 1);
        }
        c.close();
        return profile;
    }

    @Override
    public int delProfiles() {
        try {
            open();
            final String values[] = new String[]{""};

            return sqlDb.delete(TABLE_NAME, "name!=?", values);
        } finally {
            sendNotification("delProfiles");
        }
    }

    @Override
    public void readXML(final String appname) throws Exception {
        try {
            open();
            final File fileName = FileOps.makeExtFilePath(context, "TracClient", appname + ".xml", true);
            final InputStream in = new BufferedInputStream(new FileInputStream(fileName));
            final XMLReader xmlR = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

            /**
             * Create the Handler to handle each of the XML tags.
             **/
            xmlR.setContentHandler(new XMLHandler(appname, this));
            xmlR.parse(new InputSource(in));
        } finally {
            sendNotification("read XML");
        }
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    @Override
    public void writeXML(final String appname) throws Exception {
        final File fileName = FileOps.makeExtFilePath(context, "TracClient", appname + ".xml", true);
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
        XmlSerializer serializer = Xml.newSerializer();

        try {
            serializer.setOutput(out, "UTF-8");
            serializer.startDocument("UTF-8", false);
            serializer.startTag("", appname);
            serializer.startTag("", TABLE_NAME);
            final Cursor c = getAllProfiles();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                //MyLog.d(c.getString(0));
                serializer.startTag("", "profile");
                serializer.attribute("", NAME_ID, c.getString(0));
                serializer.attribute("", URL_ID, c.getString(1));
                serializer.attribute("", USERNAME_ID, c.getString(2));
                serializer.attribute("", PASSWORD_ID, c.getString(3));
                serializer.attribute("", SSLHACK_ID, c.getString(4));
                serializer.endTag("", "profile");
            }
            c.close();
            serializer.endTag("", TABLE_NAME);
            serializer.endTag("", appname);
            serializer.flush();
        } finally {
            out.close();
        }
    }

    private class XMLHandler extends DefaultHandler {
        private final PDHelper _pdb;
        String _appname = null;
        private int state = -1;
        private String profileName = null;
        private LoginProfile lp = null;

        XMLHandler(String appname, PDHelper pdb) {
            super();
            _appname = appname;
            _pdb = pdb;
            state = 0;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws
                SAXException,
                RuntimeException {
            //MyLog.d(localName);
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
                            MyLog.e("delProfiles mislukt");
                            throw new RuntimeException("delProfiles mislukt");
                        }
                    }
                    break;

                case 2:
                    if ("profile".equals(localName)) {
                        state++;
                        lp = new LoginProfileImpl(attributes.getValue(ProfileDatabaseHelper.URL_ID),
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
}
