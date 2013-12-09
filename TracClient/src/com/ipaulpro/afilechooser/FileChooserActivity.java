/* 
 * Copyright (C) 2013 Paul Burke
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

package com.ipaulpro.afilechooser;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mfvl.trac.client.R;

/**
 * Main Activity that handles the FileListFragments
 * 
 * @version 2013-06-25
 * 
 * @author paulburke (ipaulpro)
 * 
 */
public class FileChooserActivity extends ActionBarActivity implements OnBackStackChangedListener {

	public static final String PATH = "path";
	public static final String EXTERNAL_BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

	// private static final boolean HAS_ACTIONBAR = Build.VERSION.SDK_INT >=
	// Build.VERSION_CODES.HONEYCOMB;
	private static final boolean HAS_ACTIONBAR = true;

	private FragmentManager mFragmentManager;
	private final BroadcastReceiver mStorageListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, R.string.storage_removed, Toast.LENGTH_LONG).show();
			finishWithResult(null);
		}
	};

	private String mPath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chooser);

		mFragmentManager = getSupportFragmentManager();
		mFragmentManager.addOnBackStackChangedListener(this);

		if (savedInstanceState == null) {
			mPath = EXTERNAL_BASE_PATH;
			addFragment();
		} else {
			mPath = savedInstanceState.getString(PATH);
		}

		setTitle(mPath);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterStorageListener();
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerStorageListener();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(PATH, mPath);
	}

	@SuppressLint("NewApi")
	@Override
	public void onBackStackChanged() {

		final int count = mFragmentManager.getBackStackEntryCount();
		if (count > 0) {
			final BackStackEntry fragment = mFragmentManager.getBackStackEntryAt(count - 1);
			mPath = fragment.getName();
		} else {
			mPath = EXTERNAL_BASE_PATH;
		}

		setTitle(mPath);
		if (HAS_ACTIONBAR) {
			invalidateOptionsMenu();
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (HAS_ACTIONBAR) {
			final boolean hasBackStack = mFragmentManager.getBackStackEntryCount() > 0;

			final ActionBar actionBar = getSupportActionBar();
			actionBar.setDisplayHomeAsUpEnabled(hasBackStack);
			// actionBar.setHomeButtonEnabled(hasBackStack);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			mFragmentManager.popBackStack();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Add the initial Fragment with given path.
	 */
	private void addFragment() {
		final FileListFragment fragment = FileListFragment.newInstance(mPath);
		mFragmentManager.beginTransaction().add(R.id.explorer_fragment, fragment).commit();
	}

	/**
	 * "Replace" the existing Fragment with a new one using given path. We're
	 * really adding a Fragment to the back stack.
	 * 
	 * @param file
	 *            The file (directory) to display.
	 */
	private void replaceFragment(File file) {
		mPath = file.getAbsolutePath();

		final FileListFragment fragment = FileListFragment.newInstance(mPath);
		mFragmentManager.beginTransaction().replace(R.id.explorer_fragment, fragment)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).addToBackStack(mPath).commit();
	}

	/**
	 * Finish this Activity with a result code and URI of the selected file.
	 * 
	 * @param file
	 *            The file selected.
	 */
	private void finishWithResult(File file) {
		if (file != null) {
			final Uri uri = Uri.fromFile(file);
			setResult(RESULT_OK, new Intent().setData(uri));
			finish();
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	/**
	 * Called when the user selects a File
	 * 
	 * @param file
	 *            The file that was selected
	 */
	protected void onFileSelected(File file) {
		if (file != null) {
			if (file.isDirectory()) {
				replaceFragment(file);
			} else {
				finishWithResult(file);
			}
		} else {
			Toast.makeText(FileChooserActivity.this, R.string.error_selecting_file, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Register the external storage BroadcastReceiver.
	 */
	private void registerStorageListener() {
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(mStorageListener, filter);
	}

	/**
	 * Unregister the external storage BroadcastReceiver.
	 */
	private void unregisterStorageListener() {
		unregisterReceiver(mStorageListener);
	}
}