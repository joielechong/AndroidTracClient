/*
 * Copyright (C) 2013-2016 Michiel van Loon
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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.mfvl.trac.client.Const.*;

public class DetailFragment extends TracClientFragment
        implements SwipeRefreshLayout.OnRefreshListener, CompoundButton.OnCheckedChangeListener,
                   GestureDetector.OnGestureListener, OnFileSelectedListener, OnItemClickListener,
                   OnItemLongClickListener {

    private static final String EMPTYFIELDS = "emptyfields";
    private static final String MODVELD = "modveld";

    private static final List<String> skipFields = Arrays.asList("summary", "_ts", "max", "page",
                                                                 "id");
    private static final List<String> timeFields = Arrays.asList("time", "changetime");
    private final List<modifiedString> values = new ArrayList<>();
    private int ticknr = -1;
    private boolean showEmptyFields = false;
    private TicketModel tm = null;
    private ModVeldMap modVeld;
    private boolean sendNotification = false;
    private boolean didUpdate = false;
    private String[] notModified;
    private String[] isStatusUpd;
    private MenuItem selectItem;
    private GestureDetector gestureDetector = null;
    private int popup_selected_color = 0;
    private int popup_unselected_color = 0;
    private SwipeRefreshLayout swipeLayout;
    private View currentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tcLog.d( "onCreate savedInstanceState = " + savedInstanceState);
        if (fragmentArgs != null) {
            ticknr = fragmentArgs.getInt(CURRENT_TICKET);
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_TICKET)) {
            ticknr = savedInstanceState.getInt(CURRENT_TICKET, -1);
        }

        modVeld = new ModVeldMap();
        modVeld.clear();
        setHasOptionsMenu(true);
        didUpdate = false;

        notModified = getResources().getStringArray(R.array.fieldsnotmodified);
        isStatusUpd = getResources().getStringArray(R.array.fieldsstatusupdate);
        popup_selected_color = ContextCompat.getColor(context, R.color.popup_selected);
        popup_unselected_color = ContextCompat.getColor(context, R.color.popup_unselected);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.detail_view, container, false);
    }

    @Override
    public void onRefresh() {
//	tcLog.d( "onRefresh");
        refreshTicket();
        swipeLayout.setRefreshing(false);
    }

    private void refreshTicket() {
        if (_ticket != null) {
            listener.refreshTicket(_ticket.getTicketnr());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        helpFile = R.string.helpdetailfile;
        gestureDetector = new GestureDetector(context, this);

        currentView = getView();
        if (ticknr != -1) {
            display_and_refresh_ticket();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        sendNotification = isChecked;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d("savedInstanceState = " + savedInstanceState);

        tm = listener.getTicketModel();
        View view = getView();
        CheckBox updNotify = (CheckBox) view.findViewById(R.id.updNotify);
        updNotify.setOnCheckedChangeListener(this);
        setListener(R.id.canBut);
        setListener(R.id.updBut);
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.swipe_blue,
                                            R.color.swipe_green,
                                            R.color.swipe_orange,
                                            R.color.swipe_red);
        if (savedInstanceState != null) {
            showEmptyFields = savedInstanceState.getBoolean(EMPTYFIELDS, false);
            if (savedInstanceState.containsKey(CURRENT_TICKET)) {
                // tcLog.d("onActivityCreated start Loading");
                if (savedInstanceState.containsKey(MODVELD)) {
                    modVeld = (ModVeldMap) savedInstanceState.getSerializable(MODVELD);
                }
                setSelect(modVeld.isEmpty());
                ticknr = savedInstanceState.getInt(CURRENT_TICKET, -1);
            }
        }
    }

    private void display_and_refresh_ticket() {
        _ticket = listener.getTicket(ticknr);
        displayTicket();

        if (didUpdate) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshTicket();
                }
            });
            didUpdate = false;
        }

        try {
            currentView.findViewById(R.id.modveld).setVisibility(
                    modVeld.isEmpty() ? View.GONE : View.VISIBLE);
        } catch (NullPointerException ignored) {
        }
        setSelect(modVeld.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d(item.toString());

        if (item.getItemId() == R.id.dfupdate) {
            if (_ticket != null) {
                listener.onUpdateTicket(_ticket);
                didUpdate = true;
            }
        } else if (item.getItemId() == R.id.dfselect) {
            if (!listener.isFinishing()) {

                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setTitle(R.string.chooseticket)
                        .setMessage(R.string.chooseticknr)
                        .setView(input)
                        .setCancelable(false)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    final int newTicket = Integer.parseInt(
                                            input.getText().toString());
                                    // selectTicket(ticknr);
                                    listener.getTicket(newTicket);
                                } catch (final Exception e) {// noop keep old ticketnr
                                }
                            }
                        })
                        .show();
            }
        } else if (item.getItemId() == R.id.dfattach) {
            if (_ticket != null) {
                listener.onChooserSelected(this);
            }
        } else if (item.getItemId() == R.id.dfrefresh) {
            refreshTicket();
        } else if (item.getItemId() == R.id.dfempty) {
            item.setChecked(!item.isChecked());
            showEmptyFields = item.isChecked();
// 			tcLog.d( "showEmptyFields = "+showEmptyFields);
            displayTicket();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.canBut:
                modVeld.clear();
                setSelect(true);
                display_and_refresh_ticket();
                break;

            case R.id.updBut:
                updateTicket();
                try {
                    currentView.findViewById(R.id.modveld).setVisibility(View.GONE);
                } catch (NullPointerException ignored) {
                }
                currentView.postInvalidate();
                break;

            default:

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        tcLog.d("_ticket = " + _ticket + " " + modVeld);
        if (_ticket != null) {
            savedState.putInt(CURRENT_TICKET, _ticket.getTicketnr());
        } else if (ticknr != -1) {
            savedState.putInt(CURRENT_TICKET, ticknr);
        }
        if (!modVeld.isEmpty()) {
            savedState.putSerializable(MODVELD, modVeld);
        }
        savedState.putBoolean(EMPTYFIELDS, showEmptyFields);
        // tcLog.d( "onSaveInstanceState = " + savedState);
    }

    private void updateTicket() {
        tcLog.logCall();
        try {
            listener.startProgressBar(R.string.saveupdate);
            listener.updateTicket(_ticket, "leave", "", null, null, sendNotification, modVeld);
            modVeld.clear();
            listener.stopProgressBar();
            displayTicket();
        } catch (final Exception e) {
            tcLog.e("Exception during update", e);
            showAlertBox(R.string.upderr, R.string.storerrdesc, e.getMessage());
        }
    }

    @Override
    public void onFileSelected(final Uri uri) {
        tcLog.d("ticket = " + _ticket + " uri = " + uri);
        listener.startProgressBar(R.string.uploading);
        new Thread() {
            @Override
            public void run() {
                listener.addAttachment(_ticket, uri, new onTicketCompleteListener() {
                    @Override
                    public void onComplete(Ticket t2) {
                        refreshTicket();
                        listener.stopProgressBar();
                    }
                });

            }
        }.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        gestureDetector = null;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final modifiedString t = (modifiedString) parent.getItemAtPosition(position);

        tcLog.d("position = " + position);
        if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
            return false;
        } else if (t.length() >= 8 && "comment:".equals(t.substring(0, 8))) {
            showAlertBox(R.string.notpossible, R.string.nocomment, null);
        } else {
            final String[] parsed = t.split(":", 2);

            selectField(parsed[0], parsed[1].trim());
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final modifiedString t = (modifiedString) parent.getItemAtPosition(position);

        tcLog.d("position = " + position);
        if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
            final int d = t.indexOf(":");
            final int bijlagenr = Integer.parseInt(t.substring(8, d));

            selectBijlage(bijlagenr);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // tcLog.d( "onCreateOptionsMenu");
        inflater.inflate(R.menu.detailmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        selectItem = menu.findItem(R.id.dfselect);
        setSelect(true);
    }

    private void selectBijlage(final int bijlagenr) {
        listener.startProgressBar(R.string.downloading);
        try {
            final String filename = _ticket.getAttachmentFile(bijlagenr - 1);
            final String mimeType = getMimeType(filename);

            listener.getAttachment(_ticket, filename, new onAttachmentCompleteListener() {
                @Override
                public void onComplete(final byte[] filedata) {
                    // tcLog.d("onComplete filedata = "
                    // + filedata.length);
                    try {
                        final File file = TracGlobal.makeCacheFilePath(filename);
                        final OutputStream os = new FileOutputStream(file);

                        file.deleteOnExit();
                        os.write(filedata);
                        os.close();
                        final Intent viewIntent = new Intent(Intent.ACTION_VIEW);

                        // tcLog.d("file = "+ file.toString() + " mimeType = " + mimeType);
                        if (mimeType != null) {
                            viewIntent.setDataAndType(Uri.fromFile(file), mimeType);
                            startActivity(viewIntent);
                        } else {
                            viewIntent.setData(Uri.parse(file.toString()));
                            final Intent j = Intent.createChooser(viewIntent, context.getString(
                                    R.string.chooseapp));

                            startActivity(j);
                        }
                    } catch (final Exception e) {
                        tcLog.w(context.getString(R.string.ioerror) + ": " + filename, e);
                        showAlertBox(R.string.notfound, R.string.sdcardmissing, null);
                    } finally {
                        listener.stopProgressBar();
                    }
                }
            });

        } catch (final JSONException e) {
            tcLog.e("JSONException fetching attachment", e);
        }
    }

    private String getMimeType(String url) {
        return URLConnection.guessContentTypeFromName(url);
    }

    private void setSelect(final boolean value) {
        tcLog.d(String.format(Locale.US, "%b", value));
        if (selectItem != null) {
            selectItem.setEnabled(value);
        }
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

        if (e1 == null || e2 == null) {
            return false;
        }

        int newTicket = -1;

        // tcLog.d("onFling e1 = "+e1+", e2 = "+e2);

        if (e1.getX() - e2.getX() > large_move) {
            newTicket = listener.getNextTicket(_ticket.getTicketnr());
        } else if (e2.getX() - e1.getX() > large_move) {
            newTicket = listener.getPrevTicket(_ticket.getTicketnr());
        }
        if (newTicket >= 0 && modVeld.isEmpty()) {
            setTicket(newTicket);
            return true;
        }
        return false;
    }

    public void setTicket(int newTicket) {
        ticknr = newTicket;
        display_and_refresh_ticket();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return gestureDetector != null && gestureDetector.onTouchEvent(ev);
    }

    public void setModVeld(final String veld, final String waarde, final String newValue) {
        tcLog.d("veld = " + veld + " waarde = " + waarde + "newValue = " + newValue);
        final ListView parent = (ListView) currentView.findViewById(R.id.listofFields);
        if (newValue != null && !newValue.equals(waarde) || newValue == null && waarde != null) {
            if ("summary".equals(veld)) {
                final TextView dataView = (TextView) currentView.findViewById(R.id.ticknr);
                dataView.setText(newValue);
                dataView.setTextColor(popup_selected_color);
                tcLog.d("tickText na postInvalidate + " + dataView);
                final String[] parsed = newValue.split(":", 2);
                modVeld.put("summary", parsed[1].trim());
            } else {
                final int pos = ((ModifiedStringArrayAdapter) parent.getAdapter()).getPosition(
                        new modifiedString(veld, newValue));

                if (pos >= 0) {
                    final modifiedString ms = values.get(pos);

                    ms.setWaarde(newValue);
                    ms.setUpdated();
                    values.set(pos, ms);
                    ((ModifiedStringArrayAdapter) parent.getAdapter()).notifyDataSetChanged();
                }
                modVeld.put(veld, newValue);
            }
            setSelect(false);
        }
        final LinearLayout mv = (LinearLayout) currentView.findViewById(R.id.modveld);

        if (mv != null && !modVeld.isEmpty()) {
            mv.setVisibility(View.VISIBLE);
        }
    }

    private void displayTicket() {
        tcLog.d("ticket = " + _ticket);
        if (_ticket != null) {

            if (currentView == null) {
                return;
            }
            final ListView listView = (ListView) currentView.findViewById(R.id.listofFields);

            listView.setAdapter(null);
            values.clear();
            final TextView tickText = (TextView) currentView.findViewById(R.id.ticknr);

            if (tickText != null) {
                tickText.setText(String.format(Locale.US, "Ticket %d", _ticket.getTicketnr()));
                try {
                    String summ = _ticket.getString("summary");

                    tickText.setTextColor(popup_unselected_color);
                    if (modVeld.containsKey("summary")) {
                        summ = modVeld.get("summary");
                        tickText.setTextColor(popup_selected_color);
                    }
                    tickText.append(" : " + summ);
                } catch (final JSONException ignored) {
//                    tcLog.e( "JSONException fetching summary");
                }
                tickText.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        selectField("summary", ((TextView) view).getText().toString());
                        tcLog.d("tickText modVeld = " + modVeld);
                        return true;
                    }
                });
            }
            for (final String veld : tm.velden()) {

                try {
                    modifiedString ms = null;

                    //tcLog.d( "showEmptyFields = "+showEmptyFields);

                    if (!skipFields.contains(veld)) {
                        if (timeFields.contains(veld)) {
                            ms = new modifiedString(veld, toonTijd(_ticket.getJSONObject(veld)));
                        } else if (showEmptyFields || _ticket.getString(veld).length() > 0) {
                            ms = new modifiedString(veld, _ticket.getString(veld));
                        }
                    }

                    if (ms != null) {
                        if (modVeld.containsKey(veld)) {
                            ms.setUpdated();
                            ms.setWaarde(modVeld.get(veld));
                            setSelect(false);
                        }
                        values.add(ms);
                    }
                } catch (final JSONException e) {
//                    tcLog.e( "JSONException fetching field " + veld);
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
                            values.add(
                                    new modifiedString("comment",
                                                       toonTijd(cmt.getJSONObject(
                                                               0)) + " - " + cmt.getString(
                                                               1) + " - " + cmt.getString(4)));
                        }
                    } catch (final JSONException e) {
                        tcLog.e("JSONException in displayTicket loading history");
                    }
                }
            }
            final JSONArray attachments = _ticket.getAttachments();

            if (attachments != null) {
                for (int j = 0; j < attachments.length(); j++) {
                    JSONArray bijlage;

                    try {
                        bijlage = attachments.getJSONArray(j);
                        values.add(
                                new modifiedString("bijlage " + (j + 1),
                                                   toonTijd(bijlage.getJSONObject(
                                                           3)) + " - " + bijlage.getString(
                                                           4) + " - " + bijlage.getString(0)
                                                           + " - " + bijlage.getString(1)));

                    } catch (final JSONException e) {
                        tcLog.e("JSONException in displayTicket loading attachments", e);
                    }
                }
            }
            listView.setOnItemLongClickListener(this);
            listView.setOnItemClickListener(this);
            final ModifiedStringArrayAdapter dataAdapter = new ModifiedStringArrayAdapter(context,
                                                                                          R.layout.ticket_list,
                                                                                          values);

            listView.setAdapter(dataAdapter);
        }
    }

    private String toonTijd(final JSONObject v) {
        try {
            return TracGlobal.toCalendar(
                    v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
        } catch (final Exception e) {
            tcLog.e("Error converting time", e);
            return "";
        }
    }

    private void selectField(final String veld, final String waarde) {
        if (Arrays.asList(notModified).contains(veld)) {
            showAlertBox(R.string.notpossible, R.string.notchange, null);
        } else if (Arrays.asList(isStatusUpd).contains(veld)) {
            listener.onUpdateTicket(_ticket);
            didUpdate = true;
        } else {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("editfield");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            // Create and show the dialog.
            EditFieldFragment editFieldFragment = new EditFieldFragment();
            Bundle args = new Bundle();
            args.putString("veld", veld);
            args.putString("waarde", waarde);
            args.putSerializable("tm", tm);
            editFieldFragment.setArguments(args);
            editFieldFragment.show(ft, "editfield");
        }
    }

    public boolean onBackPressed() {
        tcLog.logCall();
        if (!modVeld.isEmpty()) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.unsaved)
                            .setPositiveButton(R.string.ja, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getFragmentManager().popBackStack();
                                    updateTicket();
                                }
                            })
                            .setNegativeButton(R.string.nee, null)
                            .show();
                }
            });
            return true;
        } else {
            tcLog.d("returned false");
            return false;
        }
    }

    private class ModVeldMap extends HashMap<String, String> implements Serializable {
//	private static final long serialVersionUID = 191019591050L;
    }

    private class modifiedString {
        private final String _veld;
        private boolean _updated;
        private String _waarde;

        public modifiedString(String v, String w) {
            _veld = v;
            _waarde = w;
            _updated = false;
        }

        public boolean getUpdated() {
            return _updated;
        }

        public void setUpdated() {
            _updated = true;
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

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof modifiedString && _veld.equals(
                    ((modifiedString) o).veld());
        }

        @Override
        public String toString() {
            return _veld + ": " + _waarde;
        }

        public String veld() {
            return _veld;
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

                ((TextView) view).setTextColor(
                        ms.getUpdated() ? popup_selected_color : popup_unselected_color);
                return view;
            } catch (final Exception e) {
                tcLog.e("exception", e);
                return null;
            }
        }
    }
}
