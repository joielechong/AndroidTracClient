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

import android.content.Context;
import android.widget.ArrayAdapter;

import java.io.Serializable;
import java.util.ArrayList;

interface ItemsAdapter {
    Serializable getItems();
}

class SpecAdapter<T extends Spec> extends ArrayAdapter<T> implements ItemsAdapter {
    final ArrayList<T> items;

    SpecAdapter(Context context, int textViewResourceId, ArrayList<T> _items) {
        super(context, textViewResourceId, _items);
//        MyLog.logCall();
        this.items = _items;
    }

    @Override
    public Serializable getItems() {
//    MyLog.logCall();
        return items;
    }
}
