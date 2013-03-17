package edu.depaul.x86azul;

import org.json.simple.JSONObject;

import android.location.Location;

public class MyLatLng {
	
	public double latitude;
	public double longitude;
	
	static final double earthRadius = 6371000; //in meter
	
	public MyLatLng(double latitude, double longitude){
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public MyLatLng(JSONObject obj){
		this.latitude = (Double)obj.get("lat");
		this.longitude = (Double)obj.get("lng");
	}

	public static double distance(double lat1, double lng1, double lat2, double lng2) {
		
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double sindLat = Math.sin(dLat / 2);
		double sindLng = Math.sin(dLng / 2);
		double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
				* Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = earthRadius * c;

		return dist;
	}

	public static double distance(MyLatLng point1, MyLatLng point2){
		return distance(point1.latitude, point1.longitude,
				point2.latitude, point2.longitude);
	}

	public static double distance(Location loc1, Location loc2){
		return distance(loc1.getLatitude(), loc1.getLongitude(),
				loc2.getLatitude(), loc2.getLongitude());
	}

	public static double distance(Location loc, MyLatLng point) {
		return distance(loc.getLatitude(), loc.getLongitude(),
				point.latitude, point.longitude);
	}

	public static MyLatLng getLatLngFraction(MyLatLng start, MyLatLng end, 
			double numerator, double denominator){
		double lat = start.latitude + (end.latitude - start.latitude) * numerator/denominator;
		double lng = start.longitude + (end.longitude - start.longitude) * numerator/denominator;
		return new MyLatLng(lat, lng);
	}

	public static MyLatLng inLatLng(Location loc){
		return new MyLatLng(loc.getLatitude(), loc.getLongitude());
	}
	
	public String toSimpleString() {
		return latitude + "," + longitude;
	}

	public static double bearing(MyLatLng from, MyLatLng to) {
		double deltaLong = Math.toRadians(to.longitude - from.longitude);

		double lat1 = Math.toRadians(from.latitude);
		double lat2 = Math.toRadians(to.latitude);

		double y = Math.sin(deltaLong) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLong);
		double result = Math.toDegrees(Math.atan2(y, x));
		return (result + 360.0) % 360.0;
	}
	
	public static double bearing(Location fromLoc, Debris toObject) {
		return bearing(new MyLatLng(fromLoc.getLatitude(), fromLoc.getLongitude()), toObject.getLatLng());
	}
	
	/*
	 * distance in meter, bearing in degrees
	 */
	public static MyLatLng point(MyLatLng center, double distance, double bearing){

		double lat1 = Math.toRadians(center.latitude);
		double lng1 = Math.toRadians(center.longitude);
		double brng = Math.toRadians(bearing);
	
		double distFraction = distance/earthRadius;
	
		double lat2 = Math.asin( Math.sin(lat1)*Math.cos(distFraction) +
				Math.cos(lat1)*Math.sin(distFraction)*Math.cos(brng));
	
		double lng2 = lng1 + Math.atan2(Math.sin(brng)*Math.sin(distFraction)*Math.cos(lat1),
				Math.cos(distFraction)-Math.sin(lat1)*Math.sin(lat2));
	
		lat2 = Math.toDegrees(lat2);
		lng2 = Math.toDegrees(lng2);
	
		return new MyLatLng(lat2, lng2);
	}
	
	public static MyLatLng point(Location center, double distance, double bearing){
		return point(inLatLng(center), distance, bearing);
	}
	
	@Override
	public boolean equals(Object obj){
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

        MyLatLng comp = (MyLatLng)obj;
		return (latitude == comp.latitude && 
				longitude == comp.longitude);
	}
	
} 