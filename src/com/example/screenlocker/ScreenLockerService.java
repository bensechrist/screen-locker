package com.example.screenlocker;

import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.screenlocker.MainActivity.Controller;
import com.screenlocker.widget.MyWidgetProvider;

public class ScreenLockerService extends Service implements LocationListener {
	
	public static final String TOGGLE_ACTION = "com.screenlocker.toggleService";
	public static final int notiId = 13;
	public static final int requestCode = 13;
	
	private BroadcastReceiver toggleReceiver;
	private BroadcastReceiver addedZoneReceiver;
	private BroadcastReceiver widgetReceiver;
	private ZoneSQLiteAdder zAdder;
	private SharedPreferences prefs;
	private DevicePolicyManager DPM;
	private LocationManager locationManager;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 100, this);
		
		prefs = getSharedPreferences(Controller.sharedprefs_key, Controller.pref_mode);
		DPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		
		final IntentFilter filter = new IntentFilter();
		filter.addAction(TOGGLE_ACTION);
		toggleReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				
			}
		};
		registerReceiver(toggleReceiver, filter);
		
		IntentFilter widgetFilter = new IntentFilter();
		widgetFilter.addAction("com.screenlocker.widgetZoneAdded");
		widgetReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				zAdder = new ZoneSQLiteAdder(context);
				zAdder.open();
				int[] widgetIds = intent.getExtras().getIntArray("WidgetIds");
				Zone newZone = new Zone();
				newZone.setLatitude((long) 0);
				newZone.setLongitude((long) 0);
				Location location = new Location(LocationManager.NETWORK_PROVIDER);
				if(!zAdder.zoneExists(location)) {
					zAdder.addZone(newZone);
					Intent addedIntent = new Intent("com.screenlocker.addedZone");
					sendBroadcast(addedIntent);
					Intent updateIntent = new Intent(context, MyWidgetProvider.class);
					updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
				    sendBroadcast(updateIntent);
				}
				zAdder.close();
			}
		};
		registerReceiver(widgetReceiver, widgetFilter);
		
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction("com.screenlocker.addedZone");
		addedZoneReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("receiver", "received it");
				unlockScreen();
			}
		};
		registerReceiver(addedZoneReceiver, ifilter);
	}
	
	private void unlockScreen(){
		NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(getApplicationContext());
		notiBuilder.setContentTitle("Screen Locker").setContentText("Reenter Password at Lock Screen")
			.setSmallIcon(R.drawable.ic_launcher);
		NotificationManager notiMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notiMan.notify(notiId, notiBuilder.build());
		Editor editor = prefs.edit();
		editor.putBoolean(MainActivity.PREF_REQUIRE_REENTRY_PWD, true);
		editor.commit();
		Log.i("Unlock", "Set to reenter password");
		int pwd = prefs.getInt(Controller.pwd_key, 0);
		DPM.resetPassword(String.valueOf(pwd), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		Toast.makeText(getApplicationContext(), "Reenter Password at Lock Screen", Toast.LENGTH_SHORT).show();
	}
	
	private void setLockScreen(){
		Log.i("Lock Screen", "Screen locked");
		NotificationManager notiMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notiMan.cancelAll();
		Editor editor = prefs.edit();
		editor.putBoolean(MainActivity.PREF_REQUIRE_REENTRY_PWD, false);
		editor.commit();
		int pwd = prefs.getInt(Controller.pwd_key, 0);
		DPM.resetPassword(String.valueOf(pwd), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location) {
		zAdder = new ZoneSQLiteAdder(getApplicationContext());
		zAdder.open();
		
		if(zAdder.zoneExists(location)) {
			unlockScreen();
		} else
			setLockScreen();
		
		zAdder.close();
	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
