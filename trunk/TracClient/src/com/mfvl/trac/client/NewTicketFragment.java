package com.mfvl.trac.client;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import com.mfvl.trac.client.util.tcLog;

public class NewTicketFragment extends TracClientFragment {
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
		final ProgressDialog pb = startProgressBar(R.string.downloading);
		tm = listener.getTicketModel();
		if (pb != null && !context.isFinishing()) {
			pb.dismiss();
		}
		createTicket(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// tcLog.d(this.getClass().getName(),"onActivityCreated savedInstanceState = "+
		// (savedInstanceState == null ? "null" : "not null"));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" +
		// item.getTitle());
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.newhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
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

	public void createTicket(final View view) {
		// tcLog.d(this.getClass().getName(), "createTicket");
		final Button backButton = (Button) view.findViewById(R.id.backbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);
		final TableLayout tl = (TableLayout) view.findViewById(R.id.newTickTable);

		try {
			View e = view.findViewById(R.id.waarde);
			final LayoutParams lp = e.getLayoutParams();
			for (int i = 0; i < tm.count(); i++) {
				View v = null;
				final TicketModelVeld veld = tm.getVeld(i);
				final String veldnaam = veld.label();
				int extra = 0;
				if (veldnaam.equals("Resolution") || veldnaam.equals("Status") || veldnaam.equals("Reporter")
						|| veldnaam.equals("Owner") || veldnaam.equals("Created") || veldnaam.equals("Modified")) {
					// ignore these fields so v stays null
				} else if (veld.options() != null) {
					v = makeComboSpin(context, veldnaam, veld.options(), veld.optional(), veld.value());
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
			tcLog.e(getClass().getName(), "Exception in createTicket", e);
		} finally {
			view.invalidate();
		}

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
				final ProgressDialog pb = startProgressBar(R.string.saveticket);
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
										tcLog.e(getClass().getName(), "Exception in createTicket", e);
									}
								}
							}
							velden.put("status", "new");
							velden.put("reporter", _username);
							final CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);
							final boolean notify = updNotify == null ? false : updNotify.isChecked();
							final Ticket t = new Ticket(velden);
							final int newtick = t.create(context, notify);
							if (newtick < 0) {
								throw new RuntimeException("Ticket == -1 ontvangen");
							}
							listener.putTicket(t);
							listener.refreshOverview();
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
									alertDialogBuilder.setTitle(R.string.storok);
									alertDialogBuilder.setMessage(context.getString(R.string.storokdesc) + newtick);
									alertDialogBuilder.setCancelable(false);
									alertDialogBuilder.setPositiveButton(R.string.oktext, null);
									final AlertDialog alertDialog = alertDialogBuilder.create();
									alertDialog.show();
									getFragmentManager().popBackStackImmediate();
								}
							});
						} catch (final Exception e) {
							tcLog.e(getClass().getName(), "Exception in createTicket", e);
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
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
}
