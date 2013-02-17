package edu.depaul.x86azul;

import edu.depaul.x86azul.helper.DialogHelper;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.test.TestJourney;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class MainActivity extends FragmentActivity 
	implements PositionTracker.Client, TestJourney.Client, OnLongClickListener {

	private MapWrapper mMap;
	
	private PositionTracker mPosTracker;
     
    private DebrisTracker mData;
    
    private TestJourney mTestJourney;
    
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        DialogHelper.showDebugMethodInfo(this);
        
        // this will keep screen bright while our app is on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // this map will be shared by debrisTraker and positionTracker
        mMap = new MapWrapper(this);
        mMap.setUp();
        
        // debris tracker need map handle to manage markers
        mData = new DebrisTracker(this, mMap);
        
        // posTracker need this to set map position provider and view
        mPosTracker = new PositionTracker(this, mMap);
        mPosTracker.subscribe(this);
        
		// grab compass long click action
        findViewById(R.id.compass).setOnLongClickListener(this);
   
    }

    @Override
    protected void onStart() {
        super.onStart();
        // make sure data can be accessed
        DialogHelper.showDebugMethodInfo(this);
        mData.open();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  
        DialogHelper.showDebugMethodInfo(this);
        
        mData.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mPosTracker.startTracking();   
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // stop subscribing to location
        mPosTracker.stopTracking();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        DialogHelper.showDebugMethodInfo(this);
    }
    
    
    
    /**
     * Called when the Set Debris button is clicked.
     */
    public void onSetDebrisClicked(View view) {

    	Debris debris = new Debris(mPosTracker.getLocation());
    	// need to put into data first to get the markers
    	mData.insert(debris);
  
    	return;
	}
  
	public void onNewLocationDetected() {
		if(mData != null){
			mData.analyzeNewLocation(mPosTracker.getLocation());
		}
	}
	
	public void onClearDebrisToggle(View view) {
		// untick directly
		((CheckBox) view).setChecked(false);
    	mData.resetData();
    }
	
	public void onCompassPress(View view) {
		// Toast.makeText(getBaseContext(), "compass", Toast.LENGTH_SHORT).show();

		mPosTracker.setCameraChange(mData.getCompassTarget());
    }
	
	public void onTestMapClickToggle(View view) {
		mData.setTapMeansInsertMode(((CheckBox) view).isChecked());
	}
	
	public void onTestJourneyToggle(View view) {
		if(((CheckBox) view).isChecked()) {
			// we will need to hijack this map and postracker
			mTestJourney = new TestJourney(this, mMap, mPosTracker, mData);
			// we'll let you know if we're finish
			mTestJourney.subscribe(this);
		}
		else{
			// it's not checked, we need to stop
			if(mTestJourney!=null){				
				mTestJourney.forceStop();
				mTestJourney = null;
			}
		}
	}
	
	public void onMyLocationButtonClick(View button) {
		
		// change the button based on current status
		mPosTracker.toggleTrackMode(true);

	}

	@Override
	public void onFinishTestJourney() {
		
		// simulate untick;
		// unchecked the box
		CheckBox cb = (CheckBox)findViewById(R.id.testJourney);
		cb.setChecked(false);
		
		onTestJourneyToggle(cb);
	}


	@Override
	public boolean onLongClick(View v) {
		// compass long press action
		Intent intent = new Intent(this, DebrisListActivity.class);
		mData.parcellToIntent(intent);
		
		startActivityForResult(intent, 0);
		// handle activity transition
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_down_out);
		// generate short vibrate (15 ms)
		((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(15);

		return false;
	}
	

	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		// if user press back button the intent will be null
		if(intent!=null){
			long debrisId = Long.parseLong(intent.getStringExtra("debrisId"));
			mPosTracker.setCameraChange(mData.showDebrisTarget(debrisId));
		}
	}

}

