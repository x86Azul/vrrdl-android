package edu.depaul.x86azul;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.depaul.x86azul.Debris.DangerFlag;
import edu.depaul.x86azul.helper.DialogHelper;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.URIBuilder;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.map.MarkerWrapper;
import edu.depaul.x86azul.map.MarkerWrapper.Type;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.provider.Settings.Secure;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

/* 
 * keeping track of all debris location data and their markers on map
 */
public class DataCoordinator implements MapWrapper.GestureClient, 
		DbAdapter.Client, WebWrapper.Client{

	private static final double DANGER_ZONE_IN_METERS = 2000.0;
	//private static final double SIGHT_ANGLE_IN_DEGREE = 60; 
	//private static final double MIN_VELOCITY_TRACKING = 1; 
	
	private static final long serialVersionUID = 0L;
	
	private static final String GET_ADDRESS = "getAddress";
	private static final String PUT_DEBRIS = "putDebris";
	
	private String mWebAddress; 

	private MapWrapper mMap;
	private DbAdapter mDbAdapter;
	
	private CompassController mCompass;
	
	private MainActivity mContext;
	
	// these arrays MUST be sync with each other
	private ArrayList<Debris> debrises;
	
	private Location lastGoodLocation;
	private boolean mInDanger;
	
	private boolean mTapMeansInsert;
	
	private MediaPlayer mMediaHandle;
	
	private List<Object> mBackUp;

	public DataCoordinator(MainActivity context, MapWrapper map){

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
		
		mCompass = new CompassController(mContext);
		
		mBackUp = new ArrayList<Object>();
		
		mTapMeansInsert = true;
		mInDanger = false;
		
		// set to default state
		mWebAddress = WebServiceAddressActivity.HTTPBIN;
		mTapMeansInsert = true;

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
		if (!debris.mInLocalDb)
			insertToLocalDb(debris);
    
		if (!debris.mInMap)
			insertToMap(debris, animate);
		
		if (!debris.mInWebService)
			insertToWebService(debris);
		
    	
    	// now add them for us to control
    	debrises.add(debris);
		
		
		if(update)
			updateStatus();
	}
	
	public void insert(Debris debris){
		// will animate by default
		// will directly update by default
		insert(debris, true, true);
	}
	
	private void insertToLocalDb(Debris debris) {
		if(mDbAdapter == null)
			return;
		
		mDbAdapter.insertDebris(debris);
		
		debris.mInLocalDb = true;
		
		// by right this always null first time
		if(debris.mAddress == null){

			// we should've get debris id at this point (set by dbAdapter)
			String token = toWebClientParam(GET_ADDRESS, debris.mDebrisId);
				
			String uri = URIBuilder.toGoogleGeoURI(debris.getLatLng());
			new WebWrapper(this).get(token, uri);
		}
	}
	
	private void insertToWebService(Debris debris) {	
		
		// we need to add the device UID to the param
		String androidUID = Secure.getString(mContext.getContentResolver(),
                								Secure.ANDROID_ID);
				
		JSONObject obj = Debris.toJSONObject(debris);
		obj.put("uid", androidUID);
		
		String param = obj.toString();
		
		DialogHelper.showDebugInfo("param:" + param);
		
		
		// should be set later
		// debris.mInWebService;
		String token = toWebClientParam(PUT_DEBRIS, debris.mDebrisId);
						
		String uri = URIBuilder.toTestPutURI(mWebAddress, debris);
		new WebWrapper(this).put(token, uri, param);

	}
	private void insertToMap(Debris debris, boolean animate) {
		
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
    							.snippet(debris.mTimestamp)
								.dangerFlag(debris.mDangerFlag);
    	
    	mMap.insertMarker(marker, animate);	
    	
    	debris.mMarker = marker;

    	debris.mInMap = true;
	
	}

	public void analyzeNewLocation(Location newLocation) {
		
		double distanceToLastGoodLocation = 0;
		
		if(lastGoodLocation != null)
			distanceToLastGoodLocation = MyLatLng.distance(newLocation, lastGoodLocation);
			
			
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
		
		
			// here's the rule.. the only illegal transition is
			// from non-danger -> lethal
			// this time we're going to see if a debris can be put/remove from non-danger flag
			// the danger-lethal checking will be done at the next step	
			// recalculate the new distance	
			if (isInDangerZone(newLocation, debris)){
				if(debris.mDangerFlag == DangerFlag.NON_DANGER){
					// set as dangerFlag marker
					debris.mDangerFlag = DangerFlag.DANGER;
					debris.mMarker.dangerFlag(DangerFlag.DANGER);
				}
			}
			else {
				if(debris.mDangerFlag != DangerFlag.NON_DANGER){
					// set as non dangerFlag marker
					debris.mDangerFlag = DangerFlag.NON_DANGER;
					debris.mMarker.dangerFlag(DangerFlag.NON_DANGER);
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
					Debris debris = debrises.get(lastLethalIdx);
					debris.mDangerFlag = DangerFlag.DANGER;
					debris.mMarker.dangerFlag(DangerFlag.DANGER);
				}

				// there's one debris which can be considered lethal
				if(mostDangerIdx != Integer.MAX_VALUE){
					// upgrade this one to lethal
					Debris debris = debrises.get(mostDangerIdx);
					debris.mDangerFlag = DangerFlag.LETHAL;
					debris.mMarker.dangerFlag(DangerFlag.LETHAL);
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
	
		// clear all markers from map, animate
		for(int i=0; i< getDataSize(); i++)
			debrises.get(i).mMarker.removeFromMap(true);
		
		debrises.clear();
		
		updateStatus();
		
		mDbAdapter.clearTable();
	}
	
	public void setTapMeansInsertMode(boolean enable){
		mTapMeansInsert = enable;
	}
	
	public boolean getTapMeansInsertMode(){
		return mTapMeansInsert;
	}
	
	public String getWebAddress(){
		return mWebAddress;
	}
	
	public void setWebAddress(String webAddress){
		
		mWebAddress = webAddress;
		
		DialogHelper.showDebugInfo("mWebAddress=" + mWebAddress);
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
	
	@SuppressWarnings("unchecked")
	private String toWebClientParam(String operation, Long id){
		JSONObject obj=new JSONObject();
		
		obj.put("operation", operation);
		obj.put("id", id);
		
		return obj.toString();
	}
	
	private String getWebClientParamOperation(String token){
		JSONObject obj = (JSONObject)JSONValue.parse(token);
		return (String)obj.get("operation");
	}
	
	private Long getWebClientParamId(String token){
		JSONObject obj = (JSONObject)JSONValue.parse(token);
		return (Long)obj.get("id");
	}

	@Override
	public void onFinishProcessHttp(String token, 
									String uri,
									String body,
									String result) {
		
		// here we handle several feedback notification from our web request
		// we need to differentiate them
		String op = getWebClientParamOperation(token);
		
		if(op.equals(GET_ADDRESS)) {
			
			Long debrisId = getWebClientParamId(token);
			
			for(int i=0; i<getDataSize(); i++){
				
				Debris debris = debrises.get(i);
				
				if(debris.mDebrisId == debrisId){
					
					GoogleGeoJsonParams params = new GoogleGeoJsonParams((JSONObject)JSONValue.parse(result));
					
					if(params.isValid()){
						String address = params.getDetailAddress();
						
						DialogHelper.showDebugInfo(address);
						
						// update this debris address
						debris.mAddress = address;
						mDbAdapter.updateAddress(debris, address);
					}
					
					
				}
			}
			
		}
		else if(op.equals(PUT_DEBRIS)){
			
			// TODO: check the result and change mInWebService accordingly
			DialogHelper.showDebugInfo("put:token=" + token + ";result=" + result);
			
			Long debrisId = getWebClientParamId(token);
			
			for(int i=0; i<getDataSize(); i++){
				Debris debris = debrises.get(i);
				
				if(debris.mDebrisId == debrisId){
					debris.mInWebService = true;
					mDbAdapter.updateInWebDatabaseFlag(debris, debris.mInWebService);
				}
			}
				
			showDebugHttpPutResponse(uri, body, result);
		}
	}
	
	private void showDebugHttpPutResponse(String uri,
											String body,
											String result){

		// show response from the server
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);	
	    
		alertDialogBuilder
		.setTitle("PUT Info")
		.setMessage("SENT:\n" + body + "\n\nTO: " + uri + "\n\nRESULT:\n"+ result)
		.setCancelable(false)
		.setPositiveButton("Close",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						return;
					}
		});

		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
		
		// change font size
		TextView textView = (TextView) alert.findViewById(android.R.id.message);
		if(textView != null)
			textView.setTextSize(13);
		
	}
}