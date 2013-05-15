package edu.depaul.x86azul.test;

import edu.depaul.x86azul.AlertCenter;
import android.content.Context;
import android.media.MediaPlayer;
import android.test.AndroidTestCase;


public class AlertCenterTest extends AndroidTestCase {

	public void testPlayAndStopTone(){
		
		Context ctx = getContext();
		AlertCenter ac= new AlertCenter(getContext());
		
		assertNull(ac.getMediaPlayer());
		
		ac.play(edu.depaul.x86azul.R.raw.warning_tone);
		
		MediaPlayer mpPlay = ac.getMediaPlayer();
		assertNotNull(mpPlay);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ac.stop();
		
		assertNull(ac.getMediaPlayer());
	}
	
	public void testConsecutiveTones(){
		
		Context ctx = getContext();
		AlertCenter ac= new AlertCenter(getContext());
		
		assertNull(ac.getMediaPlayer());
		
		ac.play(edu.depaul.x86azul.R.raw.warning_tone);
		
		MediaPlayer mpPlay = ac.getMediaPlayer();
		assertNotNull(mpPlay);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ac.play(edu.depaul.x86azul.R.raw.relief_tone);
		
		MediaPlayer mpStop = ac.getMediaPlayer();
		assertNotNull(mpStop);
		assertTrue(!mpPlay.equals(mpStop));
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		ac.stop();
		
		assertNull(ac.getMediaPlayer());
		
	}
	
}
