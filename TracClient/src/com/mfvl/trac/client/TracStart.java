package com.mfvl.trac.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
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

interface InterFragmentListener {
	boolean dispAds();

	void onTicketSelected(Ticket ticket);

	void onNewTicket();

	void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile);

	void onChangeHost();

	void onUpdateTicket(Ticket ticket);

	TicketModel getTicketModel();

	void refreshOverview();

	void setFilter(ArrayList<FilterSpec> filter);

	void setSort(ArrayList<SortSpec> sort);

	void onChooserSelected(onFileSelectedListener oc);

	void onFilterSelected(ArrayList<FilterSpec> filterList);

	void onSortSelected(ArrayList<SortSpec> sortList);

	void shareTicket(Ticket t);

	void initializeList();

	void enableDebug();

	void setReferenceTime();
}

public class TracStart extends ActionBarActivity implements InterFragmentListener {
	private String url;
	private String username;
	private String password;
	private boolean sslHack;
	private boolean sslHostNameHack;
	private String profile;
	private TicketModel tm = null;
	// onActivityResult requestcode for filechooser
	private static final int REQUEST_CODE = 6384;
	private onFileSelectedListener _oc = null;
	private boolean dispAds;
	private boolean debug = false; // disable menuoption at startup
	private FragmentManager fm = null;
	private long referenceTime = 0;
	private static final int timerCorr = 60 * 1000 * 2; // 2 minuten
	String urlArg = null;
	int ticketArg = -1;

