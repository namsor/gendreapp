package com.namsor.api.samples.gendre;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.namsor.api.samples.gendre.MainActivity.ResponseReceiver;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class GenderizeTask extends IntentService {
	Handler mHandler = new Handler();

	public class DisplayToast implements Runnable {
		private final Context mContext;
		String mText;

		public DisplayToast(Context mContext, String text) {
			this.mContext = mContext;
			mText = text;
		}

		public void run() {
			Toast.makeText(mContext, mText, Toast.LENGTH_LONG).show();
		}
	}

	// private static final Random RND = new Random();
	private final String xLocale;
	private static final long SECONDS = 1000;
	private static final long SLEEPER_DEFAULT = 10;
	// sleeper : in seconds
	private long sleeper = SLEEPER_DEFAULT;
	// sleeper in case network / API is down (in ms)
	private static final long SLEEPER_IN_API_ERROR = 60 * SECONDS;

	private static final double GENDER_THRESHOLD = .1;

	public static final int GENDERSTYLE_DEFAULT = 2;
	public static final int GENDERSTYLE_CUSTOM = 4;
	public static final int GENDERSTYLE_NONE = 5;
	private int genderStyle = GENDERSTYLE_DEFAULT;

	private static final String PREFIX_MS = "Ms.";
	private static final String PREFIX_MR = "Mr.";
	private static final String PREFIX_UNKNOWN = "M.";

	public static final String PREFIX_GENDERF = "♀";
	public static final String PREFIX_GENDERM = "♂";
	private static final String PREFIX_GENDERU = "∅";

	private static final String PREFIX_HEART = "♥";
	private static final String PREFIX_SPADE = "♤";
	private static final String PREFIX_DIAMOND = "♢";

	private static final String PREFIX_CHINAF = "女";
	private static final String PREFIX_CHINAM = "男";
	private static final String PREFIX_CHINAU = "另"; // check this

	private static final String PREFIX_CUSTOMF = "%f";
	private static final String PREFIX_CUSTOMM = "%m";
	private static final String PREFIX_CUSTOMU = "%u"; // check this
	
	private static final String PREFIX_NONE = "";

	private static final boolean WIPE_DEFAULT = false;
	private boolean wipe = WIPE_DEFAULT;

	// max number of moving error count
	private static final int ERROR_COUNT_MOVING_MAX = 10;	

	private static final String[][] GENDER_STYLES = {
			{ PREFIX_MS, PREFIX_MR, PREFIX_UNKNOWN },
			{ PREFIX_GENDERF, PREFIX_GENDERM, PREFIX_GENDERU },
			{ PREFIX_HEART, PREFIX_SPADE, PREFIX_DIAMOND },
			{ PREFIX_CHINAF, PREFIX_CHINAM, PREFIX_CHINAU },
			{ PREFIX_CUSTOMF, PREFIX_CUSTOMM, PREFIX_CUSTOMU },
			{ PREFIX_NONE, PREFIX_NONE, PREFIX_NONE }, };

	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id";
	private static final String ATTR_XLocale = "X-Locale";
	private long batchRequestId = -1;

	private boolean stopRequested = false;
	private static final Map<String, Double> CACHE = Collections
			.synchronizedMap(new HashMap());

	// private NotificationManager mNotifyManager;
	// private Builder mBuilder;

	private int[] genderizedCount;

	public GenderizeTask() {
		super("genderize");
		xLocale = Locale.getDefault().toString();
	}

	private ActivityReceiver receiver;
	
	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter(ActivityReceiver.ACTIVITY_STATUS);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ActivityReceiver(this);
		registerReceiver(receiver, filter);
		
	}
	

	
	@Override
	public void onDestroy() {
	    unregisterReceiver(receiver);
		super.onDestroy();
	}

	// Defines a tag for identifying log entries
	private static final String TAG = "GenderizeTask";
	private static final int COMMIT_SIZE = 50;

	/**
	 * Predict Gender from a Personal name
	 * 
	 * @param firstName
	 * @param lastName
	 * @return
	 */
	public Double genderize(String firstName, String lastName) {
		Double result = CACHE.get(firstName + "/" + lastName);
		if (result != null) {
			return result;
		}

		try {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Contact firstName " + firstName + " lastName "
						+ lastName);
			}
			DefaultHttpClient httpclient = new DefaultHttpClient();
			String url = "http://api.onomatic.com/onomastics/api/gendre/"
					+ URLEncoder.encode(firstName, "UTF-8") + "/"
					+ URLEncoder.encode(lastName, "UTF-8");
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Getting " + url);
			}
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader(ATTR_XBatchRequest, "" + batchRequestId);
			httpget.addHeader(ATTR_XLocale, "" + xLocale);

			HttpResponse response = httpclient.execute(httpget);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = in.readLine();
			if (line != null && !line.trim().equals("")) {
				result = Double.parseDouble(line);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Got " + result + 0 + " at " + url);
				}
				CACHE.put(firstName + "/" + lastName, result);
			}
			in.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Failed to get gender for firstName " + firstName
					+ " lastName " + lastName + " ex=" + e.getMessage());
			// display error msg
			mHandler.post(new DisplayToast(this, "API Error " + e.getMessage()));
			// empty or null? should we retry?
			return null;
		}
	}

	private void wipe() {
		genderizedCount = new int[3];
		mHandler.post(new DisplayToast(this, "Wiping ..."));
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		Cursor c = getContentResolver()
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
								Data.RAW_CONTACT_ID,
								Data.CONTACT_ID,
								Data._ID,
								Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.PREFIX,
								ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
								ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME },
						Data.MIMETYPE
								+ "='"
								+ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
								+ "'", null, null);
		int cCount = c.getCount();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Wiping " + cCount + " contacts");
		}
		int iCount = 0;
		List<Object[]> wipeTodo = new ArrayList();
		while (c.moveToNext() && !isStopRequested() ) {
			int i = 0;
			String rawContactId = c.getString(i++);
			String contactId = c.getString(i++);
			String dataId = c.getString(i++);
			String mimeType = c.getString(i++);
			String prefix = c.getString(i++);
			for (int j = 0; j < GENDER_STYLES.length; j++) {
				for (int j2 = 0; j2 < GENDER_STYLES[j].length; j2++) {
					if (prefix != null && !prefix.isEmpty()
							&& prefix.equals(GENDER_STYLES[j][j2])) {

						genderizedCount[j2]++;
						Intent broadcastIntent = new Intent();
						broadcastIntent
								.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
						broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
						broadcastIntent.putExtra(
								MainActivity.ResponseReceiver.ATTR_genderCount,
								genderizedCount);
						broadcastIntent
								.putExtra(
										MainActivity.ResponseReceiver.ATTR_statusType,
										MainActivity.ResponseReceiver.ATTRVAL_statusType_COUNTING);
						sendBroadcast(broadcastIntent);

						Object[] toWipe = { rawContactId, j2 };
						wipeTodo.add(toWipe);
						break;
					}
					if( isStopRequested() ) {
						break;
					}
				}
				if( isStopRequested() ) {
					break;
				}
			}
		}
		c.close();
		for (Object[] toWipe : wipeTodo) {
			if( isStopRequested() ) {
				break;
			}
			ops.add(ContentProviderOperation
					.newUpdate(ContactsContract.Data.CONTENT_URI)
					.withSelection(
							ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
									+ ContactsContract.Data.MIMETYPE + "=?",
							new String[] {
									(String) toWipe[0],
									ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE })
					.withValue(
							ContactsContract.CommonDataKinds.StructuredName.PREFIX,
							"").build());
			genderizedCount[(Integer) toWipe[1]]--;
			Intent broadcastIntent = new Intent();
			broadcastIntent
					.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			broadcastIntent.putExtra(
					MainActivity.ResponseReceiver.ATTR_genderCount,
					genderizedCount);
			broadcastIntent.putExtra(
					MainActivity.ResponseReceiver.ATTR_statusType,
					MainActivity.ResponseReceiver.ATTRVAL_statusType_WIPING);
			sendBroadcast(broadcastIntent);

			iCount++;
			if (iCount % COMMIT_SIZE == 0) {
				commitOps(ops);
			}
		}
		commitOps(ops);

		genderizedCount = null;
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent
				.putExtra(MainActivity.ResponseReceiver.ATTR_genderCount,
						genderizedCount);
		broadcastIntent.putExtra(MainActivity.ResponseReceiver.ATTR_statusType,
				MainActivity.ResponseReceiver.ATTRVAL_statusType_WIPED);
		sendBroadcast(broadcastIntent);
		mHandler.post(new DisplayToast(this, "Wiped " + wipeTodo.size()
				+ " titles."));
	}

	private boolean genderizeContacts() {
		if (wipe) {
			throw new IllegalStateException("wipe & genderize at the same time");
		}
		//Set<String> namesUnique = new HashSet();
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		Cursor c = getContentResolver()
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
								Data.RAW_CONTACT_ID,
								Data.CONTACT_ID,
								Data._ID,
								Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.PREFIX,
								ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
								ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME },
						Data.MIMETYPE
								+ "='"
								+ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
								+ "'"
								+ (genderizedCount == null ? ""
										: " AND "
												+ ContactsContract.CommonDataKinds.StructuredName.PREFIX
												+ " IS NULL"), null, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
		int cCount = c.getCount();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + cCount + " contacts");
		}
		if (genderizedCount == null) {
			genderizedCount = new int[3];
		}
		List<String[]> genderizeTodo = new ArrayList();
		while (c.moveToNext() && !isStopRequested()) {
			int i = 0;
			String rawContactId = c.getString(i++);
			String contactId = c.getString(i++);
			String dataId = c.getString(i++);
			String mimeType = c.getString(i++);
			String prefix = c.getString(i++);
			String givenName = c.getString(i++);
			String familyName = c.getString(i++);

			if (prefix != null && !prefix.isEmpty() ) {
					//&& !namesUnique.contains(givenName+" "+familyName) ) {
				
				// never override an existing prefix / phonetic name
				if (prefix.equals(GENDER_STYLES[genderStyle][0])) {
					genderizedCount[0]++;
					//namesUnique.add(givenName+" "+familyName);
				} else if (prefix.equals(GENDER_STYLES[genderStyle][1])) {
					genderizedCount[1]++;
					//namesUnique.add(givenName+" "+familyName);					
				} else if (prefix.equals(GENDER_STYLES[genderStyle][2])) {
					genderizedCount[2]++;
					//namesUnique.add(givenName+" "+familyName);
				}
				Intent broadcastIntent = new Intent();
				broadcastIntent
						.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
				broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
				broadcastIntent.putExtra(
						MainActivity.ResponseReceiver.ATTR_genderCount,
						genderizedCount);
				broadcastIntent
						.putExtra(
								MainActivity.ResponseReceiver.ATTR_statusType,
								MainActivity.ResponseReceiver.ATTRVAL_statusType_COUNTING);
				sendBroadcast(broadcastIntent);
				continue;
			} else if (givenName != null && !givenName.isEmpty()
					&& givenName.length() > 1 && familyName != null
					&& !familyName.isEmpty() && familyName.length() > 1) {
				String[] todo = new String[3];
				todo[0] = rawContactId;
				todo[1] = givenName;
				todo[2] = familyName;
				genderizeTodo.add(todo);
			}
		}
		c.close();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + genderizeTodo.size() + " contacts to genderize");
		}
		int iCount = 0;
		int errCount = 0;
		int errCountMoving = 0;
		Collections.shuffle(genderizeTodo);
		for (String[] todo : genderizeTodo) {
			if( isStopRequested() ) {
				break;
			}			
			String rawContactId = todo[0];
			String firstName = todo[1];
			String lastName = todo[2];

			firstName = firstName.replace('/', ' ');
			lastName = lastName.replace('/', ' ');
			Double gender = genderize(firstName, lastName);
			if (gender != null) {
				iCount++;
				if (errCountMoving > 0) {
					errCountMoving--;
				}
				String genderizedPrefix = GENDER_STYLES[genderStyle][2];
				int genderIndex = 2;
				if (Math.abs(gender) > GENDER_THRESHOLD) {
					genderizedPrefix = (gender > 0 ? GENDER_STYLES[genderStyle][0]
							: GENDER_STYLES[genderStyle][1]);
					if (gender > 0) {
						//if( !namesUnique.contains(firstName+" "+lastName) ) {
							genderizedCount[0]++;
							//namesUnique.add(firstName+" "+lastName);
						//}
						genderIndex = 0;
					} else {
						//if( !namesUnique.contains(firstName+" "+lastName) ) {
							genderizedCount[1]++;
							//namesUnique.add(firstName+" "+lastName);
						//}
						genderIndex = 1;
					}
				} else {
					//if( !namesUnique.contains(firstName+" "+lastName) ) {
						genderizedCount[2]++;
						//namesUnique.add(firstName+" "+lastName);
					//}
					genderIndex = 2;
				}
				ops.add(ContentProviderOperation
						.newUpdate(ContactsContract.Data.CONTENT_URI)
						.withSelection(
								ContactsContract.Data.RAW_CONTACT_ID
										+ "=? AND "
										+ ContactsContract.Data.MIMETYPE + "=?",
								new String[] {
										rawContactId,
										ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE })
						.withValue(
								ContactsContract.CommonDataKinds.StructuredName.PREFIX,
								genderizedPrefix).build());

				// processing done here
				Intent broadcastIntent = new Intent();
				broadcastIntent
						.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
				broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
				broadcastIntent.putExtra(
						MainActivity.ResponseReceiver.ATTR_genderCount,
						genderizedCount);
				broadcastIntent
						.putExtra(
								MainActivity.ResponseReceiver.ATTR_statusType,
								MainActivity.ResponseReceiver.ATTRVAL_statusType_GENDERIZING);
				if( genderIndex < 2) {
					String[] genderSample = new String[4];
					genderSample[0] = firstName;
					genderSample[1] = lastName;
					genderSample[2] = genderizedPrefix;
					genderSample[3] = ""+genderIndex;
					broadcastIntent
					.putExtra(
							MainActivity.ResponseReceiver.ATTR_genderSample,
							genderSample);					
				}
				sendBroadcast(broadcastIntent);
				if (iCount % COMMIT_SIZE == 0) {
					commitOps(ops);
				}
			} else {
				errCount++;
				errCountMoving++;
				if (errCountMoving > ERROR_COUNT_MOVING_MAX) {
					// no point continuing
					mHandler.post(new DisplayToast(this,
							"Too many API Errors (" + errCount + "/"
									+ (iCount + errCount) + ") check network"));
					break;
				}
			}
		}
		commitOps(ops);
		if (errCountMoving > ERROR_COUNT_MOVING_MAX) {
			// sleep an additional time
			try {
				Thread.sleep(SLEEPER_IN_API_ERROR);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		} else {
			return true;
		}
	}

	protected void onHandleIntent(Intent arg0) {
		// TODO Auto-generated method stub
		batchRequestId = PreferenceManager.getDefaultSharedPreferences(this)
				.getLong("batchRequestId", -1);
		if (batchRequestId == -1) {
			batchRequestId = System.currentTimeMillis();
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putLong("batchRequestId", batchRequestId).commit();
		}
		genderStyle = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(this).getString("example_list",
						"2"));
		sleeper = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(
				this).getString("sync_frequency", "60"))
				* SECONDS;
		wipe = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"example_checkbox", false);

		mHandler.post(new DisplayToast(this, "Starting genderizing"));

		
		String genderStyleF = PreferenceManager
				.getDefaultSharedPreferences(this).getString("custom_f",
						"Ms.");
		String genderStyleM = PreferenceManager
				.getDefaultSharedPreferences(this).getString("custom_m",
						"Mr.");
		String genderStyleU = PreferenceManager
				.getDefaultSharedPreferences(this).getString("custom_u",
						"M.");
		
		GENDER_STYLES[GENDERSTYLE_CUSTOM][0] = genderStyleF;
		GENDER_STYLES[GENDERSTYLE_CUSTOM][1] = genderStyleM;
		GENDER_STYLES[GENDERSTYLE_CUSTOM][2] = genderStyleU;
		
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Getting all contacts");
		}

		while (!isStopRequested()) {
			if (wipe) {
				// reset counters
				genderizedCount = null;
				wipe();
				wipe = false;
				PreferenceManager.getDefaultSharedPreferences(this).edit()
						.putBoolean("example_checkbox", false).commit();
				// wipe contacts only once
				if (genderStyle == GENDERSTYLE_NONE) {
					// wipe & none : stop servicing
					setStopRequested(true);
				} else {
					// now what?
					try {
						Thread.sleep(SECONDS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (genderStyle != GENDERSTYLE_NONE) {
				boolean success = genderizeContacts();
				try {
					for (int i = 0; i < sleeper && !isStopRequested() ; i++) {
						Intent broadcastIntent = new Intent();
						broadcastIntent
								.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
						broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
						broadcastIntent.putExtra(
								MainActivity.ResponseReceiver.ATTR_genderCount,
								genderizedCount);
						if (success) {
							broadcastIntent
									.putExtra(
											MainActivity.ResponseReceiver.ATTR_statusType,
											MainActivity.ResponseReceiver.ATTRVAL_statusType_GENDERIZED);
						} else {
							broadcastIntent
									.putExtra(
											MainActivity.ResponseReceiver.ATTR_statusType,
											MainActivity.ResponseReceiver.ATTRVAL_statusType_GENDERIZING);

						}
						sendBroadcast(broadcastIntent);
						Thread.sleep(SECONDS);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		// service stopped
		Intent broadcastIntent = new Intent();
		broadcastIntent
				.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(
				MainActivity.ResponseReceiver.ATTR_statusType,
				MainActivity.ResponseReceiver.ATTRVAL_statusType_STOPPED);
		sendBroadcast(broadcastIntent);
	}

	private void commitOps(ArrayList<ContentProviderOperation> ops) {
		if (ops == null || ops.size() == 0) {
			return;
		}
		try {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Updating " + ops.size() + " contacts");
			}
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			ops.clear();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Updating " + ops.size() + " contacts");
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Save failed");
			}
		} catch (OperationApplicationException e) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Save failed");
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	
	private synchronized boolean isStopRequested() {
		return stopRequested;
	}

	private synchronized void setStopRequested(boolean stopRequested) {
		this.stopRequested = stopRequested;
	}

	public class ActivityReceiver extends BroadcastReceiver {
		public static final String ACTIVITY_STATUS = "com.namsor.api.samples.gendre.intent.action.ACTIVITY_STATUS";
		public static final String ATTR_statusType = "statusType";
		public static final String ATTRVAL_statusType_STOP_REQUEST = "stopRequested";
		
		private GenderizeTask service;
		public ActivityReceiver(GenderizeTask service) {
			this.service = service;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(! intent.getAction().equals(ACTIVITY_STATUS) ) {
				return;
			}
			String statusType = intent.getStringExtra(ATTR_statusType);			
			if (statusType.equals(ATTRVAL_statusType_STOP_REQUEST)) {
				mHandler.post(new DisplayToast(service, "Stopping ..."));
				setStopRequested(true);				
			}
		}
	};
		
}
