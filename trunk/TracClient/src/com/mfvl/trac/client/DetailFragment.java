package com.mfvl.trac.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.trac.client.util.ColoredArrayAdapter;
import com.mfvl.trac.client.util.ISO8601;
import com.mfvl.trac.client.util.tcLog;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class DetailFragment extends TracClientFragment implements OnGestureListener {
	protected static final int LARGE_MOVE = 60;
	
	private class ModVeldMap extends HashMap<String, String> implements Serializable {
		private static final long serialVersionUID = 191019591050L;
	}

	private class modifiedString {
		private boolean _updated;
		private final String _veld;
		private String _waarde;

		public modifiedString(String v, String w) {
			_veld = v;
			_waarde = w;
			_updated = false;
		}

		@Override
		public String toString() {
			return _veld + ": " + _waarde;
		}

		public void setUpdated(boolean u) {
			_updated = u;
		}

		public boolean updated() {
			return _updated;
		}

		public int length() {
			return toString().length();
		}

		public int indexOf(String s) {
			return toString().indexOf(s);
		}

		public String substring(int b, int l) {
			return toString().substring(b, l);
		}

		public String[] split(String s, int c) {
			return toString().split(s, c);
		}

		public void setWaarde(String s) {
			_waarde = s;
		}

		public String veld() {
			return _veld;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (!(o instanceof modifiedString)) {
				return false;
			}

			return _veld.equals(((modifiedString) o).veld());
		}
	}

	private class ModifiedStringArrayAdapter extends ColoredArrayAdapter<modifiedString> {
		public ModifiedStringArrayAdapter(TracStart context, int resource, List<modifiedString> list) {
			super(context, resource, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			try {
				final View view = super.getView(position, convertView, parent);
				final modifiedString ms = getItem(position);
				((TextView) view).setTextColor(ms.updated() ? Color.RED : Color.BLACK);
				return view;
			} catch (Exception e) {
				tcLog.e(getClass().getName(),"getView exception",e);
				return null;
			}
		}
	}

	private File path = null;
	private int ticknr = -1;
	private boolean showEmptyFields = false;
	private TicketModel tm = null;
	private ModVeldMap modVeld;
	private PopupWindow pw = null;
	private boolean sendNotification = false;
	private boolean didUpdate = false;
	private final List<modifiedString> values = new ArrayList<modifiedString>();
	private String[] notModified;
	private String[] isStatusUpd;
	private MenuItem selectItem;
	private GestureDetector gestureDetector = null;

	final public static String mimeUnknown = "application/unknown";
	
	private void setSelect(final boolean value) {
		tcLog.d(this.getClass().getName(), "setSelect "+value);
		if (selectItem != null) {
			selectItem.setEnabled(value);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// tcLog.d(this.getClass().getName(), "onCreate");
		// tcLog.d(this.getClass().getName(), "savedInstanceState = " +
		// (savedInstanceState == null ? "null" : "not null"));
		final Bundle args = getArguments();
		if (args != null) {
			ticknr = args.getInt(Const.CURRENT_TICKET);
		}
		
		modVeld = new ModVeldMap();
		modVeld.clear();
		setHasOptionsMenu(true);
		tm = listener.getTicketModel();
		didUpdate = false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.detailmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
		selectItem = menu.findItem(R.id.dfselect);
		setSelect(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.detail_view, container, false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final Button canButton = (Button) view.findViewById(R.id.cancel);
		final Button updButton = (Button) view.findViewById(R.id.storebutton);
		final CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);

		if (canButton != null) {
			canButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					modVeld.clear();
					setSelect(true);
					getFragmentManager().popBackStack();
				}
			});
		}

		if (updButton != null) {
			updButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					updateTicket();
					final LinearLayout mv = (LinearLayout) getView().findViewById(R.id.modveld);
					if (mv != null) {
						mv.setVisibility(View.GONE);
					}
				}
			});
		}

		if (updNotify != null) {
			updNotify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					sendNotification = isChecked;
				}
			});
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		gestureDetector = new GestureDetector(context,this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		gestureDetector = null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// tcLog.d(this.getClass().getName(), "onActivityCreated");
		// tcLog.d(this.getClass().getName(), "savedInstanceState = " +
		// (savedInstanceState == null ? "null" : "not null"));

		if (savedInstanceState != null) {
			showEmptyFields = savedInstanceState.getBoolean("emptyfields", false);
			if (savedInstanceState.containsKey("currentTicket")) {
				// tcLog.d(this.getClass().getName(),"onActivityCreated start Loading");
				if (savedInstanceState.containsKey("modveld")) {
					modVeld = (ModVeldMap) savedInstanceState.getSerializable("modveld");
				}
				setSelect(modVeld.isEmpty());
				ticknr = savedInstanceState.getInt("currentTicket", -1);
			}
		}
	notModified = getResources().getStringArray(R.array.fieldsnotmodified);
	isStatusUpd = getResources().getStringArray(R.array.fieldsstatusupdate);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (ticknr != -1) {
			_onResume();
		} else {
			getFragmentManager().popBackStack();
		}
	}

	private void _onResume() {
		_ticket = listener.getTicket(ticknr);
		if (_ticket == null) {
			_ticket = new Ticket(ticknr, context, new onTicketCompleteListener() {
				@Override
				public void onComplete(Ticket t) {
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							listener.putTicket(_ticket);
							_resume();
						}
					});
				}
			});
		} else {
			_resume();
		}
	}

	private void _resume() {
		displayTicket();

		if (didUpdate) {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					refresh_ticket();
				}
			});
			didUpdate = false;
		}
		View v = getView();
		if (v != null) {
			final LinearLayout mv = (LinearLayout) v.findViewById(R.id.modveld);
			if (mv != null) {
				mv.setVisibility(modVeld.isEmpty() ? View.GONE : View.VISIBLE);
			}
		}
		setSelect(modVeld.isEmpty());
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		tcLog.i(getClass().getName(), "onPrepareOptionsMenu");
		MenuItem item = menu.findItem(R.id.dfempty);
		if (item != null) {
			item.setChecked(showEmptyFields);
		}
		setSelect(modVeld.isEmpty());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// tcLog.d(getClass().getName(), "onOptionsItemSelected " +
		// item.toString());
		if (item.getItemId() == R.id.dfupdate) {
			if (_ticket != null) {
				listener.onUpdateTicket(_ticket);
				didUpdate = true;
				return true;
			}
		} else if (item.getItemId() == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.helpdetailfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
			return true;
		} else if (item.getItemId() == R.id.dfselect) {
			final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
			alertDialogBuilder.setTitle(R.string.chooseticket);
			alertDialogBuilder.setMessage(R.string.chooseticknr);
			final EditText input = new EditText(context);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			alertDialogBuilder.setView(input);

			alertDialogBuilder.setCancelable(false);
			alertDialogBuilder.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					final int ticknr = Integer.parseInt(input.getText().toString());
					selectTicket(ticknr);
				}
			});
			alertDialogBuilder.setNegativeButton(R.string.cancel, null);
			final AlertDialog alertDialog = alertDialogBuilder.create();
			if (!context.isFinishing()) {
				alertDialog.show();
			}
		} else if (item.getItemId() == R.id.dfattach) {
			if (_ticket != null) {
				listener.onChooserSelected(new onFileSelectedListener() {
					@Override
					public void onSelected(final String filename) {
						// tcLog.d(this.getClass().getName(),"onChooserSelected ticket = "
						// + _ticket + " filename = " + filename);
						final ProgressDialog pb = startProgressBar(R.string.uploading);
						new Thread("addAttachment") {
							@Override
							public void run() {
								_ticket.addAttachment(filename, context, new onTicketCompleteListener() {
									@Override
									public void onComplete(Ticket t2) {
										refresh_ticket();
										if (pb != null && !context.isFinishing()) {
											pb.dismiss();
										}
									}
								});

							}
						}.start();
					}
				});
				return true;
			}
		} else if (item.getItemId() == R.id.dfrefresh) {
			if (_ticket != null) {
				refresh_ticket();
				return true;
			}
		} else if (item.getItemId() == R.id.dfshare) {
			if (_ticket != null) {
				listener.shareTicket(_ticket);
				return true;
			}
		} else if (item.getItemId() == R.id.dfempty) {
			item.setChecked(!item.isChecked());
			showEmptyFields = item.isChecked();
			if (_ticket != null) {
				refresh_ticket();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	public void refresh_ticket() {
		final ProgressDialog pb = startProgressBar(R.string.updating);
		_ticket.refresh(context, new onTicketCompleteListener() {
			@Override
			public void onComplete(Ticket t2) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final View v = getView();
						if (v != null) {
							displayTicket();
							final ListView lv = (ListView) v.findViewById(R.id.listofFields);
							if (lv != null) {
								lv.invalidateViews();
							}
						}
						if (pb != null && !context.isFinishing()) {
							pb.dismiss();
						}
					}
				});
			};
		});
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(getClass().getName(), "onSaveInstanceState _ticket = " + _ticket);
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		} else if (ticknr != -1) {
			tcLog.d(getClass().getName(), "onSaveInstanceState ticknr = " + ticknr);
			savedState.putInt("currentTicket", ticknr);
		}
		if (!modVeld.isEmpty()) {
			tcLog.d(getClass().getName(), "onSaveInstanceState modVeld = " + modVeld);
			savedState.putSerializable("modveld", modVeld);
		}
		savedState.putBoolean("emptyfields", showEmptyFields);
		tcLog.d(getClass().getName(), "onSaveInstanceState = " + savedState);
	}

	@Override
	public void onDestroyView() {
		// tcLog.d(this.getClass().getName(), "onDestroyView");
		super.onDestroyView();
	}

	private String toonTijd(final JSONObject v) {
		try {
			return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
		} catch (final Exception e) {
			tcLog.e(this.getClass().getName(), "Error converting time", e);
			return "";
		}
	}

	private String getMimeType(String url) {
		String type = null;
		type = URLConnection.guessContentTypeFromName(url);
		return type;
	}

	private void displayTicket() {
		tcLog.d(this.getClass().getName(), "displayTicket ticket = " + _ticket);
		if (_ticket != null) {
			final View v = getView();
			if (v == null) {
				return;
			}
			final ListView listView = (ListView) getView().findViewById(R.id.listofFields);
			listView.setAdapter(null);
			values.clear();
			final TextView tickText = (TextView) v.findViewById(R.id.ticknr);
			if (tickText != null) {
				tickText.setText("Ticket " + _ticket.getTicketnr());
				try {
					String summ = _ticket.getString("summary");
					tickText.setTextColor(Color.BLACK);
					if (modVeld.containsKey("summary")) {
						summ = modVeld.get("summary");
						tickText.setTextColor(Color.RED);
					}
					tickText.append(" : " + summ);
				} catch (final JSONException e) {
					tcLog.e(getClass().getName(), "JSONException fetching summary");
				}
				tickText.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view) {
						selectField("summary", ((TextView) view).getText().toString(), tickText);
						tcLog.d(getClass().getName(), "OnLongClick tickText modVeld = " + modVeld);
						return true;
					}
				});
			}
			final ArrayList<String> fields = tm.velden();
			final int count = fields.size();
			for (int i = 0; i < count; i++) {
				String veld = "veld " + i;
				try {
					veld = fields.get(i);
					modifiedString ms = null;
					if ("summary".equals(veld) || "_ts".equals(veld) || "max".equals(veld) || "page".equals(veld)
							|| "id".equals(veld)) {
						// skip
					} else if ("time".equals(veld) || "changetime".equals(veld)) {
						ms = new modifiedString(veld, toonTijd(_ticket.getJSONObject(veld)));
					} else if (showEmptyFields || _ticket.getString(veld).length() > 0) {
						ms = new modifiedString(veld, _ticket.getString(veld));
					}
					if (ms != null) {
						if (modVeld.containsKey(veld)) {
							ms.setUpdated(true);
							ms.setWaarde(modVeld.get(veld));
							setSelect(false);
						}
						values.add(ms);
					}
				} catch (final Exception e) {
					tcLog.e(getClass().getName(), "JSONException fetching field " + veld);
					values.add(new modifiedString(veld, ""));
				}
			}
			final JSONArray history = _ticket.getHistory();
			if (history != null) {
				for (int j = 0; j < history.length(); j++) {
					JSONArray cmt;
					try {
						cmt = history.getJSONArray(j);
						if ("comment".equals(cmt.getString(2)) && cmt.getString(4).length() > 0) {
							values.add(new modifiedString("comment", toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(1)
									+ " - " + cmt.getString(4)));
						}
					} catch (final JSONException e) {
						tcLog.e(getClass().getName(), "JSONException in displayTicket loading history");
					}
				}
			}
			final JSONArray attachments = _ticket.getAttachments();
			if (attachments != null) {
				for (int j = 0; j < attachments.length(); j++) {
					JSONArray bijlage;
					try {
						bijlage = attachments.getJSONArray(j);
						values.add(new modifiedString("bijlage " + (j + 1), toonTijd(bijlage.getJSONObject(3)) + " - "
								+ bijlage.getString(4) + " - " + bijlage.getString(0) + " - " + bijlage.getString(1)));

					} catch (final JSONException e) {
						tcLog.e(getClass().getName(), "JSONException in displayTicket loading attachments", e);
					}
				}
			}
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					final modifiedString t = (modifiedString) ((ListView) parent).getItemAtPosition(position);
					tcLog.d(this.getClass().getName() + ".onItemLongClick", t.toString());
					if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
						return false;
					} else if (t.length() >= 8 && "comment:".equals(t.substring(0, 8))) {
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								final AlertDialog.Builder alert = new AlertDialog.Builder(context);
								alert.setTitle(R.string.notpossible);
								alert.setMessage(R.string.nocomment);
								alert.setPositiveButton(R.string.oktext, null);
								alert.show();
							}
						});
					} else {
						final String[] parsed = t.split(":", 2);
						selectField(parsed[0], parsed[1].trim(), null);
					}
					return true;
				}
			});
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final modifiedString t = (modifiedString) ((ListView) parent).getItemAtPosition(position);
					tcLog.d(this.getClass().getName() + ".onItemClick", t.toString());
					if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
						final int d = t.indexOf(":");
						final int bijlagenr = Integer.parseInt(t.substring(8, d));
						selectBijlage(bijlagenr);
					}
				}
			});
			final ModifiedStringArrayAdapter dataAdapter = new ModifiedStringArrayAdapter(context, R.layout.ticket_list, values);
			listView.setAdapter(dataAdapter);
		}
	}

	private void selectField(final String veld, final String waarde, final View dataView) {
		if (Arrays.asList(notModified).contains(veld)) {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final AlertDialog.Builder alert = new AlertDialog.Builder(context);
					alert.setTitle(R.string.notpossible);
					alert.setMessage(R.string.notchange);
					alert.setPositiveButton(R.string.oktext, null);
					alert.show();
				}
			});
		} else if (Arrays.asList(isStatusUpd).contains(veld)) {
			listener.onUpdateTicket(_ticket);
			didUpdate = true;
		} else {
			final TicketModelVeld tmv = tm.getVeld(veld);
			final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			final RelativeLayout ll = (RelativeLayout) inflater.inflate(tmv.options() == null ? R.layout.field_spec1
					: R.layout.field_spec2, null, false);
			final TextView tv = (TextView) ll.findViewById(R.id.veldnaam);
			tv.setText(veld);
			final EditText et = (EditText) ll.findViewById(R.id.veldwaarde);
			final Spinner spinValue = tmv.options() == null ? null : makeDialogComboSpin(getActivity(), veld, tmv.options(),
					tmv.optional(), waarde);
			final Button canBut = (Button) ll.findViewById(R.id.cancel);
			final Button storBut = (Button) ll.findViewById(R.id.storebutton);
			final ListView parent = (ListView) getView().findViewById(R.id.listofFields);

			if (et != null) {
				et.setText(waarde);
				et.requestFocus();
			}

			if (spinValue != null) {
				spinValue.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				final LinearLayout vc = (LinearLayout) ll.findViewById(R.id.veld);
				if (vc != null) {
					vc.addView(spinValue);
				}
			}

			canBut.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (pw != null && !context.isFinishing()) {
						pw.dismiss();
					}
				}
			});

			storBut.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String newValue = null;
					if (spinValue != null) {
						newValue = spinValue.getSelectedItem().toString();
					}
					if (et != null) {
						newValue = et.getText().toString();
					}
					if ((newValue != null && !newValue.equals(waarde)) || (newValue == null && waarde != null)) {
						if (veld.equals("summary")) {
							((TextView) dataView).setText(newValue);
							((TextView) dataView).setTextColor(Color.RED);
							tcLog.d(getClass().getName(), "OnClick tickText na postInvalidate + " + dataView);
							final String[] parsed = newValue.split(":", 2);
							modVeld.put("summary", parsed[1].trim());
						} else {
							final int pos = ((ModifiedStringArrayAdapter) parent.getAdapter()).getPosition(new modifiedString(veld,
									newValue));
							if (pos >= 0) {
								final modifiedString ms = values.get(pos);
								ms.setWaarde(newValue);
								ms.setUpdated(true);
								values.set(pos, ms);
								((ModifiedStringArrayAdapter) parent.getAdapter()).notifyDataSetChanged();
							}
							modVeld.put(veld, newValue);
						}
						setSelect(false);
					}
					final LinearLayout mv = (LinearLayout) getView().findViewById(R.id.modveld);
					if (mv != null && !modVeld.isEmpty()) {
						mv.setVisibility(View.VISIBLE);
					}
					if (pw != null && !context.isFinishing()) {
						pw.dismiss();
					}
				}
			});

			pw = new PopupWindow(getActivity());
			pw.setFocusable(true);
			pw.setContentView(ll);
			final Drawable drw = new ColorDrawable(0xffdddddd);
			drw.setAlpha(220);
			pw.setBackgroundDrawable(drw);
			pw.setWidth(getView().getWidth() * 9 / 10);
			pw.setHeight(getView().getHeight() * 4 / 5);
			pw.showAtLocation(parent, Gravity.CENTER, 0, 0);
		}
	}

	public boolean onBackPressed() {
		tcLog.d(this.getClass().getName(), "onBackPressed");
		if (pw != null) {
			if (pw.isShowing() && !context.isFinishing()) {
				pw.dismiss();
				return true;
			}
		}
		if (!modVeld.isEmpty()) {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final AlertDialog.Builder alert = new AlertDialog.Builder(context);
					alert.setTitle(R.string.warning);
					alert.setMessage(R.string.unsaved);
					alert.setPositiveButton(R.string.ja, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							updateTicket();
							getFragmentManager().popBackStack();
						}
					});
					alert.setNegativeButton(R.string.nee, null);
					alert.show();
				}
			});
			return true;
		}
		return false;
	}

	private void selectBijlage(final int bijlagenr) {
		final ProgressDialog pb = startProgressBar(R.string.downloading);
		new Thread() {
			@Override
			public void run() {
				try {
					final String filename = _ticket.getAttachmentFile(bijlagenr - 1);
					final String mimeType = getMimeType(filename);
					_ticket.getAttachment(filename, context, new onAttachmentCompleteListener() {
						@Override
						public void onComplete(final byte[] filedata) {
							// tcLog.d(this.getClass().getName(),"onComplete filedata = "
							// + filedata.length);
							try {
								if (path == null) {
									path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
									path.mkdirs();
								}
								final File file = new File(path, filename);
								final OutputStream os = new FileOutputStream(file);
								file.deleteOnExit();
								os.write(filedata);
								os.close();
								final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
								// tcLog.d(this.getClass().getName(), "file = "
								// + file.toString() + " mimeType = " +
								// mimeType);
								if (mimeType != null) {
									viewIntent.setDataAndType(Uri.fromFile(new File(path, filename)), mimeType);
									startActivity(viewIntent);
								} else {
									viewIntent.setData(Uri.parse(file.toString()));
									final Intent j = Intent.createChooser(viewIntent, context.getString(R.string.chooseapp));
									startActivity(j);
								}
							} catch (final Exception e) {
								tcLog.w(this.getClass().getName(), context.getString(R.string.ioerror) + ": " + filename, e);
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										final AlertDialog.Builder alert = new AlertDialog.Builder(context);
										alert.setTitle(R.string.notfound);
										alert.setMessage(R.string.sdcardmissing);
										alert.setPositiveButton(R.string.oktext, null);
										alert.show();
									}
								});
							} finally {
								if (pb != null && !context.isFinishing()) {
									pb.dismiss();
								}
							}
						};
					});

				} catch (final JSONException e) {
					tcLog.e(this.getClass().getName(), "JSONException fetching attachment", e);
				}
			}
		}.start();
	}

	private void updateTicket() {
		tcLog.d(getClass().getName(), "updateTicket");
		new Thread() {
			@Override
			public void run() {
				try {
					_ticket.update("leave", "", null, null, sendNotification, context, modVeld);
					for (final modifiedString s : values) {
						s.setUpdated(false);
					}
					modVeld.clear();
					listener.refreshOverview();
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setSelect(true);
							refresh_ticket();
						}
					});
				} catch (final Exception e) {
					tcLog.e(getClass().getName(), "Exception during update", e);
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
				}
			}
		}.start();
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		final int newTicket;
//		tcLog.d(getClass().getName(),"onFling e1 = "+e1+", e2 = "+e2);
		if (e1.getX() - e2.getX() > LARGE_MOVE) {
			newTicket = listener.getNextTicket(_ticket.getTicketnr());
		} else if (e2.getX() - e1.getX() > LARGE_MOVE) {
			newTicket = listener.getPrevTicket(_ticket.getTicketnr());
		} else {
			newTicket = -1;
		}
		if (newTicket >= 0 && modVeld.isEmpty()) {
			ticknr = newTicket;
			_onResume();
		} 
		return true;
	}
	
	public boolean dispatchTouchEvent(MotionEvent ev) {
		return (gestureDetector != null ? gestureDetector.onTouchEvent(ev) : false);
	}
}
