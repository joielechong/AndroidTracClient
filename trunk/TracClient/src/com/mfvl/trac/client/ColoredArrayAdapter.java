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

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.content.Context;

public class ColoredArrayAdapter<T> extends ArrayAdapter<T> {
    private static int[] colors = null;
    private final List<T> items;
    Context context = null;

    public ColoredArrayAdapter(TracStart context, int resource, List<T> list) {
        super(context, resource, list);
        items = list;
        this.context = context;
        if (colors == null) {
            colors = context.getResources().getIntArray(R.array.list_col);
        }
    }

    @Override
    public void clear() {
        items.clear();
        super.clear();
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final int colorPos = position % colors.length;
		view.setBackgroundColor(colors[colorPos]);
		return view;
	}
}
