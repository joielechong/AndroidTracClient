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


import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.mfvl.trac.client.Const.*;

public class UpdateTicketFragment extends TracClientFragment implements HelpInterface {
    private static final String UPDATE_CURRENT_BUTTON = "currentButton";
    private static final String UPDATE_SPIN_POSITION = "spinPosition";
    private static final String UPDATE_OPTION_VAL = "optionVal";

    private String currentActionName = null;
    private int ticknr = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MyLog.d("onCreate savedInstanceState = " + savedInstanceState );
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // MyLog.d(_tag,"onCreateView savedInstanceState = " + savedInstanceState);
        return inflater.inflate(R.layout.update_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        MyLog.d("savedInstanceState = " + savedInstanceState);
        final int spinPosition;
        final String optionVal;
        final int button;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CURRENT_TICKET)) {
                ticknr = savedInstanceState.getInt(CURRENT_TICKET);
            }
            optionVal = (savedInstanceState.containsKey(
                    UPDATE_OPTION_VAL) ? savedInstanceState.getString(UPDATE_OPTION_VAL) : null);
            spinPosition = (savedInstanceState.containsKey(
                    UPDATE_SPIN_POSITION) ? savedInstanceState.getInt(UPDATE_SPIN_POSITION) : 0);
            button = (savedInstanceState.containsKey(
                    UPDATE_CURRENT_BUTTON) ? savedInstanceState.getInt(UPDATE_CURRENT_BUTTON) : 0);
        } else {
            final Bundle args = getArguments();
            if (args != null) {
                ticknr = args.getInt(CURRENT_TICKET);
            }
            button = 0;
            optionVal = null;
            spinPosition = 0;
        }
        listener.getTicket(ticknr, new OnTicketLoadedListener() {
            @Override
            public void onTicketLoaded(final Ticket t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        _ticket = t;
                        displayView(button, spinPosition, optionVal);
                    }
                });
            }
        });
    }

    private void displayView(final int checkedButton, final int spinPosition, final CharSequence optionVal) {
        final View view = getView();
        final TextView tv = (TextView) view.findViewById(R.id.titel);
        final String text = getString(R.string.updtick) + " " + _ticket;

        tv.setText(text);

        JSONArray _actions = _ticket.getActions();
        MyLog.d("actions = " + _actions);
        final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);

        try {
            for (int action = 0; action < _actions.length(); action++) {
                final JSONArray actieInfo = _actions.getJSONArray(action);
                setListener(R.id.canBut);
                setListener(R.id.storeUpdate);
                final TextView explain = (TextView) view.findViewById(R.id.explaintxt);
                final RadioButton rb = new RadioButton(getActivity());
                final String hintText = actieInfo.getString(2);
                final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);
                final EditText optieval = (EditText) view.findViewById(R.id.optieval);
                final JSONArray inputfields = actieInfo.getJSONArray(3);

                // MyLog.d("inputfields = " + inputfields);
                rb.setId(action);
                if (action == 0) { // 1st action is always leave
                    rb.setChecked(true);
                    explain.setText(hintText);
                    optiesSpin.setVisibility(View.GONE);
                    optiesSpin.setAdapter(null);
                    optieval.setVisibility(View.GONE);
                    optieval.setText(null);
                }
                if (action == checkedButton) {
                    optiesSpin.setSelection(spinPosition);
                    optieval.setText(optionVal);
                }
                rb.setText(actieInfo.getString(0));
                // rb.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.list_textsize));
                rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton v1, boolean isChecked) {
                        if (isChecked) {
                            explain.setText(hintText);
                            currentActionName = null;
                            if (inputfields.length() == 0) {
                                optiesSpin.setAdapter(null);
                                optiesSpin.setVisibility(View.GONE);
                                optieval.setVisibility(View.GONE);
                                optieval.setText(null);
                            } else {
                                try {
                                    currentActionName = inputfields.getJSONArray(0).getString(0);
                                    final String ifValue = inputfields.getJSONArray(0).getString(1);
                                    final JSONArray ifOpties = inputfields.getJSONArray(
                                            0).getJSONArray(2);

                                    if (ifOpties.length() == 0) {
                                        optiesSpin.setAdapter(null);
                                        optiesSpin.setVisibility(View.GONE);
                                        optieval.setVisibility(
                                                ifValue != null ? View.VISIBLE : View.GONE);
                                        optieval.setText(ifValue);
                                    } else {
                                        optieval.setVisibility(View.GONE);
                                        optieval.setText(null);
                                        optiesSpin.setVisibility(View.VISIBLE);
                                        final List<Object> opties = new ArrayList<>();

                                        for (int j = 0; j < ifOpties.length(); j++) {
                                            try {
                                                opties.add(ifOpties.getString(j));
                                            } catch (final JSONException e) {
                                                MyLog.e("Exception adding " + ifOpties + " j=" + j,
                                                        e);
                                            }
                                        }
                                        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(
                                                getActivity(), android.R.layout.simple_spinner_item, opties);

                                        spinAdapter.setDropDownViewResource(
                                                android.R.layout.simple_spinner_dropdown_item);
                                        optiesSpin.setAdapter(spinAdapter);
                                        if (!TextUtils.isEmpty(ifValue) && opties.contains(ifValue)) {
                                            optiesSpin.setSelection(opties.indexOf(ifValue), true);
                                        }
                                    }
                                } catch (final Exception e) {
                                    MyLog.e("Exception getting fields", e);
                                }
                            }
                            view.postInvalidate();
                        }
                    }
                });
                rg.addView(rb);
            }
        } catch (final Exception e) {
            MyLog.e("exception loading ticketdata", e);
        }
