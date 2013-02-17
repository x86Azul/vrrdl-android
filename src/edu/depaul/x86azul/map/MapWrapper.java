package edu.depaul.x86azul.map;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.helper.DialogHelper;


/**
 * a wrapper class for map service
 */
public class MapWrapper implements OnMapClickListener, 
OnMarkerClickListener, OnInfoWindowClickListener, 
OnCameraChangeListener, LocationSource {
	
	
	public static final float ZOOM_CLOSE_UP = 15;
	public static final float ZOOM_CLOSE_NORMAL = 10;
	
	private final int NORMAL_ANIMATION_TIME = 300; // in ms
	private final int SIGNAL_ANIMATION_TIME = 1500; // in ms

	private GoogleMap mGoogleMap;
	private MainActivity mContext;
	private GestureClient mGestureClient;
	private CameraClient mCameraClient;
	private Marker mMyLocation;
	private Object mBackUpData;
	
	private OnLocationChangedListener mClientMap;
	
	private int mHeight;
	private int mWidth;

	// the client need to implement this
	// and it needs to be from FragmentActivity class
	public interface GestureClient{
		public void onMapClick(MyLatLng latLng);
		public void onInfoWindowClick(MarkerWrapper marker);
		public boolean onMarkerClick(MarkerWrapper marker);
	}
	
	public interface CameraClient{
		public void onCameraChange();
	}

	public MapWrapper(MainActivity context){

		// this is to make sure the activity implements the callback
		mContext = context;
	}
	
	public void subscribeGestureAction(GestureClient client) {
		mGestureClient = client;
	}
	
	public void subscribeCameraEvent(CameraClient client) {
		mCameraClient = client;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		/*if(mGestureClient != null)
			mGestureClient.onInfoWindowClick(marker);
			*/
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		/*
		if(mGestureClient != null)
			return mGestureClient.onMarkerClick(marker);
		else
			return false;
			*/
		return true;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		if(mGestureClient != null)
			mGestureClient.onMapClick(toLocal(latLng));
	}
	
	public void hijackNotification(boolean hijack, Object newData){
		if(hijack){
			mBackUpData = mGestureClient;
			mGestureClient = (GestureClient)newData;
		}
		else {
			if(mBackUpData!=null)
				mGestureClient = (GestureClient)mBackUpData;
		}
	}

	public boolean setUp() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mGoogleMap == null) {
			
			SupportMapFragment fragment = ((SupportMapFragment) ((FragmentActivity)mContext).getSupportFragmentManager().
					findFragmentById(R.id.map));
			// Try to obtain the map from the SupportMapFragment.
			mGoogleMap = fragment.getMap();
			mHeight = mContext.getResources().getDisplayMetrics().heightPixels;
			mWidth = mContext.getResources().getDisplayMetrics().widthPixels;
			
			DialogHelper.showDebugInfo("mHeight=" + mHeight + ", mWidth=" + mWidth);

			// Check if we were successful in obtaining the map.
			if (mGoogleMap != null)
				return firstTimeSetUp();
			else
				return false;
		}
		return true;
	}
	
	private boolean firstTimeSetUp() {

		mGoogleMap.setOnMapClickListener(this);
		//mGoogleMap.setOnMarkerClickListener(this);
		//mGoogleMap.setOnInfoWindowClickListener(this);
		mGoogleMap.setOnCameraChangeListener(this);

		// we're going to user our own gps tracker
		mGoogleMap.setMyLocationEnabled(true);  
		mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
		mGoogleMap.getUiSettings().setCompassEnabled(false);
		mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
		
		mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(10));
		
		return true;
	}
	
	public void provideLocation(){
		if(!setUp())
			return;
		mGoogleMap.setLocationSource(this);
	}
	
	public void provideLocation(Location loc){
		
		if(mClientMap != null)
			mClientMap.onLocationChanged(loc);
	}

	// These are all camera updates related APIs
	
	private void updateCamera(CameraUpdate cameraUpdate, boolean animate){	
		if(!setUp() || (cameraUpdate==null))
			return;
		
		if(animate)
			mGoogleMap.animateCamera(cameraUpdate);
		else
			mGoogleMap.moveCamera(cameraUpdate);
	}
	
	public void showDrivingView(MyLatLng latlng, float bearing, boolean animate) {
		
		CameraPosition cameraPosition = new CameraPosition.Builder()
			    .target(toMap(latlng))      // Sets the center of the map to Mountain View
			    .zoom(15)                   // Sets the zoom
			    .bearing(bearing)                // Sets the orientation of the camera to east
			    .tilt(45)                   // Sets the tilt of the camera to 30 degrees
			    .build();                   // Creates a CameraPosition from the builder
		
		updateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animate);
	}

	public void showLocation(Location loc, boolean animate){	
		showLocation(MyLatLng.inLatLng(loc), animate);
	}
	
	public void showLocation(MyLatLng latLng, boolean animate){		
		updateCamera(CameraUpdateFactory.newLatLng(toMap(latLng)), animate);
	}
	
	public void showBoundLocation(MyLatLng latLng1, MyLatLng latLng2, boolean animate) {
		setNewViewPosition(getNewViewPosition(null, 0, latLng1, latLng2), animate);
	}
	
	public void setNewViewPosition(Object cuObject, boolean animate){
		updateCamera((CameraUpdate)cuObject, animate);
	}
	
	public Object getNewViewPosition(MyLatLng position, float zoom, MyLatLng bound1, MyLatLng bound2){
		
		CameraUpdate camUpdate;
		
		if (position != null){
			// if the zoom value is zero, then just set the latlng
			if(Float.compare(zoom, 0) == 0){
				camUpdate = CameraUpdateFactory.newLatLng(toMap(position));
			}else{
				camUpdate = CameraUpdateFactory.newLatLngZoom(toMap(position), zoom);
			}	
		} else if (bound1 != null && bound2 != null){
			LatLngBounds bounds = new LatLngBounds.Builder()
	        .include(toMap(bound1))
	        .include(toMap(bound2))
	        .build();

			// need to make sure the bound also consider our transparent button
			double horzDist = MyLatLng.distance(toLocal(bounds.northeast), 
					new MyLatLng(bounds.northeast.latitude, bounds.southwest.longitude));

			double vertDist = MyLatLng.distance(toLocal(bounds.southwest), 
					new MyLatLng(bounds.northeast.latitude, bounds.southwest.longitude));

			int pad = horzDist > vertDist? 80:180;

			camUpdate = CameraUpdateFactory.newLatLngBounds(bounds, pad);
		} else {
			camUpdate = null;
		}
		return camUpdate;
	}

	public void insertMarker(MarkerWrapper marker, boolean animate){
		marker.insertToMap(mGoogleMap, animate);
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
		if(mCameraClient!=null){
			mCameraClient.onCameraChange();
		}
	}
	
	private MyLatLng toLocal(LatLng latlng){
		return new MyLatLng(latlng.latitude, latlng.longitude);
	}
	
	private LatLng toMap(MyLatLng latlng){
		return new LatLng(latlng.latitude, latlng.longitude);
	}
	
	// LocationSource related APIs
	@Override
	public void activate(OnLocationChangedListener client) {
		mClientMap = client;
		//Log.w("QQQ", "activate");
	}

	@Override
	public void deactivate() {
		mClientMap = null;
	}
	

}