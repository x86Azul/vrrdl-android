package edu.depaul.x86azul;

import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import android.os.AsyncTask;

public class WebWrapper {
	

	private Client mClient;
	public interface Client {
		public void onFinishProcessHttp(String token, 
										String uri,
										String body,
										String result);
	}

	public WebWrapper(Client client) {
		mClient = client;
	}
	
	public void get(String token, String uri){
		new HttpGetTask().execute(token, uri);
	}
	
	public void put(String token, String uri, String param){
		new HttpPutTask().execute(token, uri, param);
	}
	
	private class HttpPutTask extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			// we accept 3 parameters, add all of those
			ArrayList<String> returns = new ArrayList<String>();
			returns.add(params[0]);
			returns.add(params[1]);
			returns.add(params[2]);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			
			// fill in the target address (params[1])
			HttpPut putRequest = new HttpPut(params[1]);
			
			String responseStr = null;
			try {
				
				// fill in the body (params[2])
				StringEntity se = new StringEntity(params[2]);
			    se.setContentEncoding("UTF-8");
			    se.setContentType("application/json");
			    putRequest.setEntity(se); 
			    
			    // execute
				HttpResponse response = httpClient.execute(putRequest, localContext);
				HttpEntity httpEntity = response.getEntity();
				responseStr = EntityUtils.toString(httpEntity);
				
			} catch (Exception e) {
				
				responseStr = e.getLocalizedMessage();
			}
			
			returns.add(responseStr);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null)
				mClient.onFinishProcessHttp(returns.get(0), 
											returns.get(1),
											returns.get(2),
											returns.get(3));
		}
	}

	private class HttpGetTask extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			ArrayList<String> returns = new ArrayList<String>();
			
			// we accept 2 parameters, add all of those
			returns.add(params[0]);
			returns.add(params[1]);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(params[1]);
			
			String responseStr = null;
			try {
				
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity httpEntity = response.getEntity();
				responseStr = EntityUtils.toString(httpEntity);
				
			} catch (Exception e) {
				
				responseStr = e.getLocalizedMessage();
			}
			
			returns.add(responseStr);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null){
				
				// just put null for the body part
				mClient.onFinishProcessHttp(returns.get(0), 
						returns.get(1),
						null,
						returns.get(2));
			}
		}
	}



}