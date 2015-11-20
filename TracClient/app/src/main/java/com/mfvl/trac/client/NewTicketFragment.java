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


import java.util.Arrays;

import org.json.JSONObject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import static com.mfvl.trac.client.Const.*;

public class NewTicketFragment extends TracClientFragment {
    private final static int EXTRA = 1000;
    private TicketModel tm;
	private String username = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tcLog.d( "onCreate savedInstanceState = " + savedInstanceState);
		helpFile = R.string.newhelpfile;
        if (fragmentArgs != null) {
            if (fragmentArgs.containsKey(CURRENT_USERNAME)) {
                username = fragmentArgs.getString(CURRENT_USERNAME);
            }
        }
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // tcLog.d("onCreateView savedInstanceState = " + savedInstanceState);
        if (container == null) {
            return null;
        }
        return inflater.inflate(R.layout.newtick_view, container, false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d( "sis = " + savedInstanceState);
		View view = getView();
        tm = listener.getTicketModel();
        final Button storButton = (Button) view.findViewById(R.id.storebutton);
		storButton.setOnClickListener(this);
        final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);
		
//        try {
            View e = view.findViewById(R.id.waarde);
            final LayoutParams lp = e.getLayoutParams();
            final String[] ignoreFields = getResources().getStringArray(R.array.ignorecreatefields);
			boolean first = true;
			
            for (int i = 0; i < tm.count(); i++) {
                View v;
                final TicketModelVeld veld = tm.getVeld(i);
                final String veldnaam = veld.label();
                int extra = 0;
				
                if (!Arrays.asList(ignoreFields).contains(veldnaam)) {
					if (veld.options() != null) {
						v = makeComboSpin(context, veld.options(), veld.optional(), veld.value());
					} else {
						v = LayoutInflater.from(context).inflate((veldnaam.equals("Description") ? R.layout.descrfield: R.layout.stdfield), null, false);
						extra = EXTRA;
					}
                    v.setLayoutParams(lp);
					if (first) {
						v.requestFocus();
						first = false;
					}
                    makeRow(tl, veldnaam, v, i + extra);
                }
            }
            e.setVisibility(View.GONE);
            view.findViewById(R.id.veld).setVisibility(View.GONE);
//        } catch (final Exception e) {
//            tcLog.e( "Exception in createTicket", e);
//        } finally {
            view.invalidate();
//        }
    }
    
    public void onClick(View ignored) {
        final JSONObject velden = new JSONObject();
        final View view = getView();
        final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);
        listener.startProgressBar(R.string.saveticket);
        hideSoftKeyboard(context);
		
        new Thread() {
            @Override
            public void run() {
                try {
                    final int count = tm.count();
					
                    for (int i = 0; i < count; i++) {
                        final TicketModelVeld veld = tm.getVeld(i);
                        final String veldnaam = veld.name();
						View w = tl.findViewById(i + 300);
						
						if (w == null) {
							w = tl.findViewById(i + 300 + EXTRA);
							if (w != null) {
								final String s = ((EditText) w).getText().toString();
								
								if (!"".equals(s)) {
									velden.put(veldnaam, s);
								}
                            }
                        } else {
                            try {
                                final String val = (String) ((Spinner) w).getSelectedItem();
								
                                if (val != null && !val.startsWith(" - ")) {
                                    velden.put(veldnaam, val);
                                }
                            } catch (final Exception e) {
                                tcLog.e( "Exception in createTicket", e);
                            }
                        }
                    }
                    velden.put("status", "new");
                    velden.put("reporter", username);
                    final CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);
                    final boolean notify = updNotify != null && updNotify.isChecked();
                    final Ticket t = new Ticket(velden);
                    final int newtick = listener.createTicket(t, notify);
					
                    if (newtick < 0) {
                        throw new RuntimeException("Ticket == -1 ontvangen");
                    }
                    listener.putTicket(t);
                    listener.stopProgressBar();
                    showAlertBox(R.string.storok,R.string.storokdesc,""+newtick);
                    listener.refreshOverview();
                    getFragmentManager().popBackStack();
                } catch (final Exception e) {
                    tcLog.e( "Exception in createTicket", e);
                    final String message = e.getMessage();
                    listener.stopProgressBar();
                    showAlertBox(R.string.storerr,R.string.storerrdesc,message);
                }
            }
        }.start();
    }
    
    private void makeRow(TableLayout tl, final String veldnaam, View tv2, final int id) {
        if (veldnaam != null) {
            final TableRow tr1 = new TableRow(context);
			
            tr1.setId(id + 100);
            final TextView tv1 = new TextView(context, null, android.R.attr.textAppearanceMedium);
			
            tv1.setId(id + 200);
            tr1.addView(tv1);
            tv1.setText(veldnaam);
            tl.addView(tr1);
        }
        final TableRow tr2 = new TableRow(context);
		
        tv2.setId(id + 300);
        tr2.addView(tv2);
        tl.addView(tr2);
    }
}
