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

//
// 'login' to TRAC site by sending a system.APIVersion method
//

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.alexd.jsonrpc.JSONRPCException;

import static com.mfvl.trac.client.Const.*;

public class TracLoginFragment extends TracClientFragment
        implements OnItemSelectedListener, OnCheckedChangeListener {

    private static final String NEW_URL = "newURL";
    private static final String NEW_USERNAME = "newUsername";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String bewaarText = "bewaar";
    /**
     * server url
     */
    private String url = null;
    /**
     * username to use on server
     */
    private String username;
    /**
     * password to use on server
     */
    private String password;
    /**
     * flag to indicate that SSL sites can have problems like Self signed certificates
     */
    private boolean sslHack;
    /**
     * flag to ingnore Hostname verification errors in SSL
     */
    private boolean sslHostNameHack;
    /**
     * flag to indicate that the credentials will be stored in the shared preferences
     */
    private boolean bewaren = false;
    private EditText urlView = null;
    private Button verButton = null;
    private Button okButton = null;
    private Button storButton = null;
    private EditText userView = null;
    private EditText pwView = null;
    private CheckBox bewaarBox = null;
    private CheckBox sslHackBox = null;
    private TextView credWarn = null;
    private TextView credWarnSts = null;
    private Spinner loginSpinner = null;
    private Cursor pdbCursor = null;
    private ProfileDatabaseHelper pdb = null;
    private String SelectedProfile = null;
    private final TextWatcher checkUrlInput = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }


        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            credWarn.setVisibility(View.GONE);
            credWarnSts.setVisibility(View.GONE);
            verButton.setEnabled(false);
            okButton.setEnabled(false);
            storButton.setEnabled(false);
            if (s != null && s.length() != 0) {
                verButton.setEnabled(true);
                checkHackBox(s.toString());
            }
            SelectedProfile = null;
        }
    };
    private final TextWatcher checkUserPwInput = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            credWarn.setVisibility(View.GONE);
            credWarnSts.setVisibility(View.GONE);
            verButton.setEnabled(true);
            okButton.setEnabled(false);
            storButton.setEnabled(false);
            SelectedProfile = null;
        }
    };
    private int debugcounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d("savedInstanceState = " + savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d("savedInstanceState = " + savedInstanceState);
        // tcLog.d("container = " + (container == null ? "null" : "not null"));
        if (container == null) {
            return null;
        }
        return inflater.inflate(R.layout.traclogin, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tcLog.logCall();
        urlView = (EditText) view.findViewById(R.id.trac_URL);
        userView = (EditText) view.findViewById(R.id.trac_User);
        pwView = (EditText) view.findViewById(R.id.trac_Pw);
        bewaarBox = (CheckBox) view.findViewById(R.id.bewaren);
        okButton = (Button) view.findViewById(R.id.okBut);
        okButton.setOnClickListener(this);
        verButton = (Button) view.findViewById(R.id.verBut);
        verButton.setOnClickListener(this);
        storButton = (Button) view.findViewById(R.id.storebutton);
        storButton.setOnClickListener(this);
        credWarn = (TextView) view.findViewById(R.id.connWarn);
        credWarnSts = (TextView) view.findViewById(R.id.connWarnSts);
        sslHackBox = (CheckBox) view.findViewById(R.id.sslHack);
        loginSpinner = (Spinner) view.findViewById(R.id.loginspinner);

        pdb = new ProfileDatabaseHelper(context);
        pdb.open();
        pdbCursor = pdb.getProfiles(true);
        if (pdbCursor.getCount() < 2) {
            loginSpinner.setVisibility(View.GONE);
        } else {
            final String[] columns = new String[]{"name"};
            final int[] to = new int[]{android.R.id.text1};

            loginSpinner.setVisibility(View.VISIBLE);
            final SimpleCursorAdapter adapt = new SimpleCursorAdapter(context,
                    android.R.layout.simple_spinner_dropdown_item,
                    pdbCursor,
                    columns, to,
                    CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            loginSpinner.setAdapter(adapt);

            loginSpinner.setOnItemSelectedListener(this);
        }
    }

    int getHelpFile() {
        return R.string.loginhelpfile;
    }

    @Override
    public void onResume() {
        tcLog.logCall();
        super.onResume();
        urlView.addTextChangedListener(checkUrlInput);
        userView.addTextChangedListener(checkUserPwInput);
        pwView.addTextChangedListener(checkUserPwInput);
        checkHackBox(urlView.getText().toString());
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putString(NEW_URL, urlView.getText().toString());
        savedState.putString(NEW_USERNAME, userView.getText().toString());
        savedState.putString(NEW_PASSWORD, pwView.getText().toString());
        savedState.putBoolean(CURRENT_SSLHACK, sslHackBox.isChecked());
        savedState.putBoolean(CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        savedState.putBoolean(bewaarText, bewaarBox.isChecked());
        tcLog.d(" savedState = " + savedState);
    }

    @Override
    public void onPause() {
        tcLog.logCall();
        urlView.removeTextChangedListener(checkUrlInput);
        userView.removeTextChangedListener(checkUserPwInput);
        pwView.removeTextChangedListener(checkUserPwInput);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        tcLog.logCall();
        loginSpinner.setAdapter(null);
        if (pdbCursor != null) {
            pdbCursor.close();
        }
        pdb.close();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        tcLog.logCall();
        inflater.inflate(R.menu.tracloginmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        tcLog.logCall();
        super.onPrepareOptionsMenu(menu);
        final MenuItem importItem = menu.findItem(R.id.importprofiles);
        final MenuItem exportItem = menu.findItem(R.id.exportprofiles);
        tcLog.d("canWriteSD = " + listener.getCanWriteSD());
        importItem.setEnabled(listener.getCanWriteSD());
        exportItem.setEnabled(listener.getCanWriteSD());
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (arg1 != null) {
            SelectedProfile = ((CheckedTextView) arg1).getText().toString();
            if (arg2 > 0) { // pos 0 is empty
                final LoginProfile prof = pdb.getProfile(SelectedProfile);

                if (prof != null) {
                    urlView.removeTextChangedListener(checkUrlInput);
                    userView.removeTextChangedListener(checkUserPwInput);
                    pwView.removeTextChangedListener(checkUserPwInput);

                    url = prof.getUrl();
                    urlView.setText(url);
                    sslHack = prof.getSslHack();
                    sslHackBox.setChecked(sslHack);
                    username = prof.getUsername();
                    userView.setText(username);
                    password = prof.getPassword();
                    pwView.setText(password);
                    checkHackBox(url);

                    urlView.addTextChangedListener(checkUrlInput);
                    userView.addTextChangedListener(checkUserPwInput);
                    pwView.addTextChangedListener(checkUserPwInput);
                    verButton.setEnabled(true);
                    okButton.setEnabled(false);
                    storButton.setEnabled(false);
                    credWarn.setVisibility(View.GONE);
                } else {
                    showAlertBox(R.string.notfound, R.string.loadprofiletext, SelectedProfile);
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    private void checkHackBox(String s) {
        if (sslHackBox != null && s != null) {
            sslHackBox.setVisibility(s.length() >= 6 && s.substring(0, 6).equals(
                    "https:") ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d("savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));

        String currentUsername;
        boolean currentSslHostNameHack;
        boolean currentSslHack;
        String currentPassword;
        String currentUrl;
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString(NEW_URL);
            currentUsername = savedInstanceState.getString(NEW_USERNAME);
            currentPassword = savedInstanceState.getString(NEW_PASSWORD);
            currentSslHack = savedInstanceState.getBoolean(CURRENT_SSLHACK);
            currentSslHostNameHack = savedInstanceState.getBoolean(CURRENT_SSLHOSTNAMEHACK);
            bewaren = savedInstanceState.getBoolean(bewaarText);
        } else {
            currentUrl = fragmentArgs.getString(CURRENT_URL);
            currentUsername = fragmentArgs.getString(CURRENT_USERNAME);
            currentPassword = fragmentArgs.getString(CURRENT_PASSWORD);
            currentSslHack = fragmentArgs.getBoolean(CURRENT_SSLHACK);
            currentSslHostNameHack = fragmentArgs.getBoolean(CURRENT_SSLHOSTNAMEHACK);
        }

        if (url == null) {
            url = currentUrl;
            username = currentUsername;
            password = currentPassword;
            sslHack = currentSslHack;
            sslHostNameHack = currentSslHostNameHack;
        }

        urlView.setText(url);
        userView.setText(username);
        pwView.setText(password);
        sslHackBox.setChecked(sslHack);
        boolean buttonsOn = !(url == null || url.length() == 0);
        verButton.setEnabled(buttonsOn);
        okButton.setEnabled(buttonsOn);
        storButton.setEnabled(buttonsOn);
        checkHackBox(url == null ? "" : url);
        bewaarBox.setChecked(bewaren);
        bewaarBox.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (++debugcounter == 6) {
            listener.enableDebug();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d("item=" + item.getTitle());
        switch (item.getItemId()) {
            case R.id.exportprofiles:
                try {
                    pdb.open();
                    pdb.writeXML(context.getString(R.string.app_name));
                    showAlertBox(R.string.completed, R.string.xmlwritecompleted, null);
                } catch (final Exception e) {
                    tcLog.e("Export failed", e);
                    showAlertBox(R.string.failed, 0, e.getMessage());
                }
                break;

            case R.id.importprofiles:
                try {
                    pdb.open();
                    pdb.readXML(context.getString(R.string.app_name));
                    swapSpinnerAdapter();
                    showAlertBox(R.string.completed, R.string.xmlreadcompleted, null);
                } catch (final Exception e) {
                    tcLog.e("Import failed", e);
                    showAlertBox(R.string.failed, 0, e.getMessage());
                }
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okBut:
                performLogin();
                break;

            case R.id.storebutton:
                storeProfile();
                break;

            case R.id.verBut:
                performVerify();
                break;
        }
    }

    private void performVerify() {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        sslHack = sslHackBox.isChecked();
        sslHostNameHack = false; // force check on hostname first
        listener.startProgressBar(R.string.checking);

        new Thread() {
            @Override
            public void run() {
                TracHttpClient tc = new TracHttpClient(url, sslHack, sslHostNameHack, username,
                        password);
                try {
                    final String TracVersion = tc.verifyHost();
                    tcLog.d(TracVersion);
                    setValidMessage();
                } catch (JSONRPCException e) {
                    final String errmsg = e.getMessage();

                    tcLog.d("Exception during verify 1 " + errmsg, e);
                    tcLog.toast("==" + errmsg + "==");
                    if (errmsg.startsWith("hostname in certificate didn't match:")) {
                        listener.stopProgressBar();
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final String msg = context.getString(
                                        R.string.hostnametext) + errmsg + context.getString(
                                        R.string.hostnameign);
                                new AlertDialog.Builder(context)
                                        .setMessage(msg)
                                        .setTitle(R.string.hostnametitle)
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.oktext,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        verifyHostNameHack();
                                                    }
                                                })
                                        .setNegativeButton(R.string.cancel,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        setInvalidMessage(errmsg,
                                                                "Fail UserCancel Hostname");
                                                        sslHostNameHack = false;
                                                    }
                                                })
                                        .show();
                            }
                        });
                    } else if ("NOJSON".equals(errmsg)) {
                        setNoJSONMessage("Fail NOJSON");
                    } else {
                        setInvalidMessage(errmsg, "Fail Invalidmessage");
                        sslHostNameHack = false;
                    }
                }
                listener.stopProgressBar();
            }
        }.start();
    }

    private void verifyHostNameHack() {
        listener.startProgressBar(R.string.checking);
        new Thread() {
            @Override
            public void run() {
                TracHttpClient tc = new TracHttpClient(url, sslHack, sslHostNameHack, username,
                        password);
                try {
                    final String TracVersion = tc.verifyHost();
                    tcLog.d(TracVersion);
                    setValidMessage();
                    sslHostNameHack = true;
                } catch (JSONRPCException e) {
                    final String errmsg = e.getMessage();

                    tcLog.d("Exception during verify 2 " + errmsg, e);
                    tcLog.toast("==" + errmsg + "==");
                    if ("NOJSON".equals(errmsg)) {
                        setNoJSONMessage("Fail Hostname NOJSON");
                    } else {
                        setInvalidMessage(errmsg, "Fail Invalidmessage Hostname");
                        sslHostNameHack = false;
                    }
                }
                listener.stopProgressBar();
            }
        }.start();
    }

    private void setNoJSONMessage(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warn, 0, 0, 0);
                credWarn.setText(R.string.noJSON);
                credWarn.setVisibility(View.VISIBLE);
                if (message != null) {
                    credWarnSts.setText(message);
                    credWarnSts.setVisibility(View.VISIBLE);
                }
                okButton.setEnabled(false);
                storButton.setEnabled(false);
                sslHostNameHack = false; // force check on hostname first
            }
        });
    }

    private void setInvalidMessage(final String m1, final String m2) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warn, 0, 0, 0);
                credWarn.setText(R.string.invalidCred);
                credWarn.setVisibility(View.VISIBLE);
                if (m1 != null) {
                    String message = m1;
                    if (m2 != null) {
                        message += "\n" + m2;
                    }
                    credWarnSts.setText(message);
                    credWarnSts.setVisibility(View.VISIBLE);
                }
                okButton.setEnabled(false);
                storButton.setEnabled(false);
                sslHostNameHack = false; // force check on hostname first
            }
        });
    }

    private void setValidMessage() {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                credWarn.setText(R.string.validCred);
                credWarn.setVisibility(View.VISIBLE);
                credWarnSts.setVisibility(View.GONE);
                okButton.setEnabled(true);
                storButton.setEnabled(true);
            }
        });
    }

    private void performLogin() {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        bewaren = bewaarBox.isChecked();
        sslHack = sslHackBox.isChecked();
        if (bewaren) {
            TracGlobal.setCredentials(url, username, password, SelectedProfile);
            TracGlobal.setSslHack(sslHack);
            TracGlobal.setSslHostNameHack(sslHostNameHack);
            TracGlobal.storeCredentials();
        }
        TracGlobal.removeFilterString();
        TracGlobal.removeSortString();
        listener.onLogin(url, username, password, sslHack, sslHostNameHack, SelectedProfile);
    }

    private void storeProfile() {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        sslHack = sslHackBox.isChecked();
        final LoginProfile prof = new LoginProfile(url, username, password, sslHack);

        final AlertDialog.Builder alert = new AlertDialog.Builder(context);

        // Set an EditText view to get user input
        final EditText input = new EditText(context);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);
        alert.setTitle(R.string.storeprofile)
                .setMessage(R.string.profiletext)
                .setView(input)
                .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String profileName = input.getText().toString();

                        pdb.addProfile(profileName, prof);
                        swapSpinnerAdapter();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void swapSpinnerAdapter() {
        final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();

        a.swapCursor(pdb.getProfiles(true));
        loginSpinner.postInvalidate();
    }

}
