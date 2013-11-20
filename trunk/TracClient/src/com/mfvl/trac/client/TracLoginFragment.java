package com.mfvl.trac.client;

//
//  'login' to TRAC site by sending a system.APIVersion method
//

import org.alexd.jsonrpc.JSONRPCHttpClient;
import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.mfvl.trac.client.util.Credentials;

public class TracLoginFragment extends TracClientFragment {
	private String url = null;
	private String username;
	private String password;
	private boolean sslHack;
	private boolean bewaren = false;
	private EditText urlView = null;
	private Button verButton = null;
	private Button okButton = null;
	private EditText userView = null;
	private EditText pwView = null;
	private CheckBox bewaarBox = null;
	private CheckBox sslHackBox = null;
	private ImageView credWarn = null;
	private TextView credWarnTxt = null;
	private TextView credWarnSts = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.i(this.getClass().getName(), "onCreateOptionsMenu");
		inflater.inflate(R.menu.loginmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getName(), "onCreateView");
		Log.i(this.getClass().getName() + ".container", container == null ? "null" : "not null");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		if (container == null) {
			return null;
		}
		final View view = inflater.inflate(R.layout.traclogin, container, false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.i(this.getClass().getName(), "onViewCreated");
		urlView = (EditText) view.findViewById(R.id.trac_URL);
		userView = (EditText) view.findViewById(R.id.trac_User);
		pwView = (EditText) view.findViewById(R.id.trac_Pw);
		bewaarBox = (CheckBox) view.findViewById(R.id.bewaren);
		okButton = (Button) view.findViewById(R.id.okBut);
		verButton = (Button) view.findViewById(R.id.verBut);
		credWarn = (ImageView) view.findViewById(R.id.connWarn);
		credWarnTxt = (TextView) view.findViewById(R.id.connWarnTxt);
		credWarnSts = (TextView) view.findViewById(R.id.connWarnSts);
		sslHackBox = (CheckBox) view.findViewById(R.id.sslHack);

		if (url == null) {
			if (savedInstanceState == null) {
				Log.i(this.getClass().getName(), "onViewCreated use Activity");
				// Credentials.loadCredentials(context);
				url = context.getUrl();
				username = context.getUsername();
				password = context.getPassword();
				sslHack = context.getSslHack();
			} else {
				Log.i(this.getClass().getName(), "onViewCreated use savedInstanceState");
				url = savedInstanceState.getString("url");
				username = savedInstanceState.getString("user");
				password = savedInstanceState.getString("pass");
				sslHack = savedInstanceState.getBoolean("hack");
				bewaren = savedInstanceState.getBoolean("bewaar");
				bewaarBox.setChecked(bewaren);
			}
		} else {
			Log.i(this.getClass().getName(), "onViewCreated use current values");
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
		Log.i(this.getClass().getName(), "onActivityCreated");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));

		if (url == null) {
			if (savedInstanceState == null) {
				Log.i(this.getClass().getName(), "onViewCreated use Activity");
				// Credentials.loadCredentials(context);
				url = context.getUrl();
				username = context.getUsername();
				password = context.getPassword();
				sslHack = context.getSslHack();
			} else {
				Log.i(this.getClass().getName(), "onActivityCreated use savedInstanceState");
				url = savedInstanceState.getString("url");
				username = savedInstanceState.getString("user");
				password = savedInstanceState.getString("pass");
				sslHack = savedInstanceState.getBoolean("hack");
				bewaren = savedInstanceState.getBoolean("bewaar");
				bewaarBox.setChecked(bewaren);
			}
		} else {
			Log.i(this.getClass().getName(), "onActivityCreated use current values");
		}

		urlView.setText(url);
		userView.setText(username);
		pwView.setText(password);
		sslHackBox.setChecked(sslHack);
		checkHackBox(url);
		if (url == null || url.length() == 0) {
			verButton.setEnabled(false);
			okButton.setEnabled(false);
		} else {
			verButton.setEnabled(true);
			okButton.setEnabled(true);
		}

		checkHackBox(urlView.getText().toString());

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = urlView.getText().toString();
				username = userView.getText().toString();
				password = pwView.getText().toString();
				bewaren = bewaarBox.isChecked();
				sslHack = sslHackBox.isChecked();
				if (bewaren) {
					Credentials.setCredentials(url, username, password);
					Credentials.setSslHack(sslHack);
					Credentials.storeCredentials(context);
				}
				Credentials.removeFilterString(context);
				Credentials.removeSortString(context);
				listener.onLogin(url, username, password, sslHack);
			}
		});

		verButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = urlView.getText().toString();
				username = userView.getText().toString();
				password = pwView.getText().toString();
				sslHack = sslHackBox.isChecked();
				final Thread networkThread = new Thread() {

					@Override
					public void run() {
						final JSONRPCHttpClient req = new JSONRPCHttpClient(url, sslHack);
						req.setCredentials(username, password);
						try {
							final JSONArray retval = req.callJSONArray("system.getAPIVersion");
							Log.i(this.getClass().getName(), retval.toString());
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									credWarn.setVisibility(View.GONE);
									credWarnTxt.setText(R.string.validCred);
									credWarnTxt.setVisibility(View.VISIBLE);
									credWarnSts.setVisibility(View.GONE);
									okButton.setEnabled(true);
								}
							});
						} catch (final Exception e) {
							Log.i(this.getClass().getName(), e.toString());
							context.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									credWarn.setVisibility(View.VISIBLE);
									credWarnTxt.setText(R.string.invalidCred);
									credWarnTxt.setVisibility(View.VISIBLE);
									credWarnSts.setText(e.getMessage());
									credWarnSts.setVisibility(View.VISIBLE);
									okButton.setEnabled(false);
								}
							});
						}
					}
				};
				networkThread.start();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help || itemId == R.id.over) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((itemId == R.id.over ? R.string.whatsnewhelpfile : R.string.loginhelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", itemId == R.id.over);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStop() {
		Log.i(this.getClass().getName(), "onStop");
		super.onStop();
		urlView.removeTextChangedListener(checkUrlInput);
		userView.removeTextChangedListener(checkUserPwInput);
		pwView.removeTextChangedListener(checkUserPwInput);
	}

	@Override
	public void onDestroy() {
		Log.i(this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onPause() {
		Log.i(this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onStart() {
		Log.i(this.getClass().getName(), "onStart");
		super.onStart();
		urlView.addTextChangedListener(checkUrlInput);
		userView.addTextChangedListener(checkUserPwInput);
		pwView.addTextChangedListener(checkUserPwInput);
	}

	@Override
	public void onResume() {
		Log.i(this.getClass().getName(), "onResume");
		super.onResume();
		checkHackBox(urlView.getText().toString());
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		Log.i(this.getClass().getName(), "onSaveInstanceState");
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
			if (s.length() == 0) {
				verButton.setEnabled(false);
				okButton.setEnabled(false);
			} else {
				verButton.setEnabled(true);
				okButton.setEnabled(false);
				checkHackBox(s.toString());
			}
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
		}
	};

}
