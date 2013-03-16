package edu.depaul.x86azul;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.javadocmd.simplelatlng.Geohasher;

import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Bounds;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Leg;
import edu.depaul.x86azul.map.MarkerWrapper;

import android.R.bool;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.provider.BaseColumns;
import android.provider.Settings.Secure;


/**
 * This class encapsulate the Debris object and the way it's stored in database
 */
public class Debris implements BaseColumns {
	
	public enum DangerFlag {
		NON_DANGER, DANGER, LETHAL, TARGET_INFO, TARGET_DESTINATION
	}
	
	public static final String TABLE_NAME = "debris";
	public static final String COLUMN_NAME_LATITUDE 	= "latitude";
	public static final String COLUMN_NAME_LONGITUDE 	= "longitude";
	public static final String COLUMN_NAME_TIMESTAMP	= "timestamp";
	public static final String COLUMN_NAME_SPEED	 	= "speed";
	public static final String COLUMN_NAME_ACCURACY 	= "accuracy";
	public static final String COLUMN_NAME_ADDRESS 	    = "address";
	public static final String COLUMN_NAME_WEBSERVICE   = "inWebService";
	public static final String COLUMN_NAME_GEOHASH 	    = "geohash";
	// private static final String COLUMN_NAME_DEVID	= "devid";
	
	public long mDebrisId;
	public double mLatitude;
	public double mLongitude;
	public String mTimestamp;
	public float mSpeed;
	public float mAccuracy;
	// this will be for user info
	public String mAddress;
	// flag to know if this object has been sync with web service
	public boolean mInWebService; 
	// flag to know if this object has been sync with web service
	public String mGeohash;  
	
	// all of the next variables is not stored in database
	// they're only modified/needed during application active
	
	// flag to know if this object has been sync in database
	public boolean mInLocalDb;  
	
	// flag to know if this object has been in the map
	public boolean mInMap;  
	
	// flag to know if this object has been in the map
	public MarkerWrapper mMarker;  
	
	// these two will be used for computation only and will always be updated
	public double mDistanceToUser; 	// store distance to user, it will not be accurate for far debris
									// mainly used to do efficient calculation
	public double mBearingToUser; 
	
	public DbAdapter mDbAdapter;
	
	public DangerFlag mDangerFlag;
	
	private static final String TEXT_TYPE = " TEXT";
	private static final String REAL_TYPE = " REAL";
	private static final String INT_TYPE  = " INTEGER";
	public static final String SQL_DEBRIS_TABLE_CREATE =
	    "CREATE TABLE " + TABLE_NAME + " (" +
		    _ID 					+ " INTEGER PRIMARY KEY" + "," +
		    COLUMN_NAME_LATITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_LONGITUDE 	+ REAL_TYPE + "," +
		    COLUMN_NAME_TIMESTAMP 		+ TEXT_TYPE + "," +
		    COLUMN_NAME_SPEED 		+ REAL_TYPE + "," +
		    COLUMN_NAME_ACCURACY 	+ REAL_TYPE + "," +
		    COLUMN_NAME_ADDRESS 	+ TEXT_TYPE + "," +
			COLUMN_NAME_WEBSERVICE 	+ INT_TYPE  + "," +
			COLUMN_NAME_GEOHASH 	+ TEXT_TYPE +
	    " )";
	
	public static final String SQL_DEBRIS_TABLE_DELETE =
	    "DROP TABLE IF EXISTS " + TABLE_NAME;
	
	public static final String SQL_DEBRIS_TABLE_RESET =
		    "DELETE * FROM " + TABLE_NAME;
	
	public static final String SQL_DEBRIS_SELECTION =
		    _ID + " LIKE ?";
	
	// constructor
	public Debris (Long id, Double latitude, Double longitude, String timestamp,
					Float speed, Float accuracy, String address, 
					int inWebService, String geohash) 
	{	
		mDebrisId = id;
		mLatitude = latitude;
		mLongitude = longitude;
		mTimestamp = timestamp;
		mSpeed = speed;
		mAccuracy = accuracy;
		mAddress = address;
		mInWebService = inWebService!=0?true:false;
		
		if(geohash == null){
			mGeohash = Geohasher.hash(new com.javadocmd.simplelatlng.LatLng(mLatitude, mLongitude));
		}
		else {
			mGeohash = geohash;
		}
				
		mInMap = false;  
		mMarker = null; 
			
		mInLocalDb = false;
		
		mDistanceToUser = 0;
		mBearingToUser = 0; 
	}

	public static String derivedGeohash(JSONObject obj){
		Long lat = obj.get("latitudeInternal")!=null?((Long)obj.get("latitudeInternal")):0L;
		Long lng = obj.get("longitudeInternal")!=null?((Long)obj.get("longitudeInternal")):0L;

		return Geohasher.hash(new com.javadocmd.simplelatlng.LatLng(lat, lng));
	}
	
