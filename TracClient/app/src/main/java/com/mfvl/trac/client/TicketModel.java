/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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

import android.os.Bundle;

import com.mfvl.mfvllib.MyLog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

final class TicketModel {
    final static String bundleKey = "TicketModelObject";
    private final static List<String> extraFields = Arrays.asList("max", "page");
    private final static List<String> extraValues = Arrays.asList("500", "0");
    private static Map<String, TicketModelVeld> _velden = null;
    private static List<String> _volgorde = null;
    private static int fieldCount = 0;
    private static TicketModel _instance = null;
    private static boolean _hasData = false;
    private static TracHttpClient _tracClient = null;
    private static Semaphore active = null;
    private static JSONArray v = null;

    private TicketModel(TracHttpClient tracClient) {
        MyLog.logCall();
        fieldCount = 0;
        _tracClient = tracClient;
        _hasData = false;
        _velden = new HashMap<>();
        _volgorde = new ArrayList<>();
        active = new Semaphore(1, true);
    }

    static TicketModel restore(String jsonString) {
        MyLog.logCall();
        try {
            JSONObject o = new JSONObject(jsonString);
            JSONObject h = o.getJSONObject("HttpClient");
            v = o.getJSONArray("Model");
            fieldCount = v.length();
            _instance = new TicketModel(new TracHttpClient(h));
            processModelData(v);
//            MyLog.d("_instance = " + _instance + " tracClient = " + _tracClient);
            return _instance;
        } catch (NullPointerException | JSONException e) {
            MyLog.e(e);
            return null;
        }
    }

    static void getInstance(TracHttpClient tracClient, final OnTicketModelListener oc) {
        //MyLog.d("instance = " + _instance);
        //MyLog.d("old tracClient = " + _tracClient);
        //MyLog.d("new tracClient = " + tracClient);
        if (_instance == null || !tracClient.equals(_tracClient)) {
            _instance = new TicketModel(tracClient);
        }
        if (_hasData) {
            oc.onTicketModelLoaded(_instance);
        } else {
            if (active.availablePermits() > 0) {
                _instance.loadModelData(oc);
            } else {
                active.acquireUninterruptibly();
                active.release();
                oc.onTicketModelLoaded(_instance);
            }
        }
    }

    static TicketModel getInstance() {
        //MyLog.d("noargs _instance = " + _instance);
        if (_instance == null) {
            throw new RuntimeException("No ticketmodel available");
        }
        return _instance;
    }

    static void delInstance() {
        _instance = null;
    }

    private static void processModelData(JSONArray veld) throws JSONException {
        fieldCount = veld.length();
        for (int i = 0; i < fieldCount; i++) {
            final String key = veld.getJSONObject(i).getString("name");

            _velden.put(key, new TicketModelVeld(veld.getJSONObject(i)));
            _volgorde.add(key);
        }
        for (int i = 0; i < extraFields.size(); i++) {
            String v1 = extraFields.get(i);
            _velden.put(v1, new TicketModelVeld(v1, v1, extraValues.get(i)));
            _volgorde.add(v1);
        }
        _hasData = true;
    }

    private String jsonString() {
        MyLog.logCall();
        try {
            JSONObject o = new JSONObject();
            o.put("Model", v);
            o.put("HttpClient", _tracClient.toJSON());
            return o.toString();
        } catch (JSONException e) {
            MyLog.e(e);
            return null;
        }
    }

    public void onSaveInstanceState(Bundle b) {
        MyLog.logCall();
        b.putString(bundleKey, jsonString());
        //MyLog.d("b = " + b);
    }

    private void loadModelData(final OnTicketModelListener oc) {
        MyLog.logCall();
        if (_tracClient != null) {
            MyLog.logCall();
            active.acquireUninterruptibly();
            new Thread() {
                @Override
                public void run() {
                    //MyLog.d("TicketModel loading model tracClient = " + _tracClient);
                    try {
                        v = _tracClient.getModel();
                        processModelData(v);
                        MyLog.d("Model loaded");
                        oc.onTicketModelLoaded(_instance);
                    } catch (final Exception e) {
                        MyLog.e("exception", e);
                    } finally {
                        active.release();
                    }
                }
            }.start();
        } else {
            MyLog.e("called with url == null");
        }
    }

    @Override
    public String toString() {
        wacht();
        String s = "";

        if (_velden != null) {
            for (String volg : _volgorde) {
                s += _velden.get(volg) + "\n";
            }
        } else {
            s += "TicketModel not initialized";
        }
        return s;
    }

    private void wacht() {
        if (_velden == null && active.availablePermits() == 0) {
            active.acquireUninterruptibly();
            active.release();
        }
    }

    public ArrayList<String> velden() {
        final ArrayList<String> veldlijst = new ArrayList<>();

        wacht();
        if (fieldCount > 0) {
            veldlijst.add("id");
            for (String veld : _volgorde) {
                if (!extraFields.contains(veld)) {
                    veldlijst.add(_velden.get(veld).name());
                }
            }
        }
        return veldlijst;
    }

    public int count() {
        wacht();
        return fieldCount;
    }

    TicketModelVeld getVeld(final int i) throws IndexOutOfBoundsException {
        wacht();
        if (i < 0 || i >= fieldCount) {
            throw new IndexOutOfBoundsException();
        }
        return getVeld(_volgorde.get(i));
    }

    TicketModelVeld getVeld(final String naam) throws IndexOutOfBoundsException {
        wacht();
        if (_velden.containsKey(naam)) {
            return _velden.get(naam);
        }
        if ("id".equals(naam)) {
            return new TicketModelVeld(naam, naam, "0");
        }
        throw new IndexOutOfBoundsException();
    }
}