	Messenger mService = null;
	boolean mIsBound = false;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// tcLog.d(this.getClass().getName(),
			// "onServiceConnected className = " + className + " service = " +
			// service);
			mService = new Messenger(service);
			sendMessageToService(RefreshService.MSG_START_TIMER);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			// tcLog.d(this.getClass().getName(),
			// "onServiceDisconnected className = " + className);
			mService = null;
		}
	};

	@SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// tcLog.d(this.getClass().getName(), "handleMessage msg = " + msg);
			switch (msg.what) {
			case RefreshService.MSG_REQUEST_TICKET_COUNT:
				final int count = getTicketCount();
				sendMessageToService(RefreshService.MSG_SEND_TICKET_COUNT, count);
				break;
			case RefreshService.MSG_REQUEST_NEW_TICKETS:
				new Thread() {
					@Override
					public void run() {
						final List<Integer> newTickets = getNewTickets(ISO8601.fromUnix(referenceTime));
						sendMessageToService(RefreshService.MSG_SEND_NEW_TICKETS, newTickets);
					}
				}.start();
				break;
			case RefreshService.MSG_REQUEST_REFRESH:
				refreshOverview();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToService(int message) {
		// tcLog.d(this.getClass().getName(), "sendMessageToService message = "
		// + message);
		if (mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message;
				msg.arg1 = -1;
				msg.arg2 = -1;
				msg.replyTo = mMessenger;
				// tcLog.d(this.getClass().getName(),
				// "sendMessageToService msg = " + msg);
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(this.getClass().getName(), "sendMessageToService failed", e);
			}
		}
	}

	private void sendMessageToService(int message, int value) {
		// tcLog.d(this.getClass().getName(), "sendMessageToService message = "
		// + message);
		if (mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message;
				msg.arg1 = value;
				msg.arg2 = -1;
				msg.replyTo = mMessenger;
				tcLog.d(this.getClass().getName(), "sendMessageToService msg = " + msg);
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(this.getClass().getName(), "sendMessageToService failed", e);
			}
		}
	}

	private void sendMessageToService(int message, Object value) {
		if (mService != null) {
			try {
				final Message msg = Message.obtain();
				msg.what = message;
				msg.arg1 = -1;
				msg.arg2 = -1;
				msg.replyTo = mMessenger;
				msg.obj = value;
				mService.send(msg);
			} catch (final RemoteException e) {
				tcLog.e(this.getClass().getName(), "sendMessageToService Object failed", e);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));

		startService(new Intent(this, RefreshService.class));

		setContentView(R.layout.tracstart);
		Credentials.loadCredentials(this);

		dispAds = getIntent().getBooleanExtra("AdMob", true);

		urlArg = getIntent().getStringExtra("url");
		ticketArg = (int) getIntent().getLongExtra("ticket", -1);

		final ActionBar ab = getSupportActionBar();
		if (ab != null) {
			// ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			ab.show();
		}

		url = Credentials.getUrl();
		username = Credentials.getUsername();
		password = Credentials.getPassword();
		sslHack = Credentials.getSslHack();
		sslHostNameHack = Credentials.getSslHostNameHack();
		profile = Credentials.getProfile();

		if (Credentials.getFirstRun(this)) {
			final Intent launchTrac = new Intent(this, TracShowWebPage.class);
			final String filename = getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
		}

		if (urlArg != null) {
			final String urlArg1 = urlArg + "rpc";
			final String urlArg2 = urlArg + "login/rpc";
			if (!(urlArg.equals(url) || urlArg1.equals(url) || urlArg2.equals(url))) {
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
					final String wrongDb = getString(R.string.wrongdbtext1) + url + getString(R.string.wrongdbtext2) + urlArg
							+ getString(R.string.wrongdbtext3);
					alertDialogBuilder.setMessage(wrongDb).setCancelable(false);
					alertDialogBuilder.setPositiveButton(R.string.oktext, null);
					alertDialogBuilder.setNegativeButton(R.string.cancel, null);
					final AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					urlArg = null;
					ticketArg = -1;
				} else {
					url = lp.getUrl();
					username = lp.getUsername();
					password = lp.getPassword();
					sslHack = lp.getSslHack();
					sslHostNameHack = false; // force dialog to confirm
					profile = null;
				}
			}
		}

		fm = getSupportFragmentManager();
		if (savedInstanceState == null) {
			final FragmentTransaction ft = fm.beginTransaction();
			if (url.length() > 0) {
				final TicketListFragment ticketListFragment = new TicketListFragment();
				ft.add(R.id.displayList, ticketListFragment, "List_Fragment");
			} else {
				final TracLoginFragment tracLoginFragment = new TracLoginFragment();
				ft.add(R.id.displayList, tracLoginFragment, "Login_Fragment");
			}
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}

		bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		setReferenceTime();
	}

	@Override
	public void initializeList() {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		tcLog.d(this.getClass().getName(), "initializeList ticketListFragment = " + ticketListFragment);
		ticketListFragment.setHost(url, username, password, sslHack, sslHostNameHack, profile);
		setFilter(Credentials.getFilterString(this));
		setSort(Credentials.getSortString(this));

		if (urlArg != null) {
			tcLog.d(this.getClass().getName(), "select Ticket = " + ticketArg);
			if (ticketListFragment != null) {
				ticketListFragment.selectTicket(ticketArg);
			}
			urlArg = null;
			ticketArg = -1;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		tcLog.d(this.getClass().getName(), "onCreateOptionsMenu");
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracstartmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
		final int itemId = item.getItemId();
		if (itemId == R.id.over) {
			final Intent launchTrac = new Intent(getApplicationContext(), TracShowWebPage.class);
			final String filename = getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", true);
			startActivity(launchTrac);
		} else if (itemId == R.id.debug) {
			shareDebug();
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onTicketSelected(Ticket ticket) {
		tcLog.d(this.getClass().getName(), "onTicketSelected Ticket: " + ticket.getTicketnr());
		final DetailFragment detailFragment = new DetailFragment();
		tcLog.d(this.getClass().getName(), "detailFragment =" + detailFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, detailFragment, "Detail_Fragment");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		detailFragment.setHost(url, username, password, sslHack, sslHostNameHack);
		detailFragment.setTicketContent(ticket);
	}

	@Override
	public void onNewTicket() {
		tcLog.d(this.getClass().getName(), "onNewTicket ");

		final NewTicketFragment newtickFragment = new NewTicketFragment();
		tcLog.d(this.getClass().getName(), "detailFragment =" + newtickFragment.toString());
		newtickFragment.setHost(url, username, password, sslHack, sslHostNameHack);
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, newtickFragment, "New_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
	}

	@Override
	public void onUpdateTicket(Ticket ticket) {
		tcLog.d(this.getClass().getName(), "onUpdateTicket ticket = " + ticket);

		final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
		tcLog.d(this.getClass().getName(), "updtickFragment = " + updtickFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, updtickFragment, "Modify_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		updtickFragment.setHost(url, username, password, sslHack, sslHostNameHack);
		updtickFragment.loadTicket(ticket);
	}

	@Override
	public void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
		tcLog.d(this.getClass().getName(), "onLogin " + newProfile);
		tm = null;
		url = newUrl;
		username = newUser;
		password = newPass;
		sslHack = newHack;
		sslHostNameHack = newHostNameHack;
		profile = newProfile;
		TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		if (ticketListFragment != null) {
			initializeList();
		}

		if (!fm.popBackStackImmediate()) {
			tcLog.d(this.getClass().getName(), "onLogin popBackStackImmediate=false");
			ticketListFragment = new TicketListFragment();
			final FragmentTransaction ft = fm.beginTransaction();
			ft.replace(R.id.displayList, ticketListFragment, "List_Fragment");
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
		refreshOverview();
	}

	@Override
	public void setFilter(ArrayList<FilterSpec> filter) {
		final TicketListFragment tlf = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		tcLog.d(this.getClass().getName(), "setFilter " + filter);
		if (tlf != null) {
			tlf.setFilter(filter);
			refreshOverview();
		} else {
			tcLog.toast("setFilter fragment is null");
		}
		String filterString = "";
		if (filter != null) {
			for (final FilterSpec fs : filter) {
				if (filterString.length() > 0) {
					filterString += "&";
				}
				filterString += fs.toString();
			}
		}
		Credentials.storeFilterString(this, filterString);
	}

	public void setFilter(String filterString) {
		final ArrayList<FilterSpec> filter = new ArrayList<FilterSpec>();
		if (filterString.length() > 0) {
			String[] fs;
			try {
				fs = filterString.split("\\&");
			} catch (final IllegalArgumentException e) {
				fs = new String[1];
				fs[0] = filterString;
			}
			for (final String f : fs) {
				filter.add(new FilterSpec(f, this.getApplicationContext()));
			}
		}
		setFilter(filter);
	}

	@Override
	public void setSort(ArrayList<SortSpec> sort) {
		final TicketListFragment tlf = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		tcLog.d(this.getClass().getName(), "setSort " + sort);
		if (tlf != null) {
			tlf.setSort(sort);
			refreshOverview();
		} else {
			tcLog.toast("setSort fragment is null");
		}
		String sortString = "";
		if (sort != null) {
			for (final SortSpec s : sort) {
				if (sortString.length() > 0) {
					sortString += "&";
				}
				sortString += s.toString();
			}
		}
		Credentials.storeSortString(this, sortString);
	}

	public void setSort(String sortString) {
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
		tcLog.d(this.getClass().getName(), "onChangeHost");
		final FragmentTransaction ft = fm.beginTransaction();
		TracLoginFragment tracLoginFragment = (TracLoginFragment) fm.findFragmentByTag("Login_Fragment");
		if (tracLoginFragment == null) {
			tracLoginFragment = new TracLoginFragment();
		}
		ft.replace(R.id.displayList, tracLoginFragment, "Login_Fragment");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
	}

	@Override
	public void onFilterSelected(ArrayList<FilterSpec> filterList) {
		tcLog.d(this.getClass().getName(), "onFilterSelected");
		final FragmentTransaction ft = fm.beginTransaction();
		final FilterFragment filterFragment = new FilterFragment();
		ft.replace(R.id.displayList, filterFragment, "Filter_Fragment");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		filterFragment.setList(filterList);
	}

	@Override
	public void onSortSelected(ArrayList<SortSpec> sortList) {
		tcLog.d(this.getClass().getName(), "onSortSelected");
		final FragmentTransaction ft = fm.beginTransaction();
		final SortFragment sortFragment = new SortFragment();
		ft.replace(R.id.displayList, sortFragment, "Sort_Fragment");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		sortFragment.setList(sortList);
	}

	@Override
	public void onChooserSelected(onFileSelectedListener oc) {
		tcLog.d(this.getClass().getName(), "onChooserSelected");
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		tcLog.d(this.getClass().getName(), "onPrepareOptionsMenu");
		final MenuItem item = menu.findItem(R.id.debug);
		item.setVisible(debug);
		item.setEnabled(debug);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		tcLog.d(this.getClass().getName(), "onActivityResult requestcode = " + requestCode);
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
						tcLog.d(this.getClass().getName(), "File Selected: " + file.getAbsolutePath());
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
	public TicketModel getTicketModel() {
		tcLog.d(this.getClass().getName(), "getTicketModel");
		if (tm == null) {
			tm = new TicketModel();
		}
		return tm;
	}

	@Override
	public void onStart() {
		tcLog.d(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onBackPressed() {
		tcLog.d(this.getClass().getName(), "onBackPressed");
		final DetailFragment df = (DetailFragment) fm.findFragmentByTag("Detail_Fragment");
		boolean callSuper = true;
		if (df != null) {
			callSuper = !df.onBackPressed();
		}
		if (callSuper) {
			super.onBackPressed();
		}
	}

	@Override
	public void onStop() {
		tcLog.d(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onDestroy() {
		// tcLog.d(this.getClass().getName(), "onDestroy");
		sendMessageToService(RefreshService.MSG_STOP_TIMER);
		unbindService(mConnection);
		stopService(new Intent(this, RefreshService.class));
		super.onDestroy();
	}

	@Override
	public boolean dispAds() {
		return dispAds;
	}

	public String getUrl() {
		// tcLog.d(this.getClass().getName(), "getUrl url = " + url);
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean getSslHack() {
		return sslHack;
	}

	public boolean getSslHostNameHack() {
		return sslHack;
	}

	public String getProfile() {
		return profile;
	}

	@Override
	public void shareTicket(final Ticket _ticket) {
		if (_ticket != null && _ticket.hasdata()) {
			final Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, _ticket.toText());
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
		}
	}

	@Override
	public void refreshOverview() {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		// tcLog.d(this.getClass().getName(),
		// "refreshOverview ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			ticketListFragment.forceRefresh();
			setReferenceTime();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void enableDebug() {
		tcLog.d(this.getClass().getName(), "enableDebug");
		debug = true;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			this.invalidateOptionsMenu();
		}
		tcLog.toast("Debug enabled");
	}

	private void shareDebug() {
		final Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, tcLog.getDebug());
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}

	private int getTicketCount() {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		// tcLog.d(this.getClass().getName(),
		// "getTicketCount ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			return ticketListFragment.getTicketCount();
		}
		return -1;
	}

	private List<Integer> getNewTickets(final String isoTijd) {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		// tcLog.d(this.getClass().getName(),
		// "getNewTickets ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			return ticketListFragment.getNewTickets(isoTijd);
		}
		return null;
	}

	@Override
	public void setReferenceTime() {
		// tcLog.d(this.getClass().getName(), "setReferenceTime");
		referenceTime = System.currentTimeMillis() - timerCorr;
		sendMessageToService(RefreshService.MSG_REMOVE_NOTIFICATION);
		sendMessageToService(RefreshService.MSG_STOP_TIMER);
		sendMessageToService(RefreshService.MSG_START_TIMER);
	}
}
