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


import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONArray;

import android.support.v4.util.ArrayMap;


public class TicketModel implements Serializable {
    private static final long serialVersionUID = 4307815225424930343L;
    private static ArrayMap<String, TicketModelVeld> _velden;
    private static ArrayMap<Integer, String> _volgorde;
    private static int fieldCount;
    private static boolean loading;
    private static Thread networkThread = null;
    private static TicketModel _instance = null;
    private static boolean _hasData;
    private static String tag = "**TicketModel**";
	private static TracHttpClient _tracClient = null;
	
    private TicketModel(TracHttpClient tracClient) {
        tag = getClass().getName();
        tcLog.d(tag, "TicketModel constructor");
        fieldCount = 0;
        loading = false;
		_tracClient = tracClient;
        _hasData = false;
        _velden = new ArrayMap<String, TicketModelVeld>();
        _velden.clear();
        _volgorde = new ArrayMap<Integer, String>();
        _volgorde.clear();
    }

    private void loadModelData() {
        if (_tracClient != null) {
            loading = true;
            networkThread = new Thread() {
                @Override
                public void run() {
                    tcLog.d(tag, "TicketModel tracClient = " + _tracClient);
                    try {
                        final JSONArray v = _tracClient.getModel();

                        fieldCount = v.length();
                        for (int i = 0; i < fieldCount; i++) {
                            final String key = v.getJSONObject(i).getString("name");

                            _velden.put(key, new TicketModelVeld(v.getJSONObject(i)));
                            _volgorde.put(i, key);
                        }
                        _velden.put("max", new TicketModelVeld("max", "max", "500"));
                        _volgorde.put(fieldCount, "max");
                        _velden.put("page", new TicketModelVeld("page", "page", "0"));
                        _volgorde.put(fieldCount + 1, "page");
                        _hasData = true;
                    } catch (final Exception e) {
                        tcLog.e(tag, "TicketModel exception", e);
                    } finally {
                        loading = false;
                    }
                }
            };
            networkThread.start();
        } else {
            tcLog.e(tag, "TicketModel called with url == null");
        }
    }

    public static TicketModel getInstance(TracHttpClient tracClient) {
        tcLog.d(tag, "TicketModel getInstance new tracClient = "+ tracClient);
        if (_instance == null  || tracClient.equals(_tracClient)) {
            _instance = new TicketModel(tracClient);
        }
        if (!_hasData && !loading) {
            _instance.loadModelData();
        }
        return _instance;
    }

    public static TicketModel getInstance() {
        tcLog.d(tag, "TicketModel getInstance old tracClient = "+ _tracClient);
        return _instance;
    }

    @Override
    public String toString() {
        wacht();
        String s = "";

        for (int i = 0; i < fieldCount; i++) {
            s += _velden.get(_volgorde.get(i)) + "\n";
        }
        return s;
    }

    public ArrayList<String> velden() {
        final ArrayList<String> v = new ArrayList<String>();

        wacht();
        if (fieldCount > 0) {
            v.add("id");
            for (int i = 0; i < fieldCount + 2; i++) {
                v.add(_velden.get(_volgorde.get(i)).name());
            }
        }
        return v;
    }

    public int count() {
        wacht();
        return fieldCount;
    }

    private void wacht() {
        if (loading) {
            try {
                networkThread.join();
            } catch (final Exception e) {
                tcLog.i(tag, "exception in wacht", e);
            }
        }
        loading = false;
    }
	
	boolean hasData() {
		return _hasData;
	}

    TicketModelVeld getVeld(final String naam) {
        wacht();
        return _velden.containsKey(naam) ? _velden.get(naam) : naam.equals("id") ? new TicketModelVeld(naam, naam, "0") : null;
    }

    TicketModelVeld getVeld(final int i) {
		wacht();
        return i < 0 || i >= fieldCount ? null : getVeld(_volgorde.get(i));
    }
}
