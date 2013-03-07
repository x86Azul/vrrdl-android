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
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.activities.WebServiceAddressActivity;

public class URIBuilder {
	
	private static final String GoogleMapsApiScheme = "http";
	private static final String GoogleMapsApiBaseURI = "maps.googleapis.com";
	private static final String GoogleDirectionsQueryPath = "/maps/api/directions/json";
	private static final String GoogleGeocodeQueryPath = "/maps/api/geocode/json";
	
	private static String TestWebBaseURI = "httpbin.org";
	
	public static String toGoogleDirURI(MyLatLng orig, MyLatLng dest){
		
		String uri = GoogleMapsApiScheme + "://" +
					GoogleMapsApiBaseURI +
					GoogleDirectionsQueryPath + "?" +
					"origin" + "=" + orig.toSimpleString() + "&" + 
					"destination" + "=" + dest.toSimpleString() + "&" +
					"sensor" + "=" + "true" + "&" + 
					"units" + "=" + "metric";
		
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
	
	public static String toTestPutURI() {
		
		String uri = TestWebBaseURI + "/debris";
		
		if(uri != null)
		    DialogHelper.showDebugInfo("toTestPutURI=" + uri);
		
		return uri;
	}
	
	public static String toTestGetURI(MyLatLng location, double pollRadius) {
		
		String uri = null;
		
		if(location != null && pollRadius != 0){
		uri = TestWebBaseURI + 
			"/proximity" +
			"/latitude/" + location.latitude +
			"/longitude/" + location.longitude +
			"/radius/" + pollRadius;
		}
				
		if(uri != null)
		    DialogHelper.showDebugInfo("toTestGetURI=" + uri);
		
		return uri;
	}
	
	public static void setWebBaseURI (String webAddress){
		TestWebBaseURI = webAddress;
	}
	
	public static String getWebBaseURI (){
		return TestWebBaseURI;
	}

}