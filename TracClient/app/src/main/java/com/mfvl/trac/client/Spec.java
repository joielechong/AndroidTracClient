/*
 * Copyright (C) 2015 Michiel van Loon
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

abstract public class Spec extends TcObject implements Serializable, Cloneable {
    String _veld;

    Spec(String veld) {
        _veld = veld;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        boolean retVal;
        retVal = (o instanceof Spec) && equalFields(_veld, ((Spec)o).getVeld());
        return retVal;
    }

    public String getVeld() {
        return _veld;
    }

    public Spec setEdit(final boolean edited) { //no-op
        return this;
    }
}
