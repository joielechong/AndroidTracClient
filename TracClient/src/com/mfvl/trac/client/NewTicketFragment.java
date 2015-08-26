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

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
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


public class NewTicketFragment extends TracClientFragment implements View.OnClickListener {
    private final static int EXTRA = 1000;
    private TicketModel tm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " +
        // (savedInstanceState == null ? "null" : "not null"));
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // tcLog.d(this.getClass().getName(),"onCreateView savedInstanceState = "
        // + (savedInstanceState == null ? "null" : "not null"));
        if (container == null) {
            return null;
        }
        final View view = inflater.inflate(R.layout.newtick_view, container, false);
        tm = listener.getTicketModel();
        return view;
    }
	
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tcLog.d(this.getClass().getName(), "onViewCreated view = " + view + " sis = " + savedInstanceState);
        final Button storButton = (Button) view.findViewById(R.id.storebutton);
        final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);
		LayoutInflater inflater = LayoutInflater.from(context);

        try {
            View e = view.findViewById(R.id.waarde);
            final LayoutParams lp = e.getLayoutParams();
            final String[] ignoreFields = getResources().getStringArray(R.array.ignorecreatefields);

            for (int i = 0; i < tm.count(); i++) {
                View v = null;
                final TicketModelVeld veld = tm.getVeld(i);
                final String veldnaam = veld.label();
                int extra = 0;

                if (Arrays.asList(ignoreFields).contains(veldnaam)) {// ignore these fields so v stays null
                } else if (veld.options() != null) {
                    v = makeComboSpin(context, veldnaam, veld.options(), veld.optional(), veld.value());
                } else {
					v = (EditText) inflater.inflate((veldnaam.equals("Description") ? R.layout.descrfield: R.layout.stdfield), null, false);
                    extra = EXTRA;
                }
                if (v != null) {
                    v.setLayoutParams(lp);
                    makeRow(tl, veldnaam, v, i + extra);
                }
            }
            e.setVisibility(View.GONE);
            e = view.findViewById(R.id.veld);
            e.setVisibility(View.GONE);
        } catch (final Exception e) {
            tcLog.e(getClass().getName(), "Exception in createTicket", e);
        } finally {
            view.invalidate();
        }
        storButton.setOnClickListener(this);
    }

    public void storeTicket() {
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
                                tcLog.e(getClass().getName(), "Exception in createTicket", e);
                            }
                        }
                    }
                    velden.put("status", "new");
                    velden.put("reporter", context.username);
                    final CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);
                    final boolean notify = updNotify == null ? false : updNotify.isChecked();
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
                    tcLog.e(getClass().getName(), "Exception in createTicket", e);
                    final String message = e.getMessage();
					listener.stopProgressBar();
					showAlertBox(R.string.storerr,R.string.storerrdesc,message);
                } finally {
                }
            }
        }.start();
    }
	
    @Override
    public void onClick(View v) {
		switch (v.getId()) {
			case R.id.storebutton:
			storeTicket();
			break;
			default:
		}
    }
	
	@Override
	public void showHelp() {
        final Intent launchTrac = new Intent(context, TracShowWebPage.class);
        final String filename = context.getString(R.string.newhelpfile);

        launchTrac.putExtra(Const.HELP_FILE, filename);
        launchTrac.putExtra(Const.HELP_VERSION, false);
        startActivity(launchTrac);
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());

        if (item.getItemId() == R.id.help) {
			showHelp();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
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
