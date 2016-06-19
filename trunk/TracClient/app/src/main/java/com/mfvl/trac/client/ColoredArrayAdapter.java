/*
 * Copyright (C) 2013-2016 Michiel van Loon
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

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import static com.mfvl.trac.client.TracGlobal.*;


class ColoredArrayAdapter<T> extends ArrayAdapter<T> {

    public ColoredArrayAdapter(Activity context, List<T> list) {
        super(context, R.layout.ticket_list, list);
        //MyLog.d("context = "+context+" resource = "+ resource+" list = " +list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //MyLog.d("position = " + position + " convertView = " + convertView + " parent = " + parent);
        final View view = super.getView(position, convertView, parent);
        view.setBackgroundColor(adapterColors[position % adapterColors.length]);
        return view;
    }
}
