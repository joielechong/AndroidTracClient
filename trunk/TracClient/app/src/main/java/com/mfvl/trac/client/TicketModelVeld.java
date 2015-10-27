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


import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TicketModelVeld {

    private String _name;
    private String _label;
    private String _type;
    private String _format;
    private String _value;
    private List<Object> _options = null;
    private boolean _optional;
    @SuppressWarnings("FieldCanBeLocal")
    private int _order;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean _custom;

    public TicketModelVeld(String name, String label, String value) {
        _name = name;
        _label = label;
        _value = value;
        _optional = false;
        _options = null;
    }

	private String getNeededField(final JSONObject v,final String field) throws RuntimeException {
        try {
            return v.getString(field);
        } catch (final JSONException e) {
            throw new RuntimeException("Missing "+field+" in field definition", e);
        }
	}

    public TicketModelVeld(final JSONObject v) throws RuntimeException {
        if (v == null) {
            throw new RuntimeException("JSONObject is null");
        }
		
		_name = getNeededField(v,"name");
		_label = getNeededField(v,"label");
		_type = getNeededField(v,"type");
        try {
            _custom = v.getString("custom").equals("true");
        } catch (final JSONException e) {
            _custom = false;
        }
        try {
            _order = Integer.parseInt(v.getString("order"));
        } catch (final JSONException e) {
            _order = 0;
        }

        if (_type.equals("text")) {
            try {
                _format = v.getString("format");
            } catch (final JSONException e) {
                _format = "plain";
            }
        }

        if (_type.equals("select") || _type.equals("radio")) {
            try {
                final JSONArray ja = v.getJSONArray("options");
                final int count = ja.length();

                _options = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    _options.add(ja.getString(i));
                }
            } catch (final JSONException e) {
                throw new RuntimeException("No options", e);
            }
            try {
                _value = v.getString("value");
            } catch (final JSONException ignored) {}
            try {
                _optional = v.getString("optional").equals("true");
            } catch (final JSONException e) {
                _optional = false;
            }
        }
    }

    @Override
    public String toString() {
        return _name + " (" + _label + ")[" + _format + "]";
    }

    public String name() {
        return _name;
    }

    public String label() {
        return _label;
    }

    public String type() {
        return _type;
    }

    public String format() {
        return _format;
    }

    public String value() {
        return _value;
    }

    public List<Object> options() {
        return _options;
    }

    public boolean optional() {
        return _optional;
    }

}
