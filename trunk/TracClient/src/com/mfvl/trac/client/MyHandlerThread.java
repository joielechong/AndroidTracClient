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

import android.annotation.SuppressLint;
import android.os.HandlerThread;

public class MyHandlerThread extends HandlerThread {

    MyHandlerThread(String name) {
        super(name);
    }
		
    MyHandlerThread(String name, int priority) {
        super(name, priority);
    }

    @SuppressLint("NewApi")
    public boolean tcQuitSafely() {
        // QuitSafely, if older than 18 then fall through to quit
        return (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 ? super.quitSafely() : super.quit());
    }
		
    @Override
    public void run() {
        try {
            super.run();
        } catch (Exception e) {
            tcLog.e(getClass().getName() + "." + getThreadId(), "Exception in HandlerThread Run - " +getName(), e);
        }
    }
}

