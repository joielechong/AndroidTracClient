package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.trac.client.util.tcLog;

public class UpdateFieldFragment extends TracClientFragment {
	private String currentActionName = null;
	private JSONArray _actions = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				savedInstanceState.getInt("currentTicket");
				_ticket = null;
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		tcLog.d(this.getClass().getName(), "onCreateView savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		final View view = inflater.inflate(R.layout.update_field, container, false);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				final int currentTicket = savedInstanceState.getInt("currentTicket");
				if (_ticket == null || _ticket.getTicketnr() != currentTicket) {
					_ticket = new Ticket(currentTicket, context, null);
				}
			}
		}
		if (_ticket != null) {
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("currentTicket")) {
				final int currentTicket = savedInstanceState.getInt("currentTicket");
				if (_ticket == null || _ticket.getTicketnr() != currentTicket) {
					_ticket = new Ticket(currentTicket, context, null);
				}
			}
		}
		final View view = getView();
		final TextView tv = (TextView) view.findViewById(R.id.titel);
		if (_ticket != null && tv != null) {
			final String text = context.getString(R.string.updtick) + " " + _ticket;
			tv.setText(text);
		}
		final Button backButton = (Button) view.findViewById(R.id.canbutton);
		final Button storButton = (Button) view.findViewById(R.id.storebutton);

		
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				getFragmentManager().popBackStackImmediate();
			}
		});

		storButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v1) {
				getFragmentManager().popBackStackImmediate();
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
		final int itemId = item.getItemId();
		if (itemId == R.id.help) {
			final Intent launchTrac = new Intent(context.getApplicationContext(), TracShowWebPage.class);
			final String filename = context.getString((R.string.updatefieldhelpfile));
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(this.getClass().getName(), "onSaveInstanceState");
		if (_ticket != null) {
			savedState.putInt("currentTicket", _ticket.getTicketnr());
		}
	}

	public void loadTicket(Ticket ticket) {
		tcLog.d(this.getClass().getName(), "loadTicket ticket = " + ticket);
		_ticket = ticket;
	}

}
