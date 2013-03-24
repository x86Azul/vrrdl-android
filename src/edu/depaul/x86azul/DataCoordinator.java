package edu.depaul.x86azul;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import edu.depaul.x86azul.Debris.DangerFlag;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.helper.GoogleDirJsonParams;
import edu.depaul.x86azul.helper.PolylineDecoder;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Leg;
import edu.depaul.x86azul.helper.GoogleDirJsonParams.Step;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.URIBuilder;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.map.MarkerWrapper;
import edu.depaul.x86azul.map.MarkerWrapper.Type;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.location.Location;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/* 
 * keeping track of all debris location data and their markers on map
 */
public class DataCoordinator implements MapWrapper.OnGestureEvent, 
		DbAdapter.OnCompleteDBOperation, HTTPClient.OnFinishProcessHttp{
	
	private static final String GET_ADDRESS = "getAddress";
	private static final String PUT_DEBRIS = "putDebris";
	private static final String POLL_DEBRIS = "pollDebris";
	private static final String DELETE_DEBRIS = "deleteDebris";
	private static final String GET_DIRECTION = "getDirection";
	
	private static final int SWITCH_DEBRIS_DURATION = 600;
	private static final int SWITCH_INFO_DURATION = 400;
	
	
	private enum TextInfoAnimation {NEW, PREV, NEXT};
	

	/*
	 * our map object regardless the provider (e.g. Google, Nokia)
	 */
	private MapWrapper mMap;
	
	/*
	 * handle for debris database
	 */
	private DbAdapter mDbAdapter;
	
	/*
	 * control the compass activity
	 */
	private CompassController mCompass;
	
	/*
	 * handle for context
	 */
	private MainActivity mContext;
	
	/*
	 * variable to keep the debris list
	 */
	private ArrayList<Debris> mDebrises;
	
	/*
	 * info about the last location
	 */
	private Location mLastGoodLocation;
	
	/*
	 * whether there is more than one debris near to us
	 */
	private volatile boolean mInDanger;
	
	/* 
	 * interface class to HTTPClient
	 */
	private WebProxy mWebProxy;
	
	/*
	 * we need this to backup some data when TestJourney 
	 * want to use this object
	 */
	private List<Object> mBackUp;
	
	/*
	 * view use to show marker info to user
	 */
	private PopupMarkerInfo mPopupMarkerInfo;
	
	/*
	 * view use to navigation to marker to user
	 */
	private PopupNavigationInfo mPopupNavigationInfo;
	
	/*
	 * to send to notification bar 
	 */
	private NotificationManager mNotificationManager;
	
	/*
	 * 
	 */
	private AlertCenter mAlertCenter;
	

	public DataCoordinator(MainActivity context, MapWrapper map){

		mContext = context;
		
		mMap = map;
		mMap.subscribeGestureAction(this);
		
		mPopupMarkerInfo = new PopupMarkerInfo(mContext, mMap);
		mPopupNavigationInfo = new PopupNavigationInfo(mContext, mMap);
		
		mDebrises = new ArrayList<Debris>();
		
		mCompass = new CompassController(mContext);
		
		mWebProxy = new WebProxy(this);
		
		mAlertCenter = new AlertCenter(mContext);
		
		mBackUp = new ArrayList<Object>();
		
		mNotificationManager =  (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// this will initialize and populate data with database value
		mDbAdapter = new DbAdapter(mContext);
		mDbAdapter.subscribe(this);
		mDbAdapter.initialize();

		mInDanger = false;

	}
		
	public void hijackState(boolean hijack) {
		if(hijack){
			if(mBackUp.size() == 0){
				mBackUp.add(GP.tapMeansInsert);
				mBackUp.add(mInDanger);
				// come with a fresh start
				// this will set mInDanger = false
				setOverallDangerStatus(false, false);
			}
		}
		else {
			if(mBackUp.size()!=0){
				GP.tapMeansInsert = (Boolean)mBackUp.get(0);
				setOverallDangerStatus((Boolean)mBackUp.get(1), false);
				mBackUp.clear();
			}
		}
		
	}
	
	public void onStart(){
		mCompass.onStart();
	}
	
	public void onResume(){
		
	}
	
	public void onPause(){
		
	}
	
	public void onStop(){
		
		mCompass.onStop();
		mPopupMarkerInfo.dismiss();
	}
	
	public void onDestroy(){
		
		DH.showDebugMethodInfo(this);
		mNotificationManager.cancelAll();
		mPopupNavigationInfo.dismiss();
		mWebProxy.pollStop();
		mDbAdapter.close();
		
	}
	
	public synchronized int getDataSize(){
		if (mDebrises != null)
			return mDebrises.size();
		else
			return 0;
	}
	
	/*
	 * insert debris to map, database, web service, and local cache
	 * return: debris ID return from local database
	 */
	private synchronized long insert(Debris debris, boolean checkCompare,
										boolean animate, boolean update){
		
		// compare if we have similar debris in our databse
		if(checkCompare){
			if(mDebrises.contains(debris))
				return -1;
		}
		
		mDebrises.add(debris);
		
		// force map animation to false if not visible
		if(!GP.isVisibleState()) {
			animate = false;
		}
		
		// check if this debris is dangerous
		boolean danger = false; 
		// the fact that this one debris is dangerous is enough
		// to say that we're overall in danger
		if(mLastGoodLocation != null)
			if(isInDangerZone(mLastGoodLocation, debris)){
				danger = true;
				setOverallDangerStatus(danger);
			}
			
		// make sure to insert to database first
		// this will assign debris id as well
		if (!debris.mInLocalDb)
			insertToLocalDb(debris);
		
		if (!debris.mInMap)
			insertToMap(debris, danger, animate);
		

		if(GP.testMode)
			debris.mInWebService = true;
		
		if (!debris.mInWebService)
			insertToWebService(debris);

		
		if(update)
			updateUI();
		
		//DH.showDebugInfo("insert# :" + debris.mDebrisId);
		
		return debris.mDebrisId;
	}
	
	private void insert(ArrayList<Debris> pdebrises, boolean checkCompare,
													boolean animate){

		// update the status only at the last insert
		for (int i = 0; i < pdebrises.size(); i++) {
			if(i == pdebrises.size() - 1 )
				insert(pdebrises.get(i), checkCompare, animate, true);
			else
				insert(pdebrises.get(i), checkCompare, animate, false);
		}
	}
	
	public long insert(Debris debris){
		// will compare against existing
		// will animate by default
		// will directly update by default
		return insert(debris, true, true, true);
	}
	
	public synchronized void remove(Debris debris, boolean animate, boolean update){
		if(!mDebrises.contains(debris))
			return;
		
		// force and map animation to be
		// false if not visible
		if(!GP.isVisibleState()){
			animate = false;
		}
		
		if(GP.testMode)
			debris.mInWebService = false;
		
		if (debris.mInWebService)
			removeFromWebService(debris);

		if (debris.mInMap)
			removeFromMap(debris, animate);
		
		if (debris.mInLocalDb)
			removeFromLocalDb(debris);
		
		mDebrises.remove(debris);
		
		// let see if the danger status change
		boolean bDanger = false;
		for(int i=0; i<mDebrises.size(); i++){
			DangerFlag flag = mDebrises.get(i).mDangerFlag;
			if(flag != DangerFlag.NON_DANGER){
				bDanger = true;
				break;
			}
		}
		
		setOverallDangerStatus(bDanger);

		if(update)
			updateUI();
		
		//DH.showDebugInfo("remove# :" + debris.mDebrisId);
	}
	
	public synchronized void remove(long debrisId){
		Debris debris = getDebris(debrisId);
		if(debris == null)
			return;
		
		remove(debris, true, true);
	}
	
	public synchronized void remove(ArrayList<Long> debrisIds){
		for(int i=0; i< debrisIds.size();i++){
			remove(debrisIds.get(i));
		}
	}
	
	private void insertToLocalDb(Debris debris) {
		if(mDbAdapter == null || debris == null)
			return;
		
		// mInLocalDb will be modified inside this function
		mDbAdapter.insertDebris(debris);
		
		
		// if not in test mode, get address
		if(!GP.testMode){
			// by right this always null first time
			if(debris.mAddress == null){
	
				// we should've get debris id at this point (set by dbAdapter)
				String token = toWebClientToken(GET_ADDRESS, debris.mDebrisId);
				String uri = URIBuilder.toGoogleGeoURI(debris.getLatLng());
				// launch thread
	
				mWebProxy.get(token, uri);
			}
		}
	}
	
	private void removeFromLocalDb(Debris debris){
		if(debris == null)
			return;
		
		// mInLocalDb will be modified inside this function
		mDbAdapter.removeDebris(debris);
		
	}
	
	@SuppressWarnings("unchecked")
	private void insertToWebService(Debris debris) {
		
		if(debris == null)
			return;
		
		// we need to add the device UID to the param
		String androidUID = Secure.getString(mContext.getContentResolver(),
                								Secure.ANDROID_ID);
				
		JSONObject obj = debris.toJSONObject();
		try{
			obj.put("uid", androidUID);
		}
		catch(Exception e){}
		
		String param = obj.toString();
		
		// should be set later
		// debris.mInWebService;
		String token = toWebClientToken(PUT_DEBRIS, debris.mDebrisId);
						
		String uri = URIBuilder.toTestPutURI(debris);
		mWebProxy.put(token, uri, param);

	}
	
	private void removeFromWebService(Debris debris) {
		if(debris == null)
			return;
		
		String token = toWebClientToken(DELETE_DEBRIS, debris.mDebrisId);
		
		String uri = URIBuilder.toTestDeleteURI(debris);
		mWebProxy.delete(token, uri);
		
	}
	
	private void insertToMap(Debris debris, boolean danger, boolean animate) {
		
		// by default the type in non danger unless state otherwise
    	debris.mDangerFlag = danger?DangerFlag.DANGER:DangerFlag.NON_DANGER;

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
	
	private void removeFromMap(Debris debris, boolean animate){
		if(debris == null)
			return;
		
		debris.mMarker.removeFromMap(animate);
		
		debris.mMarker = null;
		
		debris.mInMap = false;
	}
	

	public synchronized void onNewLocation(Location newLocation) {
		
		// double distanceToLastGoodLocation = 0;
		
		//if(lastGoodLocation != null)
			//distanceToLastGoodLocation = MyLatLng.distance(newLocation, lastGoodLocation);

		// check if we're in danger too
		boolean inDanger = false; 

		for(int i=0; i<mDebrises.size(); i++){
	
			Debris debris = mDebrises.get(i);
			
			DangerFlag dangerFlag = debris.mDangerFlag;
	
			// check if we need this calculation
			// the worst possible scenario is that user directly come towards us (this particular debris)
			// everytime, so the new distance would need to be directly substracted
	
			// TODO: find a good algorithm for optimizing distance calculation
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
				if(dangerFlag == DangerFlag.NON_DANGER){
					// set as dangerFlag marker
					dangerFlag = DangerFlag.DANGER;
				}
			}
			else {
				if(dangerFlag != DangerFlag.NON_DANGER){
					// set as non dangerFlag marker
					dangerFlag = DangerFlag.NON_DANGER;
				}
			}
			
			debris.mDangerFlag = dangerFlag;
	
			// one danger debris is enough to say 
			// that we're in danger
			if(dangerFlag != DangerFlag.NON_DANGER){
				inDanger = true; 
			}
			
			
		}

		setOverallDangerStatus(inDanger);
		
		mWebProxy.setPollData(MyLatLng.inLatLng(newLocation));		
		
		mLastGoodLocation = newLocation;

		updateUI();
	}

	private void updateScreen(){
		
		// don't bother to update if our screen isn't showing
		if(!GP.isVisibleState())
			return;
				
				
		// there is at least one debris which is not non-dangerous (can be danger or lethal)
		if(mInDanger){

			// see which debris is considered as the lethal one
			int mostDangerIdx = Integer.MAX_VALUE;
			int lastLethalIdx = Integer.MAX_VALUE;
			double highestLethalIdxValue = 0;

			// loop once first to whether we need to update lethal debris
			for(int i=0; i<mDebrises.size(); i++){
				Debris debris = mDebrises.get(i);
				if (debris.mDangerFlag != DangerFlag.NON_DANGER){

					// consider the bearing for lethal idx calculation
					debris.mBearingToUser = MyLatLng.bearing(mLastGoodLocation, debris);

					double lethalIdxValue = getLethalIdxValue(debris, mLastGoodLocation);

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
					Debris debris = mDebrises.get(lastLethalIdx);
					debris.mDangerFlag = DangerFlag.DANGER;
				}

				// there's one debris which can be considered lethal
				if(mostDangerIdx != Integer.MAX_VALUE){
					// upgrade this one to lethal
					Debris debris = mDebrises.get(mostDangerIdx);
					debris.mDangerFlag = DangerFlag.LETHAL;
				}
			}
		}

		// lastly.. update the marker icons, except for TARGET_XXX since user
		// is currently focus on those
		for(int i=0; i<mDebrises.size(); i++) {
			Debris debris = mDebrises.get(i);
			MarkerWrapper markerW = debris.mMarker;
			if(	markerW.getDangerFlag() != debris.mDangerFlag   &&
					markerW.getDangerFlag() != DangerFlag.TARGET_INFO &&
					markerW.getDangerFlag() != DangerFlag.TARGET_DESTINATION)

				markerW.dangerFlag(debris.mDangerFlag);
		}

		mPopupMarkerInfo.update();

		mPopupNavigationInfo.update();

		// update compass
		compassCalculation();
	}
	
	private void updateNotificationBar(){
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
		
		if(mInDanger) {
			mBuilder.setContentTitle("Potential debris encounter");
			mBuilder.setSmallIcon(R.drawable.ic_map_notification_red_marker);
		   		        
		}
		else {
			mBuilder.setContentTitle("No debris in proximity");
			mBuilder.setSmallIcon(R.drawable.ic_map_notification_white_marker);		    
		}
		
		// get closest debris location
		double nearest = Double.MAX_VALUE;
		int nearestIdx = Integer.MAX_VALUE;
		for(int i=0; i<mDebrises.size(); i++){
			Debris debris = mDebrises.get(i); 
			if(debris.mDistanceToUser < nearest){
				nearest = debris.mDistanceToUser;
				nearestIdx = i;
			}
		}
		
		Intent resultIntent = new Intent(mContext, MainActivity.class);
		resultIntent.setAction(Intent.ACTION_MAIN);
		resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		resultIntent.putExtra("inDanger", mInDanger);
		
		if(nearestIdx != Integer.MAX_VALUE){
			resultIntent.putExtra("debrisId", mDebrises.get(nearestIdx).mDebrisId);
			mBuilder.setContentText("Nearest debris: " + DH.toSimpleDistance(nearest) + " mi");
		}
		
		PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, 0, 
				resultIntent, Notification.FLAG_ONGOING_EVENT | PendingIntent.FLAG_UPDATE_CURRENT); 
		mBuilder.setContentIntent(resultPendingIntent);
		
		// finally, build notification
		Notification notification = mBuilder.build();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		mNotificationManager.notify(0, notification);
	}
	
	/*
	 * this funtions will basically do
	 * 1. decide which debris that is most dangerous
	 * 2. update map icons
	 * 3. update compass
	 * 4. update the popup view if available
	 * 
	 * Things need to be calculated before calling this function
	 * 1. overall danger status: mInDanger
	 * 2. debris info
	 */
	private synchronized void updateUI(){
		
		updateNotificationBar();
		
		updateScreen();
	}

	@SuppressLint("NewApi")
	/*
	 * assumption, all debris bearing has been calculated
	 */
	private void compassCalculation(){
		// compass will point to a debris with the following priority
		// 1. the lethal one (if exist)
		// 2. the closest

		if(getDataSize()==0 || mLastGoodLocation == null){
			mCompass.setDebris(null, null, false);
			return;
		}
		
		int pointDebrisIdx = Integer.MAX_VALUE;
		double pointDebrisValue = Double.MAX_VALUE;
		
		for(int i=0; i< mDebrises.size(); i++){

			Debris debris = mDebrises.get(i);
			
			// if there's a lethal debris than pick that one
			if(debris.mDangerFlag == DangerFlag.LETHAL){
				pointDebrisIdx = i;
				break;
			}		
						
			if(debris.mDistanceToUser > pointDebrisValue)
				continue;
			
			if(debris.mDistanceToUser < pointDebrisValue){
				pointDebrisValue = debris.mDistanceToUser;
				pointDebrisIdx = i;
				continue;
			}
			
			// rare case, but possible, use bearing to decide
			if(debris.mDistanceToUser == pointDebrisValue){
				double thisDiff = CompassController.toSmallestDiffAngle(
						mLastGoodLocation.getBearing(), debris.mBearingToUser);
				double compDiff = CompassController.toSmallestDiffAngle(
						mLastGoodLocation.getBearing(), debris.mBearingToUser);
				if(thisDiff < compDiff){
					pointDebrisValue = debris.mDistanceToUser;
					pointDebrisIdx = i;
				}
			}
		}	
		
		if(pointDebrisIdx!=Integer.MAX_VALUE){			
			mCompass.setDebris(mLastGoodLocation, mDebrises.get(pointDebrisIdx), mInDanger);
		}
		
	}

	/* 
	 * set the danger flag and play tone if there's change
	 * for sync purpose, the mInDanger flag should only
	 * be updated from this function
	 */
	private void setOverallDangerStatus(boolean inDanger, boolean alert){
		// do we have different situation here?
		if(mInDanger != inDanger){
			
			// stop whatever playing (if any)
			mAlertCenter.stop();
			
			if(alert){
				mAlertCenter.play(inDanger? R.raw.warning_tone:R.raw.relief_tone);
			}
			mInDanger = inDanger;
		}
	}
	
	private void setOverallDangerStatus(boolean inDanger){
		setOverallDangerStatus(inDanger, true);
	}
	
	public synchronized boolean getOverallDangerStatus(){
		return mInDanger;
	}
	
	private boolean isInDangerZone(Location location, Debris debris){
		// get distance
		debris.mDistanceToUser = MyLatLng.distance(location, debris.getLatLng());
		
		// is this dangerous?
		return (debris.mDistanceToUser < GP.dangerRadiusInMeter);
		
	}
	
	private double getLethalIdxValue(Debris debris, Location loc){
		double userDegree; // relative bearing to user heading direction
		double[][] mapDegreeValue = {
				{0, 1},
				{45, 0.8},
				{60, 0.6},
				{90, 0.3},
				{180, 0}
				};
		
		userDegree = debris.mBearingToUser - loc.getBearing();
		// get in absolute value (0 - 180 degrees)
		userDegree = (userDegree + 360)%360; 
		userDegree = userDegree > 180?(360-userDegree):userDegree;
		
		// the closest to point of view, the bigger the value
		double bearingVal = 0;
		
		if(userDegree <= mapDegreeValue[0][0]){
			bearingVal = mapDegreeValue[0][1];
		}
		else if (userDegree >= mapDegreeValue[mapDegreeValue.length-1][0]){
			bearingVal = mapDegreeValue[mapDegreeValue.length-1][1];
		}
		else {
			for(int i=1; i < mapDegreeValue.length ; i++){

				if(userDegree < mapDegreeValue[i][0]){
					// use regression formula
					double x1 = mapDegreeValue[i-1][1];
					double y1 = mapDegreeValue[i-1][0];
					double x2 = mapDegreeValue[i][1];
					double y2 = mapDegreeValue[i][0];
					
					double y = userDegree;
					double x = x1 + (x2 - x1)*((y - y1)/(y2 - y1));
					bearingVal = x;
						
					break;
				}
			}
		}
	
		
		// the closest the distance the higher the number
		double distanceVal = (GP.dangerRadiusInMeter*1.5 - debris.mDistanceToUser);
		
		// the fastest the user, the higher the number
		double velocityVal = loc.getSpeed();
		
		return distanceVal*bearingVal*velocityVal;
	}
	
	private double getMinimumLethalIdxValue(){
		return GP.dangerRadiusInMeter*0.5*5.0;
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
	
	public synchronized void resetDebrises() {
	
		// clear all markers from map
		// make sure do it from the tail
		for(int i=mDebrises.size()-1; i>=0; i--){
			// update UI on the last debris
			if(i == 0)
				remove(mDebrises.get(i), true, true);
			else
				remove(mDebrises.get(i), true, false);
		}
		
		mDebrises.clear();
		
		mDbAdapter.clearTable();
	}
	
	public synchronized void clearDebrisWithinRadius(double radius) {
		// clear all markers from map
		// make sure do it from the tail
		for(int i=mDebrises.size()-1; i>=0; i--){
			Debris debris = mDebrises.get(i);
			if(debris.mDistanceToUser < radius)
				remove(debris, true, true);
		}	
	}
	

	public void onMapClick(MyLatLng latLng) {

		if(GP.tapMeansInsert) {
			// insert marker for testing
	    	Debris debris = new Debris(latLng);
	    	// need to put into data first
	    	insert(debris);
		}
	}

	public void onInfoWindowClick(MarkerWrapper marker) {
		
		
	}
	
	private Debris getDebrisSortOnDistance(Debris debris, boolean next){
		if(!mDebrises.contains(debris))
			return null;
		
		ArrayList<ArrayList<Object>> listDebris = new ArrayList<ArrayList<Object>>();
		
		for(int i=0; i < mDebrises.size(); i++){
			ArrayList<Object> idxAndDist = new ArrayList<Object>();
			Debris tempDebris = mDebrises.get(i);
			
			idxAndDist.add(tempDebris.mDebrisId);
			idxAndDist.add(tempDebris.mDistanceToUser);
			listDebris.add(idxAndDist);
			
		}
		
		Collections.sort(listDebris, new Comparator<ArrayList<Object>>() {
		    public int compare(ArrayList<Object> data1, ArrayList<Object> data2) {
		    	return Double.compare((Double)data1.get(1), (Double)data2.get(1));
		    }
		});
		
		// we're ready to make decide next debris candidate
		Long targetIdx = Long.MAX_VALUE;
		for(int i=0; i< listDebris.size();i++){
			if(debris.mDebrisId == (Long)listDebris.get(i).get(0)){
				
				if(next){
					if(i == listDebris.size()-1)
						targetIdx = (Long)listDebris.get(0).get(0);
					else
						targetIdx = (Long)listDebris.get(i+1).get(0);
				}
				else {
					if(i == 0)
						targetIdx = (Long)listDebris.get(listDebris.size()-1).get(0);
					else
						targetIdx = (Long)listDebris.get(i-1).get(0);
				}
				
				break;
			}
		}
		
		return getDebris(targetIdx);
	}
	
	private Debris getDebris(Long debrisId){
		for(int i=0; i < mDebrises.size(); i++){
			Debris debris = mDebrises.get(i);
			if(debris.mDebrisId == debrisId){
				return debris;
			}
		}
		return null;
	}
	
	private Debris getDebrisEqual(Debris debris){
		for(int i=0; i < mDebrises.size(); i++){
			if(mDebrises.get(i).equals(debris)){
				return mDebrises.get(i);
			}

		}
		return null;
	}
	
	private Debris getTargetDestinationDebris(){
		
		int dstDebrisIdx = Integer.MAX_VALUE;
		int infoDebrisIdx = Integer.MAX_VALUE;
		
		for(int i=0; i < mDebrises.size(); i++){
			Debris debris = mDebrises.get(i);
			if(debris.mMarker.getDangerFlag() == DangerFlag.TARGET_DESTINATION){
				dstDebrisIdx = i;
			}else if(debris.mMarker.getDangerFlag() == DangerFlag.TARGET_INFO){
				infoDebrisIdx = i;
			}
		}
		
		if(infoDebrisIdx != Integer.MAX_VALUE)
			return mDebrises.get(infoDebrisIdx);
		
		// user want reboot
		if(dstDebrisIdx != Integer.MAX_VALUE)
			return mDebrises.get(dstDebrisIdx);
		
		
		
		return null;
	}
	
	private boolean hasDebris(Debris debris){
		return mDebrises.contains(debris);
	}
	
	private Debris getDebris(MarkerWrapper marker){

		for(int i=0; i < mDebrises.size();i++){
			Debris tempDebris = mDebrises.get(i);
			if(tempDebris.mMarker.equals(marker)){
				return tempDebris;
			}
		}
		
		return null;
	}
	
	private ArrayList<Debris> getDangerousDebrises(){
		
		ArrayList<Debris> dangerousDebrises = new ArrayList<Debris>();
		for(int i=0; i < mDebrises.size();i++){
			Debris tempDebris = mDebrises.get(i);
			if(tempDebris.mDangerFlag != DangerFlag.NON_DANGER){
				dangerousDebrises.add(tempDebris);
			}
		}
		
		return dangerousDebrises;
	}


	public void onInitDbCompleted(ArrayList<Debris> data) {
		if(data!=null){
			// insert all data first then update the status
			insert(data, false, false);

			// let's relax for a while before start polling
			// schedule in 5 second
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				public void run() {
					// TODO: change the polling time. for now just use 10 seconds
					mWebProxy.pollStart(toWebClientToken(POLL_DEBRIS, 0L), 10000);
				}
			}, 5000);
		}
	}

	public void onCloseDbCompleted() {
		
	}

	public void showCompassTarget() {
		Debris debris = mCompass.getPointingDebris();

		if(debris!= null){
			ArrayList<MyLatLng> data = new ArrayList<MyLatLng>();
			data.add(MyLatLng.inLatLng(mLastGoodLocation));
			data.add(debris.getLatLng());
			
			Object camObj = mMap.buildCamPosition(null, 0, data);
			mMap.setCamPosition(camObj, true, 0, true);
		}
	}
	
	public synchronized void showDebrisTarget(long debrisId) {
		Debris debris = getDebris(debrisId);
	
		if(debris != null) {
			mMap.setCamPosition(mMap.buildCamPosition(debris.getLatLng(), MapWrapper.ZOOM_CLOSE_UP),
								true, 0, true);
		}

	}
	
	public synchronized void parcellToIntent(Intent intent){
		intent.putExtra("DataSize", getDataSize());
		for(int i=0; i < mDebrises.size(); i++){
			Debris debris = mDebrises.get(i);
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
			
			map.put("distance" , DH.toSimpleDistance(meterValue)); 

			if(DangerFlag.valueOf(intent.getStringExtra("dangerFlag"+i)) == DangerFlag.NON_DANGER)
				map.put("img" , String.valueOf(R.drawable.ic_map_blue_marker)); 
			else
				map.put("img" , String.valueOf(R.drawable.ic_map_red_marker)); 
			
			listItem.add(map) ;
		}
		
		// sort them based on distance ;ascending order
		Collections.sort(listItem, new Comparator<HashMap<String,String>>() {

		    public int compare(HashMap<String,String> map1, HashMap<String,String> map2) {
		    	double dist1 = Double.parseDouble(map1.get("distanceOriginal"));
		    	double dist2 = Double.parseDouble(map2.get("distanceOriginal"));
		    	return Double.compare(dist1, dist2);
		    }
		});
		
		return listItem;
	}
	
	@SuppressWarnings("unchecked")
	private String toWebClientToken(String operation, Long debrisId){
		JSONObject obj=new JSONObject();
		try{
			obj.put("operation", operation);
			obj.put("debrisId", debrisId);
		} catch(Exception e){}
		
		return obj.toString();
	}
	
	private String getWebClientParamOperation(String token){
		JSONObject obj;
		try {
			obj = new JSONObject(token);
		} catch (JSONException e) {
			obj = new JSONObject();
			e.printStackTrace();
		}
		return obj.optString("operation");
	}
	
	private Long getWebClientParamId(String token){
		JSONObject obj;
		try {
			obj = new JSONObject(token);
		} catch (JSONException e) {
			obj = new JSONObject();
			e.printStackTrace();
		}
		return obj.optLong("debrisId");
	}
	
	public CompassController getCompasssController(){
		return mCompass;
	}

	public synchronized void onFinishProcessHttp(String token, 
									String uri,
									String requestBody,
									int statusCode,
									String geohash,
									String responseBody) {
		
		// here we handle several feedback notification from our web request
		// we need to differentiate them
		String op = getWebClientParamOperation(token);
		
		if(op.equals(GET_ADDRESS)) {
			
			if(!HTTPClient.success(statusCode))
				return;
			
			Long debrisId = getWebClientParamId(token);
			
			Debris debris = getDebris(debrisId);
			
			if(debris == null)
				return;
		
			// found! update the address
			try{
				GoogleGeoJsonParams params = new GoogleGeoJsonParams(responseBody);
				
				if(params.isValid()){
					String address = params.getDetailAddress();
					
					// update this debris address
					debris.mAddress = address;						
					mDbAdapter.updateAddress(debris, address);
				}
			}
			catch (Exception e){
				DH.showDebugError(e.getClass().getName() + "| GET_ADDRESS exception, " +
						"responseBody: " + responseBody);
			}
		
		}
		else if(op.equals(PUT_DEBRIS)){
			
			/*
			showPopUpWindow("PUT Info", 
							"SENT: " + "\n" + requestBody + "\n\n" +
							"TO: " + uri + "\n\n" +
							"RESULT: "+ statusCode + "\n"+ responseBody, 
							13f);
			*/
			if(!HTTPClient.success(statusCode)){
				DH.showDebugError("PUT_DEBRIS statusCode " + statusCode + 
		                  " responseBody" + responseBody); 
				return;
			}
			
			// TODO: check the result and change mInWebService accordingly
			// DialogHelper.showDebugInfo("put:token=" + token + ";result=" + responseBody);
				
			Long debrisId = getWebClientParamId(token);
			Debris debris = getDebris(debrisId);
			
			if(debris == null){
				DH.showDebugWarning("PUT_DEBRIS, debris#" + debrisId + "not found! already deleted??");
				return;
			}

			debris.mInWebService = true;
			mDbAdapter.updateInWebDatabaseFlag(debris, debris.mInWebService);
			
			debris.mGeohash = geohash;
			mDbAdapter.updateGeohash(debris, debris.mGeohash);	
		
		}
		else if(op.equals(DELETE_DEBRIS)){
			if(!HTTPClient.success(statusCode)){
				DH.showDebugError("DELETE_DEBRIS uri:" + uri + ", statusCode:" + statusCode ); 
			}
			else{
				DH.showDebugInfo("DELETE_DEBRIS uri:" + uri + ", statusCode:" + statusCode ); 
			}
			 //DH.showDebugInfo("DELETE_DEBRIS uri:" + uri + ", statusCode:" + statusCode + 
			//		 ", responseBody:" + responseBody); 
		}
		else if(op.equals(POLL_DEBRIS)){
			
			if(!HTTPClient.success(statusCode)){
				// this might be because no debris in the database
				//DH.showDebugError("POLL_DEBRIS statusCode:" + statusCode + 
				//		                  ", responseBody:" + responseBody); 
				return;
			}
				
			ArrayList<Debris> tempDebrises = Debris.toDebrises(responseBody);
			
			int numOfNewDebris = 0;
			// now check if we have already got this debrises
			for(int i=0; i<tempDebrises.size(); i++){
				
				Debris newDebris = tempDebrises.get(i);
				// set the inwebservice flag to true
				// if not it will try to send this data back to webservice
				newDebris.mInWebService = true;
				
				boolean bFound = false;
				for(int j=0; j<mDebrises.size(); j++){												
					if(mDebrises.get(j).equals(newDebris)){
						bFound = true;
						break;
					}
				}
				
				// add if not found
				if(!bFound) {
					DH.showDebugError("NEW:" + newDebris);
					for(int j=0; j<mDebrises.size(); j++){												
						DH.showDebugError("EXIST:" + mDebrises.get(j));
					}
					
					numOfNewDebris++;
					insert(newDebris);
				}
			}
			
			//DH.showDebugInfo("POLL_DEBRIS statusCode:" + statusCode + 
	        //          ", responseBody:" + responseBody); 
			
			if(numOfNewDebris!=0)
				showPopUpWindow("GET Info", 
								"Updated with "+ numOfNewDebris + " new debris(es)",
								16f);

		}
		else if(op.equals(GET_DIRECTION)){ 
			if(!HTTPClient.success(statusCode)){
				DH.showDebugError("GET_DIRECTION statusCode:" + statusCode + 
						                  ", responseBody:" + responseBody); 
				return;
			}
			
			
			mPopupNavigationInfo.show(responseBody, getTargetDestinationDebris());
		} 
	}
	

	private void showPopUpWindow(String title,
								 String message,
								 float textSize){

		
		//  don't show if we're in test mode
		if(GP.testMode)
			return;
		// show response from the server
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);	
	    
		alertDialogBuilder
		.setTitle(title)
		.setMessage(message)
		.setCancelable(false)
		.setPositiveButton("Close",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						return;
					}
		});

		
		try{
			AlertDialog alert = alertDialogBuilder.create();
			alert.show();
			
			// change font size
			TextView textView = (TextView) alert.findViewById(android.R.id.message);
			if(textView != null)
				textView.setTextSize(textSize);
		}
		catch(Exception e){
			DH.showDebugError("showPopUpWindow Error, exception:" + e.getClass().getName());
		}
		
	}
	
	public synchronized void onNotificationBarPressed(Long debrisId, boolean danger) {
		if(!danger)
			return;
		
		Debris debris = getDebris(debrisId);
		if(debris == null)
			return;
		
		// show bounding location
		ArrayList<MyLatLng> data = new ArrayList<MyLatLng>();
		data.add(MyLatLng.inLatLng(mLastGoodLocation));
		data.add(debris.getLatLng());
		
		Object camObj = mMap.buildCamPosition(null, 0, data);
		mMap.setCamPosition(camObj, true, 0, true);
		
	}

	public View getInfoContents(MarkerWrapper marker) {

        return null;
	}

	public boolean onMarkerClick(MarkerWrapper marker) {
		
		Debris debris = getDebris(marker);
		if(debris == null){
			DH.showDebugWarning("onMarkerClick: No debris marker info!");
			return false;
		}
		
		// we're going to center the marker here and popup the 
		// lone debris panel
	
		mPopupMarkerInfo.show(debris);
	
        //mMap.getNewViewPosition(position, zoom, bound1, bound2)

		return true;
	}
	
	public void directionToDebris(Debris debris){
		
		// don't bother to send the request if null
		if(debris == null)
			return;
		
		// we got everything we need, trigger the webservice request now
		String token = toWebClientToken(GET_DIRECTION, 0L);
		
		String uri = URIBuilder.toGoogleDirURI(MyLatLng.inLatLng(mLastGoodLocation), 
										debris.getLatLng());

		mWebProxy.get(token, uri);
	}
	
	public void setTargetFlag(Debris debris, DangerFlag dangerFlag){
		if(debris!=null && debris.mMarker!=null){
			// TARGET_DESTINATION > TARGET_INFO
			// dest can overwrite info, but not the other way around
			DangerFlag debrisDF = debris.mMarker.getDangerFlag();
			if(dangerFlag == DangerFlag.TARGET_DESTINATION){
				debris.mMarker.dangerFlag(dangerFlag);
			}
			else if(debrisDF != DangerFlag.TARGET_DESTINATION){
				debris.mMarker.dangerFlag(dangerFlag);
			}
				
		}
	}
	
	public void resetTargetFlag(Debris debris, DangerFlag dangerFlag){
		if(debris!=null && debris.mMarker!=null){
			// only reset if not used as target by other
			DangerFlag debrisMDF = debris.mMarker.getDangerFlag();
			if(dangerFlag == DangerFlag.TARGET_DESTINATION){
				debris.mMarker.dangerFlag(debris.mDangerFlag);
			}
			else if(debrisMDF != DangerFlag.TARGET_DESTINATION) {
				debris.mMarker.dangerFlag(debris.mDangerFlag);
			}
		}
	}
	
	private class PopupNavigationInfo implements OnItemClickListener, OnClickListener, OnTouchListener{
		private MainActivity _mContext;
		private MapWrapper _mMap;
		private LinearLayout _mParent;
		private View _mButtonView;
		private View _mNavigationView;
		private MarkerWrapper _mPathLine;
		private ArrayList<MarkerWrapper> _mTurnPoints;
		private TextView _mDuration;
		private TextView _mDistance;
		private ArrayList <HashMap<String,String>> _mlistItem;
		private ArrayList<Step> _mSteps;
		private SimpleAdapter _mAdapter;
		private volatile boolean _mIsShowing;
		private Debris _mTargetDebris;
		private Object _mCameraInfo;
		
		@SuppressLint("NewApi")
		public PopupNavigationInfo(MainActivity context, MapWrapper mMapWrapper){
			
			_mContext = context;
			
			_mMap = mMapWrapper;
			
			_mParent = (LinearLayout)mContext.findViewById(R.id.canvas);

			// set the view to be invisible in the beginning
			_mNavigationView = mContext.getLayoutInflater().inflate(R.layout.popup_navigation, null);
			//_mNavigationView.setLayoutParams(new LayoutParams(_mParent.getWidth(), _mParent.getHeight()/2));
			
			_mIsShowing = false;
			
			_mButtonView = mContext.findViewById(R.id.button_placeholder);
			
			_mDuration = (TextView) _mNavigationView.findViewById(R.id.duration);
			_mDistance = (TextView) _mNavigationView.findViewById(R.id.distance);
			
			_mTurnPoints = new ArrayList<MarkerWrapper>();
			
			_mlistItem = new ArrayList <HashMap<String,String>>();
			
			_mAdapter = new SimpleAdapter (_mContext.getBaseContext(), _mlistItem, R.layout.format_listnavsteps,
					new String[] {"index", "steps", "distance"}, new int[] {R.id.index, R.id.steps, R.id.loneDistance});

			ListView navigationSteps = (ListView) _mNavigationView.findViewById(R.id.navSteps);
			navigationSteps.setAdapter(_mAdapter);
			
			(_mNavigationView.findViewById(R.id.close)).setOnClickListener(this);
			(_mNavigationView.findViewById(R.id.journey_overall)).setOnClickListener(this);
			(_mNavigationView.findViewById(R.id.journey_overall)).setOnTouchListener(this);
			navigationSteps.setOnItemClickListener(this);
		}
		
		public void onClick(View v) {
		
			switch(v.getId()){
			case R.id.close: {
				dismiss(false);
				break;
				}
			case R.id.journey_overall: {
				_mMap.setCamPosition(_mCameraInfo, true, 0, true);
				break;
				}
			default:{
				break;
				}
			}
			
		}
		

		public synchronized boolean onTouch(View v, MotionEvent event) {
		
			switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                	// put blue filter to mark press
                	// and red color on the close button
                	if(v.getId() == R.id.journey_overall)
                		v.setBackgroundColor(0xAA3A99EC);

                    v.invalidate();	                 
                    
                    break;
                }
                default: {
                	if(v.getId() == R.id.journey_overall)
                		v.setBackgroundColor(0X00000000); 
                	
                	v.invalidate();
                	
                	break;
                }	                
            }
            return false;
		}

		public synchronized void onItemClick(AdapterView<?> a, View v, int position, long id) {

			MyLatLng targetLocation = null;
			if(position >= _mTurnPoints.size()){
				
				// hide all the tur points info and popup the debris panel
				for(int i=0; i<_mTurnPoints.size();i++)
					_mTurnPoints.get(i).hideInfo();
				
				targetLocation = _mTargetDebris.getLatLng();				
				mPopupMarkerInfo.show(_mTargetDebris);
				
			}
			else {
				
				// dismiss debris panel if exist
				mPopupMarkerInfo.dismiss();
				
				targetLocation = _mTurnPoints.get(position).getCoordinate();
				_mTurnPoints.get(position).showInfo();
				_mMap.setCamPosition(_mMap.buildCamPosition(targetLocation, MapWrapper.ZOOM_CLOSE_UP),
						true, 0, true);
			}
		}
		
		private boolean prepareData(String data){
			
			boolean bRet = false;
			GoogleDirJsonParams googleDirData = null;
			try{
				googleDirData = new GoogleDirJsonParams(data);
			}catch (Exception e){
				DH.showDebugError(e.getClass().getName() + " prepareData Fail");
			}
			
			if(googleDirData == null || !googleDirData.isValid())
				return bRet;
			
			Leg leg = googleDirData.routes.get(0).legs.get(0);
			_mSteps = leg.steps;					
			
			// DISTANCE
			String distance = "  " + DH.toSimpleDistance((double)leg.distance.value) + " mi";
			_mDistance.setText(distance);
			
			// DURATION
			String duration = "   " + leg.duration.text;					
			_mDuration.setText(duration);
			
			// STEPS
			_mlistItem.clear();				
			
			ArrayList<MyLatLng> tempPolyline = new ArrayList<MyLatLng>();
			for(int i=0; i<_mSteps.size(); i++){
				
				Step step = _mSteps.get(i);
				HashMap<String, String> map = new HashMap<String,String>();
				map.put("index", String.valueOf(i+1)); 
				map.put("steps", String.valueOf(Html.fromHtml(step.html_instructions)));
				map.put("distance", String.format(" %-15s%s ", step.distance.text, step.duration.text));
				_mlistItem.add(map);
				
				// grab marker too
				MarkerWrapper markerPoint = new MarkerWrapper(Type.PIN)
												.title(String.valueOf(Html.fromHtml(step.html_instructions)))
												.anchor(0.5f, 0.5f)
												.coordinate(step.start_location)
												.icon(R.drawable.graydot);	
				
				
				_mTurnPoints.add(markerPoint);
				
				tempPolyline.addAll(PolylineDecoder.decodePoly(step.polyline.points));
				
			}
			
			// lastly add the last address
			HashMap<String, String> map = new HashMap<String,String>();
			map.put("index", String.valueOf(_mSteps.size()+1)); 
			map.put("steps", "Final Location:\n\n" + _mTargetDebris.mAddress);
			map.put("distance", null);
			_mlistItem.add(map);
			
			_mAdapter.notifyDataSetChanged();
			
			_mPathLine = new MarkerWrapper(Type.POLYLINE)
								.coordinates(tempPolyline)
								.width(8.0f)
						        .color(0xAA1788FF);			  			
			
			ArrayList<MyLatLng> tempLatLng = googleDirData.routes.get(0).bounds.getBounds();
			tempLatLng.add(_mTargetDebris.getLatLng());
			_mCameraInfo = _mMap.buildCamPosition(null, 0, tempLatLng);
			
			bRet = true;

			 return bRet;
		}
		
		public void update() {
			// check if our debris stil there
			if(_mTargetDebris==null)
				return;
			
			if(!hasDebris(_mTargetDebris))
				dismiss(false);

		}
		
		public synchronized void show(String data, Debris debris){
			
			if(debris == null)
				return;
			
			clearData(true);
			
			if(_mTargetDebris != null)
				if(_mTargetDebris != debris){
					resetTargetFlag(_mTargetDebris, DangerFlag.TARGET_DESTINATION);
				}
			
			_mTargetDebris = debris;
			setTargetFlag(_mTargetDebris, DangerFlag.TARGET_DESTINATION);
			
			// parse data 
			if(!prepareData(data))
				return;
			
			// switch the view
			if(!_mIsShowing) {
				
				_mButtonView.setVisibility(View.GONE);
				_mParent.addView(_mNavigationView);		
				_mIsShowing = true;
				
				_mNavigationView.setLayoutParams(new LinearLayout.LayoutParams(_mParent.getWidth(), (_mParent.getHeight()/2 - 80)));
				Animation animation = AnimationUtils.loadAnimation(_mContext.getBaseContext(), R.anim.slide_up_in);
				animation.setStartOffset(0);
				
				animation.setAnimationListener(new Animation.AnimationListener(){
				    public void onAnimationStart(Animation arg0) {}           
				    public void onAnimationRepeat(Animation arg0) {}           
				    public void onAnimationEnd(Animation arg0) {
				    	adjustMapCamera();
				    }});
				
				_mNavigationView.startAnimation(animation);
				
			}
			else {
			
				adjustMapCamera();
			}
		}
		
		private void adjustMapCamera(){
			// set map
			if(_mMap==null || _mCameraInfo==null)
				return;
			
			_mMap.setCamPosition(_mCameraInfo, true, 0, true);
	
			for(int i=0; i<_mTurnPoints.size();i++){
				_mMap.insertMarker(_mTurnPoints.get(i), true);
			}
			_mMap.insertMarker(_mPathLine, true);
		}

		
		// we set the click feedback here
				
		
		public void dismiss(){
			dismiss(true);			
		}
		
		public void dismiss(boolean systemDismiss){
			
			clearData(false);
			
			if(_mIsShowing) {
				
				if(systemDismiss){
					_mParent.removeView(_mNavigationView);
					_mButtonView.setVisibility(View.VISIBLE);
					
				}
				else{
					Animation animation = AnimationUtils.loadAnimation(_mContext.getBaseContext(), R.anim.slide_down_out);
					animation.setStartOffset(0);
					animation.setAnimationListener(new Animation.AnimationListener(){
					    public void onAnimationStart(Animation arg0) {}           
					    public void onAnimationRepeat(Animation arg0) {}           
					    public void onAnimationEnd(Animation arg0) {
					    	_mParent.removeView(_mNavigationView);
							_mButtonView.setVisibility(View.VISIBLE);
					    }
					});
					_mNavigationView.startAnimation(animation);				
				}
				
				_mIsShowing = false;
			}
		}
		
		private void clearData(boolean reset){
			if(_mPathLine != null){
				_mPathLine.removeFromMap(reset);
				_mPathLine = null;
			}
			
			if(_mTurnPoints.size() != 0){
				for(int i=0; i<_mTurnPoints.size();i++){
					_mTurnPoints.get(i).removeFromMap(reset);
				}
				_mTurnPoints.clear();
			}
			
			if(_mTargetDebris != null){
				resetTargetFlag(_mTargetDebris, DangerFlag.TARGET_DESTINATION);
				_mTargetDebris = null;
			}
		}
	}

	
	private class PopupMarkerInfo implements OnTouchListener, OnClickListener{
		
		private MainActivity _mContext;
		private PopupWindow _mPopup;
		private View _mParent;
		private View _mView;
		private TextView _mInfo;
		private Debris _mDebris;
		private MapWrapper _mMapWrapper;
		
		private CharSequence _mDebrisInfo;
		
		private Animation slideOutRight;
		private Animation slideInRight;
		private Animation slideOutLeft;
		private Animation slideInLeft;
		private Animation slideInDown;
		

		
		public PopupMarkerInfo(MainActivity context, MapWrapper mMapWrapper){
			
			_mContext = context;
			
			_mMapWrapper = mMapWrapper;
			
			_mView = _mContext.getLayoutInflater().inflate(R.layout.popup_debris_panel, null);
			
			_mInfo = (TextView) _mView.findViewById(R.id.info);

			// subscribe to button push
			//(_mView.findViewById(R.id.direction)).setOnClickListener(this);
			setListener(R.id.direction, this);
			setListener(R.id.close, this);
			setListener(R.id.discard, this);
			setListener(R.id.previous, this);
			setListener(R.id.next, this);
			setListener(R.id.info, this);
			
	        
			_mParent = mContext.findViewById(R.id.map);
			_mPopup = new PopupWindow(_mView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			_mPopup.setOutsideTouchable(true);
			
			slideOutRight = buildHorzAnimation(false, false);
			slideInRight = buildHorzAnimation(true, false);
			slideOutLeft = buildHorzAnimation(false, true);
			slideInLeft = buildHorzAnimation(true, true);
			
			slideInDown = buildVertAnimation();
		}
		
		private void setListener(int id, Object obj){
			(_mView.findViewById(id)).setOnTouchListener((OnTouchListener)obj);
			(_mView.findViewById(id)).setOnClickListener((OnClickListener)obj);
		}
		
		private Animation buildHorzAnimation(boolean in, boolean left){
			float befX=0, aftX=0;
			
			if(in && left){
				befX = 250;
				aftX = 0;
			}else if(in && !left){
				befX = -250;
				aftX = 0;
			}else if(!in && left){
				befX = 0;
				aftX = -250;
			}else {
				befX = 0;
				aftX = 250;
			}
			
			TranslateAnimation ta = new TranslateAnimation(befX, aftX, 0, 0);
			ta.setDuration(SWITCH_INFO_DURATION/2); // because it consist of two
			ta.setRepeatCount(0);
			ta.setFillAfter(in);
			ta.setInterpolator(in?new DecelerateInterpolator():new AccelerateInterpolator());
			return ta;
		}
		
		private Animation buildVertAnimation(){
			TranslateAnimation ta = new TranslateAnimation(0, 0, -100, 0);
			ta.setDuration(SWITCH_INFO_DURATION); // in ms
			ta.setRepeatCount(0);
			ta.setFillAfter(true);
			ta.setInterpolator(new DecelerateInterpolator());
			
			return ta;
		}
		
		public void update() {
			
			if(_mDebris==null)
				return;
			
			// check if our debris stil there
			if(!hasDebris(_mDebris))
				dismiss();
			
			// just update with what we have, no animation
			show(_mDebris, null);

		}

		// this api called by marker click, so..
		public void show(Debris debris){
			show(debris, TextInfoAnimation.NEW);
		}
		
		@SuppressLint("NewApi")
		public synchronized void show(Debris debris, TextInfoAnimation tiAnim){
			if(_mPopup == null || debris == null)
				return;
			
			if(_mDebris != null)
				if(_mDebris != debris){
					resetTargetFlag(_mDebris, DangerFlag.TARGET_INFO);
				}
			
			_mDebris = debris;
			setTargetFlag(_mDebris, DangerFlag.TARGET_INFO);
			
			// Enter infos			
			// TITLE
			String title = "Debris #" + _mDebris.mDebrisId + 
					"  ("+ DH.toSimpleDistance(_mDebris.mDistanceToUser) + " mi)";
			
			SpannableString titleText = new SpannableString(title);
			titleText.setSpan(new RelativeSizeSpan(1.15f), 0, titleText.length(), 0);
			if (_mDebris.mDangerFlag != DangerFlag.NON_DANGER)
				titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);

			
			// ADDRESS
            String address = (_mDebris.mAddress==null)?"":DH.toShortAddress(_mDebris.mAddress);
			SpannableString addressText = new SpannableString(address);
			
			_mDebrisInfo = TextUtils.concat(titleText, "\n" , addressText);
			
			
			if(!_mPopup.isShowing()) {
				// show popup
				// this placement is to make sure it taking into consideration 
				// the changing in Map View size
				_mPopup.showAtLocation(_mParent, Gravity.TOP|Gravity.CENTER_HORIZONTAL, 
										0, (_mParent.getHeight()/2) - 70);
			}
			
			// center the camera
			if(tiAnim != null){
				Object camUpdate = _mMapWrapper.buildCamPosition(_mDebris.getLatLng(), 0, -50);
				_mMapWrapper.setCamPosition(camUpdate, true, SWITCH_DEBRIS_DURATION, true);
			}
			
			_mInfo.setVisibility(View.VISIBLE);
			_mInfo.setAnimation(null);
			if(tiAnim == TextInfoAnimation.PREV){
				
				slideOutRight.setAnimationListener(new Animation.AnimationListener(){
				    public void onAnimationStart(Animation arg0) {}           
				    public void onAnimationRepeat(Animation arg0) {}           
				    public void onAnimationEnd(Animation arg0) {
				    	
				    	slideInRight.setAnimationListener(new Animation.AnimationListener(){
						    public void onAnimationStart(Animation arg0) {}           
						    public void onAnimationRepeat(Animation arg0) {}           
						    public void onAnimationEnd(Animation arg0) {
						    	
						    	_mInfo.setAnimation(null);
						    	
						    }});
						_mInfo.startAnimation(slideInRight);
						_mInfo.setText(_mDebrisInfo);
				    }
				}); _mInfo.startAnimation(slideOutRight);  
			}
			else if (tiAnim == TextInfoAnimation.NEXT){
				slideOutLeft.setAnimationListener(new Animation.AnimationListener(){
				    public void onAnimationStart(Animation arg0) {}           
				    public void onAnimationRepeat(Animation arg0) {}           
				    public void onAnimationEnd(Animation arg0) {
				    	
				    	slideInLeft.setAnimationListener(new Animation.AnimationListener(){
									    public void onAnimationStart(Animation arg0) {}           
									    public void onAnimationRepeat(Animation arg0) {}           
									    public void onAnimationEnd(Animation arg0) {
									    	
									    	_mInfo.setAnimation(null);
									    	
									    }});
			
						_mInfo.startAnimation(slideInLeft);
						_mInfo.setText(_mDebrisInfo);
				    }
				}); _mInfo.startAnimation(slideOutLeft);  
			}
			else if (tiAnim == TextInfoAnimation.NEW){
				_mInfo.startAnimation(slideInDown); 
				_mInfo.setText(_mDebrisInfo);
			}
			else{
				// only when there's no animation
				if(_mInfo.getAnimation() == null)
					_mInfo.setText(_mDebrisInfo);
			}

		}
		
		public void dismiss(){
			dismiss(false);
		}

		public void dismiss(boolean transition){
		
			if(_mPopup!=null && _mPopup.isShowing())
				_mPopup.dismiss();
			
			if(_mDebris!=null){
	
				if(!transition)
					resetTargetFlag(_mDebris, DangerFlag.TARGET_INFO);
				
				_mDebris = null;
			}
			
			if(_mInfo != null)
				_mInfo.setAnimation(null);
		}
		


		@SuppressLint("NewApi")
		public boolean onTouch(View v, MotionEvent event) {
		
			switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                	// put blue filter to mark press
                	// and red color on the close button
                	if(v.getId() == R.id.close)
                		v.setBackgroundColor(0X55E56262);
                	else if (v.getId() == R.id.info)
                		v.setBackgroundColor(0xAA3A99EC);
                	else
                		v.getBackground().setColorFilter(0xAA3A99EC, Mode.OVERLAY);
                    v.invalidate();	                 
                    
                    break;
                }
                default: {
                	if(v.getId() == R.id.close)
                		v.setBackgroundColor(0X00000000);
                	else if (v.getId() == R.id.info)
                		v.setBackgroundColor(0X00000000);
                	else
                		v.getBackground().clearColorFilter();
                	
                	v.invalidate();
                	
                	
                	break;
                }	                
            }
            return false;
		}

		public void onClick(View v) {
			switch(v.getId()){
			case R.id.direction: {
				// dismiss();
				// trigger the webservice request now
				directionToDebris(_mDebris);
				dismiss(true);
				break;
			}
			case R.id.close: {
				dismiss();
				break;
			}
			case R.id.discard: {
				Debris debris = _mDebris;
				dismiss();
				remove(debris, true, true);
				break;
			}
			case R.id.info: {
				Object camUpdate = _mMapWrapper.buildCamPosition(_mDebris.getLatLng(), 0, -50);
				_mMapWrapper.setCamPosition(camUpdate, true, SWITCH_DEBRIS_DURATION, true);
				break;
			}
			case R.id.previous: {
				show(getDebrisSortOnDistance(_mDebris, false), TextInfoAnimation.PREV);
				break;
			}
			case R.id.next: {	
				show(getDebrisSortOnDistance(_mDebris, true), TextInfoAnimation.NEXT);
				break;
			}
			default:
				break;
			}            
		}
	}
}