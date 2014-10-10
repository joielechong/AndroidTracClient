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

package com.mfvl.trac.client.util;

import java.io.Serializable;

public class FilterSpec extends Object implements Serializable, Cloneable {

	private static final long serialVersionUID = 552288154328397222L;
	private String _veld;
	private String _operator;
	private String _waarde;
	private String _newwaarde;
	private Boolean _edited = false;

	public FilterSpec(String veld, String operator, String waarde) {
		_veld = veld;
		_operator = operator;
		_waarde = waarde;
		_newwaarde = waarde;
	}

	public FilterSpec(String string, String[] operators) {
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
		// tcLog.i(this.getClass().getName(), "setEdit veld = " + _veld + " edited = " + edited);
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

	private int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		// Start with a non-zero constant.
		int result = 23;

		// Include a hash for each field.
		result = 37 * result + hc(_edited);
		result = 37 * result + hc(_veld);
		result = 37 * result + hc(_operator);
		result = 37 * result + hc(_waarde);
		result = 37 * result + hc(_newwaarde);
		return 37 * result + super.hashCode();
	}
}
