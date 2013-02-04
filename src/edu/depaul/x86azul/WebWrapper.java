package edu.depaul.x86azul;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.ws.rs.core.MediaType;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.spi.service.ServiceFinder;


public class WebWrapper {

	private Client mJerseyClient;
	private WebResource mRESTWebService;
	private final String mURI = "http://www.reddit.com/r/programming/comments/9szpc/jsonlint_a_handy_json_validator_and_reformatter.json";
	
	private User mUser;
	
	// the user need to implement this
	// and it needs to be from Activity class
	public interface User{
		public void completedWebExec();
	}
	
	@SuppressWarnings("rawtypes")
	public WebWrapper(Activity activity){
		
		/*
		mUser = (User)activity;

		// both Client and WebResource are multi-thread safe, so we can declare them here
		mJerseyClient = Client.create(new DefaultClientConfig());
		mRESTWebService = mJerseyClient.resource(mURI);
		// this is the jersey-android patch
		ServiceFinder.setIteratorProvider(new Buscador());
		*/

	}
	
	public String getResponse(){

		new WebAsyncOperation().execute(null, null, null);

		return new String("test");
	}
	
	public class WebAsyncOperation extends AsyncTask<String, Void, String> {

		protected void onPostExecute(String result) {
			mUser.completedWebExec();
			Log.w("QQQ", "WebResult:" + result);
		}

		@Override
		protected String doInBackground(String... params) {

			try{
				String result = "";
				ClientResponse response = mRESTWebService.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

				Log.w("QQQ", "GET success!, stat=" + response.getStatus() + ", type=" + response.getType() 
						+ ", json type=" + MediaType.APPLICATION_JSON +"," + MediaType.APPLICATION_JSON_TYPE);

				// read the response body			
				BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntityInputStream()));
				String line = "";
				while ((line = rd.readLine()) != null) {
					result = result.concat(line);
				}

				// parse it
				if(response.getType().toString().contains(MediaType.APPLICATION_JSON_TYPE.toString())){

					JSONArray array=(JSONArray)JSONValue.parse(result);
					Log.w("QQQ", ((JSONObject) ((JSONObject) array.get(1)).get("data")).get("children").toString());
				}

				return result ;
			}
			catch(Exception ex){
				ex.printStackTrace();
				Log.w("QQQ", "GET panic! ex=" + ex.getClass());
				return ex.getMessage();
			}

		}

	}

}