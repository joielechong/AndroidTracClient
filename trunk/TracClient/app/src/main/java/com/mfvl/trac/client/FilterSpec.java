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


public class FilterSpec extends Spec implements Serializable, Cloneable {

    private static final long serialVersionUID = 552288154328397222L;
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

    @Override
    public boolean equals(Object o) {
        boolean retVal;
        if (this == o) {
            retVal = true;
        } else if (!(o instanceof FilterSpec)) {
            retVal = false;
        } else {
            retVal = super.equals(o);
            FilterSpec f = (FilterSpec) o;
            retVal &= equalFields(_operator, f.getOperator());
            retVal &= equalFields(_waarde, f.getWaarde());
        }
//		tcLog.d("this = "+this+" o = "+o+" retVal = "+retVal);
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
        // tcLog.i( "setEdit veld = " + _veld + " edited = " + edited);
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
