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
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;


public class UpdateTicketFragment extends TracClientFragment implements View.OnClickListener {
    private String currentActionName = null;
    private JSONArray _actions = null;
    private int ticknr;
    private Boolean sissaved = false;
	private static String _tag;
	
	private void onMyAttach(Context activity) {
 		_tag = getClass().getName();
       final Bundle args = getArguments();

        // tcLog.d(_tag, "onAttach ");
        if (args != null) {
            ticknr = args.getInt(Const.CURRENT_TICKET);
        }
	}

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
		onMyAttach(activity);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		onMyAttach(activity);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tcLog.d(_tag, "onCreate savedInstanceState = " + savedInstanceState );
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // tcLog.d(_tag,
        // "onCreateView savedInstanceState = " + (savedInstanceState == null ?
        // "null" : "not null"));
        return inflater.inflate(R.layout.update_view, container, false);
    }

    private void displayView(final int checkedButton, final int spinPosition, final String optionVal) {
        final View view = getView();
        final TextView tv = (TextView) view.findViewById(R.id.titel);
        final String text = context.getString(R.string.updtick) + " " + _ticket;

        tv.setText(text);

        _actions = _ticket.getActions();
        tcLog.d(_tag, "actions = " + _actions);
        final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);
		
		final Button canBut = (Button) view.findViewById(R.id.canBut);
		canBut.setOnClickListener(this);
		
		final Button storeBut = (Button) view.findViewById(R.id.storeBut);
		storeBut.setOnClickListener(this);
		
        try {
            for (int i = 0; i < _actions.length(); i++) {
                final JSONArray actie = _actions.getJSONArray(i);
                final LinearLayout ll = new LinearLayout(context);

                ll.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ll.setOrientation(LinearLayout.VERTICAL);
                final TextView explain = (TextView) view.findViewById(R.id.explaintxt);
                final RadioButton rb = new RadioButton(context);
                final String hintText = actie.getString(2);
                final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);
                final EditText optieval = (EditText) view.findViewById(R.id.optieval);
                final JSONArray inputfields = actie.getJSONArray(3);

                // tcLog.d(_tag, "inputfields = " +
                // inputfields);
                rb.setId(i);
                if (i == 0) {
                    rb.setChecked(true);
                    explain.setText(hintText);
                    optiesSpin.setVisibility(View.GONE);
                    optiesSpin.setAdapter(null);
                    optieval.setVisibility(View.GONE);
                    optieval.setText(null);
                }
                rb.setText(actie.getString(0));
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
                                    final JSONArray ifOpties = inputfields.getJSONArray(0).getJSONArray(2);

                                    if (ifOpties.length() == 0) {
                                        optiesSpin.setAdapter(null);
                                        optiesSpin.setVisibility(View.GONE);
                                        optieval.setVisibility(ifValue != null ? View.VISIBLE : View.GONE);
                                        optieval.setText(ifValue);
                                    } else {
                                        optieval.setVisibility(View.GONE);
                                        optieval.setText(null);
                                        optiesSpin.setVisibility(View.VISIBLE);
                                        final List<Object> opties = new ArrayList<Object>();

                                        for (int j = 0; j < ifOpties.length(); j++) {
                                            try {
                                                opties.add(ifOpties.getString(j));
                                            } catch (final JSONException e) {
                                                tcLog.e(_tag, "displayView exception adding " + ifOpties + " j=" + j,e);
                                            }
                                        }
                                        final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_item, opties);

                                        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        optiesSpin.setAdapter(spinAdapter);
                                        if (!"".equals(ifValue) && opties.contains(ifValue)) {
                                            optiesSpin.setSelection(opties.indexOf(ifValue), true);
                                        }
                                    }
                                } catch (final Exception e) {
                                    tcLog.e(_tag, "displayView exception getting fields", e);
                                }
                            }
                            view.postInvalidate();
                        }
                    }
                });
                rg.addView(rb);
            }
        } catch (final Exception e) {
            tcLog.e(_tag, "displayView exception loading ticketdata", e);
        }
