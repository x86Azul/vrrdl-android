package edu.depaul.x86azul.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.net.Uri;

import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.GP;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.activities.WebServiceAddressActivity;

public class URIBuilder {
	
	private static final String GoogleMapsApiScheme = "http";
	private static final String GoogleMapsApiBaseURI = "maps.googleapis.com";
	private static final String GoogleDirectionsQueryPath = "/maps/api/directions/json";
	private static final String GoogleGeocodeQueryPath = "/maps/api/geocode/json";
	
	
	public static String toGoogleDirURI(MyLatLng orig, MyLatLng dest){
		
		String uri = GoogleMapsApiScheme + "://" +
					GoogleMapsApiBaseURI +
					GoogleDirectionsQueryPath + "?" +
					"origin" + "=" + orig.toSimpleString() + "&" + 
					"destination" + "=" + dest.toSimpleString() + "&" +
					"sensor" + "=" + "true";
					//"units" + "=" + "metric";
		
		// if(uri != null)
	    // DialogHelper.showDebugInfo(uri);
		
		return uri;
	}
	
	public static String toGoogleGeoURI(MyLatLng point){
		
		String uri = GoogleMapsApiScheme + "://" +
					GoogleMapsApiBaseURI +
					GoogleGeocodeQueryPath + "?" +
					"latlng" + "=" + point.toSimpleString() + "&" + 
					"sensor" + "=" + "true" ;
		
		//if(uri != null)
		//	DialogHelper.showDebugInfo(uri);
		
		return uri;
	}
	
	public static String toTestPutURI(Debris debris) {
		
		String uri = GP.webServiceURI + "/debris";
		
		//if(uri != null)
		    //DialogHelper.showDebugInfo("toTestPutURI=" + uri);
		
		return uri;
	}
	
	public static String toTestDeleteURI(Debris debris) {
			
			String uri = GP.webServiceURI + "/debris/geohash/" + debris.mGeohash;
			
			//if(uri != null)
			    //DialogHelper.showDebugInfo("toTestPutURI=" + uri);
			
			return uri;
		}
	
	public static String toTestGetURI(MyLatLng location, double pollRadius) {
		
		String uri = null;
		
		if(location != null && pollRadius != 0){
		uri = GP.webServiceURI + 
			"/proximity" +
			"/latitude/" + location.latitude +
			"/longitude/" + location.longitude +
			"/radius/" + pollRadius;
		}
				
		//if(uri != null)
		    //DialogHelper.showDebugInfo("toTestGetURI=" + uri);
		
		return uri;
	}
	

}