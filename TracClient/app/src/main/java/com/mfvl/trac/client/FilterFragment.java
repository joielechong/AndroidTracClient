/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mfvl.trac.client.Const.*;

public class FilterFragment extends SpecFragment<FilterSpec> {
    private static List<String> operators = null;
    private static List<String> operatornames = null;
    private FilterAdapter filterAdapter = null;
    private Spinner addSpinner = null;

    @Override
    public String keyName() {
        return FILTERLISTNAME;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.storefilter:
                final List<FilterSpec> items = filterAdapter.items;

                for (int i = items.size() - 1; i >= 0; i--) {
                    if (TextUtils.isEmpty(items.get(i).getOperator()) || TextUtils.isEmpty(items.get(i).getWaarde())) {
                        items.remove(i);
                    } else {
                        items.get(i).setEdit(false);
                    }
                }
                listener.getService().setFilter(items);
                getFragmentManager().popBackStack();
                break;

            case R.id.addbutton:
                final String veld = (String) addSpinner.getSelectedItem();
                final FilterSpec o = new FilterSpec(veld, "=", "");

                filterAdapter.add(o);
                filterAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        MyLog.d("savedInstanceState = " + savedInstanceState);

        return inflater.inflate(R.layout.filter_view, container, false);
    }

    @Override
    public int getHelpFile() {
        return R.string.filterhelpfile;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected(); // must be called first
        MyLog.logCall();

        TicketModel tm = listener.getService().getTicketModel();
        if (tm == null) {
            MyLog.toast(getString(R.string.notpossible));
            getFragmentManager().popBackStack();
        } else {
            filterAdapter = new FilterAdapter(getActivity(), outputSpec);
            listView.setAdapter(filterAdapter);

            currentView.findViewById(R.id.storefilter).setOnClickListener(this);
            final ImageButton addButton = (ImageButton) currentView.findViewById(R.id.addbutton);
            addButton.setOnClickListener(this);
            addSpinner = (Spinner) currentView.findViewById(R.id.addspin);
            getScreensize(addSpinner, addButton);
            final ArrayList<String> velden = tm.velden();
            Collections.sort(velden);
            final SpinnerAdapter spinAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, velden);
            addSpinner.setAdapter(spinAdapter);
        }
    }

    private class FilterAdapter extends SpecAdapter<FilterSpec> implements View.OnClickListener, OnCheckedChangeListener {
        final Object lock = (Integer) 1;

        FilterAdapter(Context ctx, ArrayList<FilterSpec> input) {
            super(ctx, android.R.layout.simple_list_item_1, input);
            synchronized (lock) {
                if (operators == null) {
                    final Resources res = ctx.getResources();
                    operators = Arrays.asList(res.getStringArray(R.array.filter2_choice));
                    operatornames = Arrays.asList(res.getStringArray(R.array.filter_names));
                }
            }
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull final ViewGroup parent) {
            //MyLog.d("getView pos=" + position + " " + convertView + " " + parent);

            int p = (position >= items.size() || position < 0 ? 0 : position);
            final FilterSpec filterItem = items.get(p);
            final TicketModelVeld tmv = listener.getService().getTicketModel().getVeld(filterItem.getVeld());
            //MyLog.d( "getView pos=" + position +" " + filterItem + " " + tmv);
            final int resid = (filterItem.getEdit() ? (tmv.options() == null ? R.layout.filter_spec2 : R.layout.filter_spec3) : R.layout.filter_spec1);

            View v = convertView;

            final int curid = convertView == null ? -1 : convertView.getId();

            //MyLog.d("getView pos = " + position + " curid = " + curid + " resid=" + resid + " veld = " + filterItem.getVeld());
            if (curid != resid) {
                v = LayoutInflater.from(getActivity()).inflate(resid, null);
                //noinspection ResourceType
                v.setId(resid); // hack hack
                parent.requestLayout();
            }
            v.setTag(filterItem);

            final TextView filterNaam = (TextView) v.findViewById(R.id.filternaam);
            setOnClickListener(R.id.filternaam, v, this);
            setOnClickListener(R.id.startedit, v, this);
            setOnClickListener(R.id.stopedit, v, this);
            setOnClickListener(R.id.delitem, v, this);
            final Spinner spin = (Spinner) v.findViewById(R.id.filter_choice_spin);
            final EditText et = (EditText) v.findViewById(R.id.filtervaltext);
            final LinearLayout filterCheck = (LinearLayout) v.findViewById(R.id.filtercheck);

            filterNaam.setText((filterItem.getEdit() ? filterItem.getVeld() : filterItem.toString()));

            if (spin != null) {
                final SpinnerAdapter spinAdapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_item,
                        operatornames);

                spin.setAdapter(spinAdapter);
                spin.setSelection(operators.indexOf(filterItem.getOperator()));
                spin.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View view, int pos, long id) {
                        filterItem.setOperator(operators.get(pos));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });
            }

            if (et != null) {
                et.setText(filterItem.getWaarde());
                et.requestFocus();
                et.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterItem.setWaarde(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable arg0) {
                    }

                });
            }

            if (filterCheck != null && filterCheck.getChildCount() == 0) {
                filterCheck.addView(makeCheckBoxes(filterItem));
            }

            parent.invalidate();

            return v;
        }

        private LinearLayout makeCheckBoxes(final FilterSpec o) {
            final String veldnaam = o.getVeld();
            List<Object> waardes = listener.getService().getTicketModel().getVeld(veldnaam).options();
            final String waarde = o.getWaarde();
            final String op = o.getOperator();
            final boolean omgekeerd = "!=".equals(op);

//        MyLog.d(veldnaam + " " + w + " " + omgekeerd);
            LinearLayout valCheckBoxes = new LinearLayout(getActivity());

            valCheckBoxes.setOrientation(LinearLayout.VERTICAL);
            String[] ws;

            try {
                ws = waarde == null ? null : waarde.split("\\|");
            } catch (final IllegalArgumentException e) {
                ws = new String[1];
                ws[0] = waarde;
            }

            for (int i = 0; i < waardes.size(); i++) {
                final CheckBox rb = new CheckBox(getActivity());

                rb.setText((CharSequence) waardes.get(i));
                rb.setId(i);
                rb.setChecked(omgekeerd);
                if (ws != null) {
                    for (final String w1 : ws) {
                        if (w1.equals(waardes.get(i))) {
                            rb.setChecked(!omgekeerd);
                        }
                    }
                }
                rb.setTag(o);
                rb.setOnCheckedChangeListener(this);
                valCheckBoxes.addView(rb);
            }
            return valCheckBoxes;
        }

        @Override
        public void onClick(View v) {
//            MyLog.d("v =" + v);
            FilterSpec filterItem = getItem(v);
            switch (v.getId()) {
                case R.id.filternaam:
                    //			MyLog.d( "toggleEdit filterItem =" + filterItem);
                    if (filterItem.getEdit()) {
                        View v1 = (View) v.getParent();
                        final Spinner spin = (Spinner) v1.findViewById(R.id.filter_choice_spin);
                        stopEditItem(filterItem, spin);
                    } else {
                        startEditItem(filterItem);
                    }
                    break;

                case R.id.startedit:
                    startEditItem(filterItem);
                    break;

                case R.id.stopedit:
                    View v1 = (View) v.getParent();
                    final Spinner spin = (Spinner) v1.findViewById(R.id.filter_choice_spin);
                    stopEditItem(filterItem, spin);
                    break;

                case R.id.delitem:
                    remove(filterItem);
                    notifyDataSetChanged();
                    break;
            }
        }

        private FilterSpec getItem(View v1) {
//			MyLog.d("getItem v1 =" + v1);
            View parent = (View) v1.getParent();
//			MyLog.d("getItem parent =" + parent);
            if (parent.getTag() == null) {
                parent = (View) parent.getParent();
            }
//			MyLog.d("getItem parent2 =" + parent);
            FilterSpec o = (FilterSpec) parent.getTag();
            MyLog.d("getItem filterItem = " + o);
            return o;
        }

        private void startEditItem(Spec filterItem) {
//			MyLog.d("startEditItem filterItem =" + filterItem);
            filterItem.setEdit(true);
            notifyDataSetChanged();
        }

        private void stopEditItem(FilterSpec filterItem, Spinner spin) {
//			MyLog.d("stopEditItem filterItem =" + filterItem + " spin = " + spin);
            filterItem.setEdit(false);
            if (spin != null) {
                filterItem.setOperator(operators.get(spin.getSelectedItemPosition()));
            }
            notifyDataSetChanged();
        }

        @Override
        public void onCheckedChanged(CompoundButton cb0, boolean isChecked) {
//		MyLog.d("cb0 = "+cb0+" parent = "+ cb0.getParent());
            String temp = null;
            ViewGroup parent = (ViewGroup) cb0.getParent();

            for (int j = 0; j < parent.getChildCount(); j++) {
                final CheckBox cb = (CheckBox) parent.getChildAt(j);
//			MyLog.d("cb = "+cb+" j = "+ j);

                if (cb != null && cb.isChecked()) {
                    if (temp == null) {
                        temp = cb.getText().toString();
                    } else {
                        temp += "|" + cb.getText();
                    }
                }
            }
            FilterSpec o = (FilterSpec) cb0.getTag();
            o.setOperator("=").setWaarde(temp);
        }
    }
}
