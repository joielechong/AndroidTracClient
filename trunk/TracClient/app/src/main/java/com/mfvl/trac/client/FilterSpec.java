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


public class FilterSpec extends Spec {
    private String _operator;
    private String _waarde;
    private String _newwaarde;
    private Boolean _edited = false;

    public FilterSpec(final String veld, final String operator, final String waarde) {
        super(veld);
        _operator = operator;
        _waarde = waarde;
        _newwaarde = waarde;
    }

    public FilterSpec(final String string, final String[] operators) {
        super(null);
        _operator = null;
        _waarde = null;
        for (int i = operators.length - 1; i >= 0 && _waarde == null; i--) {
            final String op = operators[i];
            final int index = string.indexOf(op);

            if (index > 0) {
                _veld = string.substring(0, index);
                _operator = op;
                _waarde = string.substring(index + op.length());
                _newwaarde = _waarde;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        boolean retVal;
        retVal = (this == o)
                || (o instanceof FilterSpec
                && super.equals(o)
                && equalFields(_operator, ((FilterSpec) o).getOperator())
                && equalFields(_waarde, ((FilterSpec) o).getWaarde()));
//		MyLog.d("this = "+this+" o = "+o+" retVal = "+retVal);
        return retVal;
    }

    public String getOperator() {
        return _operator;
    }

    public FilterSpec setOperator(final String o) {
        _operator = o;
        return this;
    }

    public String getWaarde() {
        return _edited ? _newwaarde : _waarde;
    }

    public FilterSpec setWaarde(final String w) {
        if (_edited) {
            _newwaarde = w;
        } else {
            _waarde = w;
        }
        return this;
    }

    public boolean getEdit() {
        return _edited;
    }

    @Override
    public FilterSpec setEdit(final boolean edited) {
        // MyLog.i( "setEdit veld = " + _veld + " edited = " + edited);
        if (edited != _edited) {
            _edited = edited;
            if (_edited) {
                _newwaarde = _waarde;
            } else {
                _waarde = _newwaarde;
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return _edited ? _veld : _veld + (_operator != null ? _operator : "") + (_waarde != null ? _waarde : "");
    }
}
