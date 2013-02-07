package edu.depaul.x86azul;

import android.app.Activity;
import android.widget.TextView;

public class DebugLog {
	
	private TextView mTopText;
	private final boolean bEnable = false;
	private Activity mClient;
	
	public DebugLog(Activity client){
		
		mClient = client;
		if(bEnable)
			mTopText = null;//(TextView) mClient.findViewById(R.id.top_text);
	}
	
	public void print(String msg){
		if(bEnable)
			mTopText.setText(msg);
	}
	
}