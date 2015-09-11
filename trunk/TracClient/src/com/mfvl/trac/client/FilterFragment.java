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


public class FilterFragment extends TracClientFragment {
    private TicketModel tm;

    private final static String inputSpecText = "inputSpec";
    private final static String outputSpecText = "outputSpec";

    private ArrayList<FilterSpec> inputSpec;
    private FilterAdapter filterAdapter;

    private class FilterAdapter extends ArrayAdapter<FilterSpec> {

        private final ArrayList<FilterSpec> items;

        public FilterAdapter(Context context, int textViewResourceId, ArrayList<FilterSpec> input) {
            super(context, textViewResourceId, input);
            items = input;
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
//            tcLog.d(getClass().getName(), "getView pos=" + position + " " + convertView + " " + parent);

            final Resources res = context.getResources();
            final ArrayList<String> operators = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter2_choice)));
            final ArrayList<String> operatornames = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter_names)));
            final ListView listView = (ListView) parent;

            View v = convertView;
            int p;

            p = position >= items.size() || position < 0 ? 0 : position;

            final FilterSpec o = items.get(p);
            final TicketModelVeld tmv = tm.getVeld(o.getVeld());

//            tcLog.d(getClass().getName(), "getView pos=" + position +" " + o + " " + tmv);

            final int resid = o.getEdit()
                    ? tmv.options() == null ? R.layout.filter_spec2 : R.layout.filter_spec3
                    : R.layout.filter_spec1;
            final int curid = convertView == null ? -1 : convertView.getId();

