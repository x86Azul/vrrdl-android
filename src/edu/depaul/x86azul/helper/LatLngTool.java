package edu.depaul.x86azul.helper;

import java.util.List;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

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

	static double distance(Location loc1, Location loc2){
		return distance(loc1.getLatitude(), loc1.getLongitude(),
				loc2.getLatitude(), loc2.getLongitude());
	}

	public static Double distance(Location loc, LatLng point) {
		return distance(loc.getLatitude(), loc.getLongitude(),
				point.latitude, point.longitude);
	}

	public static LatLng getLatLngFraction(LatLng start, LatLng end, 
			double numerator, double denominator){
		double lat = start.latitude + (end.latitude - start.latitude) * numerator/denominator;
		double lng = start.longitude + (end.longitude - start.longitude) * numerator/denominator;
		return new LatLng(lat, lng);
	}

	public int analyzeNextPointIdx(LatLng loc, int nextIdx, double step, List<LatLng> points){
		if (nextIdx >= points.size())
			return 0;

		double stepTaken = 0;

		while(nextIdx >= points.size()){
			double toNextPointStep = distance(loc, points.get(nextIdx));
			if(stepTaken + toNextPointStep > step){
				// we can step safely here
				loc = getLatLngFraction(loc, points.get(nextIdx), 
						step, toNextPointStep);
				// no change in nextIdx value
				break;
			}
			else {
				
				// take the step
				loc = points.get(nextIdx);
				
				// and increase the index
				stepTaken += toNextPointStep;
				nextIdx++;
			}

		}
		
		if(nextIdx >= points.size())
			nextIdx = 0;

		return nextIdx;

	}

} 