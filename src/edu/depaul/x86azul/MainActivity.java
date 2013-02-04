package edu.depaul.x86azul;

import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class MainActivity extends FragmentActivity 
	implements MapWrapper.Client, PositionTracker.Client, 
				WebWrapper.User, DebrisTracker.Client {

	private PositionTracker mPosTracker;
    private MapWrapper mMap;
    private WebWrapper mWeb;
        
    private TextView mTopText;
    
    private DebrisTracker mData;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTopText = (TextView) findViewById(R.id.top_text);
        
        mMap = new MapWrapper(this);
        // we setup map here
        mMap.setUp();
        
        mPosTracker = new PositionTracker(this);
  
        mWeb = new WebWrapper(this);
        
        mData = new DebrisTracker(this, mMap);
        
        // this will open grab data from database
        mData.initialize();
              
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        mPosTracker.decideProvider();
		mMap.showLocation(mPosTracker.getLocationInLatLng());
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
    	
    	mTopText.setText("Set Marker: " + debris);
    	
    	return;
	}

	public void alertNewLocation() {
		// Called when a new location is found by the network location provider.
		String szTxt = "NewLocation: " + mPosTracker.getLocationInLatLng();
		mTopText.setText(szTxt);
		
		mData.analyzeNewLocation(mPosTracker.getLocation());
		
	}

	@Override
	public void onMapClick(LatLng latLng) {
		// insert marker for testing
    	Debris debris = new Debris(latLng);
    	// need to put into data first
    	mData.insert(debris);
    	  	
    	mTopText.setText("Click Marker: " + debris);
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
	public void completedWebExec() {
		Toast.makeText(getBaseContext(), "WebExe Complete", Toast.LENGTH_SHORT).show();
		
	}
	
	@Override
	public void completedPopulateDataFromDb() {
		// now analyze the current location against the data
		mData.analyzeNewLocation(mPosTracker.getLocation());
	}

	@Override
	public void completedOpenDb() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void completedCloseDb() {
		// TODO Auto-generated method stub
		
	}

}

