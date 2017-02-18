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

import static com.mfvl.trac.client.TracGlobal.*;

import java.util.Locale;

class FilterSpec extends Spec {
    private String _operator;
    private String _waarde;
    private String _newwaarde = null;
    private Boolean _edited = false;

    FilterSpec(final String veld, final String operator, final String waarde) {
        super(veld);
        _operator = operator;
        _waarde = waarde;
        _newwaarde = waarde;
    }

    @Override
    public FilterSpec clone() throws CloneNotSupportedException {
        return (FilterSpec) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        return (this == o)
                || (o instanceof FilterSpec
                && super.equals(o)
                && equalFields(_operator, ((FilterSpec) o).getOperator()));
    }

    String getOperator() {
        return _operator;
    }

    FilterSpec setOperator(final String o) {
        _operator = o;
        return this;
    }

    String getWaarde() {
        return _edited ? _newwaarde : _waarde;
    }

    FilterSpec setWaarde(final String w) {
        if (_edited) {
            _newwaarde = w;
        } else {
            _waarde = w;
        }
        return this;
    }

    boolean getEdit() {
        return _edited;
    }

    @Override
    public void setEdit(final boolean edited) {
        // MyLog.i( "setEdit veld = " + _veld + " edited = " + edited);
        if (edited != _edited) {
            _edited = edited;
            if (_edited) {
                _newwaarde = _waarde;
            } else {
                _waarde = _newwaarde;
            }
        }
    }

    public String toString() {
        return _edited ? _veld : String.format(Locale.US, "%s%s%s", _veld, denull(_operator), denull(_waarde));
        //return _edited ? _veld : _veld + (_operator != null ? _operator : "") + (_waarde != null ? _waarde : "");
    }

    @Override
    public int hashCode() {
        return _operator.hashCode() + _waarde.hashCode() + super.hashCode();
    }
}
