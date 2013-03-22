package edu.depaul.x86azul;

import android.content.Context;
import android.media.MediaPlayer;



public class AlertCenter {
	
	/*
	 * handle for media to play alert
	 */
	private MediaPlayer mMediaHandle;
	
	private Context mContext;
	
	public AlertCenter(Context context){
		mContext = context;
	}
	
	public synchronized void play(int resourceId){
		
		if(mMediaHandle != null){
			stop();
		}
				
		mMediaHandle = MediaPlayer.create(mContext, resourceId);
		mMediaHandle.start(); // no need to call prepare(); create() does that for you
	}
	
	public synchronized void stop(){
		
		if(mMediaHandle != null){
			
			mMediaHandle.stop();
			mMediaHandle.release();
			mMediaHandle = null;
		}
	}
}