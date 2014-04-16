package com.namsor.api.samples.gendre;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Build;
import android.preference.PreferenceManager;

public class MainActivity extends ActionBarActivity {

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

	private static final String TWEET_URL = "http://namesorts.com/api/gendre";


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
			
			while( activity.isServiceRunning() ) {
				final String[] sample = activity.getGenderSample();
				if( sample == null ) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if( sampleOld == null || !sampleOld[0].equals(sample[0]) || !sampleOld[1].equals(sample[1])  ) {
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

		// launch preferences on first occurrence ?
		IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_STATUS);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ResponseReceiver(this);
		registerReceiver(receiver, filter);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void animateContact(String[] sample) {
		TextView textView2 = (TextView) findViewById(R.id.textView2);
		
		TextView contactView = (TextView) findViewById(R.id.textView_contact);
		contactView.setText(sample[0]+" "+sample[1]);
		contactView.setX(textView2.getX()+textView2.getWidth()/2-contactView.getWidth()/2);
		contactView.setY(textView2.getY()+textView2.getHeight());
		contactView.setAlpha(0f);
		
		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contactView, "alpha",
				0f, 1f);
		fadeIn.setDuration(300);
		fadeIn.start();

		int gender = Integer.parseInt(sample[3])*2-1;
		ObjectAnimator moveDown1 = ObjectAnimator.ofFloat(contactView,
				"translationY", 100f);
		moveDown1.setDuration(300);
		moveDown1.setInterpolator(new DecelerateInterpolator());

		ObjectAnimator moveDown2 = ObjectAnimator.ofFloat(contactView,
				"translationY", 50f);
		moveDown2.setDuration(300);
		moveDown2.setInterpolator(new AccelerateInterpolator());
		
		
		ObjectAnimator moveSide = ObjectAnimator.ofFloat(contactView,
				"translationX", gender*50f);
		moveSide.setDuration(300);
		moveSide.setInterpolator(new AccelerateInterpolator());

		ObjectAnimator fadeOut = ObjectAnimator.ofFloat(contactView, "alpha",
				0f);
		fadeOut.setDuration(300);
		

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(fadeOut).after(moveDown2).after(moveSide).after(moveDown1);
		animatorSet.start();
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

	public void startService(View view) {
		
		Button btn = (Button) view;
		btn.setText(R.string.btn_genderize_running);
		btn.setEnabled(false);

		Button btnStop = (Button) findViewById(R.id.button_stop);
		btnStop.setEnabled(true);
		btnStop.setVisibility(Button.VISIBLE);

		Intent mServiceIntent = new Intent(this, GenderizeTask.class);
		startService(mServiceIntent);
		setServiceRunning(true);

		// start animation
		Thread t = new Thread(new AnimationRunnable(this));
		t.start();
		
	}

	public void tweetThis(View view) {
		int[] genderStats = getGenderStat();
		if (genderStats == null) {
			return;
		}
		String tweetText = "My Android contacts: " + genderStats[0]
				+ GenderizeTask.PREFIX_GENDERF + " and " + genderStats[1]
				+ GenderizeTask.PREFIX_GENDERM + " via @gendreapp ";
		String tweetURL = TWEET_URL;
		String tweetUrl;
		try {
			tweetUrl = String.format(
					"https://twitter.com/intent/tweet?text=%s&url=%s",
					URLEncoder.encode(tweetText, "UTF-8"),
					URLEncoder.encode(tweetURL, "UTF-8"));
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));

			// Narrow down to official Twitter app, if available:
			List<ResolveInfo> matches = getPackageManager()
					.queryIntentActivities(intent, 0);
			for (ResolveInfo info : matches) {
				if (info.activityInfo.packageName.toLowerCase().startsWith(
						"com.twitter")) {
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
		public static final String ACTION_STATUS = "com.namsor.api.samples.gendre.intent.action.SERVICE_STATUS";
		public static final String ATTR_genderCount = "genderCount";
		public static final String ATTR_genderSample = "genderSample";
		public static final String ATTR_statusType = "statusType";
		public static final String ATTRVAL_statusType_COUNTING = "counting";
		public static final String ATTRVAL_statusType_GENDERIZING = "genderizing";
		public static final String ATTRVAL_statusType_GENDERIZED = "genderized";
		public static final String ATTRVAL_statusType_WIPING = "wiping";
		public static final String ATTRVAL_statusType_WIPED = "wiped";
		public static final String ATTRVAL_statusType_STOPPED = "stopped";

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
				btn.setText(R.string.btn_genderize_running);
				btn.setEnabled(false);
				setServiceRunning(true);

				int[] data = intent.getIntArrayExtra(ATTR_genderCount);
				setGenderStat(data);
				if( statusType.equals(ATTRVAL_statusType_GENDERIZING) ) {
					String[] sample = intent.getStringArrayExtra(ATTR_genderSample);
					if( sample!=null) {
						setGenderSample(sample);
					}
				}
				if (data != null && data.length == 3) {
					TextView tvf = (TextView) findViewById(R.id.textView_female);
					TextView tvm = (TextView) findViewById(R.id.textView_male);
					TextView tvu = (TextView) findViewById(R.id.textView_unknown);
					tvf.setText("" + data[0]);
					tvm.setText("" + data[1]);
					tvu.setText("" + data[2]);

					if (statusType.equals(ATTRVAL_statusType_GENDERIZED)) {
						ImageButton btnTweet = (ImageButton) findViewById(R.id.imageButton_tweet);
						TextView tweetThis = (TextView) findViewById(R.id.textView_tweetthis);
						btnTweet.setVisibility(Button.VISIBLE);
						btnTweet.setEnabled(true);
						tweetThis.setVisibility(TextView.VISIBLE);
						tweetThis.setEnabled(true);
					}
				}
			} else if (statusType.equals(ATTRVAL_statusType_STOPPED)) {
				Button btn = (Button) findViewById(R.id.button_genderize);
				btn.setText(R.string.btn_genderize);
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
}
