/*
 * Copyright (C) 2013,2014 Michiel van Loon
 *);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * Licensed under the Apache License, Version 2.0 (the "License"
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mfvl.trac.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;

interface onFileSelectedListener {
    void onFileSelected(final String f);
}


/**
 * Interface for the fragments to communicate with each other and the main activity
 *
 * @author Michiel
 *
 */
interface InterFragmentListener {
    boolean getDispAds();
    void enableDebug();
    void onChooserSelected(onFileSelectedListener oc);
    void onLogin(String url, String username, String password, boolean sslHack, boolean sslHostNameHack, String profile);
    void onTicketSelected(Ticket ticket);
    void onUpdateTicket(Ticket ticket);
    void refreshOverview();
    void setDispAds(boolean b);
    void setReferenceTime();
	void showAlertBox(final int titleres, final int message, final String addit);
	void startProgressBar(String message);
	void startProgressBar(int resid);
	void stopProgressBar();
	TicketModel getTicketModel();
	TicketListAdapter getAdapter();
	Ticket getTicket(int ticknr);
	Ticket refreshTicket(int ticknr);
	void putTicket(Ticket t); //TODO Implement method
	int getNextTicket(int i);
	int getPrevTicket(int i);
	int getTicketCount();
	int getTicketContentCount();
	boolean updateTicket(Ticket t,String action, String comment, String veld, String waarde, final boolean notify, Map<String, String> modVeld) throws Exception;
	int createTicket(Ticket t , boolean notify) throws Exception; //TODO notify not used
	void setActionProvider(Menu menu,int resid);
	Intent shareList();
	Intent shareTicket(final Ticket ticket);
	void listViewCreated();
	boolean isFinishing();
	Handler getHandler();
}

public class TracStart extends Activity implements LoaderManager.LoaderCallbacks<Tickets>, InterFragmentListener, OnBackStackChangedListener {
   
    /*
     * Constanten voor communicatie met de service en fragmenten
     */
	
    static final int MSG_START_TIMER = 1; 
    static final int MSG_REQUEST_TICKET_COUNT = 2;
    static final int MSG_SEND_TICKET_COUNT = 3;
    static final int MSG_REQUEST_NEW_TICKETS = 4;
    static final int MSG_SEND_NEW_TICKETS = 5;
    static final int MSG_REQUEST_REFRESH = 6;
    static final int MSG_STOP_TIMER = 7;
    static final int MSG_REMOVE_NOTIFICATION = 8;
	static final int MSG_START_PROGRESSBAR = 21;
	static final int MSG_STOP_PROGRESSBAR = 22;
	static final int MSG_SET_SORT = 23;
	static final int MSG_SET_FILTER = 24;
	static final int MSG_SHOW_DIALOG = 25;
	static final int MSG_DISPLAY_TICKET = 26;
	static final int MSG_ACK_START = 27;
	static final int MSG_START_LISTLOADER = 28;
	
	public static final String PROVIDER_MESSAGE = "com.mfvl.trac.client.message.provider";
	public static final String DATACHANGED_MESSAGE = "com.mfvl.trac.client.message.datachanged";
    private static final int REQUEST_CODE = 6384;

	public static String[] mDrawerTitles = null;
    public static int[] mDrawerIds = null;
	
    public ArrayList<SortSpec> sortList = null;
    public ArrayList<FilterSpec> filterList = null;

    private String profile = null;
    private String url = null;
    private String username = null;
    private String password = null;
    private boolean sslHack = false;
    private boolean sslHostNameHack = false;

    private DrawerLayout mDrawerLayout = null;
    private ActionBarDrawerToggle mDrawerToggle = null;
	private ListView mDrawerList = null;
    private int timerCorr = 0;
	
    private static final String ListFragmentTag = "List_Fragment";
    private static final String LoginFragmentTag = "Login_Fragment";
    private static final String DetailFragmentTag = "Detail_Fragment";
    private static final String NewFragmentTag = "New_Fragment";
    private static final String UpdFragmentTag = "Modify_Fragment";
    private static final String FilterFragmentTag = "Filter_Fragment";
    private static final String SortFragmentTag = "Sort_Fragment";

    private static final String TICKETLISTNAME = "ticketlistInt";

	static final String BUNDLE_ISOTIJD = "ISOTIJD";
	static final String BUNDLE_TICKET = "TICKET";
	
    private boolean debug = false; // disable menuoption at startup
    private onFileSelectedListener _oc = null;
    private boolean dispAds = true;
    private FragmentManager fm = null;
    private long referenceTime = 0;
    private String urlArg = null;
    private int ticketArg = -1;
    private boolean doNotFinish = false;
	private IncomingHandler tracStartHandler = null;
	private TicketModel tm = null;

    boolean mIsBound = false;
    Messenger mService = null;
    private MyHandlerThread mHandlerThread = null;
    private Messenger mMessenger = null;
	
	private boolean changesLoaderStarted = false;
	private boolean listLoaderStarted = false;
	private boolean ticketLoaderStarted = false;
	private boolean loaderStarted = false;
	private boolean hasTicketsLoadingBar = false;
	private Boolean ticketsLoading = false;
		
	private TicketListAdapter dataAdapter = null;
	
    private static final int LIST_LOADER = 1;
	private static final int CHANGES_LOADER = 2;
	private static final int TICKET_LOADER = 3;
	private static final int TICKET_LOADER_NOSHOW = 4;
	
