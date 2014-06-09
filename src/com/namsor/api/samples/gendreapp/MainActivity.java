package com.namsor.api.samples.gendreapp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.PieRenderer.DonutMode;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.google.android.gms.plus.PlusShare;

public class MainActivity extends ActionBarActivity {
	public static final boolean ANTI_ALIASING = true;
	protected static final boolean FLAG_FULLSCREEN = true;
	protected static final boolean FLAG_NOTITLE = true;

	private static final String TAG = "MainActivity"; //$NON-NLS-1$

	private UiLifecycleHelper uiHelper;
	private boolean serviceRunning = false;

	public synchronized boolean isServiceRunning() {
		return serviceRunning;
	}

	public synchronized void setServiceRunning(boolean serviceRunning) {
		this.serviceRunning = serviceRunning;
	}

	private ResponseReceiver receiver;
	private int[] genderStat;

	public synchronized int[] getGenderStat() {
		return genderStat;
	}

	public synchronized void setGenderStat(int[] genderStat) {
		this.genderStat = genderStat;
	}

	private String[] genderSample;

	public synchronized String[] getGenderSample() {
		return genderSample;
	}

	public synchronized void setGenderSample(String[] genderSample) {
		this.genderSample = genderSample;
	}

	private static final String TWEET_URL = Messages
			.getMessageString("MainActivity.share_url"); //$NON-NLS-1$
	private static final String FACEBOOKAPP_NAME = Messages
			.getMessageString("MainActivity.facebook_app_name"); //$NON-NLS-1$
	private static final String FACEBOOKAPP_URL = "http://namesorts.com/api/gendre/";
	private static final String FACEBOOKAPP_IMG = "http://namesorts.files.wordpress.com/2014/04/google_1024_x_500a.png?w=256&h=256&crop=1";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		// dump hash for FB integration
		if (BuildConfig.DEBUG) {
			dumpHash();
		}

		IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_STATUS);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ResponseReceiver(this);
		registerReceiver(receiver, filter);

		// FB stuff
		StatusCallback callback = new StatusCallback() {

			@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				if (exception != null) {
					Log.e("Activity", //$NON-NLS-1$
							String.format("Error: %s", exception.toString())); //$NON-NLS-1$
					exception.printStackTrace();
				}
			}

		};
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		uiHelper.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (isServiceRunning()) {
				stopService();
			}
			Intent settingsIntent = new Intent(this,
					GendreSettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}

	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (state.isOpened()) {
			// Facebook logged in...
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "Facebook logged in"); //$NON-NLS-1$
			}
			startService();
		} else if (state.isClosed()) {
			// Facebook logged out...
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "Facebook logged out"); //$NON-NLS-1$
			}
		}
	}

	public void facebookConnect() {
		Session.StatusCallback statusCallback = new Session.StatusCallback() {
			// callback when session changes state
			@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				onSessionStateChange(session, state, exception);
			}
		};
		Session session = Session.getActiveSession();
		if (!session.isOpened() && !session.isClosed()) {
			System.out.println("here"); //$NON-NLS-1$
			session.openForRead(new Session.OpenRequest(this).setPermissions(
					Arrays.asList("basic_info")) //$NON-NLS-1$
					.setCallback(statusCallback));
		} else {
			Session.openActiveSession(this, true, statusCallback);
		}
	}

	private boolean isAppInstalled(String uri) {
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try {
			pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}

	public void startService(View view) {

		// if facebook is installed try to connect, the callback will start the
		// service
		if (isAppInstalled("com.facebook.katana")) { //$NON-NLS-1$
			boolean includeFacebook = PreferenceManager
					.getDefaultSharedPreferences(this).getBoolean(
							"read_facebook", false); //$NON-NLS-1$
			if (includeFacebook) {
				facebookConnect();
			} else {
				startService();
			}
		} else {
			startService();
		}
	}

	public void startService() {

		// start service
		Intent mServiceIntent = new Intent(this, GenderizeTask.class);
		startService(mServiceIntent);
		setServiceRunning(true);

		// start animation
		Thread t = new Thread(new AnimationRunnable(this));
		t.start();

		// start wallpaper selection
		launchWallpaperIntentLater();

		// draw pie
		drawPie();

		// update pie status
		if (getPie().getVisualState() == MyPieChart.VisualState.start) {
			getPie().start();
			redrawPie();
		}

	}

	private void launchWallpaperIntentLater() {
		final boolean launchWallpaperIntent = PreferenceManager
				.getDefaultSharedPreferences(this).getBoolean(
						"live_wallpaper", true); //$NON-NLS-1$
		if (!launchWallpaperIntent) {
			return;
		}
		// lauch wallpaper selection
		final Runnable launchWallpaper = new Runnable() {
			@Override
			public void run() {
				launchWallpaperIntent();
			}
		};

		Runnable launchWallpaperIntentStarter = new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 3; i++) {
					try {
						Thread.sleep(30000);
						if (GenderStats.getCount() > 10) {
							break;
						}
					} catch (InterruptedException e) {
						// ignore
						e.printStackTrace();
					}
				}
				runOnUiThread(launchWallpaper);
			}
		};
		Thread t = new Thread(launchWallpaperIntentStarter);
		t.start();
	}


	private void launchWallpaperIntent() {
		boolean launchWallpaperIntent = PreferenceManager
				.getDefaultSharedPreferences(this).getBoolean(
						"live_wallpaper", true); //$NON-NLS-1$
		if (!launchWallpaperIntent) {
			return;
		}
		Toast toast = Toast.makeText(this,
				"Choose GendRE from the list to start the Live Wallpaper.",
				Toast.LENGTH_LONG);
		toast.show();
		Intent i = new Intent();
		if (Build.VERSION.SDK_INT > 15) {
			i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
			String p = LiquidPhysicsWallpaper.class.getPackage().getName();
			String c = LiquidPhysicsWallpaper.class.getCanonicalName();
			i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
					new ComponentName(p, c));
		} else {
			i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
		}
		startActivityForResult(i, 0);
		// launch just once
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean("live_wallpaper", false).commit(); //$NON-NLS-1$
	}

	public void marketThis_(View view) {
		final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
		try {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
		} catch (android.content.ActivityNotFoundException anfe) {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
		}		
	}
	
	public void facebookThis(View view) {
		int[] genderStats = getGenderStat();
		if (genderStats == null) {
			return;
		}
		String shareText = Messages
				.getMessageString("MainActivity.share_facebook_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF
				+ Messages
						.getMessageString("MainActivity.share_facebook_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM
				+ Messages
						.getMessageString("MainActivity.share_facebook_part3"); //$NON-NLS-1$
		String shareURL = FACEBOOKAPP_URL;
		String imgURL = FACEBOOKAPP_IMG;
		String tagline = Messages.getAboutString("About.tag_line");
		FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
				.setDescription(shareText).setApplicationName(FACEBOOKAPP_NAME)
				.setCaption(tagline).setPicture(imgURL).setLink(shareURL)
				.build();
		uiHelper.trackPendingDialogCall(shareDialog.present());
	}

	/*
	 * IDEA? make a screenshot for sharing private void screenShot() { View
	 * view= findViewById(R.id.relativeLayout); View v = view.getRootView();
	 * v.setDrawingCacheEnabled(true); Bitmap b = v.getDrawingCache(); String
	 * extr = Environment.getExternalStorageDirectory().toString(); File myPath
	 * = new File(extr, "snapshot.jpg"); FileOutputStream fos = null; try { fos
	 * = new FileOutputStream(myPath); b.compress(Bitmap.CompressFormat.PNG,
	 * 100, fos); fos.flush(); fos.close();
	 * MediaStore.Images.Media.insertImage(getContentResolver(), b, "Screen",
	 * "screen"); } catch (FileNotFoundException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } catch (Exception e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } }
	 */

	public void gplusThis(View view) {
		int[] genderStats = getGenderStat();
		if (genderStats == null) {
			return;

		}
		String shareText = Messages
				.getMessageString("MainActivity.share_gplus_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF
				+ Messages.getMessageString("MainActivity.share_gplus_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM
				+ Messages.getMessageString("MainActivity.share_gplus_part3"); //$NON-NLS-1$
		String shareURL = TWEET_URL;

		// Launch the Google+ share dialog with attribution to your app.
		Intent shareIntent = new PlusShare.Builder(this).setType("text/plain") //$NON-NLS-1$
				.setText(shareText).setContentUrl(Uri.parse(shareURL))
				.getIntent();

		startActivityForResult(shareIntent, 0);
	}

	private void dumpHash() {
		// Add code to print out the key hash
		try {
			PackageInfo info = getPackageManager().getPackageInfo(
					"com.namsor.api.samples.gendreapp", //$NON-NLS-1$
					PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA"); //$NON-NLS-1$
				md.update(signature.toByteArray());
				String keyHash = Base64.encodeToString(md.digest(),
						Base64.DEFAULT);
				if (BuildConfig.DEBUG) {
					Log.d("KeyHash:", keyHash); //$NON-NLS-1$
				}
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}

	public void tweetThis(View view) {
		int[] genderStats = getGenderStat();
		if (genderStats == null) {
			return;
		}
		String tweetText = Messages
				.getMessageString("MainActivity.share_twitter_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF
				+ Messages.getMessageString("MainActivity.share_twitter_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM
				+ Messages.getMessageString("MainActivity.share_twitter_part3"); //$NON-NLS-1$
		String tweetURL = TWEET_URL;
		String tweetUrl;
		try {
			tweetUrl = String.format(
					"https://twitter.com/intent/tweet?text=%s&url=%s", //$NON-NLS-1$
					URLEncoder.encode(tweetText, "UTF-8"), //$NON-NLS-1$
					URLEncoder.encode(tweetURL, "UTF-8")); //$NON-NLS-1$
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));

			// Narrow down to official Twitter app, if available:
			List<ResolveInfo> matches = getPackageManager()
					.queryIntentActivities(intent, 0);
			for (ResolveInfo info : matches) {
				if (info.activityInfo.packageName.toLowerCase(Locale.ENGLISH)
						.startsWith("com.twitter")) { //$NON-NLS-1$
					intent.setPackage(info.activityInfo.packageName);
				}
			}
			startActivity(intent);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class ResponseReceiver extends BroadcastReceiver {
		public static final String ACTION_STATUS = "com.namsor.api.samples.gendreapp.intent.action.SERVICE_STATUS"; //$NON-NLS-1$
		public static final String ATTR_genderCount = "genderCount"; //$NON-NLS-1$
		public static final String ATTR_genderSample = "genderSample"; //$NON-NLS-1$
		public static final String ATTR_statusType = "statusType"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_COUNTING = "counting"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_GENDERIZING = "genderizing"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_GENDERIZED = "genderized"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_WIPING = "wiping"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_WIPED = "wiped"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_STOPPED = "stopped"; //$NON-NLS-1$
		private static final int REDRAW_MOD = 10;

		private MainActivity activity;

		public ResponseReceiver(MainActivity activity) {
			this.activity = activity;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(ACTION_STATUS)) {
				return;
			}
			MyGenderView genderView = (MyGenderView) findViewById(R.id.myGenderView);
			String statusType = intent.getStringExtra(ATTR_statusType);
			if (statusType.equals(ATTRVAL_statusType_GENDERIZED)
					|| statusType.equals(ATTRVAL_statusType_GENDERIZING)
					|| statusType.equals(ATTRVAL_statusType_COUNTING)
					|| statusType.equals(ATTRVAL_statusType_WIPING)) {

				if (getPie() == null
						|| getPie().getVisualState() == MyPieChart.VisualState.start) {
					drawPie();
					getPie().start();
				}
				setServiceRunning(true);

				int[] data = intent.getIntArrayExtra(ATTR_genderCount);
				setGenderStat(data);
				if (statusType.equals(ATTRVAL_statusType_GENDERIZING)
						|| statusType.equals(ATTRVAL_statusType_COUNTING)) {
					String[] sample = intent
							.getStringArrayExtra(ATTR_genderSample);
					if (sample != null) {
						setGenderSample(sample);
					}
				}
				if (data != null && data.length == 3) {

					genderView.setCountF("" + data[0]); //$NON-NLS-1$
					genderView.setCountM("" + data[1]); //$NON-NLS-1$
					genderView.setCountU("" + data[2]); //$NON-NLS-1$
					int totCount = data[0] + data[1] + data[2];
					sF.setValue(data[0]);
					sM.setValue(data[1]);
					sU.setValue(data[2]);

					if (totCount < REDRAW_MOD || totCount % REDRAW_MOD == 0
							|| statusType.equals(ATTRVAL_statusType_GENDERIZED)) {
						// request refresh of counters
						genderView.invalidate();
						redrawPie();
					}

				}
				if (statusType.equals(ATTRVAL_statusType_GENDERIZED)) {
					serviceDoneAndShare();
				}
			} else if (statusType.equals(ATTRVAL_statusType_STOPPED)) {
				setServiceRunning(false);
				// request refresh of counters
				if(GenderStats.getCount()>10) {
					serviceDoneAndShare();							
				} else {
					getPie().stopAndStart();
				}
				genderView.invalidate();
				redrawPie();
			} else if (statusType.equals(ATTRVAL_statusType_WIPED)) {
				int[] data = { 0, 0, 0 };
				setGenderStat(data);
				genderView.setCountF("" + 0); //$NON-NLS-1$
				genderView.setCountM("" + 0); //$NON-NLS-1$
				genderView.setCountU("" + 0); //$NON-NLS-1$
				sF.setValue(1);
				sM.setValue(1);
				sU.setValue(1);
				// request refresh of counters
				genderView.invalidate();
				redrawPie();
			}
		}
	};

	private void redrawPie() {
		// redraw pie
		if (getPie() == null) {
			drawPie();
		} else {
			getPie().redraw();
		}
	}

	public void stopService() {
		setServiceRunning(false);

		Intent broadcastIntent = new Intent();
		broadcastIntent
				.setAction(GenderizeTask.ActivityReceiver.ACTIVITY_STATUS);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(
				GenderizeTask.ActivityReceiver.ATTR_statusType,
				GenderizeTask.ActivityReceiver.ATTRVAL_statusType_STOP_REQUEST);
		sendBroadcast(broadcastIntent);
	}

	public void stopService(View view) {
		stopService();
	}

	public void serviceDoneAndShare() {
		redrawPie();
		final MainActivity activity = this;
		if (getPie().getVisualState() == MyPieChart.VisualState.running) {
			getPie().done();
		}
		if (getPie().getVisualState() == MyPieChart.VisualState.done) {
			getPie().share();

			// Show the panel
			final ViewGroup viewGroup = (ViewGroup) findViewById(R.id.fullscreen_content_controls);

			LayoutTransition layoutTransition = new LayoutTransition();
			viewGroup.setLayoutTransition(layoutTransition);
			final ImageView img = (ImageView) findViewById(R.id.logo_image);
			viewGroup.removeView(img);

			final MyShareView shareView = new MyShareView(activity);
			shareView.setTitle("Thanks for using GendRe");
			shareView.setSubTitle("Share your results!");
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) img
					.getLayoutParams();
			shareView.setLayoutParams(params);
			shareView.setVisibility(shareView.INVISIBLE);
			viewGroup.addView(shareView);
			TransitionListener listener = new TransitionListener() {

				@Override
				public void endTransition(LayoutTransition arg0,
						ViewGroup viewGroup_, View view, int arg3) {
					if (view == shareView) {
						shareView.setVisibility(shareView.VISIBLE);
					}
				}

				@Override
				public void startTransition(LayoutTransition arg0,
						ViewGroup arg1, View arg2, int arg3) {

				}

			};
			layoutTransition.addTransitionListener(listener);
			shareView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						int share = shareView.touched(event);
						if (share == 0) {
							activity.facebookThis(shareView);
						} else if (share == 1) {
							activity.tweetThis(shareView);
						} else if (share == 2) {
							activity.gplusThis(shareView);
						}
						return true;
					}
					return false;
				}
			});
			// logoMoveAnimation.setFillAfter(true);
			// final ImageView img2 = (ImageView)
			// findViewById(R.id.share_image);
		}
		redrawPie();
	}

	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	private MyPieChart pie;
	private final Segment sM = new Segment("♂", 1); // ♂ //$NON-NLS-1$
	private final Segment sU = new Segment("", 1); //$NON-NLS-1$
	private final Segment sF = new Segment("♀", 1); // ♀ //$NON-NLS-1$

	private void drawPie() {
		if (getPie() == null) {
			// pie chart stuff
			setPie((MyPieChart) findViewById(R.id.mySimplePieChart));
			if (getPie() == null) {
				return;
			}

			final MainActivity activity = this;
			// detect segment clicks:
			getPie().setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {

					// Toast toast = Toast.makeText(activity, "Clicked",
					// Toast.LENGTH_LONG);
					// toast.show();
					if (getPie().getVisualState() == MyPieChart.VisualState.running) {
						// pie.stop();
						if(GenderStats.getCount()>10) {
							serviceDoneAndShare();							
						} else {
							getPie().stopAndStart();
						}
						stopService();
					} else if (getPie().getVisualState() == MyPieChart.VisualState.start) {
						if (FLAG_FULLSCREEN) {
							Window w = activity.getWindow(); // in Activity's
																// onCreate()
																// for instance
							w.setFlags(
									WindowManager.LayoutParams.FLAG_FULLSCREEN,
									WindowManager.LayoutParams.FLAG_FULLSCREEN);
						}
						// start animation
						Thread t = new Thread(new AnimationRunnable(activity));
						t.start();

						getPie().start();
						startService();
					} else if (getPie().getVisualState() == MyPieChart.VisualState.share) {
						// do nothing
					}
					redrawPie();
				}

			});

			// EmbossMaskFilter emf = new EmbossMaskFilter(new float[]{1, 1, 1},
			// 0.4f, 10, 8.2f);

			SegmentFormatter sMFormat = new SegmentFormatter();
			sMFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter_m);

			SegmentFormatter sUFormat = new SegmentFormatter();
			sUFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter_u);

			SegmentFormatter sFFormat = new SegmentFormatter();
			sFFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter_f);

			getPie().addSeries(sM, sMFormat);
			getPie().addSeries(sU, sUFormat);
			getPie().addSeries(sF, sFFormat);

			// bug: will be fixed
			// pie.getRenderer(PieRenderer.class).setDonutSize(0,
			// DonutMode.PERCENT);
			PieRenderer renderer = getPie().getRenderer(PieRenderer.class);
			renderer.setStartDeg(-90);
			renderer.setDonutSize(.85f, DonutMode.PERCENT);

			getPie().getBorderPaint().setColor(Color.TRANSPARENT);

			// Drawable background =
			// getResources().getDrawable(R.drawable.gender_bg);
			// pie.setBackground(background);

			getPie().getBackgroundPaint().setColor(Color.parseColor("#F2F3F4"));

			getPie().setVisibility(PieChart.VISIBLE);
			
			ImageView graphImage = (ImageView )findViewById(R.id.graph_image);
			graphImage.setVisibility(PieChart.INVISIBLE);

		} else {
			redrawPie();
		}
	}

	public void animateContact(String[] sample) {
		int MAX_NAME_LEN = 35;
		long ANIM_DUR = 300;
		TextView contactView = (TextView) findViewById(R.id.animatedName);
		if (contactView == null) {
			return;
		}
		String fullName = sample[0] + " " + sample[1];
		if (fullName.length() > MAX_NAME_LEN) {
			fullName = fullName.substring(0, MAX_NAME_LEN - 5) + "[...]";
		}
		int gender = 1 - Integer.parseInt(sample[3]) * 2;

		/*
		 * Typeface tf_regular = Typeface.createFromAsset( getAssets(),
		 * "fonts/Montserrat-Regular.ttf"); Typeface tf_bold =
		 * Typeface.createFromAsset(getAssets(), "fonts/Montserrat-Bold.ttf");
		 * contactView.setTypeface(tf_regular);
		 * contactView.getPaint().setTypeface(tf_regular);
		 */

		contactView.setTextColor(Color.BLACK);
		contactView.setVisibility(TextView.VISIBLE);
		contactView.setText(fullName);
		LinearLayout genderAnim = (LinearLayout) this
				.findViewById(R.id.gender_anim);
		float borderRatio = .1f;

		Rect bounds = new Rect();
		contactView.getPaint().getTextBounds("" + contactView.getText(), 0,
				contactView.getText().length(), bounds);
		int textWidth = bounds.width();
		float centerX = genderAnim.getWidth() / 2;
		float startX = centerX - textWidth / 2;
		contactView.setLeft((int) startX);
		contactView.setAlpha(0f);

		float distX = 0;
		int color = Color.LTGRAY;
		if (gender > 0) {
			distX = borderRatio * genderAnim.getWidth() - startX;
			color = MyGenderView.femaleColor;
		} else if (gender < 0) {
			distX = genderAnim.getWidth() * (1 - borderRatio) - textWidth
					- startX;
			color = MyGenderView.maleColor;
		} else {
			distX = 0; // stay there
		}

		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contactView, "alpha", //$NON-NLS-1$
				0f, 1f);
		fadeIn.setDuration(ANIM_DUR);
		fadeIn.start();

		ArgbEvaluator evaluator = new ArgbEvaluator();
		ObjectAnimator colorize = ObjectAnimator.ofObject(contactView,
				"textColor", evaluator, Color.BLACK, color);
		colorize.setDuration(ANIM_DUR).start();

		ObjectAnimator moveSide = ObjectAnimator.ofFloat(contactView,
				"translationX", 0, distX); //$NON-NLS-1$
		moveSide.setDuration(ANIM_DUR);
		moveSide.setInterpolator(new AccelerateInterpolator());

		ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contactView, "alpha",
				0f);
		fadeOut.setDuration(ANIM_DUR);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(fadeOut).after(moveSide).after(colorize);
		animatorSet.start();

	}

	public MyPieChart getPie() {
		return pie;
	}

	public void setPie(MyPieChart pie) {
		this.pie = pie;
	}

	private class AnimationRunnable implements Runnable {
		public AnimationRunnable(MainActivity activity) {
			super();
			this.activity = activity;
		}

		private final MainActivity activity;

		public synchronized MainActivity getActivity() {
			return activity;
		}

		@Override
		public void run() {
			String[] sampleOld = null;

			while (activity.isServiceRunning()) {
				final String[] sample = activity.getGenderSample();
				if (sample == null || sample[0] == null || sample[1] == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if (sampleOld == null || !sampleOld[0].equals(sample[0])
						|| !sampleOld[1].equals(sample[1])) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							activity.animateContact(sample);
						}
					});
					sampleOld = sample;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

	};

}
