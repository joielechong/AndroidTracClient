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


import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class SortFragment extends SpecFragment<SortSpec> implements View.OnClickListener {

    private class SortAdapter extends SpecAdapter<SortSpec>  implements View.OnClickListener {
        public SortAdapter(Context context, int textViewResourceId, ArrayList<SortSpec> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
			// tcLog.d(getClass().getName(),"getView: "+position+" "+convertView+" "+parent);
            listView = (ListView) parent;
			
            View v = convertView;

            if (v == null) {
				v = LayoutInflater.from(context).inflate(R.layout.sort_spec, parent, false);
            }
			v.setTag(position);
            ImageButton sortup = (ImageButton) v.findViewById(R.id.sortup);
			sortup.setOnClickListener(this);
            ImageButton sortdown = (ImageButton) v.findViewById(R.id.sortdown);
			sortdown.setOnClickListener(this);
            TextView tt = (TextView) v.findViewById(R.id.sortfield);
            ImageButton direc = (ImageButton) v.findViewById(R.id.sortdirec);
			direc.setOnClickListener(this);
			v.findViewById(R.id.delitem).setOnClickListener(this);
			
            SortSpec sortItem = items.get(position);

            if (sortItem != null) {
                if (tt != null) {
                    tt.setText(sortItem.getVeld());
                }
                if (direc != null) {
                    direc.setImageResource(sortItem.getRichting() ? R.drawable.upArrow : R.drawable.downArrow);
                }

                if (sortup != null) {
                    sortup.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                }

                if (sortdown != null) {
                    sortdown.setVisibility(position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);
                }
            }
            return v;
        }
		
		private int getSortPosition(View dv) {
			return (Integer)((View)dv.getParent()).getTag();
		}

		public void onClick(View dv) {
			int position = getSortPosition(dv);
			SortSpec sortItem = items.get(position);
			switch (dv.getId()) {
				case R.id.sortup:
				if (position > 0) {
					final SortSpec o1 = sortItem;
					final SortSpec o2 = items.get(position - 1);

					items.set(position - 1, o1);
					items.set(position, o2);
					notifyDataSetChanged();
				}
				break;

				case R.id.sortdown:
				if (position < items.size() - 1) {
					final SortSpec o1 = sortItem;
					final SortSpec o2 = items.get(position + 1);

					items.set(position + 1, o1);
					items.set(position, o2);
					notifyDataSetChanged();
				}
				break;

				case R.id.sortdirec:
				((ImageButton)dv).setImageResource(sortItem.flip() ? R.drawable.upArrow : R.drawable.downArrow);
				break;
				
				case R.id.delitem:
				items.remove(sortItem);
				notifyDataSetChanged();
				break;
			}
		}
    }

    public SortAdapter sortAdapter = null;
	private Spinner addSpinner = null;
	
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
//        tcLog.d(getClass().getName(), "onAttach(C)");
		onMyAttach(activity,Const.SORTLISTNAME);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//        tcLog.d(getClass().getName(), "onAttach(A)");
		onMyAttach(activity,Const.SORTLISTNAME);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = " + savedInstanceState);
        return inflater.inflate(R.layout.sort_view, container, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);  // vult inputSpec en outputSpec
        tcLog.d(getClass().getName(), "onActivityCreated savedInstanceState = " + savedInstanceState );
		helpFile = R.string.sorthelpfile;
        final View view = getView();

        sortAdapter = new SortAdapter(context, R.layout.sort_spec, outputSpec);
        listView.setAdapter(sortAdapter);

		ImageButton addButton = (ImageButton) view.findViewById(R.id.addbutton);
		addButton.setOnClickListener(this);
		view.findViewById(R.id.storebutton).setOnClickListener(this);
        addSpinner = (Spinner) view.findViewById(R.id.addspin);
		getScreensize(addSpinner,addButton);

        if (addButton != null && addSpinner != null) {
			//tcLog.d(getClass().getName(),"before setAdapter, tm = "+tm+ " addSspinner = "+ addSpinner+" context = "+context);
            addSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, tm.velden()));
        }

    }
	
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.storebutton:
			final ArrayList<SortSpec> items = sortAdapter.items;
			for (int i = items.size() - 1; i >= 0; i--) {
				if (items.get(i).getRichting() == null) {
					items.remove(i);
				}
			}
			sendMessageToHandler(TracStart.MSG_SET_SORT,items);
			getFragmentManager().popBackStack();
			break;
			
			case R.id.addbutton:
			final String veld = tm.velden().get((int) addSpinner.getSelectedItemId());
			// tcLog.d(getClass().getName(), "addButton " + veld);
			sortAdapter.add(new SortSpec(veld));
			break;
		}
	}
}
