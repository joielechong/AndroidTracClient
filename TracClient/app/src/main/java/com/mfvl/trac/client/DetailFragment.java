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

import java.io.File;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.TextView;

public class DetailFragment extends TracClientFragment implements SwipeRefreshLayout.OnRefreshListener, CompoundButton.OnCheckedChangeListener, GestureDetector.OnGestureListener, onFileSelectedListener, OnItemClickListener,OnItemLongClickListener {

    private static final String EMPTYFIELDS = "emptyfields";
    private static final String MODVELD = "modveld";

    private static final List<String> skipFields = Arrays.asList("summary","_ts","max","page","id");
    private static final List<String> timeFields = Arrays.asList("time","changetime");

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

        public boolean getUpdated() {
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
            return this == o || o instanceof modifiedString && _veld.equals(((modifiedString) o).veld());
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

                ((TextView) view).setTextColor(ms.getUpdated() ? popup_selected_color : popup_unselected_color);
                return view;
            } catch (final Exception e) {
                tcLog.e( "exception", e);
                return null;
            }
        }
    }

//    private File path = null; // TODO voor attachments
    private int ticknr = -1;
    private boolean showEmptyFields = false;
    private TicketModel tm = null;
    private ModVeldMap modVeld;
    private PopupWindow pw = null;
    private boolean sendNotification = false;
    private boolean didUpdate = false;
    private final List<modifiedString> values = new ArrayList<>();
    private String[] notModified;
    private String[] isStatusUpd;
    private MenuItem selectItem;
    private GestureDetector gestureDetector = null;
	private int popup_selected_color = 0;
	private int popup_unselected_color = 0;
	private SwipeRefreshLayout swipeLayout;

    private void setSelect(final boolean value) {
        tcLog.d(String.format(Locale.US,"%b",value));
        if (selectItem != null) {
            selectItem.setEnabled(value);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tcLog.d( "onCreate savedInstanceState = " + savedInstanceState);
        if (fragmentArgs != null) {
            ticknr = fragmentArgs.getInt(Const.CURRENT_TICKET);
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(Const.CURRENT_TICKET)) {
            ticknr = savedInstanceState.getInt(Const.CURRENT_TICKET, -1);
        }

        modVeld = new ModVeldMap();
        modVeld.clear();
        setHasOptionsMenu(true);
        didUpdate = false;
		
        notModified = getResources().getStringArray(R.array.fieldsnotmodified);
        isStatusUpd = getResources().getStringArray(R.array.fieldsstatusupdate);
		popup_selected_color = ContextCompat.getColor(context, R.color.popup_selected);
		popup_unselected_color = ContextCompat.getColor(context,R.color.popup_unselected);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // tcLog.d( "onCreateOptionsMenu");
        inflater.inflate(R.menu.detailmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        selectItem = menu.findItem(R.id.dfselect);
        setSelect(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.detail_view, container, false);
    }

	@Override 
	public void onRefresh() {
//		tcLog.d( "onRefresh");
 		refresh_ticket();
		swipeLayout.setRefreshing(false);
	}
	
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        sendNotification = isChecked;
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.canBut:
			modVeld.clear();
			setSelect(true);
//			getFragmentManager().popBackStack();
			break;
			
			case R.id.updBut:
			updateTicket();
            try {
                getView().findViewById(R.id.modveld).setVisibility(View.GONE);
            } catch (NullPointerException ignored) {
            }
			break;
			
			case R.id.cancelpw:
			if (pw != null && !listener.isFinishing()) {
				pw.dismiss();
			}
			break;
			
			default:
			
		}
	}
	
    @Override
    public void onPause() {
        super.onPause();
        gestureDetector = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d( "savedInstanceState = " + savedInstanceState);

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
            if (savedInstanceState.containsKey(Const.CURRENT_TICKET)) {
                // tcLog.d("onActivityCreated start Loading");
                if (savedInstanceState.containsKey(MODVELD)) {
                    modVeld = (ModVeldMap) savedInstanceState.getSerializable(MODVELD);
                }
                setSelect(modVeld.isEmpty());
                ticknr = savedInstanceState.getInt(Const.CURRENT_TICKET, -1);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
		helpFile = R.string.helpdetailfile;
        gestureDetector = new GestureDetector(context, this);
        if (ticknr != -1) {
			display_and_refresh_ticket();
        } else {
            getFragmentManager().popBackStack();
        }
    }

	private void display_and_refresh_ticket() {
		_ticket = listener.getTicket(ticknr);
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

        try {
    		getView().findViewById(R.id.modveld).setVisibility(modVeld.isEmpty() ? View.GONE : View.VISIBLE);
        } catch (NullPointerException ignored) {
		}
		setSelect(modVeld.isEmpty());
	}
/*
    @Override
    public void onResume() {
        super.onResume();
        gestureDetector = new GestureDetector(context, this);
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
        final View v = getView();

        if (v != null) {
            final LinearLayout mv = (LinearLayout) v.findViewById(R.id.modveld);

            if (mv != null) {
                mv.setVisibility(modVeld.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
        setSelect(modVeld.isEmpty());
    }
*/
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        tcLog.logCall();
        final MenuItem item = menu.findItem(R.id.dfempty);

        if (item != null) {
			//tcLog.d( "showEmptyFields = "+showEmptyFields);
            item.setChecked(showEmptyFields);
        }
        setSelect(modVeld.isEmpty());
		listener.setActionProvider(menu,R.id.dfshare);
        final MenuItem itemDetail = menu.findItem(R.id.dfshare);
		if (itemDetail != null) {
			ShareActionProvider mShareActionProvider = (ShareActionProvider) itemDetail.getActionProvider();
			Intent i = listener.shareTicket(_ticket);
			tcLog.d("item = " + itemDetail + " " + mShareActionProvider + " " + i);
			if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(i);
                mShareActionProvider.setShareHistoryFileName("custom_share_history_detail.xml");
            }
		}
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d( "onOptionsItemSelected " +
        // item.toString());

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
								final int newTicket = Integer.parseInt(input.getText().toString());
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
             refresh_ticket();
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
	public void onFileSelected(final String filename) {
		tcLog.d("ticket = " + _ticket + " filename = " + filename);
/*
		listener.startProgressBar(R.string.uploading);

		new Thread("addAttachment") {
			@Override
			public void run() {
				_ticket.addAttachment(filename, context, new onTicketCompleteListener() {
					@Override
					public void onComplete(Ticket t2) {
						refresh_ticket();
						listener.stopProgressBar();
					}
				});

			}
		}.start();
*/
	}
	
	public void setTicket(int newTicket) {
		ticknr = newTicket;
		display_and_refresh_ticket();
	}

    private void refresh_ticket() {
		if (_ticket != null) {
			listener.refreshTicket(_ticket.getTicketnr());
		}
	}

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        tcLog.d( "_ticket = " + _ticket+ " "+ modVeld);
        if (_ticket != null) {
            savedState.putInt(Const.CURRENT_TICKET, _ticket.getTicketnr());
        } else if (ticknr != -1) {
            savedState.putInt(Const.CURRENT_TICKET, ticknr);
        }
        if (!modVeld.isEmpty()) {
            savedState.putSerializable(MODVELD, modVeld);
        }
        savedState.putBoolean(EMPTYFIELDS, showEmptyFields);
        // tcLog.d( "onSaveInstanceState = " + savedState);
    }

    private String toonTijd(final JSONObject v) {
        try {
            return ISO8601.toCalendar(v.getJSONArray("__jsonclass__").getString(1) + "Z").getTime().toString();
        } catch (final Exception e) {
            tcLog.e( "Error converting time", e);
            return "";
        }
    }

    private String getMimeType(String url) {
        return URLConnection.guessContentTypeFromName(url);
    }

    private void displayTicket() {
        tcLog.d( "ticket = " + _ticket);
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
                tickText.setText(String.format(Locale.US,"Ticket %d",_ticket.getTicketnr()));
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
                        selectField("summary", ((TextView) view).getText().toString(), tickText);
                        tcLog.d( "tickText modVeld = " + modVeld);
                        return true;
                    }
                });
            }
