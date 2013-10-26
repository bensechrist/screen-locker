package com.example.screenlocker;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

public class ZoneSQLiteAdder {
	
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allZonecolumns = { MySQLiteHelper.COLUMN_ID, 
			MySQLiteHelper.COLUMN_LATITUDE, MySQLiteHelper.COLUMN_LONGITUDE };
	
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
		double latitude = zone.getLatitude();
		double longitude = zone.getLongitude();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_LATITUDE, latitude);
		values.put(MySQLiteHelper.COLUMN_LONGITUDE, longitude);
		long insertId = database.insert(MySQLiteHelper.TABLE_ZONES, null, values);
		return insertId;
	}
	
	public void replaceZone(Zone zone, long id) {
		double latitude = zone.getLatitude();
		double longitude = zone.getLongitude();
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_LATITUDE, latitude);
		values.put(MySQLiteHelper.COLUMN_LONGITUDE, longitude);
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
			temp.setLatitude(cursor.getDouble(1));
			temp.setLongitude(cursor.getDouble(2));
			zones.add(temp);
			cursor.moveToNext();
		}
		cursor.close();
		return zones;
	}
	
	public boolean zoneExists(Location location) {
		List<Zone> zones = getZones();
		for(int i=0; i<zones.size(); ++i) {
			Location dest = new Location(location);
			dest.setLatitude(zones.get(i).getLatitude());
			dest.setLongitude(zones.get(i).getLongitude());
			if(location.distanceTo(dest) < 50) {
				Log.i("true", "zone exists");
				return true;
			}
		}
		Log.i("false", "zone doesn't exist");
		return false;
	}
	
	public void removeZone(Zone zone) {
		
		database.delete(MySQLiteHelper.TABLE_ZONES, MySQLiteHelper.COLUMN_LATITUDE + "=? AND " + MySQLiteHelper.COLUMN_LONGITUDE + "=?", new String[]{String.valueOf(zone.getLatitude()), String.valueOf(zone.getLongitude())});
		
		Log.i("deleted", "Deleted zone");
	}
	
	public void removeAssocZones(long zoneid) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_ZONES, allZonecolumns, MySQLiteHelper.COLUMN_ID + "=?", new String[]{String.valueOf(zoneid)}, null, null, null);
		
		if(cursor != null)
			cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			Zone temp = new Zone();
			temp.set_id(cursor.getInt(0));
			temp.setLatitude(cursor.getLong(1));
			temp.setLongitude(cursor.getLong(2));
			removeZone(temp);
			cursor.moveToNext();
		}
	}
	
}
