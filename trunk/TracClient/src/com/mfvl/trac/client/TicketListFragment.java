package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import com.mfvl.trac.client.util.tcLog;
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

import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.ColoredArrayAdapter;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.SortSpec;

public class TicketListFragment extends TracClientFragment {

	private interface onLoadListListener {
		void onComplete();
	}
	
	private class TicketLoadException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3033107270657734325L;

		public TicketLoadException(String e){
			super(e);
		}
	}

	private static final String TICKETLISTNAME = "ticketlist";
	private static final String SORTLISTNAME = "sortlist";
	private static final String FILTERLISTNAME = "filterlist";
	private static final String ZOEKENNAME = "zoeken";
	private static final String ZOEKTEXTNAME = "filtertext";
	private static final String SCROLLPOSITIONNAME = "scrollPosition";

	private Thread loadListThread = null;
	private Thread loadContentThread = null;
	private ColoredArrayAdapter<Ticket> dataAdapter = null;
	private ListView listView = null;
	private final static int ticketGroupCount = 50;
	private EditText filterText = null;
	private TextView hs = null;
	private int tickets[] = null;
	private boolean zoeken = false;
	private String zoektext = "";
	private ArrayList<SortSpec> sortList = null;
	private ArrayList<FilterSpec> filterList = null;
	private int scrollPosition = 0;
	private boolean refreshOnRestart = false;
	private boolean debug = false;

	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			tickets = savedInstanceState.getIntArray(TICKETLISTNAME);
			sortList = (ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME);
			filterList = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME);
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
		if (dataAdapter == null) {
			dataAdapter = new ColoredArrayAdapter<Ticket>(context, R.layout.ticket_list);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.list_view, container, false);
		listView = (ListView) view.findViewById(R.id.listOfTickets);
		registerForContextMenu(listView);

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
		hs = (TextView) view.findViewById(R.id.listProgress);
		listView.setAdapter(dataAdapter);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		final boolean saveRefreshOnRestart = refreshOnRestart;
		listener.initializeList();
		refreshOnRestart = saveRefreshOnRestart;
		tcLog.d(this.getClass().getName(), "onActivityCreated refreshOnRestart = " + refreshOnRestart);
		tcLog.d(this.getClass().getName(), "onActivityCreated getCount = " + dataAdapter.getCount());
		if (savedInstanceState != null) {
			zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
			zoektext = savedInstanceState.getString(ZOEKTEXTNAME);
			scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME);
		}
		tcLog.d(this.getClass().getName(), "onActivityCreated zoeken = " + zoeken);
		if (zoeken) {
			filterText.setVisibility(View.VISIBLE);
			filterText.setText(zoektext);
			filterText.addTextChangedListener(filterTextWatcher);
		} else {
			filterText.setVisibility(View.GONE);
			if (filterText.isFocused()) {
				filterText.clearFocus();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		tcLog.d(this.getClass().getName(), "onResume");
		if (refreshOnRestart) {
			tcLog.d(this.getClass().getName(), "onResume refresh");
			killThreads();
			scrollPosition = 0;
			clearTickets();
			refreshOnRestart = false;
		}

		tcLog.d(this.getClass().getName(), "onResume getCount = " + dataAdapter.getCount());
		tcLog.d(this.getClass().getName(), "onResume tickets = " + tickets);
		tcLog.d(this.getClass().getName(), "onResume loadListThread = " + loadListThread + " "
				+ (loadListThread != null && loadListThread.isAlive()));
		tcLog.d(this.getClass().getName(), "onResume loadContentThread = " + loadContentThread + " "
				+ (loadContentThread != null && loadContentThread.isAlive()));

		if (loadListThread == null || !loadListThread.isAlive() || loadListThread.isInterrupted()) {
			if (tickets == null || tickets.length == 0) {
				tcLog.d(this.getClass().getName(), "onResume tickets = empty");
				killLoadContentThread();
				hs.setText("");
				loadTicketList(new onLoadListListener() {
					@Override
					public void onComplete() {
						loadTicketContent();
					}
				});
			} else if (dataAdapter.getCount() == 0
					&& (loadContentThread == null || !loadContentThread.isAlive() || loadContentThread.isInterrupted())) {
				tcLog.d(this.getClass().getName(), "onResume geen content");
				loadTicketContent();
			} else {
				dataAdapter.notifyDataSetChanged();
			}
		}
		tcLog.d(this.getClass().getName(), "on Resume voor voor zetzoeken");
		if (loadListThread != null && loadContentThread != null && !loadListThread.isAlive() && !loadContentThread.isAlive()) {
			tcLog.d(this.getClass().getName(), "on Resume voor zetzoeken");
			zetZoeken();
			setScroll();
		}
		listView.invalidate();
	}

	@Override
	public void onPause() {
		tcLog.d(this.getClass().getName(), "onPause");
		super.onPause();
		scrollPosition = listView.getFirstVisiblePosition();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		tcLog.d(this.getClass().getName(), "onCreateContextMenu");
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.listOfTickets) {
			final MenuInflater inflater = context.getMenuInflater();
			inflater.inflate(R.menu.listcontextmenu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onContextItemSelected");
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

	private void setScroll() {
		if (listView != null) {
			listView.setSelection((scrollPosition == 0 ? 0 : scrollPosition + 1));
		}
	}

	private void zetZoeken() {
		tcLog.d(this.getClass().getName(), "zetZoeken");
		final View v = getView();
		if (v != null) {
			final EditText filterText = (EditText) v.findViewById(R.id.search_box);
			if (filterText != null && listView != null) {
				if (zoeken) {
					dataAdapter.getFilter().filter(filterText.getText());
					filterText.setVisibility(View.VISIBLE);
					filterText.requestFocus();
				} else {
					filterText.setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public void onDestroyView() {
		tcLog.d(this.getClass().getName(), "onDestroyView");
		if (filterText != null && zoeken) {
			filterText.removeTextChangedListener(filterTextWatcher);
		}
		if (listView != null) {
			listView.invalidateViews();
			listView.setAdapter(null);
			unregisterForContextMenu(listView);
			listView = null;
		}
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		tcLog.d(this.getClass().getName(), "onDestroy");
		killThreads();
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.ticketlistmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.tlnieuw) {
			listener.onNewTicket();
		} else if (itemId == R.id.tlrefresh) {
			tcLog.d(this.getClass().getName(), "tlrefresh");
			doRefresh();
		} else if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.helplistfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
		} else if (itemId == R.id.tlselect) {
			tcLog.d(this.getClass().getName(), "tlselect");
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
					final ProgressDialog pb = startProgressBar(R.string.downloading);
					new Ticket(ticknr, context, new onTicketCompleteListener() {

						@Override
						public void onComplete(Ticket t2) {
							pb.dismiss();
							if (t2.hasdata()) {
								listener.onTicketSelected(t2);
							} else {
								context.runOnUiThread(new Runnable() {

									@Override
									public void run() {
										final AlertDialog.Builder noTicketDialogBuilder = new AlertDialog.Builder(context);
										noTicketDialogBuilder.setTitle(R.string.notfound);
										noTicketDialogBuilder.setMessage(R.string.ticketnotfound);
										noTicketDialogBuilder.setCancelable(false);
										noTicketDialogBuilder.setPositiveButton(R.string.oktext, null);
										final AlertDialog noTicketDialog = noTicketDialogBuilder.create();
										noTicketDialog.show();
									}
								});
							}
						}

					});

				}
			});
			alertDialogBuilder.setNegativeButton(R.string.cancel, null);
			final AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		} else if (itemId == R.id.tlfilter) {
			tcLog.d(this.getClass().getName(), "tlfilter filterList = " + filterList);
			listener.onFilterSelected(filterList);
		} else if (itemId == R.id.tlsort) {
			tcLog.d(this.getClass().getName(), "tlsort sortList = " + sortList);
			listener.onSortSelected(sortList);
		} else if (itemId == R.id.tlchangehost) {
			killThreads();
			listener.onChangeHost();
			return true;
		} else if (itemId == R.id.tlshare) {
			shareList();
		} else if (itemId == R.id.tldebug) {
			shareDebug();
		} else if (itemId == R.id.tlzoek) {
			zoeken = !zoeken;
			if (zoeken) {
				filterText.addTextChangedListener(filterTextWatcher);
				filterText.setVisibility(View.VISIBLE);
			} else {
				filterText.removeTextChangedListener(filterTextWatcher);
				filterText.setVisibility(View.GONE);
				if (filterText.isFocused()) {
					filterText.clearFocus();
				}
				zoektext = null;
			}
			filterText.setText(null);
			if (loadListThread != null && loadContentThread != null && !loadListThread.isAlive() && !loadContentThread.isAlive()) {
				zetZoeken();
			}
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState");
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

	private void loadTicketList(final onLoadListListener oc) {
		tcLog.d(this.getClass().getName(), "loadTicketList url=" + _url + " " + dataAdapter.getCount());
		clearTickets();
		if (_url != null) {
			final ProgressDialog pb = startProgressBar(R.string.getlist);
			loadListThread = new Thread() {
				@Override
				public void run() {
					try {
						tcLog.d(this.getClass().getName(), "loadTicketList in loadListThread");
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								hs.setText(R.string.getlist);								
							}
						});
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
						tcLog.d(this.getClass().getName(), "reqString = " + reqString);
						final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack);
						req.setCredentials(_username, _password);
						final String rs = reqString;
						try {
							if (reqString.length() == 0) {
								reqString = "max=0";
							}
							final JSONArray jsonTicketlist = req.callJSONArray("ticket.query", reqString);
							if (isInterrupted()) {
								tcLog.d(this.getClass().getName(), "loadTicketList interrupt detected");
								throw  new TicketLoadException("Interrupted");
							}
							tcLog.d(this.getClass().getName(), jsonTicketlist.toString());
							final int count = jsonTicketlist.length();
							tickets = new int[count];
							for (int i = 0; i < count; i++) {
								tickets[i] = jsonTicketlist.getInt(i);
							}
							tcLog.d(this.getClass().getName(), "loadTicketList ticketlist loaded");
							oc.onComplete();
							listener.getTicketModel();
						} catch (JSONException e) {
							tcLog.d(this.getClass().getName(),e.getMessage());
							tcLog.d(this.getClass().getName(),tcLog.getStackTraceString(e));							
						} catch (final JSONRPCException e) {
							tcLog.d(this.getClass().getName(), e.toString());
							tcLog.d(this.getClass().getName(),tcLog.getStackTraceString(e));							
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
					} catch (TicketLoadException e) {
						tcLog.d(this.getClass().getName(), "loadTicketList interrupted",e);
						clearTickets();
					} finally {
						pb.dismiss();
						tcLog.d(this.getClass().getName(), "loadTicketList ended");
					}
				}
			};
			loadListThread.start();
		}
	}
	
	private void clearTicketsAct() {
		dataAdapter.clear();
		tickets=null;
	}
	
	private void clearTickets() {
		if (Looper.getMainLooper().equals(Looper.myLooper())) {
			clearTicketsAct();
		} else {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					clearTicketsAct();
				}
			});
		}
	}

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
		mc.put(makeComplexCall(Ticket.TICKET_CHANGE + "_" + ticknr, "ticket.changetcLog.d", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
	}

	private void loadTicketContent() {
		tcLog.d(this.getClass().getName(), "loadTicketContent " + tickets.length);
		
		loadContentThread = new Thread() {
			@Override
			public void run() {
				try {
					tcLog.d(this.getClass().getName(), "loadTicketContent thread = " + this);
					final Map<Integer, Ticket> ticketMap = new TreeMap<Integer, Ticket>();
					final int count = tickets.length;
					final int progress[] = new int[2];
					progress[0] = 0;
					progress[1] = count;
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!isInterrupted()) {
								hs.setText(R.string.ophalen);
								for (int i = 0; i < count; i++) {
									final Ticket t = new Ticket(tickets[i], null, null, null, null);
									dataAdapter.add(t);
									ticketMap.put(tickets[i], t);
								}
							}
						}
					});
					
					if (isInterrupted()) {
						throw new TicketLoadException("loadTicketContent interrupt1 detected");
					}

					tcLog.d(this.getClass().getName(), "loadTicketContent JSONRPCHttpClient " + _url + " " + _sslHack);
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
						// tcLog.d(this.getClass().getName(), "loadTickets mc = "
						// + mc);
						
						if (isInterrupted()) {
							throw new TicketLoadException("loadTicketContent interrupt2 detected");
						}
						
						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								hs.setText(progress[0] + "/" + progress[1]);
								dataAdapter.notifyDataSetChanged();
							}
						});

						try {
							final JSONArray mcresult = req.callJSONArray("system.multicall", mc);
							for (int i = 0; i < mcresult.length(); i++) {
								try {
									final JSONObject res = mcresult.getJSONObject(i);
									final String id = res.getString("id");
									final JSONArray result = res.getJSONArray("result");
									final int startpos = id.indexOf("_") + 1;
									final int thisTicket = Integer.parseInt(id.substring(startpos));
									final Ticket t = ticketMap.get(thisTicket);
									if (t != null) {
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
											tcLog.d(this.getClass().getName(), "loadTickets, onverwachte respons = " + result);
										}
									}
								} catch (final Exception e1) {
									e1.printStackTrace();
								}
							}
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									hs.setText(progress[0] + "/" + progress[1]);
									dataAdapter.notifyDataSetChanged();
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();
						}
						tcLog.d(this.getClass().getName(), "loadTicketContent loop "+progress[0]);			
					}
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tcLog.d(this.getClass().getName(), "loadTicketContent invalidate views");
							dataAdapter.notifyDataSetChanged();
							if (listView != null) {
								listView.invalidateViews();
							}
							setScroll();
							zetZoeken();
						}
					});
				} catch (final TicketLoadException e) {
					tcLog.d(this.getClass().getName(), "loadContentThread interrupted");
					clearTickets();
				} finally {
					tcLog.d(this.getClass().getName(), "loadTicketContent ended");
				}
			}
		};
		loadContentThread.start();
	}

	private void killThreads() {
		killLoadListThread();
		killLoadContentThread();
	}

	private void killLoadContentThread() {
		if (loadContentThread != null && loadContentThread.isAlive()) {
			tcLog.d(this.getClass().getName(), "killLoadContentThread");
			loadContentThread.interrupt();
		}
	}

	private void killLoadListThread() {
		if (loadListThread != null && loadListThread.isAlive()) {
			tcLog.d(this.getClass().getName(), "killLoadListThread");
			loadListThread.interrupt();
		}
	}

	private void doRefresh() {
		tcLog.d(this.getClass().getName(), "doRefresh");
		killThreads();
		hs.setText("");
		scrollPosition = 0;
		clearTickets();
		loadTicketList(new onLoadListListener() {
			@Override
			public void onComplete() {
				tcLog.d(this.getClass().getName(), "doRefresh - onComplete");
				loadTicketContent();
			}
		});
	}

	public void forceRefresh() {
		tcLog.d(this.getClass().getName(), "forceRefresh");
		refreshOnRestart = true;
		scrollPosition = 0;
	}

	private void shareList() {
		String lijst = "";

		for (int i = 0; i < dataAdapter.getCount(); i++) {
			final Ticket t = dataAdapter.getItem(i);
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
	
	private void shareDebug() {
		final Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, tcLog.getDebug());
		sendIntent.setType("text/plain");
		startActivity(sendIntent);

	}

	public void setFilter(ArrayList<FilterSpec> filter) {
		tcLog.d(this.getClass().getName(), "setFilter " + filter);
		filterList = filter;
	}

	public void setSort(ArrayList<SortSpec> sort) {
		tcLog.d(this.getClass().getName(), "setSort " + sort);
		sortList = sort;
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		tcLog.d(this.getClass().getName(), "onPrepareOptionsMenu");
		MenuItem item= menu.findItem(R.id.tldebug);
		item.setVisible(debug);
		item.setEnabled(debug);
		super.onPrepareOptionsMenu(menu);
	}	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void enableDebug() {
		debug = true;
		tcLog.toast("Debug enabled");
		if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			context.invalidateOptionsMenu();
		}
	}

	@Override
	public void setHost(final String url, final String username, final String password, boolean sslHack) {
		tcLog.d(this.getClass().getName(), "setHost");
		super.setHost(url, username, password, sslHack);
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