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

	public TicketModelVeld(String name, String label, String value) {
		_name = name;
		_label = label;
		_value = value;
		_optional = false;
		_options = null;
	}

	public TicketModelVeld(final JSONObject v) throws TicketModelException {
		try {
			_name = v.getString("name");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen naam in velddefinitie");
		}
		try {
			_label = v.getString("label");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen label in velddefinitie");
		}
		try {
			_type = v.getString("type");
		} catch (final JSONException e) {
			throw new TicketModelException("Geen type in velddefinitie");
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
				throw new TicketModelException("Geen opties in velddefinitie");
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

}
