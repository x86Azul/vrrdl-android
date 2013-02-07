package edu.depaul.x86azul;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import edu.depaul.x86azul.test.TestJourney;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class MainActivity extends FragmentActivity 
	implements MapWrapper.Client, PositionTracker.Client, 
				WebWrapper.Client, DebrisTracker.Client, TestJourney.Client {

	private PositionTracker mPosTracker;
    private MapWrapper mMap;
    
    private DebrisTracker mData;
    
    private DebugLog mDebugLog;
    
    private boolean mMapClickTestEnable;
    
    private TestJourney mTestJourney;
    
    private boolean mTrackMyLocation;
    
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // this will keep screen bright while our app is on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        mMapClickTestEnable = true;
        mTrackMyLocation = false;
        
        mDebugLog = new DebugLog(this);
                      
        mMap = new MapWrapper(this);
        // we setup map here
        mMap.setUp();
        
        mPosTracker = new PositionTracker(this);
  
  
        // debris tracker need map handle to manage markers
        mData = new DebrisTracker(this, mMap);
        
        // this will open grab data from database
        mData.initialize();
        
       
              
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        mPosTracker.decideProvider();
		mMap.showLocation(mPosTracker.getLocationInLatLng(), false);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  
        mData.close();
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        
        // make sure data can be accessed
        mData.open();
    }

    @Override
    protected void onResume() {
        super.onResume(); 
        // just in case map is not setup yet
        mMap.setUp();
        
        mPosTracker.startTracking();   
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // stop subscribing to location
        mPosTracker.stopTracking();
    }
    
    /**
     * Called when the Set Debris button is clicked.
     */
    public void onSetDebrisClicked(View view) {

    	Debris debris = new Debris(mPosTracker.getLocation());
    	
    	// need to put into data first to get the markers
    	mData.insert(debris);
    	
    	mDebugLog.print("Set Marker: " + debris);
    	   	
    	return;
	}
  
	public void onNewLocationDetected() {
		// Called when a new location is found by the network location provider.
		String szTxt = "NewLocation: " + mPosTracker.getLocationInLatLng();
		mDebugLog.print(szTxt);
		
		mData.analyzeNewLocation(mPosTracker.getLocation());
		
		// move the camera if necessary
		if(mTrackMyLocation)
			mMap.showLocation(mPosTracker.getLocationInLatLng(), true);
	}
	
	public void onClearDebrisToggle(View view) {
		// untick directly
		((CheckBox) view).setChecked(false);
    	mData.resetData();
    }
	
	public void onTestMapClickToggle(View view) {
	
		mMapClickTestEnable = ((CheckBox) view).isChecked();
	}
	
	public void onTestJourneyToggle(View view) {
		if(((CheckBox) view).isChecked()) {
			
			mTestJourney = new TestJourney(this, mMap);	
			// let's switch all the location sources;
			mMap.setLocationSource(mTestJourney);
			mPosTracker.setLocationSource(mTestJourney);
		}
		else{
			// it's not checked, we need to stop
			if(mTestJourney!=null){
				// switch back
				mMap.setLocationSource(null);
				mPosTracker.setLocationSource(null);
				
				mTestJourney.forceStop();
				mTestJourney = null;
			}
		}
	}
	
	@SuppressLint("NewApi")
	public void onMyLocationButtonClick(View view) {
		if(mTrackMyLocation==false){
			mMap.showLocation(mPosTracker.getLocationInLatLng(), true);
			mTrackMyLocation = true;
			view.setAlpha(0.5f);
		}
		else{
			mTrackMyLocation = false;
			view.setAlpha(0.9f);
		}
	}
	
	@Override
	public void onMapClick(LatLng latLng) {
		
		if(mTestJourney != null && mTestJourney.needCoordinate()){
			// supply coordinate
			mTestJourney.setCoordinate(latLng);
		}
		else if(mMapClickTestEnable){
			// insert marker for testing
	    	Debris debris = new Debris(latLng);
	    	// need to put into data first
	    	mData.insert(debris);
	    	  	
	    	mDebugLog.print("Click Marker: " + debris);
		}
	}
	
	@Override
	public void onInfoWindowClick(Marker marker) {
		Toast.makeText(getBaseContext(), "Click Info Window", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onMarkerClick(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void onPopulateDataFromDbCompleted() {

		Log.w("QQQ", "onPopulateDataFromDbCompleted");
		
		// now analyze the current location against the data
		mData.analyzeNewLocation(mPosTracker.getLocation());
	}

	@Override
	public void onOpenDbCompleted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCloseDbCompleted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFinishProcessHttp(String result) {
		
	}

	@Override
	public void onFinishTestJourney() {
		
		// simulate untick;
		// unchecked the box
		CheckBox cb = (CheckBox)findViewById(R.id.testJourney);
		cb.setChecked(false);
		
		onTestJourneyToggle(cb);
	}

}

