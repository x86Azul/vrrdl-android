package edu.depaul.x86azul.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GoogleGeoJsonParams {
	public List<Result> results;
	public String status;

	public class Result {
		public String formatted_address;
		
		public Result(JSONObject obj){
			formatted_address = (String)obj.get("formatted_address");;
		}
	}

	public GoogleGeoJsonParams(JSONObject obj){
		JSONArray tempArray;
		
		results = new ArrayList<Result>();
		tempArray = (JSONArray)obj.get("results");
		for(int i=0; i<tempArray.size();i++){
			results.add(new Result((JSONObject)tempArray.get(i)));
		}
		
		status = (String)obj.get("status");
	}

	public boolean isValid(){
		// there should be at least a single Step object
		try{
			if(status.equals("OK"))
				if(results.get(0).formatted_address != null)
					if(results.get(1).formatted_address != null)
						return true;
		
			return false;
		}
		catch (Exception e){
			return false;
		}
	}
	
	public String getDetailAddress(){
		return results.get(0).formatted_address;
	}
	
	public String getGeneralAddress(){
		return results.get(1).formatted_address;
	}

}