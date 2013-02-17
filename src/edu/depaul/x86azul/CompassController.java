package edu.depaul.x86azul;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;


public class CompassController implements SensorEventListener {

	private MainActivity mContext;
	private ImageView mCompass;
	private SensorManager mSensorManager;

	private float[] mGravs; 
	private float[] mGeoMags; 
	
	private double mPointerAngle;
	private double mAzimuthAngle;
	private double mDebrisBearing;
	
	private boolean mActive;
	private boolean mHidden;
	private boolean mInDanger;
	
	private final int SMOOTHING_FACTOR = 5;
	private double[] mMovingAverage;
	//private float[] mRotationM = new float[16];               // Use [16] to co-operate with android.opengl.Matrix 
	
	private final int DANGER_COLOR = 0xAAFF2222;
	private final int SAFE_COLOR = 0xAA7BBF6A;		
	private final int COLOR_ANIMATION_TIME = 400;
	
	private double mLastRotateTime;
	private Debris mTargetDebris;
	private long mTimeToRotate;

	@SuppressWarnings("deprecation")
	static public boolean isCompassAvailable(MainActivity context){
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null){
			return true;
		}
		else{
			return false;
		}
	}

	public CompassController(MainActivity context){
		mContext = context;
		mCompass = (ImageView) mContext.findViewById(R.id.compass);  
		mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);

		mGravs = new float[3];
		mGeoMags = new float[3];
		mMovingAverage = new double[SMOOTHING_FACTOR];
		for(int i=0; i<mMovingAverage.length;i++)
			mMovingAverage[i]=Double.MAX_VALUE;
		
		mActive = false;
		
		mPointerAngle = 0;
		mAzimuthAngle = 0;
		mDebrisBearing = 0;
		mLastRotateTime = 0;
		mHidden = false;
		mInDanger = false;
	}

	@SuppressWarnings("deprecation")
	public void open(){
		
		/*
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
				SensorManager.SENSOR_DELAY_NORMAL); 
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL); 
		*/
		
	
		mSensorManager.registerListener(this, mSensorManager
                .getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI );

		mActive = true;
	}

	public void close(){
		mSensorManager.unregisterListener(this);
		mActive = false;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}
	
	private double getSmallestDiffAngle(double reference, double compare){
		double minDiff = Double.MAX_VALUE;
		for(int i=0;i<3;i++){
			double diff = (double)Math.abs(reference - (compare + (i-1)*360));
			if(diff < minDiff){
				minDiff = diff;
			}
		}
		
		return minDiff;
	}
	
	private double transformToSmallestAngleDiff(double reference, double compare){
		double minDiff = Double.MAX_VALUE;
		double angle = 0;
		for(int i=0;i<3;i++){
			double diff = (double)Math.abs(reference - (compare + (i-1)*360));
			if(diff < minDiff){
				minDiff = diff;
				angle = compare + (i-1)*360;
			}
		}
		
		return angle;
	}

	public void onSensorChanged(SensorEvent event) {

	
		// make sure the change is stable before proceed rotating
		if(getSmallestDiffAngle(mAzimuthAngle, event.values[0]) > 10){
			mAzimuthAngle = event.values[0];
			return;
		}
	
		mAzimuthAngle = event.values[0];
		
		//DialogHelper.showDebugInfo("event[0]=" + event.values[0] + ", mAzimuthAngle=" + mAzimuthAngle);	
		
				
		rotate();
		/*
		switch (event.sensor.getType()) { 
		case Sensor.TYPE_ACCELEROMETER: 

			// NOTE: The data must be copied off the event.values 
			// as the system is reusing that array in all SensorEvents. 
			// Simply assigning: 
			// mGravs = event.values won't work. 
			// 
			for(int i=0; i<mGravs.length && i<event.values.length; i++)
				mGravs[i] = event.values[i];
			
			break; 
		case Sensor.TYPE_MAGNETIC_FIELD: 
			// Here let's try another way: 
			for(int i=0; i<mGeoMags.length && i<event.values.length; i++)
				mGeoMags[i] = event.values[i];

		default: 
			return; 
		} 

		float[] R = new float[16];
		float[] orientationValues = new float[3];

		if( !SensorManager.getRotationMatrix (R, null, mGravs, mGeoMags) )
			return;

		float[] outR = new float[16];
		SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, outR);

		SensorManager.getOrientation (outR, orientationValues);

		// the azimuth is on the 1st index
		double azimuth = Math.toDegrees (orientationValues[0]);

		// smooth this data;
		double total = 0;
		int count = 0;
		for(int i=0; i<mMovingAverage.length-1;i++)
		{
			if(Double.compare(mMovingAverage[i], Double.MAX_VALUE)!=0){
				total += mMovingAverage[i];
				count++;
			}
			// slide
			mMovingAverage[i] = mMovingAverage[i+1];
		}
		
		// put new value on the last index
		total += azimuth;
		count++;
		mMovingAverage[mMovingAverage.length-1] = azimuth;
				
		mAzimuthAngle = total/count;
		
		rotate();
				*/
	} 
	
	public boolean isActive(){
		return mActive;
	}
	
	public void setDebrisBearing(Location loc, Debris debris, boolean danger)
	{
		if(!mActive)
			return;
		
		show();
		setDangerValue(danger);
		mDebrisBearing = MyLatLng.bearing(loc, debris);
		mTargetDebris = debris;
		rotate();
	}

	public void setDangerValue(boolean danger){

		final long start = SystemClock.uptimeMillis();
		// check transition only
		if(mInDanger && !danger){

			final Handler handler = new Handler();
			handler.post(new Runnable(){

				public void run() {
					long elapsed = SystemClock.uptimeMillis() - start;
					float t = (float)elapsed/COLOR_ANIMATION_TIME;

					float color = t * SAFE_COLOR + (1-t) * DANGER_COLOR;
					mCompass.setColorFilter((int)color, Mode.MULTIPLY);

					if (t < 1.0) {
						handler.postDelayed(this, 10);
					}else{
						mCompass.setColorFilter(SAFE_COLOR, Mode.MULTIPLY);
					}
				}
			});
		}
		if(!mInDanger && danger){
			final Handler handler = new Handler();
			handler.post(new Runnable(){

				public void run() {
					long elapsed = SystemClock.uptimeMillis() - start;
					float t = (float)elapsed/COLOR_ANIMATION_TIME;

					//float color = t * DANGER_COLOR + (1-t) * SAFE_COLOR;
					//mCompass.setColorFilter((int)color, Mode.MULTIPLY);
					float color;
					if(t < 0.5){
						color = t * 0xFFFFFFFF + (1-t) * SAFE_COLOR;
					}
					else{
						color = t * SAFE_COLOR + (1-t) * 0xFFFFFFFF;
					}
					
					mCompass.setColorFilter((int)color, Mode.MULTIPLY);

					if (t < 1.0) {
						handler.postDelayed(this, 10);
					}else{
						mCompass.setColorFilter(DANGER_COLOR, Mode.MULTIPLY);
					}
				}
			});
		}

		mInDanger = danger;
	}

	@SuppressLint("NewApi")
	private void show() {
		if(!mHidden)
			return;

		mHidden = false;
		
		Animation anim = new ScaleAnimation(0, mCompass.getScaleX(), 0, mCompass.getScaleY(), 
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setDuration(500); // in ms
		anim.setRepeatCount(0);
		anim.setFillAfter(true);
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		mCompass.startAnimation(anim);  
		
		mPointerAngle = 0;
	}
	
	@SuppressLint("NewApi")
	public void hide() {
		if(mHidden)
			return;
		
		mHidden = true;
		
		Animation anim = new ScaleAnimation(mCompass.getScaleX(), 0, mCompass.getScaleY(),0, 
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setDuration(500); // in ms
		anim.setRepeatCount(0);
		anim.setFillAfter(true);
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		mCompass.startAnimation(anim);  
		
		mPointerAngle = 0;
	}
	

	@SuppressLint("NewApi")
	private void rotate(){
		if(!mActive || mHidden)
			return;

		//double angle = mDebrisBearing - mAzimuthAngle;
		double angle = transformToSmallestAngleDiff(mPointerAngle, mDebrisBearing-mAzimuthAngle);
		
		// make sure previous rotation has completed
		if(System.currentTimeMillis() - mLastRotateTime < mTimeToRotate)
			return;
		
		mTimeToRotate = (long)Math.abs(mPointerAngle-angle)*300/90; //300 ms per 90 degrees
		
		//DialogHelper.showDebugInfo("from=" + mPointerAngle + ", to=" + angle);
		
		Animation anim = new RotateAnimation((float)mPointerAngle, (float)angle, 
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(mTimeToRotate); // in ms
        anim.setRepeatCount(0);
        anim.setFillAfter(true);
        anim.setInterpolator(new LinearInterpolator());
        mCompass.startAnimation(anim);  
       
        
        mPointerAngle = angle;
        mLastRotateTime = System.currentTimeMillis();
	
        
		//mCompass.setRotation((float) -mPointerAngle);
		
	}

	public Debris getPointingDebris() {
		if(!mActive || mHidden)
			return null;
					
		return mTargetDebris;
	}


}

