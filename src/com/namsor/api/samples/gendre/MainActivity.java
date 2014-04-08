package com.namsor.api.samples.gendre;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Build;
import android.preference.PreferenceManager;

public class MainActivity extends ActionBarActivity {
	
	private boolean running = false;
	private ResponseReceiver receiver;
	private int[] genderStats;
	private static final String TWEET_URL = "http://namesorts.com/api/gendre";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		// launch preferences on first occurrence ?
		/*
		long batchRequestId = PreferenceManager.getDefaultSharedPreferences(this)
				.getLong("batchRequestId", -1);
		if (batchRequestId == -1) {
			Intent settingsIntent = new Intent(this,
					GendreSettingsActivity.class);
			startActivity(settingsIntent);
		}*/
		
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (!running) {
				Intent settingsIntent = new Intent(this,
						GendreSettingsActivity.class);
				startActivity(settingsIntent);
			}
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
		Intent mServiceIntent = new Intent(this, GenderizeTask.class);
		startService(mServiceIntent);
		running = true;
	}

	public void tweetThis(View view) {
		if( genderStats==null) {
			return;
		}
		String tweetText = "My Android contacts: "+genderStats[0]+GenderizeTask.PREFIX_GENDERF+" and "+genderStats[1]+GenderizeTask.PREFIX_GENDERM+" via @gendreapp ";
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
		public static final String ATTR_statusType = "statusType";
		public static final String ATTRVAL_statusType_GENDERIZING = "genderizing";
		public static final String ATTRVAL_statusType_GENDERIZED = "genderized";
		public static final String ATTRVAL_statusType_WIPING = "wiping";
		public static final String ATTRVAL_statusType_WIPED = "wiped";
		
		private MainActivity activity;
		public ResponseReceiver(MainActivity activity) {
			this.activity = activity;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String statusType = intent.getStringExtra(ATTR_statusType);			
			if (statusType.equals(ATTRVAL_statusType_GENDERIZED) || statusType.equals(ATTRVAL_statusType_GENDERIZING) || statusType.equals(ATTRVAL_statusType_WIPING)) {

				Button btn = (Button) findViewById(R.id.button_genderize);
				btn.setText(R.string.btn_genderize_running);
				btn.setEnabled(false);
				running = true;
				
				int[] data = intent.getIntArrayExtra(ATTR_genderCount);
				genderStats=data;				
				
				btn.setText(R.string.btn_genderize_running);
				
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
				
			} else if (statusType.equals(ATTRVAL_statusType_WIPED)) {
				Button btn = (Button) findViewById(R.id.button_genderize);
				btn.setText(R.string.btn_genderize_wiped);
				btn.setEnabled(false);
				int[] data = { 0, 0, 0 };
				genderStats=data;			
			}
		}
	};

}
