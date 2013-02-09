package edu.depaul.x86azul;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import edu.depaul.x86azul.helper.LatLngTool;

import android.annotation.SuppressLint;
import android.location.Location;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

/* 
 * keeping track of all debris location data and their markers on map
 */
public class DebrisTracker implements MapWrapper.GestureClient, DbAdapter.Client {

	private static final double DANGER_ZONE_IN_METERS = 2000.0;
	//private static final double SIGHT_ANGLE_IN_DEGREE = 60; 
	private static final double MIN_VELOCITY_TRACKING = 1; 

	private MapWrapper mMap;
	private DbAdapter mDbAdapter;
	
	private CompassController mCompass;
	
	private enum DangerFlag {
		NON_DANGER, DANGER, LETHAL
	}
	
	private MainActivity mContext;
	
	// these arrays MUST be sync with each other
	private ArrayList<Debris> debrises;
	private ArrayList<ArrayList<Object>> markers;
	private ArrayList<DangerFlag> dangerFlag; 
	
	private Location lastGoodLocation;
	private boolean mInDanger;
	
	private boolean mTapMeansInsert;
	
	private MediaPlayer mMediaHandle;
	
	private List<Object> mBackUp;

	public DebrisTracker(MainActivity context, MapWrapper map){

		mContext = context;
		mMap = map;
		mMap.subscribeGestureAction(this);
		
		mDbAdapter = new DbAdapter(mContext);
		mDbAdapter.subscribe(this);
		
		
		debrises = new ArrayList<Debris>();
		markers = new ArrayList<ArrayList<Object>>();
		dangerFlag = new ArrayList<DangerFlag>();
		
		mCompass = new CompassController(mContext);
		
		mBackUp = new ArrayList<Object>();
		
		mTapMeansInsert = true;
		mInDanger = false;

	}
	
	public void hijackState(boolean hijack) {
		if(hijack){
			if(mBackUp.size() == 0){
				mBackUp.add(mTapMeansInsert);
				mBackUp.add(mInDanger);
			}
		}
		else {
			if(mBackUp.size()!=0){
				mTapMeansInsert = (Boolean)mBackUp.get(0);
				mInDanger = (Boolean)mBackUp.get(1);
				mBackUp.clear();
			}
		}
		
	}
	
	public void open(){
		mDbAdapter.initialize();
		mCompass.open();
	}
	
	public void close(){
		mDbAdapter.close();
		mCompass.close();
	}
	
	public int getDataSize(){
		if (debrises != null)
			return debrises.size();
		else
			return 0;
	}
	
	private void insert(Debris debris, boolean animate){
		// make sure to insert to database first
		// this will assign debris id as well
		if (!debris.mInDatabase)
			mDbAdapter.insertDebris(debris);
    	
    	debrises.add(debris);
    	
    	// now for the markers and the dangerous flag, we need to check the location
		if(lastGoodLocation != null){

			if(isDanger(lastGoodLocation, debris)){
				markers.add(mMap.addDangerousDebrisMarker(debris, animate));
				dangerFlag.add(DangerFlag.DANGER);
			}
			else {
				markers.add(mMap.addNonDangerousDebrisMarker(debris, animate));
				dangerFlag.add(DangerFlag.NON_DANGER);
			}
		}
		else {
			markers.add(mMap.addNonDangerousDebrisMarker(debris, animate));
			dangerFlag.add(DangerFlag.NON_DANGER);
		}
	}
	
	public void insert(Debris debris){
		// will animate by default
		insert(debris, true);
	}

