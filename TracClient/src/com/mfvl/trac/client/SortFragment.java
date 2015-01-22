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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.mfvl.trac.client.util.tcLog;

public class SortFragment extends TracClientFragment {

	private class SortAdapter extends ArrayAdapter<SortSpec> {

		private final ArrayList<SortSpec> items;

		public SortAdapter(Context context, int textViewResourceId, ArrayList<SortSpec> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		public ArrayList<SortSpec> getArray() {
			return items;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.sort_spec, parent, false);
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
					direc.setImageResource(o.richting() ? Const.UPARROW : Const.DOWNARROW);
					direc.setOnClickListener(new ImageButton.OnClickListener() {
						@Override
						public void onClick(View dv) {
							direc.setImageResource(o.flip() ? Const.UPARROW : Const.DOWNARROW);
						}
					});
				}

				if (sortup != null) {
					sortup.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
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
					sortdown.setVisibility(position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);
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

	private final static String inputSpecText = "inputSpec";
	private final static String outputSpecText = "outputSpec";

	private TicketModel tm;
	private ArrayList<SortSpec> inputSpec = null;
	private SortAdapter sortAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// tcLog.d(this.getClass().getName(),
		// "onCreateView savedInstanceState = " + (savedInstanceState == null ?
		// "null" : "not null"));
		final View view = inflater.inflate(R.layout.sort_view, container, false);
		return view;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		ArrayList<SortSpec> outputSpec = null;

		super.onActivityCreated(savedInstanceState);
		tcLog.d(getClass().getName(), "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(inputSpecText)) {
				inputSpec = (ArrayList<SortSpec>) savedInstanceState.getSerializable(inputSpecText);
			}
			if (savedInstanceState.containsKey(outputSpecText)) {
				outputSpec = (ArrayList<SortSpec>) savedInstanceState.getSerializable(outputSpecText);
			}
		}
		final View view = getView();
		final ListView tl = (ListView) view.findViewById(R.id.sortlist);
		if (outputSpec == null) {
			outputSpec = new ArrayList<SortSpec>();
			if (inputSpec != null) {
				for (final SortSpec o : inputSpec) {
					try {
						outputSpec.add((SortSpec) o.clone());
					} catch (final Exception e) {
						outputSpec.add(o);
					}
				}
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
				for (int i = outputSpec.size() - 1; i >= 0; i--) {
					if (outputSpec.get(i).richting() == null) {
						outputSpec.remove(i);
					}
				}
				listener.setSort(outputSpec);
				// getFragmentManager().popBackStackImmediate();
				getFragmentManager().popBackStack();
			}
		});

		final ProgressDialog pb = startProgressBar(R.string.downloading);
		tm = TicketModel.getInstance();
		if (pb != null && !context.isFinishing()) {
			pb.dismiss();
		}

		if (addButton != null && addSpinner != null) {
			// tcLog.d(this.getClass().getName(), "addButton ");
			final ArrayList<String> velden = tm.velden();
			final ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, velden);
			// tcLog.d(this.getClass().getName(), "addButton " + spinAdapter);
			addSpinner.setAdapter(spinAdapter);
			// tcLog.d(this.getClass().getName(), "addButton " + addSpinner);
			addButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v1) {
					final String veld = velden.get((int) addSpinner.getSelectedItemId());
					// tcLog.d(this.getClass().getName(), "addButton " + veld);
					final SortSpec o = new SortSpec(veld);
					sortAdapter.add(o);
					// sortAdapter.notifyDataSetChanged();
				}
			});
		}

	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		tcLog.d(getClass().getName(), "onCreateOptionsMenu");
//		// inflater.inflate(R.menu.ticketlistmenu, menu);
//		super.onCreateOptionsMenu(menu, inflater);
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.sorthelpfile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, false);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void setList(ArrayList<SortSpec> l) {
		// tcLog.d(this.getClass().getName(), "setList l = " + l);
		inputSpec = l;
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState");
		if (inputSpec != null) {
			tcLog.d(this.getClass().getName(), "onSaveInstanceState inputSpec = " + inputSpec);
			savedState.putSerializable(inputSpecText, inputSpec);
		}
		if (sortAdapter != null) {
			final ArrayList<SortSpec> outputSpec = sortAdapter.getArray();
			if (outputSpec != null) {
				tcLog.d(this.getClass().getName(), "onSaveInstanceState outputSpec = " + outputSpec);
				savedState.putSerializable(outputSpecText, outputSpec);
			}
		}
		tcLog.d(this.getClass().getName(), "onSaveInstanceState = " + savedState);
	}

}