	private Semaphore loadingActive = new Semaphore(1, true);	
	
    @Override
    public Loader<Tickets> onCreateLoader(int loaderID, Bundle bundle) {
        tcLog.d("" + loaderID + " " + bundle);

        /*
         * Takes action based on the ID of the Loader that's being created
         */
		LoginProfile lp = new LoginProfile(url,username,password,sslHack)
				.setSslHostNameHack(sslHostNameHack)
				.setFilterList(filterList)
				.setSortList(sortList);
        switch (loaderID) {
			case LIST_LOADER:
			hasTicketsLoadingBar = false;
            // Returns a new CursorLoader
			try {
				getTicketListFragment().startLoading();
			} catch (Exception e) {
				tcLog.e("LIST_LOADER cannot contact TicketListFragment");
			}
			if (ListFragmentTag.equals(getTopFragment())) {
				startProgressBar(getString(R.string.getlist) + (profile == null ? "" : "\n" + profile));
				hasTicketsLoadingBar = true;
			}
			loadingActive.acquireUninterruptibly();
			ticketsLoading = true;
            return new TicketLoader(this, lp);
			
			case CHANGES_LOADER:
			String isoTijd = bundle.getString(BUNDLE_ISOTIJD);
			return new TicketLoader(this,lp,isoTijd);
			
			case TICKET_LOADER:
			case TICKET_LOADER_NOSHOW:
			int ticknr = bundle.getInt(BUNDLE_TICKET);
			startProgressBar(getString(R.string.downloading)+" "+ticknr);
			return new TicketLoader(this,lp,new Ticket(ticknr));
			
		}
		return null;
	}
	
    @Override
    public void onLoadFinished(Loader<Tickets> loader, Tickets tl) {
        tcLog.d( loader + " " + loader.getId() + " " + tl);
		switch (loader.getId()) {
			case LIST_LOADER:
			synchronized(this) {
				if (hasTicketsLoadingBar) {
					stopProgressBar();
					hasTicketsLoadingBar = false;
				}
				ticketsLoading = false;
			}
			dataAdapter.clear();
			dataAdapter.addAll(tl);
			newDataAdapter(tl);
			try {
				getTicketListFragment().dataHasChanged();
			} catch (Exception e) {
				tcLog.e("LIST_LOADER cannot contact TicketListFragment");
			}
			if (loadingActive.availablePermits() == 0) {
				loadingActive.release();
			}
			break;
			
			case CHANGES_LOADER:
			List<Integer> newTickets = new ArrayList<>();
			
			for (Ticket t: tl.ticketList) {
				newTickets.add(t.getTicketnr());
			}
			
			sendMessageToService(MSG_SEND_NEW_TICKETS, newTickets);
			getLoaderManager().destroyLoader(CHANGES_LOADER);
			break;
			
			case TICKET_LOADER:
			stopProgressBar();
			if (tl != null ) {
				tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_DISPLAY_TICKET,tl.ticketList.get(0)));
			} else {
				showAlertBox(R.string.notfound,R.string.ticketnotfound,null);
			}
			getLoaderManager().destroyLoader(TICKET_LOADER);
			break;
			
