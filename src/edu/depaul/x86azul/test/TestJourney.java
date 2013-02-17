package edu.depaul.x86azul.test;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import edu.depaul.x86azul.DebrisTracker;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.PositionTracker;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.WebWrapper;
import edu.depaul.x86azul.helper.DialogHelper;
import edu.depaul.x86azul.helper.GoogleDirJsonParams;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.PolylineDecoder;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Step;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.map.MarkerWrapper;
import edu.depaul.x86azul.map.MarkerWrapper.Type;


public class TestJourney implements WebWrapper.Client, MapWrapper.GestureClient {

	private final int FAST_FORWARD_FACTOR = 8;
	private final int UPDATE_PERIOD = 800; // in ms

	private Client mClient;
	private GoogleDirJsonParams mJsonParams;
	
	private MainActivity mContext;
	
	private MapWrapper mMap;
	private PositionTracker mPosTracker;
	private DebrisTracker mData;
	
	public volatile boolean mExitFlag;

	private ArrayList<MarkerWrapper> markers;
	
	// these 2 are specials because we want to add address to it
	private MarkerWrapper startMarker;
	private MarkerWrapper endMarker;

	public interface Client {
		public void onFinishTestJourney();
	}
	
	public Handler mHandler;
	public RunTestParam mRunTestParam;
	public Runnable mRunTestThread;
	
	
	public class RunTestParam {
		public int stepIdx;
		public int nextPointIdx;
		public MyLatLng currentLatLng;
		
		public void reset(){
			stepIdx = Integer.MAX_VALUE;
			nextPointIdx = Integer.MAX_VALUE;
			currentLatLng = null;

		}
	} 

	public class RunTestThread implements Runnable {

		@Override
		public void run() {
			
			int stepIdx = mRunTestParam.stepIdx;
			int nextPointIdx = mRunTestParam.nextPointIdx;
			MyLatLng currentLatLng = mRunTestParam.currentLatLng;
			
			// first time
			if (stepIdx == Integer.MAX_VALUE)
				stepIdx = 0;
			
			// first time
			if(nextPointIdx == Integer.MAX_VALUE)
				nextPointIdx = 1;
			
			Step step = mJsonParams.getSteps().get(stepIdx);

			List<MyLatLng> points = PolylineDecoder.decodePoly(step.polyline.points);
			
			// first time
			if(currentLatLng == null)
				currentLatLng = points.get(0);
				
			// this is speed in m/s (which is possible to be 0
			int totalStepDistance = step.distance.value;
			int totalStepDuration = step.duration.value != 0? step.duration.value:1;
			
			double movePerSecond = (double)totalStepDistance/totalStepDuration; 
				movePerSecond *= FAST_FORWARD_FACTOR;
				movePerSecond *= (double)UPDATE_PERIOD/1000;

			double distanceTaken = 0;

			// save location, clone the current location before it changes
			MyLatLng prevLoc = new MyLatLng(currentLatLng.latitude, currentLatLng.longitude);

			// let's try to move step by step
			while(nextPointIdx < points.size()){
				// see if we can reach next point
				
				double toNextPointDistance = MyLatLng.distance(currentLatLng, points.get(nextPointIdx));
				
				if(distanceTaken + toNextPointDistance > movePerSecond){
					// we can move safely for distance = (movePerSecond-distanceTaken) at this point
					currentLatLng = MyLatLng.getLatLngFraction(currentLatLng, points.get(nextPointIdx), 
							(movePerSecond-distanceTaken), toNextPointDistance);

					distanceTaken = movePerSecond;
					// no change in nextIdx value
					break;
				}
				else {
					// take the step
					currentLatLng = points.get(nextPointIdx);

					// and increase the index
					distanceTaken += toNextPointDistance;
					nextPointIdx++;
				}

			}
		
			// publish update
			Location location = new Location("FakeLocationProvider");
				location.setLatitude(currentLatLng.latitude);
				location.setLongitude(currentLatLng.longitude);
				location.setAccuracy(20);
				location.setTime(System.currentTimeMillis());
				location.setSpeed((float)movePerSecond);
				location.setBearing((float)MyLatLng.bearing(prevLoc, currentLatLng));
			
			if(mPosTracker != null)
				mPosTracker.updateLocation(location);

			// now update params for the next loop
			if(nextPointIdx >= points.size()){
				// we're already at the end of points of this step,
				// move to the next one if available
				nextPointIdx = 1;
				stepIdx++;
				if(stepIdx >= mJsonParams.getSteps().size()){
					// we're already at the end of the step too..
					// that means we're done
					Log.i("QQQ", "TestDone");
					cleanUp();
					if(mClient != null)
						mClient.onFinishTestJourney();
					return;
				}
			}
			
			mRunTestParam.stepIdx = stepIdx;
			mRunTestParam.nextPointIdx = nextPointIdx;
			mRunTestParam.currentLatLng = currentLatLng;
			

			mHandler.postDelayed(this, UPDATE_PERIOD);
		}
		
	}

