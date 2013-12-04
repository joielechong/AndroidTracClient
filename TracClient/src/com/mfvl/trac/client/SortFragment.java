package com.mfvl.trac.client;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.trac.client.util.SortSpec;

public class SortFragment extends TracClientFragment {

	private static int UPARROW = android.R.drawable.arrow_up_float;
	private static int DOWNARROW = android.R.drawable.arrow_down_float;

	private class SortAdapter extends ArrayAdapter<SortSpec> {

		private final ArrayList<SortSpec> items;

		public SortAdapter(Context context, int textViewResourceId, ArrayList<SortSpec> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.sort_spec, null);
			}
			final ImageButton sortup = (ImageButton) v.findViewById(R.id.sortup);
			final ImageButton sortdown = (ImageButton) v.findViewById(R.id.sortdown);
			final TextView tt = (TextView) v.findViewById(R.id.sortfield);
			final ImageButton direc = (ImageButton) v.findViewById(R.id.sortdirec);
			final SortSpec o = items.get(position);
			final ImageButton sortdel = (ImageButton) v.findViewById(R.id.sortdel);

			if (o != null) {
				if (tt != null) {
					tt.setText(o.veld());
				}
				if (direc != null) {
					if (o.richting()) {
						direc.setImageResource(UPARROW);
					} else {
						direc.setImageResource(DOWNARROW);
					}
					direc.setOnClickListener(new ImageButton.OnClickListener() {
						@Override
						public void onClick(View dv) {
							if (o.flip()) {
								direc.setImageResource(UPARROW);
							} else {
								direc.setImageResource(DOWNARROW);
							}
						}
					});
				}

				if (sortup != null) {
					if (position == 0) {
						sortup.setVisibility(View.INVISIBLE);
					} else {
						sortup.setVisibility(View.VISIBLE);
					}
					sortup.setOnClickListener(new ImageButton.OnClickListener() {
						@Override
						public void onClick(View dv) {
							if (position > 0) {
								final SortSpec o1 = items.get(position);
								final SortSpec o2 = items.get(position - 1);
								items.set(position - 1, o1);
								items.set(position, o2);
								SortAdapter.this.notifyDataSetChanged();
							}

						}
					});
				}

				if (sortdown != null) {
					if (position == items.size() - 1) {
						sortdown.setVisibility(View.INVISIBLE);
					} else {
						sortdown.setVisibility(View.VISIBLE);
					}
					sortdown.setOnClickListener(new ImageButton.OnClickListener() {
						@Override
						public void onClick(View dv) {
							if (position < items.size() - 1) {
								final SortSpec o1 = items.get(position);
								final SortSpec o2 = items.get(position + 1);
								items.set(position + 1, o1);
								items.set(position, o2);
								SortAdapter.this.notifyDataSetChanged();
							}
						}
					});
				}

				if (sortdel != null) {
					sortdel.setOnClickListener(new ImageButton.OnClickListener() {
						@Override
						public void onClick(View dv) {
							items.remove(o);
							SortAdapter.this.notifyDataSetChanged();
						}
					});
				}
			}
			return v;
		}
	}

	private TicketModel tm;
	private ArrayList<SortSpec> inputSpec;
	private SortAdapter sortAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.sort_view, container, false);
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
		final View view = getView();
		final ListView tl = (ListView) view.findViewById(R.id.sortlist);
		final ArrayList<SortSpec> outputSpec = new ArrayList<SortSpec>();
		for (final SortSpec o : inputSpec) {
			try {
				outputSpec.add((SortSpec) o.clone());
			} catch (final Exception e) {
				outputSpec.add(o);
			}
		}
		sortAdapter = new SortAdapter(context, R.layout.sort_spec, outputSpec);
		tl.setAdapter(sortAdapter);

		final Button backButton = (Button) view.findViewById(R.id.backbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);
		final Button addButton = (Button) view.findViewById(R.id.addbutton);
		final Spinner addSpinner = (Spinner) view.findViewById(R.id.addspin);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// getFragmentManager().popBackStackImmediate();
				getFragmentManager().popBackStack();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				final ArrayList<SortSpec> outputSpec = sortAdapter.items;
				Log.i(this.getClass().getName(), "stor onButton outputSpec=" + outputSpec);
				for (int i = outputSpec.size() - 1; i >= 0; i--) {
					if (outputSpec.get(i).richting() == null) {
						outputSpec.remove(i);
					}
				}
				Log.i(this.getClass().getName(), "Store is clicked! " + outputSpec);
				listener.setSort(outputSpec);
				// getFragmentManager().popBackStackImmediate();
				getFragmentManager().popBackStack();
			}
		});

		tm = listener.getTicketModel();

		if (addButton != null) {
			Log.i(this.getClass().getName(), "addButton ");
			final ArrayList<String> velden = tm.velden();
			final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, velden);
			Log.i(this.getClass().getName(), "addButton " + spinAdapter);
			addSpinner.setAdapter(spinAdapter);
			Log.i(this.getClass().getName(), "addButton " + addSpinner);
			addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v1) {
					final String veld = velden.get((int) addSpinner.getSelectedItemId());
					Log.i(this.getClass().getName(), "addButton " + veld);
					final SortSpec o = new SortSpec(veld);
					sortAdapter.add(o);
					sortAdapter.notifyDataSetChanged();
				}
			});
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(this.getClass().getName(), "onStart");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.sorthelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(this.getClass().getName(), "onResume");
	}

	public void setList(ArrayList<SortSpec> l) {
		Log.i(this.getClass().getName(), "setList l = " + l);
		inputSpec = l;
	}

}
