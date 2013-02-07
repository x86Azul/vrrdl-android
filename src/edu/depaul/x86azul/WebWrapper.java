package edu.depaul.x86azul;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import android.os.AsyncTask;

public class WebWrapper {

	private Client mClient;
	public interface Client {
		public void onFinishProcessHttp(String result);
	}

	public WebWrapper(Client client) {
		mClient = client;
	}
	
	public void get(String param){
		new LongRunningGetIO().execute(param);
	}

	private class LongRunningGetIO extends AsyncTask <String, Void, String> {
		
		@Override
		protected String doInBackground(String... params) {
			// we only expect one parameter here
			if (params == null)
				return null;
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(params[0]);
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity httpEntity = response.getEntity();
				text = EntityUtils.toString(httpEntity);
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return text;
		}
		protected void onPostExecute(String results) {
			mClient.onFinishProcessHttp(results);
		}
	}



}