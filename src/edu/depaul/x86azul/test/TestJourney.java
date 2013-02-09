package edu.depaul.x86azul.test;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import edu.depaul.x86azul.DebrisTracker;
import edu.depaul.x86azul.GoogleDirJsonParams;
import edu.depaul.x86azul.GoogleGeoJsonParams;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.MapWrapper;
import edu.depaul.x86azul.PolylineDecoder;
import edu.depaul.x86azul.PositionTracker;
import edu.depaul.x86azul.WebWrapper;
import edu.depaul.x86azul.GoogleDirJsonParams.Step;
import edu.depaul.x86azul.helper.LatLngTool;


public class TestJourney implements WebWrapper.Client, MapWrapper.GestureClient {

	private final int FAST_FORWARD_FACTOR = 8;
	private final int UPDATE_PERIOD = 800; // in ms
	
	private LatLng startPoint;
	private LatLng endPoint;

	private Client mClient;
	private GoogleDirJsonParams mJsonParams;

	private RunningSimulation mTestHandle;
	
	private MainActivity mContext;
	
	private MapWrapper mMap;
	private PositionTracker mPosTracker;
	private DebrisTracker mData;
	
	public volatile boolean mExitFlag;

	private ArrayList<Object> markers;
	
	// these 2 are specials because we want to add address to it
	private Object startMarker;
	private Object endMarker;

	public interface Client {
		public void onFinishTestJourney();
	}

	private class RunningSimulation extends AsyncTask <Object, Location, Boolean> {

		@Override
		protected Boolean doInBackground(Object... testParams) {
			GoogleDirJsonParams params = (GoogleDirJsonParams)testParams[0];

			for(int i=0; i<params.getSteps().size();i++){

				Step step = params.getSteps().get(i);

				if (isCancelled()) return Boolean.FALSE;

				List<LatLng> points = PolylineDecoder.decodePoly(step.polyline.points);
				double move = (double)step.distance.value/step.duration.value; // in mps

				move *= FAST_FORWARD_FACTOR;
				
				move *= (double)UPDATE_PERIOD/1000;

				// point to next array index
				int nextIdx = 1;
				LatLng currentLoc = points.get(0);
				while(nextIdx != 0){

					double stepTaken = 0;
					
					// save location, clone the current location before it changes
					LatLng prevLoc = new LatLng(currentLoc.latitude, currentLoc.longitude);

					// let's try to move step by step
					while(nextIdx < points.size()){
						double toNextPointStep = LatLngTool.distance(currentLoc, points.get(nextIdx));
						if(stepTaken + toNextPointStep > move){
							// we can move safely here
							currentLoc = LatLngTool.getLatLngFraction(currentLoc, points.get(nextIdx), 
									(move-stepTaken), toNextPointStep);

							stepTaken = move;
							// no change in nextIdx value
							break;
						}
						else {
							// take the step
							currentLoc = points.get(nextIdx);

							// and increase the index
							stepTaken += toNextPointStep;
							nextIdx++;
						}

					}
					// this will break
					if(nextIdx >= points.size())
						nextIdx = 0;

					Location location = new Location("FakeLocationProvider");
					location.setLatitude(currentLoc.latitude);
					location.setLongitude(currentLoc.longitude);
					location.setAccuracy(20);
					location.setTime(System.currentTimeMillis());
					location.setSpeed(step.distance.value/step.duration.value);
					location.setBearing((float)LatLngTool.bearing(prevLoc, currentLoc));
					
					publishProgress(location);

					// sleep until the next reporting time
					try {
						Thread.sleep(UPDATE_PERIOD);
					} catch (InterruptedException e) {
						Log.i("QQQ", "exception");
						e.printStackTrace();
						return Boolean.FALSE;
					}
				}

			}

			return Boolean.TRUE;
		}

		protected void onProgressUpdate(Location... progress) {
			// provide fake location
			mPosTracker.updateLocation(progress[0]);
		}

