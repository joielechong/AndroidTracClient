package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class UpdateTicketFragment extends TracClientFragment {
	private String currentActionName = null;
	private JSONArray _actions = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.modmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.update_view, container, false);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.i(this.getClass().getName(), "onSaveInstanceState");
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.i(this.getClass().getName(), "onActivityCreated");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				final int currentTicket = savedInstanceState.getInt("currentTicket");
				_ticket = new Ticket(currentTicket, context, null);
			}
		}
		final View v = getView();
		final TextView tv = (TextView) v.findViewById(R.id.titel);
		if (_ticket != null) {
			final String text = context.getString(R.string.updtick) + " " + _ticket;
			tv.setText(text);
		}
		final Button backButton = (Button) v.findViewById(R.id.canbutton);
		final Button storButton = (Button) v.findViewById(R.id.storebutton);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStackImmediate();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				String action = null;
				String comment = null;
				String waarde = null;
				final RadioGroup rg = (RadioGroup) v.findViewById(R.id.actionblock);
				final int sel = rg.getCheckedRadioButtonId();
				final RadioButton rb = (RadioButton) rg.findViewById(sel);
				final EditText et = (EditText) v.findViewById(R.id.comment);
				action = (String) rb.getText();
				comment = et.getText().toString();
				// Log.i(this.getClass().getName(), "storButton onClick sel = "
				// + action + " comment = " + comment);
				final Spinner optiesSpin = (Spinner) v.findViewById(R.id.opties);
				final EditText optieVal = (EditText) v.findViewById(R.id.optieval);
				if (currentActionName != null) {
					if (optieVal.getText() != null && !optieVal.getText().equals("")) {
						waarde = optieVal.getText().toString();
						// Log.i(this.getClass().getName(), "optieVal = " +
						// optieVal.getText());
					}
					if (optiesSpin.getAdapter() != null) {
						waarde = (String) optiesSpin.getSelectedItem();
						// Log.i(this.getClass().getName(), "optiesSpin = " +
						// optiesSpin.getSelectedItem());
					}
				}
				try {
					_ticket.update(action, comment, currentActionName, waarde, context);
					listener.refreshOverview();
					getFragmentManager().popBackStackImmediate();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
					alertDialogBuilder.setTitle(R.string.storerr);
					final String message = e.getMessage();
					if (message == null || message.equals("")) {
						alertDialogBuilder.setMessage(R.string.storerrdesc);
					} else {
						alertDialogBuilder.setMessage(message);
					}
					alertDialogBuilder.setCancelable(false).setPositiveButton(R.string.oktext, null);
					final AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				}
			}
		});

		if (_ticket != null) {
			_actions = _ticket.getActions();
			Log.i(this.getClass().getName(), "actions = " + _actions);
			final RadioGroup rg = (RadioGroup) v.findViewById(R.id.actionblock);
			try {
				for (int i = 0; i < _actions.length(); i++) {
					final JSONArray actie = _actions.getJSONArray(i);
					final LinearLayout ll = new LinearLayout(context);
					ll.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
					ll.setOrientation(LinearLayout.VERTICAL);
					final TextView explain = (TextView) v.findViewById(R.id.explaintxt);
					final RadioButton rb = new RadioButton(context);
					final String hintText = actie.getString(2);
					final Spinner optiesSpin = (Spinner) v.findViewById(R.id.opties);
					final EditText optieval = (EditText) v.findViewById(R.id.optieval);
					final JSONArray inputfields = actie.getJSONArray(3);
					// Log.i(this.getClass().getName(), "inputfields = " +
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
										if (ifValue != null && !ifValue.equals("")) {
											optiesSpin.setSelection(findValueInArray(opties, ifValue), true);
										}

									}
								} catch (final Exception e) {
									e.printStackTrace();
								}
							}
							v.postInvalidate();
						}
					});
					rg.addView(rb);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}

			v.invalidate();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(this.getClass().getName(), "onStart");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.updatehelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void loadTicket(Ticket ticket) {
		Log.i(this.getClass().getName(), "loadTicket ticket = " + ticket);
		_ticket = ticket;
	}

}
