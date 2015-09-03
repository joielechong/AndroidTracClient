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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;


public class TracLoginFragment extends TracClientFragment implements View.OnClickListener {

    /** server url */
    private String url = null;

    /** username to use on server */
    private String username;

    /** password to use on server */
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
    private static final String bewaarText = "bewaar";
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
    private LinearLayout loadProfileBox = null;
    private Spinner loginSpinner = null;
    private Cursor c = null;
    private ProfileDatabaseHelper pdb = null;
    private String SelectedProfile = null;
    private int debugcounter = 0;
	private static String _tag;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		_tag = getClass().getName();
        tcLog.d(_tag, "onCreate savedInstanceState = " + savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {
            url = listener.getUrl();
            username = listener.getUsername();
            password = listener.getPassword();
            sslHack = listener.getSslHack();
            sslHostNameHack = listener.getSslHostNameHack();
        }
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        tcLog.d(_tag, "onCreateOptionsMenu");
        inflater.inflate(R.menu.tracloginmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tcLog.d(_tag, "onCreateView savedInstanceState = " + savedInstanceState);
        // tcLog.d(_tag, "container = " + (container == null ? "null" : "not null"));
        if (container == null) {
            return null;
        }
        final View view = inflater.inflate(R.layout.traclogin, container, false);

        return view;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tcLog.d(_tag, "onViewCreated");
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
        loadProfileBox = (LinearLayout) view.findViewById(R.id.loadprofile);
        loginSpinner = (Spinner) view.findViewById(R.id.loginspinner);
	
        if (url == null) {
            if (savedInstanceState == null) {
                // tcLog.d(_tag, "onViewCreated use Activity");
				url = listener.getUrl();
				username = listener.getUsername();
				password = listener.getPassword();
				sslHack = listener.getSslHack();
				sslHostNameHack = listener.getSslHostNameHack();
            } else {
                // tcLog.d(_tag, "onViewCreated use savedInstanceState");
                url = savedInstanceState.getString(Const.NEW_URL);
                username = savedInstanceState.getString(Const.NEW_USERNAME);
                password = savedInstanceState.getString(Const.NEW_PASSWORD);
                sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK);
                sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
                bewaren = savedInstanceState.getBoolean(bewaarText);
                bewaarBox.setChecked(bewaren);
            }
            // } else {
            // tcLog.d(_tag, "onViewCreated use current values");
        }
	
        pdb = new ProfileDatabaseHelper(context);
        pdb.open();
        c = pdb.getProfiles();
        if (c.getCount() < 2) {
            loadProfileBox.setVisibility(View.GONE);
        } else {
            final String[] columns = new String[] { "name" };
            final int[] to = new int[] { android.R.id.text1 };

            loadProfileBox.setVisibility(View.VISIBLE);
            final SimpleCursorAdapter adapt = new SimpleCursorAdapter(context, android.R.layout.simple_spinner_dropdown_item, c,
                    columns, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            loginSpinner.setAdapter(adapt);
	    
            loginSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    if (arg1 != null && ((CheckedTextView) arg1).getText().toString() != null) {
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
								showAlertBox(R.string.notfound,R.string.loadprofiletext,SelectedProfile);
                            }
                        }
                    }
                }
		    
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {}
		    
            });
        }
	
        urlView.setText(url);
        userView.setText(username);
        pwView.setText(password);
        sslHackBox.setChecked(sslHack);
        checkHackBox(url);
    }
    
    private void report(final String label) {
        MyTracker.report("Normal", "Verification", label);
    }
    
    private JSONObject verifyHost(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
        final ContentValues cv = new ContentValues();

        cv.put(Const.CURRENT_URL, url);
        cv.put(Const.CURRENT_USERNAME, username);
        cv.put(Const.CURRENT_PASSWORD, password);
        cv.put(Const.CURRENT_SSLHACK, sslHack);
        cv.put(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        Uri uri = context.getContentResolver().insert(TicketProvider.VERIFY_QUERY_URI, cv);
        JSONObject j = null;

        try {
            j = new JSONObject(uri.getLastPathSegment());
            tcLog.d(_tag, "verifyHost " + uri + " " + j);
        } catch (JSONException e) {
            try {
                j = new JSONObject();
                j.put(TicketProvider.ERROR, "Bad response from TicketProvider");
                tcLog.d(_tag, "verifyHost " + uri + " " + j);
            } catch (JSONException e1) {}
        }
        return j;
        // return cv;
        // return context.getContentResolver().call(TicketProvider.AUTH_URI,TicketProvider.VERIFY_HOST,null,cv);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d(_tag,"onActivityCreated savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
	
        if (url == null) {
            if (savedInstanceState == null) {
                // tcLog.d(_tag, "onActivityCreated use Activity");
				url = listener.getUrl();
				username = listener.getUsername();
				password = listener.getPassword();
				sslHack = listener.getSslHack();
				sslHostNameHack = listener.getSslHostNameHack();
            } else {
                // tcLog.d(_tag, "onActivityCreated use savedInstanceState");
                url = savedInstanceState.getString(Const.NEW_URL);
                username = savedInstanceState.getString(Const.NEW_USERNAME);
                password = savedInstanceState.getString(Const.NEW_PASSWORD);
                sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK);
                sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
                bewaren = savedInstanceState.getBoolean(bewaarText);
                bewaarBox.setChecked(bewaren);
            }
            // } else {
            // tcLog.d(_tag, "onActivityCreated use current values");
        }
	
        urlView.setText(url);
        userView.setText(username);
        pwView.setText(password);
        sslHackBox.setChecked(sslHack);
        checkHackBox(url);
        if (url == null || url.length() == 0) {
            verButton.setEnabled(false);
            okButton.setEnabled(false);
            storButton.setEnabled(false);
        } else {
            verButton.setEnabled(true);
            okButton.setEnabled(true);
            storButton.setEnabled(true);
        }
	
        checkHackBox(urlView.getText().toString());
	
        bewaarBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                debugcounter++;
                if (debugcounter == 6) {
                    listener.enableDebug();
                }
            }
        });
	
    }
	
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.okBut:
			performLogin(v);
			break;
			
			case R.id.verBut:
			performVerify(v);
			break;
			
			case R.id.storebutton:
			storeProfile(v);
			break;
			
			default:
		}
	}

    public void performVerify(View v) {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        sslHack = sslHackBox.isChecked();
        sslHostNameHack = false; // force check on hostname first
        listener.startProgressBar(R.string.checking);

        new Thread() {
            @Override
            public void run() {
				JSONObject j = verifyHost(url, sslHack, sslHostNameHack, username, password);
				try {
					final String TracVersion = (String) j.get(TicketProvider.RESULT);

					tcLog.d(this.getClass().getName(), TracVersion);
					setValidMessage("Success");
				} catch (JSONException e) {
					try {
						final String errmsg = (String) j.get(TicketProvider.ERROR);
							
						tcLog.d(getClass().getName(), "Exception during verify 1 " + errmsg);
						tcLog.toast("==" + errmsg + "==");
						if (errmsg.startsWith("hostname in certificate didn't match:")) {
							report("Fail Hostname");
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

									alertDialogBuilder.setTitle(R.string.hostnametitle);
									final String msg = context.getString(R.string.hostnametext) + errmsg + context.getString(R.string.hostnameign);

									alertDialogBuilder.setMessage(msg);
									alertDialogBuilder.setCancelable(false);
									alertDialogBuilder.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int id) {
											listener.startProgressBar(R.string.checking);

											new Thread() {
												@Override
												public void run() {
													final JSONObject j1 = verifyHost(url, sslHack, true, username, password);
													
													try {
														final String TracVersion1 = (String) j1.get(TicketProvider.RESULT);

														tcLog.d(this.getClass().getName(), TracVersion1);
														setValidMessage("Success Hostname");
														sslHostNameHack = true;
													} catch (JSONException e1) {
														try {
															final String errmsg1 = (String) j1.get(TicketProvider.ERROR);

															tcLog.d(getClass().getName(), "Exception during verify 2 " + errmsg1);
															// tcLog.toast("==" + errmsg1 + "==");
															if ("NOJSON".equals(errmsg1)) {
																setNoJSONMessage("Fail Hostname NOJSON");
															} else {
																setInvalidMessage(errmsg1,"Fail Invalidmessage Hostname");
																sslHostNameHack = false;
															}
														} catch (JSONException e2) {
															setNoJSONMessage("Fail Hostname NOJSON fase 2");                                                                }
														}
														listener.stopProgressBar();
													}
												}.start();
											}
                                    });
									alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int id) {
											setInvalidMessage(errmsg,"Fail UserCancel Hostname");
											sslHostNameHack = false;
										}
                                    });
                                    alertDialogBuilder.show();
                                }
                            });
                        } else if ("NOJSON".equals(errmsg)) {
                            setNoJSONMessage("Fail NOJSON");
                        } else {
                            setInvalidMessage(errmsg,"Fail Invalidmessage");
                            sslHostNameHack = false;
                        }
                    } catch (JSONException e1) {
                        setNoJSONMessage("Fail Hostname NOJSON fase 1");
                    }
                }
				listener.stopProgressBar();
            }
        }.start();
    }
	
    public void performLogin(View v) {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        bewaren = bewaarBox.isChecked();
        sslHack = sslHackBox.isChecked();
        if (bewaren) {
            Credentials.setCredentials(url, username, password, SelectedProfile);
            Credentials.setSslHack(sslHack);
            Credentials.setSslHostNameHack(sslHostNameHack);
            Credentials.storeCredentials();
        }
        Credentials.removeFilterString();
        Credentials.removeSortString();
        listener.onLogin(url, username, password, sslHack, sslHostNameHack, SelectedProfile);
    }
	
    public void storeProfile(View v) {
        url = urlView.getText().toString();
        username = userView.getText().toString();
        password = pwView.getText().toString();
        sslHack = sslHackBox.isChecked();
        final LoginProfile prof = new LoginProfile(url, username, password, sslHack);
	   
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
	    
        // Set an EditText view to get user input
        final EditText input = new EditText(context);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);
        alert.setTitle(R.string.storeprofile)
			.setMessage(R.string.profiletext)
			.setView(input)
		    .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String profileName = input.getText().toString();

                        pdb.addProfile(profileName, prof);
                        final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();

                        a.swapCursor(pdb.getProfiles());
                        loginSpinner.postInvalidate();
                    }
                })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
 
	
    @Override
    public void onStart() {
        tcLog.d(_tag, "onStart");
        super.onStart();
        urlView.addTextChangedListener(checkUrlInput);
        userView.addTextChangedListener(checkUserPwInput);
        pwView.addTextChangedListener(checkUserPwInput);
    }
    
    @Override
    public void onResume() {
        tcLog.d(_tag, "onResume");
        super.onResume();
        checkHackBox(urlView.getText().toString());
    }
	
	public void showHelp() {
		showHelpFile(R.string.loginhelpfile);
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d(_tag, "onOptionsItemSelected item=" + item.getTitle());
        final int itemId = item.getItemId();

        if (itemId == R.id.help) {
			showHelp();
        } else if (itemId == R.id.exportprofiles) {
            try {
                pdb.open();
                pdb.writeXML(context.getString(R.string.app_name));
				showAlertBox(R.string.completed,R.string.xmlwritecompleted,null);
                final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();

                a.swapCursor(pdb.getProfiles());
                loginSpinner.postInvalidate();
            } catch (final Exception e) {
				tcLog.e(getClass().getName(),"Export failed",e);
				showAlertBox(R.string.failed,0,e.getMessage());
            }
        } else if (itemId == R.id.importprofiles) {
            try {
                pdb.open();
                pdb.readXML(context.getString(R.string.app_name));
				showAlertBox(R.string.completed,R.string.xmlreadcompleted,null);
            } catch (final Exception e) {
				showAlertBox(R.string.failed,0,e.getMessage());
            }
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    @Override
    public void onStop() {
        tcLog.d(_tag, "onStop");
        super.onStop();
        urlView.removeTextChangedListener(checkUrlInput);
        userView.removeTextChangedListener(checkUserPwInput);
        pwView.removeTextChangedListener(checkUserPwInput);
    }
    
    @Override
    public void onDestroyView() {
        tcLog.d(_tag, "onDestroyView");
        registerForContextMenu(loginSpinner);
        loginSpinner.setAdapter(null);
        if (c != null) {
            c.close();
        }
        pdb.close();
        super.onDestroyView();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        try {
            savedState.putString(Const.NEW_URL, urlView.getText().toString());
        } catch (final Exception e) {}
        try {
            savedState.putString(Const.NEW_USERNAME, userView.getText().toString());
        } catch (final Exception e) {}
        try {
            savedState.putString(Const.NEW_PASSWORD, pwView.getText().toString());
        } catch (final Exception e) {}
        savedState.putBoolean(Const.CURRENT_SSLHACK, sslHackBox.isChecked());
        savedState.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        try {
            savedState.putBoolean(bewaarText, bewaarBox.isChecked());
        } catch (final Exception e) {}
        tcLog.d(_tag, "onSaveInstanceState savedState = " + savedState);
    }
    
    private void checkHackBox(String s) {
        if (sslHackBox != null && s != null) {
            sslHackBox.setVisibility(s.length() >= 6 && s.substring(0, 6).equals("https:") ? View.VISIBLE : View.GONE);
        }
    }
    
    private final TextWatcher checkUrlInput = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {}
	    
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	    
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
        public void afterTextChanged(Editable s) {}
	    
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	    
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
    
    private void setNoJSONMessage(final String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warn, 0, 0, 0);
                credWarn.setText(R.string.noJSON);
                credWarn.setVisibility(View.VISIBLE);
                okButton.setEnabled(false);
                storButton.setEnabled(false);
                sslHostNameHack = false; // force check on hostname first
            }
        });
		report(message);
    }
    
    private void setInvalidMessage(final String m1,final String m2) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warn, 0, 0, 0);
                credWarn.setText(R.string.invalidCred);
                credWarn.setVisibility(View.VISIBLE);
                if (m1 != null) {
                    credWarnSts.setText(m1);
                    credWarnSts.setVisibility(View.VISIBLE);
                }
                okButton.setEnabled(false);
                storButton.setEnabled(false);
                sslHostNameHack = false; // force check on hostname first
            }
        });
		report(m2);
    }
    
    private void setValidMessage(final String message) {
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
		report(message);
    }
}