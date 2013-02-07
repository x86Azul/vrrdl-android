package edu.depaul.x86azul;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;

import edu.depaul.x86azul.helper.LatLngTool;


import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;


/* 
 * keeping track of all debris location in the map
 */
public class DebrisTracker {

	private static final double DANGER_ZONE_IN_METERS = 2000.0;

	private DbAdapter mDbAdapter;
	
	// these arrays MUST be sync with each other
	private ArrayList<Debris> debrises;
	private ArrayList<ArrayList<Object>> markers;
	private ArrayList<Boolean> dangerFlag; 
	private Client mClient;
	
	private Location lastGoodLocation;

	// we need this because we need to track the debris marker as well!
	private MapWrapper mMap;

	public interface Client {

		public void onPopulateDataFromDbCompleted();
		public void onOpenDbCompleted();
		public void onCloseDbCompleted();
	}

	public DebrisTracker(Context client, MapWrapper map){

		mClient = (Client) client;
		mMap = map;
		
		debrises = new ArrayList<Debris>();
		markers = new ArrayList<ArrayList<Object>>();
		dangerFlag = new ArrayList<Boolean>();
		
		mDbAdapter = new DbAdapter(client);
	}

	public void initialize() {
		new PopulateDataFromDatabase().execute(null, null, null);
	}

	public void open() {
		new OpenDatabase().execute(null, null, null);
	}

	public void close() {
		new CloseDatabase().execute(null, null, null);
	}
	
	public int getSize(){
		return debrises.size();
	}
	
	public void insert(Debris debris){
		// make sure to insert to database first
		// this will assign debris id as well
    	mDbAdapter.insertDebris(debris);
    	
    	debrises.add(debris);
    	
		// do we need to set this as dangerous?
		if(lastGoodLocation != null){
			Double distanceInMeters = LatLngTool.distance(lastGoodLocation, debris.getLatLng());

			// it's dangerous!
			if (distanceInMeters < DANGER_ZONE_IN_METERS){
				markers.add(mMap.addDebrisMarker(debris, true, true));
				dangerFlag.add(Boolean.TRUE);
			}
			else {
				markers.add(mMap.addDebrisMarker(debris, false, true));
				dangerFlag.add(Boolean.FALSE);
			}
		}
		else {
			markers.add(mMap.addDebrisMarker(debris, false, true));
			dangerFlag.add(Boolean.FALSE);
		}
	}

	public class PopulateDataFromDatabase extends AsyncTask<Void, Void, Void> {

		protected void onPostExecute(Void result) {
					
			// get the corresponding markers for the debrises
			for (int i = 0; i < debrises.size(); i++) {
				Debris debris = debrises.get(i);	
				markers.add(mMap.addDebrisMarker(debris, false, false));
				dangerFlag.add(Boolean.FALSE);
			}
			
			// let user know we complete the initilization
			mClient.onPopulateDataFromDbCompleted();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mDbAdapter.open();
			
			List<Debris> debrisList = mDbAdapter.getAllDebrisRecords();

			for (int i = 0; i < debrisList.size(); i++) {
				Debris debris = (Debris)debrisList.get(i);
				debrises.add(debris);				
			}
			
			return null;
		}

	}

	public class OpenDatabase extends AsyncTask<Void, Void, Void> {

		protected void onPostExecute(Void result) {
			// let user know we complete the initilization
			mClient.onOpenDbCompleted();
		}

		@Override
		protected Void doInBackground(Void... params) {

			mDbAdapter.open();

			return null;
		}
	}

	public class CloseDatabase extends AsyncTask<Void, Void, Void> {

		protected void onPostExecute(Void result) {
			// let user know we complete the initilization
			mClient.onCloseDbCompleted();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mDbAdapter.close();
			return null;
		}
	}

	public void analyzeNewLocation(Location location) {
		for(int i=0; i<debrises.size(); i++){
			Debris debris = debrises.get(i);
			Boolean danger = dangerFlag.get(i);
			Double distanceInMeters = LatLngTool.distance(location, debris.getLatLng());
			
			if (distanceInMeters < DANGER_ZONE_IN_METERS){
				if(danger == Boolean.FALSE){
					// set as dangerFlag marker
					dangerFlag.set(i, Boolean.TRUE);
					markers.set(i, mMap.setAsDangerousMarkers(debris, markers.get(i)));
				}
			}
			else {
				if(danger == Boolean.TRUE){
					// set as non dangerFlag marker
					dangerFlag.set(i, Boolean.FALSE);
					markers.set(i, mMap.setAsNonDangerousMarkers(debris, markers.get(i)));
				}
			}
		}
		
		lastGoodLocation = location;
		
	}

	public void resetData() {
		
		debrises.clear();
		dangerFlag.clear();
		
		// clear all markers from map, animate
		mMap.removeAllMarkers(markers, true);
		markers.clear();
		
		mDbAdapter.resetDebris();
	}
}