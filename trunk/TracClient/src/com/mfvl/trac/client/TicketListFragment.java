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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

public class TicketListFragment extends TracClientFragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener, AbsListView.OnScrollListener, TextWatcher {
	
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
	
 	private ShareActionProvider listShare = null;	
	private SwipeRefreshLayout swipeLayout;
	
	private class TicketDataSetObserver extends DataSetObserver  {
		@Override
		public void onChanged() {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tcLog.d(getClass().getName(), "TicketDataSetObserver.onChanged.run");
					setStatus(listener.getTicketContentCount() + "/" + listener.getTicketCount());
					listView.invalidate();
				}
			});
		}
		
		@Override
		public void onInvalidated() {
		}
	}
	
	TicketDataSetObserver ticketDataSetObserver = new TicketDataSetObserver();
	
	private void onMyAttach(Context activity) {
        final Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey("TicketArg")) {
                selectTicket(args.getInt("TicketArg"));
            }
        }
	}
	
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        tcLog.d(getClass().getName(), "onAttach(C)");
		onMyAttach(activity);
    }
	
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
        tcLog.d(getClass().getName(), "onAttach(A)");
		onMyAttach(activity);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d(getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);
        setHasOptionsMenu(true);
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(getClass().getName(), "onCreateView savedInstanceState = " + savedInstanceState);
        final View view = inflater.inflate(R.layout.list_view, container, false);

		dataAdapter = listener.getAdapter();
		
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
	public void onViewCreated(View view, Bundle savedInstanceState){
		tcLog.d(this.getClass().getName(), "onViewCreated view = " + view + " sis = " + savedInstanceState);
		swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
		swipeLayout.setOnRefreshListener(this);
		swipeLayout.setColorSchemeResources(R.color.swipe_blue, 
            R.color.swipe_green, 
            R.color.swipe_orange, 
            R.color.swipe_red);
		listener.listViewCreated();
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d(getClass().getName(), "onActivityCreated savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) {
            zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
            zoektext = savedInstanceState.getString(ZOEKTEXTNAME);
            scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME);
            // tcLog.d(getClass().getName(),"onActivityCreated scrollPosition <= "+scrollPosition);
        }
        if (zoeken) {
            filterText.setVisibility(View.VISIBLE);
            filterText.setText(zoektext);
            filterText.addTextChangedListener(this);
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
        tcLog.d(getClass().getName(), "onResume");
		dataAdapter = listener.getAdapter();
		dataAdapter.registerDataSetObserver(ticketDataSetObserver);
        zetZoeken();
        setScroll();
        listView.invalidate();
    }

    @Override
    public void onPause() {
        tcLog.d(getClass().getName(), "onPause");
        super.onPause();
        scrollPosition = listView.getFirstVisiblePosition();
		dataAdapter.unregisterDataSetObserver(ticketDataSetObserver);
        // tcLog.d(getClass().getName(),"onPause scrollPosition <= "+scrollPosition);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        tcLog.d(getClass().getName(), "onCreateContextMenu menu = "+menu+" view = "+v+"menuInfo = "+menuInfo);
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listOfTickets) {
            final MenuInflater inflater = context.getMenuInflater();
            inflater.inflate(R.menu.listcontextmenu, menu);
/*
			final Ticket t = (Ticket) listView.getItemAtPosition(((AdapterContextMenuInfo)menuInfo).position);

			final MenuItem itemDetail = menu.findItem(R.id.dfshare);
			if (itemDetail != null) {
				tcLog.d(getClass().getName(), "item = " + itemDetail);
				listener.setActionProvider(menu,R.id.dfshare);
				ShareActionProvider detailShare = (ShareActionProvider) itemDetail.getActionProvider();
				detailShare.setShareHistoryFileName("custom_share_history_detail_popup.xml");
				Intent i = listener.shareTicket(t);
				tcLog.d(getClass().getName(), "SAP = " + detailShare + " " + i);
				detailShare.setShareIntent(i);
			}
*/
		}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        tcLog.d(getClass().getName(), "onContextItemSelected item = "+item);
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Ticket t = (Ticket) listView.getItemAtPosition(info.position);

        if (t!= null && t.hasdata()) {
            switch (item.getItemId()) {
            case R.id.select:
                listener.onTicketSelected(t);
                return true;

            case R.id.dfupdate:
                listener.onUpdateTicket(t);
                return true;

            case R.id.dfshare:
                Intent i = listener.shareTicket(t);
				if (i != null) {
					startActivity(i);
				}
                return true;

            default:
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        tcLog.d(getClass().getName(), "onDestroyView");
        if (filterText != null && zoeken) {
            filterText.removeTextChangedListener(this);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        tcLog.d(getClass().getName(), "onCreateOptionsMenu");
        inflater.inflate(R.menu.ticketlistmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
	
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        tcLog.d(getClass().getName(), "onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);
		listener.setActionProvider(menu,R.id.tlshare);
		final MenuItem itemList = menu.findItem(R.id.tlshare);
		if (itemList != null) {
			tcLog.d(getClass().getName(), "item = " + itemList);
			listShare = (ShareActionProvider) itemList.getActionProvider();
			updateShareActionProvider();
		}
	}
	
	private void updateShareActionProvider() {
        tcLog.d(getClass().getName(), "updateShareActionProvider");
		Intent i = listener.shareList();
		tcLog.d(getClass().getName(), "SAP = " + listShare + " " + i);
		if (listShare != null && i != null) {
			listShare.setShareIntent(i);
			listShare.setShareHistoryFileName("custom_share_history_list.xml");
		}
     }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d(getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
        final int itemId = item.getItemId();

		if (itemId == R.id.help) {
			showHelp();
		} else if (itemId == R.id.tlselect) {
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
			
            if (!listener.isFinishing()) {
				final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
				alertDialogBuilder.setTitle(R.string.chooseticket)
					.setMessage(R.string.chooseticknr)
					.setView(input)
					.setCancelable(false)
					.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							final int ticknr = Integer.parseInt(input.getText().toString());

							selectTicket(ticknr);
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
            }
//        } else if (itemId == R.id.tlshare) {
//            shareList();
        } else if (itemId == R.id.tlzoek) {
            zoeken = !zoeken;
            if (zoeken) {
                filterText.addTextChangedListener(this);
                filterText.setVisibility(View.VISIBLE);
            } else {
                filterText.removeTextChangedListener(this);
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
        tcLog.e(getClass().getName(), "onSaveInstanceState in = "+ savedState);
        super.onSaveInstanceState(savedState);
        savedState.putBoolean(ZOEKENNAME, zoeken);
        savedState.putString(ZOEKTEXTNAME, zoektext);
        savedState.putInt(SCROLLPOSITIONNAME,scrollPosition);
        tcLog.d(getClass().getName(), "onSaveInstanceState out = " + savedState);
    }
	
	private void setStatus(final String s) {
		try {
			hs.setText(s);
		} catch (Exception e) {
		}
	}

	private void setStatus(final int s) {
		try {
			hs.setText(s);
		} catch (Exception e) {		}
	}

	public void dataHasChanged() {
		try {
			tcLog.d(getClass().getName(), "dataHasChanged hs = " + hs);
			setStatus(listener.getTicketContentCount() + "/" + listener.getTicketCount());
			updateShareActionProvider();
			listView.invalidate();
			listView.invalidateViews();
			setScroll();
		} catch (Exception e) {}
	}
	
	public void startLoading() {
        tcLog.d(getClass().getName(), "startLoading hs = " + hs);
        setStatus(R.string.ophalen);
 	}

	// AbsListView.OnScrollListener
	
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

	// AdapterView.OnItemClickListener
	
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        tcLog.d(getClass().getName(), "onItemClick + " + parent + " " + view + " " + position + " " + id);
        switch (parent.getId()) {
        case R.id.listOfTickets:
            final Ticket t = (Ticket)dataAdapter.getItem(position);

            if (t.hasdata()) {
                listener.onTicketSelected(t);
            } else {
				showAlertBox(R.string.nodata,R.string.nodatadesc,null);
            }
            break;
        }
    }

    private void setScroll() {
		try {
            listView.setSelection(scrollPosition);
        } catch (Exception e) {}
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
	
	// TracClientFragment

	public void showHelp() {
		showHelpFile(R.string.helplistfile);
	}

	//SwipeRefreshLayout.OnRefreshListener
	
	@Override 
	public void onRefresh() {
		tcLog.d(this.getClass().getName(), "onRefresh");
 		listener.refreshOverview();
		swipeLayout.setRefreshing(false);
	}
	
	// TextWatcher

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
		tcLog.d(getClass().getName(),"onTextChanged "+s+" "+dataAdapter);
        if (dataAdapter != null && s != null) {
            dataAdapter.getFilter().filter(s);
            zoektext = s.toString();
        }
    }
}