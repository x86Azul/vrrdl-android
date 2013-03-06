package edu.depaul.x86azul.map;

import java.util.ArrayList;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.Debris.DangerFlag;
import edu.depaul.x86azul.helper.DialogHelper;

// this is the marker wrapper for our map
public class MarkerWrapper {

	private final int NORMAL_ANIMATION_TIME = 300; // in ms
	private final int SIGNAL_ANIMATION_TIME = 1500; // in ms
	
	
	public enum Type {
		PIN, POLYLINE, OVERLAY, SPECIAL_SIGNAL, DEBRIS
	}
	
	// a marker can have a group of markers (for our case the DEBRIS type)
	private ArrayList<MarkerWrapper> mMarkers;
	
	private Type mType;

	// this is specific to non polyline type
	private MyLatLng mCoordinate;

	// this is specific to polyline
	private ArrayList<MyLatLng> mCoordinates;

	// this is specific to pin
	private String mTitle;
	private String mSnippet;

	// this is specific to debris type
	private DangerFlag mDangerFlag;

	// this is specific to overlay and pin
	private BitmapDescriptor mImage;

	private int mColor = 0;

	// this is specific to 
	private float mWidth;

	private float mAnchorX;
	private float mAnchorY;

	private float mTransparency;

	// these are google map marker type
	private GoogleMap gMap;
	private Marker gMarker;
	private GroundOverlay gGroundOverlay;
	private Polyline gPolyline;

	public MarkerWrapper(Type type){

		mType = type;

		if(mType == Type.DEBRIS){
			mMarkers = new ArrayList<MarkerWrapper>();
		}
	}

	public Type getType(){
		return mType;
	}

	public MarkerWrapper dangerFlag(DangerFlag dangerFlag){

		if(dangerFlag == mDangerFlag)
			return this;
		
		if(mType == Type.DEBRIS){
			if(mMarkers.size() != 0){
				
				// DialogHelper.showDebugInfo("switch from " + mDangerFlag.name() + " to " + dangerFlag.name());
				// there's some changes here, since this is a high level type 
				// we're going to perform the changes internally here here
				for(int i=0; i < mMarkers.size(); i++){
					MarkerWrapper marker = mMarkers.get(i);
					if(dangerFlag == DangerFlag.NON_DANGER){
						if(marker.getType() == Type.PIN)
							marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
						if(marker.getType() == Type.OVERLAY)
							marker.icon(R.drawable.blue_circle);
						if(marker.getType() == Type.SPECIAL_SIGNAL)
							marker.removeFromMap(false);
					} else if(dangerFlag == DangerFlag.DANGER){
						if(marker.getType() == Type.PIN)
							marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
						if(marker.getType() == Type.OVERLAY)
							marker.icon(R.drawable.red_circle);
						if(marker.getType() == Type.SPECIAL_SIGNAL)
							marker.removeFromMap(false);
					} else if(dangerFlag == DangerFlag.LETHAL){
						if(marker.getType() == Type.PIN)
							marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
						if(marker.getType() == Type.OVERLAY)
							marker.icon(R.drawable.red_circle);
						if(marker.getType() == Type.SPECIAL_SIGNAL)
							marker.insertToMap(gMap, true);
					}
				}
			} 
		}
		
		mDangerFlag = dangerFlag;

		return this;
	}
	
	public MarkerWrapper icon(BitmapDescriptor image){
		// icon is interchangeable after set, so we need to exchange the handle
		// we do it with NO animation so user won't know

		// always set the new image first
		mImage = image;
		
		if(mType == Type.PIN){
			if(gMarker != null){
				removeFromMap(false);
				insertToMap(gMap, false);
			}
		}
		if(mType == Type.OVERLAY){
			if(gGroundOverlay != null){
				removeFromMap(false);
				insertToMap(gMap, false);
			}
		}
		
		return this;
	}

	public MarkerWrapper icon(int image){
				
		return icon(BitmapDescriptorFactory.fromResource(image));
	}
	
	

	public MarkerWrapper coordinate(MyLatLng location){
			
		mCoordinate = location;
		return this;
	}
	
	public MyLatLng getCoordinate(){
		return mCoordinate;
	}
	
	public double getLatitude(){
		if(mCoordinate != null)
			return mCoordinate.latitude;
		
		return 0;
	}
	
	public double getLongitude(){
		if(mCoordinate != null)
			return mCoordinate.longitude;
		
		return 0;
	}
	
	public MarkerWrapper coordinates(ArrayList<MyLatLng> points){

		mCoordinates = points;
		return this;
	}

	public MarkerWrapper transparency(float transparency){

		mTransparency = transparency;
		return this;
	}

