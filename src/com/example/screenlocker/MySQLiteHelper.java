package com.example.screenlocker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "screenlocker";
	private static final int DATABASE_VERSION = 1;

	/***************** PROFILE TABLE SETUP ******************/
	public static final String TABLE_ZONES = "unlock_zones";
	public static final String COLUMN_LATITUDE = "latitude";
	public static final String COLUMN_LONGITUDE = "longitude";
	public static final String COLUMN_ID = "id";

	private static final String TABLE_ZONES_CREATE = 
		"create table " + TABLE_ZONES + "(" 
			+ COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_LATITUDE + " text, " + COLUMN_LONGITUDE + " text);";
		
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(TABLE_ZONES_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ",which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ZONES);
		onCreate(db);
	}

}
