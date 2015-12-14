/*
 * Copyright (C) 2013,2014,2015 Michiel van Loon
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

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore.Images;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;

import static com.mfvl.trac.client.Const.*;


public class TracStart extends Activity implements Handler.Callback, InterFragmentListener, OnBackStackChangedListener, ActivityCompat.OnRequestPermissionsResultCallback {
   
    /*
     * Constanten voor communicatie met de service en fragmenten
     */

    private static final int REQUEST_CODE_CHOOSER = 6384;
    private static final int REQUEST_CODE_WRITE_EXT = 6385;
    private static final String ListFragmentTag = "List_Fragment";
    private static final String LoginFragmentTag = "Login_Fragment";
    private static final String DetailFragmentTag = "Detail_Fragment";
    private static final String NewFragmentTag = "New_Fragment";
    private static final String UpdFragmentTag = "Modify_Fragment";
    private static final String FilterFragmentTag = "Filter_Fragment";
    private static final String SortFragmentTag = "Sort_Fragment";
    static public Handler tracStartHandler = null;
    private final Semaphore loadingActive = new Semaphore(1, true);

    private ArrayList<SortSpec> sortList = null;
    private ArrayList<FilterSpec> filterList = null;
    private String profile = null;
    private LoginProfile currentLoginProfile = null;
    private String url = null;
    private String username = null;
    private String password = null;
    private boolean sslHack = false;
    private boolean sslHostNameHack = false;
    private int timerCorr = 0;
    private boolean debug = false; // disable menuoption at startup
    private OnFileSelectedListener _oc = null;
    private boolean dispAds = true;
    private boolean canWriteSD = false;
    private long referenceTime = 0;
    private String urlArg = null;
    private int ticketArg = -1;
    private boolean doNotFinish = false;
    private TicketModel tm = null;
    private boolean mIsBound = false;
    private boolean mTicketsBound = false;
    private Messenger mMessenger = null;
    private RefreshService mService = null;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ProfileDatabaseHelper pdb = null;
    private Cursor pdbCursor;


    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            tcLog.d("className = " + className + " service = " + service);
            RefreshService.RefreshBinder binder = (RefreshService.RefreshBinder) service;
            mService = binder.getService();
            tcLog.d("mService = " + mService);
            mIsBound = true;
            dispatchMessage(Message.obtain(null, MSG_START_TIMER));
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            tcLog.d("className = " + className);
            mIsBound = false;
