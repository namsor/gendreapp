package com.namsor.api.samples.gendre;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.widget.Toast;

public class GenderizeTask extends IntentService {
	Handler mHandler = new Handler();
	public class DisplayToast implements Runnable {
	    private final Context mContext;
	    String mText;

	    public DisplayToast(Context mContext, String text){
	        this.mContext = mContext;
	        mText = text;
	    }

	    public void run(){
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
	private static final long SLEEPER_IN_API_ERROR = 60*SECONDS;

	private static final double GENDER_THRESHOLD = .1;

	private static final int GENDERSTYLE_DEFAULT = 2;
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

	private static final String PREFIX_NONE = "";

	private static final boolean WIPE_DEFAULT = false;
	private boolean wipe = WIPE_DEFAULT;
	
	// max number of moving error count 
	private static final int ERROR_COUNT_MOVING_MAX = 10;
	
	private static final String[][] GENDER_STYLES = {
			{ PREFIX_MS, PREFIX_MR, PREFIX_UNKNOWN },
			{ PREFIX_GENDERF, PREFIX_GENDERM, PREFIX_GENDERU },
			{ PREFIX_HEART, PREFIX_SPADE, PREFIX_DIAMOND },
			{ PREFIX_NONE, PREFIX_NONE, PREFIX_NONE }, };

	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id";
	private static final String ATTR_XLocale = "X-Locale";
	private long batchRequestId = -1;
	

	private static final Map<String, Double> CACHE = Collections
			.synchronizedMap(new HashMap());

	// private NotificationManager mNotifyManager;
	// private Builder mBuilder;

	private int[] genderizedCount;

	public GenderizeTask() {
		super("genderize");
		xLocale = Locale.getDefault().toString();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	// Defines a tag for identifying log entries
	private static final String TAG = "GenderizeTask";
	private static final int COMMIT_SIZE = 50;
	public static final String PARAM_OUT_MSG = "genderCount";
	public static final String PARAM_OUT_STATUS = "genderCountStatus";

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
			mHandler.post(new DisplayToast(this, "API Error "+e.getMessage()));
			// empty or null? should we retry?
			return null;
		}
	}

	private void wipe() {
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
		while (c.moveToNext()) {
			int i = 0;
			String rawContactId = c.getString(i++);
			String contactId = c.getString(i++);
			String dataId = c.getString(i++);
			String mimeType = c.getString(i++);
			String prefix = c.getString(i++);
			boolean prefixMatch = false;
			for (int j = 0; j < GENDER_STYLES.length & !prefixMatch; j++) {
				for (int j2 = 0; j2 < GENDER_STYLES[j].length & !prefixMatch; j2++) {
					if (prefix != null && !prefix.isEmpty()
							&& prefix.equals(GENDER_STYLES[j][j2])) {
						prefixMatch = true;
						break;
					}
				}
			}
			if (prefixMatch) {
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
								"").build());
			}
			iCount++;
		}
		c.close();
		commitOps(ops);

