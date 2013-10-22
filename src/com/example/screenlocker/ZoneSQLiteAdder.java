package com.example.screenlocker;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ZoneSQLiteAdder {
	
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allZonecolumns = { MySQLiteHelper.COLUMN_ID, 
			MySQLiteHelper.COLUMN_MACADDR, MySQLiteHelper.COLUMN_SSID };
	
	public ZoneSQLiteAdder(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public long addZone(Zone zone) {
		String BSSID = zone.getMacAddr();
		String SSID = zone.getSSID();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_MACADDR, BSSID);
		values.put(MySQLiteHelper.COLUMN_SSID, SSID);
		long insertId = database.insert(MySQLiteHelper.TABLE_ZONES, null, values);
		return insertId;
	}
	
	public void replaceZone(Zone zone, long id) {
		String BSSID = zone.getMacAddr();
		String SSID = zone.getSSID();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_MACADDR, BSSID);
		values.put(MySQLiteHelper.COLUMN_SSID, SSID);
		values.put(MySQLiteHelper.COLUMN_ID, id);
		database.delete(MySQLiteHelper.TABLE_ZONES, MySQLiteHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
		database.insert(MySQLiteHelper.TABLE_ZONES, null, values);
	}
	
	public List<Zone> getZones() {
		List<Zone> zones = new ArrayList<Zone>();
		
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ZONES, 
				allZonecolumns, null, null, null, null, 
				null);
		
		if (cursor != null)
			cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			Zone temp = new Zone();
			temp.set_id(cursor.getInt(0));
			temp.setMacAddr(cursor.getString(1));
			temp.setSSID(cursor.getString(2));
			zones.add(temp);
			cursor.moveToNext();
		}
		cursor.close();
		return zones;
	}
	
	public long getZoneIDbyBSSID(String BSSID) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ZONES, allZonecolumns, MySQLiteHelper.COLUMN_MACADDR + "=?", new String[]{BSSID}, null, null, null);
		cursor.moveToFirst();
		return cursor.getInt(0);
	}
	
	public boolean zoneExists(Zone zone) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ZONES, allZonecolumns, MySQLiteHelper.COLUMN_MACADDR + "=?", new String[]{zone.getMacAddr()}, null, null, null);
		cursor.moveToLast();
		int count = cursor.getCount();
		if (count == 0) {
			Log.i("false", "no zone exists");
			return false;
		}
		else {
			Log.i("true", "zone exists");
			return true;
		}
	}
	
	public void removeZone(Zone zone) {
		
		database.delete(MySQLiteHelper.TABLE_ZONES, MySQLiteHelper.COLUMN_MACADDR + "=?", new String[]{zone.getMacAddr()});
		
		Log.i("deleted", "Deleted " + zone.getMacAddr());
	}
	
	public void removeAssocZones(long zoneid) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ZONES, allZonecolumns, MySQLiteHelper.COLUMN_ID + "=?", new String[]{String.valueOf(zoneid)}, null, null, null);
		
		if(cursor != null)
			cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			Zone temp = new Zone();
			temp.set_id(cursor.getInt(0));
			temp.setMacAddr(cursor.getString(1));
			temp.setSSID(cursor.getString(2));
			removeZone(temp);
			cursor.moveToNext();
		}
	}
	
}