	public void analyzeNewLocation(Location newLocation) {
		
		double distanceToLastGoodLocation = 0;
		boolean inDanger = false; // we're not in danger by default
		
		if(lastGoodLocation != null)
			distanceToLastGoodLocation = LatLngTool.distance(newLocation, lastGoodLocation);
			
			
		for(int i=0; i< getDataSize(); i++){
			
			Debris debris = debrises.get(i);
			
			// check if we need this calculation
			// the worst possible scenario is that user directly come towards us (this particular debris)
			// everytime, so the new distance would need to be directly substracted
			
			// TODO: find a good algorithm for distance calculation
			/*if(lastGoodLocation != null){
				if(debris.mDistanceToUser - distanceToLastGoodLocation > DANGER_ZONE_IN_METERS){
					
					debris.mDistanceToUser -= distanceToLastGoodLocation;
					continue;
				}
			}*/
		
			DangerFlag flag = dangerFlag.get(i);			
			// here's the rule.. the only illegal transition is
			// from non-danger -> lethal
			// this time we're going to see if a debris can be put/remove from non-danger flag
			// the danger-lethal checking will be done at the next step	
			// recalculate the new distance	
			if (isDanger(newLocation, debris)){
				if(flag == DangerFlag.NON_DANGER){
					// set as dangerFlag marker
					dangerFlag.set(i, DangerFlag.DANGER);
					markers.set(i, mMap.setAsDangerousMarkers(debris, markers.get(i)));
				}
			}
			else {
				if(flag != DangerFlag.NON_DANGER){
					// set as non dangerFlag marker
					dangerFlag.set(i, DangerFlag.NON_DANGER);
					markers.set(i, mMap.setAsNonDangerousMarkers(debris, markers.get(i)));
				}
			}
		}

		// check if we're in danger
		for(int i=0; i < getDataSize(); i++){
			if(dangerFlag.get(i) != DangerFlag.NON_DANGER){
				inDanger = true;
				break; 
			}

			// we arrive at the last debris and it's not dangerous
			if((i == dangerFlag.size()-1) && 
					(dangerFlag.get(i) == DangerFlag.NON_DANGER)){
				inDanger = false;
				break; 
			}
		}

		// there is at least one debris which is not non-dangerous
		if(inDanger){

			// see which debris is considered as the lethal one
			int mostDangerIdx = Integer.MAX_VALUE;
			int lastLethalIdx = Integer.MAX_VALUE;
			double highestLethalIdxValue = 0;

			for(int i=0; i<dangerFlag.size(); i++){
				if (dangerFlag.get(i) != DangerFlag.NON_DANGER){

					// consider the bearing for lethal idx calculation 
					Debris debris = debrises.get(i);
					debris.mBearingToUser = LatLngTool.bearing(newLocation, debris);

					double lethalIdxValue = getLethalIdxValue(debris);

					if(lethalIdxValue > highestLethalIdxValue){
						highestLethalIdxValue = lethalIdxValue;
						mostDangerIdx = i;
					}
				}

				if (dangerFlag.get(i) == DangerFlag.LETHAL){
					// grab the last lethal index while we're in the loop
					lastLethalIdx = i;
				}
			}

			// do we need some change here?
			if(lastLethalIdx != mostDangerIdx){
				// previously there is one lethal debris recorded, downgrade to danger
				if(lastLethalIdx!= Integer.MAX_VALUE){
					// change the lethal index
					dangerFlag.set(lastLethalIdx, DangerFlag.DANGER);
					markers.set(lastLethalIdx, mMap.setAsDangerousMarkers(
							debrises.get(lastLethalIdx), markers.get(lastLethalIdx)));
				}

				// there's one debris which can be considered lethal
				if(mostDangerIdx != Integer.MAX_VALUE){
					// upgrade this one to lethal
					dangerFlag.set(mostDangerIdx, DangerFlag.LETHAL);
					markers.set(mostDangerIdx, mMap.setAsLethalMarkers(
							debrises.get(mostDangerIdx), markers.get(mostDangerIdx)));
				}
			}
		}

		lastGoodLocation = newLocation;

		setOverallDangerStatus(inDanger);
		
		if(mCompass.isActive())
			compassCalculation();
	}

	@SuppressLint("NewApi")
	private void compassCalculation(){
		// this is used for compass pointer

		if(getDataSize()==0){
			mCompass.hide();
			return;
		}
        
		int pointDebrisIdx = Integer.MAX_VALUE;
		double pointDebrisValue = Double.MAX_VALUE;
		
		for(int i=0; i<getDataSize(); i++){

			Debris debris = debrises.get(i);
			if(debris.mDistanceToUser < pointDebrisValue){
				pointDebrisValue = debris.mDistanceToUser;
				pointDebrisIdx = i;
			}
			
			// if there's a lethal debris than pick that one
			if(dangerFlag.get(i) == DangerFlag.LETHAL){
				pointDebrisIdx = i;
				break;
			}			
		}	
		
		if(pointDebrisIdx!=Integer.MAX_VALUE){
			Double bearing = LatLngTool.bearing(lastGoodLocation, debrises.get(pointDebrisIdx));
			
			mCompass.setDebrisBearing(bearing, mInDanger);
			
		}
		
	}

