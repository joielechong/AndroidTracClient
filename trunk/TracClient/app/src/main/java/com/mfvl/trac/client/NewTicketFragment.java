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


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import static com.mfvl.trac.client.Const.*;

public class NewTicketFragment extends TracClientFragment {
    static final private String NotfifyField = "Notify";
    private String username = null;
    private TicketModel tm = null;

    @Override
    public int getHelpFile() {
        return R.string.newhelpfile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MyLog.d( "onCreate savedInstanceState = " + savedInstanceState);
        if (fragmentArgs != null) {
            if (fragmentArgs.containsKey(CURRENT_USERNAME)) {
                username = fragmentArgs.getString(CURRENT_USERNAME);
            }
        }
        setHasOptionsMenu(true);
        tm = listener.getService().getTicketModel();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // MyLog.d("onCreateView savedInstanceState = " + savedInstanceState);
        if (container == null) {
            return null;
        }
        return inflater.inflate(R.layout.newtick_view, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyLog.d("savedInstanceState = " + savedInstanceState);
        final View view = getView();
        view.findViewById(R.id.storebutton).setOnClickListener(this);
        CheckBox notify = (CheckBox) view.findViewById(R.id.updNotify);
        final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);
        if (savedInstanceState != null && savedInstanceState.containsKey(NotfifyField)) {
            notify.setSelected(savedInstanceState.getBoolean(NotfifyField));
        }

        final String[] ignoreFields = getResources().getStringArray(R.array.ignorecreatefields);
        boolean first = true;

        for (int i = 0; i < tm.count(); i++) {
            final TicketModelVeld veld = tm.getVeld(i);
            final String veldnaam = veld.label();
            //MyLog.d("i = "+i+" veld = "+veld+" velnaam = "+veldnaam);

            if (!Arrays.asList(ignoreFields).contains(veldnaam)) {
                View v;
                if (veld.options() != null) {
                    List<Object> waardes = veld.options();
                    boolean optional = veld.optional();

                    v = LayoutInflater.from(getActivity()).inflate(R.layout.spinfield, tl, false);
                    @SuppressLint("CutPasteId") Spinner v1 = (Spinner) v.findViewById(R.id.nt_val);
                    v1.setPrompt(veldnaam);
                    SpinnerAdapter a = makeComboAdapter(waardes, optional);
                    v1.setAdapter(a);
                    if (savedInstanceState != null && savedInstanceState.containsKey(veldnaam)) {
                        v1.setSelection(savedInstanceState.getInt(veldnaam));
                    }
                    v1.setTag(veld.name());
                } else {
                    v = LayoutInflater.from(getActivity()).inflate(("Description".equals(veldnaam) ? R.layout.descrfield : R.layout.stdfield), tl, false);
                    @SuppressLint("CutPasteId") EditText e = (EditText) v.findViewById(R.id.nt_val);
                    if (savedInstanceState != null && savedInstanceState.containsKey(veldnaam)) {
                        e.setText(savedInstanceState.getString(veldnaam));
                    }
                    e.setTag(veld.name());
                }
                ((TextView) v.findViewById(R.id.veldnaam)).setText(veldnaam);
                tl.addView(v);
                if (first) {
                    v.requestFocus();
                    first = false;
                }
            }
        }
        view.invalidate();
    }

    @SuppressWarnings("OverlyStrongTypeCast")
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        View v = getView();
        if (v != null) {
            savedState.putBoolean(NotfifyField, ((Checkable) v.findViewById(R.id.updNotify)).isChecked());
            final TableLayout tl = (TableLayout) v.findViewById(R.id.newTickTable);
            if (tl != null) {
                for (int i = 0; i < tm.count(); i++) {
                    final String veldnaam = tm.getVeld(i).name();
                    View w = tl.findViewWithTag(veldnaam);
                    if (w instanceof Spinner) {
                        try {
                            savedState.putInt(veldnaam, ((Spinner) w).getSelectedItemPosition());
                        } catch (final Exception e) {
                            MyLog.e("Exception in createTicket", e);
                        }
                    } else if (w != null) {
                        final CharSequence s = ((TextView) w).getText();
                        if (!TextUtils.isEmpty(s)) {
                            savedState.putString(veldnaam, s.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClick(View ignored) {
        // Only store button
        final JSONObject velden = new JSONObject();
        listener.startProgressBar(R.string.saveticket);
        hideSoftKeyboard(getActivity());

        new Thread(new Runnable() {
            @Override
            public void run() {
                View view = getView();
                TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);
                try {
                    final int count = tm.count();

                    for (int i = 0; i < count; i++) {
                        final TicketModelVeld veld = tm.getVeld(i);
                        final String veldnaam = veld.name();
                        View w = tl.findViewWithTag(veldnaam);

                        if (w instanceof Spinner) {
                            try {
                                @SuppressWarnings("OverlyStrongTypeCast")
                                final String val = (String) ((Spinner) w).getSelectedItem();

                                if (val != null && !val.startsWith(" - ")) {
                                    velden.put(veldnaam, val);
                                }
                            } catch (final Exception e) {
                                MyLog.e("Exception in createTicket", e);
                            }
                        } else if (w != null) {
                            final CharSequence s = ((TextView) w).getText();

                            if (!TextUtils.isEmpty(s)) {
                                velden.put(veldnaam, s);
                            }
                        }
                    }
                    velden.put("status", "new");
                    velden.put("reporter", username);
                    final Checkable updNotify = (Checkable) view.findViewById(R.id.updNotify);
                    final boolean notify = updNotify != null && updNotify.isChecked();
                    final Ticket t = new Ticket(velden);
                    final int newtick = listener.createTicket(t, notify);

                    if (newtick < 0) {
                        throw new RuntimeException("Ticket == -1 ontvangen");
                    }
                    listener.stopProgressBar();
                    showAlertBox(R.string.storok, getString(R.string.storokdesc, newtick));
                    listener.refreshOverview();
                    getFragmentManager().popBackStack();
                } catch (final Exception e) {
                    MyLog.e("Exception in createTicket", e);
                    listener.stopProgressBar();
                    showAlertBox(R.string.storerr, getString(R.string.storerrdesc, e.getMessage()));
                }
            }
        }).start();
    }
}
