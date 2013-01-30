package edu.depaul.x86azul;

import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
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
	implements OnMapClickListener, OnMarkerClickListener, 
				OnInfoWindowClickListener, PositionTracking.Client{
    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private PositionTracking mPosTracking;
    private DbAdapter mDbAdapter;
    
    private TextView mTopText;

      

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTopText = (TextView) findViewById(R.id.top_text);
        
        mDbAdapter = new DbAdapter(this);
        
        mPosTracking = new PositionTracking(this);
        
        // now setup the map, don't need to care about the return value
        trySetUpMap();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        if(!trySetUpMap())
        	return;
        
        mDbAdapter.open();
        
        showDebrisDataFromDb();
    }
    
    @Override
    protected void onStop() {
        super.onStop();

        mDbAdapter.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if(!trySetUpMap())
        	return;
        
        mPosTracking.StartTracking();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // stop subscribing to location
        mPosTracking.StopTracking();
    }

    /**
     * we really need the map to be ready before we can proceed with others
     */
    private boolean trySetUpMap() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            } 
            else {
            	return false;
            }
        }
        return true;
    }
    
    private void setUpMap() {
    	
    	mMap.setOnInfoWindowClickListener(this);
    	
        mMap.setMyLocationEnabled(true);
        // set the camera to user location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
        					mPosTracking.getLocationInLatLng(), 10));
        
        

    }
    
    /**
     * Called when the Set Debris button is clicked.
     */
    public void onSetDebrisClicked(View view) {
    	if(!trySetUpMap())
        	return;
    	
    	Debris debris = new Debris(mPosTracking.getLocation());
    	
    	// this will assign debris id as well
    	mDbAdapter.insertDebris(debris);
    	addDebrisMarker(debris);
    	
    	mTopText.setText("Set Marker: " + extractInstanceInfo(debris));
    	
    	return;
	}

    
    @SuppressLint("NewApi")
	private String extractInstanceInfo(Debris debris)
    {
    	return  " Lat=" + debris.mLatitude +
                ", Long=" + debris.mLongitude +
                ", Time=" + debris.mTime +
                ", Speed=" + debris.mSpeed + 
                ", Accuracy=" + debris.mAccuracy +
                ", ID=" + debris.mDebrisId; 	
    }


	@Override
	public boolean onMarkerClick(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public void newLocationAlert() {
		// Called when a new location is found by the network location provider.
		String szTxt = "NewLocation: " + mPosTracking.getLocationInLatLng();
		mTopText.setText(szTxt);
		
	}
	
	private void addDebrisMarker(Debris debris){
		mMap.addMarker(new MarkerOptions()
	        .position(new LatLng(debris.mLatitude, debris.mLongitude))
	        .title("Debris#" + debris.mDebrisId)
	        .snippet("speed=" + debris.mSpeed + ", acc=" + debris.mAccuracy));
	}
	
	private void showDebrisDataFromDb(){
		List<Debris> debrisList = mDbAdapter.getAllDebrisRecords();
		
		for (int i = 0; i < debrisList.size(); i++) {
			addDebrisMarker((Debris)debrisList.get(i));
		}
	}

	@Override
	public void onMapClick(LatLng latLng) {
		// insert marker for testing
    	Debris debris = new Debris(latLng);
    	
    	// this will assign debris id as well
    	mDbAdapter.insertDebris(debris);
    	
    	addDebrisMarker(debris);
    	
    	mTopText.setText("Click Marker: " + extractInstanceInfo(debris));
	}

	@Override
	public void onInfoWindowClick(Marker arg0) {
		Toast.makeText(getBaseContext(), "Click Info Window", Toast.LENGTH_SHORT).show();
	}
}

