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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

public class TicketListFragment extends TracClientFragment implements OnGestureListener, OnItemClickListener, OnScrollListener, LoaderManager.LoaderCallbacks<Cursor> {
    
	private GestureDetector gestureDetector = null;
	
	private static final String TICKETLISTNAME = "ticketlistInt";
	private static final String SORTLISTNAME = "sortlist";
	private static final String FILTERLISTNAME = "filterlist";
	private static final String ZOEKENNAME = "zoeken";
	private static final String ZOEKTEXTNAME = "filtertext";
	private static final String SCROLLPOSITIONNAME = "scrollPosition";

	private TicketListAdapter dataAdapter = null;
	private ListView listView = null;
	private EditText filterText = null;
	private TextView hs = null;
	private boolean zoeken = false;
	private String zoektext = "";
	private int scrollPosition = 0;
	private boolean scrolling = false;
	private boolean hasScrolled = false;
	private String SelectedProfile = null;
	private int ticketGroupCount;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		tcLog.d(getClass().getName(), "onAttach");
		Tickets.resetCache();
		final Bundle args = this.getArguments();
		if (args != null) {
			SelectedProfile = Tickets.profile;
			if (args.containsKey("TicketArg")) {
				selectTicket(args.getInt("TicketArg"));
			}
		}
	}
	
	private void newDataAdapter(TicketCursor c) {
		tcLog.d(getClass().getName(), "newDataAdapter");
		dataAdapter = new TicketListAdapter(context, R.layout.ticket_list, null);
	}
	
	private static final int URL_LOADER = 0;
	private static final String[] fields = new String[] {TicketCursor.STR_FIELD_ID,TicketCursor.STR_FIELD_TICKET};
	private ProgressDialog pbl = null;
	
	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
	{
		tcLog.d(getClass().getName(), "onCreateLoader "+loaderID+" "+bundle);
		/*
		* Takes action based on the ID of the Loader that's being created
		*/
		switch (loaderID) {
			case URL_LOADER:
				// Returns a new CursorLoader
				if (hs != null) {
					hs.setText(R.string.ophalen);
				}
				pbl = startProgressBar(context.getString(R.string.getlist)+ (SelectedProfile == null ? "" : "\n" + SelectedProfile));
				CursorLoader cl = new CursorLoader(context,TicketProvider.LIST_QUERY_URI,fields,joinList(Tickets.filterList.toArray(), "&"),null,joinList(Tickets.sortList.toArray(), "&"));
				return cl;
				
        default:
            // An invalid id was passed in
            return null;
		}
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		tcLog.d(getClass().getName(), "onLoadFinished "+loader+" "+loader.getId()+" "+cursor);
		dataAdapter.changeCursor(cursor);
		if (pbl != null ) {
			pbl.dismiss();
		}
		if (hs != null) {
			hs.setText(Tickets.getTicketContentCount() + "/" + Tickets.getTicketCount());
		}
		setScroll();
	}		
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		tcLog.d(getClass().getName(), "onLoaderReset "+loader+" "+loader.getId());
	}		

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);
		tcLog.d(getClass().getName(), "onCreate url = " + Tickets.url);
		setHasOptionsMenu(true);

		try {
			ticketGroupCount = context.getResources().getInteger(R.integer.ticketGroupCount);
		} catch (Exception e) {
			tcLog.e(getClass().getName(),"Resource ticketGroupCount not found",e);
			ticketGroupCount=50;
		} finally {
			Tickets.setTicketGroupCount(ticketGroupCount);
		}

		newDataAdapter(null);
		Tickets.setOnChanged(new onTicketsChanged() {
			@Override
			public void onChanged() {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tcLog.d(getClass().getName(), "Tickets.setOnChanged.onChanged.run");
						if (hs != null) {
							hs.setText(Tickets.getTicketContentCount() + "/" + Tickets.getTicketCount());
						}
						dataAdapter.notifyDataSetChanged();
					}
				});
			}
		});
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(getClass().getName(), "onCreateView savedInstanceState = " + savedInstanceState);
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
		getLoaderManager().initLoader(URL_LOADER, null, this);
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
		tcLog.d(getClass().getName(), "onItemClick + "+parent+" "+view+" "+position+" "+id );
		switch (parent.getId()) {
			case R.id.listOfTickets:
			final Ticket t = Tickets.ticketList.get(position);
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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(getClass().getName(), "onActivityCreated savedInstanceState = " + savedInstanceState);
		if (Tickets.url == null) {
			listener.onChangeHost();
			getFragmentManager().popBackStack();
		} else {
		// listener.initializeList();
//			if (filterArg != null) {
//				listener.setFilter(filterArg);
//				setFilter(filterArg);
//				filterArg = null;
//			}
//			if (sortArg != null) {
//				listener.setSort(sortArg);
//				setSort(sortArg);
//				sortArg = null;
//			}
			if (savedInstanceState != null) {
				Tickets.tickets = savedInstanceState.getIntArray(TICKETLISTNAME);
//				listener.setSortAndFilter((ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME),(ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME));
			}
//			if (Tickets.sortList == null) {
//				Tickets.sortList = new ArrayList<SortSpec>();
//				Tickets.sortList.add(new SortSpec("priority"));
//				Tickets.sortList.add(new SortSpec("modified", false));
//			}
//			if (Tickets.filterList == null) {
//				Tickets.filterList = new ArrayList<FilterSpec>();
//				Tickets.filterList.add(new FilterSpec("max", "=", "500"));
//				Tickets.filterList.add(new FilterSpec("status", "!=", "closed"));
//			}
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
	}

	@Override
	public void onResume() {
		super.onResume();
		tcLog.d(getClass().getName(), "onResume");
		gestureDetector = new GestureDetector(context, this);
		dataAdapter.notifyDataSetChanged();
		zetZoeken();
		setScroll();
		listView.invalidate();
	}

	@Override
	public void onPause() {
		tcLog.d(getClass().getName(), "onPause");
		super.onPause();
		scrollPosition = listView.getFirstVisiblePosition();
		// tcLog.d(getClass().getName(),"onPause scrollPosition <= "+scrollPosition);
		gestureDetector = null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		tcLog.d(getClass().getName(), "onCreateContextMenu");
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.listOfTickets) {
			final MenuInflater inflater = context.getMenuInflater();
			inflater.inflate(R.menu.listcontextmenu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		tcLog.d(getClass().getName(), "onContextItemSelected");
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
				listener.shareTicket(t);
				return true;
			default:
			}
		}
		return super.onContextItemSelected(item);
	}

	private void setScroll() {
		if (listView != null) {
			listView.setSelection(scrollPosition);
		}
	}

	private void zetZoeken() {
		tcLog.d(getClass().getName(), "zetZoeken");
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
		tcLog.d(getClass().getName(), "onDestroyView");
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
		tcLog.d(getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		tcLog.d(getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.ticketlistmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	public void requery() {
		tcLog.d(getClass().getName(), "requery adapter = "+dataAdapter);
		try {
			throw new Exception("Debug");
		} catch(Exception e) {
			tcLog.d(getClass().getName(),"Debug",e);
		}
		if (dataAdapter != null) {
			Tickets.initList();
			dataAdapter.notifyDataSetChanged();
			getLoaderManager().restartLoader(URL_LOADER, null, this);
			scrollPosition = 0;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
		final int itemId = item.getItemId();
		if (itemId == R.id.tlnieuw) {
			listener.onNewTicket();
		} else if (itemId == R.id.tlrefresh) {
			requery();
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
		} else if (itemId == R.id.tlshare) {
			shareList();
		} else if (itemId == R.id.tlfilter) {
			listener.onFilterSelected(Tickets.filterList);
		} else if (itemId == R.id.tlsort) {
			listener.onSortSelected(Tickets.sortList);
		} else if (itemId == R.id.tlchangehost) {
			listener.onChangeHost();
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
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		savedState.putIntArray(TICKETLISTNAME, Tickets.tickets);
		savedState.putSerializable(SORTLISTNAME, Tickets.sortList);
		savedState.putSerializable(FILTERLISTNAME, Tickets.filterList);
		savedState.putBoolean(ZOEKENNAME, zoeken);
		savedState.putString(ZOEKTEXTNAME, zoektext);
		if (listView != null) {
			scrollPosition = listView.getFirstVisiblePosition();
		}
		savedState.putInt(SCROLLPOSITIONNAME, scrollPosition);
		tcLog.d(getClass().getName(), "onSaveInstanceState = " + savedState);
	}

	private String joinList(Object list[], final String sep) {
		String reqString = "";
		for (final Object fs : list) {
			if (fs != null) {
				if (reqString.length() > 0) {
					reqString += sep;
				}
				reqString += fs.toString();
			}
		}
		return reqString;
	}

	private void shareList() {
		tcLog.d(getClass().getName(), "shareList");
		String lijst = "";

		for (dataAdapter.moveToFirst(); !dataAdapter.isAfterLast(); dataAdapter.moveToNext()) {
			final Ticket t = (Ticket)dataAdapter.getItem(TicketCursor.FIELD_TICKET);
			try {
				lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
			} catch (final JSONException e) {
				tcLog.i(getClass().getName(), "shareList exception", e);
			}
		}
		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, lijst);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	public List<Integer> getNewTickets(final String isoTijd) {
		final TracHttpClient req = TracHttpClient.getInstance();
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
			tcLog.e(getClass().getName(), "getNewTickets JSONException", e);
			return null;
		} catch (final JSONRPCException e) {
			tcLog.e(getClass().getName(), "getNewTickets JSONRPCException", e);
			return null;
		} catch (final Exception e) {
			tcLog.e(getClass().getName(), "getNewTickets Exception", e);
			return null;
		}
	}

	@Override
	public void setHost() {
		tcLog.d(getClass().getName(), "setHost");
		super.setHost();
		SelectedProfile = Tickets.profile;
		Tickets.resetCache();
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
		// tcLog.d(getClass().getName(),"onFling e1 = "+e1+", e2 = "+e2);
		// tcLog.toast("VelocityY = "+velocityY);
		if (velocityY > fast_move &&(e2.getY() - e1.getY() > extra_large_move) && scrollPosition == 0) {
			requery();
			return true;
		}
		return false;
	}

	public boolean dispatchTouchEvent(MotionEvent ev) {
		return gestureDetector != null && gestureDetector.onTouchEvent(ev);
	}
}
