package com.mfvl.trac.client;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import com.mfvl.trac.client.util.tcLog;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class TracTitlescreenActivity extends ActionBarActivity {
	private boolean exitaftercall = false;
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			tcLog.i(this.getClass().getName(), "onCreate savedInstanceState = " + savedInstanceState);
			super.onCreate(savedInstanceState);

			if (savedInstanceState != null) {
				exitaftercall = savedInstanceState.getBoolean("exitflag", false);
			}
			if (exitaftercall) {
				finish();
			}
			setContentView(R.layout.activity_titlescreen);

			final View controlsView = findViewById(R.id.fullscreen_content_controls);
			final View contentView = findViewById(R.id.fullscreen_content);

			// Set up an instance of SystemUiHider to control the system UI for
			// this activity.
			mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
			mSystemUiHider.setup();
			mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
				// Cached values.
				int mControlsHeight;
				int mShortAnimTime;

				@Override
				@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
				public void onVisibilityChange(boolean visible) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
						// If the ViewPropertyAnimator API is available
						// (Honeycomb MR2 and later), use it to animate the
						// in-layout UI controls at the bottom of the
						// screen.
						if (mControlsHeight == 0) {
							mControlsHeight = controlsView.getHeight();
						}
						if (mShortAnimTime == 0) {
							mShortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
						}
						controlsView.animate().translationY(visible ? 0 : mControlsHeight).setDuration(mShortAnimTime);
					} else {
						// If the ViewPropertyAnimator APIs aren't
						// available, simply show or hide the in-layout UI
						// controls.
						controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
					}

					if (visible && AUTO_HIDE) {
						// Schedule a hide().
						delayedHide(AUTO_HIDE_DELAY_MILLIS);
					}
				}
			});

			// Set up the user interaction to manually show or hide the system
			// UI.
			contentView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (TOGGLE_ON_CLICK) {
						mSystemUiHider.toggle();
					} else {
						mSystemUiHider.show();
					}
				}
			});

			final String versie = Credentials.buildVersion(this, true);
			final TextView tv = (TextView) findViewById(R.id.version_content);
			tv.setText(versie);

			// Upon interacting with UI controls, delay any scheduled hide()
			// operations to prevent the jarring behavior of controls going away
			// while interacting with the UI.
		} catch (final Exception e) {
			Toast.makeText(this, "crash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		tcLog.i(this.getClass().getName(), "onPostCreate");
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		if (savedInstanceState != null) {
			exitaftercall = savedInstanceState.getBoolean("exitflag", false);
		}
		if (exitaftercall) {
			finish();
		}
		delayedHide(100);
		final Handler handler = new Handler();
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						exitaftercall = true;
						final Intent launchTrac = new Intent(getApplicationContext(), TracStart.class);
						launchTrac.putExtra("AdMob", true);
						startActivity(launchTrac);
					}
				});
			}
		}, 3000);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	@Override
	public void onPause() {
		tcLog.i(this.getClass().getName(), "onPause");
		super.onPause();
	}

	@Override
	public void onResume() {
		tcLog.i(this.getClass().getName(), "onResume");
		super.onResume();
		if (exitaftercall) {
			finish();
		}
	}

	@Override
	public void onStart() {
		tcLog.i(this.getClass().getName(), "onStart");
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
		if (exitaftercall) {
			finish();
		}
	}

	@Override
	public void onStop() {
		tcLog.i(this.getClass().getName(), "onStop");
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	public void onDestroy() {
		tcLog.i(this.getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.i(this.getClass().getName(), "onSaveInstanceState");
		savedState.putBoolean("exitflag", exitaftercall);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedState) {
		tcLog.i(this.getClass().getName(), "onRestoreInstanceState savedState = " + savedState);
		if (savedState != null) {
			exitaftercall = savedState.getBoolean("exitflag", false);
		}
		if (exitaftercall) {
			finish();
		}
	}
}