	public MarkerWrapper color(int color){
		mColor = color;
		return this;
	}

	public MarkerWrapper anchor(float x, float y){

		mAnchorX = x;
		mAnchorY = y;
		return this;
	}

	public MarkerWrapper title(String title){
		
		mTitle = title;
		
		if(mType == Type.PIN)
			if(gMarker != null)
				gMarker.setTitle(title);

		return this;
	}
	
	public MarkerWrapper width(float width){
		mWidth = width;
		return this;
	}

	public MarkerWrapper snippet(String snippet){
		mSnippet = snippet;
		
		if(mType == Type.PIN) {
			if(gMarker != null)
				gMarker.setSnippet(snippet);

		}
		return this;
	}

	//PIN, POLYLINE, OVERLAY, DEBRIS
	
	public void insertToMap(GoogleMap map, boolean animate){
		if(map == null && gMap == null)
			return;

		gMap = map;

		if(mType == Type.DEBRIS){
			
			MarkerWrapper marker1;
			
			marker1 = new MarkerWrapper(Type.PIN)
							.coordinate(mCoordinate)
							.title(mTitle)
							.snippet(mSnippet);
			
			if(mDangerFlag == DangerFlag.NON_DANGER){
				marker1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
			} else if (mDangerFlag == DangerFlag.DANGER){
				marker1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
			} else if (mDangerFlag == DangerFlag.LETHAL){
				marker1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
			}
				
			marker1.insertToMap(gMap, animate);
			
			mMarkers.add(marker1);
			
			MarkerWrapper marker2;
			marker2 = new MarkerWrapper(Type.OVERLAY)
								.anchor(0.5f, 0.5f)
								.coordinate(mCoordinate)
								.width(mWidth)
								.transparency(.60f);

			if(mDangerFlag == DangerFlag.NON_DANGER){
				marker2.icon(R.drawable.blue_circle);
			} else if (mDangerFlag == DangerFlag.DANGER){
				marker2.icon(R.drawable.red_circle);
			} else if (mDangerFlag == DangerFlag.LETHAL){
				marker2.icon(R.drawable.red_circle);
			}
			
			marker2.insertToMap(gMap, animate);
			
			mMarkers.add(marker2);
			
			MarkerWrapper marker3;
			
			marker3 = new MarkerWrapper(Type.SPECIAL_SIGNAL)
							.anchor(0.5f, 0.5f)
							.coordinate(mCoordinate)
							.transparency(0.0f)
							.icon(R.drawable.danger_signal);
							
			if (mDangerFlag == DangerFlag.LETHAL){
				marker3.insertToMap(gMap, animate);
			}
			
			mMarkers.add(marker3);
		}

		if(mType == Type.PIN){
			MarkerOptions markerOptions = new MarkerOptions()
										.position(toMap(mCoordinate))
										.title(mTitle)
										.snippet(mSnippet)
										.icon(mImage);
			
			gMarker = gMap.addMarker(markerOptions);
			
			/*
	        String str = (mImage == R.drawable.ic_map_blue_marker)? "non-danger":
	        				(mImage == R.drawable.ic_map_red_marker)? "danger" :
	        					(mImage == R.drawable.ic_map_yellow_marker)? "lethal" : "none";
	        
			DialogHelper.showDebugInfo("pin add =" + str);
			*/
			
			if(animate)
				animateMarker(true);
		}

		if(mType == Type.POLYLINE){
			// A geodesic polyline that goes around the world.
			PolylineOptions polylineOptions = new PolylineOptions()
													.width(mWidth)
													.color(mColor)
													.geodesic(true);

			for(int i=0; i< mCoordinates.size();i++){
				polylineOptions.add(toMap(mCoordinates.get(i)));
			}

			gPolyline = gMap.addPolyline(polylineOptions);
			if(animate)
				animatePolyline(gPolyline, true);
		}

		if(mType == Type.OVERLAY){

			GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
												.anchor(mAnchorX, mAnchorY)
												.position(toMap(mCoordinate), mWidth)
												.transparency(mTransparency)
												.image(mImage);

			gGroundOverlay = gMap.addGroundOverlay(groundOverlayOptions);
			if(animate)
				animateGroundOverlay(gGroundOverlay, true);
		}

		if(mType == Type.SPECIAL_SIGNAL){

			// special signal is also a ground overlay but with different preferential
			GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions()
													.anchor(mAnchorX, mAnchorY)
													.position(toMap(mCoordinate), mWidth)
													.transparency(mTransparency)
													.zIndex(1.0f)
													.image(mImage);

			gGroundOverlay = gMap.addGroundOverlay(groundOverlayOptions);

			//if(animate)
			// always animate
			animateSpecialGroundOverlay(gGroundOverlay, true);
		}
		
		
	}

