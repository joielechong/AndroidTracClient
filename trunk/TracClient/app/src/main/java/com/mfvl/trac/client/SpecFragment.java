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
import android.view.View;
import android.widget.ListView;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;

abstract public class SpecFragment<T extends Spec> extends TracClientFragment {
    private final static String inputSpecText = "inputSpec";
    private final static String outputSpecText = "outputSpec";
    ArrayList<T> outputSpec = null;
    ListView listView;
    View currentView;
    private ArrayList<T> inputSpec;

    protected abstract String keyName();

    @SuppressWarnings("unchecked")
    void onMyAttach(Context activity) {
//        MyLog.d("keyName = " + keyName());
        super.onMyAttach(activity);
        inputSpec = null;

        final Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(keyName())) {
                inputSpec = (ArrayList<T>) args.getSerializable(keyName());
            }
        }
        //MyLog.d("onMyAttach inputSpec = "+inputSpec);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //MyLog.d("savedInstanceState = " + savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        MyLog.d("view = " + view + " savedInstanceState = " + savedInstanceState);
        currentView = view;
        listView = (ListView) view.findViewById(R.id.itemlist);
//        MyLog.d("view = " + view + " listView = " + listView + " savedInstanceState = " + savedInstanceState);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
//        MyLog.logCall();
        if (inputSpec != null) {
//            MyLog.d("inputSpec = " + inputSpec);
            savedState.putSerializable(inputSpecText, inputSpec);
        }

        ItemsAdapter<T> adapter = (ItemsAdapter<T>) listView.getAdapter();

        if (adapter != null) {
            final ArrayList<T> outputSpec = adapter.getItems();

            if (outputSpec != null) {
//                MyLog.d("outputSpec = " + outputSpec);
                savedState.putSerializable(outputSpecText, outputSpec);
            }
        }
//        MyLog.d("super savedState = " + savedState);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MyLog.d("savedInstanceState = " + savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(inputSpecText)) {
                inputSpec = (ArrayList<T>) savedInstanceState.getSerializable(inputSpecText);
            }
            if (savedInstanceState.containsKey(outputSpecText)) {
                outputSpec = (ArrayList<T>) savedInstanceState.getSerializable(outputSpecText);
            }
        }

        if (outputSpec == null) {
            outputSpec = new ArrayList<>();
            if (inputSpec != null) {
                for (final T o : inputSpec) {
                    o.setEdit(false);
                    try {
                        outputSpec.add((T) o.clone());
                    } catch (final Exception e) {
                        outputSpec.add(o);
                    }
                }
            }
        }
    }
}
