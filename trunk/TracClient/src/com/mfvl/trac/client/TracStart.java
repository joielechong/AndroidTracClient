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

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.app.ActionBar;
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
	void putTicket(Ticket t);
	int getNextTicket(int i);
	int getPrevTicket(int i);
	int getTicketCount();
	int getTicketContentCount();
	void updateTicket(Ticket t,String action, String comment, String veld, String waarde, final boolean notify, Map<String, String> modVeld) throws Exception;
	int createTicket(Ticket t , boolean notify) throws Exception;
	void setActionProvider(Menu menu,int resid);
	Intent shareList();
	void listViewCreated();
	String getUsername();
	String getPassword();
	String getUrl();
	boolean getSslHack();
	boolean getSslHostNameHack();
	boolean isFinishing();
	Handler getHandler();
}

public class TracStart extends Activity implements LoaderManager.LoaderCallbacks<Cursor>, InterFragmentListener, OnBackStackChangedListener {
    // onActivityResult requestcode for filechooser

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
	
	public static final String PROVIDER_MESSAGE = "TracClientProviderMessage";
	public static final String DATACHANGED_MESSAGE = "TracClientDataChangedMessage";
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

    private static final String SORTLISTNAME = "sortlist";
    private static final String FILTERLISTNAME = "filterlist";
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
	private TicketObserver myObserver;

    boolean mIsBound = false;
    Messenger mService = null;
    private MyHandlerThread mHandlerThread = null;
    private Messenger mMessenger = null;
	
	private boolean changesLoaderStarted = false;
	private boolean ticketLoaderStarted = false;
	private boolean loaderStarted = false;
	private boolean hasTicketsLoadingBar = false;
	private Boolean ticketsLoading = false;
		
	private TicketListAdapter dataAdapter = null;
	
    private String joinList(Object list[], final String sep) {
        String reqString = "";

        for (final Object fs : list) {
            if (fs != null) {
                reqString += ((reqString.length() > 0) ? sep : "")+fs.toString();
            }
        }
        return reqString;
    }

    private static final String[] fields = new String[] { TicketCursor.STR_FIELD_ID, TicketCursor.STR_FIELD_TICKET};
	
    private static final int LIST_LOADER = 1;
	private static final int CHANGES_LOADER = 2;
	private static final int TICKET_LOADER = 3;
	
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {
        tcLog.d(getClass().getName(), "onCreateLoader " + loaderID + " " + bundle);

        /*
         * Takes action based on the ID of the Loader that's being created
         */
        switch (loaderID) {
			case LIST_LOADER:
			hasTicketsLoadingBar = false;
            // Returns a new CursorLoader
			try {
				getTicketListFragment().startLoading();
			} catch (Exception e) {
				tcLog.e(getClass().getName(),"onCreateLoader LIST_LOADER cannot contact TicketListFragment");
			}
			synchronized(this) {
				if (ListFragmentTag.equals(getTopFragment())) {
					startProgressBar(getString(R.string.getlist) + (profile == null ? "" : "\n" + profile));
					hasTicketsLoadingBar = true;
				}
			}
			ticketsLoading = true;
            return new CursorLoader(this, TicketProvider.LIST_QUERY_URI, fields, joinList(filterList.toArray(), "&"), null, joinList(sortList.toArray(), "&"));
			
			case CHANGES_LOADER:
			String isoTijd = bundle.getString(BUNDLE_ISOTIJD);
			return new CursorLoader(this,Uri.withAppendedPath(TicketProvider.QUERY_CHANGES_URI,isoTijd),fields,null,null,null);
			
			case TICKET_LOADER:
			int ticknr = bundle.getInt(BUNDLE_TICKET);
			startProgressBar(getString(R.string.downloading)+" "+ticknr);
			return new CursorLoader(this,Uri.withAppendedPath(TicketProvider.GET_QUERY_URI,""+ticknr),fields,null,null,null);
			
				
        default:
            // An invalid id was passed in
            return null;
        }
    }
	
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
		TicketCursor cursor;
        tcLog.d(getClass().getName(), "onLoadFinished " + loader + " " + loader.getId() + " " + c);
		switch (loader.getId()) {
			case LIST_LOADER:
			synchronized(this) {
				if (hasTicketsLoadingBar) {
					stopProgressBar();
					hasTicketsLoadingBar = false;
				}
				ticketsLoading = false;
			}
			dataAdapter.swapCursor(c);
			try {
				getTicketListFragment().dataHasChanged();
			} catch (Exception e) {
				tcLog.e(getClass().getName(),"onLoadFinished LIST_LOADER cannot contact TicketListFragment");
			}
			break;
			
			case CHANGES_LOADER:
			List<Integer> newTickets = new ArrayList<Integer>();
			for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				newTickets.add(c.getInt(0));
			}
			sendMessageToService(MSG_SEND_NEW_TICKETS, newTickets);
			break;
			
