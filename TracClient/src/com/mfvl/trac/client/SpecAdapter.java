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


import java.util.ArrayList;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SpecAdapter<T extends Spec> extends ArrayAdapter<T> {
	protected final ArrayList<T> items;
	protected ListView listView;

	public SpecAdapter(Context context, int textViewResourceId, ArrayList<T> items) {
		super(context, textViewResourceId, items);
		this.items = items;
	}

	public ArrayList<T> getItems() {
		return items;
	}
}