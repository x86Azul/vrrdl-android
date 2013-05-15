package edu.depaul.x86azul.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.depaul.x86azul.MyLatLng;

public class GoogleDirJsonParams {
	public List<Route> routes;
	public String status;
	
	// JSONObject is a java.util.Map and JSONArray is a java.util.List
	// this might throw exception due to conversion or bad data
	public GoogleDirJsonParams(String jsonString) throws Exception {
		
		JSONObject tempObj = new JSONObject(jsonString);
		JSONArray tempArray;
		
		status = tempObj.getString("status");

		routes = new ArrayList<Route>();
		tempArray = tempObj.getJSONArray("routes");
		for(int i=0; i<tempArray.length();i++){
			routes.add(new Route(tempArray.getJSONObject(i)));
		}
	}

	public class Route {
		public Bounds bounds;
		public String copyrights;
		public List<Leg> legs;
		public Polyline overview_polyline;
		public String summary;

		public Route(JSONObject obj) throws JSONException{
			JSONArray tempArray;
			bounds = new Bounds(obj.getJSONObject("bounds"));

			copyrights = obj.getString("copyrights");

			legs = new ArrayList<Leg>();
			tempArray = obj.getJSONArray("legs");
			for(int i=0; i<tempArray.length();i++){
				legs.add(new Leg(tempArray.getJSONObject(i)));
			}

			overview_polyline = new Polyline(obj.getJSONObject("overview_polyline"));
			summary = obj.getString("summary");

		}

	}

	// these are repetitive object encountered so we put here
	public class SimpleTextValue {
		public String text;
		public int value;
		public SimpleTextValue(JSONObject obj) throws JSONException{
			text = obj.getString("text");
			value = (int) obj.getLong("value");
		}
	}

	public class Bounds {
		public MyLatLng northeast;
		public MyLatLng southwest;
		public Bounds (JSONObject obj) throws JSONException{
			northeast = new MyLatLng(obj.getJSONObject("northeast"));
			southwest = new MyLatLng(obj.getJSONObject("southwest"));					
		}
		
		public ArrayList<MyLatLng> getBounds(){
			ArrayList<MyLatLng> tempBounds = new ArrayList<MyLatLng>();
			tempBounds.add(northeast);
			tempBounds.add(southwest);
			
			return tempBounds;
		}
	}

	public class Leg {
		public SimpleTextValue distance;
		public SimpleTextValue duration;
		public String end_address;
		public MyLatLng end_location;
		public String start_address;
		public MyLatLng start_location;			
		public ArrayList<Step> steps;

		public Leg (JSONObject obj) throws JSONException{
			JSONArray tempArray;

			distance = new SimpleTextValue(obj.getJSONObject("distance"));
			duration = new SimpleTextValue(obj.getJSONObject("duration"));
			end_address = obj.getString("end_address");
			end_location = new MyLatLng(obj.getJSONObject("end_location"));
			start_address = obj.getString("start_address");
			start_location = new MyLatLng(obj.getJSONObject("start_location"));

			steps = new ArrayList<Step>();
			tempArray = obj.getJSONArray("steps");
			for(int i=0; i<tempArray.length();i++){
				steps.add(new Step(tempArray.getJSONObject(i)));
			}
		}

	}

	public class Step {
		public SimpleTextValue distance;
		public SimpleTextValue duration;
		public MyLatLng end_location;
		public String html_instructions;
		public Polyline polyline;
		public MyLatLng start_location;
		public String travel_mode;


		public Step(JSONObject obj) throws JSONException{
			distance = new SimpleTextValue(obj.getJSONObject("distance"));
			duration = new SimpleTextValue(obj.getJSONObject("duration"));
			end_location = new MyLatLng(obj.getJSONObject("end_location"));
			html_instructions = obj.getString("html_instructions");
			polyline = new Polyline(obj.getJSONObject("polyline"));
			start_location = new MyLatLng(obj.getJSONObject("start_location"));
			travel_mode = obj.getString("travel_mode");
		}
	}

	public class Polyline{
		public String points;
		public Polyline(JSONObject obj) throws JSONException{
			points = obj.getString("points");
		}
	}
	
	public String getOverview_polyline (){

		String ret = null;

		// this can be null pointer, be careful
		try{
			ret = routes.get(0).overview_polyline.points;
		}catch (Exception e){

		}
		return ret;
	}

	
	public boolean isValid(){
		// there should be at least a single Step object
		try{
			if(status.equals("OK"))
				if(routes.get(0).legs.get(0).steps.get(0) != null)
					if(routes.get(0).overview_polyline.points != null)
						return true;
		
			return false;
		}
		catch (Exception e){
			return false;
		}
	}
	
	public List<Step> getSteps(){
		return routes.get(0).legs.get(0).steps;
	}

}