//            tcLog.d(getClass().getName(),"getView pos = " + position + " curid = " + curid + " resid=" + resid + " veld = " + o.getVeld());
            if (curid != resid) {
                final LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                v = vi.inflate(resid, null);
                v.setId(resid); // hack hack
                listView.requestLayout();
            }

            listView.invalidate();
            final TextView tt = (TextView) v.findViewById(R.id.filternaam);
            final ImageButton filterEdit = (ImageButton) v.findViewById(R.id.editfilter);
            final ImageButton filterSave = (ImageButton) v.findViewById(R.id.savefilter);
            final ImageButton filterDel = (ImageButton) v.findViewById(R.id.delfilter);
            final Spinner spin = (Spinner) v.findViewById(R.id.filter_choice_spin);
            final EditText et = (EditText) v.findViewById(R.id.filtervaltext);
            final LinearLayout filterCheck = (LinearLayout) v.findViewById(R.id.filtercheck);

            final View.OnClickListener startEdit = new View.OnClickListener() {
                @Override
                public void onClick(View v1) {
                    o.setEdit(true);
                    listView.invalidateViews();
                }
            };

            final View.OnClickListener stopEdit = new View.OnClickListener() {
                @Override
                public void onClick(View v1) {
                    o.setEdit(false);
                    if (spin != null) {
                        o.setOperator(operators.get(spin.getSelectedItemPosition()));
                    }
                    listView.invalidateViews();
                }
            };

            if (o != null) {
                if (o.getEdit()) {
                    tt.setText(o.getVeld());
                    tt.setOnClickListener(stopEdit);
                } else {
                    tt.setText(o.toString());
                    tt.setOnClickListener(startEdit);
                }
            }

            if (spin != null) {
                final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                        operatornames);

                spin.setAdapter(spinAdapter);
                spin.setSelection(operators.indexOf(o.getOperator()));
                spin.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        o.setOperator(operators.get(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            if (et != null) {
                et.setText(o.getWaarde());
                et.requestFocus();
                et.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void afterTextChanged(Editable arg0) {}

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        o.setWaarde(s.toString());
                    }

                });
            }

            if (filterCheck != null && filterCheck.getChildCount() == 0) {
                filterCheck.addView(makeCheckBoxes(o));
            }

            if (filterEdit != null) {
                filterEdit.setOnClickListener(startEdit);
            }

            if (filterSave != null) {
                filterSave.setOnClickListener(stopEdit);
            }

            if (filterDel != null) {
                filterDel.setOnClickListener(new ImageButton.OnClickListener() {
                    @Override
                    public void onClick(View v1) {
                        items.remove(o);
                        FilterAdapter.this.notifyDataSetChanged();
                    }
                });
            }

            return v;
        }
    }

	@SuppressWarnings("unchecked")
	private void onMyAttach(Context activity) {
		inputSpec = null;
			
        final Bundle args = getArguments();		
        if (args != null) {
            if (args.containsKey(Const.FILTERLISTNAME)) {
				inputSpec = (ArrayList<FilterSpec>)args.getSerializable(Const.FILTERLISTNAME);
            }
        }
        tcLog.d(getClass().getName(), "onMyAttach inputSpec = "+inputSpec);
	}
	
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        tcLog.d(getClass().getName(), "onAttach(C)");
		onMyAttach(activity);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        tcLog.d(getClass().getName(), "onAttach(A)");
		onMyAttach(activity);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d(getClass().getName(), "onCreate savedInstanceState = " +savedInstanceState);
        setHasOptionsMenu(true);
        tm = listener.getTicketModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(this.getClass().getName(),"onCreateView savedInstanceState = " + savedInstanceState);
        final View view = inflater.inflate(R.layout.filter_view, container, false);

        return view;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d(getClass().getName(),"onActivityCreated savedInstanceState = " + savedInstanceState);
		tcLog.d(getClass().getName(), "onActivityCreated on entry inputSpec = " + inputSpec);

        final View view = getView();
        final ListView lv = (ListView) view.findViewById(R.id.filterlist);
        ArrayList<FilterSpec> outputSpec = null;

		helpFile = R.string.filterhelpfile;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(inputSpecText)) {
                inputSpec = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(inputSpecText);
            }
            if (savedInstanceState.containsKey(outputSpecText)) {
                outputSpec = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(outputSpecText);
            }
        }

        if (outputSpec == null) {
            outputSpec = new ArrayList<FilterSpec>();
            if (inputSpec != null) {
                for (final FilterSpec o : inputSpec) {
                    o.setEdit(false);
                    try {
                        outputSpec.add((FilterSpec) o.clone());
                    } catch (final Exception e) {
                        outputSpec.add(o);
                    }
                }
            }
        }

        tcLog.d(getClass().getName(), "onActivityCreated on exit inputSpec = " + inputSpec);
        tcLog.d(getClass().getName(), "onActivityCreated on exit outputSpec = " + outputSpec);

        filterAdapter = new FilterAdapter(context, android.R.layout.simple_list_item_1, outputSpec);
        lv.setAdapter(filterAdapter);

        final Button storButton = (Button) view.findViewById(R.id.storebutton);
        final ImageButton addButton = (ImageButton) view.findViewById(R.id.addbutton);
        final Spinner addSpinner = (Spinner) view.findViewById(R.id.addspin);
		getScreensize(addSpinner,addButton);

        storButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                final ArrayList<FilterSpec> items = filterAdapter.items;

                for (int i = items.size() - 1; i >= 0; i--) {
                    if (items.get(i).getOperator() == null || items.get(i).getOperator().equals("")
                            || items.get(i).getWaarde() == null || items.get(i).getWaarde().equals("")) {
                        items.remove(i);
                    } else {
                        items.get(i).setEdit(false);
                    }
                }
                sendMessageToHandler(TracStart.MSG_SET_FILTER,items);
                getFragmentManager().popBackStack();
            }
        });

        if (addButton != null) {
            final ArrayList<String> velden = tm.velden();

            Collections.sort(velden);
            final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, velden);

            addSpinner.setAdapter(spinAdapter);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v1) {
                    final String veld = velden.get((int) addSpinner.getSelectedItemId());
                    final FilterSpec o = new FilterSpec(veld, "=", "");

                    filterAdapter.add(o);
                    filterAdapter.notifyDataSetChanged();
                }
            });
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
                    o.setOperator("=");
                    o.setWaarde(temp);
                }
            });
            valCheckBoxes.addView(rb);
        }
        return valCheckBoxes;
    }
}
