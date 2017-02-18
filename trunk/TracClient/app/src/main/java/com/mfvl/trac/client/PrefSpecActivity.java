/*
 * Copyright (C) 2013 - 2017 Michiel van Loon
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mfvl.mfvllib.MyLog;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

public class PrefSpecActivity extends TcBaseActivity implements ServiceConnection, FragmentManager.OnBackStackChangedListener {
    private static final String FilterFragmentTag = "Filter_Fragment";
    private static final String SortFragmentTag = "Sort_Fragment";
    private static final String LoginFragmentTag = "Login_Fragment";

    private TracClientService mService = null;

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
        MyLog.d(sis);

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
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = new Bundle();
        Fragment ff;

        if (filterAction.equals(action)) {
            tm = TicketModel.getInstance();
            ff = new FilterFragment();
            args.putSerializable(FILTERLISTNAME, parseFilterString(getFilterString()));
            ft.addToBackStack(FilterFragmentTag);
        } else if (sortAction.equals(action)) {
            tm = TicketModel.getInstance();
            ff = new SortFragment();
            args.putSerializable(SORTLISTNAME, parseSortString(getSortString()));
            ft.addToBackStack(SortFragmentTag);
        } else if (loginAction.equals(action)) {
            ff = new TracLoginFragment();
            ft.addToBackStack(LoginFragmentTag);
        } else {
            throw new RuntimeException("unkown action : " + action);
        }
        ff.setArguments(args);
        ft.add(R.id.displayList, ff);
        ft.commit();
        bindService(new Intent(this,
                        TracClientService.class).setAction(getServiceAction()),
                this,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        MyLog.logCall();
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = ((TracClientService.TcBinder) service).getService();
        MyLog.d("mConnection mService = " + mService);
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            MyLog.d(frag);
            if (frag instanceof TracClientFragment) {
                ((TracClientFragment) frag).onServiceConnected();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        MyLog.d("className = " + className);
        mService = null;
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            MyLog.d(frag);
            if (frag instanceof TracClientFragment) {
                ((TracClientFragment) frag).onServiceDisconnected();
            }
        }
    }

    @Override
    public TracClientService getService() {
        return mService;
    }

    @Override
    public void onBackStackChanged() {
        MyLog.logCall();
        final int depth = getSupportFragmentManager().getBackStackEntryCount();

        MyLog.d("depth = " + depth);
        if (depth == 0) {
            finish();
        }
    }
}
