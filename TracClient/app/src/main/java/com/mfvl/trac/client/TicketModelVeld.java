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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class TicketModelVeld {

    private final String _name;
    private final String _label;
    private String _type = null;
    private String _format = null;
    private String _value = null;
    private List<Object> _options;
    private boolean _optional = false;
//    private int _order;
//    private boolean _custom;

    TicketModelVeld(String name, String label, String value) {
        _name = name;
        _label = label;
        _value = value;
        _optional = false;
        _options = null;
    }

    TicketModelVeld(final JSONObject v) throws RuntimeException {
        if (v == null) {
            throw new RuntimeException("JSONObject is null");
        }

        _options = null;
        _name = getNeededField(v, "name");
        _label = getNeededField(v, "label");
        _type = getNeededField(v, "type");
/*
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
*/
        if ("text".equals(_type)) {
            try {
                _format = v.getString("format");
            } catch (final JSONException e) {
                _format = "plain";
            }
        }

        if ("select".equals(_type) || "radio".equals(_type)) {
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
            } catch (final JSONException ignored) {
            }
            try {
                _optional = "true".equals(v.getString("optional"));
            } catch (final JSONException e) {
                _optional = false;
            }
        }
    }

    private String getNeededField(final JSONObject v, final String field) throws RuntimeException {
        try {
            return v.getString(field);
        } catch (final JSONException e) {
            throw new RuntimeException("Missing " + field + " in field definition", e);
        }
    }

    @Override
    public String toString() {
        return _name + " (" + _label + ")[" + _format + "]";
    }

    String name() {
        return _name;
    }

    String label() {
        return _label;
    }

    String type() {
        return _type;
    }

    String format() {
        return _format;
    }

    String value() {
        return _value;
    }

    List<Object> options() {
        return _options;
    }

    boolean optional() {
        return _optional;
    }
}
