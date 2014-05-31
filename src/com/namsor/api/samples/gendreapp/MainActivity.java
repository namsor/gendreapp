package com.namsor.api.samples.gendreapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.androidplot.xy.XYPlot;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.PieRenderer.DonutMode;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusShare;
import com.namsor.api.samples.gendreapp.BuildConfig;
import com.namsor.api.samples.gendreapp.R;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

public class MainActivity extends ActionBarActivity {

	private static final String TAG = "MainActivity"; //$NON-NLS-1$

	private PieChart pie;

	private final Segment sM = new Segment("♂", 1); //$NON-NLS-1$
	private final Segment sU = new Segment("", 1); //$NON-NLS-1$
	private final Segment sF = new Segment("♀", 1); //$NON-NLS-1$

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

	private static final String TWEET_URL = Messages.getMessageString("MainActivity.share_url"); //$NON-NLS-1$
	private static final String FACEBOOKAPP_NAME = Messages.getMessageString("MainActivity.facebook_app_name"); //$NON-NLS-1$
	private static final String FACEBOOKAPP_URL = "http://namesorts.com/api/gendre/";
	private static final String FACEBOOKAPP_IMG = "http://namesorts.files.wordpress.com/2014/04/google_1024_x_500a.png?w=256&h=256&crop=1";

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
				if (sample == null) {
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		// dump hash for FB integration
		if(BuildConfig.DEBUG) {				
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

	public void animateContact(String[] sample) {
		TextView textView2 = (TextView) findViewById(R.id.textViewQuote);

		TextView contactView = (TextView) findViewById(R.id.textView_contact);
		contactView.setText(sample[0] + " " + sample[1]); //$NON-NLS-1$
		contactView.setX(textView2.getX() + textView2.getWidth() / 2
				- contactView.getWidth() / 2);
		contactView.setY(textView2.getY() + textView2.getHeight());
		contactView.setAlpha(0f);
		contactView.bringToFront();

		TextView textViewVenus = (TextView) findViewById(R.id.textView_venus);
		TextView textViewMars = (TextView) findViewById(R.id.textView_mars);
		float distY = textViewMars.getY() - contactView.getY()
				- contactView.getHeight();
		float distX = 0;
		int gender = Integer.parseInt(sample[3]) * 2 - 1;
		if (gender < 0) {
			distX = (textViewVenus.getX() - contactView.getX()) / 2;
		} else {
			distX = (textViewMars.getX() + textViewMars.getWidth()
					- contactView.getX() - contactView.getWidth()) / 2;
		}

		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contactView, "alpha", //$NON-NLS-1$
				0f, 1f);
		fadeIn.setDuration(300);
		fadeIn.start();

		ObjectAnimator moveDown1 = ObjectAnimator.ofFloat(contactView,
				"translationY", 0, 100 + distY * 2 / 3); //$NON-NLS-1$
		moveDown1.setDuration(300);
		moveDown1.setInterpolator(new DecelerateInterpolator());

		ObjectAnimator moveDown2 = ObjectAnimator.ofFloat(contactView,
				"translationY", 0, distY * 1 / 3); //$NON-NLS-1$
		moveDown2.setDuration(300);
		moveDown2.setInterpolator(new AccelerateInterpolator());

		ObjectAnimator moveSide = ObjectAnimator.ofFloat(contactView,
				"translationX", 0, distX); //$NON-NLS-1$
		moveSide.setDuration(300);
		moveSide.setInterpolator(new AccelerateInterpolator());

		ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contactView, "alpha", //$NON-NLS-1$
				0f);
		fadeOut.setDuration(300);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(fadeOut).after(moveDown2).after(moveSide)
				.after(moveDown1);
		animatorSet.start();

		// redraw pie
		pie = (PieChart) findViewById(R.id.mySimplePieChart);
		if (pie != null) {
			pie.redraw();
		}
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

	private void hidePie() {
		if (pie == null) {
			// pie chart stuff
			pie = (PieChart) findViewById(R.id.mySimplePieChart);
			if (pie == null) {
				return;
			}
			pie.setVisibility(PieChart.INVISIBLE);
			ImageView img = (ImageView) findViewById(R.id.cover_image);
			img.setVisibility(ImageView.VISIBLE);
		}
	}

	private void drawPie() {
		if (pie == null) {
			// pie chart stuff
			pie = (PieChart) findViewById(R.id.mySimplePieChart);
			if (pie == null) {
				return;
			}
			// EmbossMaskFilter emf = new EmbossMaskFilter(new float[]{1, 1, 1},
			// 0.4f, 10, 8.2f);

			SegmentFormatter sFFormat = new SegmentFormatter();
			sFFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter3);

			SegmentFormatter sUFormat = new SegmentFormatter();
			sUFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter2);

			SegmentFormatter sMFormat = new SegmentFormatter();
			sMFormat.configure(getApplicationContext(),
					R.xml.pie_segment_formatter1);

			pie.addSeries(sM, sMFormat);
			pie.addSeries(sF, sFFormat);
			pie.addSeries(sU, sUFormat);
			// bug: will be fixed
			// pie.getRenderer(PieRenderer.class).setDonutSize(0,
			// DonutMode.PERCENT);

			pie.getBorderPaint().setColor(Color.TRANSPARENT);
			pie.getBackgroundPaint().setColor(Color.WHITE);

			pie.setVisibility(PieChart.VISIBLE);
			ImageView img = (ImageView) findViewById(R.id.cover_image);
			img.setVisibility(ImageView.INVISIBLE);
			
		} else {
			pie.redraw();
		}
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (state.isOpened()) {
			// Facebook logged in...
			if(BuildConfig.DEBUG) {
				Log.i(TAG, "Facebook logged in"); //$NON-NLS-1$
			}
			startService();
		} else if (state.isClosed()) {
			// Facebook logged out...
			if(BuildConfig.DEBUG) {
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
			session.openForRead(new Session.OpenRequest(this).setPermissions(Arrays.asList("basic_info")) //$NON-NLS-1$
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
		// if facebook is installed try to connect, the callback will start the service
		if( isAppInstalled("com.facebook.katana") ) { //$NON-NLS-1$
			boolean includeFacebook = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
					"read_facebook", false); //$NON-NLS-1$
			if( includeFacebook ) {
				facebookConnect();
			} else {
				startService();
			}
		} else {
			startService();
		}
	}
	
	public void startService() {
	
		Button btn = (Button) findViewById(R.id.button_genderize);
		btn.setText(R.string.btn_genderize_running);
		btn.setEnabled(false);

		View v = findViewById(R.id.button_stop);
		Button btnStop = (Button) v;
		btnStop.setEnabled(true);
		btnStop.setVisibility(Button.VISIBLE);

		Intent mServiceIntent = new Intent(this, GenderizeTask.class);
		startService(mServiceIntent);
		setServiceRunning(true);

		// draw pie
		drawPie();

		// start animation
		Thread t = new Thread(new AnimationRunnable(this));
		t.start();

		// start wallpaper selection
		launchWallpaperIntentLater();
	}
	
	private void launchWallpaperIntentLater() {
		final boolean launchWallpaperIntent = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("live_wallpaper", true); //$NON-NLS-1$
		if( ! launchWallpaperIntent ) {
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
						Thread.sleep(5000);
						if( GenderStats.getCount() > 10 ) {
							break;
						}
					} catch (InterruptedException e) {
						// ignore
					}
				}
				runOnUiThread(launchWallpaper);
			}			
		};
		Thread t = new Thread(launchWallpaperIntentStarter);
		t.start();
	}
	
	private void launchWallpaperIntent() {
		boolean launchWallpaperIntent = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("live_wallpaper", true); //$NON-NLS-1$
		if( ! launchWallpaperIntent ) {
			return;
		}
		Toast toast = Toast.makeText(this, "Choose GendRE from the list to start the Live Wallpaper.",Toast.LENGTH_LONG);
		toast.show();
		Intent i = new Intent();
		if(Build.VERSION.SDK_INT > 15){
		    i.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
		    String p = LiquidPhysicsWallpaper.class.getPackage().getName();
		    String c = LiquidPhysicsWallpaper.class.getCanonicalName();        
		    i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(p, c));
		}
		else{
		    i.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
		}
		startActivityForResult(i, 0);
		// launch just once
		PreferenceManager.getDefaultSharedPreferences(this).edit()
		.putBoolean("live_wallpaper", false).commit(); //$NON-NLS-1$
	}

	public void facebookThis(View view) {
		int[] genderStats = getGenderStat();
		if (genderStats == null) {
			return;
		}
		String shareText = Messages.getMessageString("MainActivity.share_facebook_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF + Messages.getMessageString("MainActivity.share_facebook_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM + Messages.getMessageString("MainActivity.share_facebook_part3"); //$NON-NLS-1$
		String shareURL = FACEBOOKAPP_URL;
		String imgURL = FACEBOOKAPP_IMG;
		String tagline = Messages.getAboutString("About.tag_line");
		FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
				.setDescription(shareText)
				.setApplicationName(FACEBOOKAPP_NAME)
				.setCaption(tagline)
				.setPicture(imgURL)
				.setLink(shareURL).build();
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
		String shareText = Messages.getMessageString("MainActivity.share_gplus_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF + Messages.getMessageString("MainActivity.share_gplus_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM + Messages.getMessageString("MainActivity.share_gplus_part3"); //$NON-NLS-1$
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
				if(BuildConfig.DEBUG) {				
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
		String tweetText = Messages.getMessageString("MainActivity.share_twitter_part1") + genderStats[0] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERF + Messages.getMessageString("MainActivity.share_twitter_part2") + genderStats[1] //$NON-NLS-1$
				+ GenderizeTask.PREFIX_GENDERM + Messages.getMessageString("MainActivity.share_twitter_part3"); //$NON-NLS-1$
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
				if (info.activityInfo.packageName.toLowerCase(Locale.ENGLISH).startsWith(
						"com.twitter")) { //$NON-NLS-1$
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
		private static final int REDRAW_MOD = 20;

		private MainActivity activity;

		public ResponseReceiver(MainActivity activity) {
			this.activity = activity;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(ACTION_STATUS)) {
				return;
			}
			String statusType = intent.getStringExtra(ATTR_statusType);
			if (statusType.equals(ATTRVAL_statusType_GENDERIZED)
					|| statusType.equals(ATTRVAL_statusType_GENDERIZING)
					|| statusType.equals(ATTRVAL_statusType_COUNTING)
					|| statusType.equals(ATTRVAL_statusType_WIPING)) {

				Button btn = (Button) findViewById(R.id.button_genderize);
				if (statusType.equals(ATTRVAL_statusType_GENDERIZING)) {
					btn.setText(R.string.btn_genderize_running);
				} else if (statusType.equals(ATTRVAL_statusType_COUNTING)) {
					btn.setText(R.string.btn_genderize_counting);
				} else if (statusType.equals(ATTRVAL_statusType_WIPING)) {
					btn.setText(R.string.btn_genderize_wiping);
				}
				btn.setEnabled(false);
				setServiceRunning(true);

				Button btnStop = (Button) findViewById(R.id.button_stop);
				btnStop.setText(R.string.btn_stop);
				btnStop.setEnabled(true);
				btnStop.setVisibility(Button.VISIBLE);

				int[] data = intent.getIntArrayExtra(ATTR_genderCount);
				setGenderStat(data);
				if (statusType.equals(ATTRVAL_statusType_GENDERIZING)) {
					String[] sample = intent
							.getStringArrayExtra(ATTR_genderSample);
					if (sample != null) {
						setGenderSample(sample);
					}
				}
				if (data != null && data.length == 3) {
					TextView tvf = (TextView) findViewById(R.id.textView_female);
					TextView tvm = (TextView) findViewById(R.id.textView_male);
					TextView tvu = (TextView) findViewById(R.id.textView_unknown);
					tvf.setText("" + data[0]); //$NON-NLS-1$
					tvm.setText("" + data[1]); //$NON-NLS-1$
					tvu.setText("" + data[2]); //$NON-NLS-1$
					sF.setValue(data[0]);
					sM.setValue(data[1]);
					sU.setValue(data[2]);

					if (statusType.equals(ATTRVAL_statusType_GENDERIZED)) {
						// redraw pie
						drawPie();

						ImageButton btnTweet = (ImageButton) findViewById(R.id.imageButton_tweet);
						TextView tweetThis = (TextView) findViewById(R.id.textView_tweetthis);
						btnTweet.setVisibility(Button.VISIBLE);
						btnTweet.setEnabled(true);
						tweetThis.setVisibility(TextView.VISIBLE);
						tweetThis.setEnabled(true);
						ImageButton btnFacebook = (ImageButton) findViewById(R.id.imageButton_facebook);
						btnFacebook.setVisibility(Button.VISIBLE);
						btnFacebook.setEnabled(true);
						ImageButton btnGPlus = (ImageButton) findViewById(R.id.imageButton_googleplus);
						btnGPlus.setVisibility(Button.VISIBLE);
						btnGPlus.setEnabled(true);
					} else {
						int count = data[0] + data[1] + data[2];
						if (pie == null || count > REDRAW_MOD
								&& count % REDRAW_MOD == 0) {
							// redraw pie
							drawPie();
						}
						ImageButton btnTweet = (ImageButton) findViewById(R.id.imageButton_tweet);
						TextView tweetThis = (TextView) findViewById(R.id.textView_tweetthis);
						btnTweet.setVisibility(Button.INVISIBLE);
						btnTweet.setEnabled(false);
						tweetThis.setVisibility(TextView.INVISIBLE);
						tweetThis.setEnabled(false);
						ImageButton btnFacebook = (ImageButton) findViewById(R.id.imageButton_facebook);
						btnFacebook.setVisibility(Button.INVISIBLE);
						btnFacebook.setEnabled(false);
						ImageButton btnGPlus = (ImageButton) findViewById(R.id.imageButton_googleplus);
						btnGPlus.setVisibility(Button.INVISIBLE);
						btnGPlus.setEnabled(false);
					}
				}
			} else if (statusType.equals(ATTRVAL_statusType_STOPPED)) {
				Button btn = (Button) findViewById(R.id.button_genderize);
				btn.setText(R.string.btn_genderize_start);
				btn.setEnabled(true);

				Button btnStop = (Button) findViewById(R.id.button_stop);
				btnStop.setText(R.string.btn_stop);
				btnStop.setEnabled(false);
				btnStop.setVisibility(Button.INVISIBLE);

				setServiceRunning(false);
			} else if (statusType.equals(ATTRVAL_statusType_WIPED)) {
				Button btn = (Button) findViewById(R.id.button_genderize);
				btn.setText(R.string.btn_genderize_wiped);
				btn.setEnabled(false);
				int[] data = { 0, 0, 0 };
				setGenderStat(data);
			}
		}
	};

	public void stopService() {
		Button btn = (Button) findViewById(R.id.button_genderize);
		btn.setText(R.string.btn_genderize_stopping);
		btn.setEnabled(false);

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

}
