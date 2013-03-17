package edu.depaul.x86azul.runtest;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import edu.depaul.x86azul.DataCoordinator;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.PositionTracker;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.HTTPClient;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.helper.GoogleDirJsonParams;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.PolylineDecoder;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Step;
import edu.depaul.x86azul.helper.URIBuilder;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.map.MarkerWrapper;
import edu.depaul.x86azul.map.MarkerWrapper.Type;


public class TestJourney implements HTTPClient.Client, MapWrapper.OnGestureEvent {

	private final int FAST_FORWARD_FACTOR = 8;
	private final int UPDATE_PERIOD = 800; // in ms
	
	private final String START_MARKER = "startMarker";
	private final String END_MARKER = "endMarker";
	private final String PATH_MARKER = "pathMarker";

	private Client mClient;
	private GoogleDirJsonParams mJsonParams;
	
	private MainActivity mContext;
	
	private MapWrapper mMap;
	private PositionTracker mPosTracker;
	private DataCoordinator mData;
	
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
					DH.showDebugInfo(this.getClass().getName() + ":TestDone");
					dismiss();
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
			PositionTracker posTracker, DataCoordinator data){		
		mContext = context;

		mMap = map;
		
		mPosTracker = posTracker;
		
		mData = data;

		markers = new ArrayList<MarkerWrapper>();
		
		// all the location data will be coming from us mwuahahaha..
		mPosTracker.hijackLocationProvider(true);
		// listen to touch notification from map
		mMap.hijackNotification(true, this);
		mData.hijackState(true);
		
		mExitFlag = false;
		
		mHandler = new Handler();
		mRunTestParam = new RunTestParam();

		// ask for start point
		DH.showToast((Activity) mContext, "Choose start point");
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
	public void onFinishProcessHttp(String token, 
									String uri,
									String requestBody,
									int statusCode,
									String geohash,
									String responseBody) {
								
		if(mExitFlag){
			// everything has been clean up, just return;
			return;
		}
		
		// the first result would be for start point
		if(token.equals(START_MARKER)){
			try{
				GoogleGeoJsonParams params = new GoogleGeoJsonParams(responseBody);
				if(params.isValid())
					startMarker.snippet(params.getDetailAddress());
			}
			catch (Exception e){
				DH.showDebugError(String.valueOf(e.getClass()));
			}
		}
		else if(token.equals(END_MARKER)){
			try{
				GoogleGeoJsonParams params = new GoogleGeoJsonParams(responseBody);
				if(params.isValid())
					endMarker.snippet(params.getDetailAddress());
			}
			catch (Exception e){
				DH.showDebugError(String.valueOf(e.getClass()));
			}
		}
		else if(token.equals(PATH_MARKER)){

			// parse and start the test here
			try{
				mJsonParams = new GoogleDirJsonParams(responseBody);
				if(mJsonParams.isValid())
					runTest();
				else {
					DH.showToast(mContext, "unable to calculate path");
					dismiss();
				}
			}
			catch (Exception e){
				DH.showDebugError(String.valueOf(e.getClass()));
			}
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
	
	public void dismiss(){
		mExitFlag = true;
		cleanUp();
	}

	@Override
	public void onMapClick(MyLatLng latLng) {
		
		if(mExitFlag)
			return;
		
		if(startMarker==null){
			
			startMarker = new MarkerWrapper(Type.PIN)
						.anchor(0.5f, 1f)
						.title("Start Point")
						.coordinate(latLng)
						.icon(R.drawable.start_marker);

			markers.add(startMarker);
			mMap.insertMarker(startMarker, true);

		
			// get address
			String startPointURI = URIBuilder.toGoogleGeoURI(startMarker.getCoordinate());
			
			new HTTPClient(this).get(START_MARKER, startPointURI);
			
			// ask for end point
			DH.showToast((Activity) mContext, "Choose end point");
		}
		else {

			endMarker = new MarkerWrapper(Type.PIN)
							.anchor(0.5f, 1f)
							.title("End Point")
							.coordinate(latLng)
							.icon(R.drawable.end_marker);
			
			markers.add(endMarker);

			mMap.insertMarker(endMarker, true);
			
			// get address
			String endPointURI = URIBuilder.toGoogleGeoURI(endMarker.getCoordinate());
			
			new HTTPClient(this).get(END_MARKER, endPointURI);
			
			// we're done here, let the map go
			mMap.hijackNotification(false, null);
			
			// we got everything we need, trigger the webservice request now
			String uri = URIBuilder.toGoogleDirURI(startMarker.getCoordinate(), 
					                                endMarker.getCoordinate());

			new HTTPClient(this).get(PATH_MARKER, uri);
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

	@Override
	public View getInfoContents(MarkerWrapper marker) {
		// TODO Auto-generated method stub
		return null;
	}



}