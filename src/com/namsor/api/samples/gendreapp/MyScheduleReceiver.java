package com.namsor.api.samples.gendreapp;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyScheduleReceiver extends BroadcastReceiver {
	private static final String TAG = "MyScheduleReceiver"; //$NON-NLS-1$

	@Override
	public void onReceive(final Context context, Intent intent) {

		Intent mServiceIntent = new Intent(context, GenderizeTask.class);
		context.startService(mServiceIntent);

		/*
		 * Using AlarmMgr: didn't work AlarmManager service = (AlarmManager)
		 * context .getSystemService(Context.ALARM_SERVICE); PendingIntent
		 * pending = PendingIntent.getBroadcast(context, 0, mServiceIntent,
		 * PendingIntent.FLAG_ONE_SHOT); Calendar cal = Calendar.getInstance();
		 * cal.add(Calendar.SECOND, 30); service.set(AlarmManager.RTC,
		 * cal.getTimeInMillis(), pending);
		 */

	}
}