	public void removeFromMap(boolean animate){
		
		if(mType == Type.DEBRIS){
			for(int i=0; i<mMarkers.size() ; i++){
				mMarkers.get(i).removeFromMap(animate);
			}
		}

		if(mType == Type.PIN){

			if(gMarker == null)
				return;

			// this is a trick incase we're still in the middle of inserting
			gMarker.setTitle("Loading..");

			if(animate)
				animateMarker(false);
			else
				gMarker.remove();
			
			// DialogHelper.showDebugInfo("pin remove =" + mImage);
		}
		
		if(mType == Type.OVERLAY){

			if(gGroundOverlay == null)
				return;
			
			// this is a trick incase we're still in the middle of inserting
			gGroundOverlay.setBearing(1.0f);

			if(animate)
				animateGroundOverlay(gGroundOverlay, false);
			else
				gGroundOverlay.remove();
		}

		if(mType == Type.POLYLINE){

			if(gPolyline == null)
				return;

			if(animate)
				animatePolyline(gPolyline, false);
			else
				gPolyline.remove();
		}

		if(mType == Type.SPECIAL_SIGNAL){

			if(gGroundOverlay == null)
				return;

			// always animate
			animateSpecialGroundOverlay(gGroundOverlay, false);

		}
	}

	private void animateSpecialGroundOverlay(GroundOverlay g, boolean insert){

		final GroundOverlay ground = g;

		//we're only animate add, not removal
		if(!insert){
			ground.remove();
			return;
		}

		final LatLng targetLatLng = g.getPosition();

		final GoogleMap igMap = gMap;

		final Interpolator interpolator = new DecelerateInterpolator(2.0f);

		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = SIGNAL_ANIMATION_TIME;

		handler.post(new Runnable() {
			@Override
			public void run() {

				// check if this marker has been marked for removal
				if(Float.compare(ground.getBearing(),1.0f)==0)
					return;

				// change the center point incase user change the zoom
				Projection proj = igMap.getProjection();
				Point point = proj.toScreenLocation(targetLatLng);
				point.offset(0, -47);
				LatLng currentLatLng = proj.fromScreenLocation(point);
				ground.setPosition(currentLatLng);


				// now change the radius
				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration - (float)Math.floor((float)elapsed/duration);
				float t = interpolator.getInterpolation(timeFraction);

				float rad = (float)MyLatLng.distance(toLocal(targetLatLng), toLocal(currentLatLng)) * 2.5f * t;

				ground.setDimensions(rad);
				ground.setTransparency(t<0.8f?0.0f:(float)Math.pow(t, 2));

				// we will never stop until told so
				handler.postDelayed(this, 20);
			}
		});
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

				if (timeFraction <= 1.0) {
					ground.setDimensions(rad);
					// Post again 20ms later.
					handler.postDelayed(this, 20);
				}
				else{
					if(add){
						ground.setDimensions(targetRadius);
					} else {
						ground.remove();
					}
				}
			}
		}, 100);
	}

	private void animatePolyline(Polyline p, boolean insert){

		final Polyline polyline = p;
		final boolean add = insert;

		final int colorOrig = polyline.getColor();

		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final long duration = NORMAL_ANIMATION_TIME;

		handler.post(new Runnable() {
			@Override
			public void run() {

				long elapsed = SystemClock.uptimeMillis() - start;
				float timeFraction = (float)elapsed/duration;

				float[] prevHSV = new float[3];
				Color.colorToHSV(colorOrig, prevHSV);
				polyline.setColor(Color.HSVToColor(Math.round(timeFraction*100), prevHSV));

				if (timeFraction < 1.0) {
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

	private void animateMarker(boolean insert){

		final Marker marker = gMarker;
		final boolean add = insert;

		final Interpolator interpolator;
		final LatLng targetLatLng = marker.getPosition();

		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();

		final GoogleMap igMap = gMap;

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

				Projection proj = igMap.getProjection();
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

				if (timeFraction < 1.0 && currentLatLng != null) {
					marker.setPosition(currentLatLng);
					// Post again 20ms later.
					handler.postDelayed(this, 20);
				}
				else{
					// end of animation
					if(add){
						marker.setPosition(targetLatLng);
					}else {
						marker.remove();
					}
				}
			}
		});
	}
	
	private MyLatLng toLocal(LatLng latlng){
		return new MyLatLng(latlng.latitude, latlng.longitude);
	}
	
	private LatLng toMap(MyLatLng latlng){
		return new LatLng(latlng.latitude, latlng.longitude);
	}
}