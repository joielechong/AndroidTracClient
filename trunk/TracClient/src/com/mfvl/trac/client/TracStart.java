package com.mfvl.trac.client;

import java.io.File;
import java.util.ArrayList;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.FilterSpec;
import com.mfvl.trac.client.util.SortSpec;

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
}

public class TracStart extends FragmentActivity implements InterFragmentListener {
	private String url;
	private String username;
	private String password;
	private boolean sslHack;
	private TicketModel tm = null;
	private static final int REQUEST_CODE = 6384; // onActivityResult request
													// code
	private onFileSelectedListener _oc = null;
	private boolean dispAds;
	private TicketListFragment ticketListFragment = null;
	private FragmentManager fm = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");
		Log.i(this.getClass().getName(), "savedInstanceState = " + (savedInstanceState == null ? "null" : "not null"));

		setContentView(R.layout.tracstart);
		Credentials.loadCredentials(this);

		try {
			final Intent i = getIntent();
			dispAds = i.getBooleanExtra("AdMob", true);
		} catch (final Exception e) {
			dispAds = true;
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

		if (ticketListFragment == null) {
			ticketListFragment = new TicketListFragment();
		}
		
		fm = getSupportFragmentManager();

		if (savedInstanceState == null) {
			final FragmentTransaction ft = fm.beginTransaction();
			if (url.length() > 0) {
				ticketListFragment.setHost(url, username, password, sslHack);
				ft.add(R.id.displayList, ticketListFragment, "List_Fragment");
				setFilter(Credentials.getFilterString(this));
				setSort(Credentials.getSortString(this));
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
		 * fmfindFragmentById(R.id.displayDetail); if
		 * (detailFragment == null) { final FragmentTransaction ft =
		 * fm.beginTransaction(); detailFragment = new
		 * DetailFragment(); ft.replace(R.id.displayDetail, detailFragment,
		 * "Detail_Fragment1");
		 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		 * ft.commit(); } detailFragment.setHost(url, username, password,
		 * sslHack); }
		 */
	}

	@Override
	public void onTicketSelected(Ticket ticket) {
		Log.i(this.getClass().getName(), "onTicketSelected Ticket: " + ticket.getTicketnr());

		/*
		 * if (detailPage) { detailFragment = (DetailFragment)
		 * fm.findFragmentById(R.id.displayDetail);
		 * detailFragment.setHost(url, username, password, sslHack);
		 * detailFragment.updateTicketContent(ticket); } else {
		 */
		final DetailFragment detailFragment = new DetailFragment();
		Log.i(this.getClass().getName(), "detailFragment =" + detailFragment.toString());
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
		Log.i(this.getClass().getName(), "onNewTicket ");

		final NewTicketFragment newtickFragment = new NewTicketFragment();
		Log.i(this.getClass().getName(), "detailFragment =" + newtickFragment.toString());
		/*
		 * if (detailPage) { final FragmentTransaction ft =
		 * fm.beginTransaction();
		 * ft.replace(R.id.displayDetail, newtickFragment, "New_Fragment1");
		 * ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		 * ft.addToBackStack(null); ft.commit(); newtickFragment.setHost(url,
		 * username, password, sslHack); } else {
		 */
		final FragmentTransaction ft = fm.beginTransaction();
		ft.replace(R.id.displayList, newtickFragment, "New_Fragment2");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
		newtickFragment.setHost(url, username, password, sslHack);
		/*
		 * }
		 */}

	@Override
	public void onUpdateTicket(Ticket ticket) {
		Log.i(this.getClass().getName(), "onUpdateTicket ticket = " + ticket);

		final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
		Log.i(this.getClass().getName(), "detailFragment = " + updtickFragment.toString());
		/*
		 * if (detailPage) { final FragmentTransaction ft =
		 * fm.beginTransaction();
		 * ft.replace(R.id.displayExtra, updtickFragment, "Modify_Fragment1");
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
		Log.i(this.getClass().getName(), "onLogin");
		tm = null;
		url = newUrl;
		username = newUser;
		password = newPass;
		sslHack = newHack;
		if (ticketListFragment!=null) {
			// ticketList already started
			ticketListFragment.setHost(url, username, password, sslHack);
		}
		if ( ! fm.popBackStackImmediate()) {
			ticketListFragment = new TicketListFragment();
			final FragmentTransaction ft = fm.beginTransaction();
			ticketListFragment.setHost(url, username, password, sslHack);
			ft.replace(R.id.displayList, ticketListFragment, "List_Fragment");
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		}
		setFilter(Credentials.getFilterString(this));
		refreshOverview();
	}

	@Override
	public void setFilter(ArrayList<FilterSpec> filter) {
		Log.i(this.getClass().getName(), "setFilter " + filter);
		if (ticketListFragment != null) {
			ticketListFragment.setFilter(filter);
		}
		String filterString = null;
		for (final FilterSpec fs : filter) {
			if (filterString == null) {
				filterString = fs.toString();
			} else {
				filterString += "&" + fs;
			}
		}
		Credentials.storeFilterString(this, (filterString == null?"":filterString));
	}

	public void setFilter(String filterString) {
		final ArrayList<FilterSpec> filter = new ArrayList<FilterSpec>();
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
		ticketListFragment.setFilter(filter);
	}

	@Override
	public void setSort(ArrayList<SortSpec> sort) {
		Log.i(this.getClass().getName(), "setSort " + sort);
		if (ticketListFragment != null) {
			ticketListFragment.setSort(sort);
		}
		String sortString = null;
		for (final SortSpec fs : sort) {
			if (sortString == null) {
				sortString = fs.toString();
			} else {
				sortString += "&" + fs;
			}
		}
		Credentials.storeSortString(this, (sortString==null?"":sortString));
	}

	public void setSort(String sortString) {
		final ArrayList<SortSpec> sl = new ArrayList<SortSpec>();
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
		ticketListFragment.setSort(sl);
	}

	@Override
	public void onChangeHost() {
		Log.i(this.getClass().getName(), "onChangeHost");
		final FragmentTransaction ft = fm.beginTransaction();
		final TracLoginFragment tracLoginFragment = new TracLoginFragment();
		ft.replace(R.id.displayList, tracLoginFragment, "Login_Fragment");
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.addToBackStack(null);
		ft.commit();
	}

	@Override
	public void onFilterSelected(ArrayList<FilterSpec> filterList) {
		Log.i(this.getClass().getName(), "onFilterSelected");
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
		Log.i(this.getClass().getName(), "onSortSelected");
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
		Log.i(this.getClass().getName(), "onChooserSelected");
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
		Log.i(this.getClass().getName(), "onActivityResult requestcode = " + requestCode);
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
						Log.i(this.getClass().getName(), "File Selected: " + file.getAbsolutePath());
						if (_oc != null) {
							_oc.onSelected(file.getAbsolutePath());
						}
					} catch (final Exception e) {
						Log.e("FileSelectorTestActivity", "File select error", e);
					}
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public TicketModel getTicketModel() {
		Log.i(this.getClass().getName(), "getTicketModel");
		if (tm == null) {
			tm = new TicketModel();
		}
		return tm;
	}

	@Override
	public void onStart() {
		Log.i(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onPause() {
		Log.i(this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
		Log.i(this.getClass().getName(), "onResume");
		super.onResume();
	}

	@Override
	public void onResumeFragments() {
		Log.i(this.getClass().getName(), "onResumeFragments");
		super.onResumeFragments();
	}

	@Override
	public void onRestart() {
		Log.i(this.getClass().getName(), "onRestart");
		super.onRestart();
	}

	@Override
	public void onStop() {
		Log.i(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onDestroy() {
		Log.i(this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean dispAds() {
		return dispAds;
	}

	public String getUrl() {
		Log.i(this.getClass().getName(), "getUrl url = " + url);
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
		ticketListFragment.forceRefresh();
	}
}
