/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;

import static com.mfvl.trac.client.Const.*;

public class SortFragment extends SpecFragment<SortSpec> implements HelpInterface, SpecInterface {

    private SortAdapter sortAdapter = null;
    private Spinner addSpinner = null;

    @Override
    public String keyName() {
        return SORTLISTNAME;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.storebutton:
                sendMessageToHandler(MSG_SET_SORT, sortAdapter.items);
                getFragmentManager().popBackStack();
                break;

            case R.id.addbutton:
                final String veld = tm.velden().get((int) addSpinner.getSelectedItemId());
                //MyLog.d("addButton " + veld);
                sortAdapter.add(new SortSpec(veld));
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        MyLog.d("savedInstanceState = " + savedInstanceState);
        return inflater.inflate(R.layout.sort_view, container, false);
    }

    @Override
    public int getHelpFile() {
        return R.string.sorthelpfile;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);  // vult inputSpec en outputSpec
        MyLog.d("savedInstanceState = " + savedInstanceState);

        if (tm == null) {
            MyLog.toast(getString(R.string.notpossible));
            sendMessageToHandler(MSG_DONE, null);
            getFragmentManager().popBackStack();
        } else {
            sortAdapter = new SortAdapter(getActivity(), outputSpec);
            listView.setAdapter(sortAdapter);

            ImageButton addButton = (ImageButton) currentView.findViewById(R.id.addbutton);
            addButton.setOnClickListener(this);
            setListener(R.id.storebutton);
            addSpinner = (Spinner) currentView.findViewById(R.id.addspin);
            getScreensize(addSpinner, addButton);
            addSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, tm.velden()));
        }
    }

    private class SortAdapter extends SpecAdapter<SortSpec> implements View.OnClickListener {
        SortAdapter(Context ctx, ArrayList<SortSpec> _items) {
            super(ctx, R.layout.sort_spec, _items);
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull final ViewGroup parent) {
            // MyLog.d("getView: "+position+" "+convertView+" "+parent);
            View v = convertView;

            if (v == null) {
                v = LayoutInflater.from(getActivity()).inflate(R.layout.sort_spec, parent, false);
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

        @Override
        public void onClick(View dv) {
            int position = (Integer) ((View) dv.getParent()).getTag();
            SortSpec sortItem = items.get(position);
//            MyLog.d(dv.toString() + " " + position + " " + sortItem);
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
                    ((ImageView) dv).setImageResource(
                            sortItem.flip() ? R.drawable.upArrow : R.drawable.downArrow);
                    break;

                case R.id.delitem:
                    remove(sortItem);
                    notifyDataSetChanged();
                    break;
            }
        }
    }
}
