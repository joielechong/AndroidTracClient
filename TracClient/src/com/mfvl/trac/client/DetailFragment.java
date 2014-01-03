package com.mfvl.trac.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.mfvl.trac.client.util.tcLog;

public class DetailFragment extends TracClientFragment {
	
	private boolean activityCreated = false;
	private boolean loading = false;
	private File path = null;
	private int ticknr = -1;

	final public static String mimeUnknown = "application/unknown";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate");
		tcLog.d(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			ticknr = savedInstanceState.getInt("currentTicket", -1);
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.detailmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(this.getClass().getName(), "onCreateView");
		tcLog.d(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.detail_view, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated");
		tcLog.d(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		// tm = context.getTicketModel();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				tcLog.d(this.getClass().getName(), "onActivityCreated start Loading");
				loading = true;
				ticknr = savedInstanceState.getInt("currentTicket", -1);
				if (ticknr != -1) {
					_ticket = new Ticket(ticknr, context, new onTicketCompleteListener() {
						@Override
						public void onComplete(Ticket t2) {
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									tcLog.d(this.getClass().getName(), "onActivityCreated onComplete");
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
		tcLog.d(this.getClass().getName(), "onStart");
		if (!loading) {
			displayTicket(_ticket);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected " + item.toString());
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
		} else if (item.getItemId() == R.id.dfattach) {
			if (_ticket != null) {
				listener.onChooserSelected(new onFileSelectedListener() {
					@Override
					public void onSelected(final String filename) {
						tcLog.d(this.getClass().getName(), "onChooserSelected ticket = " + _ticket + " filename = " + filename);
						final ProgressDialog pb = startProgressBar(R.string.uploading);
						new Thread("addAttachment") {
							@Override
							public void run() {
								_ticket.addAttachment(filename, context, new onTicketCompleteListener() {
									@Override
									public void onComplete(Ticket t2) {
										refresh_ticket();
										pb.dismiss();
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
						tcLog.d(this.getClass().getName(), "refresh onComplete");
						final View v = getView();
						if (v != null) {
							displayTicket(_ticket);
							final ListView lv = (ListView) v.findViewById(R.id.listofFields);
							if (lv != null) {
								lv.invalidateViews();
							}
						}
						pb.dismiss();
					}
				});
			};
		});

	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState _ticket = " + _ticket);
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		} else if (ticknr != -1) {
			tcLog.d(this.getClass().getName(), "onSaveInstanceState ticknr = " + ticknr);
			savedState.putInt("currentTicket", ticknr);
		}
	}

	@Override
	public void onDestroyView() {
		tcLog.d(this.getClass().getName(), "onDestroyView");
		activityCreated = false;
		super.onDestroyView();
	}

	private String toonTijd(final JSONObject v) {
		try {
			return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
		} catch (final Exception e) {
			tcLog.d(this.getClass().getName(), e.toString());
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
					tcLog.d(this.getClass().getName() + ".onItemClick", t);
					if ("bijlage ".equals(t.substring(0, 8))) {
						final ProgressDialog pb = startProgressBar(R.string.downloading);
						new Thread() {
							@Override
							public void run() {
								final int d = t.indexOf(":");
								final int bijlagenr = Integer.parseInt(t.substring(8, d));
								try {
									final String filename = ticket.getAttachmentFile(bijlagenr - 1);
									final String mimeType = getMimeType(filename);
									ticket.getAttachment(filename, context, new onAttachmentCompleteListener() {
										@Override
										public void onComplete(final byte[] filedata) {
											tcLog.d(this.getClass().getName(), "onComplete filedata = " + filedata.length);
											try {
												if (path == null) {
													path = Environment
															.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
													path.mkdirs();
												}
												final File file = new File(path, filename);
												final OutputStream os = new FileOutputStream(file);
												file.deleteOnExit();
												os.write(filedata);
												os.close();
												final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
												tcLog.d(this.getClass().getName(), "file = " + file.toString() + " mimeType = "
														+ mimeType);
												if (mimeType != null) {
													viewIntent.setDataAndType(Uri.fromFile(new File(path, filename)), mimeType);
													startActivity(viewIntent);
												} else {
													viewIntent.setData(Uri.parse(file.toString()));
													final Intent j = Intent.createChooser(viewIntent,
															context.getString(R.string.chooseapp));
													startActivity(j);
												}
											} catch (final Exception e) {
												tcLog.w(this.getClass().getName(), context.getString(R.string.ioerror) + ": "
														+ filename, e);
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
												pb.dismiss();
											}
										};
									});

								} catch (final JSONException e) {
									tcLog.d(this.getClass().getName(), e.toString());
								}
							}
						}.start();

					}
				}
			});

			final ColoredArrayAdapter<String> dataAdapter = new ColoredArrayAdapter<String>(context, R.layout.ticket_list, values);
			listView.setAdapter(dataAdapter);
		}
	}

	public void setTicketContent(final Ticket ticket) {
		tcLog.d(this.getClass().getName(), "setTicketContent");
		if (_ticket != ticket) {
			_ticket = ticket;
			if (activityCreated) {
				displayTicket(_ticket);
			}
		}
	}

	public void updateTicketContent(final Ticket ticket) {
		tcLog.d(this.getClass().getName(), "updateTicketContent");
		if (_ticket != ticket) {
			_ticket = ticket;
			if (activityCreated) {
				displayTicket(_ticket);
			}
		}
	}

}