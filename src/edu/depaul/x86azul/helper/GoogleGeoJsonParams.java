package edu.depaul.x86azul.helper;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GoogleGeoJsonParams {
	public List<Result> results;
	public String status;

	public GoogleGeoJsonParams(String jsonString) throws JSONException {
		org.json.JSONObject tempObj = new JSONObject(jsonString);

		results = new ArrayList<Result>();
		JSONArray tempArray = tempObj.getJSONArray("results");
		for(int i=0; i<tempArray.length();i++){
			results.add(new Result(tempArray.getJSONObject(i)));
		}
		
		status = tempObj.getString("status");
	}
	
	public class Result {
		public String formatted_address;
		
		public Result(JSONObject obj) throws JSONException{
			formatted_address = obj.getString("formatted_address");;
		}
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