			case TICKET_LOADER_NOSHOW:
			stopProgressBar();
			if (tl == null ) {
				showAlertBox(R.string.notfound,R.string.ticketnotfound,null);
			}
			getLoaderManager().destroyLoader(TICKET_LOADER_NOSHOW);
			break;
		}
	}

    @Override
    public void onLoaderReset(Loader<Tickets> loader) {
        tcLog.d(loader + " " + loader.getId());
		switch (loader.getId()) {
			case LIST_LOADER:
			hasTicketsLoadingBar = false;
			ticketsLoading = false;
			break;
			
			case TICKET_LOADER:
			case TICKET_LOADER_NOSHOW:
			ticketLoaderStarted = false;
			break;
			
			case CHANGES_LOADER:
			changesLoaderStarted = false;
			break;
		}
	}

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //tcLog.d("onServiceConnected className = " + className + " service = " + service);
            mService = new Messenger(service);
            sendMessageToService(MSG_START_TIMER);
            //tcLog.d("mService = " + mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            //tcLog.d("onServiceDisconnected className = " + className);
            mService = null;
        }
    };

    class IncomingHandler extends Handler {
		private ProgressDialog progressBar = null;

        public IncomingHandler(Looper looper) {
            super(looper);
            tcLog.logCall();
        }

        @Override
		@SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            tcLog.d( "msg = " + msg);
            switch (msg.what) {
				case MSG_REQUEST_TICKET_COUNT:
				if (!LoginFragmentTag.equals(getTopFragment())) {
					final int count = getTicketCount();
					sendMessageToService(MSG_SEND_TICKET_COUNT, count);
				}
                break;

				case MSG_REQUEST_NEW_TICKETS:
				if (!LoginFragmentTag.equals(getTopFragment())) {
					getNewTickets(ISO8601.fromUnix(referenceTime));
				}
                break;

				case MSG_REQUEST_REFRESH:
                // tcLog.d("handleMessage msg = REFRESH");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						refreshOverview();
					}
				});
                break;
				
				case MSG_START_PROGRESSBAR:
				final String message = (String) msg.obj;
				synchronized (this) {
					// tcLog.d("handleMessage msg = START_PROGRESSBAR string = "+message);
					if (progressBar == null) {
						progressBar = new ProgressDialog(TracStart.this);
						progressBar.setCancelable(true);
						if (message != null) {
							progressBar.setMessage(message);
						}
						progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						if (!TracStart.this.isFinishing()) {
							progressBar.show();
						}
					}
				}
				break;

				case MSG_STOP_PROGRESSBAR:
				synchronized (this) {
					// tcLog.d("handleMessage msg = STOP_PROGRESSBAR");
					if (progressBar != null) {
						if (!TracStart.this.isFinishing()) {
							progressBar.dismiss();
						}
						progressBar = null;
					}
				}
				break;
				
				case MSG_SET_SORT:
				setSort((ArrayList<SortSpec>)msg.obj);
				refreshOverview();
				break;
				
				case MSG_SET_FILTER:
				setFilter((ArrayList<FilterSpec>)msg.obj);
				refreshOverview();
				break;
				
				case MSG_SHOW_DIALOG:
				showAlertBox(msg.arg1, msg.arg2, (String) msg.obj);
				break;
				
				case MSG_DISPLAY_TICKET:
				final Ticket t = (Ticket)msg.obj;
				if (DetailFragmentTag.equals(getTopFragment())) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final DetailFragment d = (DetailFragment) getFragment(DetailFragmentTag);
							d.setTicket(t.getTicketnr());
						}
					});
				} else {
					onTicketSelected(t);
				}
				break;
				
				case MSG_START_LISTLOADER:
				if (listLoaderStarted) {
					getLoaderManager().restartLoader(LIST_LOADER, null, TracStart.this);
				} else {
					getLoaderManager().initLoader(LIST_LOADER, null, TracStart.this);
					listLoaderStarted = true;
				}
				break;

				default:
                super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToService(int message) {
        //tcLog.d( "sendMessageToService message = "+ message+" mService = "+mService);
        if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
                msg.replyTo = mMessenger;
                tcLog.d( "msg = " + msg);
                mService.send(msg);
            } catch (final RemoteException e) {
                tcLog.e( "failed", e);
            }
        }
    }

    private void sendMessageToService(int message, int value) {
        //tcLog.d( "sendMessageToService message = "+ message);
        if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
                msg.arg1 = value;
                msg.replyTo = mMessenger;
                // tcLog.d(
                // "sendMessageToService msg = " + msg);
                mService.send(msg);
            } catch (final RemoteException e) {
                tcLog.e( "failed", e);
            }
        }
    }

    private void sendMessageToService(int message, Object value) {
        //tcLog.d( "sendMessageToService message = "+ message+ " value = "+value);
         if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
                msg.replyTo = mMessenger;
                msg.obj = value;
                mService.send(msg);
            } catch (final RemoteException e) {
                tcLog.e( "Object failed", e);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @SuppressWarnings("rawtypes")
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
            //tcLog.d("onItemClick: parent = " + parent + " view = " + view + " position = " + position + " id = " + id + " mDrawerId = " + mDrawerIds[position]);
        }
    }
	
	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
				
		switch (mDrawerIds[position]) {
			case R.id.over:
			showAbout();
			break;
			
			case R.id.tlrefresh:
            refreshOverview();
			break;
			
			case R.id.tlnieuw:
            onNewTicket();
			break;
			
			case R.id.tlfilter:
            onFilterSelected(filterList);
			break;
			
			case R.id.tlsort:
            onSortSelected(sortList);
			break;
			
			case R.id.tlchangehost:
            onChangeHost();
			break;
			
			default:
            tcLog.toast(getClass().getName() + " selectItem: position = " + position + " mDrawerId = " + mDrawerIds[position]);
		}
