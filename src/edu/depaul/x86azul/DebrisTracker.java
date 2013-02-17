package edu.depaul.x86azul;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import edu.depaul.x86azul.Debris.DangerFlag;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.map.MarkerWrapper;
import edu.depaul.x86azul.map.MarkerWrapper.Type;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;

/* 
 * keeping track of all debris location data and their markers on map
 */
public class DebrisTracker implements MapWrapper.GestureClient, DbAdapter.Client{

	private static final double DANGER_ZONE_IN_METERS = 2000.0;
	//private static final double SIGHT_ANGLE_IN_DEGREE = 60; 
	//private static final double MIN_VELOCITY_TRACKING = 1; 
	
	private static final long serialVersionUID = 0L;

	private MapWrapper mMap;
	private DbAdapter mDbAdapter;
	
	private CompassController mCompass;
	
	private MainActivity mContext;
	
	// these arrays MUST be sync with each other
	private ArrayList<Debris> debrises;
	private ArrayList<MarkerWrapper> markers;
	
	private Location lastGoodLocation;
	private boolean mInDanger;
	
	private boolean mTapMeansInsert;
	
	private MediaPlayer mMediaHandle;
	
	private List<Object> mBackUp;

	public DebrisTracker(MainActivity context, MapWrapper map){

		mContext = context;
		
		/*
		// grab compass action
		View view = mContext.findViewById(R.id.compass);
		view.setOnClickListener(this);
		view.setOnLongClickListener(this);
		*/
		
		mMap = map;
		mMap.subscribeGestureAction(this);
		
		mDbAdapter = new DbAdapter(mContext);
		mDbAdapter.subscribe(this);
		
		
		debrises = new ArrayList<Debris>();
		markers = new ArrayList<MarkerWrapper>();
		
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
				// come with a fresh start
				mInDanger = false;
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
	
	private void insert(Debris debris, boolean animate, boolean update){
		// make sure to insert to database first
		// this will assign debris id as well
		if (!debris.mInDatabase)
			mDbAdapter.insertDebris(debris);
    
		// by default the type in non danger unless state otherwise
    	debris.mDangerFlag = DangerFlag.NON_DANGER;
    
		if(lastGoodLocation != null)
			if(isInDangerZone(lastGoodLocation, debris))
				debris.mDangerFlag = DangerFlag.DANGER;

		// now for the markers
    	MarkerWrapper marker = new MarkerWrapper(Type.DEBRIS)
    							.coordinate(debris.getLatLng())
    							.width(debris.mAccuracy)
    							.title("Debris#" + debris.mDebrisId)
    							.snippet(debris.mTime)
								.dangerFlag(debris.mDangerFlag);
    	
    	mMap.insertMarker(marker, animate);	
    	
    	// now add them for us to control
    	debrises.add(debris);
		markers.add(marker);
		
		if(update)
			updateStatus();
	}
	
	public void insert(Debris debris){
		// will animate by default
		// will directly update by default
		insert(debris, true, true);
	}

	public void analyzeNewLocation(Location newLocation) {
		
		double distanceToLastGoodLocation = 0;
		
		if(lastGoodLocation != null)
			distanceToLastGoodLocation = MyLatLng.distance(newLocation, lastGoodLocation);
			
			
		for(int i=0; i< getDataSize(); i++){
			
			Debris debris = debrises.get(i);
			MarkerWrapper marker = markers.get(i);
			
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
		
		
			// here's the rule.. the only illegal transition is
			// from non-danger -> lethal
			// this time we're going to see if a debris can be put/remove from non-danger flag
			// the danger-lethal checking will be done at the next step	
			// recalculate the new distance	
			if (isInDangerZone(newLocation, debris)){
				if(debris.mDangerFlag == DangerFlag.NON_DANGER){
					// set as dangerFlag marker
					debris.mDangerFlag = DangerFlag.DANGER;
					marker.dangerFlag(DangerFlag.DANGER);
				}
			}
			else {
				if(debris.mDangerFlag != DangerFlag.NON_DANGER){
					// set as non dangerFlag marker
					debris.mDangerFlag = DangerFlag.NON_DANGER;
					marker.dangerFlag(DangerFlag.NON_DANGER);
				}
			}
		}
		
		lastGoodLocation = newLocation;

		updateStatus();
	}
	
	/*
	 * this to count the debris bearing, danger status, compass, 
	 * and decide which debris is lethal
	 */
	private void updateStatus(){
		
		// check if we're in danger
		boolean inDanger = false; 
		
		int dataSize = getDataSize();
		for(int i=0; i < dataSize; i++){
			Debris debris = debrises.get(i);
			if(debris.mDangerFlag != DangerFlag.NON_DANGER){
				inDanger = true;
				break; 
			}

			// we arrive at the last debris and it's not dangerous
			if((i == dataSize -1) && 
				(debris.mDangerFlag == DangerFlag.NON_DANGER)){
				
				inDanger = false;
				break; 
			}
		}

		// there is at least one debris which is not non-dangerous (can be danger or lethal)
		if(inDanger){

			// see which debris is considered as the lethal one
			int mostDangerIdx = Integer.MAX_VALUE;
			int lastLethalIdx = Integer.MAX_VALUE;
			double highestLethalIdxValue = 0;

			for(int i=0; i<dataSize; i++){
				Debris debris = debrises.get(i);
				if (debris.mDangerFlag != DangerFlag.NON_DANGER){

					// consider the bearing for lethal idx calculation
					debris.mBearingToUser = MyLatLng.bearing(lastGoodLocation, debris);

					double lethalIdxValue = getLethalIdxValue(debris, lastGoodLocation);

					// only valid if above minimum threshold
					if(lethalIdxValue >= getMinimumLethalIdxValue() && 
							lethalIdxValue > highestLethalIdxValue){
						highestLethalIdxValue = lethalIdxValue;
						mostDangerIdx = i;
					}
				}

				if (debris.mDangerFlag == DangerFlag.LETHAL){
					// grab the last lethal index while we're in the loop
					lastLethalIdx = i;
				}
			}

			// do we need some change here?
			if(lastLethalIdx != mostDangerIdx){
				// previously there is one lethal debris recorded, downgrade to danger
				if(lastLethalIdx!= Integer.MAX_VALUE){
					// change the lethal index
					debrises.get(lastLethalIdx).mDangerFlag = DangerFlag.DANGER;
					markers.get(lastLethalIdx).dangerFlag(DangerFlag.DANGER);
				}

				// there's one debris which can be considered lethal
				if(mostDangerIdx != Integer.MAX_VALUE){
					// upgrade this one to lethal
					debrises.get(mostDangerIdx).mDangerFlag = DangerFlag.LETHAL;
					markers.get(mostDangerIdx).dangerFlag(DangerFlag.LETHAL);
				}
			}
		}

		setOverallDangerStatus(inDanger);

		if(mCompass.isActive())
			compassCalculation();
	}

	@SuppressLint("NewApi")
	private void compassCalculation(){
		// compass will point to a debris with the following priority
		// 1. the lethal one (if exist)
		// 2. the closest

		if(getDataSize()==0 || lastGoodLocation == null){
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
			if(debris.mDangerFlag == DangerFlag.LETHAL){
				pointDebrisIdx = i;
				break;
			}			
		}	
		
		if(pointDebrisIdx!=Integer.MAX_VALUE){			
			mCompass.setDebrisBearing(lastGoodLocation, debrises.get(pointDebrisIdx), mInDanger);
		}
		
	}

	/* 
	 * set the danger flag and play tone if there's change
	 */
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
	
	private boolean isInDangerZone(Location location, Debris debris){
		// get distance
		debris.mDistanceToUser = MyLatLng.distance(location, debris.getLatLng());
		
		// is this dangerous?
		//if((debris.mDistanceToUser < DANGER_ZONE_IN_METERS) &
			//	(location.getSpeed() > MIN_VELOCITY_TRACKING)){
		if(debris.mDistanceToUser < DANGER_ZONE_IN_METERS){
			return true;
		}
		else {
			return false;
		}
		
	}
	
	private double getLethalIdxValue(Debris debris, Location loc){
		double userDegree; // relative bearing to user heading direction
		
		userDegree = debris.mBearingToUser - loc.getBearing();
		// get in absolute value (0 - 180 degrees)
		userDegree = (userDegree + 360)%360; 
		userDegree = userDegree > 180?(360-userDegree):userDegree;
		
		//Log.i("QQQ", "bearing=" + debris.mBearingToUser + ",degree=" + degree);
		// the closest to point of view the bigger the value
		double bearingVal=0;
		if(userDegree < 30)
			bearingVal = 1;
		else if(userDegree < 45)
			bearingVal = 0.8;
		else if(userDegree < 60)
			bearingVal = 0.5;
		else if(userDegree < 90)
			bearingVal = 0.3;
		else
			bearingVal = 0.05;
		
		// the closest the distance the higher the number
		double distanceVal = (DANGER_ZONE_IN_METERS*1.5 - debris.mDistanceToUser);
		
		double velocityVal = loc.getSpeed()/10;
		
		// return distanceVal*0.5 + bearingVal*0.5;
		return distanceVal*bearingVal*velocityVal;
	}
	
	private double getMinimumLethalIdxValue(){
		return DANGER_ZONE_IN_METERS*.5*1.0;
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
		
		// clear all markers from map, animate
		for(int i=0; i< markers.size(); i++)
			markers.get(i).removeFromMap(true);
		
		markers.clear();
		
		updateStatus();
		
		mDbAdapter.clearTable();
	}
	
	public void setTapMeansInsertMode(boolean enable){
		mTapMeansInsert = enable;
	}

	@Override
	public void onMapClick(MyLatLng latLng) {

		if(mTapMeansInsert) {
			// insert marker for testing
	    	Debris debris = new Debris(latLng);
	    	// need to put into data first
	    	insert(debris);
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
	public void onInitDbCompleted(List<Debris> data) {
		if(data!=null){
			// insert all data first then update the status
			for (int i = 0; i < data.size(); i++) {
				insert(data.get(i), false, false);
			}
			updateStatus();
		}
	}

	@Override
	public void onCloseDbCompleted() {
		// TODO Auto-generated method stub
		
	}

	public Object getCompassTarget() {
		Debris debris = mCompass.getPointingDebris();
		if(debris!= null){
			return mMap.getNewViewPosition(null, 0, MyLatLng.inLatLng(lastGoodLocation), debris.getLatLng());
		} else {
			return null;
		}
	}
	
	public Object showDebrisTarget(long debrisId) {
		for(int i=0; i < getDataSize(); i++){
			Debris debris = debrises.get(i);
			if(debris.mDebrisId == debrisId){
				return mMap.getNewViewPosition(debris.getLatLng(), MapWrapper.ZOOM_CLOSE_UP, null, null);
			}
		}
		
		return null;
	}
	
	public void parcellToIntent(Intent intent){
		intent.putExtra("DataSize", getDataSize());
		for(int i=0; i < getDataSize(); i++){
			Debris debris = debrises.get(i);
			intent.putExtra("id"+i, debris.mDebrisId);
			intent.putExtra("distance"+i, debris.mDistanceToUser);
			intent.putExtra("address"+i, debris.mAddress);
			intent.putExtra("dangerFlag"+i, debris.mDangerFlag.name());

		}
	}
	
	public static ArrayList <HashMap<String,String >> IntentToParcell(Intent intent){

		ArrayList <HashMap<String,String>> listItem =  new ArrayList <HashMap<String, String>>( );

		int dataSize = intent.getIntExtra("DataSize", 0);
		for(int i=0; i < dataSize; i++){

			HashMap <String, String> map= new HashMap<String, String>();

			long id = intent.getLongExtra("id"+i, 0);
			map.put("idOriginal" , String.valueOf(id)); 
			map.put("id" , "Debris#" + id); 
			
			map.put("address" , intent.getStringExtra("address"+i));
			
			
			// we need to parse the distance to mile
			double meterValue = intent.getDoubleExtra("distance"+i, 0);
			map.put("distanceOriginal" , String.valueOf(meterValue)); 
			
			double mileValue = meterValue * 0.000621371;
			String mileString = null;
			if (mileValue < 10.0)
				mileString = new DecimalFormat("0.00").format(mileValue);
			else if (mileValue < 100.0) 
				mileString = new DecimalFormat("0.0").format(mileValue);
			else
				mileString = new DecimalFormat("0").format(mileValue);
			
			map.put("distance" , mileString ); 

			if(DangerFlag.valueOf(intent.getStringExtra("dangerFlag"+i)) == DangerFlag.NON_DANGER)
				map.put("img" , String.valueOf(R.drawable.ic_map_blue_marker)); 
			else
				map.put("img" , String.valueOf(R.drawable.ic_map_red_marker)); 
			
			listItem.add(map) ;
		}
		
		// sort them based on distance ;ascending order
		Collections.sort(listItem, new Comparator<HashMap<String,String>>() {

		    @Override
		    public int compare(HashMap<String,String> map1, HashMap<String,String> map2) {
		    	double dist1 = Double.parseDouble(map1.get("distanceOriginal"));
		    	double dist2 = Double.parseDouble(map2.get("distanceOriginal"));
		    	return Double.compare(dist1, dist2);
		    }
		});
		
		return listItem;
	}
}