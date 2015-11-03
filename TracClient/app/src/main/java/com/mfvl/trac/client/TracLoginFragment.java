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


public class TracLoginFragment extends TracClientFragment {

    public static final String RESULT = "rv";
    public static final String ERROR = "error";

    private static final String NEW_URL = "newURL";
    private static final String NEW_USERNAME = "newUsername";
    private static final String NEW_PASSWORD = "newPassword";

    /** server url */
    private String url = null;
    private String currentUrl = null;

    /** username to use on server */
    private String username;
    private String currentUsername;

    /** password to use on server */
    private String password;
    private String currentPassword;
    
    /**
     * flag to indicate that SSL sites can have problems like Self signed certificates
     */
    private boolean sslHack;
    private boolean currentSslHack;

    /**
     * flag to ingnore Hostname verification errors in SSL
     */
    private boolean sslHostNameHack;
    private boolean currentSslHostNameHack;

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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d("savedInstanceState = " + savedInstanceState);
		currentUrl=fragmentArgs.getString(Const.CURRENT_URL);
		currentUsername=fragmentArgs.getString(Const.CURRENT_USERNAME);
		currentPassword=fragmentArgs.getString(Const.CURRENT_PASSWORD);
		currentSslHack =fragmentArgs.getBoolean(Const.CURRENT_SSLHACK);
		currentSslHostNameHack =fragmentArgs.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
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
        loadProfileBox = (LinearLayout) view.findViewById(R.id.loadprofile);
        loginSpinner = (Spinner) view.findViewById(R.id.loginspinner);
	
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
	
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        tcLog.logCall();
        inflater.inflate(R.menu.tracloginmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    private JSONObject verifyHost(final String url, final boolean sslHack, final boolean sslHostNameHack, final String username, final String password) {
        JSONObject j = new JSONObject();
		try {
			TracHttpClient tc = new TracHttpClient(url,sslHack,sslHostNameHack,username,password);
			j.put(RESULT,tc.verifyHost());
		} catch (Exception e) {
			try {
				j.put(ERROR, e.getMessage());
			} catch (JSONException ignored) {}
		}
//		tcLog.d("j = "+j);
        return j;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tcLog.d("savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
	
        if (url == null) {
            if (savedInstanceState == null) {
                // tcLog.d("onActivityCreated use Activity");
				url = currentUrl;
				username = currentUsername;
				password = currentPassword;
				sslHack = currentSslHack;
				sslHostNameHack = currentSslHostNameHack;
            } else {
                // tcLog.d("onActivityCreated use savedInstanceState");
                url = savedInstanceState.getString(NEW_URL);
                username = savedInstanceState.getString(NEW_USERNAME);
                password = savedInstanceState.getString(NEW_PASSWORD);
                sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK);
                sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK);
                bewaren = savedInstanceState.getBoolean(bewaarText);
                bewaarBox.setChecked(bewaren);
            }
        }
	
        urlView.setText(url);
        userView.setText(username);
        pwView.setText(password);
        sslHackBox.setChecked(sslHack);
        checkHackBox(url);
		boolean buttonsOn = !(url == null || url.length() == 0);
		verButton.setEnabled(buttonsOn);
		okButton.setEnabled(buttonsOn);
		storButton.setEnabled(buttonsOn);
	
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
				JSONObject j = verifyHost(url, sslHack, sslHostNameHack, username, password);
				try {
					final String TracVersion = (String) j.get(RESULT);

					tcLog.d( TracVersion);
					setValidMessage();
				} catch (JSONException e) {
					try {
						final String errmsg = (String) j.get(ERROR);
							
						tcLog.d( "Exception during verify 1 " + errmsg,e);
						tcLog.toast("==" + errmsg + "==");
						if (errmsg.startsWith("hostname in certificate didn't match:")) {
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
									final String msg = context.getString(R.string.hostnametext) + errmsg + context.getString(R.string.hostnameign);

									alertDialogBuilder.setMessage(msg)
										.setTitle(R.string.hostnametitle)
										.setCancelable(false)
										.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int id) {
											listener.startProgressBar(R.string.checking);
											new Thread() {
												@Override
												public void run() {
													final JSONObject j1 = verifyHost(url, sslHack, true, username, password);
													
													try {
														final String TracVersion1 = (String) j1.get(RESULT);

														tcLog.d( TracVersion1);
														setValidMessage();
														sslHostNameHack = true;
													} catch (JSONException e1) {
														try {
															final String errmsg1 = (String) j1.get(ERROR);

															tcLog.d( "Exception during verify 2 " + errmsg1,e1);
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
	
    private void performLogin() {
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
	
    private void storeProfile() {
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
        tcLog.logCall();
        super.onStart();
        urlView.addTextChangedListener(checkUrlInput);
        userView.addTextChangedListener(checkUserPwInput);
        pwView.addTextChangedListener(checkUserPwInput);
    }
    
    @Override
    public void onResume() {
        tcLog.logCall();
        super.onResume();
		helpFile = R.string.loginhelpfile;
        checkHackBox(urlView.getText().toString());
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tcLog.d("item=" + item.getTitle());
        final int itemId = item.getItemId();

        if (itemId == R.id.exportprofiles) {
            try {
                pdb.open();
                pdb.writeXML(context.getString(R.string.app_name));
				showAlertBox(R.string.completed,R.string.xmlwritecompleted,null);
                final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();

                a.swapCursor(pdb.getProfiles());
                loginSpinner.postInvalidate();
            } catch (final Exception e) {
				tcLog.e("Export failed",e);
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
        tcLog.logCall();
        super.onStop();
        urlView.removeTextChangedListener(checkUrlInput);
        userView.removeTextChangedListener(checkUserPwInput);
        pwView.removeTextChangedListener(checkUserPwInput);
    }
    
    @Override
    public void onDestroyView() {
        tcLog.logCall();
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
            savedState.putString(NEW_URL, urlView.getText().toString());
        } catch (final Exception ignored) {}
        try {
            savedState.putString(NEW_USERNAME, userView.getText().toString());
        } catch (final Exception ignored) {}
        try {
            savedState.putString(NEW_PASSWORD, pwView.getText().toString());
        } catch (final Exception ignored) {}
        savedState.putBoolean(Const.CURRENT_SSLHACK, sslHackBox.isChecked());
        savedState.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        try {
            savedState.putBoolean(bewaarText, bewaarBox.isChecked());
        } catch (final Exception ignored) {}
        tcLog.d(" savedState = " + savedState);
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
    
    private void setInvalidMessage(final String m1,final String m2) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                credWarn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warn, 0, 0, 0);
                credWarn.setText(R.string.invalidCred);
                credWarn.setVisibility(View.VISIBLE);
                if (m1 != null) {
					String message = m1;
					if (m2 != null) {
						message += "\n"+m2;
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
}
