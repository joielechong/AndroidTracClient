/*
 * Copyright (C) 2014,2015 Michiel van Loon
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;

import static com.mfvl.trac.client.Const.*;


public class Refresh extends Activity implements ServiceConnection {

    private RefreshService mService = null;

/*
 * Implementing ServiceConnection 
 *
 */
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        tcLog.d("className = " + className + " service = " + service);
        RefreshService.RefreshBinder binder = (RefreshService.RefreshBinder) service;
        mService = binder.getService();
        mService.send(Message.obtain(null, MSG_REFRESH_LIST));
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        tcLog.d("className = " + className);
        mService = null;
    }

/*
 * Implementing Activity 
 *
 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d("savedInstanceState = " + savedInstanceState);

        try {
            final String action = getIntent().getAction();

            if (action != null) {
                if (action.equalsIgnoreCase(RefreshService.refreshAction)) {
                    bindService(new Intent(this, RefreshService.class), this, Context.BIND_AUTO_CREATE);
                    // tcLog.i("Refresh sent");
                }
            }
        } catch (final Exception e) {
            tcLog.e("Problem consuming action from intent", e);
        }
        finish();
    }

    @Override
    public void onDestroy() {
        tcLog.logCall();
        super.onDestroy();
        try {
            unbindService(this);
        } catch (final Throwable t) {
            tcLog.e("Failed to unbind from the service", t);
        }
    }
}
