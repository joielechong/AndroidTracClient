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

class SortSpec extends Spec {
    private boolean _richting; // true = asc false = desc

    SortSpec(String veld) {
        this(veld, true);
    }

    SortSpec(String veld, boolean richting) {
        super(veld);
        _richting = richting;
    }

    @Override
    public SortSpec clone() throws CloneNotSupportedException {
        return (SortSpec) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        return (this == o)
                || (o instanceof SortSpec && super.equals(o)
                && (_richting == ((SortSpec) o).getRichting()));
    }

    boolean getRichting() {
        return _richting;
    }

    private void setRichting(boolean r) {
        _richting = r;
    }

    boolean flip() {
        _richting = !_richting;
        return _richting;
    }

    @Override
    public String toString() {
        return "order=" + _veld + (_richting ? "" : "&desc=1");
    }

    @Override
    public int hashCode() {
        return (_richting ? 1 : 0) + super.hashCode();
    }
}
