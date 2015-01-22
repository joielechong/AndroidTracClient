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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
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

import com.mfvl.trac.client.util.tcLog;

public class UpdateTicketFragment extends TracClientFragment {
	private String currentActionName = null;
	private JSONArray _actions = null;
	private int ticknr;
	private Boolean sissaved = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		final Bundle args = getArguments();
		// tcLog.d(this.getClass().getName(), "onAttach ");
		if (args != null) {
			ticknr = args.getInt(Const.CURRENT_TICKET);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState );
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// tcLog.d(this.getClass().getName(),
		// "onCreateView savedInstanceState = " + (savedInstanceState == null ?
		// "null" : "not null"));
		final View view = inflater.inflate(R.layout.update_view, container, false);
		return view;
	}

	private void displayView(final int checkedButton, final int spinPosition, final String optionVal) {
		final View view = getView();
		final TextView tv = (TextView) view.findViewById(R.id.titel);
		final String text = context.getString(R.string.updtick) + " " + _ticket;
		tv.setText(text);

		_actions = _ticket.getActions();
		tcLog.d(this.getClass().getName(), "actions = " + _actions);
		final RadioGroup rg = (RadioGroup) view.findViewById(R.id.actionblock);
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
				// tcLog.d(this.getClass().getName(), "inputfields = " +
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
										if (ifValue != null) {
											optieval.setVisibility(View.VISIBLE);
											optieval.setText(ifValue);
										} else {
											optieval.setVisibility(View.GONE);
											optieval.setText(null);
										}
									} else {
										optieval.setVisibility(View.GONE);
										optieval.setText(null);
										optiesSpin.setVisibility(View.VISIBLE);
										final List<Object> opties = new ArrayList<Object>();
										for (int j = 0; j < ifOpties.length(); j++) {
											try {
												opties.add(ifOpties.getString(j));
											} catch (final JSONException e) {
												tcLog.e(getClass().getName(), "displayView exception adding " + ifOpties + " j="
														+ j, e);
											}
										}
										final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context,
												android.R.layout.simple_spinner_item, opties);
										spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
										optiesSpin.setAdapter(spinAdapter);
										if (!"".equals(ifValue) && opties.contains(ifValue)) {
											optiesSpin.setSelection(opties.indexOf(ifValue), true);
										}
									}
								} catch (final Exception e) {
									tcLog.e(getClass().getName(), "displayView exception getting fields", e);
								}
							}
							view.postInvalidate();
						}
					}
				});
				rg.addView(rb);
			}
		} catch (final Exception e) {
			tcLog.e(getClass().getName(), "displayView exception loading ticketdata", e);
		}
		tcLog.d(getClass().getName(), "displayView currentButton = " + checkedButton);
		rg.check(checkedButton);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		final int button;
		final String optionVal;
		final int spinPosition;

		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated savedInstanceState = " + savedInstanceState);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Const.CURRENT_TICKET)) {
				ticknr = savedInstanceState.getInt(Const.CURRENT_TICKET);
			}
			if (savedInstanceState.containsKey(Const.UPDATE_OPTION_VAL)) {
				optionVal = savedInstanceState.getString(Const.UPDATE_OPTION_VAL);
			} else {
				optionVal = null;
			}
			if (savedInstanceState.containsKey(Const.UPDATE_SPIN_POSITION)) {
				spinPosition = savedInstanceState.getInt(Const.UPDATE_SPIN_POSITION);
			} else {
				spinPosition = 0;
			}
			if (savedInstanceState.containsKey(Const.UPDATE_CURRENT_BUTTON)) {
				button = savedInstanceState.getInt(Const.UPDATE_CURRENT_BUTTON);
			} else {
				button = 0;
			}
		} else {
			button = 0;
			optionVal = null;
			spinPosition = 0;
		}
		_ticket = Tickets.getTicket(ticknr);
		if (_ticket != null) {
			displayView(button, spinPosition, optionVal);
		} else {
			_ticket = new Ticket(ticknr, context, new onTicketCompleteListener() {
				@Override
				public void onComplete(Ticket ticket) {
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Tickets.putTicket(_ticket);
							displayView(button, spinPosition, optionVal);
						}
					});
				}
			});
		}
		final View view = getView();
		final Button backButton = (Button) view.findViewById(R.id.canbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				synchronized (sissaved) {
					if (!sissaved) {
						getFragmentManager().popBackStackImmediate();
					}
				}
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				String w = null;
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
				final ProgressDialog pb = startProgressBar(R.string.saveupdate);
				new Thread() {
					@Override
					public void run() {
						try {
							final boolean notify = updNotify == null ? false : updNotify.isChecked();
							_ticket.update(action, comment, currentActionName, waarde, notify, context, null);
							listener.refreshOverview();
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									synchronized (sissaved) {
										if (!sissaved) {
											final FragmentManager fm = getFragmentManager();
											if (fm != null) {
												getFragmentManager().popBackStackImmediate();
											}
										}
									}
								}
							});
						} catch (final Exception e) {
							tcLog.i(getClass().getName(), "update failed", e);
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
									alertDialogBuilder.setTitle(R.string.storerr);
									final String message = e.getMessage();
									if (message == null || "".equals(message)) {
										alertDialogBuilder.setMessage(context.getString(R.string.storerrdesc) + ": " + e);
									} else {
										alertDialogBuilder.setMessage(message);
									}
									alertDialogBuilder.setCancelable(false).setPositiveButton(R.string.oktext, null);
									final AlertDialog alertDialog = alertDialogBuilder.create();
									alertDialog.show();
								}
							});
						} finally {
							if (pb != null && !context.isFinishing()) {
								pb.dismiss();
							}
						}

					}
				}.start();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		synchronized (sissaved) {
			sissaved = false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.updatehelpfile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, false);
			startActivity(launchTrac);
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
			tcLog.d(this.getClass().getName(), "onSaveInstanceState, savedState = " + savedState);
		}
	}
}
