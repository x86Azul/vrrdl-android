package edu.depaul.x86azul;

import edu.depaul.x86azul.helper.URIBuilder;
import android.os.Handler;

public class WebProxy {
	
	private final double pollRadius = 10000; // in kilometer
	
	private Handler mPollHandler;
	private MyLatLng mLocation;
	private int mPollPeriod;
	private HTTPClient.OnFinishProcessHttp mClient;
	private PeriodicPolling mPollThread;
	private HTTPClient mHttpClient;
	
	public WebProxy(HTTPClient.OnFinishProcessHttp client){
		mClient = client;
		mHttpClient = new HTTPClient(mClient);
		mPollHandler = new Handler();
		mPollPeriod = 1000;
	}
	
	public void pollStart(String token, int periodInMilisecond){
		// make sure polling is stopped
		if(mPollThread != null)
			pollStop();
		
		mPollThread = new PeriodicPolling(token, mClient);
		mPollPeriod = periodInMilisecond;
		mPollHandler.post(mPollThread);
	}
	
	public void pollStop(){
		if(mPollThread!= null){
			mPollHandler.removeCallbacks(mPollThread);
			mPollThread = null;
		}
		
	}
	
	public void setPollData(MyLatLng location){
		mLocation = location;
	}
	
	public void get(String token, String uri){
		mHttpClient.get(token, uri);
	}
	
	public void delete(String token, String uri) {
		mHttpClient.delete(token, uri);
	}
	
	public void put(String token, String uri, String param){
		mHttpClient.put(token, uri, param);
	}
	
	
	private class PeriodicPolling implements Runnable {
		

		private String _mToken;
		
		public PeriodicPolling(String token, HTTPClient.OnFinishProcessHttp client) {
			_mToken = token;
		}
		public void run() {
			
			// build the param
			String uri = URIBuilder.toTestGetURI(mLocation, pollRadius);
			
			// launch the request
			mHttpClient.get(_mToken, uri);
			
			if(mPollHandler != null)
				mPollHandler.postDelayed(this, mPollPeriod);
			
		}

	}

}