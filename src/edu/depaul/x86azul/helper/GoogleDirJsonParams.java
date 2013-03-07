package edu.depaul.x86azul.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import edu.depaul.x86azul.MyLatLng;

public class GoogleDirJsonParams {
	public List<Route> routes;
	public String status;
	
	//JSONObject is a java.util.Map and JSONArray is a java.util.List

	public GoogleDirJsonParams(JSONObject obj) {
		
		JSONArray tempArray;

		status = (String) obj.get("status");

		routes = new ArrayList<Route>();
		tempArray = (JSONArray)obj.get("routes");
		for(int i=0; i<tempArray.size();i++){
			routes.add(new Route((JSONObject)tempArray.get(i)));
		}
	}

	public class Route {
		public Bounds bounds;
		public String copyrights;
		public List<Leg> legs;
		public Polyline overview_polyline;
		public String summary;
		public List<Warning> warnings;
		public List<Waypoint_order> waypoint_order;



		public Route(JSONObject obj){
			JSONArray tempArray;
			bounds = new Bounds((JSONObject)obj.get("bounds"));

			copyrights = (String)obj.get("copyrights");

			legs = new ArrayList<Leg>();
			tempArray = (JSONArray)obj.get("legs");
			for(int i=0; i<tempArray.size();i++){
				legs.add(new Leg((JSONObject)tempArray.get(i)));
			}

			overview_polyline = new Polyline((JSONObject)obj.get("overview_polyline"));
			summary = (String)obj.get("summary");

			warnings = new ArrayList<Warning>();
			tempArray = (JSONArray)obj.get("warnings");
			for(int i=0; i<tempArray.size();i++){
				warnings.add(new Warning((JSONObject)tempArray.get(i)));
			}

			waypoint_order = new ArrayList<Waypoint_order>();
			tempArray = (JSONArray)obj.get("waypoint_order");
			for(int i=0; i<tempArray.size();i++){
				waypoint_order.add(new Waypoint_order((JSONObject)tempArray.get(i)));
			}

		}

	}

	// these are repetitive object encountered so we put here
	public class SimpleTextValue {
		public String text;
		public int value;
		public SimpleTextValue(JSONObject obj){
			text = (String)obj.get("text");
			value = ((Long)obj.get("value")).intValue();
		}
	}

	public class Bounds {
		public MyLatLng northeast;
		public MyLatLng southwest;
		public Bounds (JSONObject obj){
			northeast = new MyLatLng((JSONObject)obj.get("northeast"));
			southwest = new MyLatLng((JSONObject)obj.get("southwest"));					
		}
	}

	public class Leg {
		public SimpleTextValue distance;
		public SimpleTextValue duration;
		public String end_address;
		public MyLatLng end_location;
		public String start_address;
		public MyLatLng start_location;			
		public List<Step> steps;
		public List<Via_waypoint> via_waypoint;

		public Leg (JSONObject obj){
			JSONArray tempArray;

			distance = (SimpleTextValue)new SimpleTextValue((JSONObject) obj.get("distance"));
			duration = (SimpleTextValue)new SimpleTextValue((JSONObject) obj.get("duration"));
			end_address = (String)obj.get("end_address");
			end_location = (MyLatLng)new MyLatLng((JSONObject) obj.get("end_location"));
			start_address = (String)obj.get("start_address");
			start_location = (MyLatLng)new MyLatLng((JSONObject) obj.get("start_location"));

			steps = new ArrayList<Step>();
			tempArray = (JSONArray)obj.get("steps");
			for(int i=0; i<tempArray.size();i++){
				steps.add(new Step((JSONObject)tempArray.get(i)));
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


		public Step(JSONObject obj){
			distance = (SimpleTextValue)new SimpleTextValue((JSONObject) obj.get("distance"));
			duration = (SimpleTextValue)new SimpleTextValue((JSONObject) obj.get("duration"));
			end_location = (MyLatLng)new MyLatLng((JSONObject) obj.get("end_location"));
			html_instructions = (String)obj.get("html_instructions");
			polyline = (Polyline)new Polyline((JSONObject) obj.get("polyline"));
			start_location = (MyLatLng)new MyLatLng((JSONObject) obj.get("start_location"));
			travel_mode = (String)obj.get("travel_mode");
		}
	}

	public class Via_waypoint{
		public Via_waypoint(JSONObject obj){
		}
	}

	public class Polyline{
		public String points;
		public Polyline(JSONObject obj){
			points = (String)obj.get("points");
		}
	}

	public class Warning {
		public Warning (JSONObject obj){
		}
	}

	public class Waypoint_order{
		public Waypoint_order(JSONObject obj){
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