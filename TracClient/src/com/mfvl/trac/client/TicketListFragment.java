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

import org.alexd.jsonrpc.JSONRPCException;
import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mfvl.trac.client.util.ColoredArrayAdapter;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.SortSpec;
import com.mfvl.trac.client.util.tcLog;

public class TicketListFragment extends TracClientFragment implements OnItemClickListener, OnScrollListener {

	private static final int CMPL_SUCCESS = 1;
	private static final int CMPL_NOTICKETS = 0;
	private static final int CMPL_EXCEPTION = -1;

	private interface onCompleteListener {
		void onComplete(final int errorcode);
	}

	private class TicketLoadException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 7847222445437027235L;

		public TicketLoadException(String s) {
			super(s);
		}

		public TicketLoadException(String s, Throwable e) {
			super(s, e);
		}
	}

	private HandlerThread mHandlerThread = null;
	private TicketListHandler mTicketListHandler;
	private Messenger mMessenger = null;

	private static final int MSG_INIT = 1;
	private static final int MSG_QUIT = 2;
	private static final int MSG_LOADLIST = 3;
	private static final int MSG_LOADCONT = 4;
	private static final int MSG_KILLLIST = 5;
	private static final int MSG_KILLCONT = 6;
	private static final int MSG_RELOAD = 7;
	private static final int MSG_REFRESH = 8;
	private static final int MSG_CLEARTICK = 9;
	private static final int MSG_KILLANDLOADLIST = 10;
	
	private static final String HandlerName = "TicketListHandler";

	private final class TicketListHandler extends Handler {
		Boolean loadListLock = false;
		Boolean loadContLock = false;

		public TicketListHandler(Looper looper) {
			super(looper);
			tcLog.d(this.getClass().getName(), HandlerName);
		}

		public void mySendEmptyMessage(int message) {
			tcLog.d(this.getClass().getName(), "mySendEmptyMessage message = " + message);
			sendEmptyMessage(message);
		}

		public void myRemoveMessages(int message) {
			tcLog.d(this.getClass().getName(), "myRemoveMessages message = " + message);
			removeMessages(message);
		}

		@Override
		public void handleMessage(final Message msg) {

//			tcLog.d(this.getClass().getName(), "handleMessage msg = " + msg);
			switch (msg.what) {
			case MSG_INIT:
				break;
			case MSG_QUIT:
				myRemoveMessages(MSG_RELOAD);
				myRemoveMessages(MSG_REFRESH);
				mySendEmptyMessage(MSG_KILLLIST);
				mySendEmptyMessage(MSG_KILLCONT);
				break;
			case MSG_LOADLIST:
				if (!loadListLock && !loadContLock) {
					synchronized (loadListLock) {
						loadListLock = true;
						clearTickets();
						post(new Runnable() {
							@Override
							public void run() {
								loadTicketList(new onCompleteListener() {
									@Override
									public void onComplete(final int error) {
										loadListLock = false;
										if (error == CMPL_SUCCESS) {
											mySendEmptyMessage(MSG_LOADCONT);
										}
									}
								});
							}
						});
					}
//				} else {
//					tcLog.d(getClass().getName(), "MSG_LOADLIST called while active " + loadListLock + " " + loadContLock);
				}
				break;
			case MSG_LOADCONT:
				if (!loadListLock && !loadContLock) {
					synchronized (loadListLock) {
						loadContLock = true;
						post(new Runnable() {
							@Override
							public void run() {
								loadTicketContent(new onCompleteListener() {
									@Override
									public void onComplete(final int error) {
										loadContLock = false;
									}
								});
							}
						});
					}
//				} else {
//					tcLog.d(getClass().getName(), "MSG_LOADCONT called while active " + loadListLock + " " + loadContLock);
				}
				break;
			case MSG_KILLLIST:
				myRemoveMessages(MSG_LOADLIST);
				if (loadListThread != null && loadListThread.isAlive()) {
//					tcLog.d(this.getClass().getName(), "killLoadListThread");
					loadListThread.interrupt();
				}
				break;
			case MSG_KILLANDLOADLIST:
				myRemoveMessages(MSG_LOADLIST);
				if (loadListThread != null && loadListThread.isAlive()) {
//					tcLog.d(this.getClass().getName(), "killLoadListThread");
					loadListThread.interrupt();
				}
				mySendEmptyMessage(MSG_LOADLIST);
				break;
			case MSG_KILLCONT:
				myRemoveMessages(MSG_LOADCONT);
				if (loadContentThread != null && loadContentThread.isAlive()) {
//					tcLog.d(this.getClass().getName(), "killLoadContentThread");
					loadContentThread.interrupt();
				}
				break;
			case MSG_RELOAD:
				myRemoveMessages(MSG_KILLLIST);
				myRemoveMessages(MSG_KILLANDLOADLIST);
				myRemoveMessages(MSG_KILLCONT);
				mySendEmptyMessage(MSG_KILLANDLOADLIST);
				mySendEmptyMessage(MSG_KILLCONT);
				break;
			case MSG_REFRESH:
				if (listView != null) {
					listView.invalidate();
				}
				break;
			case MSG_CLEARTICK:
				clearTickets();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private static final String TICKETLISTNAME = "ticketlistInt";
	private static final String SORTLISTNAME = "sortlist";
	private static final String FILTERLISTNAME = "filterlist";
	private static final String ZOEKENNAME = "zoeken";
	private static final String ZOEKTEXTNAME = "filtertext";
	private static final String SCROLLPOSITIONNAME = "scrollPosition";

	private final Thread loadListThread = null;
	private final Thread loadContentThread = null;
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
	private ArrayList<Ticket> ticketList = null;
	private int scrollPosition = 0;
	private boolean scrolling = false;
	private boolean hasScrolled = false;
	private boolean refreshOnRestart = false;
	private String SelectedProfile = null;
	private String filterArg = null;
	private String sortArg = null;

	private ShareActionProvider mShareActionProvider;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		resetCache();
		final Bundle args = this.getArguments();
		if (args != null) {
			filterArg = args.getString(Const.CURRENT_FILTER);
			sortArg = args.getString(Const.CURRENT_SORTORDER);
		}
		if (args != null) {
			SelectedProfile = args.getString(Const.CURRENT_PROFILE);
			if (args.containsKey("TicketArg")) {
				selectTicket(args.getInt("TicketArg"));
			}
		}
	}

	private void newDataAdapter() {
		ticketList = new ArrayList<Ticket>();
		dataAdapter = new ColoredArrayAdapter<Ticket>(context, R.layout.ticket_list, ticketList);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
		newDataAdapter();
		mHandlerThread = new HandlerThread(HandlerName, Process.THREAD_PRIORITY_DEFAULT);
		mHandlerThread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mTicketListHandler = new TicketListHandler(mHandlerThread.getLooper());
		mMessenger = new Messenger(mTicketListHandler);
		sendMessage(MSG_INIT);
	}

	private void sendMessage(int message) {
//		tcLog.d(this.getClass().getName(), "sendMessage message = " + message);
		if (mMessenger != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message;
				msg.arg1 = -1;
				msg.arg2 = -1;
				msg.replyTo = null;
				tcLog.d(this.getClass().getName(), "sendMessage msg = " + msg);
				mMessenger.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(this.getClass().getName(), "sendMessage failed", e);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = "	+ savedInstanceState);
		final View view = inflater.inflate(R.layout.list_view, container, false);
		listView = (ListView) view.findViewById(R.id.listOfTickets);
		registerForContextMenu(listView);
		scrolling = false;
		hasScrolled = false;
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(this);
		filterText = (EditText) view.findViewById(R.id.search_box);
		hs = (TextView) view.findViewById(R.id.listProgress);
		listView.setAdapter(dataAdapter);
		return view;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (scrolling || hasScrolled) {
			scrollPosition = firstVisibleItem;
			// tcLog.d(getClass().getName(),"onScroll scrollPosition <= "+scrollPosition);
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			if (scrolling) {
				hasScrolled = true;
				scrolling = false;
			}
		} else {
			scrolling = true;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (parent.getId()) {
		case R.id.listOfTickets:
			final Ticket t = (Ticket) ((ListView) parent).getItemAtPosition(position);
			if (t.hasdata()) {
				listener.onTicketSelected(t);
			} else {
				final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle(R.string.nodata);
				alertDialogBuilder.setMessage(R.string.nodatadesc).setCancelable(false).setPositiveButton(R.string.oktext, null);
				final AlertDialog alertDialog = alertDialogBuilder.create();
				if (!context.isFinishing()) {
					alertDialog.show();
				}
			}
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated savedInstanceState = "+ savedInstanceState);
		final boolean saveRefreshOnRestart = refreshOnRestart;
		// listener.initializeList();
		refreshOnRestart = saveRefreshOnRestart;
		if (filterArg != null) {
			listener.setFilter(filterArg);
			filterArg = null;
		}
		if (sortArg != null) {
			listener.setSort(sortArg);
			sortArg = null;
		}
		if (savedInstanceState != null) {
			tickets = savedInstanceState.getIntArray(TICKETLISTNAME);
			listener.setSort((ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME));
			listener.setFilter((ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME));
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
		if (savedInstanceState != null) {
			zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
			zoektext = savedInstanceState.getString(ZOEKTEXTNAME);
			scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME);
			// tcLog.d(getClass().getName(),"onActivityCreated scrollPosition <= "+scrollPosition);
		}
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
			sendMessage(MSG_RELOAD);
			scrollPosition = 0;
			// tcLog.d(getClass().getName(),"onResume scrollPosition <= "+scrollPosition);
			refreshOnRestart = false;
		}

//		tcLog.d(this.getClass().getName(), "onResume getCount = " + dataAdapter.getCount());
//		tcLog.d(this.getClass().getName(), "onResume tickets = " + tickets);
//		tcLog.d(this.getClass().getName(), "onResume loadListThread = " + loadListThread + " "
//				+ (loadListThread != null && loadListThread.isAlive()));
//		tcLog.d(this.getClass().getName(), "onResume loadContentThread = " + loadContentThread + " "
//				+ (loadContentThread != null && loadContentThread.isAlive()));

		if (loadListThread == null || !loadListThread.isAlive() || loadListThread.isInterrupted()) {
			if (tickets == null || tickets.length == 0) {
				killLoadContentThread();
				hs.setText("");
				sendMessage(MSG_LOADLIST);
			} else if (dataAdapter.getCount() == 0
					&& (loadContentThread == null || !loadContentThread.isAlive() || loadContentThread.isInterrupted())) {
				sendMessage(MSG_LOADCONT);
			} else {
				dataAdapter.notifyDataSetChanged();
			}
		}
		if (loadListThread != null && loadContentThread != null && !loadListThread.isAlive() && !loadContentThread.isAlive()) {
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
		// tcLog.d(getClass().getName(),"onPause scrollPosition <= "+scrollPosition);
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
		if (t.hasdata()) {
			switch (item.getItemId()) {
			case R.id.select:
				listener.onTicketSelected(t);
				return true;
			case R.id.dfupdate:
				listener.onUpdateTicket(t);
				return true;
			case R.id.dfshare:
				final Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, t.toText());
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
				return true;
			default:
			}
		}
		return super.onContextItemSelected(item);
	}

	private void setScroll() {
		if (listView != null) {
			listView.setSelection(scrollPosition);
			// listView.setSelection(scrollPosition == 0 ? 0 : scrollPosition +
			// 1);
			// tcLog.d(getClass().getName(),"setScroll scrollPosition = "+scrollPosition);
		}
	}

	private void zetZoeken() {
		tcLog.d(this.getClass().getName(), "zetZoeken");
		final View v = getView();
		if (v != null) {
			final EditText filterText = (EditText) v.findViewById(R.id.search_box);
			if (filterText != null && listView != null && dataAdapter != null) {
				if (zoeken) {
					dataAdapter.getFilter().filter(filterText.getText());
					filterText.setVisibility(View.VISIBLE);
					filterText.requestFocus();
				} else {
					filterText.setVisibility(View.GONE);
					dataAdapter.getFilter().filter(null);
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
		sendMessage(MSG_QUIT);
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.ticketlistmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
		MenuItem shareItem = menu.findItem(R.id.tlshare);
		mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
		mShareActionProvider.setShareIntent(shareList());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
		final int itemId = item.getItemId();
		if (itemId == R.id.tlnieuw) {
			listener.onNewTicket();
		} else if (itemId == R.id.tlrefresh) {
//			tcLog.d(this.getClass().getName(), "tlrefresh");
			sendMessage(MSG_RELOAD);
			doRefresh();
		} else if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.helplistfile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, false);
			startActivity(launchTrac);
		} else if (itemId == R.id.tlselect) {
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
		} else if (itemId == R.id.tlfilter) {
			listener.onFilterSelected(filterList);
		} else if (itemId == R.id.tlsort) {
			listener.onSortSelected(sortList);
		} else if (itemId == R.id.tlchangehost) {
			killThreads();
			listener.onChangeHost();
//		} else if (itemId == R.id.tlshare) {
//			shareList();
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
				dataAdapter.getFilter().filter(null);
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
			// tcLog.d(getClass().getName(),"onSaveInstanceState scrollPosition <= "+scrollPosition);
		}
		savedState.putInt(SCROLLPOSITIONNAME, scrollPosition);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState = " + savedState);
	}

	private void loadTicketList(final onCompleteListener oc) {
		tcLog.d(this.getClass().getName(), "loadTicketList url=" + _url + " " + dataAdapter.getCount());
		if (_url != null) {
			final ProgressDialog pb = startProgressBar(context.getString(R.string.getlist)
					+ (SelectedProfile == null ? "" : "\n" + SelectedProfile));
			new Thread() {
				@Override
				public void run() {
					final String logTag = this.getClass().getName() + "." + this.getId();
					try {
						tcLog.d(logTag, "loadTicketList in loadListThread ");
						listener.setReferenceTime();
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
						tcLog.d(logTag, "reqString = " + reqString);
						final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack, _sslHostNameHack);
						req.setCredentials(_username, _password);
						final String rs = reqString;
						try {
							if (reqString.length() == 0) {
								reqString = "max=0";
							}
							final JSONArray jsonTicketlist = req.callJSONArray("ticket.query", reqString);
							if (isInterrupted()) {
								tcLog.d(logTag, "loadTicketList interrupt detected");
								throw new TicketLoadException("Interrupted");
							}
							tcLog.d(logTag, jsonTicketlist.toString());
							final int count = jsonTicketlist.length();
							final TicketModel tm = listener.getTicketModel();
							if (count > 0) {
								tickets = new int[count];
								if (tickets != null) {
									for (int i = 0; i < count; i++) {
										tickets[i] = jsonTicketlist.getInt(i);
									}
									tcLog.d(logTag, "loadTicketList ticketlist loaded");
									oc.onComplete(CMPL_SUCCESS);
								} else {
									tcLog.d(logTag, "loadTicketList ticketlist loaded");
									oc.onComplete(CMPL_EXCEPTION);									
								}
							} else {
								tcLog.d(logTag, "loadTicketList Ticket Query returned 0 tickets");
								oc.onComplete(CMPL_NOTICKETS);

								final int titleString = R.string.warning;
								final int messString = tm.count() > 0 ? R.string.notickets : R.string.nopermission;
								context.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
										alertDialogBuilder.setTitle(titleString);
										alertDialogBuilder.setMessage(messString).setCancelable(false)
										.setPositiveButton(R.string.oktext, null);
										final AlertDialog alertDialog = alertDialogBuilder.create();
										if (!context.isFinishing()) {
											alertDialog.show();
										}
									}
								});
							}
						} catch (final JSONException e) {
							throw new TicketLoadException("loadTicketList JSONException thrown during ticketquery", e);
						} catch (final JSONRPCException e) {
							tcLog.d(logTag, "loadTicketList JSONException thrown during ticketquery", e);
							oc.onComplete(CMPL_EXCEPTION);
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
					} catch (final TicketLoadException e) {
						tcLog.d(logTag, "loadTicketList interrupted");
						sendMessage(MSG_CLEARTICK);
						oc.onComplete(CMPL_EXCEPTION);
					} finally {
						if (pb != null && !context.isFinishing()) {
							pb.dismiss();
						}
						tcLog.d(logTag, "loadTicketList ended");
						if (listView != null) {
							listView.postInvalidate();
						}
					}
				}
			}.start();
		}
	}

	private void _clearTickets() {
		tickets = null;
		listener.resetCache();
		if (listView != null) {
			zoeken = false;
			zoektext = null;
			ticketList.clear();
			// dataAdapter.clear();
			// dataAdapter.notifyDataSetChanged();
			newDataAdapter();
			listView.setAdapter(dataAdapter);
			// tcLog.d(getClass().getName(), "_clearTickets dataAdapter = " +
			// dataAdapter);
			// tcLog.d(getClass().getName(), "_clearTickets count = " +
			// dataAdapter.getCount());
			// tcLog.d(getClass().getName(), "_clearTickets listView = " +
			// listView);
			// tcLog.d(getClass().getName(), "_clearTickets listViewAdapter = "
			// + listView.getAdapter());
			// tcLog.d(getClass().getName(), "_clearTickets count = " +
			// listView.getAdapter().getCount());
		}
	}

	private void clearTickets() {
		if (Looper.getMainLooper().equals(Looper.myLooper())) {
			_clearTickets();
		} else {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					_clearTickets();
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
		mc.put(makeComplexCall(Ticket.TICKET_CHANGE + "_" + ticknr, "ticket.changeLog", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ATTACH + "_" + ticknr, "ticket.listAttachments", ticknr));
		mc.put(makeComplexCall(Ticket.TICKET_ACTION + "_" + ticknr, "ticket.getActions", ticknr));
	}

	private void loadTicketContent(final onCompleteListener oc) {
		tcLog.d(this.getClass().getName(), "loadTicketContent started ");
		new Thread() {
			@Override
			public void run() {
				final long tid = this.getId();
				final String logTag = this.getClass().getName() + "." + tid;
				try {
					tcLog.d(logTag, "loadTicketContent thread = " + this);
					final int count = tickets.length;
					final int progress[] = new int[2];
					progress[0] = 0;
					progress[1] = count;
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try {
								if (!isInterrupted()) {
									hs.setText(R.string.ophalen);
									if (tickets != null) {
										for (int i = 0; i < count; i++) {
											final Ticket t = new Ticket(tickets[i], null, null, null, null);
											dataAdapter.add(t);
											listener.putTicket(t);
										}
									}
									if (listView != null) {
										listView.invalidate();
									}
								}
							} catch (final Exception e) {
								tcLog.e(logTag, "loadTicketContent Exception thrown building ticketlist", e);
							}
						}
					});

					if (isInterrupted()) {
						throw new TicketLoadException("loadTicketContent interrupt1 detected");
					}

					tcLog.d(logTag, "loadTicketContent JSONRPCHttpClient " + _url + " " + _sslHack);
					final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack, _sslHostNameHack);
					req.setCredentials(_username, _password);
					for (int j = 0; j < count; j += ticketGroupCount) {
						final JSONArray mc = new JSONArray();
						for (int i = j; i < (j + ticketGroupCount < count ? j + ticketGroupCount : count); i++) {
							try {
								buildCall(mc, tickets[i]);
							} catch (final Exception e) {
								throw new TicketLoadException("loadTicketContent Exception during buildCall");
							}
						}
						// tcLog.d(logTag, "loadTickets mc = " + mc);

						if (isInterrupted()) {
							throw new TicketLoadException("loadTicketContent interrupt2 detected");
						}

						context.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								hs.setText(progress[0] + "/" + progress[1]);
								if (dataAdapter != null) {
									dataAdapter.notifyDataSetChanged();
								}
							}
						});

						try {
							final JSONArray mcresult = req.callJSONArray("system.multicall", mc);
							if (isInterrupted()) {
								throw new TicketLoadException("loadTicketContent interrupt3 detected");
							}
							// tcLog.d(logTag, "mcresult = " + mcresult);
							for (int i = 0; i < mcresult.length(); i++) {
								try {
									final JSONObject res = mcresult.getJSONObject(i);
									final String id = res.getString("id");
									final JSONArray result = res.getJSONArray("result");
									final int startpos = id.indexOf("_") + 1;
									final int thisTicket = Integer.parseInt(id.substring(startpos));
									final Ticket t = listener.getTicket(thisTicket);
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
											tcLog.d(logTag, "loadTickets, onverwachte respons = " + result);
										}
										// listener.putTicket(t);
									}
								} catch (final Exception e1) {
									throw new TicketLoadException(
											"loadTicketContent Exception thrown innerloop j=" + j + " i=" + i, e1);
								}
							}
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									hs.setText(progress[0] + "/" + progress[1]);
									if (dataAdapter != null) {
										dataAdapter.notifyDataSetChanged();
									}
								}
							});
						} catch (final TicketLoadException e) {
							throw new TicketLoadException("loadTicketContent TicketLoadException thrown outerloop j=" + j, e);
						} catch (final Exception e) {
							throw new TicketLoadException("loadTicketContent Exception thrown outerloop j=" + j, e);
						}
						tcLog.d(logTag, "loadTicketContent loop " + progress[0]);
					}
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tcLog.d(logTag, "loadTicketContent invalidate views");
							oc.onComplete(CMPL_SUCCESS);
							if (dataAdapter != null) {
								dataAdapter.notifyDataSetChanged();
							}
							if (listView != null) {
								listView.invalidateViews();
							}
							setScroll();
							zetZoeken();
						}
					});
				} catch (final TicketLoadException e) {
					tcLog.d(logTag, "loadContentThread interrupted");
					if (oc != null) {
						oc.onComplete(CMPL_EXCEPTION);
					}
					if (loadContentThread != null && loadContentThread.getId() == tid) {
						tcLog.d(logTag, "loadContentThread tickets cleared");
						sendMessage(MSG_CLEARTICK);
					}
				} catch (final Exception e) {
					tcLog.d(logTag, "loadContentThread exception", e);
					if (oc != null) {
						oc.onComplete(CMPL_EXCEPTION);
					}
					if (loadContentThread != null && loadContentThread.getId() == tid) {
						tcLog.d(logTag, "loadContentThread tickets cleared");
						sendMessage(MSG_CLEARTICK);
					}
				} finally {
					tcLog.d(logTag, "loadTicketContent ended");
					mShareActionProvider.setShareIntent(shareList());
				}
			}
		}.start();
	}

	private void killThreads() {
		killLoadListThread();
		killLoadContentThread();
	}

	private void killLoadContentThread() {
		sendMessage(MSG_KILLCONT);
	}

	private void killLoadListThread() {
		sendMessage(MSG_KILLLIST);
	}

	private void doRefresh() {
		killThreads();
		hs.setText("");
		scrollPosition = 0;
		// tcLog.d(getClass().getName(),"doRefresh scrollPosition <= "+scrollPosition);
		sendMessage(MSG_LOADLIST);
	}

	public void forceRefresh() {
		tcLog.d(this.getClass().getName(), "forceRefresh");
		refreshOnRestart = true;
	}

	private Intent shareList() {
		String lijst = "";

		for (int i = 0; i < dataAdapter.getCount(); i++) {
			final Ticket t = dataAdapter.getItem(i);
			try {
				lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
			} catch (final JSONException e) {
				tcLog.i(getClass().getName(), "shareList exception", e);
			}
		}
		final Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, lijst);
		sendIntent.setType("text/plain");
		return sendIntent;
	}

	public void setFilter(ArrayList<FilterSpec> filter) {
		tcLog.d(this.getClass().getName(), "setFilter " + filter);
		filterList = filter;
	}

	public void setSort(ArrayList<SortSpec> sort) {
		tcLog.d(this.getClass().getName(), "setSort " + sort);
		sortList = sort;
	}

	public int getTicketCount() {
		try {
			return dataAdapter.getCount();
		} catch (final Exception e) {
			tcLog.e(this.getClass().getName(), "getTicketCount Exception", e);
			return -1;
		}
	}

	public List<Integer> getNewTickets(final String isoTijd) {
		final JSONRPCHttpClient req = new JSONRPCHttpClient(_url, _sslHack, _sslHostNameHack);
		req.setCredentials(_username, _password);
		try {
			final JSONArray datum = new JSONArray();
			datum.put("datetime");
			datum.put(isoTijd);
			final JSONObject ob = new JSONObject();
			ob.put("__jsonclass__", datum);
			final JSONArray param = new JSONArray();
			param.put(ob);
			final JSONArray jsonTicketlist = req.callJSONArray("ticket.getRecentChanges", param);
			final List<Integer> l = new ArrayList<Integer>();

			final int count = jsonTicketlist.length();
			for (int i = 0; i < count; i++) {
				l.add(jsonTicketlist.getInt(i));
			}
			return l;
		} catch (final JSONException e) {
			tcLog.e(this.getClass().getName(), "getNewTickets JSONException", e);
			return null;
		} catch (final JSONRPCException e) {
			tcLog.e(this.getClass().getName(), "getNewTickets JSONRPCException", e);
			return null;
		} catch (final Exception e) {
			tcLog.e(this.getClass().getName(), "getNewTickets Exception", e);
			return null;
		}
	}

	private int getNeighTicket(int ticknr, int dir) {
		tcLog.d(getClass().getName(), "getNeighTicket ticknr = " + ticknr + ", dir = " + dir);
		Ticket t = listener.getTicket(ticknr);
		tcLog.d(getClass().getName(), "t = " + t);
		if (t == null) {
			return -1;
		} else {
			final int pos = dataAdapter.getPosition(t);
			final int newpos = pos + dir;
			// tcLog.d(getClass().getName(),"pos = "+pos+", newpos = "+newpos+", count = "+dataAdapter.getCount());
			if (pos < 0 || newpos < 0 || newpos >= dataAdapter.getCount()) {
				return -1;
			} else {
				t = dataAdapter.getItem(newpos);
				tcLog.d(getClass().getName(), "new ticket = " + t);
				return t != null && t.hasdata() ? t.getTicketnr() : ticknr;
			}
		}
	}

	public int getNextTicket(int ticket) {
		return getNeighTicket(ticket, 1);
	}

	public int getPrevTicket(int ticket) {
		return getNeighTicket(ticket, -1);
	}

	public void setHost(final String url, final String username, final String password, boolean sslHack, boolean sslHostNameHack,
			String profile) {
		tcLog.d(this.getClass().getName(), "setHost " + profile);
		super.setHost(url, username, password, sslHack, sslHostNameHack);
		SelectedProfile = profile;
		resetCache();
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
			if (dataAdapter != null && s != null) {
				dataAdapter.getFilter().filter(s);
				zoektext = s.toString();
			}
		}
	};
}