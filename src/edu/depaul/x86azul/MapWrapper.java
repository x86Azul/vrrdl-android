package edu.depaul.x86azul;

import java.util.ArrayList;

import java.util.List;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


/**
 * a wrapper class for map service
 */
public class MapWrapper implements OnMapClickListener, 
OnMarkerClickListener, OnInfoWindowClickListener {
	
	private final int ANIMATION_TIME = 400; // in ms

	private GoogleMap mGoogleMap;
	private Client mClient;

	// the client need to implement this
	// and it needs to be from FragmentActivity class
	public interface Client{
		public void onMapClick(LatLng latLng);
		public void onInfoWindowClick(Marker marker);
		public boolean onMarkerClick(Marker marker);
	}

	public MapWrapper(FragmentActivity activity){

		// this is to make sure the activity implements the callback
		mClient = (Client)activity;
	}
	
	public void setClient(Client client) {
		mClient = client;
	}


	@Override
	public void onInfoWindowClick(Marker marker) {
		mClient.onInfoWindowClick(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return mClient.onMarkerClick(marker);
	}

	@Override
	public void onMapClick(LatLng latLng) {
		mClient.onMapClick(latLng);

	}

	public boolean setUp() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mGoogleMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mGoogleMap = ((SupportMapFragment) ((FragmentActivity)mClient).getSupportFragmentManager().
					findFragmentById(R.id.map)).getMap();

			// Check if we were successful in obtaining the map.
			if (mGoogleMap != null) {
				return initializeMap();
			} 
			else {
				return false;
			}
		}
		return true;
	}
	
	private boolean initializeMap() {

		mGoogleMap.setOnMapClickListener(this);
		//mGoogleMap.setOnMarkerClickListener(this);
		//mGoogleMap.setOnInfoWindowClickListener(this);

		mGoogleMap.setMyLocationEnabled(true);  
		mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
		mGoogleMap.getUiSettings().setCompassEnabled(false);
		mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
		
		mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(10));
		
		return true;
	}

	public void showLocation(LatLng latLng, boolean animate){
		if(!setUp())
			return;
		// set the camera to user location
		if(animate)
			mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
		else
			mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
	}
	
	public void setLocationSource(LocationSource ls){
		mGoogleMap.setLocationSource(ls);
	}
	
	private void animate(Object marker, boolean add){

		String markerClass = marker.getClass().getName();

		if(markerClass.equals(Marker.class.getName()))
			animateMarker((Marker)marker, add);
		if(markerClass.equals(GroundOverlay.class.getName()))
			animateGroundOverlay((GroundOverlay)marker, add);
		if(markerClass.equals(Polyline.class.getName()))
			animatePolyline((Polyline)marker, add);
	}


	public void removeMarker(Object marker, boolean animate){
		
		if(animate){
			// the remove option will remove by itself in the end
			// if we remove directly the user won't see the animation effect
			animate(marker, false);
		}
		else {
			// this will remove the marker right away
			String markerClass = marker.getClass().getName();

			if(markerClass.equals(Marker.class.getName()))
				((Marker)marker).remove();
			if(markerClass.equals(GroundOverlay.class.getName()))
				((GroundOverlay)marker).remove();
			if(markerClass.equals(Polyline.class.getName()))
				((Polyline)marker).remove();
		}
		return ;
	}
	
	public Object addMarker(Object options, boolean animate){
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
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker));;


		return addMarker(markerOptions, true);
	}

	public Object addEndPointMarker(LatLng endPoint) {
		// always animate
		MarkerOptions markerOptions = new MarkerOptions()
							.position(endPoint)
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_marker));;

		return addMarker(markerOptions, true);
	}
	
	public ArrayList<Object> addDebrisMarker(Debris debris, boolean dangerous, boolean animate){
		if(!setUp())
			return null;
		// this is where we decide how many markers we're going to associate
		// with a single debris
		ArrayList<Object> markers = new ArrayList<Object>();

		// marker#1: Marker class
		MarkerOptions markerOptions = new MarkerOptions()
							.position(new LatLng(debris.mLatitude, debris.mLongitude))
							.title("Debris#" + debris.mDebrisId)
							.snippet(debris.mTime + ", acc=" + debris.mAccuracy);
		
		if(dangerous)
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		else
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
		
		markers.add(addMarker(markerOptions, animate));
		
		// marker#2: GroundOverlayOptions
		GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy);
		
		if(dangerous){
			groundOverlayOptions
					.transparency(.60f)
					.image(BitmapDescriptorFactory.fromResource(R.drawable.red_circle));
		}
		else{
			groundOverlayOptions
					.transparency(.40f)
					.image(BitmapDescriptorFactory.fromResource(R.drawable.blue_circle));
		}
	
		markers.add(addMarker(groundOverlayOptions, animate));
				
		return markers;
	}
	
	public ArrayList<Object> setAsDangerousMarkers(Debris debris, ArrayList<Object> markers){
		
		// we can't change the property we want, so need to replace these markers :(
		removeMarkers(markers, false);

		return addDebrisMarker(debris, true, false);
	}
	
	public ArrayList<Object> setAsNonDangerousMarkers(Debris debris, ArrayList<Object> markers){
		
		// we can't change the property we want, so need to replace these markers :(
		removeMarkers(markers, false);

		return addDebrisMarker(debris, false, false);
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
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();
		
		Projection proj = mGoogleMap.getProjection();
		Point startPoint = proj.toScreenLocation(marker.getPosition());
		startPoint.offset(0, -80);
		
		final LatLng fromLatLng = proj.fromScreenLocation(startPoint);
		final LatLng toLatLng = marker.getPosition();
		
		final LatLng startLatLngAdd = fromLatLng, finalLatLngAdd = toLatLng;
		final LatLng startLatLngDel = toLatLng, finalLatLngDel = fromLatLng;
		
		final Interpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
		final Interpolator accelerateInterpolator = new AccelerateInterpolator(1.5f);
		
		final long duration = ANIMATION_TIME;


		marker.setPosition(add?startLatLngAdd:toLatLng);
		marker.setVisible(true);

		handler.post(new Runnable() {
			@Override
			public void run() {
				
				LatLng startLatLng, finalLatLng;
				Interpolator interpolator;
				if(add){
					startLatLng = startLatLngAdd;
					finalLatLng = finalLatLngAdd;
					interpolator = decelerateInterpolator;
				}
				else{
					startLatLng = startLatLngDel;
					finalLatLng = finalLatLngDel;
					interpolator = accelerateInterpolator;
				}
				
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;
				float t = interpolator.getInterpolation(timeFraction);
				double lng = t * finalLatLng.longitude + (1 - t) * startLatLng.longitude;
				double lat = t * finalLatLng.latitude + (1 - t) * startLatLng.latitude;
				
				marker.setPosition(new LatLng(lat, lng));
				
				if (timeFraction < 1.0 && t < 1.0) {
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
	
	private void animateGroundOverlay(GroundOverlay g, boolean insert){

		final GroundOverlay ground = g;
		final boolean add = insert;
		
		final Interpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
		final Interpolator accelerateInterpolator = new AccelerateInterpolator(1.5f);
		
		final float startRadiusAdd = 0, finalRadiusAdd = ground.getWidth();
		final float startRadiusDel = ground.getWidth(), finalRadiusDel = 0;
			
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = ANIMATION_TIME;
		
		// now make it visible
		ground.setDimensions(add?startRadiusAdd:startRadiusDel);
		ground.setVisible(true);
		
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				float startRadius, finalRadius;
				Interpolator interpolator;
				if(add){
					startRadius = startRadiusAdd;
					finalRadius = finalRadiusAdd;
					interpolator = decelerateInterpolator;
				}
				else{
					startRadius = startRadiusDel;
					finalRadius = finalRadiusDel;
					interpolator = accelerateInterpolator;
				}
				
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;
				float t = interpolator.getInterpolation(timeFraction);
				float rad = t * finalRadius + (1 - t) * startRadius;
				
				ground.setDimensions(rad);
				
				if (timeFraction < 1.0 && t < 1.0) {
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

		final long duration = ANIMATION_TIME;
		
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
}