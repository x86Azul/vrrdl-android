package edu.depaul.x86azul;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.provider.BaseColumns;


/**
 * This class encapsulate the Debris object and the way it's stored in database
 */
public class Debris implements BaseColumns {
	
	public static final String TABLE_NAME = "debris";
	public static final String COLUMN_NAME_LATITUDE 	= "latitude";
	public static final String COLUMN_NAME_LONGITUDE 	= "longitude";
	public static final String COLUMN_NAME_TIME	 		= "time";
	public static final String COLUMN_NAME_SPEED	 	= "speed";
	public static final String COLUMN_NAME_ACCURACY 	= "accuracy";
	// private static final String COLUMN_NAME_DEVID	= "devid";
	
	public long mDebrisId;
	public double mLatitude;
	public double mLongitude;
	public String mTime;
	public float mSpeed;
	public float mAccuracy;
	
	private static final String TEXT_TYPE = " TEXT";
	private static final String REAL_TYPE = " REAL";
	public static final String SQL_DEBRIS_TABLE_CREATE =
	    "CREATE TABLE " + TABLE_NAME + " (" +
		    _ID 					+ " INTEGER PRIMARY KEY" + "," +
		    COLUMN_NAME_LATITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_LONGITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_TIME 		+ TEXT_TYPE + "," +
		    COLUMN_NAME_SPEED 		+ REAL_TYPE + "," +
		    COLUMN_NAME_ACCURACY 	+ REAL_TYPE + 
	    " )";

	public static final String SQL_DEBRIS_TABLE_DELETE =
	    "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	// constructor
	public Debris (Long id, Double latitude, Double longitude, String time,
					Float speed, Float accuracy) 
	{	
		mDebrisId = id;
		mLatitude = latitude;
		mLongitude = longitude;
		mTime = time;
		mSpeed = speed;
		mAccuracy = accuracy;
	}
	
	public Debris (Location location) 
	{	
		mDebrisId = 999; // set random id, it will be updated later
		mLatitude = location.getLatitude();
		mLongitude = location.getLongitude();
		mTime = DateFormat.getDateTimeInstance().format(new Date()).toString();
		mSpeed = location.getSpeed();
		mAccuracy = location.getAccuracy();
	}
	
	public Debris (LatLng latLng) 
	{	
		mDebrisId = 999; // set random id, it will be updated later
		mLatitude = latLng.latitude;
		mLongitude = latLng.longitude;
		mTime = DateFormat.getDateTimeInstance().format(new Date()).toString();
		mSpeed = 19;
		mAccuracy = 83;
	}
	
	// APIs for database portion 
	
	public ContentValues getDbFormat()
	{
		ContentValues data = new ContentValues();
		
		data.put(COLUMN_NAME_LATITUDE, mLatitude);
		data.put(COLUMN_NAME_LONGITUDE, mLongitude);
		data.put(COLUMN_NAME_TIME, mTime);
		data.put(COLUMN_NAME_SPEED, mSpeed);
		data.put(COLUMN_NAME_ACCURACY, mAccuracy);
		
		return data;
	}

	public static List<Debris> cursorToDebrisData(Cursor cursor) {
		
		List<Debris> debrisList = new ArrayList<Debris>();
		
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) 
	    {
	    	// create the object from entry in database
			Debris debris = new Debris(
					cursor.getLong(cursor.getColumnIndex(_ID)),
					cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LATITUDE)),
					cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LONGITUDE)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TIME)),
					cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_SPEED)),
					cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_ACCURACY))					
					);
			
			debrisList.add(debris);
			cursor.moveToNext();
	    }
	    
		return debrisList;
	}
	

}