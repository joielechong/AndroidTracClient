package com.mfvl.trac.client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
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
import com.mfvl.trac.client.util.SortSpec;
import com.mfvl.trac.client.util.tcLog;

interface onFileSelectedListener {
	void onSelected(final String f);
}

interface InterFragmentListener {
	boolean dispAds();

	void onTicketSelected(Ticket ticket);

	void onNewTicket();

	void onLogin(String url, String username, String password, boolean sslHack);

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
	private TicketModel tm = null;
	// onActivityResult requestcode for filechooser
	private static final int REQUEST_CODE = 6384;
	private onFileSelectedListener _oc = null;
	private boolean dispAds;
	private boolean debug = false; // disable menuoption at startup
	private FragmentManager fm = null;
	private Timer monitorTimer = null;
	private static final int timerStart = 5 * 60 * 1000; // 5 minuten
	private static final int timerPeriod = 5 * 60 * 1000; // 5 minuten
	private static final int timerCorr = 60 * 1000 * 2; // 2 minuten
	private long referenceTime = 0;
	private static final int notifId = 1234;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(this.getClass().getName(), "onCreate savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));

		setReferenceTime();
		setContentView(R.layout.tracstart);
		Credentials.loadCredentials(this);

		try {
			final Intent i = getIntent();
			dispAds = i.getBooleanExtra("AdMob", true);
		} catch (final Exception e) {
			dispAds = true;
		}

		final ActionBar ab = getSupportActionBar();
		if (ab != null) {
			// ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			ab.show();
		}

		url = Credentials.getUrl();
		username = Credentials.getUsername();
		password = Credentials.getPassword();
		sslHack = Credentials.getSslHack();

		if (Credentials.getFirstRun(this)) {
			final Intent launchTrac = new Intent(this, TracShowWebPage.class);
			final String filename = getString(R.string.whatsnewhelpfile);
			launchTrac.putExtra("file", filename);
			launchTrac.putExtra("version", false);
			startActivity(launchTrac);
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
		/*
		 * this is extra code when using a split screen on e.g. a tablet
		 * 
		 * if (findViewById(R.id.displayDetail) != null) { detailPage = true;
		 * fm.popBackStack();
		 * 
		 * detailFragment = (DetailFragment)
		 * fmfindFragmentById(R.id.displayDetail); if (detailFragment == null) {
		 * final FragmentTransaction ft = fm.beginTransaction(); detailFragment
		 * = new DetailFragment(); ft.replace(R.id.displayDetail,
		 * detailFragment, "Detail_Fragment1");
		 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		 * ft.commit(); } detailFragment.setHost(url, username, password,
		 * sslHack); }
		 */

		monitorTimer = new Timer("monitorTickets");
		monitorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				tcLog.d(this.getClass().getName(), "timertask started");
				final int count = getTicketCount();
				if (count > 0) {
					final List<Integer> newTickets = getNewTickets(ISO8601.fromUnix(referenceTime));
					if (newTickets != null) {
						if (newTickets.size() > 0) {
							final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(TracStart.this)
									.setSmallIcon(R.drawable.traclogo).setContentTitle(TracStart.this.getString(R.string.notifmod))
									.setContentText(TracStart.this.getString(R.string.foundnew) + " " + newTickets);
							final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							mNotificationManager.notify(notifId, mBuilder.build());
						}
					}
				}
			}
		}, timerStart, timerPeriod);
	}

	@Override
	public void initializeList() {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		tcLog.d(this.getClass().getName(), "initializeList ticketListFragment = " + ticketListFragment);
		ticketListFragment.setHost(url, username, password, sslHack);
		setFilter(Credentials.getFilterString(this));
		setSort(Credentials.getSortString(this));
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
		tcLog.d(this.getClass().getName(), "onOptionsItemSelected item=" + item);
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

		/*
		 * if (detailPage) { detailFragment = (DetailFragment)
		 * fm.findFragmentById(R.id.displayDetail); detailFragment.setHost(url,
		 * username, password, sslHack);
		 * detailFragment.updateTicketContent(ticket); } else {
		 */
		final DetailFragment detailFragment = new DetailFragment();
		tcLog.d(this.getClass().getName(), "detailFragment =" + detailFragment.toString());
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, detailFragment, "Detail_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		detailFragment.setHost(url, username, password, sslHack);
		detailFragment.setTicketContent(ticket);
		/*
		 * }
		 */
	}

	@Override
	public void onNewTicket() {
		tcLog.d(this.getClass().getName(), "onNewTicket ");

		final NewTicketFragment newtickFragment = new NewTicketFragment();
		tcLog.d(this.getClass().getName(), "detailFragment =" + newtickFragment.toString());
		/*
		 * if (detailPage) { final FragmentTransaction ft =
		 * fm.beginTransaction(); ft.replace(R.id.displayDetail,
		 * newtickFragment, "New_Fragment1");
		 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		 * ft.addToBackStack(null); ft.commit(); newtickFragment.setHost(url,
		 * username, password, sslHack); } else {
		 */
		newtickFragment.setHost(url, username, password, sslHack);
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, newtickFragment, "New_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		/*
		 * }
		 */
	}

	@Override
	public void onUpdateTicket(Ticket ticket) {
		tcLog.d(this.getClass().getName(), "onUpdateTicket ticket = " + ticket);

		final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
		tcLog.d(this.getClass().getName(), "detailFragment = " + updtickFragment.toString());
		/*
		 * if (detailPage) { final FragmentTransaction ft =
		 * fm.beginTransaction(); ft.replace(R.id.displayExtra, updtickFragment,
		 * "Modify_Fragment1");
		 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		 * ft.addToBackStack(null); ft.commit(); updtickFragment.setHost(url,
		 * username, password, sslHack); updtickFragment.loadTicket(ticket); }
		 * else {
		 */
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, updtickFragment, "Modify_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		updtickFragment.setHost(url, username, password, sslHack);
		updtickFragment.loadTicket(ticket);
		/*
		 * }
		 */
	}

	@Override
	public void onLogin(String newUrl, String newUser, String newPass, boolean newHack) {
		tcLog.d(this.getClass().getName(), "onLogin");
		tm = null;
		url = newUrl;
		username = newUser;
		password = newPass;
		sslHack = newHack;
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
		TracLoginFragment tracLoginFragment = (TracLoginFragment) fm.findFragmentByTag("tcLog.din_Fragment");
		if (tracLoginFragment == null) {
			tracLoginFragment = new TracLoginFragment();
		}
		ft.replace(R.id.displayList, tracLoginFragment, "tcLog.din_Fragment");
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
	public void onPause() {
		tcLog.d(this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
		tcLog.d(this.getClass().getName(), "onResume");
		super.onResume();
	}

	@Override
	public void onResumeFragments() {
		tcLog.d(this.getClass().getName(), "onResumeFragments");
		super.onResumeFragments();
	}

	@Override
	public void onRestart() {
		tcLog.d(this.getClass().getName(), "onRestart");
		super.onRestart();
	}

	@Override
	public void onStop() {
		tcLog.d(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onDestroy() {
		tcLog.d(this.getClass().getName(), "onDestroy");
		if (monitorTimer != null) {
			monitorTimer.cancel();
		}
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(notifId);
		super.onDestroy();
	}

	@Override
	public boolean dispAds() {
		return dispAds;
	}

	public String getUrl() {
		tcLog.d(this.getClass().getName(), "getUrl url = " + url);
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
		tcLog.d(this.getClass().getName(), "refreshOverview ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			ticketListFragment.forceRefresh();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void enableDebug() {
		tcLog.d(this.getClass().getName(), "enableDebug");
		debug = true;
		tcLog.toast("Debug enabled");
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			this.invalidateOptionsMenu();
		}
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
		tcLog.d(this.getClass().getName(), "getTicketCount ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			return ticketListFragment.getTicketCount();
		}
		return -1;
	}

	private List<Integer> getNewTickets(final String isoTijd) {
		final TicketListFragment ticketListFragment = (TicketListFragment) fm.findFragmentByTag("List_Fragment");
		tcLog.d(this.getClass().getName(), "getNewTickets ticketListFragment = " + ticketListFragment);
		if (ticketListFragment != null) {
			return ticketListFragment.getNewTickets(isoTijd);
		}
		return null;
	}

	@Override
	public void setReferenceTime() {
		tcLog.d(this.getClass().getName(), "setReferenceTime");
		referenceTime = System.currentTimeMillis() - timerCorr;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(notifId);
	}
}
