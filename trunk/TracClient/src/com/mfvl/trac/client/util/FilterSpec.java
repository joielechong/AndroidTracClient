package com.mfvl.trac.client.util;

import java.io.Serializable;

import android.content.Context;
import android.content.res.Resources;
import com.mfvl.trac.client.util.tcLog;

import com.mfvl.trac.client.R;

public class FilterSpec extends Object implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 552288154328397222L;
	private String _veld;
	private String _operator;
	private String _waarde;
	private String _newwaarde;
	private boolean _edited = false;

	public FilterSpec(String veld, String operator, String waarde) {
		_veld = veld;
		_operator = operator;
		_waarde = waarde;
		_newwaarde = waarde;
	}

	public FilterSpec(String string, Context context) {
		final Resources res = context.getResources();
		final String[] operators = res.getStringArray(R.array.filter2_choice);
		_veld = null;
		_operator = null;
		_waarde = null;
		for (int i = operators.length - 1; i >= 0; i--) {
			final String op = operators[i];
			final int index = string.indexOf(op);
			if (index > 0) {
				_veld = string.substring(0, index);
				_operator = op;
				_waarde = string.substring(index + op.length());
				_newwaarde = _waarde;
				i = 0;
			}
		}
		tcLog.d(this.getClass().getName(), "FilterSpec " + _veld + " " + _operator + " " + _waarde);
	}

	public String veld() {
		return _veld;
	}

	public void setOperator(String o) {
		_operator = o;
	}

	public String operator() {
		return _operator;
	}

	public void setWaarde(String w) {
		tcLog.i(this.getClass().getName(), "setWaarde veld = " + _veld + " edited = " + _edited + " w = " + w);
		if (_edited) {
			_newwaarde = w;
		} else {
			_waarde = w;
		}
	}

	public String waarde() {
		return _edited ? _newwaarde : _waarde;
	}

	public void setEdit(boolean edited) {
		tcLog.i(this.getClass().getName(), "setEdit veld = " + _veld + " edited = " + edited);
		if (edited != _edited) {
			_edited = edited;
			if (_edited) {
				_newwaarde = _waarde;
			} else {
				_waarde = _newwaarde;
			}
		}
	}

	public boolean isEdit() {
		return _edited;
	}

	@Override
	public String toString() {
		return _edited ? _veld : _veld + (_operator != null ? _operator : "") + (_waarde != null ? _waarde : "");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
