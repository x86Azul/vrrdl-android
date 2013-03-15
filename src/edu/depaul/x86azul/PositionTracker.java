package edu.depaul.x86azul;

import edu.depaul.x86azul.map.MapWrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;

public class PositionTracker implements LocationListener, MapWrapper.OnCameraDisrupted {

	private LocationManager mLocationManager;
	private Location mCurrentLocation;
	private String mLocationProvider;
	
	private MainActivity mContext;
	private Client mClient;
	
	private MapWrapper mMap;
	
	private Location mBackupLocation;
	
    private TrackMode mTrackMode;
	
    private enum TrackMode{
    	NORMAL, FOLLOW, DRIVE
    }
	// the client need to implement this
	public interface Client{
		public void onNewLocationDetected();
	}

	public PositionTracker(MainActivity context, MapWrapper map){

		// this is to make sure the activity implements the callback
		mContext = context;
		
		mMap = map;
		mMap.provideLocation();
		mMap.subscribeCameraEvent(this);

		// setup the location finder
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		// set default to Network
		mLocationProvider = LocationManager.NETWORK_PROVIDER;

		mCurrentLocation = mLocationManager.getLastKnownLocation(mLocationProvider);
		
		mTrackMode = TrackMode.NORMAL;
		
		// this will check if we can use other provider (e.g. GPS)
		decideProvider();
		
		// finally, broadcast the location info and show user the current location
		mMap.showLocation(mCurrentLocation, false);
		updateLocation(mCurrentLocation);
		
	}
	
	public void subscribe(Client client){
		mClient = client;
		// give the first location
		mClient.onNewLocationDetected();
	}
	
	public void onDestroy(){
		stopTracking(); 
	}

	public void decideProvider(){

		// see if we can upgrade to GPS
		if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			mLocationProvider = LocationManager.GPS_PROVIDER;
		}else {
			offerGpsEnableDialog();
		}

		// get last good location again (in case we use different provider)
		Location newLocation = mLocationManager.getLastKnownLocation(mLocationProvider);

		if(isBetterLocation(newLocation, mCurrentLocation))
			updateLocation(newLocation);
	}

	public void startTracking(){
		// start subscribing to location
		mLocationManager.requestLocationUpdates(mLocationProvider, 0, 0, this);
	}

	public void stopTracking(){
		mLocationManager.removeUpdates(this);
	}

	public Location getLocation(){
		return mCurrentLocation;
	}

	public MyLatLng getLocationInLatLng(){
		return new MyLatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
	}
	
	public void hijackLocationProvider(boolean start){
		// we have been hijacked, hence we'll use different provider
		if(start){
			mBackupLocation = mCurrentLocation;
			stopTracking();
		}
		else {
			startTracking();
			updateLocation(mBackupLocation);
		}
		
	}
	
	public void updateLocation(Location newLocation){
		// update the current location and notify the client
		mCurrentLocation = newLocation;

		//Log.w("QQQ", "updateLocation, mClientMap="+ mClientMap);
		
		if(mTrackMode == TrackMode.FOLLOW)
			mMap.showLocation(mCurrentLocation, true);
		if(mTrackMode == TrackMode.DRIVE)
			mMap.showDrivingView(MyLatLng.inLatLng(mCurrentLocation), 
					mCurrentLocation.getBearing(), 0, true);
	
		if(mClient!= null)
			mClient.onNewLocationDetected();
		
		mMap.provideLocation(mCurrentLocation);
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 * this happen to be called by both LocationListener and LocationSource.OnLocationChangedListener
	 */
	public void onLocationChanged(Location newLocation) {
		if(isBetterLocation(newLocation, mCurrentLocation)) {
			updateLocation(newLocation);
		}
	}
	
	@Override
	public void onCameraDisrupted() {
		
		if(mTrackMode != TrackMode.NORMAL){
			Button button = (Button)mContext.findViewById(R.id.location_button);
			button.setBackgroundResource(R.drawable.location_button_normal);
			mTrackMode = TrackMode.NORMAL;

		}
	}
	
	public void toggleTrackMode(){
		
		Button button = (Button)mContext.findViewById(R.id.location_button);
		
		if(mTrackMode == TrackMode.NORMAL){
			mTrackMode = TrackMode.FOLLOW;
			button.setBackgroundResource(R.drawable.location_button_follow);				
			mMap.showLocation(mCurrentLocation, true);
		} 
		else if(mTrackMode == TrackMode.FOLLOW){
			mTrackMode = TrackMode.DRIVE;
			button.setBackgroundResource(R.drawable.location_button_drive);
			mMap.showDrivingView(MyLatLng.inLatLng(mCurrentLocation), 
					mCurrentLocation.getBearing(), 0, true);
		}
		else if(mTrackMode == TrackMode.DRIVE){
			button.setBackgroundResource(R.drawable.location_button_normal);
			mTrackMode = TrackMode.NORMAL;
		}
		
		return;
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	private boolean isBetterLocation(Location newLocation, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}
		if(newLocation == null){
			// A null new location is definitely no better
			return false;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private void offerGpsEnableDialog(){
		
		// ask user if wanted to enable GPS. if not just use network provided location
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
		alertDialogBuilder
		.setMessage("GPS service is disabled in your device. What do you want to do?")
		.setCancelable(false)
		.setPositiveButton("Enable in Location Setting",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent callGPSSettingIntent = new Intent(
						android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				((Activity)mClient).startActivity(callGPSSettingIntent);
			}
		});
		alertDialogBuilder.setNegativeButton("Use Network Location Service",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		AlertDialog alert = alertDialogBuilder.create();
		alert.show();
	}

}
