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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class FilterFragment extends SpecFragment<FilterSpec> implements View.OnClickListener {
    private FilterAdapter filterAdapter;
	private Spinner addSpinner;
	
	
    private class FilterAdapter extends SpecAdapter<FilterSpec> implements View.OnClickListener {
	private ArrayList<String> operators;
	private ArrayList<String> operatornames;

        public FilterAdapter(Context context, int textViewResourceId, ArrayList<FilterSpec> input) {
            super(context, textViewResourceId, input);
            final Resources res = context.getResources();
            operators = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter2_choice)));
            operatornames = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter_names)));
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
			//tcLog.d(getClass().getName(), "getView pos=" + position + " " + convertView + " " + parent);

            listView = (ListView) parent;
			
            int p = (position >= items.size() || position < 0 ? 0 : position);
            final FilterSpec filterItem = items.get(p);
            final TicketModelVeld tmv = tm.getVeld(filterItem.getVeld());
			//tcLog.d(getClass().getName(), "getView pos=" + position +" " + filterItem + " " + tmv);
            final int resid = (filterItem.getEdit() ? (tmv.options() == null ? R.layout.filter_spec2 : R.layout.filter_spec3) : R.layout.filter_spec1);
			
            View v = convertView;

            final int curid = convertView == null ? -1 : convertView.getId();

            //tcLog.d(getClass().getName(),"getView pos = " + position + " curid = " + curid + " resid=" + resid + " veld = " + filterItem.getVeld());
            if (curid != resid) {
                v = LayoutInflater.from(context).inflate(resid, null);
                v.setId(resid); // hack hack
                listView.requestLayout();
            }
			v.setTag(filterItem);

            final TextView filterNaam = (TextView) v.findViewById(R.id.filternaam);
			filterNaam.setOnClickListener(this);
			v.findViewById(R.id.startedit).setOnClickListener(this);
			v.findViewById(R.id.stopedit).setOnClickListener(this);
			v.findViewById(R.id.delitem).setOnClickListener(this);
            final Spinner spin = (Spinner) v.findViewById(R.id.filter_choice_spin);
            final EditText et = (EditText) v.findViewById(R.id.filtervaltext);
            final LinearLayout filterCheck = (LinearLayout) v.findViewById(R.id.filtercheck);
			
			filterNaam.setText((filterItem.getEdit()?filterItem.getVeld():filterItem.toString()));

            if (spin != null) {
                final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                        operatornames);

                spin.setAdapter(spinAdapter);
                spin.setSelection(operators.indexOf(filterItem.getOperator()));
                spin.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        filterItem.setOperator(operators.get(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            if (et != null) {
                et.setText(filterItem.getWaarde());
                et.requestFocus();
                et.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void afterTextChanged(Editable arg0) {}

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterItem.setWaarde(s.toString());
                    }

                });
            }

            if (filterCheck != null && filterCheck.getChildCount() == 0) {
                filterCheck.addView(makeCheckBoxes(filterItem));
            }

            listView.invalidate();

            return v;
        }

		public void onClick(View v) {
			FilterSpec filterItem = getItem(v);
			switch (v.getId()) {
				case R.id.filternaam:
	//			tcLog.d(getClass().getName(), "toggleEdit filterItem =" + filterItem);
				if (filterItem.getEdit()) {
					View v1=(View)v.getParent();
					final Spinner spin = (Spinner) v1.findViewById(R.id.filter_choice_spin);
					stopEditItem(filterItem,spin);
				} else {
					startEditItem(filterItem);
				}
				break;
				
				case R.id.startedit:
				startEditItem(filterItem);
				break;
				
				case R.id.stopedit:
				View v1=(View)v.getParent();
				final Spinner spin = (Spinner) v1.findViewById(R.id.filter_choice_spin);
				stopEditItem(filterItem,spin);
				break;
				
				case R.id.delitem:
				items.remove(filterItem);
				notifyDataSetChanged();
				break;
			}
		}
	
		private FilterSpec getItem(View v1) {
//			tcLog.d(getClass().getName(), "getItem v1 =" + v1);
			View parent = (View)v1.getParent();
//			tcLog.d(getClass().getName(), "getItem parent =" + parent);
			if (parent.getTag() == null) {
				parent = (View)parent.getParent();
			}
//			tcLog.d(getClass().getName(), "getItem parent2 =" + parent);
			FilterSpec o = (FilterSpec)parent.getTag();
			tcLog.d(getClass().getName(), "getItem filterItem = " + o);
			return o;
		}
		
		private void startEditItem(FilterSpec filterItem) {
//			tcLog.d(getClass().getName(), "startEditItem filterItem =" + filterItem );
			filterItem.setEdit(true);
			listView.invalidateViews();
		}
		
		private void stopEditItem(FilterSpec filterItem,Spinner spin){
//			tcLog.d(getClass().getName(), "stopEditItem filterItem =" + filterItem + " spin = " + spin);
			filterItem.setEdit(false);
			if (spin != null) {
				filterItem.setOperator(operators.get(spin.getSelectedItemPosition()));
			}
			listView.invalidateViews();
		}
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
//        tcLog.d(getClass().getName(), "onAttach(C)");
		onMyAttach(activity,Const.FILTERLISTNAME);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//        tcLog.d(getClass().getName(), "onAttach(A)");
		onMyAttach(activity,Const.FILTERLISTNAME);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(this.getClass().getName(),"onCreateView savedInstanceState = " + savedInstanceState);
        final View view = inflater.inflate(R.layout.filter_view, container, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState); // must be called first
        tcLog.d(getClass().getName(),"onActivityCreated savedInstanceState = " + savedInstanceState);
        final View view = getView();
		helpFile = R.string.filterhelpfile;

        filterAdapter = new FilterAdapter(context, android.R.layout.simple_list_item_1, outputSpec);
        listView.setAdapter(filterAdapter);

		view.findViewById(R.id.storefilter).setOnClickListener(this);
        final ImageButton addButton = (ImageButton) view.findViewById(R.id.addbutton);
		addButton.setOnClickListener(this);
        addSpinner = (Spinner) view.findViewById(R.id.addspin);
		getScreensize(addSpinner,addButton);
		final ArrayList<String> velden = tm.velden();
		Collections.sort(velden);
		final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, velden);
		addSpinner.setAdapter(spinAdapter);
    }
	
	public void onClick(View v1) {
		switch (v1.getId()) {
			case R.id.storefilter:
			final ArrayList<FilterSpec> items = filterAdapter.items;

			for (int i = items.size() - 1; i >= 0; i--) {
				if (items.get(i).getOperator() == null || items.get(i).getOperator().equals("")
						|| items.get(i).getWaarde() == null || items.get(i).getWaarde().equals("")) {
					items.remove(i);
				} else {
					items.get(i).setEdit(false);
				}
			sendMessageToHandler(TracStart.MSG_SET_FILTER,items);
			getFragmentManager().popBackStack();
			}
			break;
			
			case R.id.addbutton:
			final String veld = tm.velden().get((int) addSpinner.getSelectedItemId());
			final FilterSpec o = new FilterSpec(veld, "=", "");

			filterAdapter.add(o);
			filterAdapter.notifyDataSetChanged();
			break;
		}
	}
	

    private LinearLayout makeCheckBoxes(final FilterSpec o) {
        final String veldnaam = o.getVeld();
        final List<Object> waardes = tm.getVeld(veldnaam).options();
        final String w = o.getWaarde();
        final String op = o.getOperator();
        final boolean omgekeerd = op != null && op.equals("!=");

        tcLog.d(this.getClass().getName(), "makeCheckBoxes " + veldnaam + " " + w + " " + omgekeerd);
        final LinearLayout valCheckBoxes = new LinearLayout(context);

        valCheckBoxes.setOrientation(LinearLayout.VERTICAL);
        String[] ws;

        try {
            ws = w == null ? null : w.split("\\|");
        } catch (final IllegalArgumentException e) {
            ws = new String[1];
            ws[0] = w;
        }

        for (int i = 0; i < waardes.size(); i++) {
            final CheckBox rb = new CheckBox(context);

            rb.setText((String) waardes.get(i));
            rb.setId(i);
            rb.setChecked(omgekeerd);
            if (w != null) {
                for (final String w1 : ws) {
                    if (w1.equals(waardes.get(i))) {
                        rb.setChecked(!omgekeerd);
                    }
                }
            }
            rb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb0, boolean isChecked) {
                    String temp = null;

                    for (int j = 0; j < waardes.size(); j++) {
                        final CheckBox cb = (CheckBox) valCheckBoxes.findViewById(j);

                        if (cb != null && cb.isChecked()) {
                            if (temp == null) {
                                temp = cb.getText().toString();
                            } else {
                                temp += "|" + cb.getText();
                            }
                        }
                    }
                    o.setOperator("=").setWaarde(temp);
                }
            });
            valCheckBoxes.addView(rb);
        }
        return valCheckBoxes;
    }
}
