package edu.depaul.x86azul;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.depaul.x86azul.helper.DialogHelper;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.provider.BaseColumns;


/**
 * This class encapsulate the Debris object and the way it's stored in database
 */
public class Debris implements BaseColumns, WebWrapper.Client{
	
	public enum DangerFlag {
		NON_DANGER, DANGER, LETHAL
	}
	
	public static final String TABLE_NAME = "debris";
	public static final String COLUMN_NAME_LATITUDE 	= "latitude";
	public static final String COLUMN_NAME_LONGITUDE 	= "longitude";
	public static final String COLUMN_NAME_TIME	 		= "time";
	public static final String COLUMN_NAME_SPEED	 	= "speed";
	public static final String COLUMN_NAME_ACCURACY 	= "accuracy";
	public static final String COLUMN_NAME_ADDRESS 	    = "address";
	// private static final String COLUMN_NAME_DEVID	= "devid";
	
	public long mDebrisId;
	public double mLatitude;
	public double mLongitude;
	public String mTime;
	public float mSpeed;
	public float mAccuracy;
	// this will be for user info
	public String mAddress;
	
	// all of the next variables is not stored in database
	// variables not stored in database (only modified/needed during active)
	public boolean mInDatabase;  // flag to know if this object has been sync in database
	
	// these two will be used for computation only and will always be updated
	public double mDistanceToUser; 	// store distance to user, it will not be accurate for far debris
									// mainly used to do efficient calculation
	public double mBearingToUser; 
	
	public DbAdapter mDbAdapter;
	
	public DangerFlag mDangerFlag;
	
	private static final String TEXT_TYPE = " TEXT";
	private static final String REAL_TYPE = " REAL";
	public static final String SQL_DEBRIS_TABLE_CREATE =
	    "CREATE TABLE " + TABLE_NAME + " (" +
		    _ID 					+ " INTEGER PRIMARY KEY" + "," +
		    COLUMN_NAME_LATITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_LONGITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_TIME 		+ TEXT_TYPE + "," +
		    COLUMN_NAME_SPEED 		+ REAL_TYPE + "," +
		    COLUMN_NAME_ACCURACY 	+ REAL_TYPE + "," +
		    COLUMN_NAME_ADDRESS 	+ TEXT_TYPE + 
	    " )";

	public static final String SQL_DEBRIS_TABLE_DELETE =
	    "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String SQL_DEBRIS_TABLE_RESET =
		    "DELETE * FROM " + TABLE_NAME;
	
	public static final String SQL_DEBRIS_SELECTION =
		    _ID + " LIKE ?";
	
	// constructor
	public Debris (Long id, Double latitude, Double longitude, String time,
					Float speed, Float accuracy, String address) 
	{	
		mDebrisId = id;
		mLatitude = latitude;
		mLongitude = longitude;
		mTime = time;
		mSpeed = speed;
		mAccuracy = accuracy;
		mInDatabase = false;
		mDistanceToUser = 0;
		mBearingToUser = 0; 
		
									
		mAddress = address;
	}
	
	public Debris (Location location) 
	{	
		this ( (long)999, 
				location.getLatitude(), 
				location.getLongitude(),
				getSimpleDateString(),
				location.getSpeed(), 
				location.getAccuracy(),
				null);
	}
	
	public Debris (MyLatLng latLng) 
	{	
		this ( (long)999, 
				latLng.latitude, 
				latLng.longitude,
				getSimpleDateString(),
				10f, 
				100f,
				null);
	}
	
	public String toString(){
    	return  " Lat=" + mLatitude +
                ", Long=" + mLongitude +
                ", Time=" + mTime +
                ", Speed=" + mSpeed + 
                ", Accuracy=" + mAccuracy +
                ", ID=" + mDebrisId; 	
	}
	
	public MyLatLng getLatLng (){	
		return new MyLatLng(mLatitude, mLongitude);
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
		data.put(COLUMN_NAME_ADDRESS, mAddress);
		
		return data;
	}
	
	public String getSelectionId(Debris debris)
	{
		return Debris._ID + " LIKE ?";
	}
	
	public String getSelectionArgsid(Debris debris)
	{
		return String.valueOf(debris.mDebrisId);
	}
	
	public ContentValues getContentValue(String column, Object val){
		ContentValues data = new ContentValues();
		
		if(column == COLUMN_NAME_LATITUDE)
			data.put(COLUMN_NAME_LATITUDE, (Double)val);
		if(column == COLUMN_NAME_LONGITUDE)
			data.put(COLUMN_NAME_LONGITUDE, (Double)val);
		if(column == COLUMN_NAME_TIME)
			data.put(COLUMN_NAME_TIME, (String)val);
		if(column == COLUMN_NAME_SPEED)
			data.put(COLUMN_NAME_SPEED, (Double)val);
		if(column == COLUMN_NAME_ACCURACY)
			data.put(COLUMN_NAME_ACCURACY, (Double)val);
		if(column == COLUMN_NAME_ADDRESS)
			data.put(COLUMN_NAME_ADDRESS, (String)val);
	
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
					cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_ACCURACY)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME_ADDRESS))					
					);
			
			debrisList.add(debris);
			cursor.moveToNext();
	    }
	    
		return debrisList;
	}
	
	private static String getSimpleDateString(){
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()).toString();
	}

	@Override
	public void onFinishProcessHttp(String token, String result) {
		
		if(token.equals("address")){
			GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
			if(params.isValid()){
				mAddress = params.getDetailAddress();
				if(mInDatabase){
					
					DialogHelper.showDebugMethodInfo(this, mAddress);
					// by right we are suppose to be here
					mDbAdapter.needAddressUpdate(this);
				}
				else {
					// no need to do anything, it will be stored together with other data
				}
			}
		}
	}

	public void putDatabaseStamp(DbAdapter dbAdapter) {
		mInDatabase = true;
		mDbAdapter = dbAdapter;
		
		// good time for checking if address null
		mAddress = null;

		if (mAddress == null){
			String needAddressURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
					"latlng=" + mLatitude + "," + mLongitude + "&" +
					"sensor=true";
			new WebWrapper(this).get("address", needAddressURI);
		}
	}
	

}