//        MyLog.d("currentButton = " + checkedButton);
        rg.check(checkedButton);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.canBut:
                MyLog.d("cancel pressed v = " + v);
                getFragmentManager().popBackStack();
                break;

            case R.id.storeUpdate:
                getFragmentManager().popBackStack();
                storeUpdate();
                listener.refreshTicket(ticknr);
                break;
        }
    }

    /**
     * storeUpdate - called when the Store button is pressed
     */

    private void storeUpdate() {
        String w = null;
        final View view = getView();
        final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);
        final int sel = rg.getCheckedRadioButtonId();
        final RadioButton rb = (RadioButton) rg.findViewById(sel);
        final EditText et = (EditText) view.findViewById(R.id.comment);
        final Checkable updNotify = (Checkable) view.findViewById(R.id.updNotify);

        final String action = (String) rb.getText();
        final String comment = et.getText().toString();
        final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);
        final EditText optieVal = (EditText) view.findViewById(R.id.optieval);

        if (currentActionName != null) {
            if (!TextUtils.isEmpty(optieVal.getText())) {
                w = optieVal.getText().toString();
            }
            if (optiesSpin.getAdapter() != null) {
                w = (String) optiesSpin.getSelectedItem();
            }
        }
        final String waarde = w;
        listener.startProgressBar(R.string.saveupdate);
        try {
            final boolean notify = updNotify != null && updNotify.isChecked();
            listener.updateTicket(_ticket, action, comment, currentActionName, waarde, notify, null);
        } catch (final Exception e) {
            MyLog.e("update failed", e);
            showAlertBox(R.string.storerr, getString(R.string.storerrdesc, e.getMessage()));
        } finally {
            listener.stopProgressBar();
        }
    }

    @Override
    public int getHelpFile() {
        return R.string.updatehelpfile;
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        if (_ticket != null) {
            savedState.putInt(CURRENT_TICKET, _ticket.getTicketnr());
        }
        final View view = getView();

        if (view != null) {
            final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);

            if (rg != null) {
                savedState.putInt(UPDATE_CURRENT_BUTTON, rg.getCheckedRadioButtonId());
            }
            final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);

            if (optiesSpin != null) {
                savedState.putInt(UPDATE_SPIN_POSITION, optiesSpin.getSelectedItemPosition());
            }
            final EditText optieVal = (EditText) view.findViewById(R.id.optieval);

            if (optieVal != null) {
                savedState.putString(UPDATE_OPTION_VAL, optieVal.getText().toString());
            }
        }
        MyLog.d("savedState = " + savedState);
    }
}
