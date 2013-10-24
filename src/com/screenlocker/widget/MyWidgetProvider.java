package com.screenlocker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.screenlocker.R;
import com.example.screenlocker.ZoneSQLiteAdder;

public class MyWidgetProvider extends AppWidgetProvider {

	  @Override
	  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
	      int[] appWidgetIds) {
			  
			  ZoneSQLiteAdder zAdder = new ZoneSQLiteAdder(context);
			  zAdder.open();
			  
			  // Get all ids
			  ComponentName thisWidget = new ComponentName(context,
					  MyWidgetProvider.class);
			  int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
			  for (int widgetId : allWidgetIds) {
				  RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
						  R.layout.widget_layout);
				  remoteViews.setTextViewText(R.id.update, "No Wifi Detected");
		
				  // Register an onClickListener
				  Intent updateIntent = new Intent(context, MyWidgetProvider.class);
		
				  updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				  updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		
				  PendingIntent updatePIntent = PendingIntent.getBroadcast(context,
						  0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				  remoteViews.setOnClickPendingIntent(R.id.update, updatePIntent);
		
				  remoteViews.setTextViewText(R.id.update, "No Wifi Detected");
		
				  // Register an onClickListener
				  Intent intent = new Intent(context, MyWidgetProvider.class);
		
				  intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				  intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		
				  PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
						  0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				  remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
		
				  appWidgetManager.updateAppWidget(widgetId, remoteViews);
			  }
	    }
}