//            if (!mTicketsBound) {
//                mService = null;
//            }
        }
    };

    private final ServiceConnection mTicketsConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            tcLog.d("className = " + className + " service = " + service);
            RefreshService.RefreshBinder binder = (RefreshService.RefreshBinder) service;
            mService = binder.getService();
            tcLog.d("mService = " + mService);
            mTicketsBound = true;
            dispatchMessage(Message.obtain(null, MSG_LOAD_TICKETS, currentLoginProfile));
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            tcLog.d("className = " + className);
            mTicketsBound = false;
            if (!mIsBound) {
                mService = null;
            }
        }
    };

    private MyHandlerThread mHandlerThread = null;
    private boolean hasTicketsLoadingBar = false;
    private Boolean ticketsLoading = false;
    private TicketListAdapter dataAdapter = null;
    private ProgressDialog progressBar = null;

    private void dispatchMessage(Message msg) {
        msg.replyTo = mMessenger;
        tcLog.d("mService = " + mService + " msg = " + msg);
        if (mService != null) {
            mService.send(msg);
        }
    }

    private void sendMessageToService(int message) {
        dispatchMessage(Message.obtain(null, message));
    }

    private void sendMessageToService(int message, int value, Object o) {
        dispatchMessage(Message.obtain(null, message, value, 0, o));
    }

    private void sendMessageToService(int message, int value, int msg_back, Object o) {
        dispatchMessage(Message.obtain(null, message, value, msg_back, o));
    }

    private void sendMessageToService(int message, Object value) {
        dispatchMessage(Message.obtain(null, message, value));
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
        launchTrac.putExtra(HELP_FILE, getString(R.string.whatsnewhelpfile));
        launchTrac.putExtra(HELP_VERSION, true);
        startActivity(launchTrac);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tcLog.setContext(this);
        tcLog.d("savedInstanceState = " + savedInstanceState);

        if (DEBUG_MANAGERS) {
            FragmentManager.enableDebugLogging(true);
//            LoaderManager.enableDebugLogging(true);
        }

        TracGlobal.getInstance(getApplicationContext());
        mHandlerThread = new MyHandlerThread("IncomingHandler");
        mHandlerThread.start();
        tracStartHandler = new Handler(mHandlerThread.getLooper(), this);
        mMessenger = new Messenger(tracStartHandler);
//        startService(new Intent(this, RefreshService.class));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
        } else {
            canWriteSD = true;
        }

        TracGlobal.ticketGroupCount = getResources().getInteger(R.integer.ticketGroupCount);
        timerCorr = getResources().getInteger(R.integer.timerCorr);
        setContentView(R.layout.tracstart);
        debug |= TracGlobal.isRCVersion();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        TextView  tv = (TextView)getLayoutInflater().inflate(R.layout.ticket_list,null);
        tv.setText(R.string.changehost);
        mDrawerList.addHeaderView(tv);

        if (savedInstanceState != null) {
            url = savedInstanceState.getString(CURRENT_URL);
            username = savedInstanceState.getString(CURRENT_USERNAME);
            password = savedInstanceState.getString(CURRENT_PASSWORD);
            sslHack = savedInstanceState.getBoolean(CURRENT_SSLHACK, false);
            sslHostNameHack = savedInstanceState.getBoolean(CURRENT_SSLHOSTNAMEHACK, false);
            filterList = (ArrayList<FilterSpec>) savedInstanceState.getSerializable(FILTERLISTNAME);
            sortList = (ArrayList<SortSpec>) savedInstanceState.getSerializable(SORTLISTNAME);
            dispAds = savedInstanceState.getBoolean(ADMOB, true);
        } else {
            url = TracGlobal.getUrl();
            username = TracGlobal.getUsername();
            password = TracGlobal.getPassword();
            sslHack = TracGlobal.getSslHack();
            sslHostNameHack = TracGlobal.getSslHostNameHack();
            profile = TracGlobal.getProfile();
            setFilter(TracGlobal.getFilterString());
            setSort(TracGlobal.getSortString());

            // only at first start
            if (TracGlobal.getFirstRun()) {
                final Intent launchTrac = new Intent(this, TracShowWebPage.class);
                launchTrac.putExtra(HELP_FILE, getString(R.string.whatsnewhelpfile));
                launchTrac.putExtra(HELP_VERSION, false);
                startActivity(launchTrac);
            }
            dispAds = !getIntent().hasExtra(ADMOB) || getIntent().getBooleanExtra(ADMOB, true);
            if (getIntent().hasExtra(INTENT_URL)) {
                urlArg = getIntent().getStringExtra(INTENT_URL);
                ticketArg = (int) getIntent().getLongExtra(INTENT_TICKET, -1);
            }
        }

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
                    showAlertBox(R.string.wrongdb, R.string.wrongdbtext1, url + getString(R.string.wrongdbtext2) + urlArg + getString(R.string.wrongdbtext3));
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
                currentLoginProfile = lp;
            }
        }

        newDataAdapter(new Tickets()); // empty list
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
            startListLoader(true);
        } else {
            final FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (url != null && url.length() > 0) {
                startListLoader(true);

                final TicketListFragment ticketListFragment = new TicketListFragment();

                if (urlArg != null) {
                    tcLog.d("select Ticket = " + ticketArg);
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

        bindService(new Intent(this, RefreshService.class).setAction(ACTION_START_TIMER), mConnection, Context.BIND_AUTO_CREATE);
        setReferenceTime();
    }

    private void startListLoader(boolean newLoad) {
        tcLog.d("newLoad = " + newLoad);
        if (newLoad) {
            tracStartHandler.obtainMessage(MSG_START_LISTLOADER, null).sendToTarget();
        } else {
            dispatchMessage(Message.obtain(null, MSG_REFRESH_LIST));
        }
    }

    private void showAlertBox(final int titleres, final int message, final String addit) {
        tcLog.d("titleres = " + titleres + " : " + getString(titleres));
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String s = (message != 0 ? getResources().getString(message) + (addit != null ? ": " + addit : "") : addit);
                    final AlertDialog ad = new AlertDialog.Builder(TracStart.this)
                            .setTitle(titleres)
                            .setMessage(s)
                            .setPositiveButton(R.string.oktext, null)
                            .create();
                    tracStartHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tcLog.d("dismiss");
                            ad.dismiss();
                        }
                    }, 7500);
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
            } catch (final Exception ignored) {
            }
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

    private void shouldDisplayHomeUp() {
        // Enable Up button only if there are entries in the back stack
        tcLog.d("entry count = " + getFragmentManager().getBackStackEntryCount());
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
    protected void onStart() {
        super.onStart();
        tcLog.logCall();
        // Set the adapter for the list view
        pdbCursor = pdb.getProfiles(false);
        final String[] columns = new String[]{"name"};
        final int[] to = new int[]{android.R.id.text1};
        mDrawerList.setAdapter(new SimpleCursorAdapter(this, R.layout.drawer_list_item, pdbCursor, columns, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                tcLog.d("parent = " + parent + " view = " + view + " position = " + position);
                String newProfile = ((TextView) view).getText().toString();
                tcLog.d(newProfile);
                LoginProfile lp = pdb.getProfile(newProfile);
                tcLog.d(lp);
                if (lp != null) {
                    onLogin(lp.getUrl(), lp.getUsername(), lp.getPassword(), lp.getSslHack(), lp.getSslHostNameHack(), newProfile);
                }
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        tcLog.logCall();
        pdbCursor.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tcLog.logCall();
        stopProgressBar();
        /* save logfile when exiting */
        tcLog.d("isFinishing = " + isFinishing());
        if (isFinishing() && TracGlobal.isRCVersion()) {
            tcLog.save();
        }
    }

    @Override
    public void listViewCreated() {
        tcLog.d("ticketsLoading = " + ticketsLoading + " hasTicketsLoadingBar = " + hasTicketsLoadingBar);
        synchronized (this) {
            if (ticketsLoading) {
                if (!hasTicketsLoadingBar) {
                    startProgressBar(getString(R.string.getlist) + (profile == null ? "" : "\n" + profile));
                    hasTicketsLoadingBar = true;
                }
                getTicketListFragment().startLoading();
            }
        }
    }

    @Override
    public void onChooserSelected(OnFileSelectedListener oc) {
        tcLog.logCall();
        // save callback
        _oc = oc;
        // Use the GET_CONTENT intent from the utility class
        final Intent target = new Intent(Intent.ACTION_GET_CONTENT);

        target.setType("*/*");
        target.addCategory(Intent.CATEGORY_OPENABLE);
        // Create the chooser Intent
        startActivityForResult(Intent.createChooser(target, getString(R.string.chooser_title)), REQUEST_CODE_CHOOSER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        tcLog.d("requestcode = " + requestCode + " intent = " + data);
        switch (requestCode) {
            case REQUEST_CODE_CHOOSER:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        tcLog.d("uri = " + uri);
                        if (_oc != null) {
                            _oc.onFileSelected(uri);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onAttachFragment(final Fragment frag) {
        tcLog.d(frag + " this = " + this);
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

    private TracLoginFragment newLoginFrag() {
        final TracLoginFragment tracLoginFragment = new TracLoginFragment();
        final Bundle args = makeArgs();
        args.putString(CURRENT_URL, url);
        args.putString(CURRENT_USERNAME, username);
        args.putString(CURRENT_PASSWORD, password);
        args.putBoolean(CURRENT_SSLHACK, sslHack);
        args.putBoolean(CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
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

    public void setActionProvider(Menu menu, int resid) {
        final MenuItem item = menu.findItem(resid);
        ShareActionProvider mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        if (mShareActionProvider == null) {
            tcLog.d("create new shareActionProvider item = " + item);
            mShareActionProvider = new ShareActionProvider(this);
            item.setActionProvider(mShareActionProvider);
        }
        mShareActionProvider.setShareHistoryFileName("custom_share_history" + resid + ".xml");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        tcLog.logCall();
        getMenuInflater().inflate(R.menu.tracstartmenu, menu);
        setActionProvider(menu, R.id.debug);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        tcLog.logCall();
        super.onDestroy();
        if (isFinishing()) {
            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }
            if (mTicketsBound) {
                unbindService(mTicketsConnection);
                mTicketsBound = false;
            }
            mHandlerThread.quit();
        }
    }

    private void onFilterSelected(ArrayList<FilterSpec> filterList) {
        tcLog.d("filterList = " + filterList);

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final FilterFragment filterFragment = new FilterFragment();

        final Bundle args = makeArgs();
        args.putSerializable(FILTERLISTNAME, filterList);
        filterFragment.setArguments(args);

        ft.replace(R.id.displayList, filterFragment, FilterFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(FilterFragmentTag);
        ft.commit();
    }

    @Override
    public void onLogin(String newUrl, String newUser, String newPass, boolean newHack, boolean newHostNameHack, String newProfile) {
        tcLog.d(newUrl + " " + newUser + " " + newPass + " " + newHack + " " + newHostNameHack + " " + newProfile);
        url = newUrl;
        username = newUser;
        password = newPass;
        sslHack = newHack;
        sslHostNameHack = newHostNameHack;
        profile = newProfile;
        TicketListFragment ticketListFragment = (TicketListFragment) getFragment(ListFragmentTag);

        if (ticketListFragment == null) {
            getFragmentManager().popBackStackImmediate(LoginFragmentTag,FragmentManager.POP_BACK_STACK_INCLUSIVE );
            ticketListFragment = new TicketListFragment();
            doNotFinish = true;
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.displayList, ticketListFragment, ListFragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(ListFragmentTag);
            ft.commit();
        } else {
            getFragmentManager().popBackStackImmediate(ListFragmentTag,0);
        }
//        refreshOverview();
        dataAdapter.clear();
        startListLoader(true);
    }

    private void onNewTicket() {
        tcLog.logCall();

        final NewTicketFragment newtickFragment = new NewTicketFragment();
        // tcLog.d("newTickFragment =" +  newtickFragment.toString());
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Bundle args = makeArgs();
        args.putString(CURRENT_USERNAME, username);
        newtickFragment.setArguments(args);

        ft.replace(R.id.displayList, newtickFragment, NewFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(NewFragmentTag);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        tcLog.d("item=" + item.getTitle());

        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() == 1) {
                    mDrawerLayout.openDrawer(mDrawerList);
                } else {
                    return false;
                }
                break;

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
        final MenuItem itemDebug = menu.findItem(R.id.debug);

        itemDebug.setVisible(debug).setEnabled(debug);
        if (debug) {
            Intent i = shareDebug();

            ShareActionProvider debugShare = (ShareActionProvider) itemDebug.getActionProvider();
            tcLog.d("item = " + itemDebug + " " + debugShare + " " + i);
            if (debugShare != null && i != null) {
                debugShare.setShareIntent(i);
            }
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("Admob", dispAds);
        savedInstanceState.putSerializable(SORTLISTNAME, sortList);
        savedInstanceState.putSerializable(FILTERLISTNAME, filterList);
        savedInstanceState.putString(CURRENT_URL, url);
        savedInstanceState.putString(CURRENT_USERNAME, username);
        savedInstanceState.putString(CURRENT_PASSWORD, password);
        savedInstanceState.putBoolean(CURRENT_SSLHACK, sslHack);
        savedInstanceState.putBoolean(CURRENT_SSLHOSTNAMEHACK, sslHostNameHack);
        saveFragment(savedInstanceState, ListFragmentTag);
        saveFragment(savedInstanceState, LoginFragmentTag);
        saveFragment(savedInstanceState, DetailFragmentTag);
        saveFragment(savedInstanceState, NewFragmentTag);
        saveFragment(savedInstanceState, UpdFragmentTag);
        saveFragment(savedInstanceState, FilterFragmentTag);
        saveFragment(savedInstanceState, SortFragmentTag);
        tcLog.d("savedInstanceState = " + savedInstanceState);
    }

    private void onSortSelected(ArrayList<SortSpec> sortList) {
        tcLog.logCall();
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final SortFragment sortFragment = new SortFragment();

        final Bundle args = makeArgs();
        args.putSerializable(SORTLISTNAME, sortList);
        sortFragment.setArguments(args);

        ft.replace(R.id.displayList, sortFragment, SortFragmentTag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(SortFragmentTag);
        ft.commit();
    }

    @Override
    public void onTicketSelected(Ticket ticket) {
        boolean isTop = (DetailFragmentTag.equals(getTopFragment()));
        tcLog.d("Ticket: " + ticket + " isTop = " + isTop);

        DetailFragment detailFragment = new DetailFragment();
        final Bundle args = makeArgs();
        args.putInt(CURRENT_TICKET, ticket.getTicketnr());
        detailFragment.setArguments(args);

        //		tcLog.d("detailFragment =" + detailFragment.toString());
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
        tcLog.d("ticket = " + ticket);

        final UpdateTicketFragment updtickFragment = new UpdateTicketFragment();
        final Bundle args = makeArgs();

        args.putInt(CURRENT_TICKET, ticket.getTicketnr());
        updtickFragment.setArguments(args);
//		tcLog.d("updtickFragment = " + updtickFragment.toString());
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
//            tm = TicketModel.getInstance();
            tm = mService.getTicketModel();
            stopProgressBar();
        }
        return tm;
    }

    private Bundle makeArgs() {
        return new Bundle();
    }

    private void setFilter(ArrayList<FilterSpec> filter) {
        tcLog.d(filter.toString());
        String filterString = TracGlobal.joinList(filter.toArray(), "&");
        TracGlobal.storeFilterString(filterString);
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

        String sortString = TracGlobal.joinList(sort.toArray(), "&");
        TracGlobal.storeSortString(sortString);
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
        tcLog.d("dispAds = " + dispAds);
        return dispAds;
    }

    @Override
    public void setDispAds(boolean b) {
        dispAds = b;
    }

    @Override
    public void refreshOverview() {
        tcLog.logCall();
//		dataAdapter.notifyDataSetChanged();
        startListLoader(false);
        setReferenceTime();
    }

    public Intent shareList() {
        tcLog.logCall();
        String lijst = "";

        if (dataAdapter != null) {
            for (Ticket t : dataAdapter.getTicketList()) {
                try {
                    lijst += t.getTicketnr() + ";" + t.getString("status") + ";" + t.getString("summary") + "\r\n";
                } catch (final Exception e) {
                    tcLog.e("exception", e);
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
        // tcLog.d("enableDebug");
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
        return (TicketListFragment) getFragment(ListFragmentTag);
    }

    private Fragment getFragment(final String tag) {
        return getFragmentManager().findFragmentByTag(tag);
    }

    private void setReferenceTime() {
        // tcLog.d("setReferenceTime");
        referenceTime = System.currentTimeMillis() - timerCorr;
        sendMessageToService(MSG_REMOVE_NOTIFICATION);
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

    private void startProgressBar(String message) {
        tcLog.d(message);
        try {
            tracStartHandler.obtainMessage(MSG_START_PROGRESSBAR, message).sendToTarget();
        } catch (NullPointerException e) {
            tcLog.e("NullPointerException", e);
        }
    }

    public void startProgressBar(int resid) {
        startProgressBar(getString(resid));
    }

    public void stopProgressBar() {
        tcLog.logCall();
        try {
            tracStartHandler.obtainMessage(MSG_STOP_PROGRESSBAR).sendToTarget();
        } catch (Exception e) {
            tcLog.e("Exception", e);
        }
    }

    private void newDataAdapter(Tickets tl) {
        tcLog.logCall();
        dataAdapter = new TicketListAdapter(this, R.layout.ticket_list, tl);
        dataAdapter.getFilter().filter(null);
        dataAdapter.setNotifyOnChange(true);
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
        tcLog.d("i = " + i + " semaphore = " + loadingActive.availablePermits());
        loadingActive.acquireUninterruptibly();
        loadingActive.release();

        Ticket t = dataAdapter.getTicket(i);
        tcLog.d("i = " + i + " ticket = " + t);
        if (t != null) {
            if (!t.hasdata()) {
                refreshTicket(i);
            }
        }
        return t;
    }

    @Override
    public void refreshTicket(final int i) {
        sendMessageToService(MSG_SEND_TICKETS, i, MSG_DISPLAY_TICKET, null);
    }

    public void updateTicket(final Ticket t, final String action, final String comment, final String veld, final String waarde, final boolean notify, final Map<String, String> modVeld) throws Exception {
        final JSONObject velden = t.getVelden();
        tcLog.d("Ticket = " + t);
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
            for (Entry<String, String> e : modVeld.entrySet()) {
//				tcLog.d(e.toString());
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
                    TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack, username, password);
                    JSONArray retTick = tracClient.updateTicket(ticknr, cmt, velden, notify);
                    t.setFields(retTick.getJSONObject(3));
                    if (modVeld != null) {
                        modVeld.clear();
                    }
                    tcLog.d("retTicket = " + retTick);
                } catch (final Exception e) {
                    tcLog.e("JSONRPCException", e);
                    showAlertBox(R.string.upderr, R.string.storerrdesc, e.getMessage());
                }
            }
        };
        updateThread.start();
        updateThread.join();
    }

    public int createTicket(Ticket t, boolean notify) throws Exception {
        int ticknr = t.getTicketnr();
        final JSONObject velden = t.getVelden();

        if (ticknr != -1) {
            throw new IllegalArgumentException("Call create ticket not -1");
        }
        tcLog.i("create: " + velden.toString());
        final String s = velden.getString("summary");
        final String d = velden.getString("description");

        velden.remove("summary");
        velden.remove("description");

        try {
            TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack, username, password);
            final int newticknr = tracClient.createTicket(s, d, velden, notify);
            if (newticknr != -1) {
//				reloadTicketData(new Ticket(newticknr));
                refreshTicket(newticknr);
                return newticknr;
            } else {
                showAlertBox(R.string.storerr, R.string.noticketUnk, "");
                return -1;
            }
        } catch (final Exception e) {
            tcLog.d("Exception during create", e);
            showAlertBox(R.string.storerr, R.string.storerrdesc, e.getMessage());
            return -1;
        }
    }

    @Override
    public void getAttachment(final Ticket ticket, final String filename, final onAttachmentCompleteListener oc) {
        tcLog.i(ticket.toString() + " " + filename);
        final int _ticknr = ticket.getTicketnr();
        new Thread() {
            @Override
            public void run() {
//                available.acquireUninterruptibly();
                if (oc != null) {
                    try {
                        TracHttpClient tracClient = new TracHttpClient(url, sslHack, sslHostNameHack, username, password);
                        oc.onComplete(tracClient.getAttachment(_ticknr, filename));
                    } catch (final Exception e) {
                        tcLog.e("Exception during getAttachment", e);
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
    public void addAttachment(final Ticket ticket, final Uri uri, final onTicketCompleteListener oc) {
        tcLog.i(ticket.toString() + " " + uri);
        final int _ticknr = ticket.getTicketnr();
        String filename = null;
        int bytes = 0;
        if (uri != null) {
            if (uri.toString().startsWith("file:")) {
                filename = uri.getPath();
            } else {
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c != null && c.moveToFirst()) {
//                    tcLog.d("ColumnNames = "+Arrays.asList(c.getColumnNames()));
                    int id = c.getColumnIndex(Images.Media.DISPLAY_NAME);
                    if (id != -1) {
                        filename = c.getString(id);
                    }
                    id = c.getColumnIndex(Images.Media.SIZE);
                    if (id != -1) {
                        bytes = c.getInt(id);
                    }
                    c.close();
                }
            }
            tcLog.d("filename = " + filename);
            tcLog.d("bytes = " + bytes);
            final File file = new File(filename == null ? uri.getPath() : filename);
            tcLog.d("file = " + file);
            tcLog.d("File path: " + file.getAbsolutePath());
            tcLog.d("file.getName = " + file.getName());
            if (bytes == 0) {
                bytes = (int) file.length();
                tcLog.d("bytes = " + bytes);
            }
            final int maxBytes = (bytes > 0 ? bytes : 120000);   // 6 fold because of Base64
            final byte[] data = new byte[maxBytes];

            try {
                final InputStream is = getContentResolver().openInputStream(uri);
                String b64 = "";
                for (int nbytes = is.read(data); nbytes >= 0; nbytes = is.read(data)) {
                    b64 += Base64.encodeToString(data, 0, nbytes, Base64.DEFAULT);
                }
                is.close();

                new TracHttpClient(url, sslHack, sslHostNameHack, username, password)
                        .putAttachment(_ticknr,file.getName(),b64);

            } catch (final FileNotFoundException e) {
                tcLog.i("Exception", e);
                showAlertBox(R.string.warning, R.string.notfound, filename);
            } catch (final Exception e) {
                tcLog.i("Exception", e);
                showAlertBox(R.string.warning, R.string.failed, filename);
            } finally {
                oc.onComplete(ticket);
            }
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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        tcLog.d("requestCode = " + requestCode + " permissions = " + Arrays.asList(permissions) + " grantResults = " + Arrays.asList(grantResults));
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXT: {
                // If request is cancelled, the result arrays are empty.
                canWriteSD = (grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            }
        }
    }

    public boolean getCanWriteSD() {
        return canWriteSD;
    }

    @Override
    @SuppressWarnings({"InlinedAPI", "unchecked"})
    public boolean handleMessage(Message msg) {
        tcLog.d("msg = " + msg);
        switch (msg.what) {
            case MSG_REQUEST_TICKET_COUNT:
                if (!LoginFragmentTag.equals(getTopFragment())) {
                    final int count = getTicketCount();
                    sendMessageToService(MSG_SEND_TICKET_COUNT, count, ISO8601.fromUnix(referenceTime));
                }
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
                    tcLog.d("handleMessage msg = STOP_PROGRESSBAR "+progressBar+" "+TracStart.this.isFinishing());
                    if (progressBar != null) {
                        if (!TracStart.this.isFinishing()) {
                            progressBar.dismiss();
                        }
                        progressBar = null;
                    }
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
                showAlertBox(msg.arg1, msg.arg2, (String) msg.obj);
                break;

            case MSG_DISPLAY_TICKET:
                final Ticket t = (Ticket) msg.obj;
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

            case MSG_GET_PERMISSIONS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(TracStart.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_WRITE_EXT);
                }
                break;

            case MSG_START_LISTLOADER:
                currentLoginProfile = new LoginProfile(url, username, password, sslHack)
                        .setSslHostNameHack(sslHostNameHack)
                        .setFilterList(filterList)
                        .setSortList(sortList);

                hasTicketsLoadingBar = false;
                // Returns a new CursorLoader
                try {
                    getTicketListFragment().startLoading();
                } catch (Exception e) {
                    tcLog.e("LISTLOADER cannot contact TicketListFragment");
                }
                if (ListFragmentTag.equals(getTopFragment())) {
                    startProgressBar(getString(R.string.getlist) + (profile == null ? "" : "\n" + profile));
                    hasTicketsLoadingBar = true;
                }
                loadingActive.acquireUninterruptibly();
                ticketsLoading = true;

                if (mService == null) {
                    bindService(new Intent(TracStart.this, RefreshService.class).setAction(ACTION_LOAD_TICKETS), mTicketsConnection, Context.BIND_AUTO_CREATE);
                } else {
                    dispatchMessage(Message.obtain(null, MSG_LOAD_TICKETS, currentLoginProfile));
                }
                break;

            case MSG_REFRESH_LIST:
                dispatchMessage(Message.obtain(null, MSG_LOAD_TICKETS, null));
                break;

            case MSG_LOAD_FASE1_FINISHED:
                final Tickets tl = (Tickets) msg.obj;
                tcLog.d("Tickets = " + tl);
                synchronized (this) {
                    if (hasTicketsLoadingBar) {
                        stopProgressBar();
                        hasTicketsLoadingBar = false;
                    }
                    ticketsLoading = false;
                }
                TracStart.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataAdapter.clear();
                        dataAdapter.addAll(tl);
                        if (loadingActive.availablePermits() == 0) {
                            loadingActive.release();
                        }
						notifyTicketListFragment();
                    }
                });
                break;

            case MSG_DATA_CHANGED:
                TracStart.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
						notifyTicketListFragment();
                    }
                });
                break;

            case MSG_LOAD_FASE2_FINISHED:
                TracStart.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTicketsBound) {
                            unbindService(mTicketsConnection);
                            mTicketsBound = false;
                        }
						notifyTicketListFragment();
                    }
                });
                break;

            default:
                return false;
        }
        return true;
    }
	
	private void notifyTicketListFragment() {
		try {
			getTicketListFragment().dataHasChanged();
		} catch (Exception e) {
			tcLog.e("Cannot contact TicketListFragment");
		}
	}
}