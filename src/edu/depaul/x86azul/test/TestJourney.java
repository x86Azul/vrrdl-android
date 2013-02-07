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

import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

import edu.depaul.x86azul.GoogleJsonParams;
import edu.depaul.x86azul.MapWrapper;
import edu.depaul.x86azul.PolylineDecoder;
import edu.depaul.x86azul.WebWrapper;
import edu.depaul.x86azul.GoogleJsonParams.Step;
import edu.depaul.x86azul.helper.LatLngTool;


public class TestJourney implements LocationSource, WebWrapper.Client {

	private final int FAST_FORWARD_FACTOR = 5;
	private LatLng startPoint;
	private LatLng endPoint;

	private Client mClient;
	private GoogleJsonParams mJsonParams;
	private ArrayList<OnLocationChangedListener> mSubscribers;

	private RunningSimulation mTestHandle;
	private MapWrapper mMap;
	public boolean isRunning;

	private ArrayList<Object> markers;

	public interface Client {
		public void onFinishTestJourney();
	}

	private class RunningSimulation extends AsyncTask <Object, Location, Boolean> {

		@Override
		protected Boolean doInBackground(Object... testParams) {
			GoogleJsonParams params = (GoogleJsonParams)testParams[0];

			if(!params.isValid())
				return Boolean.FALSE;

			for(int i=0; i<params.getSteps().size();i++){

				Step step = params.getSteps().get(i);

				if (isCancelled()) return Boolean.FALSE;

				List<LatLng> points = PolylineDecoder.decodePoly(step.polyline.points);
				double move = (double)step.distance.value/step.duration.value; // in mps

				move *= FAST_FORWARD_FACTOR;

				// point to next array index
				int nextIdx = 1;
				LatLng currentLoc = points.get(0);
				while(nextIdx != 0){

					double stepTaken = 0;

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

					Log.w("QQQ", "move=" + stepTaken + ",point " + nextIdx + "-outOf-"+ points.size() + " " + currentLoc);


					Location location = new Location("FakeLocationProvider");
					location.setLatitude(currentLoc.latitude);
					location.setLongitude(currentLoc.longitude);
					location.setAccuracy(20);
					location.setTime(System.currentTimeMillis());
					location.setSpeed(step.distance.value/step.duration.value);

					publishProgress(location);

					// sleep for a second
					try {
						Thread.sleep(1000);
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
			for(int i=0;i<mSubscribers.size();i++){
				mSubscribers.get(i).onLocationChanged(progress[0]);
			}
		}


		protected void onPostExecute(Boolean results) {
			Log.i("QQQ", "done");
			
			// sleep for a second
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Log.i("QQQ", "exception");
				e.printStackTrace();
			}
			
			mClient.onFinishTestJourney();
		}
	}

	public TestJourney(Activity activity, MapWrapper map){
		mClient = (Client)activity;

		mMap = map;

		mSubscribers = new ArrayList<OnLocationChangedListener>();

		markers = new ArrayList<Object>();

		// ask for start point
		Toast.makeText(((Activity) mClient).getBaseContext(), "Choose start point", Toast.LENGTH_SHORT).show();
	}

	// this is the test
	private void runTest(){

		if(!mJsonParams.status.equals("OK")){
			Log.w("QQQ", "Test not running because mJsonParams.status=" + mJsonParams.status);
			return;
		}

		// draw the path 
		markers.add(mMap.addPathMarker(PolylineDecoder.decodePoly(mJsonParams.getOverview_polyline())));

		mTestHandle = new RunningSimulation();
		mTestHandle.execute(mJsonParams);

	}

	public boolean needCoordinate(){
		if(startPoint==null || endPoint==null)
			return true;
		else
			return false;
	}

	public void setCoordinate(LatLng location) {
		// the first one should be the start, follow by end
		if(startPoint==null){
			startPoint = location;

			markers.add(mMap.addStartPointMarker(startPoint));

			// ask for end point
			Toast.makeText(((Activity) mClient).getBaseContext(), "Choose end point", Toast.LENGTH_SHORT).show();
		}
		else {
			endPoint = location;

			markers.add(mMap.addEndPointMarker(endPoint));

			// we got everything we need, trigger the webservice request now
			String uri = "http://maps.googleapis.com/maps/api/directions/json?" +
					"origin=" + startPoint.latitude + "," + startPoint.longitude + "&" +
					"destination=" + endPoint.latitude + "," + endPoint.longitude + "&" +
					"sensor=true&units=metric";
			new WebWrapper(this).get(uri);
		}
	}

	@Override
	public void onFinishProcessHttp(String result) {

		// parse and start the test here
		// JSONObject is a java.util.Map and JSONArray is a java.util.Lis
		mJsonParams = new GoogleJsonParams((JSONObject)JSONValue.parse(result));

		runTest();
	}

	public void forceStop() {

		if(mTestHandle != null) 
			mTestHandle.cancel(true);
		
		mMap.removeMarkers(markers, true);
		markers.clear();
	}

	@Override
	public void activate(OnLocationChangedListener client) {

		mSubscribers.add(client);	

	}

	@Override
	public void deactivate() {
		// this is a workaround method to really sure who's calling us
		// because we might have more than one subscribers

		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

		for (int i=0;i<stackTraceElements.length;i++){
			String targetClassName = stackTraceElements[i].getClassName();
			String targetMethodName = stackTraceElements[i].getMethodName();

			if(targetClassName.equals(this.getClass().getName()) &&
					targetMethodName.equals("deactivate"))
			{
				// the caller class would be the next index
				targetClassName = stackTraceElements[i+1].getClassName();
				targetMethodName = stackTraceElements[i+1].getMethodName();

				for (int j=0;j<mSubscribers.size();j++){					
					if(mSubscribers.get(j).getClass().getName().contains(targetClassName) || 
							targetClassName.contains(mSubscribers.get(j).getClass().getName())) {
						mSubscribers.remove(j);
					}
				}		
				break;
			}	
		}
	}



}