		protected void onPostExecute(Boolean results) {
			Log.i("QQQ", "done");
			
			// sleep for a second
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Log.i("QQQ", "exception");
				e.printStackTrace();
			}
			cleanUp();
			mClient.onFinishTestJourney();
		}
	}

	public TestJourney(MainActivity context, MapWrapper map, 
			PositionTracker posTracker, DebrisTracker data){		
		mContext = context;

		mMap = map;
		
		mPosTracker = posTracker;
		
		mData = data;

		markers = new ArrayList<Object>();
		
		// all the location will be coming from us mwuahahaha..
		mPosTracker.hijackLocationProvider(true);
		// listen to notification from map
		mMap.hijackNotification(true, this);
		
		mData.hijackState(true);

		// ask for start point
		Toast.makeText(mContext.getBaseContext(), "Choose start point", Toast.LENGTH_SHORT).show();
		
		mExitFlag = false;
	}
	
	public void subscribe(Client client){
		mClient = client;
	}

	// this is the test
	private void runTest(){
		
		// draw the path 
		markers.add(mMap.addPathMarker(PolylineDecoder.decodePoly(mJsonParams.getOverview_polyline())));

		mTestHandle = new RunningSimulation();
		mTestHandle.execute(mJsonParams);

	}


	@Override
	public void onFinishProcessHttp(String result) {
		
		if(mExitFlag){
			// everything has been clean up, just return;
			return;
		}
		
		// the first result would be for start point
		if(!mMap.hasSnippet(startMarker)){
			GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
			if(params.isValid())
				mMap.setSnippet(startMarker, params.getAddress());
			else
				mMap.setSnippet(startMarker, "cannot get location!");
		}
		else if(!mMap.hasSnippet(endMarker)){
			GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
			if(params.isValid())
				mMap.setSnippet(endMarker, params.getAddress());
			else
				mMap.setSnippet(endMarker, "cannot get location!");
		}
		else {

			// parse and start the test here
			// JSONObject is a java.util.Map and JSONArray is a java.util.Lis
			mJsonParams = new GoogleDirJsonParams((JSONObject)JSONValue.parse(result));
			if(mJsonParams.isValid())
				runTest();
		}
	}
	
	public void cleanUp(){
		mData.hijackState(false);
		mMap.hijackNotification(false, null);
		mPosTracker.hijackLocationProvider(false);
		
		startMarker = null;
		endMarker = null;
		mMap.removeMarkers(markers, true);
		markers.clear();
	}

	public void forceStop() {
		cleanUp();
		
		mExitFlag = true;
		
		if(mTestHandle != null) 
			mTestHandle.cancel(true);
		
	}

	@Override
	public void onMapClick(LatLng latLng) {
		
		if(mExitFlag)
			return;
		
		if(startPoint==null){
			startPoint = latLng;

			startMarker = mMap.addStartPointMarker(startPoint);
			markers.add(startMarker);

			// ask for end point
			Toast.makeText(((Activity) mClient).getBaseContext(), "Choose end point", Toast.LENGTH_SHORT).show();
			
			String startPointURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
					"latlng=" + startPoint.latitude + "," + startPoint.longitude + "&" +
					"sensor=true";
			new WebWrapper(this).get(startPointURI);
		}
		else {
			endPoint = latLng;

			endMarker = mMap.addEndPointMarker(endPoint);
			markers.add(endMarker);
			
			String endPointURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
					"latlng=" + endPoint.latitude + "," + endPoint.longitude + "&" +
					"sensor=true";
			
			new WebWrapper(this).get(endPointURI);
			
			// we're done here, let the map go
			mMap.hijackNotification(false, null);

			// we got everything we need, trigger the webservice request now
			String uri = "http://maps.googleapis.com/maps/api/directions/json?" +
					"origin=" + startPoint.latitude + "," + startPoint.longitude + "&" +
					"destination=" + endPoint.latitude + "," + endPoint.longitude + "&" +
					"sensor=true&units=metric";
			new WebWrapper(this).get(uri);
		}
		
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}



}