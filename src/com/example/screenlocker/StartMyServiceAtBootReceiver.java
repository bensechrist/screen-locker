package com.example.screenlocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			Intent serviceIntent = new Intent(context, ScreenLockerService.class);
			context.startService(serviceIntent);
			Log.i("Service at Boot", "started");
		}
	}
}
