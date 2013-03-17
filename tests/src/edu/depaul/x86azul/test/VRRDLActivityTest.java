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
import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.PositionTracker;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.map.MapWrapper;
import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.KeyEvent;
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
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}
			

		// now we're going to insert debris one by one in the danger zone
		mDistance = mActivity.getDangerRadiusInMeter();
		myRunnable = new Runnable() {public void run() {     
			mData.clearDebrisWithinRadius(mDistance);
        }};
        mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}
		
		assertEquals(mData.getOverallDangerStatus(), false);
		
		
		double dangerRadius = mDistance;
		final double distanceStep = 500;
		final double bearingStep = 45;
		
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
			try {Thread.sleep(300);} catch (Exception e1) {e1.printStackTrace();}
			
			assertEquals(mData.getOverallDangerStatus(), true);
			
			// now remove it
			myRunnable = new Runnable() {public void run() {     
				mData.remove(mDebrisIds);
				mDebrisIds.clear();
	        }};	
	        mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(300);} catch (Exception e1) {e1.printStackTrace();}
			
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
    	
    	DH.showDebugInfo("testCompass+");
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
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
			
		// now we're going to insert debris one by one in the danger zone
		mDistance = mActivity.getDangerRadiusInMeter();
		myRunnable = new Runnable() {public void run() {    
			// for testing debris detection outside the dangerous zone
			mData.clearDebrisWithinRadius(mDistance*3);
        }};
        mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}
		
		assertEquals(mData.getOverallDangerStatus(), false);
		
		
		double dangerRadius = mDistance;
		final double distanceStep = 500;
		final double bearingStep = 45;
		int i=0;
		mDebrisIds.clear();
		// now test the debrises ooutside the dangerous areas
		for(double distance=dangerRadius*2.5; distance > dangerRadius; distance-=distanceStep, i++){

			double bearing = 360 - i*bearingStep;
			MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, bearing);
			
			final Debris newDebris = new Debris(debrisLatLng);
			
			myRunnable = new Runnable() {public void run() {     
				mDebrisIds.add(mData.insert(newDebris));
	        }};
	        mActivity.runOnUiThread(myRunnable);
			try {Thread.sleep(500);} catch (Exception e1) {e1.printStackTrace();}
			
			double angleDiff = CompassController.toSmallestDiffAngle(
					mCompass.getDebrisBearing(), bearing);
			
			assertTrue(angleDiff < 1.0);
		}
		
		
		// now test the lethal one
		double distance = 0;
		i=0;
		for(distance = 200; distance < dangerRadius; i++){

			distance+=(i*distanceStep);
			double bearing = (bearingStep-10)*i;
			for(int j=0 ; j<4 && (bearing +j*bearingStep)<180; j++){
				bearing+=(j*bearingStep);
				MyLatLng debrisLatLng = MyLatLng.point(centerLocation, distance, (double)bearing);
				final Debris newDebris = new Debris(debrisLatLng);
				
				myRunnable = new Runnable() {public void run() {   
					mDebrisIds.add(mData.insert(newDebris));
		        }};
		        mActivity.runOnUiThread(myRunnable);
				try {Thread.sleep(300);} catch (Exception e1) {e1.printStackTrace();}
			}
			
			// given the distance are all the same, 
			// compass should point to the smallest bearing 
			double angleDiff = CompassController.toSmallestDiffAngle(
					mCompass.getDebrisBearing(), (bearingStep-10)*i);
			if(!(angleDiff < 1.0)){
				DH.showDebugError("expected:"+ (bearingStep-10)*i +
						          " value:" + mCompass.getDebrisBearing() + 
						          ", diff:" + angleDiff);
			}
			else{
				DH.showDebugInfo("expected:"+ (bearingStep-10)*i +
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
			try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		}
		
		// now remove it
		myRunnable = new Runnable() {public void run() {     
			mData.remove(mDebrisIds);
			mDebrisIds.clear();
        }};	
        mActivity.runOnUiThread(myRunnable);
		try {Thread.sleep(1000);} catch (Exception e1) {e1.printStackTrace();}
		
		DH.showDebugInfo("testCompass-");
    }

}
