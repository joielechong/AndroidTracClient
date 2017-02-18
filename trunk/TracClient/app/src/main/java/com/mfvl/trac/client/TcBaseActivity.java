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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.mfvl.mfvllib.MyLog;
import com.mfvl.mfvllib.MyProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

abstract class TcBaseActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        InterFragmentListener {
    private static final int REQUEST_CODE_WRITE_EXT = 175;
    static boolean debug = false; // disable menuoption at startup
    TicketModel tm = null;
    private ProgressDialog progressBar = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.logCall();
        TracGlobal.initialize(this);
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
        MyLog.logCall();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            TracGlobal.setCanWriteSD(true);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permissiontitle)
                        .setMessage(R.string.permissiontext)
                        .setCancelable(false)
                        .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                getPermissions();
                            }
                        }).show();
            } else {
                getPermissions();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MyLog.d("requestCode = " + requestCode + " permissions = " + Arrays.asList(
                permissions) + " grantResults = " + Arrays.asList(grantResults));
        if (requestCode == REQUEST_CODE_WRITE_EXT) {
            // If request is cancelled, the result arrays are empty.
            setCanWriteSD((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        MyLog.logCall();
    }

    @SuppressLint("InlinedApi")
    private void getPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_WRITE_EXT);
    }

    @Override
    public ArrayList<FilterSpec> parseFilterString(String filterString) {
        final ArrayList<FilterSpec> filter = new ArrayList<>();

        if (filterString.length() > 0) {
            String[] fs;

            try {
                fs = filterString.split("&");
            } catch (final IllegalArgumentException e) {
                fs = new String[1];
                fs[0] = filterString;
            }
            final String[] operators = getResources().getStringArray(R.array.filter2_choice);

            for (final String f : fs) {
                String veld = null;
                for (int i = operators.length - 1; i >= 0 && veld == null; i--) {
                    final String op = operators[i];
                    final int index = f.indexOf(op);

                    if (index > 0) {
                        veld = f.substring(0, index);
                        final String waarde = f.substring(index + op.length());
                        filter.add(new FilterSpec(veld, op, waarde));
                    }
                }
            }
        }
        return filter;
    }

    @Override
    public ArrayList<SortSpec> parseSortString(String sortString) {
        final ArrayList<SortSpec> sl = new ArrayList<>();

        if (sortString.length() > 0) {
            String[] sort;

            try {
                sort = sortString.split("&");
            } catch (final IllegalArgumentException e) {
                sort = new String[1];
                sort[0] = sortString;
            }
            for (int i = 0; i < sort.length; i++) {
                final String s = sort[i];

                if (s.startsWith("order=")) {
                    final String veld = s.substring(6);
                    boolean richting = true;

                    if (i + 1 < sort.length) {
                        final String s1 = sort[i + 1];

                        if ("desc=1".equalsIgnoreCase(s1)) {
                            richting = false;
                            i++;
                        }
                    }
                    sl.add(new SortSpec(veld, richting));
                }
            }
        }
        return sl;
    }

    @Override
    public void enableDebug() {
        // MyLog.d("enableDebug");
        debug = true;
        invalidateOptionsMenu();
        MyLog.toast("Debug enabled");
    }

    @Override
    public boolean debugEnabled() {
        return debug;
    }

    @Override
    public void onChooserSelected(OnFileSelectedListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onUpdateTicket(Ticket ticket) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void refreshOverview() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void startProgressBar(int resid) {
        startProgressBar(getString(resid));
    }

    void startProgressBar(final String message) {
        MyLog.d(message);
        try {
            synchronized (this) {
                //MyLog.d("handleMessage msg = START_PROGRESSBAR string = "+message);
                if (progressBar == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar = new MyProgressBar(TcBaseActivity.this, message);
                        }
                    });
                }
            }
        } catch (NullPointerException e) {
            MyLog.e("NullPointerException", e);
        }
    }

    @Override
    public void stopProgressBar() {
        MyLog.logCall();
        try {
            synchronized (this) {
                // MyLog.d("handleMessage msg = STOP_PROGRESSBAR "+progressBar+" "+TracStart.this.isFinishing());
                if (progressBar != null) {
                    if (!isFinishing()) {
                        progressBar.dismiss();
                    }
                    progressBar = null;
                }
            }
        } catch (Exception e) {
            MyLog.e("Exception", e);
        }
    }

    @Override
    public TicketListAdapter getAdapter() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void getTicket(int ticknr, OnTicketLoadedListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void refreshTicket(int ticknr) {
        throw new RuntimeException("not implemented");
    }


    String getTopFragment() {
        String retval;
        try {
            int bs = getSupportFragmentManager().getBackStackEntryCount();
            retval = getSupportFragmentManager().getBackStackEntryAt(bs - 1).getName();
        } catch (Exception e) {
            retval = null;
        }
        MyLog.d(retval);
        return retval;
    }

    @Override
    public void showAlertBox(final int titleres, final CharSequence message) {
        MyLog.d("titleres = " + titleres + " : " + getString(titleres));
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog ad = new AlertDialog.Builder(TcBaseActivity.this)
                            .setTitle(titleres)
                            .setMessage(message)
                            .setPositiveButton(R.string.oktext, null)
                            .create();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //MyLog.d("dismiss");
                            try {
                                ad.dismiss();
                            } catch (Exception ignored) {
                            }
                        }
                    }, ALERT_TIME);
                    ad.show();
                }
            });
        }
    }

    @Override
    public int getNextTicket(int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getPrevTicket(int i) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getTicketCount() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int getTicketContentCount() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void updateTicket(Ticket t, String action, String comment, String veld, String waarde, boolean notify, Map<String, String> modVeld) throws Exception {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int createTicket(Ticket t, boolean notify) throws Exception {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void listViewCreated() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void getAttachment(Ticket t, String filename, onAttachmentCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addAttachment(Ticket ticket, Uri uri, onTicketCompleteListener oc) {
        throw new RuntimeException("not implemented");
    }
}
