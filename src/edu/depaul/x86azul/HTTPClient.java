package edu.depaul.x86azul;

import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import edu.depaul.x86azul.helper.DH;
import android.annotation.SuppressLint;
import android.os.AsyncTask;

public class HTTPClient {
	
	private static final int CONNECTION_TIMEOUT = 3000;
	private static final int SOCKET_TIMEOUT = 5000;
	
	//private HttpClient mHttpClient;
	private HttpParams mHttpParameters;
	
	public interface OnFinishProcessHttp {
		public void onFinishProcessHttp(String token, 
										String uri,
										String requestBody,
										int statusCode,
										String geohash,
										String responseBody);
	}
	
	private OnFinishProcessHttp mClient;

	public HTTPClient(OnFinishProcessHttp client) {
		mClient = client;
		
		mHttpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(mHttpParameters, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(mHttpParameters, SOCKET_TIMEOUT);
		
	}
	
	@SuppressLint("NewApi")
	public void get(String token, String uri){
		new HttpGetTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, token, uri);
	}
	
	@SuppressLint("NewApi")
	public void delete(String token, String uri) {
		new HttpDeleteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, token, uri);
	}
	
	@SuppressLint("NewApi")
	public void put(String token, String uri, String param){
		new HttpPutTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, token, uri, param);
	}
	
	
	
	public static boolean success(int code){
		return (code < HttpStatus.SC_BAD_REQUEST);
	}

	private static String subtractGeohash(Header[] headers){
		String returnStr = null;

		if(headers==null)
			return null;

		for(int i=0; i<headers.length; i++){
			if(headers[i].getName().equals("Location")){
				String[] seperated = headers[i].getValue().split("/");
				if(seperated!=null){
					// the geohash is at the last index
					returnStr = seperated[seperated.length-1];
					break;
				}
			}
		}

		return returnStr;
	}

	private class HttpPutTask extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			// we accept token, uri, and requestBody as parameters
			// add all of those
			ArrayList<String> returns = new ArrayList<String>();
			returns.add(params[0]);
			returns.add(params[1]);
			returns.add(params[2]);
			
			
			HttpClient httpClient = new DefaultHttpClient(mHttpParameters);
			HttpContext localContext = new BasicHttpContext();			
			
			// put default status code as bad gateway
			int statusCode = HttpStatus.SC_BAD_GATEWAY;
			String geohash = null;
			String responseStr = null;
			
			try {
				
				// fill in the target address (params[1])
				HttpPut putRequest = new HttpPut(params[1]);
				
				// fill in the body (params[2])
				StringEntity se = new StringEntity(params[2]);
			    se.setContentEncoding("UTF-8");
			    se.setContentType("application/json");
			    putRequest.setEntity(se); 
			    
			    // execute
				HttpResponse response = httpClient.execute(putRequest, localContext);
			
				if(response!=null){
					statusCode = response.getStatusLine().getStatusCode();
					geohash = subtractGeohash(response.getAllHeaders());
					HttpEntity httpEntity = response.getEntity();
					responseStr = EntityUtils.toString(httpEntity);
				}
			} 
			catch (Exception e) {	
				// for debugging purpose
				statusCode = HttpStatus.SC_BAD_REQUEST;
				responseStr = e.getClass().getName() + ": " + e.getLocalizedMessage();
			}
			
			returns.add(String.valueOf(statusCode));
			returns.add(geohash);
			returns.add(responseStr);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null)
				mClient.onFinishProcessHttp(returns.get(0), 
											returns.get(1),
											returns.get(2),
											Integer.valueOf(returns.get(3)),
											returns.get(4),
											returns.get(5));
		}
	}

	private class HttpGetTask extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			ArrayList<String> returns = new ArrayList<String>();
			
			// we accept token, uri, as parameters
			// add all of those, plus a null for requestBody replacement
			returns.add(params[0]);
			returns.add(params[1]);
			returns.add(null);
			
			HttpClient httpClient = new DefaultHttpClient(mHttpParameters);
			HttpContext localContext = new BasicHttpContext();
			
			
			// put default status code as bad gateway
			int statusCode = HttpStatus.SC_BAD_GATEWAY;
			String responseStr = null;
						
			//DH.showDebugInfo("HttpGetTask URI:" + params[1]);
			try {
				HttpGet httpGet = new HttpGet(params[1]);
				HttpResponse response = httpClient.execute(httpGet, localContext);
				
				if(response!=null){
					statusCode = response.getStatusLine().getStatusCode();
					responseStr = EntityUtils.toString(response.getEntity());
				}
				
			} catch (Exception e) {
				statusCode = HttpStatus.SC_BAD_REQUEST;
				responseStr = e.getClass().getName() + ":" + e.getLocalizedMessage();
			}
			
			
			returns.add(String.valueOf(statusCode));
			// add null for geohash replacement
			returns.add(null);
			returns.add(responseStr);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null)
				mClient.onFinishProcessHttp(returns.get(0), 
											returns.get(1),
											returns.get(2),
											Integer.valueOf(returns.get(3)),
											returns.get(4),
											returns.get(5));
		}
	}
	
	private class HttpDeleteTask extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			ArrayList<String> returns = new ArrayList<String>();
			
			// we accept token, uri, as parameters
			// add all of those, plus a null for requestBody replacement
			returns.add(params[0]);
			returns.add(params[1]);
			returns.add(null);
			
			HttpClient httpClient = new DefaultHttpClient(mHttpParameters);
			HttpContext localContext = new BasicHttpContext();
			
			
			// put default status code as bad gateway
			int statusCode = HttpStatus.SC_BAD_GATEWAY;
			String responseStr = null;			
			//DH.showDebugInfo("HttpGetTask URI:" + params[1]);
			try {
				HttpDelete httpDelete = new HttpDelete(params[1]);
				HttpResponse response = httpClient.execute(httpDelete, localContext);
				
				if(response!=null){
					statusCode = response.getStatusLine().getStatusCode();
					responseStr = EntityUtils.toString(response.getEntity());
				}
				
			} catch (Exception e) {
				statusCode = HttpStatus.SC_BAD_REQUEST;
				responseStr = e.getClass().getName() + ":" + e.getLocalizedMessage();
			}
			
			
			returns.add(String.valueOf(statusCode));
			// add null for geohas replacement
			returns.add(null);
			returns.add(responseStr);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null)
				mClient.onFinishProcessHttp(returns.get(0), 
											returns.get(1),
											returns.get(2),
											Integer.valueOf(returns.get(3)),
											returns.get(4),
											returns.get(5));
		}
	}

	



}