			case TICKET_LOADER:
			stopProgressBar();
			if (c != null && c.moveToFirst()) {
				tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_DISPLAY_TICKET,getTicket(c.getInt(0))));
			} else {
				showAlertBox(R.string.notfound,R.string.ticketnotfound,null);
				getLoaderManager().destroyLoader(TICKET_LOADER);
			}
			break;
		}
    }		
	
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        tcLog.d(getClass().getName(), "onLoaderReset " + loader + " " + loader.getId());
		switch (loader.getId()) {
			case LIST_LOADER:
			dataAdapter.swapCursor(null);
			hasTicketsLoadingBar = false;
			ticketsLoading = false;
			break;
			
			case TICKET_LOADER:
			ticketLoaderStarted = false;
			break;
		}
    }
	
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //tcLog.d(getClass().getName(),"onServiceConnected className = " + className + " service = " + service);
            mService = new Messenger(service);
            sendMessageToService(MSG_START_TIMER);
            //tcLog.d(getClass().getName(),"mService = " + mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            //tcLog.d(getClass().getName(),"onServiceDisconnected className = " + className);
            mService = null;
        }
    };

    class IncomingHandler extends Handler {
		private ProgressDialog progressBar = null;

        public IncomingHandler(Looper looper) {
            super(looper);
            //tcLog.d(getClass().getName(), "ServiceHandler");
        }

        @Override
		@SuppressWarnings("unchecked")
        public void handleMessage(Message msg) {
            //tcLog.d(getClass().getName(), "handleMessage msg = " + msg);
            switch (msg.what) {
				case MSG_REQUEST_TICKET_COUNT:
                final int count = getTicketCount();

                sendMessageToService(MSG_SEND_TICKET_COUNT, count);
                break;

				case MSG_REQUEST_NEW_TICKETS:
				getNewTickets(ISO8601.fromUnix(referenceTime));
                break;

				case MSG_REQUEST_REFRESH:
                // tcLog.d(getClass().getName(),"handleMessage msg = REFRESH");
                refreshOverview();
                break;
				
				case MSG_START_PROGRESSBAR:
				final String message = (String) msg.obj;
                post(new Runnable() {
                    @Override
                    public void run() {
						synchronized (this) {
							// tcLog.d(getClass().getName(),"handleMessage msg = START_PROGRESSBAR string = "+message);
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
					}
                });
				break;

				case MSG_STOP_PROGRESSBAR:
                post(new Runnable() {
                    @Override
                    public void run() {
						synchronized (this) {
						// tcLog.d(getClass().getName(),"handleMessage msg = STOP_PROGRESSBAR");
							if (progressBar != null) {
								if (!TracStart.this.isFinishing()) {
									progressBar.dismiss();
								}
								progressBar = null;
							}
						}
					}
				});
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
				onTicketSelected((Ticket)msg.obj);
				break;

				default:
                super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToService(int message) {
        //tcLog.d(getClass().getName(), "sendMessageToService message = "+ message+" mService = "+mService);
        if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
                msg.replyTo = mMessenger;
                tcLog.d(getClass().getName(), "sendMessageToService msg = " + msg);
                mService.send(msg);
            } catch (final RemoteException e) {
                tcLog.e(getClass().getName(), "sendMessageToService failed", e);
            }
        }
    }

    private void sendMessageToService(int message, int value) {
        // tcLog.d(getClass().getName(), "sendMessageToService message = "+ message);
        if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
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

    private void sendMessageToService(int message, Object value) {
        if (mIsBound && mService != null) {
            try {
                final Message msg = Message.obtain();

                msg.what = message;
                msg.replyTo = mMessenger;
                msg.obj = value;
                mService.send(msg);
            } catch (final RemoteException e) {
                tcLog.e(getClass().getName(), "sendMessageToService Object failed", e);
            }
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @SuppressWarnings("rawtypes")
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
            tcLog.d(getClass().getName(),"onItemClick: parent = " + parent + " view = " + view + " position = " + position + " id = " + id + " mDrawerId = " + mDrawerIds[position]);
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
			
			case R.id.help: 
			try {
				String fragmentTag = getTopFragment();
				TracClientFragment currentFragment = (TracClientFragment)fm.findFragmentByTag(fragmentTag);	
				currentFragment.showHelp();
			} catch (Exception e) {}
			break;
			
			default:
            tcLog.toast(getClass().getName() + " selectItem: position = " + position + " mDrawerId = " + mDrawerIds[position]);
		}
//		mDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(mDrawerList);
	}
	
	private String getTopFragment() {
		try {
			int bs = fm.getBackStackEntryCount();
			String fragmentTag = fm.getBackStackEntryAt(bs - 1).getName();
			return fragmentTag;
		} catch (Exception e) {
			return null;
		}
	}

	private void showAbout() {
		tcLog.d(getClass().getName(), "showAbout");
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
			
			ArrayList<String> s = new ArrayList<String>();
			ArrayList<Integer> id = new ArrayList<Integer>();
			
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
				tcLog.d(getClass().getName(), "onDrawerClosed view = " + view);
                super.onDrawerClosed(view);
//                ab.setTitle(mTitle);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
				tcLog.d(getClass().getName(), "onDrawerOpened drawerView = " + drawerView);
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
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());		
    }
	
	private void setContentObserver() {
		MyHandlerThread mHandlerThread = new MyHandlerThread("ObserverHandler");
        mHandlerThread.start();
		myObserver = new TicketObserver(new Handler(mHandlerThread.getLooper()));
		getContentResolver().registerContentObserver(TicketProvider.GET_QUERY_URI,true,myObserver);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.d(getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);

        try {
            Const.ticketGroupCount = getResources().getInteger(R.integer.ticketGroupCount);
        } catch (Exception e) {
            tcLog.e(getClass().getName(), "Resource ticketGroupCount not found", e);
        }

        timerCorr = getResources().getInteger(R.integer.timerCorr);

        // FragmentManager.enableDebugLogging(true);
        MyTracker.getInstance(this);
		
        mHandlerThread = new MyHandlerThread("IncomingHandler", Process.THREAD_PRIORITY_BACKGROUND);
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

        dispAds = true;
		if (savedInstanceState != null) {
            dispAds = savedInstanceState.getBoolean(Const.ADMOB, true);
        } else if (getIntent().hasExtra(Const.ADMOB)) {
            dispAds = getIntent().getBooleanExtra(Const.ADMOB, true);
		}
		

        url = Credentials.getUrl();
        username = Credentials.getUsername();
        password = Credentials.getPassword();
        sslHack = Credentials.getSslHack();
        sslHostNameHack = Credentials.getSslHostNameHack();
        profile = Credentials.getProfile();
		setFilter(Credentials.getFilterString());
		setSort(Credentials.getSortString());

        if (savedInstanceState != null) {
            url = savedInstanceState.getString(Const.CURRENT_URL);
            username = savedInstanceState.getString(Const.CURRENT_USERNAME);
            password = savedInstanceState.getString(Const.CURRENT_PASSWORD);
            sslHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHACK, false);
            sslHostNameHack = savedInstanceState.getBoolean(Const.CURRENT_SSLHOSTNAMEHACK, false);
			filterList = (ArrayList<FilterSpec>)savedInstanceState.getSerializable(FILTERLISTNAME);
			sortList = (ArrayList<SortSpec>)savedInstanceState.getSerializable(FILTERLISTNAME);
        }
		
        if (Credentials.getFirstRun()) {
            final Intent launchTrac = new Intent(this, TracShowWebPage.class);
            final String filename = getString(R.string.whatsnewhelpfile);

            launchTrac.putExtra(Const.HELP_FILE, filename);
            launchTrac.putExtra(Const.HELP_VERSION, false);
            startActivity(launchTrac);
        }

        urlArg = getIntent().getStringExtra(Const.INTENT_URL);
        ticketArg = (int) getIntent().getLongExtra(Const.INTENT_TICKET, -1);

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
		
		newDataAdapter(null);
		
        setConfigProvider();
//		setContentObserver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mProviderMessageReceiver,new IntentFilter(PROVIDER_MESSAGE));
		LocalBroadcastManager.getInstance(this).registerReceiver(mDataChangedMessageReceiver,new IntentFilter(DATACHANGED_MESSAGE));
		
        getLoaderManager().initLoader(LIST_LOADER, null, this);

        fm = getFragmentManager();
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
            initializeList(getTicketListFragment());
        } else {
            final FragmentTransaction ft = fm.beginTransaction();

            if (url != null && url.length() > 0) {
                final TicketListFragment ticketListFragment = new TicketListFragment();
                final Bundle args = makeArgs();

                args.putString("currentProfile", profile);
//                args.putString("currentFilter", filterList);
//                args.putString("currectSortOrder", sortList);
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
				ft.addToBackStack(ListFragmentTag);
            } else {
                final TracLoginFragment tracLoginFragment = new TracLoginFragment();

                ft.add(R.id.displayList, tracLoginFragment, LoginFragmentTag);
				ft.addToBackStack(LoginFragmentTag);
            }
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
            tcLog.d(getClass().getName(), "onCreate: backstack initiated");
        }

        bindService(new Intent(this, RefreshService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        setReferenceTime();
    }

	public void showAlertBox(final int titleres, final int message, final String addit) {
        tcLog.d(getClass().getName(), "showAlertBox: titleres = "+titleres +" : "+getString(titleres));
		if (!isFinishing()) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String s = (message != 0 ? getResources().getString(message)+(addit != null ? ": "+addit:"") : addit);
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TracStart.this);
					alertDialogBuilder.setTitle(titleres)
						.setMessage(s)
						.setCancelable(false)
						.setPositiveButton(R.string.oktext, null)
						.show();
				}
			});
		}
	}

    private void setConfigProvider() {
        ContentValues cv = new ContentValues();

        cv.put(Const.CURRENT_URL, url);
        cv.put(Const.CURRENT_USERNAME, username);
        cv.put(Const.CURRENT_PASSWORD, password);
        cv.put(Const.CURRENT_SSLHACK, sslHack);
        cv.put(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);

		
        try {
            getContentResolver().insert(TicketProvider.CONFIG_QUERY_URI, cv);
            // getContentResolver().call(TicketProvider.AUTH_URI,TicketProvider.SET_CONFIG,null,cv);
        } catch (Exception e) {
            tcLog.e(getClass().getName(), "Error while configuring TicketProvider", e);
        }
    }
	
	@Override 
	public int getTicketCount() {
		return dataAdapter.getCount();
	}

	@Override 
	public int getTicketContentCount() {
		try {
			return ((TicketCursor)dataAdapter.getCursor()).getTicketList().getTicketContentCount();
		} catch (Exception e) {
			return 0;
		}
	}

    private void restoreFragment(Bundle savedInstanceState, final String tag) {
        if (savedInstanceState.containsKey(tag)) {
            try {
                fm.getFragment(savedInstanceState, tag);
            } catch (final Exception e) {}
        }
    }

    private void saveFragment(Bundle savedInstanceState, final String tag) {
        try {
            final Fragment f = getFragment(tag);

            if (f != null) {
                fm.putFragment(savedInstanceState, tag, f);
            }
        } catch (final Exception e) {// Exception if fragment not on stack can be ignored
        }
    }

    public void shouldDisplayHomeUp() {
        // Enable Up button only if there are entries in the back stack
        final boolean canBack = fm.getBackStackEntryCount() > 1;

        tcLog.d(getClass().getName(), "shouldDisplayHomeUp canBack = " + canBack);
        final ActionBar ab = getActionBar();

        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(canBack);
        }
    }

    @Override
    public boolean onNavigateUp() {
        tcLog.d(getClass().getName(), "onNavigateUp entry count = " + fm.getBackStackEntryCount());
        // This method is called when the up button is pressed. Just the pop back stack.
        fm.popBackStack();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        tcLog.d(getClass().getName(), "onPause ");
		
		stopProgressBar();

        /* save logfile when exiting */
        tcLog.d(getClass().getName(), "onPause  isFinishing = " + isFinishing());
        if (isFinishing() && Credentials.isRCVersion()) {
            File path = null;
            File file = null;

            try {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                tcLog.d(getClass().getName(), "path =  " + path);
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

    public void initializeList(final TicketListFragment ticketListFragment) {
//        setFilter(Credentials.getFilterString());
//        setSort(Credentials.getSortString());

        if (ticketListFragment != null) {
            tcLog.d(getClass().getName(), "initializeList ticketListFragment = " + ticketListFragment);

            if (urlArg != null) {
                tcLog.d(getClass().getName(), "select Ticket = " + ticketArg);
                ticketListFragment.selectTicket(ticketArg);
                urlArg = null;
                ticketArg = -1;
            }
        }
    }
	
	@Override
	public void listViewCreated() {
		tcLog.d(getClass().getName(), "listViewCreated: ticketsLoading = "+ticketsLoading + " hasTicketsLoadingBar = "+hasTicketsLoadingBar);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        tcLog.d(getClass().getName(), "onActivityResult: requestcode = " + requestCode);
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

                        tcLog.d(getClass().getName(), "File Selected: " + file.getAbsolutePath());
                        if (_oc != null) {
                            _oc.onFileSelected(file.getAbsolutePath());
                        }
                    } catch (final Exception e) {
                        tcLog.d(getClass().getName(), "on ActivityResult: File select error", e);
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        tcLog.d(getClass().getName(), "onAttachFragment " + frag+" this = "+this);
        if (ListFragmentTag.equals(frag.getTag())) {
            initializeList((TicketListFragment) frag);
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
    public void onBackStackChanged() {
        final int depth = fm.getBackStackEntryCount();

        // tcLog.d(getClass().getName(), "onBackStackChanged depth = " + depth);
        if (depth == 0 && !doNotFinish) {
            finish();
        }
        doNotFinish = false;
        shouldDisplayHomeUp();
    }

    private void onChangeHost() {
        tcLog.d(getClass().getName(), "onChangeHost");
        final FragmentTransaction ft = fm.beginTransaction();
        final TracLoginFragment tracLoginFragment = new TracLoginFragment();

        ft.replace(R.id.displayList, tracLoginFragment, LoginFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(LoginFragmentTag);
        ft.commit();
    }

    @Override
    public void onChooserSelected(onFileSelectedListener oc) {
        tcLog.d(getClass().getName(), "onChooserSelected");
        // save callback
        _oc = oc;
        // Use the GET_CONTENT intent from the utility class
        final Intent target = new Intent(Intent.ACTION_GET_CONTENT);

        target.setType("*/*");
        target.addCategory(Intent.CATEGORY_OPENABLE);
        // Create the chooser Intent
        final Intent intent = Intent.createChooser(target, getString(R.string.chooser_title));

        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (final ActivityNotFoundException e) {// The reason for the existence of aFileChooser
        }
    }
	
	public void setActionProvider(Menu menu,int resid) {
        final MenuItem item = menu.findItem(resid);
        ShareActionProvider mShareActionProvider = (ShareActionProvider)item.getActionProvider();
        if (mShareActionProvider == null) {
            tcLog.d(getClass().getName(), "onCreateOptionsMenu create new shareActionProvider item = "+item);
            mShareActionProvider = new ShareActionProvider(this);
            item.setActionProvider(mShareActionProvider);
       }
		mShareActionProvider.setShareHistoryFileName("custom_share_history"+resid+".xml");
		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        tcLog.d(getClass().getName(), "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.tracstartmenu, menu);
		setActionProvider(menu,R.id.debug);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        tcLog.d(getClass().getName(), "onDestroy");
        if (mIsBound) {
            sendMessageToService(MSG_STOP_TIMER);
            mHandlerThread.tcQuitSafely();
            unbindService(mConnection);
            mIsBound = false;
        }
        // stopService(new Intent(this, RefreshService.class));
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mProviderMessageReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mDataChangedMessageReceiver);
        super.onDestroy();
    }

    private void onFilterSelected(ArrayList<FilterSpec> filterList) {
        tcLog.d(getClass().getName(), "onFilterSelected");
        final FragmentTransaction ft = fm.beginTransaction();
        final FilterFragment filterFragment = new FilterFragment();

        ft.replace(R.id.displayList, filterFragment, FilterFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(FilterFragmentTag);
        ft.commit();
        filterFragment.setList(filterList);
    }
	
    @Override
    public void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
        tcLog.d(getClass().getName(), "onLogin " + newProfile);
        url = newUrl;
        username = newUser;
        password = newPass;
        sslHack = newHack;
        sslHostNameHack = newHostNameHack;
        profile = newProfile;
        setConfigProvider();
        TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);
        final FragmentTransaction ft = fm.beginTransaction();

        fm.popBackStack();
        if (ticketListFragment == null) {
            ticketListFragment = new TicketListFragment();
            final Bundle args = makeArgs();

            args.putString("currentProfile", profile);
            args.putString("currentFilter", Credentials.getFilterString());
            args.putString("currectSortOrder", Credentials.getSortString());
            ticketListFragment.setArguments(args);
            doNotFinish = true;
            ft.replace(R.id.displayList, ticketListFragment, ListFragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(ListFragmentTag);
        }
        ft.commit();
        refreshOverview();
    }

    private void onNewTicket() {
        tcLog.d(getClass().getName(), "onNewTicket ");

        final NewTicketFragment newtickFragment = new NewTicketFragment();
        // tcLog.d(getClass().getName(), "newTickFragment =" +  newtickFragment.toString());
        final FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(NewFragmentTag);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d(getClass().getName(), "onOptionsItemSelected item=" + item.getTitle());
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
        tcLog.d(getClass().getName(), "onPrepareOptionsMenu");
        // If the nav drawer is open, hide action items related to the content view
		
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		for(int i=0;i<mDrawerIds.length;i++) {
			try {
				menu.findItem(mDrawerIds[i]).setVisible(!drawerOpen);
			} catch(Exception e) {}
		}
		Intent i = null;
	
        final MenuItem itemDebug = menu.findItem(R.id.debug);

        itemDebug.setVisible(debug).setEnabled(debug);
        if (debug) {
           i = shareDebug();

			ShareActionProvider debugShare = (ShareActionProvider)itemDebug.getActionProvider();
            tcLog.d(getClass().getName(), "item = " + itemDebug + " " + debugShare + " " + i);
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
        savedInstanceState.putSerializable(SORTLISTNAME, sortList);
        savedInstanceState.putSerializable(FILTERLISTNAME, filterList);
        savedInstanceState.putString(Const.CURRENT_URL, url);
        savedInstanceState.putString(Const.CURRENT_USERNAME, username);
        savedInstanceState.putString(Const.CURRENT_PASSWORD, password);
        savedInstanceState.putBoolean(Const.CURRENT_SSLHACK, sslHack);
        savedInstanceState.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
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

    private void onSortSelected(ArrayList<SortSpec> sortList) {
        tcLog.d(getClass().getName(), "onSortSelected");
        final FragmentTransaction ft = fm.beginTransaction();
        final SortFragment sortFragment = new SortFragment();

        ft.replace(R.id.displayList, sortFragment, SortFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(SortFragmentTag);
        ft.commit();
        sortFragment.setList(sortList);
    }

    @Override
    public void onStart() {
        super.onStart();
        tcLog.d(getClass().getName(), "onStart");
        MyTracker.reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        tcLog.d(getClass().getName(), "onStop");
        MyTracker.reportActivityStop(this);
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
		boolean isTop = (DetailFragmentTag.equals(getTopFragment()));
        tcLog.d(getClass().getName(), "onTicketSelected Ticket: " + ticket+"isTop = "+ isTop);
		
		DetailFragment detailFragment = new DetailFragment();
        final Bundle args = makeArgs();
        args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
        detailFragment.setArguments(args);
	
        // tcLog.d(getClass().getName(), "detailFragment =" +
        // detailFragment.toString());
        final FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.displayList, detailFragment, DetailFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		if (!isTop) {
			ft.addToBackStack(DetailFragmentTag);
		}
		
        ft.commit();
    }

    @Override
    public void onUpdateTicket(Ticket ticket) {
        tcLog.d(getClass().getName(), "onUpdateTicket ticket = " + ticket);

        final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
        final Bundle args = makeArgs();

        args.putInt(Const.CURRENT_TICKET, ticket.getTicketnr());
        updtickFragment.setArguments(args);
        // tcLog.d(getClass().getName(), "updtickFragment = " + updtickFragment.toString());
        final FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.displayList, updtickFragment, UpdFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(UpdFragmentTag);
        ft.commit();
    }
	
	public Handler getHandler() {
		return tracStartHandler;
	}
	
	public TicketModel getTicketModel() {
//        tcLog.d(getClass().getName(), "getTicketModel " + tm);
		if (tm == null) {
			startProgressBar(R.string.downloading);
			tm = TicketModel.getInstance();
			stopProgressBar();
		}
		return tm;
	}

    private Bundle makeArgs() {
        final Bundle args = new Bundle();

        // args.putString(Const.CURRENT_URL, url);
        // args.putString(Const.CURRENT_USERNAME, username);
        // args.putString(Const.CURRENT_PASSWORD, password);
        // args.putBoolean(Const.CURRENT_SSLHACK, sslHack);
        // args.putBoolean(Const.CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        return args;
    }
	
    private void setFilter(ArrayList<FilterSpec> filter) {
        tcLog.d(getClass().getName(), "setFilter " + filter);
        String filterString = "";

        if (filter != null) {
			filterString = joinList(filter.toArray(),"&");
        }
        Credentials.storeFilterString(filterString);
        filterList = filter;
    }

    private void setFilter(String filterString) {
        tcLog.d(getClass().getName(), "setFilter " + filterString);
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

    private void setSort(ArrayList<SortSpec> sort) {
        tcLog.d(getClass().getName(), "setSort " + sort);

        String sortString = "";

        if (sort != null) {
			sortString = joinList(sort.toArray(),"&");
        }
        Credentials.storeSortString(sortString);
        sortList = sort;
    }

    private void setSort(String sortString) {
        tcLog.d(getClass().getName(), "setSort " + sortString);
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
        setSort(sl);
    }

    @Override
    public boolean getDispAds() {
        return dispAds;
    }

    @Override
    public void refreshOverview() {
        tcLog.d(getClass().getName(), "refreshOverview");
		dataAdapter.changeCursor(null);
        getContentResolver().insert(TicketProvider.RESET_QUERY_URI, null);
		setConfigProvider();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tcLog.d(getClass().getName(), "refreshOverview in UiThread");
				dataAdapter.notifyDataSetChanged();
				getLoaderManager().restartLoader(LIST_LOADER, null, TracStart.this);
				setReferenceTime();
			}
		});
	}

    @Override
    public void setDispAds(boolean b) {
        dispAds = b;
    }

    public Intent shareList() {
        tcLog.d(getClass().getName(), "shareList");
        String lijst = "";
		
		if (dataAdapter != null && dataAdapter.getCursor() != null) {
			TicketCursor c = (TicketCursor)dataAdapter.getCursor();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				final Ticket t = (Ticket) c.getTicket(TicketCursor.FIELD_TICKET);

				try {
					lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
				} catch (final Exception e) {
					tcLog.e(getClass().getName(), "shareList exception", e);
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
	
    @Override
    public void enableDebug() {
        // tcLog.d(getClass().getName(), "enableDebug");
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
        return fm.findFragmentByTag(tag);
    }

    public void getNewTickets(final String isoTijd) {
        tcLog.d(getClass().getName(), "getNewTickets tijd = "+isoTijd) ;
		
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
        // tcLog.d(getClass().getName(), "setReferenceTime");
        referenceTime = System.currentTimeMillis() - timerCorr;
        sendMessageToService(MSG_REMOVE_NOTIFICATION);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        try {
			ret |= ((DetailFragment) getFragment(DetailFragmentTag)).dispatchTouchEvent(ev);
		} catch (Exception e) {}
        return ret;
    }
	
    public void startProgressBar(String message) {
        tcLog.d(getClass().getName(), "startProgressBar " + message);
		try {
			tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_START_PROGRESSBAR,message));
		} catch (NullPointerException e) {
			tcLog.e(getClass().getName(),"startProgressBar NullPointerException");
		}
    }

    public void startProgressBar(int resid) {
        startProgressBar(getString(resid));
    }

 	public void stopProgressBar() {
        tcLog.d(getClass().getName(), "stopProgressBar ");
		try {
			tracStartHandler.sendMessage(tracStartHandler.obtainMessage(MSG_STOP_PROGRESSBAR));
		} catch (NullPointerException e) {
			tcLog.e(getClass().getName(),"stopProgressBar NullPointerException");
		}
	}
	
    private void newDataAdapter(TicketCursor c) {
        tcLog.d(getClass().getName(), "newDataAdapter");
        dataAdapter = new TicketListAdapter(this, R.layout.ticket_list, null);
	}
	
	@Override
	public TicketListAdapter getAdapter() {
		return dataAdapter;
	}
	
	@Override
	public Ticket getTicket(int i) {
		Ticket t = dataAdapter.getTicket(i);
        tcLog.d(getClass().getName(), "getTicket i = "+i+ " ticket = "+ t);
		if (t != null && t.hasdata()) {
			return t;
		}
		
		return refreshTicket(i);
	}
	
	@Override
	public Ticket refreshTicket(int i) {
		Bundle args = new Bundle();
		args.putInt(BUNDLE_TICKET,i);
		if (ticketLoaderStarted) {
			getLoaderManager().restartLoader(TICKET_LOADER,args,this);
		} else {
			getLoaderManager().initLoader(TICKET_LOADER,args,this);
			ticketLoaderStarted = true;
		}
		return null;  // TODO
	}
	
	@Override 
	public void putTicket(Ticket t) {
		//TODO
	}
	
	public void updateTicket(Ticket t,String action, String comment, String veld, String waarde, final boolean notify, Map<String, String> modVeld) throws Exception{
		JSONObject velden = t.getVelden();;

		tcLog.d(this.getClass().getName(), "update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "' " + modVeld);
        tcLog.d(this.getClass().getName(), "velden voor = " + velden);
		int ticknr = t.getTicketnr();
		
        if (ticknr == -1) {
            throw new IllegalArgumentException("Invalid ticketnumber during update");
        }
        if (action == null) {
            throw new NullPointerException("No action supplied update ticket " + ticknr);
        }
        velden.put("action", action);
        if (waarde != null && veld != null && !"".equals(veld) && !"".equals(waarde)) {
            velden.put(veld, waarde);
        }
        if (modVeld != null) {
            final Iterator<Entry<String, String>> i = modVeld.entrySet().iterator();

            while (i.hasNext()) {
                final Entry<String, String> e = i.next();
                // tcLog.d(getClass().getName(), e.toString());
                final String v = e.getKey();
                final String w = e.getValue();

                velden.put(v, w);
            }
        }

        final String cmt = comment == null ? "" : comment;

        velden.remove("changetime");
        velden.remove("time");
		
 		ContentValues args = new ContentValues();
		args.put("comment",cmt);
		args.put("notify",notify);
		args.put("velden",velden.toString());
		Uri uri = Uri.withAppendedPath(TicketProvider.GET_QUERY_URI,""+ticknr);
		getContentResolver().update(uri, args,null,null);
	}
	
	public int createTicket(Ticket t,boolean notify) throws Exception{
		int ticknr = t.getTicketnr();
		final JSONObject velden = t.getVelden();
		
        if (ticknr != -1) {
            throw new RuntimeException("Call create ticket not -1");
        }
        tcLog.i(this.getClass().getName(), "create: " + velden.toString());
        final String s = velden.getString("summary");
        final String d = velden.getString("description");

        velden.remove("summary");
        velden.remove("description");

 		ContentValues args = new ContentValues();
		args.put("summary",s);
		args.put("description",d);
		args.put("velden",velden.toString());
		Uri uri = Uri.withAppendedPath(TicketProvider.GET_QUERY_URI,""+ticknr);
		Uri resultUri = getContentResolver().insert(uri, args);
		if (resultUri == null) {
			return -1;
		} else {
			String lastPart = uri.getLastPathSegment();
			return Integer.parseInt(lastPart);
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

    private BroadcastReceiver mProviderMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c,Intent i) {
			tcLog.d(this.getClass().getName(), "Receive PROVIDER_MESSAGE");
			int title = i.getIntExtra("title",R.string.warning);
			int message = i.getIntExtra("message",R.string.unknownError);
			String addit = i.getStringExtra("additional");
			showAlertBox(title,message,addit);
		}
    };
	
    private BroadcastReceiver mDataChangedMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c,Intent i) {
			tcLog.d(this.getClass().getName(), "Receive DATACHANGED_MESSAGE");
			View v = findViewById(R.id.displayList);
			v.invalidate();
			try {
				getTicketListFragment().dataHasChanged();
			} catch (Exception e) {
				tcLog.e(getClass().getName(),"mDataChangedMessageReceiver cannot contact TicketListFragment");
			}			
		}
    };
	
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean getSslHack() {
		return sslHack;
	}
	
	public boolean getSslHostNameHack() {
		return sslHostNameHack;
	}
	
}
