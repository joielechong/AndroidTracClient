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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class UpdateTicketFragment extends TracClientFragment {
	private String currentActionName = null;
	private JSONArray _actions = null;
	private int ticknr;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		final Bundle args = this.getArguments();
		// tcLog.d(this.getClass().getName(), "onAttach ");
		if (args != null) {
			ticknr = args.getInt(Const.CURRENT_TICKET);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " +
		// (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// tcLog.d(this.getClass().getName(),
		// "onCreateView savedInstanceState = " + (savedInstanceState == null ?
		// "null" : "not null"));
		final View view = inflater.inflate(R.layout.update_view, container, false);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				ticknr = savedInstanceState.getInt("currentTicket");
			}
		}
		_ticket = listener.getTicket(ticknr);
		if (_ticket != null) {
			_actions = _ticket.getActions();
			// tcLog.d(this.getClass().getName(), "actions = " + _actions);
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
					rb.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v1) {
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
												e.printStackTrace();
											}
										}
										final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context,
												android.R.layout.simple_spinner_item, opties);
										spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
										optiesSpin.setAdapter(spinAdapter);
										if (ifValue != null && !ifValue.equals("") && opties.contains(ifValue)) {
											optiesSpin.setSelection(opties.indexOf(ifValue), true);
										}

									}
								} catch (final Exception e) {
									e.printStackTrace();
								}
							}
							view.postInvalidate();
						}
					});
					rg.addView(rb);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// tcLog.d(this.getClass().getName(),
		// "onActivityCreated savedInstanceState = "+ (savedInstanceState ==
		// null ? "null" : "not null"));
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				ticknr = savedInstanceState.getInt("currentTicket");
			}
		}
		_ticket = listener.getTicket(ticknr);
		final View view = getView();
		final TextView tv = (TextView) view.findViewById(R.id.titel);
		if (_ticket != null) {
			final String text = context.getString(R.string.updtick) + " " + _ticket;
			tv.setText(text);
		}
		final Button backButton = (Button) view.findViewById(R.id.canbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				getFragmentManager().popBackStackImmediate();
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
					if (optieVal.getText() != null && !optieVal.getText().equals("")) {
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
									getFragmentManager().popBackStackImmediate();
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();
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
							pb.dismiss();
						}

					}
				}.start();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" +
		// item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.updatehelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		// tcLog.d(this.getClass().getName(), "onSaveInstanceState");
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		}
	}
}
