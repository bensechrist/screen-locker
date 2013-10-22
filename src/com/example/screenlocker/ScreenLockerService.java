package com.example.screenlocker;

import java.io.IOException;
import java.io.OutputStreamWriter;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.screenlocker.MainActivity.Controller;
import com.screenlocker.widget.MyWidgetProvider;

public class ScreenLockerService extends Service {
	
	public static final String TOGGLE_ACTION = "com.screenlocker.toggleService";
	public static final int notiId = 13;
	public static final int requestCode = 13;
	
	private WifiManager wm;
	private ConnectivityManager cm;
	private BroadcastReceiver toggleReceiver;
	private BroadcastReceiver wifistatechange;
	private BroadcastReceiver widgetReceiver;
	private ZoneSQLiteAdder zAdder;
	private SharedPreferences prefs;
	private DevicePolicyManager DPM;
	
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
				String BSSID = intent.getExtras().getString("BSSIDfromWidget");
				String SSID = intent.getExtras().getString("SSIDfromWidget");
				int[] widgetIds = intent.getExtras().getIntArray("WidgetIds");
				Zone newZone = new Zone();
				newZone.setMacAddr(BSSID);
				newZone.setSSID(SSID);
				if(!zAdder.zoneExists(newZone)) {
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
		ifilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		ifilter.addAction("com.screenlocker.addedZone");
		wifistatechange = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("receiver", "received it");
				zAdder = new ZoneSQLiteAdder(getApplicationContext());
				zAdder.open();
				if (intent.getAction() == ConnectivityManager.CONNECTIVITY_ACTION) {
					cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo nInfo = cm.getActiveNetworkInfo();
					if (nInfo != null) {
						int type = nInfo.getType();
						switch (type) {
						case ConnectivityManager.TYPE_WIFI:
							wm = (WifiManager)getSystemService(WIFI_SERVICE);
							WifiInfo winfo = wm.getConnectionInfo();
							String macAddr = winfo.getMacAddress();
							Zone temp = new Zone();
							temp.setMacAddr(macAddr);
							if(zAdder.zoneExists(temp))
								unlockScreen();
							else
								setLockScreen();
							break;
		
						default:
							setLockScreen();
							break;
						}
					} else
						setLockScreen();
				} else if (intent.getAction() == "com.screenlocker.addedZone") {
					unlockScreen();
				}
				zAdder.close();
			}
		};
		registerReceiver(wifistatechange, ifilter);
		
		IntentFilter lockFilter = new IntentFilter("com.screenlocker.lockscreen");
		BroadcastReceiver lockScreen = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Controller.mDPM.lockNow();
			}
		};
		registerReceiver(lockScreen, lockFilter);
	}
	
	private void writeToFile(String data) {
	    try {
	        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("log.txt", 0));
	        outputStreamWriter.write(data);
	        outputStreamWriter.close();
	    }
	    catch (IOException e) {
	        Log.e("Exception", "File write failed: " + e.toString());
	    } 
	}
	
	private void unlockScreen(){
		//Intent intent = new Intent("com.screenlocker.lockscreen");
		//PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(getApplicationContext());
		notiBuilder.setContentTitle("Screen Locker").setContentText("Reenter Password at Lock Screen")
			.setSmallIcon(R.drawable.ic_launcher); //.setContentIntent(pIntent);
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
	}
}
