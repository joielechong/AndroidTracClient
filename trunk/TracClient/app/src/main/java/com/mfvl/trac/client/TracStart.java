/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mfvl.mfvllib.MyLog;

import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.ArrayDeque;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

interface OnTicketLoadedListener {
    void onTicketLoaded(Ticket t);
}

public class TracStart extends TcBaseActivity implements ServiceConnection, FragmentManager.OnBackStackChangedListener,
        NavigationView.OnNavigationItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        ViewTreeObserver.OnGlobalLayoutListener, SharedPreferences.OnSharedPreferenceChangeListener, TcBaseInterface {
    public static final String DetailFragmentTag = "Detail_Fragment";
    private static final int DELAY_2ND_BACK = 2000;
    private static final int CHANGEHOSTMARKER = 1234;
    private static final int ALERT_TIME = 7500;
    private static final int REQUEST_CODE_CHOOSER = 174;
    private static final int REQUEST_CODE_WRITE_EXT = 175;
    private static final String ListFragmentTag = "List_Fragment";
    private static final String LoginFragmentTag = "Login_Fragment";
    private static final String NewFragmentTag = "New_Fragment";
    private static final String UpdFragmentTag = "Modify_Fragment";
    private static final String FilterFragmentTag = "Filter_Fragment";
    private static final String SortFragmentTag = "Sort_Fragment";
    private static final String[] FragmentTags = new String[]{ListFragmentTag, LoginFragmentTag, DetailFragmentTag,
            NewFragmentTag, UpdFragmentTag, FilterFragmentTag, SortFragmentTag};
    private final Semaphore loadingActive = new TcSemaphore(1, true);
    private final Semaphore isBinding = new TcSemaphore(1, true);
	private final BroadcastReceiver performFilterReceiver = new BroadcastReceiver() {
		@Override
		public void	onReceive(Context context, Intent intent) {
            MyLog.d("intent = " + intent);
			String filterString = intent.getStringExtra(FILTERLISTNAME);
			Message m = tracStartHandler.obtainMessage(MSG_SET_FILTER,parseFilterString(filterString));
			MyLog.d(m);
			m.sendToTarget();
		}
	};
	private final BroadcastReceiver performSortReceiver = new BroadcastReceiver() {
		@Override
		public void	onReceive(Context context, Intent intent) {
            MyLog.d("intent = " + intent);
			String sortString = intent.getStringExtra(SORTLISTNAME);
			Message m = tracStartHandler.obtainMessage(MSG_SET_SORT,parseSortString(sortString));
			MyLog.d(m);
			m.sendToTarget();
		}
	};
	private boolean doubleBackToExitPressedOnce = false;
    private FrameLayout adViewContainer = null;
    private AdView adView = null;
    private boolean dispAds = true;
    private String adUnitId;
    private String[] testDevices;
    private ArrayList<SortSpec> sortList = null;
    private ArrayList<FilterSpec> filterList = null;
    private String profile = null;
    private LoginProfile currentLoginProfile = null;
    private String url = null;
    private String userName = null;
    private String passWord = null;
    private boolean sslHack = false;
    private final BroadcastReceiver performLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            MyLog.d("intent = " + intent);
            String uri = intent.getStringExtra(CURRENT_URL);
            String username = intent.getStringExtra(CURRENT_USERNAME);
            String password = intent.getStringExtra(CURRENT_PASSWORD);
            String SelectedProfile = intent.getStringExtra(CURRENT_PROFILE);
            boolean bewaren = intent.getBooleanExtra(BEWAREN, false);
            boolean sslhack = intent.getBooleanExtra(CURRENT_SSLHACK, false);
            boolean sslHostNamehack = intent.getBooleanExtra(CURRENT_SSLHOSTNAMEHACK, false);
            LoginProfile lp = new LoginProfileImpl(uri, username, password, sslHack, sslHostNamehack);
            lp.setProfile(SelectedProfile);
            MyLog.d(lp);
            Message m = tracStartHandler.obtainMessage(MSG_PERFORM_LOGIN, (bewaren ? 1 : 0), 0, lp);
            MyLog.d(m);
            m.sendToTarget();
        }
    };
    private boolean sslHostNameHack = false;
    private OnFileSelectedListener oc = null;
    private boolean canWriteSD = false;
    private long referenceTime = 0;
    private String urlArg = null;
    private int ticketArg = -1;
    private boolean doNotFinish = false;
    private RefreshService mService = null;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle toggle;
    private ProfileDatabaseHelper pdb = null;
    private Intent serviceIntent;
    private boolean hasTicketsLoadingBar = false;
    private Boolean ticketsLoading = false;
    private ArrayAdapter<Ticket> dataAdapter = null;
    private String serviceAction = null;
    private NavigationView navigationView = null;
    private final BroadcastReceiver sqlupdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            MyLog.d("intent = " + intent);
            Menu menu = navigationView.getMenu();
            menu.removeGroup(CHANGEHOSTMARKER);

            MenuItem mi = menu.add(CHANGEHOSTMARKER, Menu.NONE, Menu.NONE, R.string.changehost);
            mi.setEnabled(false);
            Cursor pdbCursor = pdb.getProfiles(false);
            MyLog.d("pdbCursor = " + pdbCursor);
            for (pdbCursor.moveToFirst(); !pdbCursor.isAfterLast(); pdbCursor.moveToNext()) {
                //MyLog.d("pdbCursor 0 = "+pdbCursor.getInt(0));
                //MyLog.d("pdbCursor 1 = "+pdbCursor.getString(1));
                menu.add(CHANGEHOSTMARKER, Menu.NONE, Menu.NONE, pdbCursor.getString(1));
            }
            pdbCursor.close();
        }
    };

    private void removeFilterString() {
        // MyLog.logCall();
        storeFilterString("max=500&status!=closed");
    }

    /**
     * Transform Calendar to ISO 8601 string.
     */
    private String fromUnix(final long tijd) {
        final Date date = new Date();

        date.setTime(tijd);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private void removeSortString() {
        // MyLog.logCall();
        storeSortString("order=priority&order=modified&desc=1");
    }

    @Override
    public ArrayDeque<Message> getMessageQueue() {
        MyLog.logCall();
        return MsgQueueHolder.msgQueue;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        MyLog.d("item = " + item);
        // Handle navigation view item clicks here.

        switch (item.getItemId()) {
            case R.id.help:
                Fragment frag = getSupportFragmentManager().findFragmentByTag(getTopFragment());
                if (frag instanceof TracClientFragment) {
                    ((TracClientFragment) frag).showHelp();
                }
                break;

            case R.id.over:
                showAbout(false);
                break;

            default:
                if (item.getGroupId() == CHANGEHOSTMARKER && item.isEnabled()) {
                    String newProfile = item.getTitle().toString();
                    //MyLog.d(newProfile);
                    LoginProfile lp = pdb.getProfile(newProfile);
                    MyLog.d(lp);
                    if (lp != null) {
                        removeFilterString();
                        removeSortString();
                        onLogin(lp.getUrl(), lp.getUsername(), lp.getPassword(), lp.getSslHack(),
                                lp.getSslHostNameHack(), newProfile, false);
                    }
                }
                break;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLog.setContext(this, getString(R.string.logfile));
//		MyLog.logCall();
        MyLog.d("savedInstanceState = " + savedInstanceState);
        serviceAction = getString(R.string.serviceAction);
        sendMessageToService(-1);

        if (DEBUG_MANAGERS) {
            FragmentManager.enableDebugLogging(true);
        }
        tracStartHandler = new Handler(mHandlerThread.getLooper(), this);
        mMessenger = new Messenger(tracStartHandler);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            canWriteSD = true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permissiontitle)
                        .setMessage(R.string.permissiontext)
                        .setCancelable(false)
                        .setPositiveButton(R.string.oktext, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                tracStartHandler.obtainMessage(MSG_GET_PERMISSIONS).sendToTarget();
                            }
                        }).show();
            } else {
                tracStartHandler.obtainMessage(MSG_GET_PERMISSIONS).sendToTarget();
            }
        }

        setContentView(R.layout.tracstart);
        debug |= isRCVersion();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.traclogo);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        LocalBroadcastManager.getInstance(this).registerReceiver(sqlupdateReceiver, new IntentFilter(DB_UPDATED));
        LocalBroadcastManager.getInstance(this).registerReceiver(performLoginReceiver, new IntentFilter(PERFORM_LOGIN));
        LocalBroadcastManager.getInstance(this).registerReceiver(performFilterReceiver, new IntentFilter(PERFORM_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(performSortReceiver, new IntentFilter(PERFORM_SORT));

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ImageButton settings = (ImageButton) navigationView.getHeaderView(0).findViewById(R.id.settings);
        MyLog.d("settings = " + settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPreferences();
            }
        });

        serviceIntent = new Intent(this, RefreshService.class);
        if (savedInstanceState != null) {
            url = savedInstanceState.getString(CURRENT_URL);
            userName = savedInstanceState.getString(CURRENT_USERNAME);
            passWord = savedInstanceState.getString(CURRENT_PASSWORD);
            sslHack = savedInstanceState.getBoolean(CURRENT_SSLHACK, false);
            sslHostNameHack = savedInstanceState.getBoolean(CURRENT_SSLHOSTNAMEHACK, false);
            filterList = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME);
            sortList = (ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME);
            dispAds = savedInstanceState.getBoolean(ADMOB, true);
            tm = StdTicketModel.restore(savedInstanceState.getString(TicketModel.bundleKey));
            if (tm != null) {
                MyLog.d("restoring TicketModel");
                tracStartHandler.sendMessage(Message.obtain(null, MSG_SET_TICKET_MODEL, tm));
            }
        } else {
            url = getUrl();
            userName = getUsername();
            passWord = getPassword();
            sslHack = getSslHack();
            sslHostNameHack = getSslHostNameHack();
            profile = getProfile();
            setFilter(getFilterString());
            setSort(getSortString());

            // only at first start
            if (isFirstRun()) {
                showAbout(true);
            }
            dispAds = !getIntent().hasExtra(ADMOB) || getIntent().getBooleanExtra(ADMOB, true);
            if (getIntent().hasExtra(INTENT_URL)) {
                urlArg = getIntent().getStringExtra(INTENT_URL);
                ticketArg = (int) getIntent().getLongExtra(INTENT_TICKET, -1);
            }
        }
        adViewContainer = (FrameLayout) findViewById(R.id.displayAd);
        adUnitId = getString(R.string.adUnitId);
        testDevices = getTestDevices();

        initAds();

        pdb = new ProfileDatabaseHelper(this);
        if (urlArg != null) {
            final String urlArg1 = urlArg + "rpc";
            final String urlArg2 = urlArg + "login/rpc";

            if (!(urlArg.equals(url) || urlArg1.equals(url) || urlArg2.equals(url))) {
                LoginProfile lp = pdb.findProfile(urlArg2);

                if (lp == null) {
                    lp = pdb.findProfile(urlArg1);
                }
                if (lp == null) {
                    lp = pdb.findProfile(urlArg);
                }
                if (lp == null) {
                    showAlertBox(R.string.wrongdb, getString(R.string.wrongdbtext, url, urlArg));
                    //R.string.wrongdbtext1,
                    //url + getString(R.string.wrongdbtext2) + urlArg + getString(
                    //       R.string.wrongdbtext3));
                    urlArg = null;
                    ticketArg = -1;
                } else {
                    url = lp.getUrl();
                    userName = lp.getUsername();
                    passWord = lp.getPassword();
                    sslHack = lp.getSslHack();
                    sslHostNameHack = false; // force dialog to confirm
                    profile = null;
                }
                currentLoginProfile = lp;
            }
        }

        newDataAdapter(new TicketsStore()); // empty list
        getSupportFragmentManager().addOnBackStackChangedListener(this);
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
            MyLog.d("backstack restored");
            startListLoader(true);
        } else {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            if (url != null && url.length() > 0) {
                startListLoader(true);

                final Fragment ticketListFragment = new TicketListFragment();

                if (urlArg != null) {
                    MyLog.d("select NormalTicket = " + ticketArg);
                    final Bundle args = makeArgs();
                    args.putInt("TicketArg", ticketArg);
                    urlArg = null;
                    ticketArg = -1;
                    ticketListFragment.setArguments(args);
                }
                ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
//                MyLog.d("ft.add "+ListFragmentTag);
                ft.addToBackStack(ListFragmentTag);
            } else {
                final Fragment tracLoginFragment = newLoginFrag();

                ft.add(R.id.displayList, tracLoginFragment, LoginFragmentTag);
//                MyLog.d("ft.add " + LoginFragmentTag);
                ft.addToBackStack(LoginFragmentTag);
            }
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.commit();
            MyLog.d("backstack initiated");
        }
        setReferenceTime();
        TracGlobal.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void startPreferences() {
        Intent launchPrefs = new Intent(TracStart.this, TcPreference.class);
        launchPrefs.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        launchPrefs.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.mfvl.trac.client.TcPreference$SettingsFragment");
        startActivity(launchPrefs);
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//		MyLog.d("newConfig = "+newConfig);
        if (dispAds) {
            if (adView != null) {
                adViewContainer.removeView(adView);  // first remove old adView
            }
            initAds();
        }
    }

    private String[] getTestDevices() {
        String[] td = null;
        final String t = getString(R.string.testDevice1);
        if (!"".equals(t)) {
            try {
                td = t.split(",");
            } catch (final IllegalArgumentException e) { // only 1 in split
                td = new String[]{t};
            } catch (Exception e) {
                MyLog.e(e);
            }
        }
        return td;
    }

    private void initAds() {
        if (dispAds) {
            try {
                adView = new AdView(this);
                adView.setAdUnitId(adUnitId);
                adView.setAdSize(AdSize.SMART_BANNER);

                final AdRequest.Builder arb = new AdRequest.Builder();

                arb.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
                if (isDebuggable() && testDevices != null) {
                    for (final String t : testDevices) {
                        MyLog.d("testDevice = " + t);
                        arb.addTestDevice(t);
                    }
                }
                arb.setGender(AdRequest.GENDER_UNKNOWN);
                final AdRequest adRequest = arb.build();

                adView.loadAd(adRequest);
                adView.setLayoutParams(adViewContainer.getLayoutParams());
                adViewContainer.addView(adView);
                adViewContainer.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                MyLog.e("Not displaying ads", e);
            }
        }
    }

    private void startListLoader(boolean newLoad) {
        MyLog.d("newLoad = " + newLoad);
        if (newLoad) {
            tracStartHandler.obtainMessage(MSG_START_LISTLOADER, null).sendToTarget();
        } else {
            tracStartHandler.obtainMessage(MSG_REFRESH_LIST, null).sendToTarget();
        }
    }

    private void dispatchMessage(final Message msg) {
        msg.replyTo = mMessenger;
        MyLog.d("mService = " + mService + " msg = " + msg);
        if (mService != null) {
            mService.send(msg);
        } else {
            new Thread() {
                @Override
                public void run() {
                    isBinding.acquireUninterruptibly();
                    if (mService == null) {
                        MyLog.d("using bindService");
                        bindService(new Intent(TracStart.this,
                                        RefreshService.class).setAction(serviceAction)
                                        .putExtra(INTENT_CMD, msg.what)
                                        .putExtra(INTENT_ARG1, msg.arg1)
                                        .putExtra(INTENT_ARG2, msg.arg2)
                                        .putExtra(INTENT_OBJ, (Serializable) msg.obj),
                                TracStart.this, Context.BIND_AUTO_CREATE);
                    } else {
                        isBinding.release();
                        MyLog.d("using sendMessage");
                        mService.send(msg);
                    }
                    MyLog.d("Message " + msg + " sent");
                }
            }.start();
        }
    }

    private void showAlertBox(final int titleres, final CharSequence message) {
        MyLog.d("titleres = " + titleres + " : " + getString(titleres));
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog ad = new AlertDialog.Builder(TracStart.this)
                            .setTitle(titleres)
                            .setMessage(message)
                            .setPositiveButton(R.string.oktext, null)
                            .create();
                    tracStartHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //MyLog.d("dismiss");
                            try {
                                ad.dismiss();
                            } catch (Exception ignored) {
                            }
                        }
                    }, ALERT_TIME);
                    ad.show();
                }
            });
        }
    }

    private void restoreFragment(Bundle savedInstanceState, final String tag) {
        MyLog.d("tag = " + tag);
        if (savedInstanceState.containsKey(tag)) {
            try {
                getSupportFragmentManager().getFragment(savedInstanceState, tag);
            } catch (final Exception ignored) {
            }
        }
    }

    private void shouldDisplayHomeUp() {
        // Enable Up button only if there are entries in the back stack
        MyLog.d("entry count = " + getSupportFragmentManager().getBackStackEntryCount());
        final boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 1;

        MyLog.d("canBack = " + canBack);
        final ActionBar ab = getActionBar();

        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(canBack);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        MyLog.d(key);
        if (prefFilterKey.equals(key)) {
            String filterString = sharedPreferences.getString(key, "");
            MyLog.d(filterString);
            filterList = parseFilterString(filterString);
            MyLog.d(filterList);
            startListLoader(true);
        } else if (prefSortKey.equals(key)) {
            String sortString = sharedPreferences.getString(key, "");
            MyLog.d(sortString);
            sortList = parseSortString(sortString);
            MyLog.d(sortList);
            startListLoader(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyLog.logCall();
        if (adView != null) {
            adView.resume();
        }
        findViewById(R.id.displayList).getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        MyLog.logCall();
        if (tm != null) {
            tm.onSaveInstanceState(savedInstanceState);
        }
        savedInstanceState.putBoolean(ADMOB, dispAds);
        savedInstanceState.putSerializable(SORTLISTNAME, sortList);
        savedInstanceState.putSerializable(FILTERLISTNAME, filterList);
        savedInstanceState.putString(CURRENT_URL, url);
        savedInstanceState.putString(CURRENT_USERNAME, userName);
        savedInstanceState.putString(CURRENT_PASSWORD, passWord);
        savedInstanceState.putBoolean(CURRENT_SSLHACK, sslHack);
        savedInstanceState.putBoolean(CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        saveFragment(savedInstanceState, ListFragmentTag);
        saveFragment(savedInstanceState, LoginFragmentTag);
        saveFragment(savedInstanceState, DetailFragmentTag);
        saveFragment(savedInstanceState, NewFragmentTag);
        saveFragment(savedInstanceState, UpdFragmentTag);
        saveFragment(savedInstanceState, FilterFragmentTag);
        saveFragment(savedInstanceState, SortFragmentTag);
        MyLog.d("savedInstanceState = " + savedInstanceState);
    }

    private void saveFragment(Bundle savedInstanceState, final String tag) {
        try {
            final Fragment f = getFragment(tag);

            if (f != null) {
                getSupportFragmentManager().putFragment(savedInstanceState, tag, f);
            }
        } catch (final Exception ignored) {// Exception if fragment not on stack can be ignored
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        MyLog.logCall();
        stopProgressBar();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findViewById(R.id.displayList).getViewTreeObserver().removeOnGlobalLayoutListener(this);
        } else {
            findViewById(R.id.displayList).getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
        if (adView != null) {
            adView.pause();
        }
        MyLog.d("isFinishing = " + isFinishing());
    /* save logfile when exiting */
        if (isFinishing() && isRCVersion()) {
            MyLog.save();
        }
    }

    @Override
    protected void onDestroy() {
        MyLog.d("isFinishing = " + isFinishing());
        TracGlobal.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mDrawerLayout.addDrawerListener(toggle);
        if (isFinishing()) {
            stopService(serviceIntent);
        }
        super.onDestroy();
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        MyLog.d(frag + " this = " + this);
        super.onAttachFragment(frag);

        if (ListFragmentTag.equals(frag.getTag())) {
            final TicketListFragment ticketListFragment = getTicketListFragment();
            if (ticketListFragment != null) {
                MyLog.d("ticketListFragment = " + ticketListFragment);

                if (urlArg != null) {
                    MyLog.d("Ticket = " + ticketArg);
                    ticketListFragment.selectTicket(ticketArg);
                    urlArg = null;
                    ticketArg = -1;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        MyLog.logCall();
        DetailInterface dt = (DetailInterface) getFragment(DetailFragmentTag);

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            boolean processed = false;
            if (DetailFragmentTag.equals(getTopFragment())) {
                processed = dt.onBackPressed();
            }
            if (!processed) {
                //MyLog.d("Stackcount = "+getSupportFragmentManager().getBackStackEntryCount());
                //MyLog.d("doubleBackToExitPressedOnce="+doubleBackToExitPressedOnce);
                if (getSupportFragmentManager().getBackStackEntryCount() > 1 || doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                } else {
                    doubleBackToExitPressedOnce = true;
                    MyLog.toast(getString(R.string.doubleback));
                    tracStartHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, DELAY_2ND_BACK);
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean ret = super.dispatchTouchEvent(ev);
        try {
            ret |= ((DetailInterface) getFragment(DetailFragmentTag)).dispatchTouchEvent(ev);
        } catch (Exception ignored) {
        }
        return ret;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MyLog.logCall();
        getMenuInflater().inflate(R.menu.tracstartmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MyLog.logCall();
        menu.findItem(R.id.debug).setVisible(debug).setEnabled(debug);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MyLog.d("item=" + item.getTitle());

        switch (item.getItemId()) {
            case android.R.id.home:
                if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                } else {
                    return false;
                }
                break;

            case R.id.settings:
                startPreferences();
                break;

            case R.id.over:
                showAbout(false);
                break;

            case R.id.tlrefresh:
                refreshOverview();
                break;

            case R.id.tlnieuw:
                onNewTicket();
                break;

            case R.id.debug:
                final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, MyLog.getDebug());
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showAbout(boolean showCookies) {
        MyLog.d("showAbout");
        DialogFragment about = new TracShowWebPageDialogFragment();
        Bundle aboutArgs = new Bundle();
        aboutArgs.putString(HELP_FILE, getString(R.string.whatsnewhelpfile));
        aboutArgs.putBoolean(HELP_VERSION, true);
        aboutArgs.putBoolean(HELP_COOKIES, showCookies);
        aboutArgs.putInt(HELP_ZOOM, webzoom);
//        about.preLoad(getLayoutInflater(), aboutArgs);
        about.setArguments(aboutArgs);
        about.show(getSupportFragmentManager(), "about");
    }

    @Override
    public boolean onNavigateUp() {
        MyLog.d("entry count = " + getSupportFragmentManager().getBackStackEntryCount());
        // This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MyLog.d("requestCode = " + requestCode + " permissions = " + Arrays.asList(
                permissions) + " grantResults = " + Arrays.asList(grantResults));
        if (requestCode == REQUEST_CODE_WRITE_EXT) {
            // If request is cancelled, the result arrays are empty.
            canWriteSD = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MyLog.d("requestcode = " + requestCode + " intent = " + data);
        if (requestCode == REQUEST_CODE_CHOOSER) {
            if (resultCode == RESULT_OK && data != null) {
                // Get the URI of the selected file
                final Uri uri = data.getData();
                MyLog.d("uri = " + uri);
                if (oc != null) {
                    oc.onFileSelected(uri);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onNewTicket() {
        MyLog.logCall();

        final Fragment newtickFragment = new NewTicketFragment();
        // MyLog.d("newTickFragment =" +  newtickFragment.toString());
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = makeArgs();
        args.putString(CURRENT_USERNAME, userName);
        newtickFragment.setArguments(args);

        ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.addToBackStack(NewFragmentTag);
        ft.commit();
    }

    private TracLoginFragment newLoginFrag() {
        final TracLoginFragment tracLoginFragment = new TracLoginFragment();
        final Bundle args = makeArgs();
        args.putString(CURRENT_URL, url);
        args.putString(CURRENT_USERNAME, userName);
        args.putString(CURRENT_PASSWORD, passWord);
        args.putBoolean(CURRENT_SSLHACK, sslHack);
        args.putBoolean(CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        tracLoginFragment.setArguments(args);
        return tracLoginFragment;
    }

    private void setFilter(String filterString) {
        MyLog.d(filterString);
        setFilter(parseFilterString(filterString));
    }

    private void setFilter(ArrayList<FilterSpec> filter) {
        MyLog.d(filter.toString());
        String filterString = joinList(filter.toArray(), "&");
        storeFilterString(filterString);
        filterList = filter;
    }

    private void setSort(String sortString) {
        MyLog.d(sortString);
        final ArrayList<SortSpec> sl = parseSortString(sortString);
        setSort(sl);
    }

    private void setSort(ArrayList<SortSpec> sort) {
        MyLog.d(sort.toString());

        String sortString = joinList(sort.toArray(), "&");
        storeSortString(sortString);
        sortList = sort;
    }

    private void setReferenceTime() {
        // MyLog.d("setReferenceTime");
        referenceTime = new Date().getTime() - timerCorr;
        sendMessageToService(MSG_REMOVE_NOTIFICATION);
    }

    private void sendMessageToService(int message) {
        dispatchMessage(Message.obtain(null, message));
    }

    private void newDataAdapter(Tickets tl) {
        MyLog.logCall();
        dataAdapter = new TicketListAdapter(this, tl);
        dataAdapter.getFilter().filter(null);
        dataAdapter.setNotifyOnChange(true);
        try {
            getTicketListFragment().setAdapter(dataAdapter);
        } catch (NullPointerException e) {
            MyLog.e("NullPointerException");
        }
    }

    private TicketListFragment getTicketListFragment() {
        return (TicketListFragment) getFragment(ListFragmentTag);
    }

    private Fragment getFragment(final String tag) {
        return getSupportFragmentManager().findFragmentByTag(tag);
    }

    @Override
    public void onBackStackChanged() {
        final int depth = getSupportFragmentManager().getBackStackEntryCount();

        MyLog.d("depth = " + depth);
        if (depth == 0 && !doNotFinish) {
            finish();
        }
        doNotFinish = false;
        shouldDisplayHomeUp();
    }

    @Override
    public void onChooserSelected(OnFileSelectedListener _oc) {
        MyLog.logCall();
        // store  callback
        this.oc = _oc;
        // Create the ACTION_GET_CONTENT Intent
        Intent getContentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(getContentIntent, getString(R.string.chooser_title));
        startActivityForResult(intent, REQUEST_CODE_CHOOSER);
    }

    private void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile, boolean bewaren) {
        MyLog.d(newUrl + " " + newUser + " " + newPass + " " + newHack + " " + newHostNameHack + " " + newProfile);
        url = newUrl;
        userName = newUser;
        passWord = newPass;
        sslHack = newHack;
        sslHostNameHack = newHostNameHack;
        profile = newProfile;
        setFilter(getFilterString());
        setSort(getSortString());
        TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);

        if (ticketListFragment == null) {
            doNotFinish = true;
            getSupportFragmentManager().popBackStackImmediate(LoginFragmentTag,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ticketListFragment = new TicketListFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.addToBackStack(ListFragmentTag);
            ft.commit();
        } else {
            getSupportFragmentManager().popBackStackImmediate(ListFragmentTag, 0);
        }
        newDataAdapter(new TicketsStore()); // empty list
        startListLoader(true);
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
        boolean isTop = (DetailFragmentTag.equals(getTopFragment()));
        MyLog.d("Ticket: " + ticket + " isTop = " + isTop);

        Fragment detailFragment = new DetailFragment();
        final Bundle args = makeArgs();
        args.putInt(CURRENT_TICKET, ticket.getTicketnr());
        detailFragment.setArguments(args);

        //		MyLog.d("detailFragment =" + detailFragment.toString());
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.displayList, detailFragment, DetailFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        if (!isTop) {
            ft.addToBackStack(DetailFragmentTag);
        }

        ft.commit();
    }

    @Override
    public void onUpdateTicket(Ticket ticket) {
        MyLog.d("ticket = " + ticket);

        final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
        final Bundle args = makeArgs();

        args.putInt(CURRENT_TICKET, ticket.getTicketnr());
        updtickFragment.setArguments(args);
//		MyLog.d("updtickFragment = " + updtickFragment.toString());
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ft.replace(R.id.displayList, updtickFragment, UpdFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.addToBackStack(UpdFragmentTag);
        ft.commit();
    }

    @Override
    public void refreshOverview() {
        MyLog.logCall();
//		dataAdapter.notifyDataSetChanged();
        startListLoader(false);
        setReferenceTime();
    }

    @Override
    public ArrayAdapter<Ticket> getAdapter() {
        MyLog.d("dataAdapter = " + dataAdapter);
        return dataAdapter;
    }

    @Override
    public void getTicket(final int i, final OnTicketLoadedListener _oc) {
        //MyLog.d("i = " + i + " semaphore = " + loadingActive);

        new Thread() {
            @Override
            public void run() {
                if (loadingActive.availablePermits() == 0) {
                    loadingActive.acquireUninterruptibly();
                    loadingActive.release();
                }

                Ticket t = TicketsStore.getTicket(i);
                MyLog.d("i = " + i + " ticket = " + t);
                if (t != null && !t.hasdata()) {
                    refreshTicket(i);
                }
                if (_oc != null) {
                    _oc.onTicketLoaded(t);
                }
            }
        }.start();
    }

    @Override
    public void refreshTicket(final int i) {
        startProgressBar(R.string.ophalen);
        sendMessageToService(MSG_SEND_TICKETS, i, MSG_DISPLAY_TICKET, null);
    }

    private void sendMessageToService(int message, int value, int msg_back, Object o) {
        dispatchMessage(Message.obtain(null, message, value, msg_back, o));
    }

    @Override
    public int getNextTicket(int i) {
        return ((TicketListAdapterIF) dataAdapter).getNextTicket(i);
    }

    @Override
    public int getPrevTicket(int i) {
        return ((TicketListAdapterIF) dataAdapter).getPrevTicket(i);
    }

    @Override
    public int getTicketCount() {
        return dataAdapter.getCount();
    }

    @Override
    public int getTicketContentCount() {
        try {
            return ((TicketListAdapterIF) dataAdapter).getTicketContentCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void updateTicket(final Ticket t, final String action, final String comment, final String veld, final String waarde, final boolean notify, final Map<String, String> modVeld) throws
            Exception {
        final JSONObject velden = t.getVelden();
        MyLog.d("Ticket = " + t + "update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "' " + modVeld);
//        MyLog.d("velden voor = " + velden);
        final int ticknr = t.getTicketnr();

        if (ticknr == -1) {
            throw new IllegalArgumentException(getString(R.string.invtick));
        }
        if (action == null) {
            throw new NullPointerException(getString(R.string.noaction));
        }
        velden.put("serviceAction", action);
        if (waarde != null && veld != null && !"".equals(veld) && !"".equals(waarde)) {
            velden.put(veld, waarde);
        }
        if (modVeld != null) {
            for (Entry<String, String> e : modVeld.entrySet()) {
//				MyLog.d(e.toString());
                velden.put(e.getKey(), e.getValue());
            }
        }

        final String cmt = comment == null ? "" : comment;

        velden.remove("changetime");
        velden.remove("time");

        new Thread() {
            @Override
            public void run() {
                try {
                    TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack,
                            userName, passWord);
                    JSONArray retTick = tracClient.updateTicket(ticknr, cmt, velden, notify);
                    t.setFields(retTick.getJSONObject(3));
                    if (modVeld != null) {
                        modVeld.clear();
                    }
                    MyLog.d("retTicket = " + retTick);
                } catch (final Exception e) {
                    MyLog.e("Exception during update", e);
                    showAlertBox(R.string.upderr, getString(R.string.storerrdesc, e.getMessage()));
                }
            }
        }.start();
    }

    @Override
    public int createTicket(Ticket t, boolean notify) throws Exception {
        int ticknr = t.getTicketnr();
        final JSONObject velden = t.getVelden();

        if (ticknr != -1) {
            throw new IllegalArgumentException("Call create ticket not -1");
        }
        MyLog.i("create: " + velden.toString());
        final String s = velden.getString("summary");
        final String d = velden.getString("description");

        velden.remove("summary");
        velden.remove("description");

        try {
            TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack, userName, passWord);
            final int newticknr = tracClient.createTicket(s, d, velden, notify);
            if (newticknr == -1) {
                showAlertBox(R.string.storerr, getString(R.string.noticketUnk));
                return -1;
            } else {
//				reloadTicketData(new NormalTicket(newticknr));
                refreshTicket(newticknr);
                return newticknr;
            }
        } catch (final Exception e) {
            MyLog.e("Exception during create", e);
            showAlertBox(R.string.storerr, getString(R.string.storerrdesc, e.getMessage()));
            return -1;
        }
    }

    @Override
    public void listViewCreated() {
        MyLog.d("ticketsLoading = " + ticketsLoading + " hasTicketsLoadingBar = " + hasTicketsLoadingBar);
        synchronized (this) {
            if (ticketsLoading) {
                if (!hasTicketsLoadingBar) {
                    startProgressBar(getString(R.string.getlist, (profile == null ? "" : profile)));
                    hasTicketsLoadingBar = true;
                }
                getTicketListFragment().startLoading();
            }
        }
    }

    @Override
    public boolean getCanWriteSD() {
        return canWriteSD;
    }

    @Override
    public void getAttachment(final Ticket ticket, final String filename, final onAttachmentCompleteListener _oc) {
        MyLog.d(ticket.toString() + " " + filename);
        final int _ticknr = ticket.getTicketnr();
        new Thread() {
            @Override
            public void run() {
//                available.acquireUninterruptibly();
                if (_oc != null) {
                    try {
                        TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack, userName, passWord);
                        _oc.onComplete(tracClient.getAttachment(_ticknr, filename));
                    } catch (final Exception e) {
                        MyLog.e("Exception during getAttachment", e);
//                    } finally {
//                        available.release();
                    }
                }
            }
        }.start();
    }

    /*
     * always executed in a thread
     */
    @Override
    public void addAttachment(final Ticket ticket, final Uri uri, final onTicketCompleteListener _oc) {
        tracStartHandler.post(new Runnable() {

            @Override
            public void run() {
                MyLog.i(ticket.toString() + " " + uri);
                final int _ticknr = ticket.getTicketnr();
                if (uri != null) {
                    String filename = null;
                    int bytes = 0;
                    if (uri.toString().startsWith("file:")) {
                        filename = uri.getPath();
                    } else {
                        Cursor c = getContentResolver().query(uri, null, null, null, null);
                        if (c != null) {
                            if (c.moveToFirst()) {
                                // MyLog.d("ColumnNames = "+Arrays.asList(c.getColumnNames()));
                                int id = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                                if (id != -1) {
                                    filename = c.getString(id);
                                }
                                id = c.getColumnIndex(MediaStore.MediaColumns.SIZE);
                                if (id != -1) {
                                    bytes = c.getInt(id);
                                }
                            }
                            c.close();
                        }
                    }
                    final File file = new File(filename == null ? uri.getPath() : filename);
                    if (bytes == 0) {
                        bytes = (int) file.length();
                    }
                    final int maxBytes = (bytes > 0 ? bytes : 120000);   // 6 fold because of Base64
                    final byte[] data = new byte[maxBytes];

                    try {
                        final InputStream is = getContentResolver().openInputStream(uri);
                        if (is != null) {
                            String b64 = "";
                            for (int nbytes = is.read(data); nbytes >= 0; nbytes = is.read(data)) {
                                b64 += Base64.encodeToString(data, 0, nbytes, Base64.DEFAULT);
                            }
                            is.close();
                            new TracHttpClient(url, sslHack, sslHostNameHack, userName,
                                    passWord).putAttachment(_ticknr, file.getName(), b64);
                        } else {
                            MyLog.e("Cannot open " + uri);
                            showAlertBox(R.string.warning, getString(R.string.notfound, filename));
                        }
                    } catch (final FileNotFoundException e) {
                        MyLog.e("Cannot open : " + uri, e);
                        showAlertBox(R.string.warning, getString(R.string.notfound, filename));
                    } catch (final NullPointerException | IOException | JSONRPCException | JSONException e) {
                        MyLog.e("Exception during addAttachment, uri = " + uri, e);
                        showAlertBox(R.string.warning, getString(R.string.failed, filename));
                    } finally {
                        _oc.onComplete();
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean processMessage(final Message msg) {
        MyLog.d("msg = " + msg);
        switch (msg.what) {
            case MSG_REQUEST_TICKET_COUNT:
                if (!LoginFragmentTag.equals(getTopFragment())) {
                    final int count = getTicketCount();
                    sendMessageToService(MSG_SEND_TICKET_COUNT, count, fromUnix(referenceTime));
                }
                break;

            case MSG_SET_SORT:
                setSort((ArrayList<SortSpec>) msg.obj);
                sendMessageToService(MSG_SET_SORT, msg.obj);
                refreshOverview();
                break;

            case MSG_SET_FILTER:
                setFilter((ArrayList<FilterSpec>) msg.obj);
                sendMessageToService(MSG_SET_FILTER, msg.obj);
                refreshOverview();
                break;

            case MSG_SHOW_DIALOG:
                showAlertBox(msg.arg1, (CharSequence) msg.obj);
                break;

            case MSG_SET_TICKET_MODEL:
                tm = (TicketModel) msg.obj;
                for (String f : FragmentTags) {
                    Fragment frag = getSupportFragmentManager().findFragmentByTag(f);
                    if (frag != null && frag instanceof TracClientFragment) {
                        ((TracClientFragment) frag).onNewTicketModel(tm);
                    }
                }
                break;

            case MSG_DISPLAY_TICKET:
                stopProgressBar();
                final Ticket t = (Ticket) msg.obj;
                if (DetailFragmentTag.equals(getTopFragment())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final DetailInterface d = (DetailInterface) getFragment(DetailFragmentTag);
                            d.setTicket(t.getTicketnr());
                        }
                    });
                } else {
                    onTicketSelected(t);
                }
                break;

            case MSG_GET_PERMISSIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(TracStart.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_WRITE_EXT);
                }
                break;

            case MSG_START_LISTLOADER:
                currentLoginProfile = new LoginProfileImpl(url, userName, passWord, sslHack)
                        .setSslHostNameHack(sslHostNameHack)
                        .setFilterList(filterList)
                        .setSortList(sortList);

                hasTicketsLoadingBar = false;
                // Returns a new CursorLoader
                try {
                    getTicketListFragment().startLoading();
                } catch (Exception ignored) {
                }
                if (ListFragmentTag.equals(getTopFragment())) {
                    startProgressBar(getString(R.string.getlist, (profile == null ? "" : profile)));
                    hasTicketsLoadingBar = true;
                }
                if (loadingActive.availablePermits() == 0) {  // release semaphore if in use
                    loadingActive.release();
                }
                tracStartHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingActive.acquireUninterruptibly();
                        ticketsLoading = true;

                        MyLog.d("MSG_START_LISTLOADER: " + mService);
                        dispatchMessage(Message.obtain(null, MSG_LOAD_TICKETS, currentLoginProfile));
                    }
                });
                break;

            case MSG_REFRESH_LIST:
                if (ListFragmentTag.equals(getTopFragment())) {
                    startProgressBar(getString(R.string.getlist, (profile == null ? "" : profile)));
                    hasTicketsLoadingBar = true;
                }
                dispatchMessage(Message.obtain(null, MSG_LOAD_TICKETS, null));
                break;

            case MSG_LOAD_ABORTED:
                if (loadingActive.availablePermits() == 0) {
                    loadingActive.release();
                }
                notifyTicketListFragment();
                break;

            case MSG_LOAD_FASE1_FINISHED:
                final Tickets tl = (Tickets) msg.obj;
                // MyLog.d("Tickets = " + tl);
                if (hasTicketsLoadingBar) {
                    stopProgressBar();
                    hasTicketsLoadingBar = false;
                }
                ticketsLoading = false;
                TracStart.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataAdapter.clear();
                        ((TicketListAdapterIF) dataAdapter).addAll(tl);
                        notifyTicketListFragment();
                    }
                });
                break;

            case MSG_DATA_CHANGED:
                notifyTicketListFragment();
                break;

            case MSG_LOAD_FASE2_FINISHED:
                if (loadingActive.availablePermits() == 0) {
                    loadingActive.release();
                }
                notifyTicketListFragment();
                break;

            case MSG_PERFORM_LOGIN:
                final LoginProfile lp = (LoginProfile) msg.obj;
                MyLog.d(lp);
                removeFilterString();
                removeSortString();
                TracStart.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MyLog.d(lp);
                        onLogin(lp.getUrl(), lp.getUsername(), lp.getPassword(), lp.getSslHack(),
                                lp.getSslHostNameHack(), lp.getProfile(), (msg.arg1 == 1));
                    }
                });
                break;

            default:
                return super.processMessage(msg);
        }
        return true;
    }

    private void sendMessageToService(int message, int value, Object o) {
        dispatchMessage(Message.obtain(null, message, value, 0, o));
    }

    private void sendMessageToService(int message, Object value) {
        dispatchMessage(Message.obtain(null, message, value));
    }

    private String getTopFragment() {
        try {
            int bs = getSupportFragmentManager().getBackStackEntryCount();
            return getSupportFragmentManager().getBackStackEntryAt(bs - 1).getName();
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyTicketListFragment() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    getTicketListFragment().dataHasChanged();
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onGlobalLayout() {
        final View view = findViewById(android.R.id.content);

        if (view != null) {
            final ActionBar ab = getActionBar();
            final Rect r = new Rect();

            // r will be populated with the coordinates of your view that area still visible.
            view.getWindowVisibleDisplayFrame(r);

            final int heightDiff = view.getRootView().getHeight() - (r.bottom - r.top);
            if (heightDiff > 100) { // if more than 100 pixels,
                // its probably a keyboard...
                if (ab != null) {
                    ab.hide();
                }
                if (dispAds) {
                    adViewContainer.setVisibility(View.GONE);
                }
            } else {
                if (dispAds) {
                    adViewContainer.setVisibility(View.VISIBLE);
                }
                if (ab != null) {
                    ab.show();
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        RefreshBinder binder = (RefreshBinder) service;

        mService = binder.getService();
        mService.setTracStartHandler(tracStartHandler);
        MyLog.d("mConnection mService = " + mService);
        unbindService(this);
        isBinding.release();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        MyLog.d("className = " + className);
    }
}

class TcSemaphore extends Semaphore {

    TcSemaphore(int permits, boolean fair) {
        super(permits, fair);
    }

    @Override
    public void acquireUninterruptibly() {
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            try {
                throw new Exception("debug");
            } catch (Exception e) {
                MyLog.e(e);
            }
        }
        super.acquireUninterruptibly();
    }
}
