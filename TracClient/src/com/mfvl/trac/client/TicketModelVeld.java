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
	private int _order;
	private boolean _custom;
	private boolean _canChange = false;

	public TicketModelVeld(String name, String label, String value) {
		_name = name;
		_label = label;
		_value = value;
		_optional = false;
		_options = null;
	}

	private int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		// Start with a non-zero constant.
		int result = 17;

		// Include a hash for each field.
		result = 31 * result + hc(_name);
		result = 31 * result + hc(_label);
		result = 31 * result + hc(_type);
		result = 31 * result + hc(_format);
		result = 31 * result + hc(_value);
		result = 31 * result + hc(_options);
		result = 31 * result + hc(_optional);
		result = 31 * result + hc(_order);
		result = 31 * result + hc(_custom);
		result = 31 * result + hc(_canChange);
		return result + super.hashCode();
	}

	public TicketModelVeld(final JSONObject v) throws TicketModelException {
		if (v == null) {
			throw new TicketModelException("JSONObject is null");
		}
		try {
			_name = v.getString("name");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen naam in velddefinitie", e);
		}
		try {
			_label = v.getString("label");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen label in velddefinitie", e);
		}
		try {
			_type = v.getString("type");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen type in velddefinitie", e);
		}
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
				_options = new ArrayList<Object>();
				for (int i = 0; i < count; i++) {
					_options.add(ja.getString(i));
				}
			} catch (final JSONException e) {
				throw new TicketModelException("Geen opties in velddefinitie", e);
			}
			try {
				_value = v.getString("value");
			} catch (final JSONException e) {
			}
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

	public int order() {
		return _order;
	}

	public boolean custom() {
		return _custom;
	}

	public boolean canChange() {
		return _canChange;
	}

	public void setChange(boolean c) {
		_canChange = c;
	}

}