		genderizedCount = null;
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(PARAM_OUT_MSG, new int[3]);
		broadcastIntent.putExtra(PARAM_OUT_STATUS, true);
		sendBroadcast(broadcastIntent);
	}

	private boolean genderizeContacts() {
		if (wipe) {
			throw new IllegalStateException("wipe & genderize at the same time");
		}
		if (genderizedCount == null) {
			Intent broadcastIntent = new Intent();
			broadcastIntent
					.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
			broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
			int[] arr = { 0, 0, 1 };
			broadcastIntent.putExtra(PARAM_OUT_MSG, arr);
			sendBroadcast(broadcastIntent);
		}
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
												+ " IS NULL"), null, null);
		int cCount = c.getCount();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + cCount + " contacts");
		}
		if (genderizedCount == null) {
			genderizedCount = new int[3];
		}
		List<String[]> genderizeTodo = new ArrayList();
		while (c.moveToNext()) {
			int i = 0;
			String rawContactId = c.getString(i++);
			String contactId = c.getString(i++);
			String dataId = c.getString(i++);
			String mimeType = c.getString(i++);
			String prefix = c.getString(i++);
			String givenName = c.getString(i++);
			String familyName = c.getString(i++);

			if (prefix != null && !prefix.isEmpty() ) {
				// never override an existing prefix / phonetic name
				if (prefix.equals(GENDER_STYLES[genderStyle][0])) {
					genderizedCount[0]++;
				} else if (prefix.equals(GENDER_STYLES[genderStyle][1])) {
					genderizedCount[1]++;
				} else if (prefix.equals(GENDER_STYLES[genderStyle][2])) {
					genderizedCount[2]++;
				}
				Intent broadcastIntent = new Intent();
				broadcastIntent
						.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
				broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
				broadcastIntent.putExtra(PARAM_OUT_MSG, genderizedCount);
				sendBroadcast(broadcastIntent);
				continue;
			} else if (givenName != null && !givenName.isEmpty() && givenName.length() > 1 &&
					   familyName != null && !familyName.isEmpty() && familyName.length() > 1
						) {
				String[] todo = new String[3];
				todo[0] = rawContactId;
				todo[1] = givenName;
				todo[2] = familyName;
				genderizeTodo.add(todo);
			}
		}
		c.close();		
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + genderizeTodo.size()+ " contacts to genderize");
		}
		int iCount = 0;
		int errCount = 0;
		int errCountMoving = 0;
		for (String[] todo : genderizeTodo) {
			String rawContactId = todo[0];
			String firstName = todo[1];
			String lastName = todo[2];

			firstName = firstName.replace('/', ' ');
			lastName = lastName.replace('/', ' ');
			Double gender = genderize(firstName, lastName);
			if(gender != null) {
				iCount++;
				if( errCountMoving>0) {
					errCountMoving--;
				}
				String genderizedPrefix = GENDER_STYLES[genderStyle][2];
				if (Math.abs(gender) > GENDER_THRESHOLD) {
					genderizedPrefix = (gender > 0 ? GENDER_STYLES[genderStyle][0]
							: GENDER_STYLES[genderStyle][1]);
					if (gender > 0) {
						genderizedCount[0]++;
					} else {
						genderizedCount[1]++;
					}
				} else {
					genderizedCount[2]++;
				}
				ops.add(ContentProviderOperation
						.newUpdate(ContactsContract.Data.CONTENT_URI)
						.withSelection(
								ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
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
						.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
				broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
				broadcastIntent.putExtra(PARAM_OUT_MSG, genderizedCount);
				sendBroadcast(broadcastIntent);
				if( iCount % COMMIT_SIZE == 0) {
					commitOps(ops);
				}
			} else {
				errCount++;
				errCountMoving++;
				if( errCountMoving > ERROR_COUNT_MOVING_MAX ) {
					// no point continuing
					mHandler.post(new DisplayToast(this, "Too many API Errors ("+errCount+"/"+(iCount+errCount)+") check network"));
					break;
				}
			}
		}
		commitOps(ops);
		if( errCountMoving > ERROR_COUNT_MOVING_MAX ) {
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
		batchRequestId = PreferenceManager
				.getDefaultSharedPreferences(this).getLong("batchRequestId",
						-1);
		if( batchRequestId == -1 ) {
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
		
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Getting all contacts");
		}

		while (true) {
			if (wipe) {
				// reset counters
				genderizedCount = null;
				wipe();
				wipe = false;
				PreferenceManager.getDefaultSharedPreferences(this).edit()
						.putBoolean("example_checkbox", false).commit();
				// wipe contacts only once
				stopSelf();
				// now what?
				try {
					Thread.sleep(SECONDS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				boolean success = genderizeContacts();
				try {
					for (int i = 0; i < sleeper; i++) {
						Intent broadcastIntent = new Intent();
						broadcastIntent
								.setAction(MainActivity.ResponseReceiver.ACTION_RESP);
						broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
						broadcastIntent.putExtra(PARAM_OUT_MSG, genderizedCount);
						if( success ) {
							broadcastIntent.putExtra(PARAM_OUT_STATUS, true);
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
	}

	private void commitOps(ArrayList<ContentProviderOperation> ops) {
		if( ops == null || ops.size() == 0 ) {
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

}
