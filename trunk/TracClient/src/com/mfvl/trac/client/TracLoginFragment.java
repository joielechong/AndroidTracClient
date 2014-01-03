package com.mfvl.trac.client;

//
//  'login' to TRAC site by sending a system.APIVersion method
//

import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.LoginProfile;
import com.mfvl.trac.client.util.ProfileDatabaseHelper;
import com.mfvl.trac.client.util.tcLog;

public class TracLoginFragment extends TracClientFragment {
	/** server url */
	private String url = null;
	/** username to use on server */
	private String username;
	/** password to use on server */
	private String password;

	/** flag to indicate that SSL sites can have problems like Self signed certificates */
	private boolean sslHack;
	/** flag to indicate that the credentials will be stored in the shared preferences */
	private boolean bewaren = false;
	private EditText urlView = null;
	private Button verButton = null;
	private Button okButton = null;
	private Button storButton = null;
	private EditText userView = null;
	private EditText pwView = null;
	private CheckBox bewaarBox = null;
	private CheckBox sslHackBox = null;
	private ImageView credWarn = null;
	private TextView credWarnTxt = null;
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
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
		if (savedInstanceState == null) {
			tcLog.d(this.getClass().getName(), "onViewCreated use Activity");
			// Credentials.loadCredentials(context);
			url = context.getUrl();
			username = context.getUsername();
			password = context.getPassword();
			sslHack = context.getSslHack();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.tracloginmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		tcLog.d(this.getClass().getName(), "container = " + (container == null ? "null" : "not null"));
		if (container == null) {
			return null;
		}
		final View view = inflater.inflate(R.layout.traclogin, container, false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		tcLog.d(this.getClass().getName(), "onViewCreated");
		urlView = (EditText) view.findViewById(R.id.trac_URL);
		userView = (EditText) view.findViewById(R.id.trac_User);
		pwView = (EditText) view.findViewById(R.id.trac_Pw);
		bewaarBox = (CheckBox) view.findViewById(R.id.bewaren);
		okButton = (Button) view.findViewById(R.id.okBut);
		verButton = (Button) view.findViewById(R.id.verBut);
		storButton = (Button) view.findViewById(R.id.storebutton);
		credWarn = (ImageView) view.findViewById(R.id.connWarn);
		credWarnTxt = (TextView) view.findViewById(R.id.connWarnTxt);
		credWarnSts = (TextView) view.findViewById(R.id.connWarnSts);
		sslHackBox = (CheckBox) view.findViewById(R.id.sslHack);
		loadProfileBox = (LinearLayout) view.findViewById(R.id.loadprofile);
		loginSpinner = (Spinner) view.findViewById(R.id.loginspinner);

		if (url == null) {
			if (savedInstanceState == null) {
				tcLog.d(this.getClass().getName(), "onViewCreated use Activity");
				// Credentials.loadCredentials(context);
				url = context.getUrl();
				username = context.getUsername();
				password = context.getPassword();
				sslHack = context.getSslHack();
			} else {
				tcLog.d(this.getClass().getName(), "onViewCreated use savedInstanceState");
				url = savedInstanceState.getString("url");
				username = savedInstanceState.getString("user");
				password = savedInstanceState.getString("pass");
				sslHack = savedInstanceState.getBoolean("hack");
				bewaren = savedInstanceState.getBoolean("bewaar");
				bewaarBox.setChecked(bewaren);
			}
		} else {
			tcLog.d(this.getClass().getName(), "onViewCreated use current values");
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
							} else {
								final AlertDialog.Builder alert = new AlertDialog.Builder(context);

								alert.setTitle(R.string.notfound);
								alert.setMessage(context.getString(R.string.loadprofiletext) + ": " + SelectedProfile);
								alert.setPositiveButton(R.string.oktext, null);
								alert.show();
							}
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}

			});
		}

		urlView.setText(url);
		userView.setText(username);
		pwView.setText(password);
		sslHackBox.setChecked(sslHack);
		checkHackBox(url);
}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));

		if (url == null) {
			if (savedInstanceState == null) {
				tcLog.d(this.getClass().getName(), "onViewCreated use Activity");
				// Credentials.loadCredentials(context);
				url = context.getUrl();
				username = context.getUsername();
				password = context.getPassword();
				sslHack = context.getSslHack();
			} else {
				tcLog.d(this.getClass().getName(), "onActivityCreated use savedInstanceState");
				url = savedInstanceState.getString("url");
				username = savedInstanceState.getString("user");
				password = savedInstanceState.getString("pass");
				sslHack = savedInstanceState.getBoolean("hack");
				bewaren = savedInstanceState.getBoolean("bewaar");
				bewaarBox.setChecked(bewaren);
			}
		} else {
			tcLog.d(this.getClass().getName(), "onActivityCreated use current values");
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

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = urlView.getText().toString();
				username = userView.getText().toString();
				password = pwView.getText().toString();
				bewaren = bewaarBox.isChecked();
				sslHack = sslHackBox.isChecked();
				if (bewaren) {
					Credentials.setCredentials(url, username, password,SelectedProfile);
					Credentials.setSslHack(sslHack);
					Credentials.storeCredentials(context);
				}
				Credentials.removeFilterString(context);
				Credentials.removeSortString(context);
				listener.onLogin(url, username, password, sslHack,SelectedProfile);
			}
		});

		verButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = urlView.getText().toString();
				username = userView.getText().toString();
				password = pwView.getText().toString();
				sslHack = sslHackBox.isChecked();
				final ProgressDialog pb = startProgressBar(R.string.checking);
				final Thread networkThread = new Thread() {

					@Override
					public void run() {
						final JSONRPCHttpClient req = new JSONRPCHttpClient(url, sslHack);
						req.setCredentials(username, password);
						try {
							final JSONArray retval = req.callJSONArray("system.getAPIVersion");
							tcLog.d(this.getClass().getName(), retval.toString());
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									credWarn.setVisibility(View.GONE);
									credWarnTxt.setText(R.string.validCred);
									credWarnTxt.setVisibility(View.VISIBLE);
									credWarnSts.setVisibility(View.GONE);
									okButton.setEnabled(true);
									storButton.setEnabled(true);
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();
							tcLog.d(this.getClass().getName(), e.toString());
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									credWarn.setVisibility(View.VISIBLE);
									credWarnTxt.setText(R.string.invalidCred);
									credWarnTxt.setVisibility(View.VISIBLE);
									credWarnSts.setText(e.getMessage());
									credWarnSts.setVisibility(View.VISIBLE);
									okButton.setEnabled(false);
									storButton.setEnabled(false);
								}
							});
						} finally {
							pb.dismiss();
						}
					}
				};
				networkThread.start();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = urlView.getText().toString();
				username = userView.getText().toString();
				password = pwView.getText().toString();
				sslHack = sslHackBox.isChecked();
				final LoginProfile prof = new LoginProfile(url, username, password, sslHack);

				final AlertDialog.Builder alert = new AlertDialog.Builder(context);

				alert.setTitle(R.string.storeprofile);
				alert.setMessage(R.string.profiletext);

				// Set an EditText view to get user input
				final EditText input = new EditText(context);
				final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT);
				input.setLayoutParams(lp);
				alert.setView(input);

				alert.setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						final String profileName = input.getText().toString();
						pdb.addProfile(profileName, prof);
						final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();
						a.swapCursor(pdb.getProfiles());
						loginSpinner.postInvalidate();
					}
				});
				alert.setNegativeButton(R.string.cancel, null);
				alert.show();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString(R.string.loginhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
		} else if (itemId == R.id.exportprofiles) {
			try {
				pdb.open();
				pdb.writeXML(context.getString(R.string.app_name));
				final AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle(R.string.completed);
				alert.setMessage(context.getString(R.string.xmlwritecompleted));
				alert.setPositiveButton(R.string.oktext, null);
				alert.show();
				final SimpleCursorAdapter a = (SimpleCursorAdapter) loginSpinner.getAdapter();
				a.swapCursor(pdb.getProfiles());
				loginSpinner.postInvalidate();
			} catch (final Exception e) {
				final AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle(R.string.failed);
				alert.setMessage(e.getMessage());
				alert.setPositiveButton(R.string.oktext, null);
				alert.show();
			}
		} else if (itemId == R.id.importprofiles) {
			try {
				pdb.open();
				pdb.readXML(context.getString(R.string.app_name));
				final AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle(R.string.completed);
				alert.setMessage(context.getString(R.string.xmlreadcompleted));
				alert.setPositiveButton(R.string.oktext, null);
				alert.show();
			} catch (final Exception e) {
				final AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle(R.string.failed);
				alert.setMessage(e.getMessage());
				alert.setPositiveButton(R.string.oktext, null);
				alert.show();
			}
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onStop() {
		tcLog.d(this.getClass().getName(), "onStop");
		super.onStop();
		urlView.removeTextChangedListener(checkUrlInput);
		userView.removeTextChangedListener(checkUserPwInput);
		pwView.removeTextChangedListener(checkUserPwInput);
	}

	@Override
	public void onDestroyView() {
		tcLog.d(this.getClass().getName(), "onDestroyView");
		registerForContextMenu(loginSpinner);
		loginSpinner.setAdapter(null);
		if (c != null) {
			c.close();
		}
		pdb.close();
		super.onDestroyView();
	}

	@Override
	public void onStart() {
		tcLog.d(this.getClass().getName(), "onStart");
		super.onStart();
		urlView.addTextChangedListener(checkUrlInput);
		userView.addTextChangedListener(checkUserPwInput);
		pwView.addTextChangedListener(checkUserPwInput);
	}

	@Override
	public void onResume() {
		tcLog.d(this.getClass().getName(), "onResume");
		super.onResume();
		checkHackBox(urlView.getText().toString());
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState");
		savedState.putString("url", urlView.getText().toString());
		savedState.putString("user", userView.getText().toString());
		savedState.putString("pass", pwView.getText().toString());
		savedState.putBoolean("hack", sslHackBox.isChecked());
		savedState.putBoolean("bewaar", bewaarBox.isChecked());
	}

	private void checkHackBox(String s) {
		if (sslHackBox != null) {
			if (s.length() >= 6 && s.substring(0, 6).equals("https:")) {
				sslHackBox.setVisibility(View.VISIBLE);
			} else {
				sslHackBox.setVisibility(View.GONE);
			}
		}
	}

	private final TextWatcher checkUrlInput = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			credWarnTxt.setVisibility(View.GONE);
			credWarn.setVisibility(View.GONE);
			credWarnSts.setVisibility(View.GONE);
			verButton.setEnabled(false);
			okButton.setEnabled(false);
			storButton.setEnabled(false);
			if (s.length() != 0) {
				verButton.setEnabled(true);
				checkHackBox(s.toString());
			}
			SelectedProfile=null;
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
			credWarnTxt.setVisibility(View.GONE);
			credWarn.setVisibility(View.GONE);
			credWarnSts.setVisibility(View.GONE);
			verButton.setEnabled(true);
			okButton.setEnabled(false);
			storButton.setEnabled(false);
			SelectedProfile = null;
		}
	};
}
