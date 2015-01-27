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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.ISO8601;
import com.mfvl.trac.client.util.LoginProfile;
import com.mfvl.trac.client.util.ProfileDatabaseHelper;
import com.mfvl.trac.client.util.SortSpec;
import com.mfvl.trac.client.util.tcLog;

interface onFileSelectedListener {
	void onSelected(final String f);
}

/**
 * Interface for the fragments to communicate with each other and the main activity
 *
 * @author Michiel
 *
 */
interface InterFragmentListener {
	boolean dispAds();

	void enableDebug();

	void onChangeHost();

	void onChooserSelected(onFileSelectedListener oc);

	void onFilterSelected(ArrayList<FilterSpec> filterList);

	void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile);

	void onNewTicket();

	void onTicketSelected(Ticket ticket);

	void onSortSelected(ArrayList<SortSpec> sortList);

	void onUpdateTicket(Ticket ticket);

	void refreshOverview();

	void setDispAds(boolean b);

	void setFilter(ArrayList<FilterSpec> filter);

	void setFilter(String filter);

	void setReferenceTime();

	void setSort(ArrayList<SortSpec> sort);

	void setSort(String sort);

	void shareTicket(final Ticket t);
}

public class TracStart extends ActionBarActivity implements InterFragmentListener, OnBackStackChangedListener {
	// onActivityResult requestcode for filechooser
	private static final int REQUEST_CODE = 6384;
	private static final int timerCorr = 60 * 1000 * 2; // 2 minuten
	private static final String ListFragmentTag = "List_Fragment";
	private static final String LoginFragmentTag = "Login_Fragment";
	private static final String DetailFragmentTag = "Detail_Fragment";
	private static final String NewFragmentTag = "New_Fragment";
	private static final String UpdFragmentTag = "Modify_Fragment";
	private static final String FilterFragmentTag = "Filter_Fragment";
	private static final String SortFragmentTag = "Sort_Fragment";

	private boolean debug = false; // disable menuoption at startup
	private onFileSelectedListener _oc = null;
	private boolean dispAds;
	private FragmentManager fm = null;
	private long referenceTime = 0;
	private String urlArg = null;
	private int ticketArg = -1;
	private ShareActionProvider mShareActionProvider = null;
	private boolean doNotFinish = false;

