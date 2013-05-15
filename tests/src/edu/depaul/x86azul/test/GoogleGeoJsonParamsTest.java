package edu.depaul.x86azul.test;

import junit.framework.Assert;
import android.test.AndroidTestCase;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.helper.GoogleGeoJsonParams;
import edu.depaul.x86azul.helper.URIBuilder;

public class GoogleGeoJsonParamsTest extends AndroidTestCase {
	
	private String GoogleGeoJsonResponse = "{\"results\":[{\"address_components\":[{\"long_name\":\"609\",\"short_name\":\"609\",\"types\":[\"street_number\"]},{\"long_name\":\"Fairbanks Court\",\"short_name\":\"Fairbanks Ct\",\"types\":[\"route\"]},{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"locality\",\"political\"]},{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"administrative_area_level_3\",\"political\"]},{\"long_name\":\"Cook\",\"short_name\":\"Cook\",\"types\":[\"administrative_area_level_2\",\"political\"]},{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]},{\"long_name\":\"60194\",\"short_name\":\"60194\",\"types\":[\"postal_code\"]}],\"formatted_address\":\"609 Fairbanks Court, Schaumburg, IL 60194, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.03367370,\"lng\":-88.06434929999999},\"southwest\":{\"lat\":42.03345740,\"lng\":-88.06464579999999}},\"location\":{\"lat\":42.03367490,\"lng\":-88.06458250},\"location_type\":\"RANGE_INTERPOLATED\",\"viewport\":{\"northeast\":{\"lat\":42.03491453029150,\"lng\":-88.06314856970850},\"southwest\":{\"lat\":42.03221656970850,\"lng\":-88.06584653029151}}},\"postcode_localities\":[],\"types\":[\"street_address\"]},{\"address_components\":[{\"long_name\":\"60194\",\"short_name\":\"60194\",\"types\":[\"postal_code\"]},{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"locality\",\"political\"]},{\"long_name\":\"Cook\",\"short_name\":\"Cook\",\"types\":[\"administrative_area_level_2\",\"political\"]},{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"Schaumburg, IL 60194, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.04960010,\"lng\":-88.0602030},\"southwest\":{\"lat\":42.02223480,\"lng\":-88.15442890}},\"location\":{\"lat\":42.03054340,\"lng\":-88.11205310},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":42.04960010,\"lng\":-88.0602030},\"southwest\":{\"lat\":42.02223480,\"lng\":-88.15442890}}},\"postcode_localities\":[],\"types\":[\"postal_code\"]},{\"address_components\":[{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"administrative_area_level_3\",\"political\"]},{\"long_name\":\"Cook\",\"short_name\":\"Cook\",\"types\":[\"administrative_area_level_2\",\"political\"]},{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"Schaumburg, IL, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.0673260,\"lng\":-88.03027109999999},\"southwest\":{\"lat\":41.98824390,\"lng\":-88.1450320}},\"location\":{\"lat\":42.04222760,\"lng\":-88.07053190000001},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":42.0673260,\"lng\":-88.03027109999999},\"southwest\":{\"lat\":41.98824390,\"lng\":-88.1450320}}},\"postcode_localities\":[],\"types\":[\"administrative_area_level_3\",\"political\"]},{\"address_components\":[{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"locality\",\"political\"]},{\"long_name\":\"Schaumburg\",\"short_name\":\"Schaumburg\",\"types\":[\"administrative_area_level_3\",\"political\"]},{\"long_name\":\"Cook\",\"short_name\":\"Cook\",\"types\":[\"administrative_area_level_2\",\"political\"]},{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"Schaumburg, IL, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.0780260,\"lng\":-88.02797090},\"southwest\":{\"lat\":41.9868390,\"lng\":-88.15441280}},\"location\":{\"lat\":42.03336070,\"lng\":-88.08340590},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":42.0780260,\"lng\":-88.02797090},\"southwest\":{\"lat\":41.9868390,\"lng\":-88.15441280}}},\"postcode_localities\":[],\"types\":[\"locality\",\"political\"]},{\"address_components\":[{\"long_name\":\"Cook\",\"short_name\":\"Cook\",\"types\":[\"administrative_area_level_2\",\"political\"]},{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"Cook, IL, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.15432390,\"lng\":-87.52404399999999},\"southwest\":{\"lat\":41.46953389999999,\"lng\":-88.26347790}},\"location\":{\"lat\":41.73765870,\"lng\":-87.6975540},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":42.15432390,\"lng\":-87.52404399999999},\"southwest\":{\"lat\":41.46953389999999,\"lng\":-88.26347790}}},\"postcode_localities\":[],\"types\":[\"administrative_area_level_2\",\"political\"]},{\"address_components\":[{\"long_name\":\"Illinois\",\"short_name\":\"IL\",\"types\":[\"administrative_area_level_1\",\"political\"]},{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"Illinois, USA\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":42.50833790,\"lng\":-87.49519909999999},\"southwest\":{\"lat\":36.9702980,\"lng\":-91.51307890}},\"location\":{\"lat\":40.63312490,\"lng\":-89.39852830},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":42.50833790,\"lng\":-87.49519909999999},\"southwest\":{\"lat\":36.9702980,\"lng\":-91.51307890}}},\"postcode_localities\":[],\"types\":[\"administrative_area_level_1\",\"political\"]},{\"address_components\":[{\"long_name\":\"United States\",\"short_name\":\"US\",\"types\":[\"country\",\"political\"]}],\"formatted_address\":\"United States\",\"geometry\":{\"bounds\":{\"northeast\":{\"lat\":71.3898880,\"lng\":-66.94976079999999},\"southwest\":{\"lat\":18.91106420,\"lng\":172.45469660}},\"location\":{\"lat\":37.090240,\"lng\":-95.7128910},\"location_type\":\"APPROXIMATE\",\"viewport\":{\"northeast\":{\"lat\":49.380,\"lng\":-66.940},\"southwest\":{\"lat\":25.820,\"lng\":-124.390}}},\"postcode_localities\":[],\"types\":[\"country\",\"political\"]}],\"status\":\"OK\"}";
	private String expectedAddr = "609 Fairbanks Court, Schaumburg, IL 60194, USA";
	private static final MyLatLng SCHAUMBURG = new MyLatLng(42.033739,-88.06469);
	private static final MyLatLng CHICAGO = new MyLatLng(41.878114, -87.629798);
	
	
	public void testDecodeFromGoogleGeoJSON(){

		GoogleGeoJsonParams jsonParams;

		try {
			jsonParams = new GoogleGeoJsonParams("");
			fail();
		} catch (Exception e) {
		}

		try {
			jsonParams = new GoogleGeoJsonParams("testing");
			fail();
		} catch (Exception e) {
		}
		
		try {
			jsonParams = new GoogleGeoJsonParams(GoogleGeoJsonResponse);
			
			Assert.assertNotNull(jsonParams.getGeneralAddress());
			
			String actualAddr = jsonParams.getDetailAddress();
			Assert.assertEquals(expectedAddr, actualAddr);
			
		} catch (Exception e) {
			fail();
		}
	}
	
	public void testEncodeToGoogleGeoQuery(){
			
		String testExpectedURI = "http://maps.googleapis.com/maps/api/geocode/json?" +
				"latlng=" + SCHAUMBURG.toSimpleString() + "&" + 
				"sensor=true";
		
		String testActualURI = URIBuilder.toGoogleGeoURI(SCHAUMBURG);
		
		Assert.assertEquals(testExpectedURI, testActualURI);
		
	}

}