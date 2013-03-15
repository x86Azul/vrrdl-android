package edu.depaul.x86azul;

import edu.depaul.x86azul.helper.URIBuilder;
import android.os.Handler;

public class WebProxy {
	
	private final double pollRadius = 10000; // in kilometer
	
	private String mPollBaseWebAddress;
	private Handler mPollHandler;
	private MyLatLng mLocation;
	private int mPollPeriod;
	private HTTPClient.Client mClient;
	private PeriodicPolling mPollThread;
	
	public WebProxy(HTTPClient.Client client){
		mClient = client;
		
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
	
	
	private class PeriodicPolling implements Runnable {
		
		private HTTPClient.Client mClient;
		private String mToken;
		
		public PeriodicPolling(String token, HTTPClient.Client client) {
			mClient = client;
			mToken = token;
		}
		@Override
		public void run() {
			
			// build the param
			String uri = URIBuilder.toTestGetURI(mLocation, pollRadius);
			
			// launch the request
			new HTTPClient(mClient).get(mToken, uri);
			
			if(mPollHandler != null)
				mPollHandler.postDelayed(this, mPollPeriod);
			
		}

	}

}