//		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);
	}
	
	private String getTopFragment() {
		try {
			int bs = getFragmentManager().getBackStackEntryCount();
			return getFragmentManager().getBackStackEntryAt(bs - 1).getName();
		} catch (Exception e) {
			return null;
		}
	}

	private void showAbout() {
		//tcLog.d( "showAbout");
        final Intent launchTrac = new Intent(getApplicationContext(), TracShowWebPage.class);
        launchTrac.putExtra(Const.HELP_FILE, getString(R.string.whatsnewhelpfile));
        launchTrac.putExtra(Const.HELP_VERSION, true);
        startActivity(launchTrac);
	}
	
    private void setupDrawer() {
		ActionBar ab = getActionBar();
		if (mDrawerTitles == null || mDrawerIds == null) {
			PopupMenu p = new PopupMenu(this,findViewById(R.id.left_drawer));
			p.inflate(R.menu.drawermenu);
			Menu m = p.getMenu();
			
			ArrayList<String> s = new ArrayList<>();
			ArrayList<Integer> id = new ArrayList<>();
			
			mDrawerTitles = new String[m.size()];
			mDrawerIds = new int[m.size()];
			
			for (int i=0; i< m.size(); i++) {
				mDrawerTitles[i] = m.getItem(i).getTitle().toString();
				mDrawerIds[i] = m.getItem(i).getItemId();
			}
		}
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
                ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
				tcLog.d("view = " + view);
                super.onDrawerClosed(view);
//                ab.setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
				tcLog.d("drawerView = " + drawerView);
                super.onDrawerOpened(drawerView);
//                ab.setTitle(mDrawerTitle);
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, mDrawerTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());		
    }
	
	@SuppressWarnings("unchecked")	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d( "savedInstanceState = " + savedInstanceState);

		if (Const.DEBUG_MANAGERS) {
			FragmentManager.enableDebugLogging(true);
			LoaderManager.enableDebugLogging(true);
		}
		
		try {
            Const.ticketGroupCount = getResources().getInteger(R.integer.ticketGroupCount);
        } catch (Exception ignored) {}

        timerCorr = getResources().getInteger(R.integer.timerCorr);

		Credentials.getInstance(getApplicationContext());
		
        mHandlerThread = new MyHandlerThread("IncomingHandler");
        mHandlerThread.start();

        // Get the HandlerThread's Looper and use it for our Handler
		tracStartHandler = new IncomingHandler(mHandlerThread.getLooper());
        mMessenger = new Messenger(tracStartHandler);

        startService(new Intent(this, RefreshService.class));
		
        setContentView(R.layout.tracstart);
        if (findViewById(R.id.left_drawer) != null) {
            setupDrawer();
        }
		
        debug |= Credentials.isRCVersion();

        url = Credentials.getUrl();
        username = Credentials.getUsername();
        password = Credentials.getPassword();
        sslHack = Credentials.getSslHack();
        sslHostNameHack = Credentials.getSslHostNameHack();
        profile = Credentials.getProfile();
		setFilter(Credentials.getFilterString());
		setSort(Credentials.getSortString());

        dispAds = true;
		if (savedInstanceState != null) {
            dispAds = savedInstanceState.getBoolean(Const.ADMOB, true);
            url = savedInstanceState.getString(Const.CURRENT_URL);
            username = savedInstanceState.getString(Const.CURRENT_USERNAME);
            password = savedInstanceState.getString(Const.CURRENT_PASSWORD);
            sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK, false);
            sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK, false);
			filterList = (ArrayList<FilterSpec>)savedInstanceState.getSerializable(Const.FILTERLISTNAME);
			sortList = (ArrayList<SortSpec>)savedInstanceState.getSerializable(Const.SORTLISTNAME);
        } else {
			// only at first start
			if (Credentials.getFirstRun()) {
				final Intent launchTrac = new Intent(this, TracShowWebPage.class);
				launchTrac.putExtra(Const.HELP_FILE, getString(R.string.whatsnewhelpfile));
				launchTrac.putExtra(Const.HELP_VERSION, false);
				startActivity(launchTrac);
			}
			if (getIntent().hasExtra(Const.ADMOB)) {
				dispAds = getIntent().getBooleanExtra(Const.ADMOB, true);
			}
			if (getIntent().hasExtra(Const.INTENT_URL)) {
				urlArg = getIntent().getStringExtra(Const.INTENT_URL);
				ticketArg = (int) getIntent().getLongExtra(Const.INTENT_TICKET, -1);
			}
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
					showAlertBox(R.string.wrongdb,R.string.wrongdbtext1,url + getString(R.string.wrongdbtext2) + urlArg + getString(R.string.wrongdbtext3));
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
		
		newDataAdapter(new Tickets()); // empty list
		
		
		IntentFilter intFilt = new IntentFilter(PROVIDER_MESSAGE);
		intFilt.addAction(DATACHANGED_MESSAGE);
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastMessageReceiver,intFilt);
		
        getFragmentManager().addOnBackStackChangedListener(this);
        // Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        if (savedInstanceState != null) {
            restoreFragment(savedInstanceState, ListFragmentTag);
            restoreFragment(savedInstanceState, LoginFragmentTag);
            restoreFragment(savedInstanceState, DetailFragmentTag);
            restoreFragment(savedInstanceState, NewFragmentTag);
            restoreFragment(savedInstanceState, UpdFragmentTag);
            restoreFragment(savedInstanceState, FilterFragmentTag);
            restoreFragment(savedInstanceState, SortFragmentTag);
            tcLog.d("backstack restored");
			startListLoader();
        } else {
            final FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (url != null && url.length() > 0) {
				startListLoader();

                final TicketListFragment ticketListFragment = new TicketListFragment();

                if (urlArg != null) {
					tcLog.d( "select Ticket = " + ticketArg);
					final Bundle args = makeArgs();
                    args.putInt("TicketArg", ticketArg);
                    urlArg = null;
                    ticketArg = -1;
					ticketListFragment.setArguments(args);
                }
                ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
				ft.addToBackStack(ListFragmentTag);
            } else {
                final TracLoginFragment tracLoginFragment = newLoginFrag();

                ft.add(R.id.displayList, tracLoginFragment, LoginFragmentTag);
				ft.addToBackStack(LoginFragmentTag);
            }
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            tcLog.d("backstack initiated");
        }

        bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        setReferenceTime();
    }
	
	@Override
	public void onPostCreate (Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
        tcLog.logCall();
		mDrawerToggle.syncState();
	}
	
	private void startListLoader() {
        tcLog.logCall();
		tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_START_LISTLOADER,null));
	}

	public void showAlertBox(final int titleres, final int message, final String addit) {
        tcLog.d("titleres = "+titleres +" : "+getString(titleres));
		if (!isFinishing()) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String s = (message != 0 ? getResources().getString(message)+(addit != null ? ": "+addit:"") : addit);
					final AlertDialog ad = new AlertDialog.Builder(TracStart.this)
						.setTitle(titleres)
						.setMessage(s)
//						.setCancelable(false)
						.setPositiveButton(R.string.oktext, null)
						.create();
					tracStartHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							tcLog.d( "dismiss");
							ad.dismiss();
						}
					},7500);
					ad.show();	
				}
			});
		}
	}

	@Override 
	public int getTicketCount() {
		return dataAdapter.getCount();
	}

	@Override 
	public int getTicketContentCount() {
		try {
			return dataAdapter.getTicketContentCount();
		} catch (Exception e) {
			return 0;
		}
	}

    private void restoreFragment(Bundle savedInstanceState, final String tag) {
        if (savedInstanceState.containsKey(tag)) {
            try {
                getFragmentManager().getFragment(savedInstanceState, tag);
            } catch (final Exception ignored) {}
        }
    }

    private void saveFragment(Bundle savedInstanceState, final String tag) {
        try {
            final Fragment f = getFragment(tag);

            if (f != null) {
                getFragmentManager().putFragment(savedInstanceState, tag, f);
            }
        } catch (final Exception ignored) {// Exception if fragment not on stack can be ignored
        }
    }

    public void shouldDisplayHomeUp() {
        // Enable Up button only if there are entries in the back stack
        final boolean canBack = getFragmentManager().getBackStackEntryCount() > 1;

        tcLog.d("canBack = " + canBack);
        final ActionBar ab = getActionBar();

        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(canBack);
        }
    }

    @Override
    public boolean onNavigateUp() {
        tcLog.d("entry count = " + getFragmentManager().getBackStackEntryCount());
        // This method is called when the up button is pressed. Just the pop back stack.
        getFragmentManager().popBackStack();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        tcLog.logCall();
		
		stopProgressBar();

        /* save logfile when exiting */
        tcLog.d("isFinishing = " + isFinishing());
        if (isFinishing() && Credentials.isRCVersion()) {
			tcLog.save();
        }
    }

	@Override
	public void listViewCreated() {
		tcLog.d("ticketsLoading = "+ticketsLoading + " hasTicketsLoadingBar = "+hasTicketsLoadingBar);
		synchronized (this) {
			if (ticketsLoading) {
				if (!hasTicketsLoadingBar) {
					startProgressBar(getString(R.string.getlist) + (profile == null ? "" : "\n" + profile));
					hasTicketsLoadingBar = true;
				}
			}
		}
	}

    @Override
    public void onChooserSelected(onFileSelectedListener oc) {
        tcLog.logCall();
        // save callback
        _oc = oc;
        // Use the GET_CONTENT intent from the utility class
        final Intent target = new Intent(Intent.ACTION_GET_CONTENT);

        target.setType("*/*");
        target.addCategory(Intent.CATEGORY_OPENABLE);
        // Create the chooser Intent
		startActivityForResult(Intent.createChooser(target, getString(R.string.chooser_title)), REQUEST_CODE);
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        tcLog.d("requestcode = " + requestCode);
        switch (requestCode) {
        case REQUEST_CODE:
            // If the file selection was successful
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // Get the URI of the selected file
                    final Uri uri = data.getData();

                    try {
                        // Create a file instance from the URI
                        final File file = new File(uri.getPath());

                        tcLog.d( "File Selected: " + file.getAbsolutePath());
                        if (_oc != null) {
                            _oc.onFileSelected(file.getAbsolutePath());
                        }
                    } catch (final Exception e) {
                        tcLog.d("File select error", e);
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        tcLog.d(frag+" this = "+this);
        if (ListFragmentTag.equals(frag.getTag())) {
			final TicketListFragment ticketListFragment = getTicketListFragment();
			if (ticketListFragment != null) {
				tcLog.d("ticketListFragment = " + ticketListFragment);

				if (urlArg != null) {
					tcLog.d("Ticket = " + ticketArg);
					ticketListFragment.selectTicket(ticketArg);
					urlArg = null;
					ticketArg = -1;
				}
			}
        }
    }

    @Override
    public void onBackPressed() {
        tcLog.logCall();
        final DetailFragment df = (DetailFragment) getFragment(DetailFragmentTag);
        final boolean callSuper = df == null || !df.onBackPressed();

        if (callSuper) {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        final int depth = getFragmentManager().getBackStackEntryCount();

        // tcLog.d("onBackStackChanged depth = " + depth);
        if (depth == 0 && !doNotFinish) {
            finish();
        }
        doNotFinish = false;
        shouldDisplayHomeUp();
    }
	
	TracLoginFragment newLoginFrag() {
        final TracLoginFragment tracLoginFragment = new TracLoginFragment();
		final Bundle args = makeArgs();
		args.putString(Const.CURRENT_URL, url);
		args.putString(Const.CURRENT_USERNAME, username);
		args.putString(Const.CURRENT_PASSWORD, password);
		args.putBoolean(Const.CURRENT_SSLHACK, sslHack);
		args.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
		tracLoginFragment.setArguments(args);
		return tracLoginFragment;
	}

    private void onChangeHost() {
        tcLog.logCall();
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final TracLoginFragment tracLoginFragment = newLoginFrag();

        ft.replace(R.id.displayList, tracLoginFragment, LoginFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(LoginFragmentTag);
        ft.commit();
    }

	public void setActionProvider(Menu menu,int resid) {
        final MenuItem item = menu.findItem(resid);
        ShareActionProvider mShareActionProvider = (ShareActionProvider)item.getActionProvider();
        if (mShareActionProvider == null) {
            tcLog.d( "create new shareActionProvider item = "+item);
            mShareActionProvider = new ShareActionProvider(this);
            item.setActionProvider(mShareActionProvider);
       }
		mShareActionProvider.setShareHistoryFileName("custom_share_history"+resid+".xml");
		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        tcLog.logCall();
        getMenuInflater().inflate(R.menu.tracstartmenu, menu);
		setActionProvider(menu,R.id.debug);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        tcLog.logCall();
        if (mIsBound) {
            sendMessageToService(MSG_STOP_TIMER);
            mHandlerThread.quit();
            unbindService(mConnection);
            mIsBound = false;
        }
        // stopService(new Intent(this, RefreshService.class));
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(mProviderMessageReceiver);
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(mProviderMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastMessageReceiver);
        super.onDestroy();
    }

    private void onFilterSelected(ArrayList<FilterSpec> filterList) {
        tcLog.d("filterList = "+ filterList);
		
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final FilterFragment filterFragment = new FilterFragment();

		final Bundle args = makeArgs();
		args.putSerializable(Const.FILTERLISTNAME, filterList);
		filterFragment.setArguments(args);
		
        ft.replace(R.id.displayList, filterFragment, FilterFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(FilterFragmentTag);
        ft.commit();
    }
	
    @Override
    public void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
        tcLog.d(newProfile);
        url = newUrl;
        username = newUser;
        password = newPass;
        sslHack = newHack;
        sslHostNameHack = newHostNameHack;
        profile = newProfile;
        TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);
        final FragmentTransaction ft = getFragmentManager().beginTransaction();

        getFragmentManager().popBackStack();
        if (ticketListFragment == null) {
            ticketListFragment = new TicketListFragment();
            doNotFinish = true;
            ft.replace(R.id.displayList, ticketListFragment, ListFragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(ListFragmentTag);
        }
        ft.commit();
        refreshOverview();
    }

    private void onNewTicket() {
        tcLog.logCall();

        final NewTicketFragment newtickFragment = new NewTicketFragment();
        // tcLog.d( "newTickFragment =" +  newtickFragment.toString());
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
		final Bundle args = makeArgs();
		args.putString(Const.CURRENT_USERNAME, username);
		newtickFragment.setArguments(args);

        ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(NewFragmentTag);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d( "item=" + item.getTitle());
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			tcLog.d( "hanlded by drawer: item=" + item.getTitle());
			return true;
		}
        switch(item.getItemId()) {
			case R.id.over:
			showAbout();
			break;
			
			case R.id.tlrefresh:
            refreshOverview();
			break;
			
			case R.id.tlnieuw:
            onNewTicket();
			break;
			
			case R.id.tlfilter:
            onFilterSelected(filterList);
			break;
			
			case R.id.tlsort:
            onSortSelected(sortList);
			break;
			
			case R.id.tlchangehost:
            onChangeHost();
			break;
			
			default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        tcLog.logCall();
        // If the nav drawer is open, hide action items related to the content view
		
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		for (int mDrawerId : mDrawerIds) {
			try {
				menu.findItem(mDrawerId).setVisible(!drawerOpen);
			} catch(Exception ignored) {}
		}
		Intent i = null;
	
        final MenuItem itemDebug = menu.findItem(R.id.debug);

        itemDebug.setVisible(debug).setEnabled(debug);
        if (debug) {
           i = shareDebug();

			ShareActionProvider debugShare = (ShareActionProvider)itemDebug.getActionProvider();
            tcLog.d( "item = " + itemDebug + " " + debugShare + " " + i);
			if (debugShare != null && i != null) {
				debugShare.setShareIntent(i);
			}
        }
		
       return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("Admob", dispAds);
        savedInstanceState.putSerializable(Const.SORTLISTNAME, sortList);
        savedInstanceState.putSerializable(Const.FILTERLISTNAME, filterList);
        savedInstanceState.putString(Const.CURRENT_URL, url);
        savedInstanceState.putString(Const.CURRENT_USERNAME, username);
        savedInstanceState.putString(Const.CURRENT_PASSWORD, password);
        savedInstanceState.putBoolean(Const.CURRENT_SSLHACK, sslHack);
        savedInstanceState.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        saveFragment(savedInstanceState, ListFragmentTag);
        saveFragment(savedInstanceState, LoginFragmentTag);
        saveFragment(savedInstanceState, DetailFragmentTag);
        saveFragment(savedInstanceState, NewFragmentTag);
        saveFragment(savedInstanceState, UpdFragmentTag);
        saveFragment(savedInstanceState, FilterFragmentTag);
        saveFragment(savedInstanceState, SortFragmentTag);
        tcLog.d( "savedInstanceState = " + savedInstanceState);
    }

    private void onSortSelected(ArrayList<SortSpec> sortList) {
        tcLog.logCall();
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final SortFragment sortFragment = new SortFragment();

		final Bundle args = makeArgs();
		args.putSerializable(Const.SORTLISTNAME, sortList);
		sortFragment.setArguments(args);

        ft.replace(R.id.displayList, sortFragment, SortFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(SortFragmentTag);
        ft.commit();
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
		boolean isTop = (DetailFragmentTag.equals(getTopFragment()));
        tcLog.d( "Ticket: " + ticket+" isTop = "+ isTop);
		
		DetailFragment detailFragment = new DetailFragment();
        final Bundle args = makeArgs();
        args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
        detailFragment.setArguments(args);
	
        // tcLog.d( "detailFragment =" +
        // detailFragment.toString());
        final FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.displayList, detailFragment, DetailFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if (!isTop) {
			ft.addToBackStack(DetailFragmentTag);
		}
		
        ft.commit();
    }

    @Override
    public void onUpdateTicket(Ticket ticket) {
        tcLog.d( "ticket = " + ticket);

        final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
        final Bundle args = makeArgs();

        args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
        updtickFragment.setArguments(args);
        // tcLog.d( "updtickFragment = " + updtickFragment.toString());
        final FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.replace(R.id.displayList, updtickFragment, UpdFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(UpdFragmentTag);
        ft.commit();
    }
	
	public Handler getHandler() {
		return tracStartHandler;
	}
	
	public TicketModel getTicketModel() {
        tcLog.logCall();
		if (tm == null) {
			startProgressBar(R.string.downloading);
			tm = TicketModel.getInstance();
			stopProgressBar();
		}
		return tm;
	}

    private Bundle makeArgs() {
        return new Bundle();
    }
	
    private void setFilter(ArrayList<FilterSpec> filter) {
        tcLog.d(filter.toString());
        String filterString = "";

        if (filter != null) {
			filterString = Credentials.joinList(filter.toArray(),"&");
        }
        Credentials.storeFilterString(filterString);
        filterList = filter;
    }

    private void setFilter(String filterString) {
        tcLog.d(filterString);
        final ArrayList<FilterSpec> filter = new ArrayList<>();

        if (filterString.length() > 0) {
            String[] fs;

            try {
                fs = filterString.split("&");
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

    private void setSort(ArrayList<SortSpec> sort) {
        tcLog.d(sort.toString());

        String sortString = "";

        if (sort != null) {
			sortString = Credentials.joinList(sort.toArray(),"&");
        }
        Credentials.storeSortString(sortString);
        sortList = sort;
    }

    private void setSort(String sortString) {
        tcLog.d(sortString);
        final ArrayList<SortSpec> sl = new ArrayList<>();

        if (sortString.length() > 0) {
            String[] sort;

            try {
                sort = sortString.split("&");
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

                        if ("desc=1".equalsIgnoreCase(s1)) {
                            richting = false;
                            i++;
                        }
                    }
                    sl.add(new SortSpec(veld, richting));
                }
            }
        }
        setSort(sl);
    }

    @Override
    public boolean getDispAds() {
		tcLog.d("dispAds = "+dispAds);
        return dispAds;
    }

    @Override
    public void refreshOverview() {
        tcLog.logCall();
//				dataAdapter.notifyDataSetChanged();
		startListLoader();
		setReferenceTime();
	}

    @Override
    public void setDispAds(boolean b) {
        dispAds = b;
    }

    public Intent shareList() {
        tcLog.logCall();
        String lijst = "";
		
		if (dataAdapter != null ) {
			for (Ticket t: dataAdapter.getTicketList()){
				try {
					lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
				} catch (final Exception e) {
					tcLog.e( "exception", e);
				}
			}
			final Intent sendIntent = new Intent(Intent.ACTION_SEND);

			sendIntent.putExtra(Intent.EXTRA_TEXT, lijst);
			sendIntent.setType("text/plain");
			return sendIntent;
		} else {
			return null;
		}

    }
	
	public Intent shareTicket(final Ticket ticket) {
        if (ticket != null && ticket.hasdata()) {
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);

            sendIntent.putExtra(Intent.EXTRA_TEXT, ticket.toText());
            sendIntent.setType("text/plain");
            return sendIntent;
        }
		return null;
    }

    @Override
    public void enableDebug() {
        // tcLog.d( "enableDebug");
        debug = true;
        invalidateOptionsMenu();
        tcLog.toast("Debug enabled");
    }

    private Intent shareDebug() {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, tcLog.getDebug());
        sendIntent.setType("text/plain");
        return sendIntent;
    }
	
	private TicketListFragment getTicketListFragment() {
		return (TicketListFragment)getFragment(ListFragmentTag);
	}

    private Fragment getFragment(final String tag) {
        return getFragmentManager().findFragmentByTag(tag);
    }

    public void getNewTickets(final String isoTijd) {
        tcLog.d( "tijd = "+isoTijd) ;
		
		Bundle args = new Bundle();
		args.putString(BUNDLE_ISOTIJD,isoTijd);
		if (changesLoaderStarted) {
			getLoaderManager().restartLoader(CHANGES_LOADER,args,this);
		} else {
			getLoaderManager().initLoader(CHANGES_LOADER,args,this);
			changesLoaderStarted = true;
		}
    }

    @Override
    public void setReferenceTime() {
        // tcLog.d( "setReferenceTime");
        referenceTime = System.currentTimeMillis() - timerCorr;
        sendMessageToService(MSG_REMOVE_NOTIFICATION);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        try {
			ret |= ((DetailFragment) getFragment(DetailFragmentTag)).dispatchTouchEvent(ev);
		} catch (Exception ignored) {}
        return ret;
    }
	
    public void startProgressBar(String message) {
        tcLog.d( message+" tracStartHandler = "+tracStartHandler);
		try {
			tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_START_PROGRESSBAR,message));
		} catch (NullPointerException e) {
			tcLog.e("NullPointerException",e);
		}
    }

    public void startProgressBar(int resid) {
        startProgressBar(getString(resid));
    }

 	public void stopProgressBar() {
        tcLog.logCall();
		try {
			tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_STOP_PROGRESSBAR));
		} catch (NullPointerException e) {
			tcLog.e("NullPointerException");
		}
	}
	
    private void newDataAdapter(Tickets tl) {
        tcLog.logCall();
        dataAdapter = new TicketListAdapter(this, R.layout.ticket_list, tl);
		try {
			getTicketListFragment().setAdapter(dataAdapter);
		} catch (NullPointerException e) {
			tcLog.e("NullPointerException");
		}
	}
	
	@Override
	public TicketListAdapter getAdapter() {
        tcLog.logCall();
		return dataAdapter;
	}
	
	@Override
	public Ticket getTicket(int i) {
        tcLog.d( "i = "+i+ " semaphore = "+ loadingActive.availablePermits());
		loadingActive.acquireUninterruptibly();
		loadingActive.release();

		Ticket t = dataAdapter.getTicket(i);
        tcLog.d( "i = "+i+ " ticket = "+ t);
		if (t != null && t.hasdata()) {
			return t;
		}
		
		return refreshTicket(i);
	}
	
	@Override
	public Ticket refreshTicket(final int i) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Bundle args = new Bundle();
				args.putInt(BUNDLE_TICKET,i);
				if (ticketLoaderStarted) {
					getLoaderManager().restartLoader(TICKET_LOADER_NOSHOW,args,TracStart.this);
				} else {
					getLoaderManager().initLoader(TICKET_LOADER_NOSHOW,args,TracStart.this);
					ticketLoaderStarted = true;
				}
			}
		});
		return null;  // TODO
	}
	
	@Override 
	public void putTicket(Ticket t) {
		//TODO
	}
	
	public boolean updateTicket(final Ticket t,final String action, final String comment, final String veld, final String waarde, final boolean notify, final Map<String, String> modVeld) throws Exception{
		final JSONObject velden = t.getVelden();
		tcLog.d("Ticket = "+ t);
		tcLog.d("update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "' " + modVeld);
        tcLog.d("velden voor = " + velden);
		final int ticknr = t.getTicketnr();
		
        if (ticknr == -1) {
            throw new IllegalArgumentException(getString(R.string.invtick) + " " + ticknr);
        }
        if (action == null) {
            throw new NullPointerException(getString(R.string.noaction));
        }
        velden.put("action", action);
        if (waarde != null && veld != null && !"".equals(veld) && !"".equals(waarde)) {
            velden.put(veld, waarde);
        }
        if (modVeld != null) {
			for (Entry<String,String> e: modVeld.entrySet()) {
                // tcLog.d( e.toString());
                velden.put(e.getKey(), e.getValue());
            }
        }

        final String cmt = comment == null ? "" : comment;

        velden.remove("changetime");
        velden.remove("time");

		Thread updateThread = new Thread() {
			@Override
			public void run() {
				try {
					TracHttpClient tracClient = new TracHttpClient(url,sslHack,sslHostNameHack,username,password);
					JSONArray retTick = tracClient.updateTicket(ticknr, cmt, velden, notify); // TODO zou nieuwe ticket retourneren en is volgende dus niet nodig
					t.setFields(retTick.getJSONObject(3));
					if (modVeld != null) {
						modVeld.clear();
					}
					tcLog.d("retTicket = "+retTick);
				} catch (final Exception e) {
					tcLog.e("JSONRPCException", e);
					showAlertBox(R.string.upderr,R.string.storerrdesc,e.getMessage());
				}
			}
		};
		updateThread.start();
		updateThread.join();
		return true;
	}
	
	public int createTicket(Ticket t,boolean notify) throws Exception{
		int ticknr = t.getTicketnr();
		final JSONObject velden = t.getVelden();
		
        if (ticknr != -1) {
            throw new IllegalArgumentException("Call create ticket not -1");
        }
        tcLog.i( "create: " + velden.toString());
        final String s = velden.getString("summary");
        final String d = velden.getString("description");

        velden.remove("summary");
        velden.remove("description");

		try {
			TracHttpClient tracClient = new TracHttpClient(url,sslHack,sslHostNameHack,username,password);
			final int newticknr = tracClient.createTicket(s, d, velden);
			if (newticknr != -1) {
//				reloadTicketData(new Ticket(newticknr));
				refreshTicket(newticknr);
				return newticknr;
			} else {
				showAlertBox(R.string.storerr,R.string.noticketUnk,"");
				return -1;
			}
		} catch (final Exception e) {
			tcLog.d("Exception during create",e);
			showAlertBox(R.string.storerr,R.string.storerrdesc,e.getMessage());
			return -1;
		}
	}

	@Override 
	public int getNextTicket(int i) {
		return dataAdapter.getNextTicket(i);
	}

	@Override 
	public int getPrevTicket(int i) {
		return dataAdapter.getPrevTicket(i);
	}
	
    private BroadcastReceiver mBroadcastMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c,Intent i) {
//			tcLog.d( "Receive "+i);
			switch (i.getAction()) {
				case PROVIDER_MESSAGE:
//				tcLog.d( "Receive PROVIDER_MESSAGE");
				int title = i.getIntExtra("title",R.string.warning);
				int message = i.getIntExtra("message",R.string.unknownError);
				String addit = i.getStringExtra("additional");
				showAlertBox(title,message,addit);
				break;

				case DATACHANGED_MESSAGE:
//				tcLog.d( "Receive DATACHANGED_MESSAGE");
				findViewById(R.id.displayList).invalidate();
				try {
					getTicketListFragment().dataHasChanged();
				} catch (Exception e) {
					tcLog.e("cannot contact TicketListFragment");
				}	
				break;

				default:
				throw new IllegalArgumentException();
			}
		}
    };
}
