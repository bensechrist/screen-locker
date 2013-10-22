package com.screenlocker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.RemoteViews;

import com.example.screenlocker.R;
import com.example.screenlocker.Zone;
import com.example.screenlocker.ZoneSQLiteAdder;

public class MyWidgetProvider extends AppWidgetProvider {

	  @Override
	  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	      int[] appWidgetIds) {
		  
		  ZoneSQLiteAdder zAdder = new ZoneSQLiteAdder(context);
		  zAdder.open();
		  
		  ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		  NetworkInfo nInfo = cm.getActiveNetworkInfo();
		  
		  WifiManager wm;
		  
	    // Get all ids
	    ComponentName thisWidget = new ComponentName(context,
	        MyWidgetProvider.class);
	    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
	    for (int widgetId : allWidgetIds) {
	    	RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
			          R.layout.widget_layout);
			if (nInfo != null) {
				int type = nInfo.getType();
				switch (type) {
				case ConnectivityManager.TYPE_WIFI:
					wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
					WifiInfo winfo = wm.getConnectionInfo();
					String BSSID = winfo.getBSSID();
					String SSID = winfo.getSSID();
					Zone zone = new Zone();
					zone.setMacAddr(BSSID);
					if(zAdder.zoneExists(zone)) {
						remoteViews.setTextViewText(R.id.update, SSID + "\n" + BSSID + "\nZone Exists");
						Intent intent = new Intent(context, MyWidgetProvider.class);

					    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
					    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

					    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
					    remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
					} else {
						remoteViews.setTextViewText(R.id.update, SSID + "\n" + BSSID + "\nTap to Add Zone");
						Intent intent = new Intent("com.screenlocker.widgetZoneAdded");
						intent.putExtra("BSSIDfromWidget", BSSID);
						intent.putExtra("SSIDfromWidget", SSID);
						intent.putExtra("WidgetIds", appWidgetIds);
						PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
						remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
					}
					break;

				default:
					remoteViews.setTextViewText(R.id.update, "No Wifi Detected");

				    // Register an onClickListener
				    Intent updateIntent = new Intent(context, MyWidgetProvider.class);

				    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

				    PendingIntent updatePIntent = PendingIntent.getBroadcast(context,
				        0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				    remoteViews.setOnClickPendingIntent(R.id.update, updatePIntent);
					break;
				}
			} else {
				remoteViews.setTextViewText(R.id.update, "No Wifi Detected");

			    // Register an onClickListener
			    Intent intent = new Intent(context, MyWidgetProvider.class);

			    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

			    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
			        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			    remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
			}

	      appWidgetManager.updateAppWidget(widgetId, remoteViews);
	    }
	  }
}