	private void setOverallDangerStatus(boolean inDanger){
		// do we have different situation here?
		if(mInDanger != inDanger){

			if(inDanger){
				// we're entering danger zone
				if(mMediaHandle != null){
					mMediaHandle.stop();
					mMediaHandle.release();
					mMediaHandle=null;
				}
				mMediaHandle = MediaPlayer.create(mContext, R.raw.warning_tone);
				mMediaHandle.start(); // no need to call prepare(); create() does that for you
			}
			else {
				// we're out of danger zone
				if(mMediaHandle != null){
					mMediaHandle.stop();
					mMediaHandle.release();
					mMediaHandle=null;
				}
				mMediaHandle = MediaPlayer.create(mContext, R.raw.relief_tone);
				mMediaHandle.start(); // no need to call prepare(); create() does that for you
			}

			mInDanger = inDanger;
		}
	}
	
	private boolean isDanger(Location location, Debris debris){
		// get distance
		debris.mDistanceToUser = LatLngTool.distance(location, debris.getLatLng());
		
		// is this dangerous?
		if((debris.mDistanceToUser < DANGER_ZONE_IN_METERS) &
				(location.getSpeed() > MIN_VELOCITY_TRACKING)){
			return true;
		}
		else {
			return false;
		}
		
	}
	
	private double getLethalIdxValue(Debris debris){
		double bearingVal;
		double degree = debris.mBearingToUser>180?(360-debris.mBearingToUser):debris.mBearingToUser;
		
		//Log.i("QQQ", "bearing=" + debris.mBearingToUser + ",degree=" + degree);
		
		if(degree < 30)
			bearingVal = 1;
		else if(degree < 45)
			bearingVal = 0.8;
		else if(degree < 60)
			bearingVal = 0.6;
		else if(degree < 90)
			bearingVal = 0.2;
		else
			bearingVal = 0.1;
		
		double distanceVal = (DANGER_ZONE_IN_METERS - debris.mDistanceToUser)/DANGER_ZONE_IN_METERS;
		
		// return distanceVal*0.5 + bearingVal*0.5;
		return Math.pow(distanceVal,2)*bearingVal;
	}
	
	/*
	private boolean withinSight(Location loc, Debris debris){
		double debrisBearing = LatLngTool.bearing(loc, debris);
		
		// this is tricky because degree value reset to 0 after 360
		// test all the possible scenario
		if((debrisBearing < (loc.getBearing()+ SIGHT_ANGLE_IN_DEGREE)) &&
				(debrisBearing > (loc.getBearing()- SIGHT_ANGLE_IN_DEGREE))){
			return true;
		}
		if((debrisBearing + 360 < (loc.getBearing()+ SIGHT_ANGLE_IN_DEGREE)) &&
				(debrisBearing + 360 > (loc.getBearing()- SIGHT_ANGLE_IN_DEGREE))){
			return true;
		}
		if((debrisBearing - 360 < (loc.getBearing()+ SIGHT_ANGLE_IN_DEGREE)) &&
				(debrisBearing - 360 > (loc.getBearing()- SIGHT_ANGLE_IN_DEGREE))){
			return true;
		}
		
		return false;
	}
	*/
	
	public void resetData() {
		debrises.clear();
		dangerFlag.clear();
		
		// clear all markers from map, animate
		mMap.removeAllMarkers(markers, true);
		markers.clear();
		
		mDbAdapter.clearTable();
	}
	
	public void setTapMeansInsertMode(boolean enable){
		mTapMeansInsert = enable;
	}

	@Override
	public void onMapClick(LatLng latLng) {

		if(mTapMeansInsert) {
			// insert marker for testing
	    	Debris debris = new Debris(latLng);
	    	// need to put into data first
	    	insert(debris);
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

	@Override
	public void onInitDbCompleted(List<Debris> data) {
		if(data!=null)
			for (int i = 0; i < data.size(); i++) {
				insert(data.get(i), false);
			}
	}

	@Override
	public void onCloseDbCompleted() {
		// TODO Auto-generated method stub
		
	}


}