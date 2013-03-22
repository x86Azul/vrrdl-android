package edu.depaul.x86azul;

import edu.depaul.x86azul.GP.AppState;
import edu.depaul.x86azul.activities.DebrisListActivity;
import edu.depaul.x86azul.activities.WebServiceAddressActivity;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.helper.URIBuilder;
import edu.depaul.x86azul.map.MapWrapper;
import edu.depaul.x86azul.runtest.TestJourney;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.WindowManager;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * This shows how to create a simple activity with a map and a marker on the map.
 * <p>
 * Notice how we deal with the possibility that the Google Play services APK is not
 * installed/enabled/updated on a user's device.
 */
public class MainActivity extends FragmentActivity 
	implements PositionTracker.OnNewLocationDetected, TestJourney.Client, OnLongClickListener {

	public static final String PREFERENCES_FILE = "VRRDLPrefs";
	public static final String PREF_TAP_MEANS_INSERT = "TapMeansInsert";
	
	public static final String PREF_VRRDL_WEB_SERVICE = "WebService";
	
	private MapWrapper mMap;
	
	private PositionTracker mPosTracker;
     
    private DataCoordinator mData;
    
    private TestJourney mTestJourney;
    
   
    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        GP.state = AppState.CREATED;
        
        setContentView(R.layout.activity_main);
        
        DH.showDebugMethodInfo(this);
        
        // this will keep screen bright while our app is on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // this map will be shared by debrisTraker and positionTracker
        mMap = new MapWrapper(this);
        mMap.setUp();
        
        // debris tracker need map handle to manage markers
        mData = new DataCoordinator(this, mMap);
        
        // posTracker need this to set map position provider and view
        mPosTracker = new PositionTracker(this, mMap);
        mPosTracker.subscribe(this);
        
        mPosTracker.startTracking();  
        
		// grab compass long click action
        findViewById(R.id.compass).setOnLongClickListener(this);
        
        // we want to do this here instead of onStart
        readInstanceState();   
        
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        DH.showDebugMethodInfo(this);
        
        GP.state = AppState.STARTED;  
        
        mData.onStart();       
            
    }
    
    @Override
    protected void onResume() {
        super.onResume(); 
        
        DH.showDebugMethodInfo(this);
        
        GP.state = AppState.RESUMED;
        
     
        
        mData.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        DH.showDebugMethodInfo(this);
        
        GP.state = AppState.PAUSED;
        
        mData.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  
        DH.showDebugMethodInfo(this);
        
        GP.state = AppState.STOPPED;   
        
        mData.onStop();
      
        writeInstanceState();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        DH.showDebugMethodInfo(this);
        
        GP.state = AppState.DESTROYED;       
        
        // close data
        mData.onDestroy();
        // stop subscribing to location
        mPosTracker.onDestroy();   
        
        // we need to stop
		if(mTestJourney!=null){				
			mTestJourney.dismiss();
			mTestJourney = null;
		}
    }
    
    @Override
    public void onBackPressed() {
    	offerRunInBackgroundDialog();
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
			mData.onNewLocation(mPosTracker.getLocation());
		}
	}
	
	public void onClearDebrisToggle(View view) {
		// untick directly
		((CheckBox) view).setChecked(false);
    	mData.resetDebrises();
    }
	
	public void onCompassPress(View view) {
		// Toast.makeText(getBaseContext(), "compass", Toast.LENGTH_SHORT).show();

		mData.showCompassTarget();
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
				mTestJourney.dismiss();
				mTestJourney = null;
			}
		}
	}
	
	public void onMyLocationButtonClick(View button) {
		
		// change the button based on current status
		mPosTracker.toggleTrackMode();

	}

	@Override
	public void onFinishTestJourney() {
		
		// simulate untick;
		// unchecked the box
		CheckBox cb = (CheckBox)findViewById(R.id.testJourney);
		cb.setChecked(false);
		
		onTestJourneyToggle(cb);
	}
	
	public void onSettingsButtonClick(View view) {
		Intent intent = new Intent(this, WebServiceAddressActivity.class);

		startActivityForResult(intent, 2);
		// handle activity transition
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_down_out);
	}

	@Override
	public boolean onLongClick(View v) {
	
		// compass long press action
		Intent intent = new Intent(this, DebrisListActivity.class);
		mData.parcellToIntent(intent);
		
		startActivityForResult(intent, 1);
		// handle activity transition
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_down_out);
		// generate short vibrate (15 ms)
		((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(15);

		return false;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		// if user press back button the intent will be null
		if(intent!=null){
			if(requestCode == 1){
				long debrisId = Long.parseLong(intent.getStringExtra("debrisId"));
				mData.showDebrisTarget(debrisId);
			}
			else if(requestCode == 2){

				DH.showToast(this, "Set WebURI to: " + GP.webServiceURI);
				// save this new info
				writeInstanceState();
			}
		}
	}
	@Override
	protected void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		
		Long debrisId = intent.getLongExtra("debrisId", 0L);
		boolean danger = intent.getBooleanExtra(("inDanger"), false);
		
		mData.onNotificationBarPressed(debrisId, danger);
	}
	
	public boolean readInstanceState() {

		SharedPreferences p = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);

		GP.tapMeansInsert = p.getBoolean(PREF_TAP_MEANS_INSERT, true);
		GP.webServiceURI = p.getString(PREF_VRRDL_WEB_SERVICE, WebServiceAddressActivity.AMAZON_SERVER);
		
		//DialogHelper.showDebugInfo("read:"+ enable);
		
		return true;
	}
	
	public boolean writeInstanceState() {

        SharedPreferences p = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);

        SharedPreferences.Editor e = p.edit();

        e.putBoolean(PREF_TAP_MEANS_INSERT, GP.tapMeansInsert);
        e.putString(PREF_VRRDL_WEB_SERVICE, GP.webServiceURI);
        

        boolean ret = e.commit();
        
        //DialogHelper.showDebugInfo("write:"+ iData + ";commit:" + ret);
        return (ret);

    }
	
	private void offerRunInBackgroundDialog(){
		
		// ask user if wanted to enable GPS. if not just use network provided location
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
		.setMessage("You're leaving VRRDL screen. What do you want to do?")
		.setCancelable(false)
		.setPositiveButton("Run in background",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent homeIntent= new Intent(Intent.ACTION_MAIN);
				homeIntent.addCategory(Intent.CATEGORY_HOME);
				homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(homeIntent);
			}
		});
		alertDialogBuilder.setNegativeButton("Exit application",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				finish();
			}
		});

		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}
	
	/*
	 * for black box testing
	 */
	
	public DataCoordinator getDataCoordinator(){
		return mData;
	}
	
	public PositionTracker getPositionTracker(){
		return mPosTracker;
	}
	
	public MapWrapper getMap(){
		return mMap;
	}
	
	public double getDangerRadiusInMeter(){
		return GP.dangerRadiusInMeter;
	}
	
	public void setTestMode(boolean test){
		GP.testMode = test;
	}
}

