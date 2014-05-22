package com.mfvl.trac.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.mfvl.trac.client.util.tcLog;

public class Refresh extends Activity {

	Messenger mService = null;

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// tcLog.d(this.getClass().getName(),"onServiceConnected className = "
			// + className + " service = " + service);
			mService = new Messenger(service);
			try {
				final Message msg = Message.obtain(null, Const.MSG_REQUEST_REFRESH);
				msg.replyTo = null;
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(this.getClass().getName(), "Problem connecting", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// tcLog.d(this.getClass().getName(),
			// "onServiceDisconnected className = " + className);
			mService = null;
		}
	};

	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " +
		// (savedInstanceState == null ? "null" : "not null"));

		try {
			final String action = getIntent().getAction().toUpperCase();

			if (action != null) {
				if (action.equalsIgnoreCase(RefreshService.refreshAction)) {
					bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
					// tcLog.i(this.getClass().getName(), "Refresh sent");
				}
			}
		} catch (final Exception e) {
			tcLog.e(this.getClass().getName(), "Problem consuming action from intent", e);
		}
		finish();
	}

	@Override
	public void onDestroy() {
		// tcLog.d(this.getClass().getName(), "onDestroy");
		super.onDestroy();
		try {
			unbindService(mConnection);
		} catch (final Throwable t) {
			tcLog.e(this.getClass().getName(), "Failed to unbind from the service", t);
		}
	}
}