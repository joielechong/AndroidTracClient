package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mfvl.trac.client.util.ColoredArrayAdapter;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.SortSpec;

public class TicketListFragment extends TracClientFragment {

	private static final String TICKETLISTNAME = "ticketlist";
	private static final String SORTLISTNAME = "sortlist";
	private static final String FILTERLISTNAME = "filterlist";
	private static final String ZOEKENNAME = "zoeken";
	private static final String ZOEKTEXTNAME = "filtertext";
	private static final String SCROLLPOSITIONNAME = "scrollPosition";

	List<Ticket> ticketList = new ArrayList<Ticket>();
	boolean viewActive = false;
	private Thread networkThread = null;
	private ColoredArrayAdapter<Ticket> dataAdapter = null;
	private ListView listView = null;
	private final static int ticketGroupCount = 50;
	private EditText filterText = null;
	private TextView hs = null;
	private int tickets[] = null;
	private boolean loading = false;
	private boolean zoeken = false;
	private String zoektext = "";
	ArrayList<SortSpec> sortList = null;
	ArrayList<FilterSpec> filterList = null;
	int scrollPosition = 0;
	boolean refreshOnRestart = false;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		ticketList.clear();
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			tickets = savedInstanceState.getIntArray(TICKETLISTNAME);
			sortList = (ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME);
			filterList = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME);
			zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
			zoektext = savedInstanceState.getString(ZOEKTEXTNAME);
			scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME, 0);
		}
		if (sortList == null) {
			sortList = new ArrayList<SortSpec>();
			sortList.add(new SortSpec("priority"));
			sortList.add(new SortSpec("modified", false));
		}
		if (filterList == null) {
			filterList = new ArrayList<FilterSpec>();
			filterList.add(new FilterSpec("max", "=", "500"));
			filterList.add(new FilterSpec("status", "!=", "closed"));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(this.getClass().getName(), "onCreateView savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.list_view, container, false);
		listView = (ListView) view.findViewById(R.id.listofTickets);
		registerForContextMenu(listView);

		if (listView == null) {
			throw new RuntimeException(context.getString(R.string.nolist));
		} else {
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final Ticket t = (Ticket) ((ListView) parent).getItemAtPosition(position);
					if (t.hasdata()) {
						listener.onTicketSelected(t);
					} else {
						final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
						alertDialogBuilder.setTitle(R.string.nodata);
						alertDialogBuilder.setMessage(R.string.nodatadesc).setCancelable(false)
								.setPositiveButton(R.string.oktext, null);
						final AlertDialog alertDialog = alertDialogBuilder.create();
						alertDialog.show();
					}
				}
			});
			filterText = (EditText) view.findViewById(R.id.search_box);
			if (zoeken) {
				filterText.setVisibility(View.VISIBLE);
			} else {
				filterText.setVisibility(View.GONE);
				if (filterText.isFocused()) {
					filterText.clearFocus();
				}
				zoektext = "";
			}
			filterText.setText(zoektext);
			filterText.addTextChangedListener(filterTextWatcher);
			hs = (TextView) view.findViewById(R.id.listProgress);
			dataAdapter = new ColoredArrayAdapter<Ticket>(context, R.layout.ticket_list, ticketList);
			if (dataAdapter == null) {
				throw new RuntimeException(context.getString(R.string.noadapter));
			}
			listView.setAdapter(dataAdapter);
		}
		viewActive = true;
		return view;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.listofTickets) {
			final MenuInflater inflater = context.getMenuInflater();
			inflater.inflate(R.menu.listcontextmenu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final Ticket t = (Ticket) listView.getItemAtPosition(info.position);
		switch (item.getItemId()) {
		case R.id.select:
			if (t.hasdata()) {
				listener.onTicketSelected(t);
			}
			return true;
		case R.id.dfupdate:
			if (t.hasdata()) {
				listener.onUpdateTicket(t);
			}
			return true;
		case R.id.dfshare:
			if (t.hasdata()) {
				final Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, t.toText());
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void setScroll() {
		Log.d(this.getClass().getName(), "setScroll " + Build.VERSION.SDK_INT);
		listView.setSelection((scrollPosition == 0 ? 0 : scrollPosition + 1));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(this.getClass().getName(), "onActivityCreated savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
			filterText.setText(zoektext);
			scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME);
			if (tickets == null || tickets.length == 0) {
				loadTicketList();
			} else {
				loadTickets();
			}
		} else if (ticketList.size() == 0) {
			loadTicketList();
		}
		setScroll();
	}

	@Override
	public void onStart() {
		Log.d(this.getClass().getName(), "onStart");
		super.onStart();
	}

	@Override
	public void onPause() {
		Log.d(this.getClass().getName(), "onPause");
		super.onPause();
		scrollPosition = listView.getFirstVisiblePosition();
		Log.d(this.getClass().getName(), "onPause Scroll " + scrollPosition);
	}

	@Override
	public void onResume() {
		Log.d(this.getClass().getName(), "onResume");
		super.onResume();
		if (refreshOnRestart) {
			scrollPosition = 0;
			tickets = null;
			ticketList.clear();
			refreshOnRestart = false;
			if (listView != null) {
				listView.setAdapter(null);
			}
		}
		if (!loading && ticketList.size() == 0) {
			if (tickets == null) {
				loadTicketList();
			}
		}
		dataAdapter = new ColoredArrayAdapter<Ticket>(context, R.layout.ticket_list, ticketList);
		if (dataAdapter == null) {
			throw new RuntimeException(context.getString(R.string.noadapter));
		}
		listView.setAdapter(dataAdapter);
		zetZoeken();
		setScroll();
	}

	private void zetZoeken() {
		final EditText filterText = (EditText) getView().findViewById(R.id.search_box);
		if (filterText != null) {
			if (zoeken) {
				((ColoredArrayAdapter<?>) listView.getAdapter()).getFilter().filter(filterText.getText());
				filterText.setVisibility(View.VISIBLE);
				filterText.requestFocus();
			} else {
				filterText.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onStop() {
		Log.d(this.getClass().getName(), "onStop");
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		Log.d(this.getClass().getName(), "onDestroyView");
		if (filterText != null) {
			filterText.removeTextChangedListener(filterTextWatcher);
		}
		if (listView != null) {
			listView.setAdapter(null);
		}
		viewActive = false;
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Log.d(this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDetach() {
		Log.d(this.getClass().getName(), "onDetach");
		super.onDetach();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.ticketlistmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.tlnieuw) {
			listener.onNewTicket();
			return true;
		} else if (itemId == R.id.tlrefresh) {
			doRefresh();
			return true;
		} else if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.helplistfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else if (itemId == R.id.tlfilter) {
			Log.d(this.getClass().getName(), "tlfilter filterList = " + filterList);
			listener.onFilterSelected(filterList);
			return true;
		} else if (itemId == R.id.tlsort) {
			Log.d(this.getClass().getName(), "tlsort sortList = " + sortList);
			listener.onSortSelected(sortList);
			return true;
		} else if (itemId == R.id.tlchangehost) {
			if (networkThread != null) {
				networkThread.interrupt();
			}
			listener.onChangeHost();
			return true;
		} else if (itemId == R.id.tlshare) {
			shareList();
			return true;
		} else if (itemId == R.id.tlzoek) {
			zoeken = !zoeken;
			filterText.setText("");
			zetZoeken();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.d(this.getClass().getName(), "onSaveInstanceState");
		// final int count = tickets.length;
		// Log.d(this.getClass().getName(), "count = " + count);
		savedState.putIntArray(TICKETLISTNAME, tickets);
		savedState.putSerializable(SORTLISTNAME, sortList);
		savedState.putSerializable(FILTERLISTNAME, filterList);
		savedState.putBoolean(ZOEKENNAME, zoeken);
		savedState.putString(ZOEKTEXTNAME, zoektext);
		if (listView != null) {
			scrollPosition = listView.getFirstVisiblePosition();
		}
		savedState.putInt(SCROLLPOSITIONNAME, scrollPosition);
	}

	@Override
	public void setHost(final String url, final String username, final String password, boolean sslHack) {
		Log.d(this.getClass().getName(), "setHost");
		super.setHost(url, username, password, sslHack);
		if (listView != null) {
			listView.setAdapter(null);
		}
		tickets = null;
		ticketList.clear();
	}

	private void loadTicketList() {
		Log.d(this.getClass().getName(), "loadTicketList");
		ticketList.clear();
		callBack.onComplete();
		if (_url != null) {
			loading = true;
			hs.setText(R.string.getlist);
			networkThread = new Thread() {
				@Override
				public void run() {
					try {
						String reqString = "";
						for (final FilterSpec fs : filterList) {
							if (fs != null) {
								if (reqString.length() > 0) {
									reqString += "&";
								}
								reqString += fs.toString();
							}
						}
						for (final SortSpec s : sortList) {
							if (s != null) {
								if (reqString.length() > 0) {
									reqString += "&";
								}
								reqString += s.toString();
							}
						}
						Log.d(this.getClass().getName(), "reqString = " + reqString);
						final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack);
						req.setCredentials(_username, _password);
						final String rs = reqString;
						try {
							if (reqString.length() == 0) {
								reqString = "max=0";
							}
							final JSONArray ticketlist = req.callJSONArray("ticket.query", reqString);
							Log.d(this.getClass().getName(), ticketlist.toString());
							try {
								final int count = ticketlist.length();
								tickets = new int[count];
								for (int i = 0; i < count; i++) {
									tickets[i] = ticketlist.getInt(i);
								}
								loadTickets();
								listener.getTicketModel();
							} catch (final Exception e) {
								e.printStackTrace();
							}
						} catch (final JSONRPCException e) {
							Log.i(this.getClass().getName(), e.toString());
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
									alertDialogBuilder.setTitle(R.string.connerr);
									alertDialogBuilder.setMessage("regString = " + rs + "\n" + e.getMessage()).setCancelable(false)
											.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int id) {
													listener.onChangeHost();
												}
											});
									final AlertDialog alertDialog = alertDialogBuilder.create();
									if (!context.isFinishing()) {
										alertDialog.show();
									}
								}
							});
						}
					} catch (final Exception e) {
						Log.i(this.getClass().getName(), e.toString());
					} finally {
						loading = false;
					}
				}
			};
			networkThread.start();
		}
	}

	private final onTicketCompleteListener callBack = new onTicketCompleteListener() {
		@Override
		public void onComplete() {
			if (filterText.getVisibility() == View.VISIBLE) {
				dataAdapter.getFilter().filter(filterText.getText());
			}
			if (viewActive && listView != null) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (listView != null) {
							dataAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		};
	};

	private JSONObject makeComplexCall(String id, String method, Object... params) throws JSONException {
		final JSONObject call = new JSONObject();
		call.put("method", method);
		call.put("id", id);
		final JSONArray args = new JSONArray();
		for (final Object o : params) {
			args.put(o);
		}
		call.put("params", args);
		return call;
	}

	private void buildCall(JSONArray mc, int ticknr) throws JSONException {
		mc.put(makeComplexCall(Ticket.TICKET_GET + "_" + ticknr, "ticket.get", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_CHANGE + "_" + ticknr, "ticket.changeLog", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
	}

	private void loadTickets() {
		Log.d(this.getClass().getName(), "loadTicket " + tickets.length);
		final Thread networkThread = new Thread() {
			@Override
			public void run() {
				final Map<Integer, Ticket> ticketMap = new TreeMap<Integer, Ticket>();
				final int count = tickets.length;
				final int progress[] = new int[2];
				progress[0] = 0;
				progress[1] = count;
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < count; i++) {
							final Ticket t = new Ticket(tickets[i], null, null, null, null);
							ticketList.add(t);
							ticketMap.put(tickets[i], t);
						}
						hs.setText(R.string.ophalen);
					}
				});
				callBack.onComplete();

				Log.d(this.getClass().getName(), "loadTicket JSONRPCHttpClient " + _url + " " + _sslHack);
				final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack);
				req.setCredentials(_username, _password);
				for (int j = 0; j < count; j += ticketGroupCount) {
					final JSONArray mc = new JSONArray();
					for (int i = j; i < (j + ticketGroupCount < count ? j + ticketGroupCount : count); i++) {
						try {
							buildCall(mc, tickets[i]);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
					// Log.d(this.getClass().getName(), "loadTickets mc = " + mc);
					try {
						final JSONArray mcresult = req.callJSONArray("system.multicall", mc);
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								for (int i = 0; i < mcresult.length(); i++) {
									try {
										final JSONObject res = mcresult.getJSONObject(i);
										final String id = res.getString("id");
										final JSONArray result = res.getJSONArray("result");
										final int startpos = id.indexOf("_") + 1;
										final int thisTicket = Integer.parseInt(id.substring(startpos));
										final Ticket t = ticketMap.get(thisTicket);
										if (id.equals(Ticket.TICKET_GET + "_" + thisTicket)) {
											final JSONObject v = result.getJSONObject(3);
											t.setFields(v);
											progress[0]++;
										} else if (id.equals(Ticket.TICKET_CHANGE + "_" + thisTicket)) {
											final JSONArray h = result;
											t.setHistory(h);
										} else if (id.equals(Ticket.TICKET_ATTACH + "_" + thisTicket)) {
											final JSONArray at = result;
											t.setAttachments(at);
										} else if (id.equals(Ticket.TICKET_ACTION + "_" + thisTicket)) {
											final JSONArray ac = result;
											t.setActions(ac);
										} else {
											Log.i(this.getClass().getName(), "loadTickets, onverwachte respons = " + result);
										}
									} catch (final Exception e1) {
										e1.printStackTrace();
									}
								}
								hs.setText(progress[0] + "/" + progress[1]);
								callBack.onComplete();
							}
						});
					} catch (final Exception e) {
						e.printStackTrace();
					}
					callBack.onComplete();
				}
			}
		};
		networkThread.start();
	}

	private void doRefresh() {
		if (networkThread != null) {
			networkThread.interrupt();
			loading = false;
		}
		hs.setText(null);
		scrollPosition = 0;
		loadTicketList();
	}

	public void forceRefresh() {
		if (networkThread != null) {
			networkThread.interrupt();
			loading = false;
		}
		refreshOnRestart = true;
		scrollPosition = 0;
	}

	private void shareList() {
		String lijst = "";

		for (int i = 0; i < ticketList.size(); i++) {
			final Ticket t = ticketList.get(i);
			try {
				lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
			} catch (final JSONException e) {
				e.printStackTrace();
			}
		}
		final Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, lijst);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	public void setFilter(ArrayList<FilterSpec> filter) {
		Log.d(this.getClass().getName(), "setFilter " + filter);
		filterList = filter;
		tickets = null;
		ticketList.clear();
	}

	public void setSort(ArrayList<SortSpec> sort) {
		Log.d(this.getClass().getName(), "setSort " + sort);
		sortList = sort;
		tickets = null;
		ticketList.clear();
	}

	private final TextWatcher filterTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			dataAdapter.getFilter().filter(s);
			zoektext = s.toString();
		}
	};
}