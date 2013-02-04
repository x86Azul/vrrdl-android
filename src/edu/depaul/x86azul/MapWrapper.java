package edu.depaul.x86azul;

import java.util.ArrayList;

import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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


/**
 * a wrapper class for map service
 */
public class MapWrapper implements OnMapClickListener, 
OnMarkerClickListener, OnInfoWindowClickListener {

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
		return true;
	}

	public void showLocation(LatLng latLng){
		if(!setUp())
			return;
		// set the camera to user location
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				latLng, 10));
	}

	public ArrayList<Object> addMarker(Debris debris, boolean dangerous){
		if(!setUp())
			return null;
		
		ArrayList<Object> markers = new ArrayList<Object>();

		MarkerOptions markerOptions = new MarkerOptions()
							.position(new LatLng(debris.mLatitude, debris.mLongitude))
							.title("Debris#" + debris.mDebrisId)
							.snippet("speed=" + debris.mSpeed + ", acc=" + debris.mAccuracy);
		
		GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
							.image(BitmapDescriptorFactory
							.fromResource(R.drawable.blue_circle))
							.anchor(0.5f, 0.5f)
							.position(new LatLng(debris.mLatitude, debris.mLongitude), debris.mAccuracy);
		
		if(dangerous){
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
			groundOverlayOptions.transparency(.40f);
		}
		else{
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
			groundOverlayOptions.transparency(.80f);
		}
		
		markers.add(mGoogleMap.addMarker(markerOptions));
		markers.add(mGoogleMap.addGroundOverlay(groundOverlayOptions));
		return markers;
	}
	
	public ArrayList<Object> setAsDangerousMarkers(Debris debris, ArrayList<Object> markers){
		Marker marker = (Marker)markers.get(0);
		GroundOverlay groundOverlay = (GroundOverlay)markers.get(1);
		
		// we can't change the property we want, so just create new markers :(
		if(marker!= null)
			marker.remove();
		
		if(groundOverlay != null)
			groundOverlay.remove();
		
		return addMarker(debris, true);
	}
	
	public ArrayList<Object> setAsNonDangerousMarkers(Debris debris, ArrayList<Object> markers){
		Marker marker = (Marker)markers.get(0);
		GroundOverlay groundOverlay = (GroundOverlay)markers.get(1);
		
		// we can't change the property we want, so just create new markers :(
		if(marker!= null)
			marker.remove();
		
		if(groundOverlay != null)
			groundOverlay.remove();
		
		return addMarker(debris, false);
	}
}