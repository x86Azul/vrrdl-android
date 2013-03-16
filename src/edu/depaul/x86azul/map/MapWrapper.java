package edu.depaul.x86azul.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.helper.DH;
import edu.depaul.x86azul.map.MarkerWrapper.Type;


/**
 * a wrapper class for map service
 */
public class MapWrapper implements OnMapClickListener, 
OnMarkerClickListener, OnInfoWindowClickListener, 
OnCameraChangeListener, LocationSource, InfoWindowAdapter,
CancelableCallback {
	
	
	public static final float ZOOM_CLOSE_UP = 15;
	public static final float ZOOM_CLOSE_NORMAL = 10;
	
	private final int DEFAULT_CAMERA_ANIMATION_TIME = 800; // in ms

	private GoogleMap mGoogleMap;
	private MainActivity mContext;
	private OnGestureEvent mGestureClient;
	private OnCameraDisrupted mCameraClient;
	private Marker mMyLocation;
	private Object mBackUpData;
	
	/*
	 * we need to make sure animation doesn't clash 
	 * with each other, not only it might create crash internally
	 * but also interrupted animation is not really beautiful 
	 */
	private volatile boolean mIsCameraAnimating;
	private long mLastRequestTs;
	
	/*
	 * this is to hold the marker wrapper and marker 
	 * for translation when user tap marker in the map
	 */
	private Map<Marker, MarkerWrapper> mMarkerPair;
	
	private OnLocationChangedListener mClientMap;
	
	
	//private int mWidth;

	// the client need to implement this
	// and it needs to be from FragmentActivity class
	public interface OnGestureEvent{
		public void onMapClick(MyLatLng latLng);
		public void onInfoWindowClick(MarkerWrapper marker);
		public boolean onMarkerClick(MarkerWrapper marker);
		public View getInfoContents(MarkerWrapper marker);
		
	}
	
	public interface OnCameraDisrupted{
		public void onCameraDisrupted();
	}

	public MapWrapper(MainActivity context){

		// this is to make sure the activity implements the callback
		mContext = context;
		mMarkerPair = new HashMap<Marker, MarkerWrapper>();
		mIsCameraAnimating = false;
		
	}
	
	public void subscribeGestureAction(OnGestureEvent client) {
		mGestureClient = client;
	}
	
	public void subscribeCameraEvent(OnCameraDisrupted client) {
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

		MarkerWrapper markerW = getMarkerPair(marker);
		
		if(mGestureClient != null && markerW != null)
			return mGestureClient.onMarkerClick(markerW);
		
		return false;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		if(mGestureClient != null)
			mGestureClient.onMapClick(toLocal(latLng));
	}
	
	public void hijackNotification(boolean hijack, Object newData){
		if(hijack){
			mBackUpData = mGestureClient;
			mGestureClient = (OnGestureEvent)newData;
		}
		else {
			if(mBackUpData!=null)
				mGestureClient = (OnGestureEvent)mBackUpData;
		}
	}

	public boolean setUp() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mGoogleMap == null) {
			
			SupportMapFragment fragment = ((SupportMapFragment) ((FragmentActivity)mContext).getSupportFragmentManager().
					findFragmentById(R.id.map));
			// Try to obtain the map from the SupportMapFragment.
			mGoogleMap = fragment.getMap();			
			
			//mHeight = mContext.getResources().getDisplayMetrics().heightPixels;
			//mWidth = mContext.getResources().getDisplayMetrics().widthPixels;
			
			//DH.showDebugInfo("1 mHeight=" + mHeight + ", mWidth=" + mWidth);

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
		mGoogleMap.setOnMarkerClickListener(this);
		//mGoogleMap.setOnInfoWindowClickListener(this);
		mGoogleMap.setOnCameraChangeListener(this);
		
		mGoogleMap.setInfoWindowAdapter(this);

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
	
	private void updateCamera(CameraUpdate cameraUpdate, 
								boolean animate,
								int durationMs,
								boolean forceUpdate){	
		
		if(!setUp() || (cameraUpdate==null))
			return;
		
		if(animate){
			// only animate if currently no animation is performed
			
			// can't believe I uncover Google Map bug. animateCamera never return finish
			// if there's no change required in camera. no biggie, just put workaround
			// DH.showDebugInfo("updateCamera. isAnimating=" + mIsCameraAnimating);
			if(forceUpdate) {
				mGoogleMap.stopAnimation();
				if(mCameraClient!=null){
					mCameraClient.onCameraDisrupted();
				}
			}
				
			if(!mIsCameraAnimating || forceUpdate){

				mIsCameraAnimating = true;
				
				try{
					if(durationMs!=0){
						mGoogleMap.animateCamera(cameraUpdate, durationMs, this);
					} 
					else {
						mGoogleMap.animateCamera(cameraUpdate, this);
					}
				}
				catch(Exception e){
					DH.showDebugError(e.getClass().getName() + ", animateCamera fail");
				}
			}
		}
		else{
			mGoogleMap.moveCamera(cameraUpdate);
		}
	}

	private void updateCamera(CameraUpdate cameraUpdate, boolean animate){
		updateCamera(cameraUpdate, animate, 0, false);
	}
	
	public void showDrivingView(MyLatLng latlng, float bearing, int durationInMs, boolean animate) {
		
		CameraPosition cameraPosition = new CameraPosition.Builder()
			    .target(toMap(latlng))      // Sets the center of the map to Mountain View
			    .zoom(15)                   // Sets the zoom
			    .bearing(bearing)                // Sets the orientation of the camera to east
			    .tilt(45)                   // Sets the tilt of the camera to 30 degrees
			    .build();                   // Creates a CameraPosition from the builder
		
		updateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), animate, durationInMs, false);
	}

	public void showLocation(MyLatLng latLng, boolean animate){		
		updateCamera((CameraUpdate)buildCamPosition(latLng), animate);
	}
	
	public void showLocation(Location loc, boolean animate){	
		showLocation(MyLatLng.inLatLng(loc), animate);
	}
	
	public void setCamPosition(Object cuObject, boolean animate, int durationMs, boolean forceUpdate){
		updateCamera((CameraUpdate)cuObject, animate, durationMs, forceUpdate);
	}
	
	public void setCamPosition(Object cuObject, boolean animate, int durationMs){
		setCamPosition(cuObject, animate, durationMs, false);
	}
	
	public void setCamPosition(Object cuObject, boolean animate){
		setCamPosition(cuObject, animate, 0, false);
	}		
	
	public Object buildCamPosition(MyLatLng position, 
									int offsetX, 
									int offsetY,
									float zoom, 
									ArrayList<MyLatLng> bounds){
		CameraUpdate camUpdate;
		
		if (position != null){
			
			LatLng finalLatLng = toMap(position);
			
			if(offsetX != 0 || offsetY != 0){
				Projection proj = mGoogleMap.getProjection();
				Point point = proj.toScreenLocation(finalLatLng);
				point.offset(offsetX, offsetY);
				finalLatLng = proj.fromScreenLocation(point);
			}
			 		
			// if the zoom value is zero, then just set the latlng
			if(Float.compare(zoom, 0) == 0){
				camUpdate = CameraUpdateFactory.newLatLng(finalLatLng);
			}else{
				camUpdate = CameraUpdateFactory.newLatLngZoom(finalLatLng, zoom);
			}	
		} else if (bounds != null){
			
			Builder builder = new LatLngBounds.Builder();
			
			for(int i=0; i< bounds.size(); i++){
				builder.include(toMap(bounds.get(i)));
			}
			
			LatLngBounds tempBound = builder.build();

			camUpdate = CameraUpdateFactory.newLatLngBounds(tempBound, 100);
		} else {
			camUpdate = null;
		}
		return camUpdate;
	}
	
	
	public Object buildCamPosition(MyLatLng position, float zoom, ArrayList<MyLatLng> bounds){
		return buildCamPosition(position, 0, 0, zoom, bounds);
	}
	
	public Object buildCamPosition(MyLatLng position, float zoom){
		return buildCamPosition(position, 0, 0, zoom, null);
	}
	
	public Object buildCamPosition(MyLatLng position){
		return buildCamPosition(position, 0, 0, 0, null);
	}
	
	public Object buildCamPosition(MyLatLng position, int offsetX, int offsetY){
		return buildCamPosition(position, offsetX, offsetY, 0f, null);
	}
	

	public void insertMarker(MarkerWrapper markerW, boolean animate){
		
		if(markerW != null) {
			if(markerW.getType() == Type.DEBRIS)
				markerW.insertToMap(mGoogleMap, animate, this);
			else
				markerW.insertToMap(mGoogleMap, animate);
		}
	}
	
	public synchronized void removeMarkerPair(Marker marker){
		mMarkerPair.remove(marker);
	}
	
	public synchronized void setMarkerPair(Marker marker, MarkerWrapper markerW){
		mMarkerPair.put(marker, markerW);
	}
	
	private synchronized MarkerWrapper getMarkerPair(Marker marker){
		MarkerWrapper markerW = mMarkerPair.get(marker);
		
		return markerW;
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
		
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

	@Override
	public View getInfoContents(Marker marker) {
		

		MarkerWrapper markerW = getMarkerPair(marker);
		if(mGestureClient != null && markerW != null)
			return mGestureClient.getInfoContents(markerW);
		else
			return null;

	}

	@Override
	public View getInfoWindow(Marker marker) {

		MarkerWrapper markerW = getMarkerPair(marker);
		if(mGestureClient != null && markerW != null)
			return mGestureClient.getInfoContents(markerW);
		else
			return null;
			
		//return null;
	}

	@Override
	public void onCancel() {
		if(mCameraClient!=null){
			mCameraClient.onCameraDisrupted();
		}
		mIsCameraAnimating = false;
	}

	@Override
	public void onFinish() {
		mIsCameraAnimating = false;
	}
}