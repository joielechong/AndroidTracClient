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
			// tcLog.d(getClass().getName(),"getView: "+position+" "+convertView+" "+parent);
            View v = convertView;

            if (v == null) {
                final LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                v = vi.inflate(R.layout.sort_spec, parent, false);
            }
			v.setTag(position);
            ImageButton sortup = (ImageButton) v.findViewById(R.id.sortup);
            ImageButton sortdown = (ImageButton) v.findViewById(R.id.sortdown);
            TextView tt = (TextView) v.findViewById(R.id.sortfield);
            ImageButton direc = (ImageButton) v.findViewById(R.id.sortdirec);
            SortSpec sortItem = items.get(position);

            if (sortItem != null) {
                if (tt != null) {
                    tt.setText(sortItem.getVeld());
                }
                if (direc != null) {
                    direc.setImageResource(sortItem.getRichting() ? R.drawable.upArrow : R.drawable.downArrow);
                }

                if (sortup != null) {
                    sortup.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                }

                if (sortdown != null) {
                    sortdown.setVisibility(position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);
                }
            }
            return v;
        }
		
		private int getSortPosition(View dv) {
			return (Integer)((View)dv.getParent()).getTag();
		}

		public void sortDirect(View dv) {
			((ImageButton)dv).setImageResource(items.get(getSortPosition(dv)).flip() ? R.drawable.upArrow : R.drawable.downArrow);
		}
		
		public void sortDown(View dv) {
			int position = getSortPosition(dv);
			if (position < items.size() - 1) {
				final SortSpec o1 = items.get(position);
				final SortSpec o2 = items.get(position + 1);

				items.set(position + 1, o1);
				items.set(position, o2);
				notifyDataSetChanged();
			}
		}

		public void sortUp(View dv) {
			int position = getSortPosition(dv);			
			if (position > 0) {
				final SortSpec o1 = items.get(position);
				final SortSpec o2 = items.get(position - 1);

				items.set(position - 1, o1);
				items.set(position, o2);
				notifyDataSetChanged();
			}
		}

		public void delItem(View dv) {
			items.remove(items.get(getSortPosition(dv)));
			notifyDataSetChanged();
		}
    }

	private final static String inputSpecText = "inputSpec";
    private final static String outputSpecText = "outputSpec";

    private TicketModel tm;
    private ArrayList<SortSpec> inputSpec = null;
    public SortAdapter sortAdapter = null;
	private Spinner addSpinner = null;
	private ImageButton addButton = null;
	
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
        tm = listener.getTicketModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = " + savedInstanceState);
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

        addButton = (ImageButton) view.findViewById(R.id.addbutton);
        addSpinner = (Spinner) view.findViewById(R.id.addspin);
		getScreensize(addSpinner,addButton);

        if (addButton != null && addSpinner != null) {
            addSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, tm.velden()));
        }

    }
	
	public void addField(View v) {
		final String veld = tm.velden().get((int) addSpinner.getSelectedItemId());
		// tcLog.d(this.getClass().getName(), "addButton " + veld);
		sortAdapter.add(new SortSpec(veld));
 	}
	
	public void performStore(View v) {
		final ArrayList<SortSpec> outputSpec = sortAdapter.items;
		for (int i = outputSpec.size() - 1; i >= 0; i--) {
			if (outputSpec.get(i).getRichting() == null) {
				outputSpec.remove(i);
			}
		}
		sendMessageToHandler(TracStart.MSG_SET_SORT,outputSpec);
		getFragmentManager().popBackStack();
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

	public void sortDirect(View v) {
		sortAdapter.sortDirect(v);
	}

	public void sortDown(View v) {
		sortAdapter.sortDown(v);
	}

	public void sortUp(View v) {
		sortAdapter.sortUp(v);
	}

	public void delItem(View v) {
		sortAdapter.delItem(v);
	}

}
