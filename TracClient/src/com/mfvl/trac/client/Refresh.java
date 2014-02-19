package com.mfvl.trac.client;

import android.app.Activity;
import android.os.Bundle;
import com.mfvl.trac.client.util.tcLog;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import com.mfvl.trac.client.util.tcLog;

public class Refresh extends Activity {

    Messenger mService = null;
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
			tcLog.d(this.getClass().getName(),"onServiceConnected className = "+className+" service = "+service);
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, RefreshService.MSG_REQUEST_REFRESH);
                msg.replyTo = null;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
			tcLog.d(this.getClass().getName(),"onServiceDisconnected className = "+className);
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));
		
		try {
			String action = getIntent().getAction().toUpperCase();
			
            if (action != null) {
                if(action.equalsIgnoreCase(RefreshService.refreshAction)) {
					bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
					tcLog.i(this.getClass().getName(), "Refresh sent");
				}
			}
        } catch(Exception e) {
            tcLog.e(this.getClass().getName(), "Problem consuming action from intent", e);
        }
		finish();
	}
	
	@Override 
	public void onDestroy() {
		tcLog.d(this.getClass().getName(), "onDestroy");
		super.onDestroy();
		try {
			unbindService(mConnection);
		} catch (Throwable t) {
			tcLog.e(this.getClass().getName(), "Failed to unbind from the service", t);
		}
	}
}