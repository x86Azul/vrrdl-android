package edu.depaul.x86azul;


import edu.depaul.x86azul.helper.DH;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

/*
 * class to controll compass. Only active when main activity in 
 * START, RESUME, or PAUSE state
 */
public class CompassController implements SensorEventListener {

	private MainActivity mContext;
	private ImageView mCompass;
	private SensorManager mSensorManager;

	private float[] mGravs; 
	private float[] mGeoMags; 
	
	private double mPointerAngle;
	private double mAzimuthAngle;
	private double mDebrisBearing;
	
	private volatile boolean mActive;
	private volatile boolean mInDanger;
	
	// this is for smoothing algorithm
	private final int SMOOTHING_FACTOR = 3;
	private enum AngleCompare {SMALL, BIG, UNDEFINED};
	private AngleCompare[] mFilter;
	
	private double prevAngleValue;
	
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

		return (mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null);
	}

	@SuppressLint("NewApi")
	public CompassController(MainActivity context){
		mContext = context;
		mCompass = (ImageView) mContext.findViewById(R.id.compass);  
		mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
		
		mFilter = new AngleCompare[SMOOTHING_FACTOR];
		for(int i=0; i<mFilter.length;i++)
			mFilter[i]=AngleCompare.UNDEFINED;
		
		mActive = false;
		
		mPointerAngle = 0;
		mAzimuthAngle = 0;
		mDebrisBearing = 0;
		mLastRotateTime = 0;
		prevAngleValue = 0;
		
		mInDanger = false;
		
		// hide for the first time
		mActive = false;
		mCompass.setVisibility(View.INVISIBLE);

	}

	@SuppressWarnings("deprecation")
	public void onStart(){
		
		/*
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 
				SensorManager.SENSOR_DELAY_NORMAL); 
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL); 
		*/

		// register for notification here
		mSensorManager.registerListener(this, mSensorManager
                .getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
		
		// don't set to active unless they're notification from Controller

	}

	public void onStop(){
		mSensorManager.unregisterListener(this);
		setActive(false);
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
		// we do this by making sure we got X consecutive of low reading

		boolean smallDiff = (getSmallestDiffAngle(prevAngleValue, event.values[0]) < 5);
		AngleCompare ac = smallDiff?AngleCompare.SMALL:AngleCompare.BIG;
		
		prevAngleValue = event.values[0];
		
		for(int i=0; i<mFilter.length;i++){
			
			if(mFilter[i]==AngleCompare.UNDEFINED){
				// fill this one in
				mFilter[i] = ac;
				
				// only continue if we're filling the end
				if(i != mFilter.length-1)
					return;
				else 
					break;
			}
			
			// if the buffer already full with data, put this one
			// at the end
			if(i == mFilter.length-1){
				for(int j=0; j<mFilter.length-1;j++){
					
					mFilter[j] = mFilter[j+1];
					
					if(j == mFilter.length-2)
						mFilter[j+1] = ac;
				}
			}
		}
		
		// if we reach here means the buffer is full, time to do calculation
		boolean bOK = true;
		for(int i=0; i<mFilter.length;i++){
			if(mFilter[i]==AngleCompare.BIG){
				bOK = false;
				break;
			}
		}
		
		if(!bOK)
			return;
		
		// we're clear, we can pass the info and don't forget 
		// clear out the filter
			
		for(int i=0; i<mFilter.length;i++)
			mFilter[i]=AngleCompare.UNDEFINED;
		
		// but wait, if the change is really small then don't bother
		if(Math.abs(mAzimuthAngle - event.values[0]) < 5)
			return;
		
		mAzimuthAngle = event.values[0];
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
	
	public void setDebris(Location loc, Debris debris, boolean danger)
	{
		mTargetDebris = debris;
		
		// this means Coordinator doesn't want us to track debris anymore
		if(mTargetDebris == null || loc == null) {
			mDebrisBearing = 0;
			setActive(false);
			return;
		}
		
		mDebrisBearing = MyLatLng.bearing(loc, debris);
		
		// ups.. cannot go further
		if(!GP.isVisibleState()){
			return;
		}
		
		setActive(true);
		setDangerValue(danger);
		
		rotate();
	}

	public void setDangerValue(boolean danger){
		if(danger && mInDanger)
			return;
		if(!danger && !mInDanger)
			return;

		mInDanger = danger;
		
		colorAnimation(mInDanger);
	}
	
	public void setActive(boolean active) {
		if(active && mActive)
			return;
		if(!active && !mActive)
			return;
		
		mActive = active;
		
		sizeAnimation(mActive);
	}
	
	@SuppressLint("NewApi")
	private void sizeAnimation(boolean active){
		
		float startX = active?0:mCompass.getScaleX();
		float endX   = active?mCompass.getScaleX():0;
				
		float startY = active?0:mCompass.getScaleY();
		float endY   = active?mCompass.getScaleY():0;
			
		Animation anim = new ScaleAnimation(startX, endX, startY, endY, 
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setDuration(500); // in ms
		anim.setRepeatCount(0);
		anim.setFillAfter(true);
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		mCompass.setVisibility(View.VISIBLE);
		mCompass.startAnimation(anim);  
		
		mPointerAngle = 0;
	}
	
	private void colorAnimation(boolean danger){
		
		final long start = SystemClock.uptimeMillis();
		final int startColor = danger?SAFE_COLOR:DANGER_COLOR;
		final int endColor   = danger?DANGER_COLOR:SAFE_COLOR;
		final Handler handler = new Handler();
		
		handler.post(new Runnable(){
			public void run() {
				long elapsed = SystemClock.uptimeMillis() - start;
				float t = (float)elapsed/COLOR_ANIMATION_TIME;

				float color = t * (float)endColor + (1-t) * (float)startColor;
				mCompass.setColorFilter((int)color, Mode.MULTIPLY);

				if (t < 1.0) {
					handler.postDelayed(this, 20);
				}else{
					mCompass.setColorFilter(endColor, Mode.MULTIPLY);
				}
			}
		});
	}
	

	@SuppressLint("NewApi")
	private void rotate(){
		if(!mActive)
			return;

		//double angle = mDebrisBearing - mAzimuthAngle;
		double angle = transformToSmallestAngleDiff(mPointerAngle, mDebrisBearing-mAzimuthAngle);
		
		// make sure previous rotation has completed
		if(System.currentTimeMillis() - mLastRotateTime < mTimeToRotate)
			return;
		
		mTimeToRotate = (long)Math.abs(mPointerAngle-angle)*300/90; //300 ms per 90 degrees
		
		//DialogHelper.showDebugInfo("from=" + mPointerAngle + ", to=" + angle);
		
		if(mCompass.getAnimation() == null || mCompass.getAnimation().hasEnded())
		{
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
       
        
		
	}

	public Debris getPointingDebris() {
		if(!mActive)
			return null;
					
		return mTargetDebris;
	}


}

