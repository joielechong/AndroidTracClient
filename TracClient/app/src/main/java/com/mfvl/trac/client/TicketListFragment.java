/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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

import android.app.AlertDialog;
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
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

public class TicketListFragment extends TracClientFragment implements AbsListView.OnScrollListener,
        SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    private static final String ZOEKENNAME = "zoeken";
    private static final String ZOEKTEXTNAME = "filtertext";
    private static final String SCROLLPOSITIONNAME = "scrollPosition";

    private int scrollPosition = 0;
    private boolean scrolling = false;
    private boolean hasScrolled = false;
    private SwipeRefreshLayout swipeLayout = null;
    private TicketListAdapter dataAdapter = null;
    private ListView listView = null;
    private EditText filterText = null;
    private TextView hs = null;
    private final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            MyLog.logCall();
            //MyLog.d("debug",new Exception());
            setStatus(listener.getTicketContentCount() + "/" + listener.getTicketCount());
        }
    };
    private boolean zoeken = false;
    private String zoektext = "";
    private final TextWatcher filterTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (listView != null && listView.getAdapter() != null && s != null) {
                MyLog.d(s);
                MyLog.d(listView.toString());
                Filterable adapter = (Filterable) listView.getAdapter();
                MyLog.d(adapter);
                adapter.getFilter().filter(s);
                zoektext = s.toString();
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyLog.d("savedInstanceState = " + savedInstanceState);

        if (savedInstanceState != null) {
            zoeken = savedInstanceState.getBoolean(ZOEKENNAME);
            zoektext = savedInstanceState.getString(ZOEKTEXTNAME);
            scrollPosition = savedInstanceState.getInt(SCROLLPOSITIONNAME);
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

    void setAdapter(TicketListAdapter a) {
        MyLog.d("a = " + a + " listView = " + listView);
        dataAdapter = a;
        listView.setAdapter(a);
        dataAdapter.registerDataSetObserver(observer);
        zetZoeken();
    }

    private void zetZoeken() {
        MyLog.logCall();
        final View v = getView();

        if (v != null) {
            final EditText filter = (EditText) v.findViewById(R.id.search_box);

            if (filter != null && dataAdapter != null) {
                if (zoeken) {
                    dataAdapter.getFilter().filter(filter.getText());
                    filter.setVisibility(View.VISIBLE);
                    filter.requestFocus();
                } else {
                    filter.setVisibility(View.GONE);
                    dataAdapter.getFilter().filter(null);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MyLog.d("item=" + item.getTitle());

        switch (item.getItemId()) {
            case R.id.tlselect:
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                if (!listener.isFinishing()) {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
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
                break;

            case R.id.tlshare:
                if (dataAdapter != null) {
                    final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    String lijst = "";
                    for (Ticket t : dataAdapter.getTicketList()) {
                        try {
                            lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
                        } catch (final Exception e) {
                            MyLog.e("exception ticket = " + t, e);
                        }
                    }
                    sendIntent.putExtra(Intent.EXTRA_TEXT, lijst);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
                break;

            case R.id.tlzoek:
                MyLog.d("zoeken =" + zoeken);
                zoeken = !zoeken;
                if (zoeken) {
                    MyLog.d("Filter wordt gezet");
                    filterText.addTextChangedListener(filterTextWatcher);
                    filterText.setVisibility(View.VISIBLE);
                    dataAdapter.getFilter().filter("");
                } else {
                    MyLog.d("Filter wordt verwijderd");
                    filterText.removeTextChangedListener(filterTextWatcher);
                    filterText.setVisibility(View.GONE);
                    if (filterText.isFocused()) {
                        filterText.clearFocus();
                    }
                    dataAdapter.getFilter().filter(null);
                }
                zoektext = null;
                filterText.setText(null);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.d("savedInstanceState = " + savedInstanceState);
        setHasOptionsMenu(true);
        if (fragmentArgs != null) {
            if (fragmentArgs.containsKey("TicketArg")) {
                selectTicket(fragmentArgs.getInt("TicketArg"));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MyLog.logCall();
        final View view = inflater.inflate(R.layout.list_view, container, false);

        listView = (ListView) view.findViewById(R.id.listOfTickets);
//        MyLog.d("listView = " + listView);
        scrolling = false;
        hasScrolled = false;
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(this);
        filterText = (EditText) view.findViewById(R.id.search_box);
        hs = (TextView) view.findViewById(R.id.listProgress);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        MyLog.logCall();
        super.onViewCreated(view, savedInstanceState);
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.swipe_blue, R.color.swipe_green, R.color.swipe_orange, R.color.swipe_red);
        listener.listViewCreated();
    }

    @Override
    public int getHelpFile() {
        return R.string.helplistfile;
    }

    @Override
    public void onResume() {
        super.onResume();
        MyLog.logCall();
        dataAdapter = listener.getAdapter();
        listView.setAdapter(dataAdapter);
        dataAdapter.registerDataSetObserver(observer);
        zetZoeken();
        setScroll();
        listView.invalidate();
        registerForContextMenu(listView);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        MyLog.logCall();
        super.onSaveInstanceState(savedState);
        savedState.putBoolean(ZOEKENNAME, zoeken);
        savedState.putString(ZOEKTEXTNAME, zoektext);
        savedState.putInt(SCROLLPOSITIONNAME, scrollPosition);
    }

    @Override
    public void onPause() {
        MyLog.logCall();
        super.onPause();
        scrollPosition = listView.getFirstVisiblePosition();
        dataAdapter = listener.getAdapter();
        dataAdapter.unregisterDataSetObserver(observer);
        listView.setAdapter(null);
        unregisterForContextMenu(listView);
    }

    @Override
    public void onDestroyView() {
        MyLog.logCall();
        if (filterText != null && zoeken) {
            filterText.removeTextChangedListener(filterTextWatcher);
        }
        if (listView != null) {
            listView.invalidateViews();
            listView = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MyLog.logCall();
        inflater.inflate(R.menu.ticketlistmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        MyLog.d("menu = " + menu + " view = " + v + " menuInfo = " + menuInfo);
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listOfTickets) {
            final MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.listcontextmenu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        MyLog.d("item = " + item);
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final Ticket t = (Ticket) listView.getItemAtPosition(info.position);

        if (t != null && t.hasdata()) {
            switch (item.getItemId()) {
                case R.id.select:
                    listener.onTicketSelected(t);
                    return true;

                case R.id.dfupdate:
                    listener.onUpdateTicket(t);
                    return true;

                case R.id.dfshare:
                    final Intent sendIntent = new Intent(Intent.ACTION_SEND);
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
        }
    }

    void dataHasChanged() {
//        try {
        //MyLog.d("hs = " + hs);
            zetZoeken();
//            setStatus(listener.getTicketContentCount() + "/" + listener.getTicketCount());
            listener.getAdapter().notifyDataSetChanged();
//            getView().invalidate();
//            listView.invalidate();
//            listView.invalidateViews();
            setScroll();
//        } catch (Exception ignored) {
//        }
    }

    private void setStatus(final String s) {
        MyLog.d("s = " + s);
        if (hs != null) {
            hs.setText(s);
            hs.invalidate();
        }
    }

    void startLoading() {
//        MyLog.d("hs = " + hs);
        setStatus(R.string.ophalen);
    }

    private void setStatus(final int s) {
        if (hs != null) {
            hs.setText(s);
            hs.invalidate();
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
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (scrolling || hasScrolled) {
            scrollPosition = firstVisibleItem;
            // MyLog.d("onScroll scrollPosition <= "+scrollPosition);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MyLog.d(parent.toString() + " " + view + " " + position + " " + id);
        switch (parent.getId()) {
            case R.id.listOfTickets:
                final Ticket t = dataAdapter.getItem(position);

                if (t != null && t.hasdata()) {
                    listener.onTicketSelected(t);
                } else {
                    showAlertBox(R.string.nodata, R.string.nodatadesc);
                }
                break;
        }
    }

    @Override
    public void onRefresh() {
        MyLog.logCall();
        listener.refreshOverview();
        swipeLayout.setRefreshing(false);
    }
}
