package com.mfvl.trac.client;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdRequest;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.mfvl.trac.client.util.Credentials;
import com.mfvl.trac.client.util.tcLog;

public class TracClientFragment extends Fragment {
	public Ticket _ticket = null;
	public String _url = null;
	public String _username = null;
	public String _password = null;
	public boolean _sslHack = false;
	public boolean _sslHostNameHack = false;
	public TracStart context;
	private AdView adView = null;
	public InterFragmentListener listener = null;
	private EasyTracker tracker;
	private boolean adsVisible = true;
	private int padTop;
	private int padRight;
	private int padBot;
	private int padLeft;
	private String adUnitId;
	private String[] testDevices;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		tcLog.d(getClass().getName() + ".super", "onAttach ");
		context = (TracStart) activity;
		listener = context;
		final Bundle args = getArguments();
		if (args != null) {
			_url = args.getString(Const.CURRENT_URL);
			_username = args.getString(Const.CURRENT_USERNAME);
			_password = args.getString(Const.CURRENT_PASSWORD);
			_sslHack = args.getBoolean("sslHack", false);
			_sslHostNameHack = args.getBoolean("sslHostNameHack", false);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tcLog.d(getClass().getName() + ".super", "onCreate savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		if (savedInstanceState != null) {
			_url = savedInstanceState.getString("currentURL");
			_username = savedInstanceState.getString("currentUsername");
			_password = savedInstanceState.getString("currentPassword");
			_sslHack = savedInstanceState.getBoolean("sslHack", false);
			_sslHostNameHack = savedInstanceState.getBoolean("sslHostNameHack", false);
		}
		Bundle aBundle;
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			aBundle=ai.metaData;
			if (aBundle == null) {
				listener.setDispAds(false);
			} else {
				adUnitId=aBundle.getString("com.mfvl.trac.client.adUnitId");
				String t=aBundle.getString("com.mfvl.trac.client.testDevices");
				try {
					testDevices=t.split("\\,");
				} catch (final IllegalArgumentException e) {
					testDevices = new String[1];
					testDevices[0] = t;
				}
			}
		} catch (Exception e) {
			aBundle = null;
			listener.setDispAds(false);
			adUnitId="";
			testDevices = new String[1];
			testDevices[0]="";
		}
//		tcLog.d(getClass().getName() + ".super", "onCreate adUnitId = "+adUnitId);
//		tcLog.d(getClass().getName() + ".super", "onCreate testDevices = "+Arrays.asList(testDevices));
	}
	
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		tcLog.d(getClass().getName() + ".super", "onViewCreated");
		final View activityRootView = view.findViewById(R.id.updateTop);
		final View aboveView = view.findViewById(R.id.aboveAdBlock);
		final LinearLayout ll = (LinearLayout) view.findViewById(R.id.adBlock);

		if (listener != null && listener.dispAds()) {
			if (ll != null) {
				final AdView adView = new AdView(context);
				adView.setAdUnitId(adUnitId);
				adView.setAdSize(AdSize.BANNER);

				final AdRequest.Builder arb = new AdRequest.Builder();
				if (adView != null && arb != null) {
					if (Credentials.isDebuggable(context)) {
						arb.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
						for (String t:testDevices) {
							tcLog.d(getClass().getName() + ".super", "onViewCreated testDevice = "+t);
							arb.addTestDevice(t);
						}
					}
					arb.setGender(AdRequest.GENDER_UNKNOWN);
					final AdRequest adRequest = arb.build();

					if (adRequest != null) {
						adView.loadAd(adRequest);
						adView.setLayoutParams(ll.getLayoutParams());
						// tcLog.d(getClass().getName(), "adView size = " +
						// adView.getHeight());
						ll.addView(adView);
					}
				}
				if (activityRootView != null && aboveView != null) {
					padTop = aboveView.getPaddingTop();
					padRight = aboveView.getPaddingRight();
					padBot = aboveView.getPaddingBottom();
					padLeft = aboveView.getPaddingLeft();
					adsVisible = true;
					activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							Rect r = new Rect();
							//r will be populated with the coordinates of your view that area still visible.
							activityRootView.getWindowVisibleDisplayFrame(r);

							int heightDiff = activityRootView.getRootView().getHeight() - (r.bottom - r.top);
//							tcLog.d(getClass().getName(),"OnGlobalLayout heightDiff = "+ heightDiff);
//							tcLog.d(getClass().getName(),"OnGlobalLayout r = "+ r);
							if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
								if (adsVisible) {
									ll.setVisibility(View.GONE);
									aboveView.setPadding(padLeft,padTop,padRight,0);
									adsVisible = false;
								}
							} else {
								if (! adsVisible) {
									ll.setVisibility(View.VISIBLE);
									aboveView.setPadding(padLeft,padTop,padRight,padBot);
									adsVisible = true;
								}
							}
						}
					}); 
				}
			}
		} else {
			final View above = getView().findViewById(R.id.aboveAdBlock);
			if (above != null) {
				above.setPadding(0, 0, 0, 0);
			}
		}

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		tcLog.d(getClass().getName() + ".super", "onActivityCreated savedInstanceState = "
				+ (savedInstanceState == null ? "null" : "not null"));
		tracker = EasyTracker.getInstance(context);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		tcLog.d(getClass().getName() + ".super", "onSaveInstanceState");
		savedState.putString("currentURL", _url);
		savedState.putString("currentUsername", _username);
		savedState.putString("currentPassword", _password);
		savedState.putBoolean("sslHack", _sslHack);
		savedState.putBoolean("sslHostNameHack", _sslHostNameHack);
		tcLog.d(this.getClass().getName() + ".super", "onSaveInstanceState = " + savedState);
	}

	@Override
	public void onStart() {
		tcLog.d(getClass().getName() + ".super", "onStart");
		super.onStart();
		tracker.activityStart(context);
		tracker.set(Fields.SCREEN_NAME, getClass().getSimpleName());
		tracker.send(MapBuilder.createAppView().build());
	}

	@Override
	public void onStop() {
		tcLog.d(getClass().getName() + ".super", "onStop");
		super.onStop();
		tracker.activityStop(context);
	}

	@Override
	public void onPause() {
		if (adView != null) {
			adView.pause();
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (adView != null) {
			adView.resume();
		}
	}

	@Override
	public void onDestroy() {
		tcLog.d(getClass().getName() + ".super", "onDestroy");
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}

	final public void resetCache() {
		tcLog.d(getClass().getName() + ".super", "resetCache");
		listener.resetCache();
	}

	public void setHost(final String url, final String username, final String password, boolean sslHack, boolean sslHostNameHack) {
		tcLog.d(getClass().getName() + ".super", "setHost");
		if (_url != url) {
			_url = url;
			_username = username;
			_password = password;
			_sslHack = sslHack;
			_sslHostNameHack = sslHostNameHack;
			_ticket = null;
		}
	}

	public ProgressDialog startProgressBar(String message) {
		ProgressDialog progressBar = null;
		progressBar = new ProgressDialog(context);
		progressBar.setCancelable(true);
		if (message != null) {
			progressBar.setMessage(message);
		}
		progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		if (!context.isFinishing()) {
			progressBar.show();
			return progressBar;
		} else {
			return null;
		}
	}

	public ProgressDialog startProgressBar(int resid) {
		return startProgressBar(context.getString(resid));
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private Spinner makeDialogSpinner(Context context, boolean dialogWanted) {
		if (dialogWanted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			return new Spinner(context, Spinner.MODE_DIALOG);
		} else {
			return new Spinner(context);
		}
	}

	private Spinner _makeComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w,
			boolean dialogWanted) {
		final List<Object> spinValues = new ArrayList<Object>();

		if (optional) {
			spinValues.add("");
		}

		if (waardes != null) {
			for (int i = 0; i < waardes.size(); i++) {
				spinValues.add(waardes.get(i));
			}
		}

		final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_item, spinValues);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner valSpinner = makeDialogSpinner(context, dialogWanted);
		valSpinner.setAdapter(spinAdapter);
		if (waardes != null && w != null && !w.equals("")) {
			valSpinner.setSelection(waardes.indexOf(w) + (optional ? 1 : 0), true);
		}
		return valSpinner;
	}

	protected Spinner makeDialogComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w) {
		return _makeComboSpin(context, veldnaam, waardes, optional, w, true);
	}

	protected Spinner makeComboSpin(Context context, final String veldnaam, List<Object> waardes, boolean optional, Object w) {
		return _makeComboSpin(context, veldnaam, waardes, optional, w, false);
	}
	protected void selectTicket(int ticknr) {
		tcLog.d(this.getClass().getName(), "selectTicket = " + ticknr);
		final Ticket t = listener.getTicket(ticknr);
		if (t != null && t.hasdata()) {
			listener.onTicketSelected(t);
		} else {
			final ProgressDialog pb = startProgressBar(R.string.downloading);
			new Ticket(ticknr, context, new onTicketCompleteListener() {

				@Override
				public void onComplete(Ticket t2) {
					if (pb != null && !context.isFinishing()) {
						pb.dismiss();
					}
					if (t2.hasdata()) {
						listener.putTicket(t2);
						listener.onTicketSelected(t2);
					} else {
						context.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								final AlertDialog.Builder noTicketDialogBuilder = new AlertDialog.Builder(context);
								noTicketDialogBuilder.setTitle(R.string.notfound);
								noTicketDialogBuilder.setMessage(R.string.ticketnotfound);
								noTicketDialogBuilder.setCancelable(false);
								noTicketDialogBuilder.setPositiveButton(R.string.oktext, null);
								final AlertDialog noTicketDialog = noTicketDialogBuilder.create();
								noTicketDialog.show();
							}
						});
					}
				}
			});
		}
	}

}
