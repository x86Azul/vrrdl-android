package edu.depaul.x86azul;

import java.util.ArrayList;

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
		public void onFinishProcessHttp(String token, String result);
	}

	public WebWrapper(Client client) {
		mClient = client;
	}
	
	public void get(String token, String param){
		new LongRunningGetIO().execute(token, param);
	}

	private class LongRunningGetIO extends AsyncTask <String, Void, ArrayList<String>> {
		
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			// we expect at least two parameters here
			if (params == null || params.length < 2)
				return null;
			
			ArrayList<String> returns = new ArrayList<String>();
			returns.add(params[0]);
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(params[1]);
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
				HttpEntity httpEntity = response.getEntity();
				text = EntityUtils.toString(httpEntity);
			} catch (Exception e) {
				returns.add(e.getLocalizedMessage());
				return returns;
			}
			
			returns.add(text);
			return returns;
		}
		protected void onPostExecute(ArrayList<String> returns) {
			if (returns != null)
				mClient.onFinishProcessHttp(returns.get(0), returns.get(1));
		}
	}



}