//        tcLog.d(_tag, "displayView currentButton = " + checkedButton);
        rg.check(checkedButton);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        final int button;
        final String optionVal;
        final int spinPosition;

        super.onActivityCreated(savedInstanceState);
        tcLog.d(_tag, "onActivityCreated savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Const.CURRENT_TICKET)) {
                ticknr = savedInstanceState.getInt(Const.CURRENT_TICKET);
            }
            optionVal = (savedInstanceState.containsKey(Const.UPDATE_OPTION_VAL) ?savedInstanceState.getString(Const.UPDATE_OPTION_VAL) : null);
            spinPosition = (savedInstanceState.containsKey(Const.UPDATE_SPIN_POSITION) ?savedInstanceState.getInt(Const.UPDATE_SPIN_POSITION) : 0);
			button = (savedInstanceState.containsKey(Const.UPDATE_CURRENT_BUTTON) ? savedInstanceState.getInt(Const.UPDATE_CURRENT_BUTTON) :0);
        } else {
            button = 0;
            optionVal = null;
            spinPosition = 0;
        }
        _ticket = listener.getTicket(ticknr);
//       if (_ticket != null) {
            displayView(button, spinPosition, optionVal);
/*
        } else {
            _ticket = new Ticket(ticknr, context, new onTicketCompleteListener() {
                @Override
                public void onComplete(Ticket ticket) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.putTicket(_ticket);
                            displayView(button, spinPosition, optionVal);
                        }
                    });
                }
            });
        }
*/
    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.canBut:
			leaveFragment(v);
			break;
			
			case R.id.storeBut:
			storeUpdate(v);
			break;
		}
	}
	
	/**
	 * storeUpdate - called when the Store button is pressed
	*/

    public void storeUpdate(View v1) {
        String w = null;
        final View view = getView();
        final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);
        final int sel = rg.getCheckedRadioButtonId();
        final RadioButton rb = (RadioButton) rg.findViewById(sel);
        final EditText et = (EditText) view.findViewById(R.id.comment);
        final CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);

        final String action = (String) rb.getText();
        final String comment = et.getText().toString();
        final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);
        final EditText optieVal = (EditText) view.findViewById(R.id.optieval);

        if (currentActionName != null) {
            if (!"".equals(optieVal.getText())) {
                w = optieVal.getText().toString();
            }
            if (optiesSpin.getAdapter() != null) {
                w = (String) optiesSpin.getSelectedItem();
            }
        }
        final String waarde = w;
        listener.startProgressBar(R.string.saveupdate);
		try {
			final boolean notify = updNotify == null ? false : updNotify.isChecked();
			listener.updateTicket(_ticket,action, comment, currentActionName, waarde, notify, null);
			} catch (final Exception e) {
				tcLog.e(getClass().getName(), "update failed", e);
				showAlertBox(R.string.storerr,R.string.storerrdesc,e.getMessage());
			} finally {
				listener.stopProgressBar();
			}
/*
        new Thread() {
            @Override
            public void run() {
                try {

                    context.runOnUiThread(new Runnable() {
                        @Override
                         public void run() {
							listener.refreshOverview();
                            synchronized (sissaved) {
                                if (!sissaved) {
									try {
										getFragmentManager().popBackStack();
									} catch (Exception e) {
									}
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
					tcLog.e(getClass().getName(), "update failed", e);
					showAlertBox(R.string.storerr,R.string.storerrdesc,e.getMessage());
                } finally {
					listener.stopProgressBar();
                }
            }
        }.start();
*/
	}

	/**
	 * leaveFragment - called when the Cancel button is pressed
	*/

	public void leaveFragment(View v) {
        synchronized (sissaved) {
            if (!sissaved) {
                getFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (sissaved) {
            sissaved = false;
        }
    }

	public void showHelp() {
		showHelpFile(R.string.updatehelpfile);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d(_tag, "onOptionsItemSelected item=" + item);
        final int itemId = item.getItemId();

        if (itemId == R.id.help) {
			showHelp();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        synchronized (sissaved) {
            super.onSaveInstanceState(savedState);
            if (_ticket != null) {
                savedState.putInt(Const.CURRENT_TICKET, _ticket.getTicketnr());
            }
            final View view = getView();

            if (view != null) {
                final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);

                if (rg != null) {
                    savedState.putInt(Const.UPDATE_CURRENT_BUTTON, rg.getCheckedRadioButtonId());
                }
                final Spinner optiesSpin = (Spinner) view.findViewById(R.id.opties);

                if (optiesSpin != null) {
                    savedState.putInt(Const.UPDATE_SPIN_POSITION, optiesSpin.getSelectedItemPosition());
                }
                final EditText optieVal = (EditText) view.findViewById(R.id.optieval);

                if (optieVal != null) {
                    savedState.putString(Const.UPDATE_OPTION_VAL, optieVal.getText().toString());
                }
            }
            sissaved = true;
            tcLog.d(_tag, "onSaveInstanceState, savedState = " + savedState);
        }
    }
}
