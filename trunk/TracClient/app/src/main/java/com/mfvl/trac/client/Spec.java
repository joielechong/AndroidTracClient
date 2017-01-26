/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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

class Spec extends TcObject implements Serializable, Cloneable {
    final String _veld;

    Spec(String veld) {
        _veld = veld;
    }

    @Override
    public Spec clone() throws CloneNotSupportedException {
        return (Spec) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Spec) && equalFields(_veld, ((Spec) o).getVeld());
    }

    String getVeld() {
        return _veld;
    }

    void setEdit(final boolean edited) { //no-op
    }

    @Override
    public int hashCode() {
        return (_veld != null ? _veld.hashCode() : 0) + super.hashCode();
    }
}
