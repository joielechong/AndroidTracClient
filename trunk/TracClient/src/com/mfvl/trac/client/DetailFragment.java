package com.mfvl.trac.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.mfvl.trac.client.util.ColoredArrayAdapter;
import com.mfvl.trac.client.util.ISO8601;

public class DetailFragment extends TracClientFragment {

	private boolean activityCreated = false;
	private boolean loading = false;
	private final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	private int ticknr = -1;

	final public static String mimeUnknown = "application/unknown";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			ticknr = savedInstanceState.getInt("currentTicket", -1);
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.detailmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.detail_view, container, false);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.i(this.getClass().getName(), "onSaveInstanceState _ticket = " + _ticket);
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		} else if (ticknr != -1) {
			Log.i(this.getClass().getName(), "onSaveInstanceState ticknr = " + ticknr);
			savedState.putInt("currentTicket", ticknr);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.i(this.getClass().getName(), "onActivityCreated");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		// tm = context.getTicketModel();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				Log.i(this.getClass().getName(), "onActivityCreated start Loading");
				loading = true;
				ticknr = savedInstanceState.getInt("currentTicket", -1);
				if (ticknr != -1) {
					_ticket = new Ticket(ticknr, context, new onTicketCompleteListener() {
						@Override
						public void onComplete() {
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Log.i(this.getClass().getName(), "onActivityCreated onComplete");
									displayTicket(_ticket);
									loading = false;
								}
							});
						};
					});
				}
			}
		}
		activityCreated = true;
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(this.getClass().getName(), "onStart");
		if (!loading) {
			displayTicket(_ticket);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected " + item.toString());
		if (item.getItemId() == R.id.dfupdate) {
			if (_ticket != null) {
				listener.onUpdateTicket(_ticket);
				return true;
			}
		} else if (item.getItemId() == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.helpdetailfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
			return true;
		} else if (item.getItemId() == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", true);
			startActivity(launchTrac);
			return true;
		} else if (item.getItemId() == R.id.dfattach) {
			if (_ticket != null) {
				listener.onChooserSelected(new onFileSelectedListener() {
					@Override
					public void onSelected(final String filename) {
						Log.i(this.getClass().getName(), "onChooserSelected ticket = " + _ticket + " filename = " + filename);
						_ticket.addAttachment(filename, context, new onTicketCompleteListener() {
							@Override
							public void onComplete() {
								refresh_ticket();
							}
						});
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
		}
		return super.onOptionsItemSelected(item);
	}

	public void refresh_ticket() {
		_ticket.refresh(context, new onTicketCompleteListener() {
			@Override
			public void onComplete() {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Log.i(this.getClass().getName(), "refresh onComplete");
						final View v = getView();
						if (v != null) {
							displayTicket(_ticket);
							final ListView lv = (ListView) v.findViewById(R.id.listofFields);
							if (lv != null) {
								lv.invalidateViews();
							}
						}
					}
				});
			};
		});

	}

	@Override
	public void onDestroyView() {
		Log.i(this.getClass().getName(), "onDestroyView");
		activityCreated = false;
		super.onDestroyView();
	}

	private String toonTijd(final JSONObject v) {
		try {
			return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
		} catch (final Exception e) {
			Log.i(this.getClass().getName(), e.toString());
			return "";
		}
	}

	private String getMimeType(String url) {
		String type = null;
		type = URLConnection.guessContentTypeFromName(url);
		return type;
	}

	private void displayTicket(final Ticket ticket) {
		if (ticket != null) {
			final View v = getView();
			if (v == null) {
				return;
			}
			final TextView tickText = (TextView) v.findViewById(R.id.ticknr);
			if (tickText != null) {
				tickText.setText("Ticket " + ticket.getTicketnr());
				try {
					tickText.append(" : " + ticket.getString("summary"));
				} catch (final JSONException e) {
				}
			}
			final ListView listView = (ListView) getView().findViewById(R.id.listofFields);
			final List<String> values = new ArrayList<String>();
			final JSONArray fields = ticket.getFields();
			final int count = fields.length();
			for (int i = 0; i < count; i++) {
				String veld = "veld " + i;
				try {
					veld = fields.getString(i);
					if ("summary".equals(veld) || "_ts".equals(veld)) {
						// skip
					} else if ("time".equals(veld) || "changetime".equals(veld)) {
						values.add(veld + ": " + toonTijd(ticket.getJSONObject(veld)));
					} else if (ticket.getString(veld).length() > 0) {
						values.add(veld + ": " + ticket.getString(veld));
					}
				} catch (final Exception e) {
					values.add(veld + ": Error: " + e.toString());
				}
			}
			final JSONArray history = ticket.getHistory();
			for (int j = 0; j < history.length(); j++) {
				JSONArray cmt;
				try {
					cmt = history.getJSONArray(j);
					if ("comment".equals(cmt.getString(2)) && cmt.getString(4).length() > 0) {
						values.add("comment: " + toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(1) + " - "
								+ cmt.getString(4));
					}
				} catch (final JSONException e) {
					e.printStackTrace();
				}
			}
			final JSONArray attachments = ticket.getAttachments();
			for (int j = 0; j < attachments.length(); j++) {
				JSONArray bijlage;
				try {
					bijlage = attachments.getJSONArray(j);
					values.add("bijlage " + (j + 1) + ": " + toonTijd(bijlage.getJSONObject(3)) + " - " + bijlage.getString(4)
							+ " - " + bijlage.getString(0) + " - " + bijlage.getString(1));

				} catch (final JSONException e) {
					e.printStackTrace();
				}
			}
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final String t = (String) ((ListView) parent).getItemAtPosition(position);
					Log.i(this.getClass().getName() + ".onClick", t);
					if ("bijlage ".equals(t.substring(0, 8))) {
						final int d = t.indexOf(":");
						final int bijlagenr = Integer.parseInt(t.substring(8, d));
						try {
							final String filename = ticket.getAttachmentFile(bijlagenr - 1);
							final String mimeType = getMimeType(filename);
							_ticket.getAttachment(filename, context, new onAttachmentCompleteListener() {
								@Override
								public void onComplete(final byte[] filedata) {
									Log.i(this.getClass().getName(), "onComplete filedata = " + filedata.length);
									try {
										path.mkdirs();
										final File file = new File(path, filename);
										final OutputStream os = new FileOutputStream(file);
										file.deleteOnExit();
										os.write(filedata);
										os.close();
										final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
										Log.i(this.getClass().getName(), "file = " + file.toString() + " mimeType = " + mimeType);
										if (mimeType != null) {
											viewIntent.setDataAndType(Uri.fromFile(new File(path, filename)), mimeType);
											startActivity(viewIntent);
										} else {
											viewIntent.setData(Uri.parse(file.toString()));
											final Intent j = Intent
													.createChooser(viewIntent, "Choose an application to open with:");
											startActivity(j);
										}
									} catch (final IOException e) {
										// Unable to create file, likely
										// because
										// external storage is
										// not currently mounted.
										Log.w(this.getClass().getName(), "ExternalStorage - Error writing " + filename, e);
									} catch (final Exception e) {
										// TODO Auto-generated catch
										// block
										e.printStackTrace();
									}
								};
							});

						} catch (final JSONException e) {
							Log.i(this.getClass().getName(), e.toString());
							// TODO hier een alert
						}
					}
				}
			});

			final ColoredArrayAdapter<String> dataAdapter = new ColoredArrayAdapter<String>(context, R.layout.ticket_list, values);
			listView.setAdapter(dataAdapter);
		}
	}

	public void setTicketContent(final Ticket ticket) {
		Log.i(this.getClass().getName(), "setTicketContent");
		if (_ticket != ticket) {
			_ticket = ticket;
			if (activityCreated) {
				displayTicket(_ticket);
			}
		}
	}

	public void updateTicketContent(final Ticket ticket) {
		Log.i(this.getClass().getName(), "updateTicketContent");
		if (_ticket != ticket) {
			_ticket = ticket;
			if (activityCreated) {
				displayTicket(_ticket);
			}
		}
	}

	@Override
	public void onPause() {
		Log.i(this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
		Log.i(this.getClass().getName(), "onResume");
		super.onResume();
	}

	@Override
	public void onStop() {
		Log.i(this.getClass().getName(), "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.i(this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}
}