	boolean mIsBound = false;
	Messenger mService = null;
	private HandlerThread mHandlerThread = null;
	private Messenger mMessenger = null;

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// tcLog.d(getClass().getName(),
			// "onServiceConnected className = " + className + " service = " +
			// service);
			sendMessageToService(Const.ServiceMsg.MSG_START_TIMER);
			mService = new Messenger(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// tcLog.d(getClass().getName(),
			// "onServiceDisconnected className = " + className);
			mService = null;
		}
	};

	static Const.ServiceMsg[] sm = Const.ServiceMsg.values();
	
	class IncomingHandler extends Handler {
		public IncomingHandler(Looper looper) {
			super(looper);
			// tcLog.d(getClass().getName(), "ServiceHandler");
		}

		@Override
		public void handleMessage(Message msg) {
			// tcLog.d(getClass().getName(), "handleMessage msg = " + msg);
			switch (sm[msg.what]) {
			case MSG_REQUEST_TICKET_COUNT:
				final int count = Tickets.getTicketCount();
				sendMessageToService(Const.ServiceMsg.MSG_SEND_TICKET_COUNT, count);
				break;
			case MSG_REQUEST_NEW_TICKETS:
				post(new Runnable() {
					@Override
					public void run() {
						final List<Integer> newTickets = getNewTickets(ISO8601.fromUnix(referenceTime));
						sendMessageToService(Const.ServiceMsg.MSG_SEND_NEW_TICKETS, newTickets);
					}
				});
				break;
			case MSG_REQUEST_REFRESH:
				// tcLog.d(getClass().getName(),
				// "handleMessage msg = REFRESH");
				refreshOverview();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToService(Const.ServiceMsg message) {
		// tcLog.d(getClass().getName(), "sendMessageToService message = "
		// + message);
		if (mIsBound && mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message.ordinal();
				msg.replyTo = mMessenger;
				// tcLog.d(getClass().getName(),
				// "sendMessageToService msg = " + msg);
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(getClass().getName(), "sendMessageToService failed", e);
			}
		}
	}

	private void sendMessageToService(Const.ServiceMsg message, int value) {
		// tcLog.d(getClass().getName(), "sendMessageToService message = "
		// + message);
		if (mIsBound && mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message.ordinal();
				msg.arg1 = value;
				msg.replyTo = mMessenger;
				// tcLog.d(getClass().getName(),
				// "sendMessageToService msg = " + msg);
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(getClass().getName(), "sendMessageToService failed", e);
			}
		}
	}

	private void sendMessageToService(Const.ServiceMsg message, Object value) {
		if (mIsBound && mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message.ordinal();
				msg.replyTo = mMessenger;
				msg.obj = value;
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(getClass().getName(), "sendMessageToService Object failed", e);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);

		// FragmentManager.enableDebugLogging(true);

		if (Const.doAnalytics) {
			MyTracker.getInstance(this);
		}
		
		mHandlerThread = new HandlerThread("IncomingHandler", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mMessenger = new Messenger(new IncomingHandler(mHandlerThread.getLooper()));

		startService(new Intent(this, RefreshService.class));

		setContentView(R.layout.tracstart);
		Credentials.getInstance(this);
		debug |= Credentials.isRCVersion();
		Credentials.loadCredentials();

		dispAds = true;
		if (getIntent().hasExtra(Const.ADMOB)) {
			dispAds = getIntent().getBooleanExtra(Const.ADMOB, true);
		} else if (savedInstanceState != null) {
			dispAds = savedInstanceState.getBoolean(Const.ADMOB, true);
		}

		urlArg = getIntent().getStringExtra(Const.INTENT_URL);
		ticketArg = (int) getIntent().getLongExtra(Const.INTENT_TICKET, -1);

		Tickets.url = Credentials.getUrl();
		Tickets.username = Credentials.getUsername();
		Tickets.password = Credentials.getPassword();
		Tickets.sslHack = Credentials.getSslHack();
		Tickets.sslHostNameHack = Credentials.getSslHostNameHack();
		Tickets.profile = Credentials.getProfile();

		if (Credentials.getFirstRun()) {
			final Intent launchTrac = new Intent(this, TracShowWebPage.class);
			final String filename = getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, false);
			startActivity(launchTrac);
		}

		if (urlArg != null) {
			final String urlArg1 = urlArg + "rpc";
			final String urlArg2 = urlArg + "login/rpc";
			if (!(urlArg.equals(Tickets.url) || urlArg1.equals(Tickets.url) || urlArg2.equals(Tickets.url))) {
				final ProfileDatabaseHelper pdb = new ProfileDatabaseHelper(this);
				LoginProfile lp = pdb.findProfile(urlArg2);
				if (lp == null) {
					lp = pdb.findProfile(urlArg1);
				}
				if (lp == null) {
					lp = pdb.findProfile(urlArg);
				}
				if (lp == null) {
					final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
					alertDialogBuilder.setTitle(R.string.wrongdb);
					final String wrongDb = getString(R.string.wrongdbtext1) + Tickets.url + getString(R.string.wrongdbtext2)
							+ urlArg + getString(R.string.wrongdbtext3);
					alertDialogBuilder.setMessage(wrongDb).setCancelable(false);
					alertDialogBuilder.setPositiveButton(R.string.oktext, null);
					alertDialogBuilder.setNegativeButton(R.string.cancel, null);
					final AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					urlArg = null;
					ticketArg = -1;
				} else {
					Tickets.url = lp.getUrl();
					Tickets.username = lp.getUsername();
					Tickets.password = lp.getPassword();
					Tickets.sslHack = lp.getSslHack();
					Tickets.sslHostNameHack = false; // force dialog to confirm
					Tickets.profile = null;
				}
			}
		}

		fm = getSupportFragmentManager();
		fm.addOnBackStackChangedListener(this);
		// Handle when activity is recreated like on orientation Change
		shouldDisplayHomeUp();

		if (savedInstanceState != null) {
			if (fm != null) {
				restoreFragment(savedInstanceState, ListFragmentTag);
				restoreFragment(savedInstanceState, LoginFragmentTag);
				restoreFragment(savedInstanceState, DetailFragmentTag);
				restoreFragment(savedInstanceState, NewFragmentTag);
				restoreFragment(savedInstanceState, UpdFragmentTag);
				restoreFragment(savedInstanceState, FilterFragmentTag);
				restoreFragment(savedInstanceState, SortFragmentTag);
				tcLog.d(getClass().getName(), "onCreate: backstack restored");
			}
			initializeList((TicketListFragment) getFragment(ListFragmentTag));
		} else {
			final FragmentTransaction ft = fm.beginTransaction();
                        if (Tickets.url != null && Tickets.url.length() > 0) {
                                final TicketListFragment ticketListFragment = new TicketListFragment();
                                final Bundle args = makeArgs();
                                args.putString("currentProfile", Tickets.profile);
                                args.putString("currentFilter", Credentials.getFilterString());
                                args.putString("currectSortOrder", Credentials.getSortString());
                                if (urlArg != null) {
                                        tcLog.d(getClass().getName(), "select Ticket = " + ticketArg);
                                        if (ticketListFragment != null) {
                                                args.putInt("TicketArg", ticketArg);
                                        }
                                        urlArg = null;
                                        ticketArg = -1;
                                }
                                ticketListFragment.setArguments(args);
                                ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
			} else {
                                final TracLoginFragment tracLoginFragment = new TracLoginFragment();
                                ft.add(R.id.displayList, tracLoginFragment, LoginFragmentTag);
                        }
			ft.addToBackStack("onCreateInitBackstack");
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
			tcLog.d(getClass().getName(), "onCreate: backstack initiated");
		}

		bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		setReferenceTime();
	}

	private void restoreFragment(Bundle savedInstanceState, final String tag) {
		if (savedInstanceState.containsKey(tag)) {
			try {
				fm.getFragment(savedInstanceState, tag);
			} catch (final Exception e) {
			}
		}
	}

	private void saveFragment(Bundle savedInstanceState, final String tag) {
		try {
			final Fragment f = getFragment(tag);
			if (f != null) {
				fm.putFragment(savedInstanceState, tag, f);
			}
		} catch (final Exception e) {
			// Exception if fragment not on stack can be ignored
		}
	}

	public void shouldDisplayHomeUp() {
		// Enable Up button only if there are entries in the back stack
		final boolean canBack = fm.getBackStackEntryCount() > 1;
		tcLog.d(getClass().getName(), "shouldDisplayHomeUp canBack = " + canBack);
		getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
	}

	@Override
	public boolean onSupportNavigateUp() {
		tcLog.d(getClass().getName(), "onSupportNavigateUp entry count = " + fm.getBackStackEntryCount());
		// This method is called when the up button is pressed. Just the pop
		// back stack.
		fm.popBackStack();
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		tcLog.d(getClass().getName(), "onPause ");
		/* save logfile when exiting */
		tcLog.d(getClass().getName(), "onPause  isFinishing = " + isFinishing());
		if (isFinishing() && Credentials.isRCVersion()) {
			File path = null;
			File file = null;
			try {
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				tcLog.d(getClass().getName(), "path =  " + path);
				path.mkdirs();

				file = new File(path, "tc-log.txt");
				final OutputStream os = new FileOutputStream(file);
				os.write(tcLog.getDebug().getBytes());
				os.close();
				tcLog.d(getClass().getName(), "File saved  =  " + file);
			} catch (final Exception e) {
				tcLog.e(getClass().getName(), "Exception while saving logfile on " + path + " " + file, e);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("Admob", dispAds);
		if (fm != null) {
			saveFragment(savedInstanceState, ListFragmentTag);
			saveFragment(savedInstanceState, LoginFragmentTag);
			saveFragment(savedInstanceState, DetailFragmentTag);
			saveFragment(savedInstanceState, NewFragmentTag);
			saveFragment(savedInstanceState, UpdFragmentTag);
			saveFragment(savedInstanceState, FilterFragmentTag);
			saveFragment(savedInstanceState, SortFragmentTag);
		}
		tcLog.d(getClass().getName(), "onSaveInstanceState savedInstanceState = " + savedInstanceState);
	}

	@Override
	public void onAttachFragment(final Fragment frag) {
		tcLog.d(getClass().getName(), "onAttachFragment " + frag);
		Credentials.getInstance(this);
		if (ListFragmentTag.equals(frag.getTag())) {
			initializeList((TicketListFragment)frag);
		}
	}

	public void initializeList(final TicketListFragment ticketListFragment) {
		if (ticketListFragment != null) {
			tcLog.d(getClass().getName(), "initializeList ticketListFragment = " + ticketListFragment);
			ticketListFragment.setHost();
			setFilter(Credentials.getFilterString());
			setSort(Credentials.getSortString());

			if (urlArg != null) {
				tcLog.d(getClass().getName(), "select Ticket = " + ticketArg);
				if (ticketListFragment != null) {
					ticketListFragment.selectTicket(ticketArg);
				}
				urlArg = null;
				ticketArg = -1;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		tcLog.d(getClass().getName(), "onCreateOptionsMenu");
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracstartmenu, menu);

		final MenuItem item = menu.findItem(R.id.debug);
		mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
		if (mShareActionProvider == null) {
			tcLog.d(getClass().getName(), "onCreateOptionsMenu create new shareActionProvider");
			mShareActionProvider = new ShareActionProvider(this);
			MenuItemCompat.setActionProvider(item, mShareActionProvider);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		tcLog.d(getClass().getName(), "onPrepareOptionsMenu");
		final MenuItem item = menu.findItem(R.id.debug);
		item.setVisible(debug).setEnabled(debug);
		if (debug) {
			final Intent sendIntent = shareDebug();
			// tcLog.d(getClass().getName(), "item = " + item + " " + mShareActionProvider + " " + sendIntent);
			mShareActionProvider.setShareIntent(sendIntent);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
		final int itemId = item.getItemId();
		if (itemId == R.id.over) {
			final Intent launchTrac = new Intent(getApplicationContext(), TracShowWebPage.class);
			final String filename = getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra(Const.HELP_FILE, filename);
			launchTrac.putExtra(Const.HELP_VERSION, true);
			startActivity(launchTrac);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onTicketSelected(Ticket ticket) {
		tcLog.d(getClass().getName(), "onTicketSelected Ticket: " + ticket.getTicketnr());
		final DetailFragment detailFragment = new DetailFragment();
		final Bundle args = makeArgs();
		args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
		detailFragment.setArguments(args);
		// tcLog.d(getClass().getName(), "detailFragment =" +
		// detailFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, detailFragment, DetailFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onTicketSelected");
		ft.commit();
	}

	@Override
	public void onNewTicket() {
		tcLog.d(getClass().getName(), "onNewTicket ");

		final NewTicketFragment newtickFragment = new NewTicketFragment();
		// tcLog.d(getClass().getName(), "newTickFragment =" +
		// newtickFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onNewTicket");
		ft.commit();
	}

	@Override
	public void onUpdateTicket(Ticket ticket) {
		tcLog.d(getClass().getName(), "onUpdateTicket ticket = " + ticket);

		final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
		final Bundle args = makeArgs();
		args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
		updtickFragment.setArguments(args);
		// tcLog.d(getClass().getName(), "updtickFragment = " +
		// updtickFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, updtickFragment, UpdFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onUpdateTicket");
		ft.commit();
	}

	@Override
	public void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
		tcLog.d(getClass().getName(), "onLogin " + newProfile);
		Tickets.url = newUrl;
		Tickets.username = newUser;
		Tickets.password = newPass;
		Tickets.sslHack = newHack;
		Tickets.sslHostNameHack = newHostNameHack;
		Tickets.profile = newProfile;
		TicketModel.newInstance();
		Tickets.setInvalid();
		TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);
		final FragmentTransaction ft = fm.beginTransaction();
		fm.popBackStack();
		if (ticketListFragment == null) {
            ticketListFragment = new TicketListFragment();
            final Bundle args = makeArgs();
            args.putString("currentProfile", Tickets.profile);
            args.putString("currentFilter", Credentials.getFilterString());
            args.putString("currectSortOrder", Credentials.getSortString());
            ticketListFragment.setArguments(args);
			doNotFinish = true;
            ft.replace(R.id.displayList, ticketListFragment, ListFragmentTag);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.addToBackStack("onLogin");
       } else {
			initializeList(ticketListFragment);
		}
		ft.commit();
	}

	private Bundle makeArgs() {
		final Bundle args = new Bundle();
		// args.putString(Const.CURRENT_URL, Tickets.url);
		// args.putString(Const.CURRENT_USERNAME, Tickets.username);
		// args.putString(Const.CURRENT_PASSWORD, Tickets.password);
		// args.putBoolean(Const.CURRENT_SSLHACK, Tickets.sslHack);
		// args.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, Tickets.sslHostNameHack);
		return args;
	}

	@Override
	public void setFilter(ArrayList<FilterSpec> filter) {
		tcLog.d(getClass().getName(), "setFilter " + filter);
		String filterString = "";
		if (filter != null) {
			for (final FilterSpec fs : filter) {
				if (filterString.length() > 0) {
					filterString += "&";
				}
				filterString += fs.toString();
			}
		}
		Credentials.storeFilterString(filterString);

		final TicketListFragment tlf = (TicketListFragment) getFragment(ListFragmentTag);
		if (tlf != null) {
			tlf.setFilter(filter);
			refreshOverview();
//		} else {
//			tcLog.toast("setFilter TicketListFragment is null");
		}
	}

	@Override
	public void setFilter(String filterString) {
		// tcLog.d(getClass().getName(), "setFilter " + filterString);
		final ArrayList<FilterSpec> filter = new ArrayList<FilterSpec>();

		if (filterString.length() > 0) {
			String[] fs;
			try {
				fs = filterString.split("\\&");
			} catch (final IllegalArgumentException e) {
				fs = new String[1];
				fs[0] = filterString;
			}
			final String[] operators = getResources().getStringArray(R.array.filter2_choice);
			for (final String f : fs) {
				filter.add(new FilterSpec(f, operators));
			}
		}
		setFilter(filter);
	}

	@Override
	public void setSort(ArrayList<SortSpec> sort) {
		tcLog.d(getClass().getName(), "setSort " + sort);

		String sortString = "";
		if (sort != null) {
			for (final SortSpec s : sort) {
				if (sortString.length() > 0) {
					sortString += "&";
				}
				sortString += s.toString();
			}
		}
		Credentials.storeSortString(sortString);

		final TicketListFragment tlf = (TicketListFragment) getFragment(ListFragmentTag);
		if (tlf != null) {
			tlf.setSort(sort);
			refreshOverview();
//		} else {
//			tcLog.toast("setSort TicketListFragment is null");
		}
	}

	@Override
	public void setSort(String sortString) {
		// tcLog.d(getClass().getName(), "setSort " + sortString);
		final ArrayList<SortSpec> sl = new ArrayList<SortSpec>();
		if (sortString.length() > 0) {
			String[] sort;
			try {
				sort = sortString.split("\\&");
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
						if (s1.equalsIgnoreCase("desc=1")) {
							richting = false;
							i++;
						}
					}
					sl.add(new SortSpec(veld, richting));
				}
			}
		}
		this.setSort(sl);
	}

	@Override
	public void onChangeHost() {
		tcLog.d(getClass().getName(), "onChangeHost");
		final FragmentTransaction ft = fm.beginTransaction();
		final TracLoginFragment tracLoginFragment = new TracLoginFragment();
		ft.replace(R.id.displayList, tracLoginFragment, LoginFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onChangeHost");
		ft.commit();
	}

	@Override
	public void onFilterSelected(ArrayList<FilterSpec> filterList) {
		tcLog.d(getClass().getName(), "onFilterSelected");
		final FragmentTransaction ft = fm.beginTransaction();
		final FilterFragment filterFragment = new FilterFragment();
		ft.replace(R.id.displayList, filterFragment, FilterFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onFilterSelected");
		ft.commit();
		filterFragment.setList(filterList);
	}

	@Override
	public void onSortSelected(ArrayList<SortSpec> sortList) {
		tcLog.d(getClass().getName(), "onSortSelected");
		final FragmentTransaction ft = fm.beginTransaction();
		final SortFragment sortFragment = new SortFragment();
		ft.replace(R.id.displayList, sortFragment, SortFragmentTag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack("onSortSelected");
		ft.commit();
		sortFragment.setList(sortList);
	}

	@Override
	public void onChooserSelected(onFileSelectedListener oc) {
		tcLog.d(getClass().getName(), "onChooserSelected");
		// save callback
		_oc = oc;
		// Use the GET_CONTENT intent from the utility class
		final Intent target = FileUtils.createGetContentIntent();
		// Create the chooser Intent
		final Intent intent = Intent.createChooser(target, getString(R.string.chooser_title));
		try {
			startActivityForResult(intent, REQUEST_CODE);
		} catch (final ActivityNotFoundException e) {
			// The reason for the existence of aFileChooser
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		tcLog.d(getClass().getName(), "onActivityResult requestcode = " + requestCode);
		switch (requestCode) {
		case REQUEST_CODE:
			// If the file selection was successful
			if (resultCode == RESULT_OK) {
				if (data != null) {
					// Get the URI of the selected file
					final Uri uri = data.getData();

					try {
						// Create a file instance from the URI
						final File file = FileUtils.getFile(uri);
						tcLog.d(getClass().getName(), "File Selected: " + file.getAbsolutePath());
						if (_oc != null) {
							_oc.onSelected(file.getAbsolutePath());
						}
					} catch (final Exception e) {
						tcLog.d("FileSelectorTestActivity", "File select error", e);
					}
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onStart() {
		super.onStart();
		tcLog.d(getClass().getName(), "onStart");
		if (Const.doAnalytics) {
			MyTracker.reportActivityStart(this);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		tcLog.d(getClass().getName(), "onStop");
		if (Const.doAnalytics) {
			MyTracker.reportActivityStop(this);
		}
	}

	@Override
	public void onBackPressed() {
		tcLog.d(getClass().getName(), "onBackPressed");
		final DetailFragment df = (DetailFragment) getFragment(DetailFragmentTag);
		final boolean callSuper = df != null ? !df.onBackPressed() : true;
		if (callSuper) {
			super.onBackPressed();
		}
	}

	@Override
	public void onDestroy() {
		tcLog.d(getClass().getName(), "onDestroy");
		if (mIsBound) {
			sendMessageToService(Const.ServiceMsg.MSG_STOP_TIMER);
			unbindService(mConnection);
			mIsBound = false;
		}
		// stopService(new Intent(this, RefreshService.class));
		super.onDestroy();
	}

	@Override
	public boolean dispAds() {
		return dispAds;
	}

	@Override
	public void setDispAds(boolean b) {
		dispAds = b;
	}

	@Override
	public void shareTicket(final Ticket _ticket) {
		if (_ticket != null && _ticket.hasdata()) {
			final Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, _ticket.toText());
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
		}
	}

	@Override
	public void refreshOverview() {
		final TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);
//		tcLog.d(getClass().getName(), "refreshOverview ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			ticketListFragment.forceRefresh();
			setReferenceTime();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void enableDebug() {
//		tcLog.d(getClass().getName(), "enableDebug");
		debug = true;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
		tcLog.toast("Debug enabled");
	}

	private Intent shareDebug() {
		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, tcLog.getDebug());
		sendIntent.setType("text/plain");
		return sendIntent;
	}

	private Fragment getFragment(final String tag) {
		return fm.findFragmentByTag(tag);
	}

	private List<Integer> getNewTickets(final String isoTijd) {
		final TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);
//		tcLog.d(getClass().getName(), "getNewTickets ticketListFragment = " + ticketListFragment);
		return ticketListFragment != null ? ticketListFragment.getNewTickets(isoTijd) : null;
	}

	@Override
	public void setReferenceTime() {
		// tcLog.d(getClass().getName(), "setReferenceTime");
		referenceTime = System.currentTimeMillis() - timerCorr;
		sendMessageToService(Const.ServiceMsg.MSG_REMOVE_NOTIFICATION);
		sendMessageToService(Const.ServiceMsg.MSG_STOP_TIMER);
		sendMessageToService(Const.ServiceMsg.MSG_START_TIMER);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		boolean ret = super.dispatchTouchEvent(ev);
		final DetailFragment df = (DetailFragment) getFragment(DetailFragmentTag);
		if (df != null) {
			ret |= df.dispatchTouchEvent(ev);
		}
		final TicketListFragment tf = (TicketListFragment) getFragment(ListFragmentTag);
		if (tf != null) {
			ret |= tf.dispatchTouchEvent(ev);
		}
		return ret;
	}

	@Override
	public void onBackStackChanged() {
		final int depth = fm.getBackStackEntryCount();
//		tcLog.d(getClass().getName(), "onBackStackChanged depth = " + depth);
		if (depth == 0 && !doNotFinish) {
			finish();
		}
		doNotFinish = false;
	}
}