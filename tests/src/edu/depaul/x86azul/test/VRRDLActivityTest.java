package edu.depaul.x86azul.test;

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;

import junit.framework.Assert;
import edu.depaul.x86azul.CompassController;
import edu.depaul.x86azul.DataCoordinator;
import edu.depaul.x86azul.DataCoordinator.PopupMarkerInfo;
import edu.depaul.x86azul.DataCoordinator.PopupNavigationInfo;
import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.Debris.DangerFlag;
import edu.depaul.x86azul.GP;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.PositionTracker;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.runtest.TestJourney;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/*
 * Tests the example application Spinner. Uses the instrumentation test class
 * ActivityInstrumentationTestCase2 as its base class. The tests include
 *   - test initial conditions
 *   - test the UI
 *   - state management - preserving state after the app is shut down and restarted, preserving
 *     state after the app is hidden (paused) and re-displayed (resumed)
 *
 * Demonstrates the use of JUnit setUp() and assert() methods.
 */

public class VRRDLActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private static final MyLatLng MILLENIUM_PARK = new MyLatLng(41.88303,-87.624335);
	private static final MyLatLng CHICAGO_CENTER = new MyLatLng(41.899211,-87.628326);

	private static final MyLatLng SCHAUMBURG = new MyLatLng(42.033739,-88.06469);
	private static final MyLatLng PALATINE = new MyLatLng(42.115033,-88.034821);
	
	private MainActivity mActivity;

	private MapWrapper mMap;

	private PositionTracker mPosTracker;

	private DataCoordinator mData;

	private CompassController mCompass;

	private ArrayList<Long> mDebrisIds;

	private double mDistance;

	/*
	 * Constructor for the test class. Required by Android test classes. The constructor
	 * must call the super constructor, providing the Android package name of the app under test
	 * and the Java class name of the activity in that application that handles the MAIN intent.
	 */
	public VRRDLActivityTest() {

		super(MainActivity.class);
	}

	/*
	 * Sets up the test environment before each test.
	 * @see android.test.ActivityInstrumentationTestCase2#setUp()
	 */
	@Override
	protected void setUp() throws Exception {

		/*
		 * Call the super constructor (required by JUnit)
		 */

		super.setUp();

		/*
		 * prepare to send key events to the app under test by turning off touch mode.
		 * Must be done before the first call to getActivity()
		 */

		setActivityInitialTouchMode(false);

		/*
		 * Start the app under test by starting its main activity. The test runner already knows
		 * which activity this is from the call to the super constructor, as mentioned
		 * previously. The tests can now use instrumentation to directly access the main
		 * activity through mActivity.
		 */
		mActivity = getActivity();

		mMap = mActivity.getMap();

		mPosTracker = mActivity.getPositionTracker();

		mData = mActivity.getDataCoordinator();

		mCompass = mData.getCompasssController();

		/*
		 *  An example of an initialization test. Assert that the item select listener in
		 *  the main Activity is not null (has been set to a valid callback)
		 */
		assertTrue(mMap != null);

		assertTrue(mPosTracker != null);

		assertTrue(mData != null);

		assertTrue(mCompass != null);        

		mActivity.setTestMode(true);

		// all the location data will be coming from us mwuahahaha..
		mPosTracker.hijackLocationProvider(true);

		mData.hijackState(true);

		mDebrisIds = new ArrayList<Long>();
		
		// dismiss warning dialog
		mActivity.dismissWarningDialog();

	}

	@Override
	protected void tearDown() throws Exception {
		if(mActivity!= null)
			mActivity.finish();

		// sleep for a while before going to the next test
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}
	}




	/*
	 * Test to see if the alert went off when we're in danger
	 */
	public void testDangerousDebris() {

		Runnable myRunnable;
		// set current location
		final Location centerLocation = new Location("FakeLocationProvider");
		centerLocation.setLatitude(CHICAGO_CENTER.latitude);
		centerLocation.setLongitude(CHICAGO_CENTER.longitude);
		centerLocation.setAccuracy(20);
		centerLocation.setTime(System.currentTimeMillis());
		centerLocation.setSpeed(10.0f);
		centerLocation.setBearing(0);

		// need to run on UI Thread
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(centerLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(3000);} catch (Exception e1) {e1.printStackTrace();}

		myRunnable = new Runnable() {public void run() { 
			mActivity.onClearDebrisToggle(null);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		

		// now we're going to insert debris one by one in the danger zone
		mDistance = mActivity.getDangerRadiusInMeter();
		myRunnable = new Runnable() {public void run() {     
			mData.clearDebrisWithinRadius(mDistance*3);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertEquals(mData.getOverallDangerStatus(), false);


		double dangerRadius = mDistance;
		final double distanceStep = 800;
		final double bearingStep = 80;

		mDebrisIds.clear();
		// test dangerous debris
		for(int i=0; i*distanceStep <dangerRadius; i++){
			// get debris coordinate
			double distance = i*distanceStep;
			double bearing = i*bearingStep;
			MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, bearing);

			final Debris newDebris = new Debris(debrisLatLng);

			myRunnable = new Runnable() {public void run() {     
				mDebrisIds.add(mData.insert(newDebris));
			}};
			mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

			assertEquals(mData.getOverallDangerStatus(), true);

			// now remove it
			myRunnable = new Runnable() {public void run() {     
				mData.remove(mDebrisIds);
				mDebrisIds.clear();
			}};	
			mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

			assertEquals(mData.getOverallDangerStatus(), false);
		}

		// test non-dangerous debris
		for(int i=0; i < 3; i++){

			double distance = i*distanceStep + dangerRadius + 100;
			double bearing = 360 - i*bearingStep;
			// get debris coordinate
			MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, bearing);

			final Debris newDebris = new Debris(debrisLatLng);

			myRunnable = new Runnable() {public void run() {     
				mDebrisIds.add(mData.insert(newDebris));
			}};
			mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

			assertEquals(mData.getOverallDangerStatus(), false);

			// now remove it
			myRunnable = new Runnable() {public void run() {     
				mData.remove(mDebrisIds);
				mDebrisIds.clear();
			}};	
			mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

			assertEquals(mData.getOverallDangerStatus(), false);
		}

		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

	}


	public void testCompass() {

		Runnable myRunnable;

		// set current location
		final Location centerLocation = new Location("FakeLocationProvider");
		centerLocation.setLatitude(CHICAGO_CENTER.latitude);
		centerLocation.setLongitude(CHICAGO_CENTER.longitude);
		centerLocation.setAccuracy(20);
		centerLocation.setTime(System.currentTimeMillis());
		centerLocation.setSpeed(10.0f);
		centerLocation.setBearing(0);

		// need to run on UI Thread
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(centerLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(3000);} catch (Exception e1) {e1.printStackTrace();}

		myRunnable = new Runnable() {public void run() { 
			mActivity.onClearDebrisToggle(null);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// now we're going to insert debris one by one in the danger zone
		mDistance = mActivity.getDangerRadiusInMeter();
		myRunnable = new Runnable() {public void run() {    
			// for testing debris detection outside the dangerous zone
			mData.clearDebrisWithinRadius(mDistance*3);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

		assertEquals(mData.getOverallDangerStatus(), false);

		double dangerRadius = mDistance;
		final double distanceStep = 1000;
		final double bearingStep = 60;
		int i=0;
		mDebrisIds.clear();
		// now test the debrises outside the dangerous areas
		for(double distance=dangerRadius*2.5; distance > dangerRadius; distance-=distanceStep, i++){

			final double bearing = 360 - i*bearingStep;
			MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, bearing);

			final Debris newDebris = new Debris(debrisLatLng);

			newDebris.mBearingToUser = bearing;

			DH.showDebugWarning("bef runnable: " + newDebris.mBearingToUser);

			myRunnable = new Runnable() {public void run() { 
				DH.showDebugWarning("bef insert: " + newDebris.mBearingToUser);
				mDebrisIds.add(mData.insert(newDebris));
				DH.showDebugWarning("aft insert: " + newDebris.mBearingToUser);
			}};
			mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

			double angleDiff = CompassController.toSmallestDiffAngle(
					mCompass.getDebrisBearing(), bearing);

			if(!(angleDiff < 1.0)){
				DH.showDebugError("expected:"+ bearing +
						" value:" + mCompass.getDebrisBearing() + 
						", diff:" + angleDiff);
			}

			assertTrue(angleDiff < 1.0);
		}

		// now test the lethal one
		double distance = 0;
		i=0;
		for(distance = 200; distance < dangerRadius; i++){

			distance+=(i*distanceStep);
			double bearing = (bearingStep-10)*i;
			for(int j=0 ; j<3 && (bearing +j*bearingStep)<180; j++){
				bearing+=(j*bearingStep);
				MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, (double)bearing);
				final Debris newDebris = new Debris(debrisLatLng);


				DH.showDebugWarning("bef Runnable exp bearing: " + bearing);

				myRunnable = new Runnable() {public void run() {  
					DH.showDebugWarning("bef insert: " + newDebris.mBearingToUser);
					mDebrisIds.add(mData.insert(newDebris));
					DH.showDebugWarning("aft insert: " + newDebris.mBearingToUser);
				}};
				mActivity.runOnUiThread(myRunnable);
				try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}
			}
			try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

			// given the distance are all the same, 
			// compass should point to the smallest bearing 
			double angleDiff = CompassController.toSmallestDiffAngle(
					mCompass.getDebrisBearing(), (bearingStep-10)*i);
			if(!(angleDiff < 1.0)){
				DH.showDebugError("expected:"+ (bearingStep-10)*i +
						" value:" + mCompass.getDebrisBearing() + 
						", diff:" + angleDiff);
			}			 
			assertTrue(angleDiff < 1.0);

			// now remove it
			myRunnable = new Runnable() {public void run() {     
				mData.remove(mDebrisIds);
				mDebrisIds.clear();
			}};	
			mActivity.runOnUiThread(myRunnable);
			try {myRunnable.wait();} catch (Exception e1) {e1.printStackTrace();}
		}

		// now remove it
		myRunnable = new Runnable() {public void run() {     
			mData.remove(mDebrisIds);
			mDebrisIds.clear();
		}};	
		mActivity.runOnUiThread(myRunnable);
		try {myRunnable.wait();} catch (Exception e1) {e1.printStackTrace();}

		DH.showDebugInfo("testCompass-");
	}

	/*
	 * test the debris encounter from NON_DANGER-> DANGER->
	 * LETHAL-> DANGER-> NON_DANGER
	 */
	public void testDebrisEncounter() {

		Runnable myRunnable;
		MyLatLng point;
		final Debris debris = new Debris(CHICAGO_CENTER);

		// set current location
		final Location userLocation = new Location("FakeLocationProvider");
		userLocation.setLatitude(CHICAGO_CENTER.latitude);
		userLocation.setLongitude(CHICAGO_CENTER.longitude);
		userLocation.setAccuracy(20);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10.0f);
		userLocation.setBearing(0);

		// need to run on UI Thread
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mPosTracker.onLocationChanged(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}

		// now we're going to insert debris one by one in the danger zone
		// but first clear it first
		mDistance = mActivity.getDangerRadiusInMeter();
		myRunnable = new Runnable() {public void run() {    
			// for testing debris detection outside the dangerous zone
			mData.clearDebrisWithinRadius(mDistance*3);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

		// put the location outside the center point
		point = MyLatLng.point(userLocation, mDistance*2, (double)180);

		// update location
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);
		userLocation.setTime(System.currentTimeMillis());
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		// put the debris in Chicago
		myRunnable = new Runnable() {public void run() {    
			// for testing debris detection outside the dangerous zone
			mData.insert(debris);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(debris.mDangerFlag == Debris.DangerFlag.NON_DANGER);

		// now enter the radius with low speed (DANGER)
		// put the location outside the center point
		point = MyLatLng.point(CHICAGO_CENTER, mDistance-.5, (double)180);

		// update location
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(.5f);
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(debris.mDangerFlag == Debris.DangerFlag.DANGER);

		// now set so that as if the user will encounter debris with high speed (LETHAL)
		point = MyLatLng.point(CHICAGO_CENTER, mDistance/2, (double)180);

		// update location
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10f);
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(debris.mDangerFlag == Debris.DangerFlag.LETHAL);

		// now set so that as if the user will about to leave debris (LETHAL)
		point = MyLatLng.point(CHICAGO_CENTER, mDistance-.1, (double)0);

		// update location
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10f);
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(debris.mDangerFlag == Debris.DangerFlag.DANGER);

		// now leave the debris alert radius (NON_DEBRIS)
		point = MyLatLng.point(CHICAGO_CENTER, mDistance+.1, (double)0);

		// update location
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10f);
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(debris.mDangerFlag == Debris.DangerFlag.NON_DANGER);

		// last ly clean debris
		myRunnable = new Runnable() {public void run() {    
			// for testing debris detection outside the dangerous zone
			mData.clearDebrisWithinRadius(mDistance*3);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
	}

	public void testLaunchActivities() {

		// need to run on UI Thread
		final Location userLocation = new Location("FakeLocationProvider");
		userLocation.setLatitude(CHICAGO_CENTER.latitude);
		userLocation.setLongitude(CHICAGO_CENTER.longitude);
		userLocation.setAccuracy(20);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10.0f);
		userLocation.setBearing(0);
		
		Runnable myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}

		
		// reset debrises
		myRunnable = new Runnable() {public void run() {     
			mData.resetDebrises();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}
		
		// put sample debrises
		final Debris targetDebris = new Debris(SCHAUMBURG);
		final Debris sideDebris = new Debris(PALATINE);
		
		assertTrue(targetDebris.mDebrisId == Debris.DefaultId);
		
		myRunnable = new Runnable() {public void run() {     
			mData.insert(targetDebris);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}
		
		assertFalse(targetDebris.mDebrisId == Debris.DefaultId);
		
		myRunnable = new Runnable() {public void run() {     
			mData.insert(sideDebris);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}
	
		
		// test OfferGPS
		assertNull(mPosTracker.getOfferGpsAlert());
		
		myRunnable = new Runnable() {public void run() { 
			mPosTracker.offerGpsEnableDialog();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		final AlertDialog ad = mPosTracker.getOfferGpsAlert();
		assertNotNull(ad);
		
		// close OfferGPS
		myRunnable = new Runnable() {public void run() { 
			ad.dismiss();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		
		DH.showDebugInfo("testLaunchActivities, state=" + GP.state);
		assertTrue(GP.state == GP.AppState.RESUMED);

		
		// launch settings page
		mActivity.onSettingsButtonClick(null);

		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

		DH.showDebugInfo("testLaunchActivities, state=" + GP.state);
		assertTrue(GP.state == GP.AppState.STOPPED);

		sendKeys(KeyEvent.KEYCODE_BACK);

		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(GP.state == GP.AppState.RESUMED);

		myRunnable = new Runnable() {public void run() { 
			mData.insert(new Debris(CHICAGO_CENTER));
		}};
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// test compass click
		myRunnable = new Runnable() {public void run() { 
			mActivity.onCompassPress(null);
		}};
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
				
		// launch debris list page
		mActivity.onLongClick(null);

		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(GP.state == GP.AppState.STOPPED);

		sendKeys(KeyEvent.KEYCODE_BACK);

		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}

		assertTrue(GP.state == GP.AppState.RESUMED);
		
		// trigger run in background window
		sendKeys(KeyEvent.KEYCODE_BACK);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// close dialog
		myRunnable = new Runnable() {public void run() { 
			mActivity.dismissWarningDialog();
		}};
		
		myRunnable = new Runnable() {public void run() {     
			mData.resetDebrises();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}

	}
	
	public void testDebrisPanel(){
		
		final PopupMarkerInfo markerInfo = mData.getPopupMarkerInfo();
		final PopupNavigationInfo navInvo = mData.getPopupNavigationInfo();
		
		// need to run on UI Thread
		final Location userLocation = new Location("FakeLocationProvider");
		userLocation.setLatitude(CHICAGO_CENTER.latitude);
		userLocation.setLongitude(CHICAGO_CENTER.longitude);
		userLocation.setAccuracy(20);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10.0f);
		userLocation.setBearing(0);
		
		Runnable myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}
		
		// make sure all the panel doesn't have debris target yet
		assertNull(markerInfo.getTargetDebris());
		assertNull(navInvo.getTargetDebris());
		
		// put a debris in schaumburg
		final Debris targetDebris = new Debris(SCHAUMBURG);
		final Debris sideDebris = new Debris(PALATINE);
		
		assertTrue(targetDebris.mDebrisId == Debris.DefaultId);
		
		myRunnable = new Runnable() {public void run() {     
			mData.insert(targetDebris);
			mData.onMapClick(SCHAUMBURG);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}
		
		myRunnable = new Runnable() {public void run() {     
			mData.insert(sideDebris);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(600);} catch (Exception e1) {e1.printStackTrace();}
		
		assertTrue(targetDebris.mDebrisId != Debris.DefaultId);
		
		// simulate click on debris
		myRunnable = new Runnable() {public void run() {     
			mData.onMarkerClick(targetDebris.mMarker);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// make sure all the debris panel have our debris
		assertTrue(markerInfo.getTargetDebris().equals(targetDebris));
		
		// switch next
		myRunnable = new Runnable() {public void run() {     
			markerInfo.onShowNext(true);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		// switch before
		myRunnable = new Runnable() {public void run() {     
			markerInfo.onShowNext(false);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// simulate click to the navigation button
		myRunnable = new Runnable() {public void run() {     
			markerInfo.onNavigationButtonClick();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(4000);} catch (Exception e1) {e1.printStackTrace();}
		
		// make sure the navigation panel have our debris
		assertTrue(navInvo.getTargetDebris().equals(targetDebris));
		assertNull(markerInfo.getTargetDebris());
		
		// simulate click on debris
		myRunnable = new Runnable() {public void run() {     
			mData.onMarkerClick(targetDebris.mMarker);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// make sure all the debris panel have our debris
		assertTrue(markerInfo.getTargetDebris().equals(targetDebris));
		
		// remove the debris, simulate discard button
		myRunnable = new Runnable() {public void run() {     
			markerInfo.onDiscardButtonClick();
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
	}

	public void testTestJourney(){
		
		// need to run on UI Thread
		final Location userLocation = new Location("FakeLocationProvider");
		userLocation.setLatitude(CHICAGO_CENTER.latitude);
		userLocation.setLongitude(CHICAGO_CENTER.longitude);
		userLocation.setAccuracy(20);
		userLocation.setTime(System.currentTimeMillis());
		userLocation.setSpeed(10.0f);
		userLocation.setBearing(0);
		
		Runnable myRunnable = new Runnable() {public void run() { 
			mPosTracker.updateLocation(userLocation);
			mMap.setCamPosition(mMap.buildCamPosition(CHICAGO_CENTER), false);
		}};

		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}
		
		
		// start TestJourney
		myRunnable = new Runnable() {public void run() { 
			CheckBox cb = new CheckBox(mActivity.getBaseContext());
			cb.setChecked(true);
			mActivity.onTestJourneyToggle(cb);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// get the handle here
		final TestJourney tj = mActivity.getTestJourney();
		
		// make sure all the panel doesn't have debris target yet
		assertNull(tj.getStartMarker());
		assertNull(tj.getEndMarker());
		assertNull(tj.getRunTestThread());

		
		myRunnable = new Runnable() {public void run() {     
			tj.onMapClick(CHICAGO_CENTER);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		assertNotNull(tj.getStartMarker());
		
		myRunnable = new Runnable() {public void run() {     
			tj.onMapClick(SCHAUMBURG);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(6000);} catch (Exception e1) {e1.printStackTrace();}
		
		assertNotNull(tj.getEndMarker());
		assertNotNull(tj.getRunTestThread());
		
		myRunnable = new Runnable() {public void run() { 
			CheckBox cb = new CheckBox(mActivity.getBaseContext());
			cb.setChecked(false);
			mActivity.onTestJourneyToggle(cb);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		// close the test journey
	}
	
	public void testCameraViewMode(){
		
		DH.showDebugInfo("getTrackMode: " + mPosTracker.getTrackMode());
		assertTrue(mPosTracker.getTrackMode() == PositionTracker.TrackMode.NORMAL);
		
		Runnable myRunnable = new Runnable() {public void run() { 
			mActivity.onMyLocationButtonClick(null);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		DH.showDebugInfo("getTrackMode: " + mPosTracker.getTrackMode());
		assertTrue(mPosTracker.getTrackMode() == PositionTracker.TrackMode.FOLLOW);
		
		myRunnable = new Runnable() {public void run() { 
			mActivity.onMyLocationButtonClick(null);
		}};
		mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(2000);} catch (Exception e1) {e1.printStackTrace();}
		
		assertTrue(mPosTracker.getTrackMode() == PositionTracker.TrackMode.DRIVE);
		
	}
}
