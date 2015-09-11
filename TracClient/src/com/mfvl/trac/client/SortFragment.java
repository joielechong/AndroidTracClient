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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class SortFragment extends TracClientFragment implements View.OnClickListener {

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
                    tt.setText(o.getVeld());
                }
                if (direc != null) {
                    direc.setImageResource(o.getRichting() ? R.drawable.upArrow : R.drawable.downArrow);
                    direc.setOnClickListener(new ImageButton.OnClickListener() {
                        @Override
                        public void onClick(View dv) {
                            direc.setImageResource(o.flip() ? R.drawable.upArrow : R.drawable.downArrow);
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
	
	@SuppressWarnings("unchecked")
	private void onMyAttach(Context activity) {
		inputSpec = null;
			
        final Bundle args = getArguments();		
        if (args != null) {
            if (args.containsKey(Const.SORTLISTNAME)) {
				inputSpec = (ArrayList<SortSpec>)args.getSerializable(Const.SORTLISTNAME);
            }
        }
        tcLog.d(getClass().getName(), "onMyAttach inputSpec = "+inputSpec);
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
		helpFile = R.string.sorthelpfile;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = " + savedInstanceState);
        return inflater.inflate(R.layout.sort_view, container, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d(getClass().getName(), "onActivityCreated savedInstanceState = " + savedInstanceState );
        ArrayList<SortSpec> outputSpec = null;

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

        final Button storButton = (Button) view.findViewById(R.id.storebutton);
        final ImageButton addButton = (ImageButton) view.findViewById(R.id.addbutton);
        final Spinner addSpinner = (Spinner) view.findViewById(R.id.addspin);
		getScreensize(addSpinner,addButton);

        storButton.setOnClickListener(this);

        tm = listener.getTicketModel();

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
					sortAdapter.add(new SortSpec(veld));
                }
            });
        }

    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.storebutton:
            final ArrayList<SortSpec> outputSpec = sortAdapter.items;
            for (int i = outputSpec.size() - 1; i >= 0; i--) {
                if (outputSpec.get(i).getRichting() == null) {
                    outputSpec.remove(i);
				}
            }
			sendMessageToHandler(TracStart.MSG_SET_SORT,outputSpec);
            getFragmentManager().popBackStack();
			break;
						
			default:
		}
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