//            final ArrayList<String> fields = tm.velden();
//            final int count = fields.size();

			for (final String veld : tm.velden()) {

                try {
                    modifiedString ms = null;
					
//					tcLog.d( "showEmptyFields = "+showEmptyFields);

                    if (skipFields.contains(veld)) {
                    } else if (timeFields.contains(veld)){
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
                                    toonTijd(cmt.getJSONObject(0)) + " - " + cmt.getString(1) + " - " + cmt.getString(4)));
                        }
                    } catch (final JSONException e) {
                        tcLog.e( "JSONException in displayTicket loading history");
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
                                toonTijd(bijlage.getJSONObject(3)) + " - " + bijlage.getString(4) + " - " + bijlage.getString(0)
                                + " - " + bijlage.getString(1)));

                    } catch (final JSONException e) {
                        tcLog.e( "JSONException in displayTicket loading attachments", e);
                    }
                }
            }
            listView.setOnItemLongClickListener(this);
            listView.setOnItemClickListener(this);
            final ModifiedStringArrayAdapter dataAdapter = new ModifiedStringArrayAdapter(context, R.layout.ticket_list, values);

            listView.setAdapter(dataAdapter);
        }
    }

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		final modifiedString t = (modifiedString) parent.getItemAtPosition(position);

		tcLog.d( "position = " + position);
		if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
			return false;
		} else if (t.length() >= 8 && "comment:".equals(t.substring(0, 8))) {
			showAlertBox(R.string.notpossible,R.string.nocomment,null);
		} else {
			final String[] parsed = t.split(":", 2);

			selectField(parsed[0], parsed[1].trim(), null);
		}
		return true;
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final modifiedString t = (modifiedString) parent.getItemAtPosition(position);

		tcLog.d( "position = " + position);
		if (t.length() >= 8 && "bijlage ".equals(t.substring(0, 8))) {
			final int d = t.indexOf(":");
			final int bijlagenr = Integer.parseInt(t.substring(8, d));

			selectBijlage(bijlagenr);
		}
	}
		
	private void selectField(final String veld, final String waarde, final View dataView) {
		if (Arrays.asList(notModified).contains(veld)) {
			showAlertBox(R.string.notpossible,R.string.notchange,null);
		} else if (Arrays.asList(isStatusUpd).contains(veld)) {
			listener.onUpdateTicket(_ticket);
			didUpdate = true;
		} else {
			final TicketModelVeld tmv = tm.getVeld(veld);
			final LayoutInflater inflater = LayoutInflater.from(context);

			final RelativeLayout ll = (RelativeLayout) inflater.inflate(
					tmv.options() == null ? R.layout.field_spec1 : R.layout.field_spec2, null, false);

			((TextView) ll.findViewById(R.id.veldnaam)).setText(veld);
			final EditText et = (EditText) ll.findViewById(R.id.veldwaarde);
			final Spinner spinValue = makeDialogComboSpin(getActivity(), veld, tmv.options(), tmv.optional(), waarde);
			final Button canBut = (Button) ll.findViewById(R.id.cancelpw);
			canBut.setOnClickListener(this);
			final Button storBut = (Button) ll.findViewById(R.id.okBut);
			final ListView parent = (ListView) getView().findViewById(R.id.listofFields);

			if (et != null) {
				et.setText(waarde);
				et.requestFocus();
			}

			try {
				spinValue.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				((LinearLayout) ll.findViewById(R.id.veld)).addView(spinValue);
			} catch (final Exception ignored) {}

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
					if (newValue != null && !newValue.equals(waarde) || newValue == null && waarde != null) {
						if ("summary".equals(veld)) {
							((TextView) dataView).setText(newValue);
							((TextView) dataView).setTextColor(popup_selected_color);
							tcLog.d( "tickText na postInvalidate + " + dataView);
							final String[] parsed = newValue.split(":", 2);

							modVeld.put("summary", parsed[1].trim());
						} else {
							final int pos = ((ModifiedStringArrayAdapter) parent.getAdapter()).getPosition(
									new modifiedString(veld, newValue));

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
					if (pw != null && !listener.isFinishing()) {
						pw.dismiss();
					}
				}
			});

			pw = new PopupWindow(ll,getView().getWidth() * 9 / 10,getView().getHeight() * 4 / 5,true);
			final Drawable drw = new ColorDrawable(ContextCompat.getColor(context,R.color.popup_back));
			drw.setAlpha(context.getResources().getInteger(R.integer.popupAlpha));
			pw.setBackgroundDrawable(drw);
			pw.showAtLocation(parent, Gravity.CENTER, 0, 0);
		}
	}

    public boolean onBackPressed() {
        tcLog.logCall();
		try {
            if (pw.isShowing() && !listener.isFinishing()) {
                pw.dismiss();
                return true;
            }
        } catch (Exception e) {
			if (!modVeld.isEmpty()) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final AlertDialog.Builder alert = new AlertDialog.Builder(context);

						alert
							.setTitle(R.string.warning)
							.setMessage(R.string.unsaved)
							.setPositiveButton(R.string.ja, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									getFragmentManager().popBackStack();
									updateTicket();
								}})
							.setNegativeButton(R.string.nee, null)
							.show();
					}
				});
				return true;
			}
		}
        tcLog.d( "returned false");
		return false;
    }

    private void selectBijlage(final int bijlagenr) {
        listener.startProgressBar(R.string.downloading);
/* TODO
        new Thread() {
            @Override
            public void run() {
                try {
                    final String filename = _ticket.getAttachmentFile(bijlagenr - 1);
                    final String mimeType = getMimeType(filename);

                    _ticket.getAttachment(filename, new onAttachmentCompleteListener() {
                        @Override
                        public void onComplete(final byte[] filedata) {
                            // tcLog.d("onComplete filedata = "
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

                                // tcLog.d( "file = "
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
                                tcLog.w( context.getString(R.string.ioerror) + ": " + filename, e);
								showAlertBox(R.string.notfound,R.string.sdcardmissing,null);
                            } finally {
								listener.stopProgressBar();
                            }
                        }
                        ;
                    });

                } catch (final JSONException e) {
                    tcLog.e( "JSONException fetching attachment", e);
                }
            }
        }.start();
*/
    }

    private void updateTicket() {
        tcLog.logCall();
		try {
			listener.startProgressBar(R.string.saveupdate);
			listener.updateTicket(_ticket,"leave", "", null, null, sendNotification,  modVeld);
			modVeld.clear();
			listener.stopProgressBar();
			displayTicket();
//			listener.refreshOverview();
//			context.runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					setSelect(true);
//					refresh_ticket();
//				}
//			});
		} catch (final Exception e) {
			tcLog.e( "Exception during update", e);
			showAlertBox(R.string.upderr,R.string.storerrdesc,e.getMessage());
		}
/*
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
                    tcLog.e( "Exception during update", e);
                    final String message = e.getMessage();
					showAlertBox(R.string.storerr,R.string.storerrdesc,message);
                }
            }
        }.start();
*/
		}

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		
		if (e1 == null || e2 == null) {
			return false;
		}
		
        int newTicket = -1;

        // tcLog.d("onFling e1 = "+e1+", e2 = "+e2);
		
        if (e1.getX() - e2.getX() > large_move) {
            newTicket = listener.getNextTicket(_ticket.getTicketnr());
        } else if (e1.getX() > drawer_border && (e2.getX() - e1.getX() > large_move)) {
            newTicket =listener.getPrevTicket(_ticket.getTicketnr());
        }
        if (newTicket >= 0 && modVeld.isEmpty()) {
			setTicket(newTicket);
            return true;
        }
        return false;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return gestureDetector != null && gestureDetector.onTouchEvent(ev);
    }
}