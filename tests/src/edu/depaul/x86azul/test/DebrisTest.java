package edu.depaul.x86azul.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import org.json.JSONObject;

import com.javadocmd.simplelatlng.Geohasher;
import com.javadocmd.simplelatlng.LatLng;

import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.helper.DH;
import android.location.Location;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import junit.framework.TestCase;

public class DebrisTest extends AndroidTestCase {

	private static final MyLatLng SYDNEY = new MyLatLng(-33.87365, 151.20689);
	private static final MyLatLng CHICAGO = new MyLatLng(41.878114, -87.629798);

	public void testConstructor(){

		// constructed with lat lng info
		Debris debris = new Debris(SYDNEY);

		Assert.assertEquals(SYDNEY.latitude, debris.mLatitude);
		Assert.assertEquals(SYDNEY, debris.getLatLng());
		
		// constructed with Location info
		float accuracy = 20;
		float speed = 40;
		float bearing = 30;

		
		Location location = new Location("FakeLocationProvider");
		location.setLatitude(SYDNEY.latitude);
		location.setLongitude(SYDNEY.longitude);
		location.setAccuracy(accuracy);
		location.setTime(System.currentTimeMillis());
		location.setSpeed(speed);
		location.setBearing(bearing);
		
		debris = new Debris(location);
		
		Assert.assertEquals(SYDNEY.latitude, debris.mLatitude);
		Assert.assertEquals(SYDNEY, debris.getLatLng());
		Assert.assertEquals(accuracy, debris.mAccuracy, 0.0001);
		Assert.assertEquals(speed, debris.mSpeed, 0.0001);
		
		String strExpected = 	"ID=" 		+ Debris.DefaultId + 
								", Lat=" 	+ SYDNEY.latitude +
						        ", Lng=" 	+ SYDNEY.longitude +
						        ", Timestamp=" 	+ debris.mTimestamp +
						        ", Speed=" 	+ speed + 
						        ", Accuracy=" 	+ accuracy +
						        ", Geohash=" 	+ debris.mGeohash;
		
		Assert.assertEquals(strExpected, debris.toString());
		
	}
	
	/*
	 * geohash and timestamp suppose to be assigned automatically
	 */
	public void testDerivedParameters(){

		Debris debris = new Debris(CHICAGO);

		Assert.assertEquals(CHICAGO.latitude, debris.mLatitude);
		Assert.assertEquals(CHICAGO, debris.getLatLng());
		
		Assert.assertNotNull(debris.mGeohash);
		Assert.assertNotNull(debris.mTimestamp);

	}
	
	public void testEquals(){

		Debris debris = new Debris(CHICAGO);
		Debris debris1 = new Debris(CHICAGO);

		Assert.assertEquals(debris, debris);
		Assert.assertEquals(debris, debris1);

	}
	
	public void testEncodeWebServiceJson(){
		
		String jsonString = "[{\"timestamp\":\"2013-02-18T22:38:20.185-08:00\",\"latitude\":37.174872,\"longitude\":-178.532039,\"speed\":40.0,\"uid\":\"123312231\"}" + 
					",{\"latitude\":39.174872,\"longitude\":-178.532039,\"speed\":40.0,\"uid\":\"123312231\",\"timestamp\":\"2013-02-19T06:38:20.185+0000\"}" + 
					",{\"latitude\":39.174872,\"longitude\":-178.532039,\"speed\":40.0,\"uid\":\"123312231\",\"timestamp\":\"2013-02-19T06:38:20.185+0000\"}]";
		
		
		ArrayList <Debris> debrises = Debris.toDebrises(jsonString);
		
		Assert.assertEquals(debrises.size(), 3);
		Assert.assertTrue(debrises.get(0).getLatLng().equals(new MyLatLng(37.174872, -178.532039)));
		Assert.assertTrue(debrises.get(1).mTimestamp.equals("2013-02-19T06:38:20.185+0000"));
		Assert.assertTrue(debrises.get(2).mSpeed == 40.0);
	}
	
	@SuppressWarnings("unchecked")
	public void testDecodeWebServiceJson(){
		
		double latitude = 37.174872;
		double longitude = -178.532039;
		float speed = 40.0f;
		String randomUid = "123312231";
		String timestamp = "2013-02-19T06:38:20.185+0000";
		
			
		Debris debris = new Debris (0L, latitude, longitude, timestamp,
				speed, 0f, null, 0, null);
		
		
		try{
			JSONObject obj = debris.toJSONObject();
			obj.put("uid", randomUid);
			
			DH.showDebugWarning(obj.toString());
			// do back parse to get rid of all cast info
			obj = new JSONObject(obj.toString());
			
	
			String jsonTest = "{\"latitude\":37.174872,\"longitude\":-178.532039,\"speed\":40.0," +
	                "\"uid\":\"123312231\",\"timestamp\":\"2013-02-19T06:38:20.185+0000\"}";
			
			JSONObject obj1 = new JSONObject(jsonTest);
			
			Assert.assertEquals(obj1.optDouble("latitude"), obj.optDouble("latitude"), 0.0001);
			Assert.assertEquals(obj1.optDouble("longitude"), obj.optDouble("longitude"), 0.0001);
			Assert.assertEquals(obj1.optDouble("speed"), obj.optDouble("speed"), 0.0001);
			Assert.assertEquals(obj1.optString("uid"), obj.optString("uid"));
			Assert.assertEquals(obj1.optString("timestamp"), obj.optString("timestamp"));
		}
		catch(Exception e){
			DH.showDebugError(e.getMessage());
			Assert.fail();
			
		}
	}

}