	public TestJourney(MainActivity context, MapWrapper map, 
			PositionTracker posTracker, DebrisTracker data){		
		mContext = context;

		mMap = map;
		
		mPosTracker = posTracker;
		
		mData = data;

		markers = new ArrayList<MarkerWrapper>();
		
		// all the location will be coming from us mwuahahaha..
		mPosTracker.hijackLocationProvider(true);
		// listen to notification from map
		mMap.hijackNotification(true, this);
		mData.hijackState(true);
		
		mExitFlag = false;
		
		mHandler = new Handler();
		mRunTestParam = new RunTestParam();

		// ask for start point
		DialogHelper.showToast((Activity) mContext, "Choose start point");
	}
	
	public void subscribe(Client client){
		mClient = client;
	}

	// this is the test
	private void runTest(){
		
		// draw the path 
		MarkerWrapper marker = new MarkerWrapper(Type.POLYLINE)
							.coordinates(PolylineDecoder.decodePoly(mJsonParams.getOverview_polyline()))
							.width(8.0f)
					        .color(0xAA1788FF);
							
		markers.add(marker);		
		mMap.insertMarker(marker, true);

		/*
		mTestHandle = new RunningSimulation();
		mTestHandle.execute(mJsonParams);
		*/
		
		mRunTestThread = new RunTestThread();
		mRunTestParam.reset();
		mHandler.post(mRunTestThread);

	}


	@Override
	public void onFinishProcessHttp(String token, String result) {
		
		if(mExitFlag){
			// everything has been clean up, just return;
			return;
		}
		
		// the first result would be for start point
		if(token.equals("startMarker")){
			GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
			if(params.isValid())
				startMarker.snippet(params.getDetailAddress());
		}
		else if(token.equals("endMarker")){
			GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
			if(params.isValid())
				endMarker.snippet(params.getDetailAddress());
		}
		else if(token.equals("pathMarker")){

			// parse and start the test here
			// JSONObject is a java.util.Map and JSONArray is a java.util.Lis
			mJsonParams = new GoogleDirJsonParams((JSONObject)JSONValue.parse(result));
			if(mJsonParams.isValid())
				runTest();
		}
	}
	
	public void cleanUp(){
		
		if (mRunTestThread!=null && mHandler!=null)
			mHandler.removeCallbacks(mRunTestThread);
		
		mData.hijackState(false);
		mMap.hijackNotification(false, null);
		mPosTracker.hijackLocationProvider(false);
		
		for(int i=0;i<markers.size(); i++)
			markers.get(i).removeFromMap(true);
		
		markers.clear();
		startMarker = null;
		endMarker = null;
		
	}
	
	public void forceStop(){
		mExitFlag = true;
		cleanUp();
	}

	@Override
	public void onMapClick(MyLatLng latLng) {
		
		if(mExitFlag)
			return;
		
		if(startMarker==null){
			
			startMarker = new MarkerWrapper(Type.PIN)
						.title("Start Point")
						.coordinate(latLng)
						.icon(R.drawable.start_marker);

			markers.add(startMarker);
			mMap.insertMarker(startMarker, true);

			// ask for end point
			DialogHelper.showToast((Activity) mContext, "Choose end point");
			
			String startPointURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
					"latlng=" + latLng.latitude + "," + latLng.longitude + "&" +
					"sensor=true";
			new WebWrapper(this).get("startMarker", startPointURI);
		}
		else {

			endMarker = new MarkerWrapper(Type.PIN)
							.title("End Point")
							.coordinate(latLng)
							.icon(R.drawable.end_marker);
			
			markers.add(endMarker);

			mMap.insertMarker(endMarker, true);
			
			String endPointURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
					"latlng=" + latLng.latitude + "," + latLng.longitude + "&" +
					"sensor=true";
			
			new WebWrapper(this).get("endMarker", endPointURI);
			
			// we're done here, let the map go
			mMap.hijackNotification(false, null);

			// we got everything we need, trigger the webservice request now
			String uri = "http://maps.googleapis.com/maps/api/directions/json?" +
					"origin=" + startMarker.getLatitude() + "," + 
					startMarker.getLongitude() + "&" + "destination=" + 
					endMarker.getLatitude() + "," + endMarker.getLongitude() + 
					"&" + "sensor=true&units=metric";
			new WebWrapper(this).get("pathMarker", uri);
		}
		
	}

	@Override
	public void onInfoWindowClick(MarkerWrapper marker) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onMarkerClick(MarkerWrapper marker) {
		// TODO Auto-generated method stub
		return false;
	}



}