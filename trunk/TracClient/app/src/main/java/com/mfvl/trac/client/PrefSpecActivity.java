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

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.mfvl.mfvllib.MyLog;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

public class PrefSpecActivity extends TcBaseActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MyLog.logCall();
        getMenuInflater().inflate(R.menu.preferences, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MyLog.logCall();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);

        String filterAction = getString(R.string.editFilterAction);
        String sortAction = getString(R.string.editSortAction);
        String loginAction = getString(R.string.editLoginAction);

        setContentView(R.layout.app_bar_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.traclogo);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        MyLog.d(intent);
        String action = intent.getAction();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = new Bundle();
        Fragment ff;

        if (filterAction.equals(action)) {
            tm = StdTicketModel.getInstance();
            ff = new FilterFragment();
            args.putSerializable(FILTERLISTNAME, parseFilterString(getFilterString()));
        } else if (sortAction.equals(action)) {
            tm = StdTicketModel.getInstance();
            ff = new SortFragment();
            args.putSerializable(SORTLISTNAME, parseSortString(getSortString()));
        } else if (loginAction.equals(action)) {
            ff = new TracLoginFragment();
        } else {
            throw new RuntimeException("unkown action : " + action);
        }
        ff.setArguments(args);
        ft.add(R.id.displayList, ff);
        ft.commit();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean processMessage(Message msg) {
        MyLog.d(msg);
        Intent intent;
        switch (msg.what) {
            case MSG_DONE:
                finish();
                break;

            case MSG_SET_FILTER:
                Iterable<FilterSpec> filter = (Iterable<FilterSpec>) msg.obj;
                String filterString = TextUtils.join("&", filter);
                storeFilterString(filterString);
                intent = new Intent(PERFORM_FILTER);
                intent.putExtra(FILTERLISTNAME, filterString);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                finish();
                break;

            case MSG_SET_SORT:
                Iterable<SortSpec> sort = (Iterable<SortSpec>) msg.obj;
                String sortString = TextUtils.join("&", sort);
                storeSortString(sortString);
                intent = new Intent(PERFORM_SORT);
                intent.putExtra(SORTLISTNAME, sortString);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                finish();
                break;

            default:
                return super.processMessage(msg);
        }
        return true;
    }
}
