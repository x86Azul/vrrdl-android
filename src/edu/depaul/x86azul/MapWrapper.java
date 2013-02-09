package edu.depaul.x86azul;

import java.util.ArrayList;

import java.util.List;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import edu.depaul.x86azul.helper.LatLngTool;


/**
 * a wrapper class for map service
 */
public class MapWrapper implements OnMapClickListener, 
OnMarkerClickListener, OnInfoWindowClickListener, OnCameraChangeListener {
	
	private final int NORMAL_ANIMATION_TIME = 300; // in ms
	private final int SIGNAL_ANIMATION_TIME = 1500; // in ms

	private GoogleMap mGoogleMap;
	private MainActivity mContext;
	private GestureClient mGestureClient;
	private CameraClient mCameraClient;
	private Marker mMyLocation;
	private Object mBackUpData;

	// the client need to implement this
	// and it needs to be from FragmentActivity class
	public interface GestureClient{
		public void onMapClick(LatLng latLng);
		public void onInfoWindowClick(Marker marker);
		public boolean onMarkerClick(Marker marker);
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
		if(mGestureClient != null)
			mGestureClient.onInfoWindowClick(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if(mGestureClient != null)
			return mGestureClient.onMarkerClick(marker);
		else
			return false;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		if(mGestureClient != null)
			mGestureClient.onMapClick(latLng);
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
			// Try to obtain the map from the SupportMapFragment.
			mGoogleMap = ((SupportMapFragment) ((FragmentActivity)mContext).getSupportFragmentManager().
					findFragmentById(R.id.map)).getMap();

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
	
	public void setLocationSource(LocationSource ls){
		if(!setUp())
			return;
		mGoogleMap.setLocationSource(ls);
	}

	public void showLocation(Location loc, boolean animate){
		if(!setUp())
			return;
		// set the camera to user location
		if(animate)
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(LatLngTool.inLatLng(loc)));
		else
			mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLngTool.inLatLng(loc)));
	}
	
	public void showUserView(Location loc, boolean animate) {
		CameraPosition cameraPosition = new CameraPosition.Builder()
			    .target(LatLngTool.inLatLng(loc))      // Sets the center of the map to Mountain View
			    .zoom(15)                   // Sets the zoom
			    .bearing(loc.getBearing())                // Sets the orientation of the camera to east
			    .tilt(45)                   // Sets the tilt of the camera to 30 degrees
			    .build();                   // Creates a CameraPosition from the builder
		
		if(animate)
			mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		else
			mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		
	}

	
	private void animate(Object marker, boolean add){

		String markerClass = marker.getClass().getName();

		if(markerClass.equals(Marker.class.getName()))
			animateMarker((Marker)marker, add);
		if(markerClass.equals(GroundOverlay.class.getName())){
			// special for ground overlay we have two type of marker
			Float zIndex = ((GroundOverlay)marker).getZIndex();
			if(Float.compare(zIndex, 1.0f) == 0)
				animateSpecialGroundOverlay((GroundOverlay)marker, add);
			else
				animateGroundOverlay((GroundOverlay)marker, add);
		}
		if(markerClass.equals(Polyline.class.getName()))
			animatePolyline((Polyline)marker, add);
	}


	public void removeMarker(Object marker, boolean animate){
		
		// this will remove the marker right away
		String markerClass = marker.getClass().getName();

		// this is special trick for our add/remove animation synchronization
		if(markerClass.equals(Marker.class.getName()))
			((Marker)marker).setTitle("Loading..");
		if(markerClass.equals(GroundOverlay.class.getName()))
			((GroundOverlay)marker).setBearing(1.0f);
					
		if(animate){
			// the remove option will remove by itself in the end
			// if we remove directly the user won't see the animation effect
			animate(marker, false);
		}
		else {
			// this will remove the marker right away
			if(markerClass.equals(Marker.class.getName()))
				((Marker)marker).remove();
			if(markerClass.equals(GroundOverlay.class.getName()))
				((GroundOverlay)marker).remove();
			if(markerClass.equals(Polyline.class.getName()))
				((Polyline)marker).remove();
		}
		
		return;
	}
	
	private Object addMarker(Object options, boolean animate){
		String optionClass = options.getClass().getName();
		Object markerHandle = null;
		
		if(optionClass.equals(MarkerOptions.class.getName()))
			markerHandle = mGoogleMap.addMarker((MarkerOptions) options);
		if(optionClass.equals(GroundOverlayOptions.class.getName()))
			markerHandle = mGoogleMap.addGroundOverlay((GroundOverlayOptions) options);
		if(optionClass.equals(PolylineOptions.class.getName()))
			markerHandle = mGoogleMap.addPolyline((PolylineOptions) options);
		
		if(animate)
			animate(markerHandle, true);
		
		return markerHandle;
	}
	

	public void removeMarkers(ArrayList<Object> markers, boolean animate){
		
		for(int i=0; i<markers.size();i++)
			removeMarker(markers.get(i), animate);
	}
	
	public void removeAllMarkers(ArrayList<ArrayList<Object>> markers, boolean animate) {
		
		for(int i=0; i<markers.size();i++)
			for(int j=0; j<markers.get(i).size();j++)
				removeMarker(markers.get(i).get(j), animate);
	}

	public Object addStartPointMarker(LatLng startPoint) {
		// always animate
		MarkerOptions markerOptions = new MarkerOptions()
							.position(startPoint)
							.title("Start Point")
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));


		return addMarker(markerOptions, true);
	}

	public Object addEndPointMarker(LatLng endPoint) {
		// always animate
		MarkerOptions markerOptions = new MarkerOptions()
							.position(endPoint)
							.title("End Point")
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_marker));;

		return addMarker(markerOptions, true);
	}
	
	public void setSnippet(Object marker, String snippet){
		((Marker)marker).setSnippet(snippet);
	}
	
	public boolean hasSnippet(Object marker){

		return(((Marker)marker).getSnippet()!= null);
	}
	
	public ArrayList<Object> addNonDangerousDebrisMarker(Debris debris, boolean animate){
		if(!setUp())
			return null;
		// this is where we decide how many markers we're going to associate
		// with a single debris
		ArrayList<Object> markers = new ArrayList<Object>();

		// marker#1: Marker class
		MarkerOptions markerOptions = new MarkerOptions()
							.position(new LatLng(debris.mLatitude, debris.mLongitude))
							.title("Debris#" + debris.mDebrisId)
							.snippet(debris.mTime)
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
		
		markers.add(addMarker(markerOptions, animate));
		
		// marker#2: GroundOverlayOptions
		GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy)
							.transparency(.40f)
							.image(BitmapDescriptorFactory.fromResource(R.drawable.blue_circle));
	
		markers.add(addMarker(groundOverlayOptions, animate));
		

		return markers;
	}
	
	public ArrayList<Object> addDangerousDebrisMarker(Debris debris, boolean animate){
		if(!setUp())
			return null;
		// this is where we decide how many markers we're going to associate
		// with a single debris
		ArrayList<Object> markers = new ArrayList<Object>();

		// marker#1: Marker class
		MarkerOptions markerOptions = new MarkerOptions()
							.position(new LatLng(debris.mLatitude, debris.mLongitude))
							.title("Debris#" + debris.mDebrisId)
							.snippet(debris.mTime)
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

		
		markers.add(addMarker(markerOptions, animate));
		
		// marker#2: GroundOverlayOptions
		GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy)
							.transparency(.60f)
							.image(BitmapDescriptorFactory.fromResource(R.drawable.red_circle));

		markers.add(addMarker(groundOverlayOptions, animate));
				
		return markers;
	}
	
	
	public ArrayList<Object> addLethalDebrisMarker(Debris debris, boolean animate){
		if(!setUp())
			return null;
		// this is where we decide how many markers we're going to associate
		// with a single debris
		ArrayList<Object> markers = new ArrayList<Object>();

		// marker#1: Marker class
		MarkerOptions markerOptions = new MarkerOptions()
							.position(new LatLng(debris.mLatitude, debris.mLongitude))
							.title("Debris#" + debris.mDebrisId)
							.snippet(debris.mTime)
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

		
		markers.add(addMarker(markerOptions, animate));
		
		// marker#2: GroundOverlayOptions
		GroundOverlayOptions groundOverlayOptions1 = new GroundOverlayOptions()
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy)
							.transparency(.60f)
							.image(BitmapDescriptorFactory.fromResource(R.drawable.red_circle));

		markers.add(addMarker(groundOverlayOptions1, animate));
		
		// marker#3: "special" GroundOverlayOptions
		GroundOverlayOptions groundOverlayOptions2 = new GroundOverlayOptions()
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy)
							.transparency(0.0f)
							.zIndex(1.0f) //this is special marker for our danger signal
							.image(BitmapDescriptorFactory.fromResource(R.drawable.danger_signal));


		// this one "always" animate
		markers.add(addMarker(groundOverlayOptions2, true));
				
		return markers;
	}
	
	public ArrayList<Object> setAsDangerousMarkers(Debris debris, ArrayList<Object> markers){
		
		// we can't change the property we want, so need to replace these markers :(
		removeMarkers(markers, false);

		return addDangerousDebrisMarker(debris, false);
	}
	
	public ArrayList<Object> setAsNonDangerousMarkers(Debris debris, ArrayList<Object> markers){
		
		// we can't change the property we want, so need to replace these markers :(
		removeMarkers(markers, false);

		return addNonDangerousDebrisMarker(debris, false);
	}
	
	public ArrayList<Object> setAsLethalMarkers(Debris debris, ArrayList<Object> markers){
		
		// we can't change the property we want, so need to replace these markers :(
		removeMarkers(markers, false);
				
		// just add in another marker
		return addLethalDebrisMarker(debris, false);
	}
	
	public void setLocation(LatLng location) {
		
		if(mMyLocation == null){
			MarkerOptions markerOptions = new MarkerOptions()
				.position(location)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.my_location));
			mMyLocation = (Marker)addMarker(markerOptions, true);
		}
		else{
			mMyLocation.setPosition(location);
		}		
	}

	
	public Object addPathMarker(List<LatLng> points) {
		// A geodesic polyline that goes around the world.
		PolylineOptions polylineOptions = new PolylineOptions()
											.width(8)
									        .color(0xAA1788FF)
									        .geodesic(true);
		
		for(int i=0; i< points.size();i++){
			polylineOptions.add(points.get(i));
		}

		return addMarker(polylineOptions, true);
	}

	private void animateMarker(Marker m, boolean insert){
		
		final Marker marker = m;
		final boolean add = insert;
		
		final Interpolator interpolator;
		final LatLng targetLatLng = marker.getPosition();
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final GoogleMap gMap = mGoogleMap;

		if(insert)
			interpolator = new DecelerateInterpolator(1.5f);
		else
			interpolator = new AccelerateInterpolator(1.5f);
			
		final long duration = NORMAL_ANIMATION_TIME;

		marker.setVisible(true);

		handler.post(new Runnable() {
			@Override
			public void run() {
				
				if(add){
					// check if this marker has been marked for removal
					if(marker.getTitle().equals("Loading.."))
						return;
				}

				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;
				float t = interpolator.getInterpolation(timeFraction);

				Projection proj = gMap.getProjection();
				Point point;
				if(add){
					// we're going to descend slowly TO the actual value
					point= proj.toScreenLocation(targetLatLng);
					point.offset(0, Math.round(-80*(1-t)));
				}
				else {
					// we're going to ascend slowly FROM the actual value
					point= proj.toScreenLocation(targetLatLng);
					point.offset(0, Math.round(-80*t));
				}
				
				LatLng currentLatLng = proj.fromScreenLocation(point);
				marker.setPosition(currentLatLng);

				if (timeFraction < 1.0) {
					// Post again 20ms later.
					handler.postDelayed(this, 20);
				}
				else{
					if(!add)
						marker.remove();
				}
			}
		});
	}
	
	private void animateSpecialGroundOverlay(GroundOverlay g, boolean insert){

		final GroundOverlay ground = g;
		
		//we're only animate add, not removal
		if(!insert){
			ground.remove();
			return;
		}
		
		final LatLng targetLatLng = g.getPosition();
		
		final GoogleMap gMap = mGoogleMap;
	
		final Interpolator interpolator = new DecelerateInterpolator(2.0f);
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = SIGNAL_ANIMATION_TIME;
		
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {

				// check if this marker has been marked for removal
				if(Float.compare(ground.getBearing(),1.0f)==0)
					return;
		
				// change the center point incase user change the zoom
				Projection proj = gMap.getProjection();
				Point point = proj.toScreenLocation(targetLatLng);
				point.offset(0, -47);
				LatLng currentLatLng = proj.fromScreenLocation(point);
				ground.setPosition(currentLatLng);

				 
				// now change the radius
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration - (float)Math.floor((float)elapsed/duration);
				float t = interpolator.getInterpolation(timeFraction);
	
				float rad = (float)LatLngTool.distance(targetLatLng, currentLatLng) * 2.5f * t;
				
				ground.setDimensions(rad);
				ground.setTransparency(t<0.8f?0.0f:(float)Math.pow(t, 2));
			
				// we will never stop until told so
				handler.postDelayed(this, 20);
			}
		}, 100);
	}
	
	
	private void animateGroundOverlay(GroundOverlay g, boolean insert){

		final GroundOverlay ground = g;
		final boolean add = insert;
		
		final float targetRadius = ground.getWidth();
		
		final Interpolator interpolator;
		if(insert)
			interpolator = new DecelerateInterpolator(1.5f);
		else
			interpolator = new AccelerateInterpolator(1.5f);
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = NORMAL_ANIMATION_TIME;
		
		
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				if(add){
					// check if this marker has been marked for removal
					if(Float.compare(ground.getBearing(),1.0f) == 0)
						return;
				}
				
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;
				
				float t = interpolator.getInterpolation(timeFraction);
				
				float rad;
				if(add)
					rad = targetRadius * t;
				else
					rad = targetRadius * (1-t);
				
				ground.setDimensions(rad);
				
				if (timeFraction <= 1.0) {
					// Post again 10ms later.
					handler.postDelayed(this, 20);
				}
				else{
					if(!add)
						ground.remove();
				}
			}
		}, 100);
	}
	
	private void animatePolyline(Polyline p, boolean insert){
		
		final Polyline polyline = p;
		final boolean add = insert;
		
		final int colorOrig = polyline.getColor();
		
		final Interpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
		final Interpolator accelerateInterpolator = new AccelerateInterpolator(1.5f);
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = NORMAL_ANIMATION_TIME;
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				
				Interpolator interpolator;
				if(add){
					interpolator = decelerateInterpolator;
				}
				else{
					interpolator = accelerateInterpolator;
				}
				
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;
				float t = interpolator.getInterpolation(timeFraction);
						
				float[] prevHSV = new float[3];
	            Color.colorToHSV(colorOrig, prevHSV);
	            polyline.setColor(Color.HSVToColor(Math.round(timeFraction*100), prevHSV));

				if (timeFraction < 1.0 && t < 1.0) {
					// Post again 10ms later.
					handler.postDelayed(this, 20);
				}
				else{
					if(!add)
						polyline.remove();
				}
			}
		});
		
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {
		Log.i("QQQ", "camerachange");
		if(mCameraClient!=null){
			mCameraClient.onCameraChange();
		}
	}


}