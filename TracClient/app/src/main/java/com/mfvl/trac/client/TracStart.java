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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mfvl.mfvllib.MyLog;
import com.mfvl.mfvllib.SysOps;

import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

interface OnTicketLoadedListener {
    void onTicketLoaded(Ticket t);
}

public class TracStart extends TcBaseActivity implements ServiceConnection, FragmentManager.OnBackStackChangedListener,
        NavigationView.OnNavigationItemSelectedListener, DataChangedListener, TicketCountInterface,
        ViewTreeObserver.OnGlobalLayoutListener, SharedPreferences.OnSharedPreferenceChangeListener {
    static final String DetailFragmentTag = "Detail_Fragment";
    private static final int DELAY_2ND_BACK = 2000;
    private static final int CHANGEHOSTMARKER = 1234;
    private static final int REQUEST_CODE_CHOOSER = 174;
    private static final String ListFragmentTag = "List_Fragment";
    private static final String NewFragmentTag = "New_Fragment";
    private static final String UpdFragmentTag = "Modify_Fragment";

    private boolean doubleBackToExitPressedOnce = false;
    private FrameLayout adViewContainer = null;
    private AdView adView = null;
    private boolean dispAds = true;
    private String adUnitId = null;
    private String[] testDevices = null;
    private ArrayList<SortSpec> sortList = null;
    private ArrayList<FilterSpec> filterList = null;
    private String profile = null;
    private LoginProfile currentLoginProfile = null;
    private String url = null;
    private String userName = null;
    private String passWord = null;
    private boolean sslHack = false;
    private boolean sslHostNameHack = false;
    private OnFileSelectedListener oc = null;
    private long referenceTime = 0;
    private String urlArg = null;
    private int ticketArg = -1;
    private boolean doNotFinish = false;
    private TracClientService mService = null;
    private DrawerLayout mDrawerLayout = null;
    private ActionBarDrawerToggle toggle = null;
    private ProfileDatabaseHelper pdb = null;
    private Intent serviceIntent = null;
    private boolean hasTicketsLoadingBar = false;
    private Boolean ticketsLoading = false;
    private TicketListAdapter dataAdapter = null;
    private NavigationView navigationView = null;
    private final BroadcastReceiver sqlupdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            MyLog.d("intent = " + intent + "(" + intent.getStringExtra(DB_UPDATED) + ")");
            update_navmenu();
        }
    };

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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        MyLog.d("item = " + item);
        // Handle navigation view item clicks here.

        switch (item.getItemId()) {
            case R.id.settings:
                startPreferences();
                break;

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
                                lp.getSslHostNameHack(), newProfile);
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
        serviceIntent = new Intent(this, TracClientService.class);

        if (DEBUG_MANAGERS) {
            FragmentManager.enableDebugLogging(true);
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

        if (savedInstanceState != null) {
            url = savedInstanceState.getString(CURRENT_URL);
            userName = savedInstanceState.getString(CURRENT_USERNAME);
            passWord = savedInstanceState.getString(CURRENT_PASSWORD);
            sslHack = savedInstanceState.getBoolean(CURRENT_SSLHACK, false);
            sslHostNameHack = savedInstanceState.getBoolean(CURRENT_SSLHOSTNAMEHACK, false);
            filterList = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME);
            sortList = (ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME);
            dispAds = savedInstanceState.getBoolean(ADMOB, true);
            TicketModel ticketModel = TicketModel.restore(savedInstanceState.getString(TicketModel.bundleKey));
            if (ticketModel != null) {
                MyLog.d("restoring TicketModel");
                newTicketModel(ticketModel);
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
                showAbout(mustShowCookie(this));
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
            final String urlArg1 = String.format(Locale.US, "%srpc", urlArg);
            final String urlArg2 = String.format(Locale.US, "%slogin/rpc", urlArg);

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

        newDataAdapter(new Tickets()); // empty list
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        // Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        if (savedInstanceState != null) {
            restoreFragment(savedInstanceState, ListFragmentTag);
            restoreFragment(savedInstanceState, DetailFragmentTag);
            restoreFragment(savedInstanceState, NewFragmentTag);
            restoreFragment(savedInstanceState, UpdFragmentTag);
            MyLog.d("backstack restored");
            startListLoader(true);
        } else {

            if (url != null && url.length() > 0) {
                startListLoader(true);
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                final Fragment ticketListFragment = new TicketListFragment();

                if (urlArg != null) {
                    MyLog.d("select Ticket = " + ticketArg);
                    final Bundle args = new Bundle();
                    args.putInt("TicketArg", ticketArg);
                    urlArg = null;
                    ticketArg = -1;
                    ticketListFragment.setArguments(args);
                }
                ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
                ft.addToBackStack(ListFragmentTag);
                ft.setTransition(FragmentTransaction.TRANSIT_NONE);
                ft.commit();
                MyLog.d("backstack initiated");
            } else {  // No host known
                Intent intent = new Intent(getString(R.string.editLoginAction));
                intent.setClass(this, PrefSpecActivity.class);
                MyLog.d(intent);
                doNotFinish = true;
                startActivity(intent);
                MyLog.d("login pref started");
            }
        }
        TracGlobal.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        update_navmenu();
    }

    private void update_navmenu() {
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
        if (!TextUtils.isEmpty(t)) {
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
                if (SysOps.isDebuggable(this) && testDevices != null) {
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
        //MyLog.d("newLoad = " + newLoad);
        //MyLog.d("mService = "+ mService);
        if (mService != null) {
            if (newLoad || mService.getLoginProfile() == null) {
                currentLoginProfile = new LoginProfile(url, userName, passWord, sslHack)
                        .setSslHostNameHack(sslHostNameHack)
                        .setFilterList(filterList)
                        .setSortList(sortList);
                loadTicketList(currentLoginProfile);

            } else {
                profile = mService.getLoginProfile().getProfile();
                loadTicketList(null);
            }
            mService.resetProfileChanged();
        }
    }

    private void loadTicketList(LoginProfile lp) {
        hasTicketsLoadingBar = false;
        if (ListFragmentTag.equals(getTopFragment())) {
            startProgressBar(getString(R.string.getlist, (profile == null ? "" : profile)));
            getTicketListFragment().startLoading();
            hasTicketsLoadingBar = true;
        }
        ticketsLoading = true;
        mService.msgLoadTickets(lp);
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
        final ActionBar ab = getSupportActionBar();

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

        bindService(new Intent(TracStart.this,
                        TracClientService.class).setAction(getServiceAction()),
                this, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(sqlupdateReceiver, new IntentFilter(DB_UPDATED));

        if (getTopFragment() == null && !doNotFinish) {
            finish();
        }
        doNotFinish = false;
        if (adView != null) {
            adView.resume();
        }
        findViewById(R.id.displayList).getViewTreeObserver().addOnGlobalLayoutListener(this);
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
        mService.unregisterDataChangedListener(this);
        if (isFinishing()) {
            MyLog.d("Stopping service");
            stopService(serviceIntent);
            if (isRCVersion()) {
                MyLog.save();
            }
        } else {
            MyLog.d("unbinding from service");
            unbindService(this);
            mService = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sqlupdateReceiver);
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
        saveFragment(savedInstanceState, DetailFragmentTag);
        saveFragment(savedInstanceState, NewFragmentTag);
        saveFragment(savedInstanceState, UpdFragmentTag);
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

    @Override
    protected void onDestroy() {
        MyLog.d("isFinishing = " + isFinishing());
        TracGlobal.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mDrawerLayout.addDrawerListener(toggle);
        super.onDestroy();
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        MyLog.d(frag + " this = " + this);
        super.onAttachFragment(frag);

        if (frag instanceof TicketListFragment && urlArg != null) {
            MyLog.d("ticketListFragment = " + frag);
            MyLog.d("Ticket = " + ticketArg);
            ((TracClientFragment) frag).selectTicket(ticketArg);
            urlArg = null;
            ticketArg = -1;
        }
    }

    @Override
    public void onBackPressed() {
        MyLog.logCall();
        DetailFragment dt = (DetailFragment) getFragment(DetailFragmentTag);

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
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
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
            ret |= ((DetailFragment) getFragment(DetailFragmentTag)).dispatchTouchEvent(ev);
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
        //MyLog.d("showAbout");
        DialogFragment about = new TracShowWebPageDialogFragment();
        Bundle aboutArgs = new Bundle();
        aboutArgs.putString(HELP_FILE, getString(R.string.whatsnewhelpfile));
        aboutArgs.putBoolean(HELP_VERSION, true);
        aboutArgs.putBoolean(HELP_COOKIES, showCookies);
        aboutArgs.putInt(HELP_ZOOM, webzoom);
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
        final Bundle args = new Bundle();
        args.putString(CURRENT_USERNAME, userName);
        newtickFragment.setArguments(args);

        ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_NONE);
        ft.addToBackStack(NewFragmentTag);
        ft.commit();
    }

    private void setFilter(String filterString) {
        MyLog.d(filterString);
        setFilter(parseFilterString(filterString));
    }

    private void setFilter(ArrayList<FilterSpec> filter) {
        MyLog.d(filter.toString());
        String filterString = TextUtils.join("&", filter);
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

        String sortString = TextUtils.join("&", sort);
        storeSortString(sortString);
        sortList = sort;
    }

    private void setReferenceTime() {
        //MyLog.d("setReferenceTime");
        referenceTime = new Date().getTime() - timerCorr;
        mService.removeNotification();
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
            unbindService(this);
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

    private void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
        MyLog.d(newUrl + " " + newUser + " " + newPass + " " + newHack + " " + newHostNameHack + " " + newProfile);
        url = newUrl;
        userName = newUser;
        passWord = newPass;
        sslHack = newHack;
        sslHostNameHack = newHostNameHack;
        profile = newProfile;
        setFilter(getFilterString());
        setSort(getSortString());
        tm = null;


        if (getFragment(ListFragmentTag) == null) {
            doNotFinish = true;
            Fragment frag = new TicketListFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.displayList, frag, ListFragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.addToBackStack(ListFragmentTag);
            ft.commit();
        } else {
            getSupportFragmentManager().popBackStack(ListFragmentTag, 0);
        }
        newDataAdapter(new Tickets()); // empty list
        startListLoader(true);
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
        boolean isTop = (DetailFragmentTag.equals(getTopFragment()));
        MyLog.d("Ticket: " + ticket + " isTop = " + isTop);

        Fragment detailFragment = new DetailFragment();
        final Bundle args = new Bundle();
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

        final Fragment updtickFragment = new UpdateTicketFragment();
        final Bundle args = new Bundle();

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
    public TicketListAdapter getAdapter() {
        //MyLog.d("dataAdapter = " + dataAdapter);
        return dataAdapter;
    }

    @Override
    public void getTicket(final int i, final OnTicketLoadedListener _oc) {
        MyLog.d("i = " + i, new Exception());

        new Thread() {
            @Override
            public void run() {
                Ticket t = Tickets.getTicket(i);
                MyLog.d("i = " + i + " ticket = " + t);
                if (t == null || !t.hasdata()) {
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
        MyLog.d(i);
        startProgressBar(R.string.ophalen);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Ticket t = mService.requestTicket(i);
                MyLog.d(t);
                stopProgressBar();
                if (t != null) {
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
                }
            }
        }).start();
    }

    @Override
    public TracClientService getService() {
        return mService;
    }

    @Override
    public int getNextTicket(int i) {
        return dataAdapter.getNextTicket(i);
    }

    @Override
    public int getPrevTicket(int i) {
        return dataAdapter.getPrevTicket(i);
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

    @Override
    public void updateTicket(final Ticket t, final String action, final String comment, final String veld, final String waarde, final boolean notify, final Map<String, String> modVeld) throws
            Exception {
        final JSONObject velden = t.getVelden();
        MyLog.d("Ticket = " + t + " update: " + action + " '" + comment + "' '" + veld + "' '" + waarde + "' " + modVeld);
        MyLog.d("velden voor = " + velden);
        final int ticknr = t.getTicketnr();

        if (ticknr == -1) {
            throw new IllegalArgumentException(getString(R.string.invtick));
        }
        if (action == null) {
            throw new NullPointerException(getString(R.string.noaction));
        }
        velden.put("action", action);
        if (!TextUtils.isEmpty(waarde) && !TextUtils.isEmpty(veld)) {
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
//				reloadTicketData(new Ticket(newticknr));
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
        new Thread(new Runnable() {

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
                                int dispId = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                                if (dispId != -1) {
                                    filename = c.getString(dispId);
                                }
                                int sizeId = c.getColumnIndex(MediaStore.MediaColumns.SIZE);
                                if (sizeId != -1) {
                                    bytes = c.getInt(sizeId);
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
        }).start();
    }

    @Override
    public void requestTicketCount() {
        MyLog.logCall();
        final int count = getTicketCount();
        mService.sendTicketCount(count, fromUnix(referenceTime));
    }

    @Override
    public void newTicketModel(TicketModel ticketModel) {
        MyLog.logCall();
        tm = ticketModel;
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            MyLog.d(frag);
            if (frag instanceof TracClientFragment) {
                ((TracClientFragment) frag).onTicketModelChanged(tm);
            }
        }
    }

    @Override
    public void loadAborted() {
        notifyTicketListFragment();
    }

    private void notifyTicketListFragment() {
        MyLog.logCall();
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
        //MyLog.logCall();
        final View view = findViewById(android.R.id.content);

        if (view != null) {
            final ActionBar ab = getSupportActionBar();
            final Rect r = new Rect();

            // r will be populated with the coordinates of your view that area still visible.
            view.getWindowVisibleDisplayFrame(r);
            final int viewHeight = view.getRootView().getHeight();
            int visibleHeight = r.bottom - r.top;
            float ratio = (float) viewHeight / (float) visibleHeight;
            if (ratio > 1.7f) { // if more than 100 pixels,
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
        mService = ((TracClientService.TcBinder) service).getService();
        MyLog.d("mConnection mService = " + mService);
        boolean mustLoad = mService.getLoginProfile() == null;
        mService.registerDataChangedListener(this);
        if (mService.isProfileChanged()) {
            mustLoad = true;
            mService.resetProfileChanged();
            String fragName = getTopFragment();
            if (!ListFragmentTag.equals(fragName)) {
                getSupportFragmentManager().popBackStack(ListFragmentTag, 0);
            }
        }
        if (mustLoad) {
            setReferenceTime();
            startListLoader(false);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        MyLog.d("className = " + className);
        mService.unregisterDataChangedListener(this);
        if (isFinishing() && serviceIntent != null) {
            stopService(serviceIntent);
        }
        mService = null;
        serviceIntent = null;
    }

    @Override
    public void onDataChanged() {
        MyLog.logCall();
        notifyTicketListFragment();
    }

    @Override
    public void onFase1Completed(final Tickets tl) {
        MyLog.d(tl);
        if (hasTicketsLoadingBar) {
            stopProgressBar();
            hasTicketsLoadingBar = false;
        }
        ticketsLoading = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataAdapter.clear();
                dataAdapter.addAll(tl);
                //notifyTicketListFragment();
            }
        });
    }

    @Override
    public void onFase2Completed() {
        MyLog.logCall();
        notifyTicketListFragment();
    }
}
