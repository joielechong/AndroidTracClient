/*
 * Copyright (C) 2014 Michiel van Loon
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.mfvl.trac.client.util.tcLog;

public class RefreshService extends Service {

	private static final int timerStart = 1 * 60 * 1000; // 5 minuten
	private static final int timerPeriod = 5 * 60 * 1000; // 5 minuten
	public static final String refreshAction = "LIST_REFRESH";
	private Timer monitorTimer = null;
	private static final int notifId = 1234;

	private HandlerThread mHandlerThread = null;
	private ServiceHandler mServiceHandler;
	private Messenger mMessenger = null;
	private Messenger receiver = null;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
			// tcLog.d(getClass().getName(), "ServiceHandler");
		}

		@Override
		public void handleMessage(final Message msg) {
			// tcLog.d(this.getClass().getName(), "handleMessage msg = " + msg);
			final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			switch (msg.what) {
			case Const.MSG_START_TIMER:
				stopTimer();
				startTimer(msg);
				receiver = msg.replyTo;
				break;
			case Const.MSG_STOP_TIMER:
				stopTimer();
				break;
			case Const.MSG_REQUEST_REFRESH:
				sendMessageToUI(Const.MSG_REQUEST_REFRESH);
				break;
			case Const.MSG_REMOVE_NOTIFICATION:
				mNotificationManager.cancel(notifId);
				break;
			case Const.MSG_SEND_TICKET_COUNT:
				if (msg.arg1 > 0) {
					sendMessageToUI(Const.MSG_REQUEST_NEW_TICKETS);
				}
				break;
			case Const.MSG_SEND_NEW_TICKETS:
				@SuppressWarnings("unchecked")
				final List<Integer> newTickets = (List<Integer>) msg.obj;
				if (newTickets != null) {
					if (newTickets.size() > 0) {
						try {
							final Intent launchIntent = new Intent(RefreshService.this, Refresh.class);
							launchIntent.setAction(refreshAction);
							final PendingIntent pendingIntent = PendingIntent.getActivity(RefreshService.this, -1, launchIntent,
									PendingIntent.FLAG_UPDATE_CURRENT);

							final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(RefreshService.this)
							.setSmallIcon(R.drawable.traclogo)
							.setContentTitle(RefreshService.this.getString(R.string.notifmod))
							.setTicker(RefreshService.this.getString(R.string.foundnew))
							.setContentText(RefreshService.this.getString(R.string.foundnew))
							.setSubText(newTickets.toString()).setContentIntent(pendingIntent);
							final Notification notification = mBuilder.build();
							notification.flags |= Notification.FLAG_AUTO_CANCEL;
							mNotificationManager.notify(notifId, notification);
							// tcLog.d(this.getClass().getName(),
							// "Notification sent");
						} catch (final IllegalArgumentException e) {
							tcLog.e(this.getClass().getName(), "IllegalArgumentException in notification", e);
						}
					}
				}
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToUI(int message) {
		// tcLog.d(this.getClass().getName(), "sendMessageToUI");
		try {
			// Send data as an Integer
			if (receiver != null) {
				receiver.send(Message.obtain(null, message, 0, 0));
			}
		} catch (final RemoteException e) {
			tcLog.e(this.getClass().getName(), "sendMessageToUI failed", e);
		}
	}

	@Override
	public void onCreate() {
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		// tcLog.d(this.getClass().getName(), "onCreate");
		mHandlerThread = new HandlerThread("ServiceHandler", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());
		mMessenger = new Messenger(mServiceHandler);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// tcLog.d(this.getClass().getName(), "onStartCommand intent = " + intent);

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the
		// job
		final Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// tcLog.d(this.getClass().getName(), "onBind intent = " + intent);
		return mMessenger.getBinder();
	}

	@Override
	public boolean onUnbind(Intent i) {
		stopSelf();
		return false;
	}

	@Override
	public void onDestroy() {
		// tcLog.d(this.getClass().getName(), "onDestroy");
		stopTimer();
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(notifId);
		mHandlerThread.quit();
		super.onDestroy();
	}

	private void startTimer(final Message msg) {
		// tcLog.d(this.getClass().getName(), "startTimer");
		monitorTimer = new Timer("monitorTickets");
		monitorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// tcLog.d(this.getClass().getName(), "timertask started");
				sendMessageToUI(Const.MSG_REQUEST_TICKET_COUNT);
			}
		}, timerStart, timerPeriod);
	}

	private void stopTimer() {
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
		monitorTimer = null;
	}

}