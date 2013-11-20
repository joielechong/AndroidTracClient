package com.mfvl.trac.client;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;

public class NewTicketFragment extends TracClientFragment {
	private final static int EXTRA = 1000;
	private TicketModel tm;

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
		inflater.inflate(R.menu.newtickmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (container == null) {
			return null;
		}
		final View view = inflater.inflate(R.layout.newtick_view, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.i(this.getClass().getName(), "onActivityCreated");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(this.getClass().getName(), "onStart context = " + context);
		tm = context.getTicketModel();
		createTicket();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.newhelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void setHost(final String url, final String username, final String password, boolean sslHack) {
		Log.i(this.getClass().getName(), "setHost");
		if (_url != url) {
			_url = url;
			_sslHack = sslHack;
			_username = username;
			// _password = password;
		}
	}

	public void createTicket() {
		Log.i(this.getClass().getName(), "createTicket");
		final View view = getView();
		final Button backButton = (Button) view.findViewById(R.id.backbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);
		final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStackImmediate();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final JSONObject velden = new JSONObject();
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
								if (s != null && !s.equals("")) {
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
							}
						}
					}
					velden.put("status", "new");
					velden.put("reporter", _username);

					final Ticket t = new Ticket(velden);
					final int newtick = t.create(context);
					if (newtick < 0) {
						throw new RuntimeException("Ticket == -1 ontvangen");
					}
					final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
					alertDialogBuilder.setTitle(R.string.storok);
					alertDialogBuilder.setMessage(context.getString(R.string.storokdesc) + newtick).setCancelable(false)
							.setPositiveButton(R.string.oktext, null);
					final AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					listener.refreshOverview();
					getFragmentManager().popBackStackImmediate();
				} catch (final Exception e) {
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

		try {
			final int count = tm.count();
			View e = view.findViewById(R.id.waarde);
			final LayoutParams lp = e.getLayoutParams();
			for (int i = 0; i < count; i++) {
				View v = null;
				final TicketModelVeld veld = tm.getVeld(i);
				final String veldnaam = veld.label();
				int extra = 0;
				if (veldnaam.equals("Resolution") || veldnaam.equals("Status") || veldnaam.equals("Reporter")
						|| veldnaam.equals("Owner") || veldnaam.equals("Created") || veldnaam.equals("Modified")) {
					// ignore these fields so v stays null
				} else if (veld.options() != null) {
					v = makeComboSpin(veldnaam, veld.options(), veld.optional(), veld.value());
				} else {
					v = new EditText(context);
					((EditText) v).setTextAppearance(context, android.R.attr.textAppearanceMedium);
					((EditText) v).setMinLines(1);
					if (veldnaam.equals("Description")) {
						((EditText) v).setMaxLines(10);
						((EditText) v).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
								| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
					} else {
						((EditText) v).setMaxLines(1);
						((EditText) v).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
					}
					((EditText) v).setEms(10);
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
		}
		view.invalidate();
	}

}
