/*
 * Copyright (C) 2013-2015 Michiel van Loon
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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static com.mfvl.trac.client.Const.*;

public class SortFragment extends SpecFragment<SortSpec> {

    private SortAdapter sortAdapter = null;
    private Spinner addSpinner = null;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
//        tcLog.d("onAttach(C)");
        onMyAttach(SORTLISTNAME);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        tcLog.d("onAttach(A)");
        onMyAttach(SORTLISTNAME);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.storebutton:
                final ArrayList<SortSpec> items = sortAdapter.items;
                for (int i = items.size() - 1; i >= 0; i--) {
                    if (items.get(i).getRichting() == null) {
                        items.remove(i);
                    }
                }
                sendMessageToHandler(MSG_SET_SORT, items);
                getFragmentManager().popBackStack();
                break;

            case R.id.addbutton:
                final String veld = tm.velden().get((int) addSpinner.getSelectedItemId());
                // tcLog.d(     "addButton " + veld);
                sortAdapter.add(new SortSpec(veld));
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d("savedInstanceState = " + savedInstanceState);
        return inflater.inflate(R.layout.sort_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);  // vult inputSpec en outputSpec
        tcLog.d("savedInstanceState = " + savedInstanceState);
        helpFile = R.string.sorthelpfile;

        sortAdapter = new SortAdapter(context, outputSpec);
        listView.setAdapter(sortAdapter);

        ImageButton addButton = (ImageButton) currentView.findViewById(R.id.addbutton);
        addButton.setOnClickListener(this);
        setListener(R.id.storebutton);
        addSpinner = (Spinner) currentView.findViewById(R.id.addspin);
        getScreensize(addSpinner, addButton);

        if (addSpinner != null) {
            //tcLog.d("before setAdapter, tm = "+tm+ " addSspinner = "+ addSpinner+" context = "+context);
            addSpinner.setAdapter(
                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, tm.velden()));
        }

    }

    private class SortAdapter extends SpecAdapter<SortSpec> implements View.OnClickListener {
        public SortAdapter(Context context, ArrayList<SortSpec> items) {
            super(context, R.layout.sort_spec, items);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            // tcLog.d(    "getView: "+position+" "+convertView+" "+parent);
            View v = convertView;

            if (v == null) {
                v = LayoutInflater.from(context).inflate(R.layout.sort_spec, parent, false);
            }
            v.setTag(position);
            ImageButton sortup = (ImageButton) v.findViewById(R.id.sortup);
            sortup.setOnClickListener(this);
            ImageButton sortdown = (ImageButton) v.findViewById(R.id.sortdown);
            sortdown.setOnClickListener(this);
            TextView tt = (TextView) v.findViewById(R.id.sortfield);
            ImageButton direc = (ImageButton) v.findViewById(R.id.sortdirec);
            direc.setOnClickListener(this);
            setListener(R.id.delitem, v, this);

            SortSpec sortItem = items.get(position);

            if (sortItem != null) {
                if (tt != null) {
                    tt.setText(sortItem.getVeld());
                }
                direc.setImageResource(
                        sortItem.getRichting() ? R.drawable.upArrow : R.drawable.downArrow);

                sortup.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);

                sortdown.setVisibility(
                        position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            }
            return v;
        }

        public void onClick(View dv) {
            int position = getSortPosition(dv);
            SortSpec sortItem = items.get(position);
            tcLog.d(dv.toString() + " " + position + " " + sortItem);
            switch (dv.getId()) {
                case R.id.sortup:
                    if (position > 0) {
                        final SortSpec o2 = items.get(position - 1);

                        items.set(position - 1, sortItem);
                        items.set(position, o2);
                        notifyDataSetChanged();
                    }
                    break;

                case R.id.sortdown:
                    if (position < items.size() - 1) {
                        final SortSpec o2 = items.get(position + 1);

                        items.set(position + 1, sortItem);
                        items.set(position, o2);
                        notifyDataSetChanged();
                    }
                    break;

                case R.id.sortdirec:
                    ((ImageButton) dv).setImageResource(
                            sortItem.flip() ? R.drawable.upArrow : R.drawable.downArrow);
                    break;

                case R.id.delitem:
                    remove(sortItem);
                    notifyDataSetChanged();
                    break;
            }
        }

        private int getSortPosition(View dv) {
            return (Integer) ((View) dv.getParent()).getTag();
        }
    }
}
