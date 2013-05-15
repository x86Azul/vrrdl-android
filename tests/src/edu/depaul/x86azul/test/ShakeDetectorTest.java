package edu.depaul.x86azul.test;

import java.util.ArrayList;

import edu.depaul.x86azul.DbAdapter;
import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.MyLatLng;
import edu.depaul.x86azul.ShakeDetector;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.test.AndroidTestCase;


public class ShakeDetectorTest extends AndroidTestCase
implements ShakeDetector.OnShakeDetected {

	private static final MyLatLng CHICAGO = new MyLatLng(41.878114, -87.629798);

	private boolean shakeDetected;

	public void testWeakShake(){
		
		shakeDetected = false;
		
		Context ctx = getContext();
		ShakeDetector sd = new ShakeDetector(ctx);
		sd.subscribe(this);
		
		// simulate weak movement
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(1, 2, 3);
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(1, 2, 3);
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(1, 2, 3);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertFalse(shakeDetected);
		
		// simulate strong but only one time change
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(50, 50, 50);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertFalse(shakeDetected);
	}
	
	public void testStrongShake(){
		
		shakeDetected = false;
		
		Context ctx = getContext();
		ShakeDetector sd = new ShakeDetector(ctx);
		sd.subscribe(this);
		
		// simulate strong movement several times
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(50, 50, 50);
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(50, 50, 50);
		sd.sensorChanged(0, 0, 0);
		sd.sensorChanged(50, 50, 50);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertTrue(shakeDetected);
		
	}


	@Override
	public void onShakeDetected() {
		
		shakeDetected = true;
	}



}