	public Debris (JSONObject obj){
	
		this (	obj.get("id")!=null? (Long)obj.get("id"):0L,
				obj.get("latitude")!=null? (Double)obj.get("latitude"):0,
				obj.get("longitude")!=null? (Double)obj.get("longitude"):0,
				obj.get("timestamp")!=null? (String)obj.get("timestamp"):null,
				obj.get("speed")!=null?((Double)obj.get("speed")).floatValue():0f,
				obj.get("accuracy")!=null?((Double)obj.get("accuracy")).floatValue():0f,
				obj.get("address")!=null? (String)obj.get("address"):null,
				0,
				derivedGeohash(obj));
	}

	public Debris (Location location) 
	{	
		this ( (long)999, 
				location.getLatitude(), 
				location.getLongitude(),
				getSimpleDateString(),
				location.getSpeed(), 
				location.getAccuracy(),
				null,
				0,
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
				null,
				0,
				null);
	}
	
	public String toString(){
    	return  " Lat=" + mLatitude +
                ", Long=" + mLongitude +
                ", Timestamp=" + mTimestamp +
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
		data.put(COLUMN_NAME_TIMESTAMP, mTimestamp);
		data.put(COLUMN_NAME_SPEED, mSpeed);
		data.put(COLUMN_NAME_ACCURACY, mAccuracy);
		data.put(COLUMN_NAME_ADDRESS, mAddress);
		data.put(COLUMN_NAME_WEBSERVICE, mInWebService);
		data.put(COLUMN_NAME_GEOHASH, mGeohash);
		
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
		if(column == COLUMN_NAME_TIMESTAMP)
			data.put(COLUMN_NAME_TIMESTAMP, (String)val);
		if(column == COLUMN_NAME_SPEED)
			data.put(COLUMN_NAME_SPEED, (Double)val);
		if(column == COLUMN_NAME_ACCURACY)
			data.put(COLUMN_NAME_ACCURACY, (Double)val);
		if(column == COLUMN_NAME_ADDRESS)
			data.put(COLUMN_NAME_ADDRESS, (String)val);
		if(column == COLUMN_NAME_WEBSERVICE)
			data.put(COLUMN_NAME_WEBSERVICE, (Integer)val);
		if(column == COLUMN_NAME_GEOHASH)
			data.put(COLUMN_NAME_GEOHASH, (String)val);
	
		return data; 
	}

	public static ArrayList<Debris> cursorToDebrisData(Cursor cursor) {
		
		ArrayList<Debris> debrisList = new ArrayList<Debris>();
		
		cursor.moveToFirst();
	    while (!cursor.isAfterLast()) 
	    {
	    	// create the object from entry in database
			Debris debris = new Debris(
					cursor.getLong(cursor.getColumnIndex(_ID)),
					cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LATITUDE)),
					cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LONGITUDE)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP)),
					cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_SPEED)),
					cursor.getFloat(cursor.getColumnIndex(COLUMN_NAME_ACCURACY)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME_ADDRESS)),
					cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_WEBSERVICE)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME_GEOHASH))
					);

			
			debrisList.add(debris);
			cursor.moveToNext();
	    }
	    
		return debrisList;
	}
	
	private static String getSimpleDateString(){
		
		Date t = new Date();
			
		String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(t);
		
		// we need to do this because date format does NOT support timezone in +/-HH:mm
		// let's convert +0800 to +08:00 format
		String timezone = new SimpleDateFormat("ZZZZZ").format(t);
		timezone = timezone.substring(0, 3) + ":" + timezone.substring(3, timezone.length());
	
		String tmp = dateTime + timezone;
		
		return tmp;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject toJSONObject(Debris debris){
		
		JSONObject obj=new JSONObject();
		  obj.put("timestamp", debris.mTimestamp);
		  obj.put("latitude", debris.mLatitude);
		  obj.put("longitude", debris.mLongitude);
		  obj.put("speed", debris.mSpeed);
		  
		  return obj;
	}
	
	public static ArrayList<Debris> toDebrises(String jsonDebrisArray){
		
		ArrayList<Debris> debrises = new ArrayList<Debris>();
		
		try {
			// it's possible that the string is rubbish
			// and can't be castable, that's why need to put in try
			JSONArray tempArray = (JSONArray)JSONValue.parse(jsonDebrisArray);
			for(int i=0; i<tempArray.size();i++){
				debrises.add(new Debris((JSONObject)tempArray.get(i)));
			}
		}
		catch(Exception e) {
			DH.showDebugError("Exception:" + e.getMessage() +
					                    "\nString:" + jsonDebrisArray);
		}
		
		return debrises;
	}
	
	private static String inSpecialStringDoubleFormat(double value){
		DecimalFormat df = new DecimalFormat("0.000000");
		String ret = df.format(value);
		ret = ret.substring(0, ret.length()-1);
		return ret;
	}

	public static boolean isSimilar(Debris debris1, Debris debris2) {
		return debris1.mGeohash.equals(debris2.mGeohash);
		//return debris1.mGeohash.equals(debris2.mGeohash);
	}

}