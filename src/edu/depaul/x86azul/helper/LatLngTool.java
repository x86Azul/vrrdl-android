package edu.depaul.x86azul.helper;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import edu.depaul.x86azul.Debris;

public class LatLngTool {

	public static double distance(double lat1, double lng1, double lat2, double lng2) {
		final double earthRadius = 6371000; //in meter
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

	public static double distance(LatLng point1, LatLng point2){
		return distance(point1.latitude, point1.longitude,
				point2.latitude, point2.longitude);
	}

	public static double distance(Location loc1, Location loc2){
		return distance(loc1.getLatitude(), loc1.getLongitude(),
				loc2.getLatitude(), loc2.getLongitude());
	}

	public static double distance(Location loc, LatLng point) {
		return distance(loc.getLatitude(), loc.getLongitude(),
				point.latitude, point.longitude);
	}

	public static LatLng getLatLngFraction(LatLng start, LatLng end, 
			double numerator, double denominator){
		double lat = start.latitude + (end.latitude - start.latitude) * numerator/denominator;
		double lng = start.longitude + (end.longitude - start.longitude) * numerator/denominator;
		return new LatLng(lat, lng);
	}

	public static LatLng inLatLng(Location loc){
		return new LatLng(loc.getLatitude(), loc.getLongitude());
	}

	public static double bearing(LatLng from, LatLng to) {
		double deltaLong = Math.toRadians(to.longitude - from.longitude);

		double lat1 = Math.toRadians(from.latitude);
		double lat2 = Math.toRadians(to.latitude);

		double y = Math.sin(deltaLong) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLong);
		double result = Math.toDegrees(Math.atan2(y, x));
		return (result + 360.0) % 360.0;
	}
	
	public static double bearing(Location fromLoc, Debris toObject) {
		return bearing(new LatLng(fromLoc.getLatitude(), fromLoc.getLongitude()), toObject.getLatLng());
	}


} 