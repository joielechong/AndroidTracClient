package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.trac.client.util.FilterSpec;

public class FilterFragment extends TracClientFragment {
	private TicketModel tm;

	private class FilterAdapter extends ArrayAdapter<FilterSpec> {

		private final ArrayList<FilterSpec> items;

		public FilterAdapter(Context context, int textViewResourceId, ArrayList<FilterSpec> input) {
			super(context, textViewResourceId, input);
			items = input;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			Log.i(this.getClass().getName(), "getView pos=" + position + " " + convertView + " " + parent);

			final Resources res = context.getResources();
			final ArrayList<String> operators = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter2_choice)));
			final ArrayList<String> operatornames = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.filter3_choice)));
			final ListView listView = (ListView) parent;

			View v = convertView;
			if (v != null) {	
				LinearLayout lv = (LinearLayout)v;
				Log.i(this.getClass().getName(), "getView convertView.childCount=" + lv.getChildCount());
				for (int i=0;i<((LinearLayout)v).getChildCount();i++){
					Log.i(this.getClass().getName(), "getView convertView.child("+i+") "+lv.getChildAt(i));
				}
			}
			int p;
			if (position >= items.size() || position < 0) {
				p = 0;
			} else {
				p = position;
			}

			final FilterSpec o = items.get(p);
			final TicketModelVeld tmv = tm.getVeld(o.veld());

			Log.i(this.getClass().getName(), "getView pos=" + position + " " + o + " " + tmv);

			final int resid = o.isEdit() ? (tmv.options() == null ? R.layout.filter_spec2 : R.layout.filter_spec3)
					: R.layout.filter_spec1;
			final int curid = convertView == null ? -1 : convertView.getId();
			Log.i(this.getClass().getName(),"getView pos = " + position + " curid = " + curid + " resid=" + resid + " veld = " + o.veld());
			if (curid != resid) {
				final LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(resid, null);
				v.setId(resid); // hack hack
//				listView.requestLayout();
			}
			if (v != null) {	
				LinearLayout lv = (LinearLayout)v;
				Log.i(this.getClass().getName(), "getView convertedView.childCount=" + lv.getChildCount());
				for (int i=0;i<((LinearLayout)v).getChildCount();i++){
					Log.i(this.getClass().getName(), "getView convertedView.child("+i+") "+lv.getChildAt(i));
				}
			}

			listView.invalidate();
			final TextView tt = (TextView) v.findViewById(R.id.filternaam);
			final ImageButton filterEdit = (ImageButton) v.findViewById(R.id.editfilter);
			final ImageButton filterSave = (ImageButton) v.findViewById(R.id.savefilter);
			final ImageButton filterDel = (ImageButton) v.findViewById(R.id.delfilter);
			final Spinner spin = (Spinner) v.findViewById(R.id.filter_choice_spin);
			final EditText et = (EditText) v.findViewById(R.id.filtervaltext);
			final LinearLayout ll = (LinearLayout) v.findViewById(R.id.filtercheck);

			if (o != null) {
				if (o.isEdit()) {
					tt.setText(o.veld());
				} else {
					tt.setText(o.toString());
				}
			}

			if (spin != null) {
				final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
						operatornames);
				spin.setAdapter(spinAdapter);
				spin.setSelection(operators.indexOf(o.operator()));
				spin.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						Log.i(this.getClass().getName(), "onItemSelected " + parent + " " + view + " " + position + " " + id);
						o.setOperator(operators.get(position));
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
			}

			if (et != null) {
				et.setText(o.waarde());
				et.requestFocus();
				et.addTextChangedListener(new TextWatcher() {

					@Override
					public void afterTextChanged(Editable arg0) {
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						Log.i(this.getClass().getName(), "onTextChanged s = " + s);
						o.setWaarde(s.toString());
					}

				});
			}

			if (ll != null && ll.getChildCount() == 0) {
				ll.addView(makeCheckBoxes(o));
			}

			if (filterEdit != null) {
				filterEdit.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v1) {
						Log.i(this.getClass().getName(), "edit onClick " + o.veld() + " parent=" + parent);
						o.setEdit(true);
						listView.invalidateViews();
					}
				});
			}

			if (filterSave != null) {
				filterSave.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v1) {
						Log.i(this.getClass().getName(), "save onClick " + o.veld() + " parent=" + parent);
						o.setEdit(false);
						if (et != null) {
							Log.i(this.getClass().getName(), "value = " + et.getText());
						}
						if (ll != null) {
							Log.i(this.getClass().getName(), "value = " + o.waarde());
						}
						if (spin != null) {
							o.setOperator(operators.get(spin.getSelectedItemPosition()));
						}
						listView.invalidateViews();
					}
				});
			}

			if (filterDel != null) {
				filterDel.setOnClickListener(new ImageButton.OnClickListener() {
					@Override
					public void onClick(View v1) {
						items.remove(o);
						FilterAdapter.this.notifyDataSetChanged();
					}
				});
			}

			return v;
		}
	}

	private ArrayList<FilterSpec> inputSpec;
	private FilterAdapter filterAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.filtermenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.filter_view, container, false);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.i(this.getClass().getName(), "onSaveInstanceState");
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.i(this.getClass().getName(), "onActivityCreated");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		tm = listener.getTicketModel();
		final View view = getView();
		final ListView lv = (ListView) view.findViewById(R.id.filterlist);
		final ArrayList<FilterSpec> outputSpec = new ArrayList<FilterSpec>();
		for (final FilterSpec o : inputSpec) {
			o.setEdit(false);
			try {
				outputSpec.add((FilterSpec) o.clone());
			} catch (final Exception e) {
				outputSpec.add(o);
			}
		}
		filterAdapter = new FilterAdapter(context, android.R.layout.simple_list_item_1, outputSpec);
		lv.setAdapter(filterAdapter);

		final Button backButton = (Button) view.findViewById(R.id.backbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);
		final Button addButton = (Button) view.findViewById(R.id.addbutton);
		final Spinner addSpinner = (Spinner) view.findViewById(R.id.addspin);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStack();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				final ArrayList<FilterSpec> items = filterAdapter.items;
				Log.i(this.getClass().getName(), "stor onButton items=" + items);
				for (int i = items.size() - 1; i >= 0; i--) {
					if (items.get(i).operator() == null || items.get(i).operator().equals("") || items.get(i).waarde() == null
							|| items.get(i).waarde().equals("")) {
						items.remove(i);
					} else {
						items.get(i).setEdit(false);
					}
				}
				listener.setFilter(items);
				getFragmentManager().popBackStack();
			}
		});

		if (addButton != null) {
			Log.i(this.getClass().getName(), "addButton ");
			final ArrayList<String> velden = tm.velden();
			Collections.sort(velden);
			final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, velden);
			Log.i(this.getClass().getName(), "addButton " + spinAdapter);
			addSpinner.setAdapter(spinAdapter);
			Log.i(this.getClass().getName(), "addButton " + addSpinner);
			addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v1) {
					final String veld = velden.get((int) addSpinner.getSelectedItemId());
					Log.i(this.getClass().getName(), "addButton " + veld);
					final FilterSpec o = new FilterSpec(veld, "=", "");
					filterAdapter.add(o);
					filterAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(this.getClass().getName(), "onStart");
		tm = listener.getTicketModel();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.filterhelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void setList(ArrayList<FilterSpec> l) {
		Log.i(this.getClass().getName(), "setList l = " + l);
		inputSpec = l;
	}

	private LinearLayout makeCheckBoxes(final FilterSpec o) {
		final String veldnaam = o.veld();
		final List<Object> waardes = tm.getVeld(veldnaam).options();
		final String w = o.waarde();
		final String op = o.operator();
		final boolean omgekeerd = op != null && op.equals("!=");

		Log.i(this.getClass().getName(), "makeCheckBoxes " + veldnaam + " " + w + " " + omgekeerd);
		final LinearLayout valCheckBoxes = new LinearLayout(context);
		valCheckBoxes.setOrientation(LinearLayout.VERTICAL);
		String[] ws;
		try {
			ws = w == null ? null : w.split("\\|");
		} catch (final IllegalArgumentException e) {
			ws = new String[1];
			ws[0] = w;
		}
		Log.i(this.getClass().getName(), "makeCheckBoxes " + ws);
		Log.i(this.getClass().getName(), "makeCheckBoxes " + waardes);

		for (int i = 0; i < waardes.size(); i++) {
			final CheckBox rb = new CheckBox(context);
			rb.setText((String) waardes.get(i));
			rb.setId(i);
			rb.setChecked(omgekeerd);
			if (w != null) {
				for (final String w1 : ws) {
					if (w1.equals(waardes.get(i))) {
						rb.setChecked(!omgekeerd);
					}
				}
			}
			rb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton cb0, boolean isChecked) {
					Log.i(this.getClass().getName(), "onCheckedChanged " + cb0.getText() + " " + isChecked);
					String temp = null;
					for (int j = 0; j < waardes.size(); j++) {
						final CheckBox cb = (CheckBox) valCheckBoxes.findViewById(j);
						if (cb != null && cb.isChecked()) {
							if (temp == null) {
								temp = cb.getText().toString();
							} else {
								temp += "|" + cb.getText();
							}
						}
					}
					o.setOperator("=");
					o.setWaarde(temp);
				}
			});
			valCheckBoxes.addView(rb);
		}
		return valCheckBoxes;
	}
}
