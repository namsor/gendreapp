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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.namsor.api.samples.gendre.MainActivity.ResponseReceiver;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class GenderizeTask extends IntentService {
	// need to manually change this before releasing
	private static final String ATTVALUE_ClientAppVersion = "GendRE_app_v0.0.7"; //$NON-NLS-1$

	private static final String CONTACT_GROUP_ALL_FEMALES = Messages.getString("GenderizeTask.contact_group_females"); //$NON-NLS-1$
	private static final String CONTACT_GROUP_ALL_MALES = Messages.getString("GenderizeTask.contact_group_males"); //$NON-NLS-1$
	private static final String CONTACT_GROUP_OTHERS = Messages.getString("GenderizeTask.contact_group_other"); //$NON-NLS-1$
	private static final String[] CONTACT_GROUPS = {
		CONTACT_GROUP_ALL_FEMALES,
		CONTACT_GROUP_ALL_MALES,
		CONTACT_GROUP_OTHERS};
	
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
	private final String xCountry;
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

	private static final String PREFIX_MS = "Ms."; //$NON-NLS-1$
	private static final String PREFIX_MR = "Mr."; //$NON-NLS-1$
	private static final String PREFIX_UNKNOWN = "M."; //$NON-NLS-1$

	public static final String PREFIX_GENDERF = "♀"; //$NON-NLS-1$
	public static final String PREFIX_GENDERM = "♂"; //$NON-NLS-1$
	private static final String PREFIX_GENDERU = "∅"; //$NON-NLS-1$

	private static final String PREFIX_HEART = "♥"; //$NON-NLS-1$
	private static final String PREFIX_SPADE = "♤"; //$NON-NLS-1$
	private static final String PREFIX_DIAMOND = "♢"; //$NON-NLS-1$

	private static final String PREFIX_CHINAF = "女"; //$NON-NLS-1$
	private static final String PREFIX_CHINAM = "男"; //$NON-NLS-1$
	private static final String PREFIX_CHINAU = "另"; // check this //$NON-NLS-1$

	private static final String PREFIX_CUSTOMF = "%f"; //$NON-NLS-1$
	private static final String PREFIX_CUSTOMM = "%m"; //$NON-NLS-1$
	private static final String PREFIX_CUSTOMU = "%u"; // check this //$NON-NLS-1$

	private static final String PREFIX_NONE = ""; //$NON-NLS-1$

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

	private static final String ATTR_XBatchRequest = "X-BatchRequest-Id"; //$NON-NLS-1$
	private static final String ATTR_XLocale = "X-Locale"; //$NON-NLS-1$
	private static final String ATTR_XHint = "X-Hint"; //$NON-NLS-1$
	private static final String ATTR_XClientVersion = "X-Client-Version"; //$NON-NLS-1$
	private long batchRequestId = -1;

	private boolean stopRequested = false;
	private static final Map<String, Double> CACHE = Collections
			.synchronizedMap(new HashMap());

	// private NotificationManager mNotifyManager;
	// private Builder mBuilder;

	private int[] genderizedCount;

	public GenderizeTask() {
		super("genderize"); //$NON-NLS-1$
		xLocale = Locale.getDefault().toString();
		xCountry = Locale.getDefault().getCountry();
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
	private static final String TAG = "GenderizeTask"; //$NON-NLS-1$
	private static final int COMMIT_SIZE = 50;

	/**
	 * Predict Gender from a Personal name
	 * 
	 * @param firstName
	 * @param lastName
	 * @return
	 */
	public Double genderize(String firstName, String lastName, String hint) {
		Double result = CACHE.get(firstName + "/" + lastName); //$NON-NLS-1$
		if (result != null) {
			return result;
		}

		try {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Contact firstName " + firstName + " lastName " //$NON-NLS-1$ //$NON-NLS-2$
						+ lastName);
			}
			DefaultHttpClient httpclient = new DefaultHttpClient();
			String url = "http://api.onomatic.com/onomastics/api/gendre/" //$NON-NLS-1$
					+ URLEncoder.encode(firstName, "UTF-8") + "/" //$NON-NLS-1$ //$NON-NLS-2$
					+ URLEncoder.encode(lastName, "UTF-8"); //$NON-NLS-1$
			if (xCountry != null && xCountry.length() == 2) {
				url = url + "/" + xCountry; //$NON-NLS-1$
			}
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Getting " + url); //$NON-NLS-1$
			}
			HttpGet httpget = new HttpGet(url);
			httpget.addHeader(ATTR_XBatchRequest, "" + batchRequestId); //$NON-NLS-1$
			httpget.addHeader(ATTR_XLocale, "" + xLocale); //$NON-NLS-1$
			if(hint!=null&&!hint.trim().isEmpty()) {
				httpget.addHeader(ATTR_XHint, hint);
			}
			httpget.addHeader(ATTR_XClientVersion, ATTVALUE_ClientAppVersion);
			HttpResponse response = httpclient.execute(httpget);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			String line = in.readLine();
			if (line != null && !line.trim().equals("")) { //$NON-NLS-1$
				result = Double.parseDouble(line);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Got " + result + 0 + " at " + url); //$NON-NLS-1$ //$NON-NLS-2$
				}
				CACHE.put(firstName + "/" + lastName, result); //$NON-NLS-1$
			}
			in.close();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Failed to get gender for firstName " + firstName //$NON-NLS-1$
					+ " lastName " + lastName + " ex=" + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			// display error msg
			mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.api_error") + e.getMessage())); //$NON-NLS-1$
			// empty or null? should we retry?
			return null;
		}
	}
	
	private void wipeGroups() {
		List<Long> groupsToWipe = new ArrayList();
		for (String groupTitle : CONTACT_GROUPS ) {
		    final Cursor cursor = getContentResolver().query(ContactsContract.Groups.CONTENT_URI, new String[] { ContactsContract.Groups._ID },
		    		ContactsContract.Groups.TITLE + "=?", //$NON-NLS-1$
		            new String[] { groupTitle }, null);
		    while (cursor.moveToNext() ) {
		            Long groupId = cursor.getLong(0);
		            groupsToWipe.add(groupId);
		    }
		    cursor.close();
		}
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		for (Long groupId : groupsToWipe) {
		    String where = ContactsContract.Data.MIMETYPE+" = ? AND "+ ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+" = ? "; //$NON-NLS-1$ //$NON-NLS-2$
		    String[] params = { ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, ""+groupId}; //$NON-NLS-1$
			ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(where, params)
                    .build());					
		    			
		}
		commitOps(ops);
		for(Long groupId : groupsToWipe) {
		    getContentResolver().delete(
		            ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId), null, null);
			
		}
	}
	
	private void wipe() {
		// wipe groups 
		wipeGroups();
		
		genderizedCount = new int[3];
		mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.wiping_titles"))); //$NON-NLS-1$
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		Cursor c = getContentResolver()
				.query(ContactsContract.Data.CONTENT_URI,
						new String[] {
								Data.RAW_CONTACT_ID,
								Data.CONTACT_ID,
								Data._ID,
								Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.PREFIX
								},
						Data.MIMETYPE
								+ "='" //$NON-NLS-1$
								+ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
								+ "'" //$NON-NLS-1$
								+ " AND " //$NON-NLS-1$
								+ ContactsContract.CommonDataKinds.StructuredName.PREFIX
								+ " IS NOT NULL", //$NON-NLS-1$
						null,
						ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
		int cCount = c.getCount();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, Messages.getString("GenderizeTask.wiping_titles_part1") + cCount + Messages.getString("GenderizeTask.wiping_titles_part2")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int iCount = 0;
		List<Object[]> wipeTodo = new ArrayList();
		while (c.moveToNext() && !isStopRequested()) {
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
					if (isStopRequested()) {
						break;
					}
				}
				if (isStopRequested()) {
					break;
				}
			}
		}
		c.close();
		for (Object[] toWipe : wipeTodo) {
			if (isStopRequested()) {
				break;
			}
			ops.add(ContentProviderOperation
					.newUpdate(ContactsContract.Data.CONTENT_URI)
					.withSelection(
							ContactsContract.Data.RAW_CONTACT_ID + "=? AND " //$NON-NLS-1$
									+ ContactsContract.Data.MIMETYPE + "=?", //$NON-NLS-1$
							new String[] {
									(String) toWipe[0],
									ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE })
					.withValue(
							ContactsContract.CommonDataKinds.StructuredName.PREFIX,
							"").build()); //$NON-NLS-1$
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
		mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.wiped_titles_part1") + wipeTodo.size() //$NON-NLS-1$
				+ Messages.getString("GenderizeTask.wiped_titles_part2"))); //$NON-NLS-1$
	}

	private List<String[]> facebookContacts() {
		boolean includeFacebook = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"read_facebook", false); //$NON-NLS-1$
		if( ! includeFacebook ) {
			return null;
		}

		Session session = Session.getActiveSession();
		if (session.isOpened()) {
			mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.toast_get_facebook_contacts"))); //$NON-NLS-1$
		} else {
			mHandler.post(new DisplayToast(this,
					Messages.getString("GenderizeTask.toast_skip_fb_contacts"))); //$NON-NLS-1$
			return null;
		}
		List<String[]> facebookContacts = new ArrayList();
		try {
			String fqlQuery = "SELECT uid, name, sex FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 = me())"; //$NON-NLS-1$
			Bundle params = new Bundle();
			params.putString("q", fqlQuery); //$NON-NLS-1$

			Request request = new Request(session, "/fql", params, //$NON-NLS-1$
					HttpMethod.GET);
			Response response = Request.executeAndWait(request);
			GraphObject graphObject = response.getGraphObject();
			if (graphObject != null) {
				// clear contacts
				JSONObject jsonObject = graphObject.getInnerJSONObject();
				JSONArray jsonArray = jsonObject.getJSONArray("data"); //$NON-NLS-1$
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
					String userFriendId = object.getString("uid"); //$NON-NLS-1$
					String userFriendName = object.getString("name"); //$NON-NLS-1$
					String userFriendSex = object.getString("sex"); //$NON-NLS-1$
					String[] fbContact = { userFriendId, userFriendName,
							userFriendSex };
					facebookContacts.add(fbContact);
				}
				mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.got_facebook_contacts_part1") //$NON-NLS-1$
						+ jsonArray.length() + Messages.getString("GenderizeTask.got_facebook_contacts_part2"))); //$NON-NLS-1$
			}
			
			// try reading facebook just once
			PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putBoolean("read_facebook", false).commit(); //$NON-NLS-1$
			return facebookContacts;
		} catch (Exception ex) {
			String msg = Messages.getString("GenderizeTask.toast_error_getting_fb_contacts")+ex.getMessage(); //$NON-NLS-1$
			Log.e(TAG,msg,ex);
			mHandler.post(new DisplayToast(this, msg));
		}
		// for now close and clear
		// session.closeAndClearTokenInformation();
		return null;
	}
	
	private void emulateGenderize(List<GenderizeTodo> genderizeTodo, List<String[]> facebookContacts) {
		for (String[] fbContact : facebookContacts) {
			String contactName = fbContact[1];
			String[] nameData = contactName.split(" "); //$NON-NLS-1$
			String firstName = contactName;
			String lastName = xCountry;
			if( nameData.length == 2 ) {
				firstName = nameData[0];
				lastName = nameData[1];
			}
			String hint = fbContact[2];
			GenderizeTodo todo = new GenderizeTodo(null, firstName, lastName, hint, null, null);
			genderizeTodo.add(todo);
		}
	}
	
	private class GenderizeTodo {
		public String getRawContactID() {
			return rawContactID;
		}
		public String getFirstName() {
			return firstName;
		}
		public String getLastName() {
			return lastName;
		}
		public String getHint() {
			return hint;
		}
		public String getAccountName() {
			return accountName;
		}
		public String getAccountType() {
			return accountType;
		}
		public GenderizeTodo(String rawContactID, String firstName,
				String lastName, String hint, String accountName,
				String accountType) {
			super();
			this.rawContactID = rawContactID;
			this.firstName = firstName;
			this.lastName = lastName;
			this.hint = hint;
			this.accountName = accountName;
			this.accountType = accountType;
		}
		final String rawContactID;
		final String firstName;
		final String lastName;
		final String hint;
		final String accountName;
		final String accountType;
	}
	
	private Map<String, Long> groupsCache = new HashMap();
	
	private Long findOrCreateGroup(GenderizeTodo todo, int gender) {
		String groupKey = todo.getAccountType()+"/"+todo.getAccountName()+"/"+gender; //$NON-NLS-1$ //$NON-NLS-2$
		Long groupId = groupsCache.get(groupKey);
		if( groupId == null ) {
			String groupTitle = CONTACT_GROUPS[gender];
			// create group
		    final Cursor cursor = getContentResolver().query(ContactsContract.Groups.CONTENT_URI, new String[] { ContactsContract.Groups._ID },
		    		ContactsContract.Groups.ACCOUNT_NAME + "=? AND " + ContactsContract.Groups.ACCOUNT_TYPE + "=? AND " + //$NON-NLS-1$ //$NON-NLS-2$
		    				ContactsContract.Groups.TITLE + "=?", //$NON-NLS-1$
		            new String[] { todo.getAccountName(), todo.getAccountType(), groupTitle }, null);
		    if (cursor != null) {
		        try {
		            if (cursor.moveToFirst()) {
		                groupId = cursor.getLong(0);
		            }
		        } finally {
		            cursor.close();
		        }
		    }
			if( groupId == null ) {
				final ContentValues contentValues = new ContentValues();
		        contentValues.put(ContactsContract.Groups.ACCOUNT_NAME, todo.getAccountName());
		        contentValues.put(ContactsContract.Groups.ACCOUNT_TYPE, todo.getAccountType());
		        contentValues.put(ContactsContract.Groups.TITLE, groupTitle);

		        final Uri newGroupUri = getContentResolver().insert(ContactsContract.Groups.CONTENT_URI, contentValues);
		        groupId = ContentUris.parseId(newGroupUri);							
			}
			groupsCache.put(groupKey, groupId);
			return groupId;
		} else {
			return groupId;			
		}
	}

	private boolean genderizeContacts() {
		if (wipe) {
			throw new IllegalStateException("wipe & genderize at the same time"); //$NON-NLS-1$
		}
		List<GenderizeTodo> genderizeTodo = new ArrayList();
		
		List<String[]> facebookContacts = null;
		if (isAppInstalled("com.facebook.katana") ) { //$NON-NLS-1$
			// try and get FB contacts
			facebookContacts = facebookContacts();
			if( facebookContacts!=null ) {
				emulateGenderize(genderizeTodo, facebookContacts);
			}
		}

		// Set<String> namesUnique = new HashSet();
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
								ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
								ContactsContract.RawContacts.ACCOUNT_NAME,
								ContactsContract.RawContacts.ACCOUNT_TYPE,
						},
						Data.MIMETYPE
								+ "='" //$NON-NLS-1$
								+ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
								+ "'" //$NON-NLS-1$
								+ " AND " //$NON-NLS-1$
								+ ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME
								+ " IS NOT NULL " //$NON-NLS-1$
								+ (genderizedCount == null ? "" //$NON-NLS-1$
										: " AND " //$NON-NLS-1$
												+ ContactsContract.CommonDataKinds.StructuredName.PREFIX
												+ " IS NULL"), //$NON-NLS-1$
						null,
						ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);

		int cCount = c.getCount();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + cCount + " android contacts"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (genderizedCount == null) {
			genderizedCount = new int[3];
		}
		while (c.moveToNext() && !isStopRequested()) {
			int i = 0;
			String rawContactId = c.getString(i++);
			String contactId = c.getString(i++);
			String dataId = c.getString(i++);
			String mimeType = c.getString(i++);
			String prefix = c.getString(i++);
			String givenName = c.getString(i++);
			String familyName = c.getString(i++);
			String accountName = c.getString(i++);
			String accountType = c.getString(i++);
			if (prefix != null && !prefix.isEmpty()) {
				// && !namesUnique.contains(givenName+" "+familyName) ) {

				// never override an existing prefix / phonetic name
				if (prefix.equals(GENDER_STYLES[genderStyle][0])) {
					genderizedCount[0]++;
					// namesUnique.add(givenName+" "+familyName);
				} else if (prefix.equals(GENDER_STYLES[genderStyle][1])) {
					genderizedCount[1]++;
					// namesUnique.add(givenName+" "+familyName);
				} else if (prefix.equals(GENDER_STYLES[genderStyle][2])) {
					genderizedCount[2]++;
					// namesUnique.add(givenName+" "+familyName);
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
				// no hint, but idea to use later existing title when WIPE=true
				GenderizeTodo todo = new GenderizeTodo(rawContactId, givenName, familyName, null, accountName, accountType);
				genderizeTodo.add(todo);
			}
		}
		c.close();
		if (BuildConfig.DEBUG && cCount > 0) {
			Log.d(TAG, "Got " + genderizeTodo.size() + " contacts to genderize"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int iCount = 0;
		int errCount = 0;
		int errCountMoving = 0;
		Collections.shuffle(genderizeTodo);
		for (GenderizeTodo todo : genderizeTodo) {
			if (isStopRequested()) {
				break;
			}
			String rawContactId = todo.getRawContactID();
			String firstName = todo.getFirstName();
			String lastName = todo.getLastName();
			String hint = todo.getHint();
			firstName = firstName.replace('/', ' ');
			lastName = lastName.replace('/', ' ');
			Double gender = genderize(firstName, lastName, hint);
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
						// if( !namesUnique.contains(firstName+" "+lastName) ) {
						genderizedCount[0]++;
						// namesUnique.add(firstName+" "+lastName);
						// }
						genderIndex = 0;
					} else {
						// if( !namesUnique.contains(firstName+" "+lastName) ) {
						genderizedCount[1]++;
						// namesUnique.add(firstName+" "+lastName);
						// }
						genderIndex = 1;
					}
				} else {
					// if( !namesUnique.contains(firstName+" "+lastName) ) {
					genderizedCount[2]++;
					// namesUnique.add(firstName+" "+lastName);
					// }
					genderIndex = 2;
				}
				if( rawContactId!= null ) { // facebook contacts not saved for now
					ops.add(ContentProviderOperation
							.newUpdate(ContactsContract.Data.CONTENT_URI)
							.withSelection(
									ContactsContract.Data.RAW_CONTACT_ID
											+ "=? AND " //$NON-NLS-1$
											+ ContactsContract.Data.MIMETYPE + "=?", //$NON-NLS-1$
									new String[] {
											rawContactId,
											ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE })
							.withValue(
									ContactsContract.CommonDataKinds.StructuredName.PREFIX,
									genderizedPrefix).build());
					// group membership
					Long groupId = findOrCreateGroup(todo, genderIndex);
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
	                        .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
	                        .build());					
				}				

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
				if (genderIndex < 2) {
					String[] genderSample = new String[4];
					genderSample[0] = firstName;
					genderSample[1] = lastName;
					genderSample[2] = genderizedPrefix;
					genderSample[3] = "" + genderIndex; //$NON-NLS-1$
					broadcastIntent.putExtra(
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
							Messages.getString("GenderizeTask.toast_toomany_api_errors") + errCount + "/" //$NON-NLS-1$ //$NON-NLS-2$
									+ (iCount + errCount) + ") check network")); //$NON-NLS-1$
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
				.getLong("batchRequestId", -1); //$NON-NLS-1$
		if (batchRequestId == -1) {
			batchRequestId = System.currentTimeMillis();
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putLong("batchRequestId", batchRequestId).commit(); //$NON-NLS-1$
		}
		genderStyle = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(this).getString("example_list", //$NON-NLS-1$
						"2")); //$NON-NLS-1$
		sleeper = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(
				this).getString("sync_frequency", "60")) //$NON-NLS-1$ //$NON-NLS-2$
				* SECONDS;
		wipe = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"example_checkbox", false); //$NON-NLS-1$

		mHandler.post(new DisplayToast(this, Messages.getString("GenderizeTask.toast_starting_genderizing"))); //$NON-NLS-1$

		String genderStyleF = PreferenceManager.getDefaultSharedPreferences(
				this).getString("custom_f", "Ms."); //$NON-NLS-1$ //$NON-NLS-2$
		String genderStyleM = PreferenceManager.getDefaultSharedPreferences(
				this).getString("custom_m", "Mr."); //$NON-NLS-1$ //$NON-NLS-2$
		String genderStyleU = PreferenceManager.getDefaultSharedPreferences(
				this).getString("custom_u", "M."); //$NON-NLS-1$ //$NON-NLS-2$

		GENDER_STYLES[GENDERSTYLE_CUSTOM][0] = genderStyleF;
		GENDER_STYLES[GENDERSTYLE_CUSTOM][1] = genderStyleM;
		GENDER_STYLES[GENDERSTYLE_CUSTOM][2] = genderStyleU;

		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Getting all contacts"); //$NON-NLS-1$
		}

		while (!isStopRequested()) {
			if (wipe) {
				// reset counters
				genderizedCount = null;
				wipe();
				wipe = false;
				PreferenceManager.getDefaultSharedPreferences(this).edit()
						.putBoolean("example_checkbox", false).commit(); //$NON-NLS-1$
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
					for (int i = 0; i < sleeper && !isStopRequested(); i++) {
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
		broadcastIntent.setAction(MainActivity.ResponseReceiver.ACTION_STATUS);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(MainActivity.ResponseReceiver.ATTR_statusType,
				MainActivity.ResponseReceiver.ATTRVAL_statusType_STOPPED);
		sendBroadcast(broadcastIntent);
	}

	private void commitOps(ArrayList<ContentProviderOperation> ops) {
		if (ops == null || ops.size() == 0) {
			return;
		}
		try {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Updating " + ops.size() + " contacts"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			ops.clear();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Updating " + ops.size() + " contacts"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Save failed"); //$NON-NLS-1$
			}
		} catch (OperationApplicationException e) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Save failed"); //$NON-NLS-1$
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
		public static final String ACTIVITY_STATUS = "com.namsor.api.samples.gendre.intent.action.ACTIVITY_STATUS"; //$NON-NLS-1$
		public static final String ATTR_statusType = "statusType"; //$NON-NLS-1$
		public static final String ATTRVAL_statusType_STOP_REQUEST = "stopRequested"; //$NON-NLS-1$

		private GenderizeTask service;

		public ActivityReceiver(GenderizeTask service) {
			this.service = service;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(ACTIVITY_STATUS)) {
				return;
			}
			String statusType = intent.getStringExtra(ATTR_statusType);
			if (statusType.equals(ATTRVAL_statusType_STOP_REQUEST)) {
				mHandler.post(new DisplayToast(service, Messages.getString("GenderizeTask.toast_stopping"))); //$NON-NLS-1$
				setStopRequested(true);
			}
		}
	};

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
}
