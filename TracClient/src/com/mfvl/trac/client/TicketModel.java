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
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;

public class TicketModel implements Serializable {
    private static final long serialVersionUID = 4307815225424930343L;
    private static HashMap<String, TicketModelVeld> _velden;
    private static ArrayList<String> _volgorde;
    private static int fieldCount;
    private static boolean loading;
    private static TicketModel _instance = null;
    private static boolean _hasData;
    private static String tag = "**TicketModel**";
	private static TracHttpClient _tracClient = null;
	private static Semaphore active = new Semaphore(1, true);	
	private static List<String> extraFields;
	private static List<String> extraValues;
	
	static {
		extraFields = Arrays.asList(new String[] {"max","page"});
		extraValues = Arrays.asList(new String[] {"500","0"});
	};
	
    private TicketModel(TracHttpClient tracClient) {
		
        tag = getClass().getName();
        tcLog.d(tag, "");
        fieldCount = 0;
        loading = false;
		_tracClient = tracClient;
        _hasData = false;
        _velden = new HashMap<String, TicketModelVeld>();
        _volgorde = new ArrayList<String>();
    }

    private void loadModelData() {
        tcLog.d(tag, "loadModelData");
        if (_tracClient != null) {
			loading = true;
			active.acquireUninterruptibly ();
            new Thread() {
                @Override
                public void run() {
                    //tcLog.d(tag, "TicketModel loading model tracClient = " + _tracClient);
                    try {
                        final JSONArray v = _tracClient.getModel();

                        fieldCount = v.length();
                        for (int i = 0; i < fieldCount; i++) {
                            final String key = v.getJSONObject(i).getString("name");

                            _velden.put(key, new TicketModelVeld(v.getJSONObject(i)));
                            _volgorde.add(key);
                        }
						for (int i=0;i<extraFields.size();i++) {
							String v1 = extraFields.get(i);
							_velden.put(v1,new TicketModelVeld(v1,v1,extraValues.get(i)));
							_volgorde.add(v1);
						}
                        _hasData = true;
                    } catch (final Exception e) {
                        tcLog.e(tag, "exception", e);
                    } finally {
						active.release();
						loading = false;
						tcLog.d(tag, "Model loaded");
                    }
                }
            }.start();
        } else {
            tcLog.e(tag, "called with url == null");
        }
    }

    public static TicketModel getInstance(TracHttpClient tracClient) {
        tcLog.d(tag, "new tracClient = "+ tracClient);
        if (_instance == null  || tracClient.equals(_tracClient)) {
            _instance = new TicketModel(tracClient);
        }
        if (!_hasData && !loading) {
            _instance.loadModelData();
        }
        return _instance;
    }

    public static TicketModel getInstance() {
        tcLog.d(tag, "old tracClient = "+ _tracClient);
        return _instance;
    }

    @Override
    public String toString() {
        wacht();
        String s = "";

		for (String v: _volgorde) {
            s += _velden.get(v) + "\n";
        }
        return s;
    }

    public ArrayList<String> velden() {
        final ArrayList<String> v = new ArrayList<String>();

        wacht();
        if (fieldCount > 0) {
            v.add("id");
			for (String v1: _volgorde) {
				if (!extraFields.contains(v1)) {
					v.add(_velden.get(v1).name());
				}
            }
        }
        return v;
    }

    public int count() {
        wacht();
        return fieldCount;
    }

    public void wacht() {
        if (loading) {
			active.acquireUninterruptibly ();
			active.release();
			loading = false;
		}
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
