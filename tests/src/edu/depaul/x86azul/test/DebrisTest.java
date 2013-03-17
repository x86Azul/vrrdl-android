package edu.depaul.x86azul.test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.javadocmd.simplelatlng.Geohasher;
import com.javadocmd.simplelatlng.LatLng;

import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.helper.DH;
import android.test.AndroidTestCase;
import junit.framework.Assert;
import junit.framework.TestCase;

public class DebrisTest extends AndroidTestCase {

	/*
	 * public Debris (Long id, Double latitude, Double longitude, String timestamp,
					Float speed, Float accuracy, String address, 
					int inWebService, String geohash) 
	{	
	 */


	private static final MyLatLng SYDNEY = new MyLatLng(-33.87365, 151.20689);
	private static final MyLatLng CHICAGO = new MyLatLng(41.878114, -87.629798);

	public void testConstructor(){

		Debris debris = new Debris(SYDNEY);

		Assert.assertEquals(SYDNEY.latitude, debris.mLatitude);
		Assert.assertEquals(SYDNEY, debris.getLatLng());
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

		Assert.assertTrue(debris.equals(debris));
		Assert.assertTrue(debris.equals(debris1));

	}
	
	public void testConvertFromWebServiceJson(){
		
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
	public void testConvertToWebServiceJson(){
		
		double latitude = 37.174872;
		double longitude = -178.532039;
		float speed = 40.0f;
		String randomUid = "123312231";
		String timestamp = "2013-02-19T06:38:20.185+0000";
		
			
		Debris debris = new Debris (0L, latitude, longitude, timestamp,
				speed, 0f, null, 0, null);
		
		JSONObject obj = debris.toJSONObject();
		obj.put("uid", randomUid);
		
		// do back parse to get rid of all cast info
		obj = (JSONObject) JSONValue.parse(obj.toString());
		

		String jsonTest = "{\"latitude\":37.174872,\"longitude\":-178.532039,\"speed\":40.0," +
                "\"uid\":\"123312231\",\"timestamp\":\"2013-02-19T06:38:20.185+0000\"}";
		
		JSONObject obj1 = (JSONObject) JSONValue.parse(jsonTest);
		
		Assert.assertEquals((Double)obj1.get("latitude"), (Double)obj.get("latitude"));
		Assert.assertEquals((Double)obj1.get("longitude"), (Double)obj.get("longitude"));
		Assert.assertEquals(((Double)obj1.get("speed")).floatValue(), ((Double)obj.get("speed")).floatValue());
		Assert.assertEquals((String)obj1.get("uid"), (String)obj.get("uid"));
		Assert.assertEquals((String)obj1.get("timestamp"), (String)obj.get("timestamp"));

	}

}
