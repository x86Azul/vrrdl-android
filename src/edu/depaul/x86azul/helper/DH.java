package edu.depaul.x86azul.helper;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;



public class DH {
	public static void showToast(Activity context, String txt, int duration, int gravity){
		Toast toast = Toast.makeText(context.getBaseContext(), txt, duration);
		toast.setGravity(gravity, 0, 150);
		toast.show();
	}

	public static void showToast(Activity context, String txt){
		Toast toast = Toast.makeText(context.getBaseContext(), txt, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 150);
		toast.show();
	}

	public static void showDebugInfo(String txt){
		Log.i("QQQ", txt);
	}
	
	public static void showDebugError(String txt){
		Log.e("QQQ", Thread.currentThread().getStackTrace()[3].toString() + 
				"|" + txt);
	}

	public static void showDebugMethodInfo(Object o){

		showDebugInfo(o.getClass().getName() + "|" + 
				Thread.currentThread().getStackTrace()[3].getMethodName());
	}

	public static void showDebugWarning(String txt){
		Log.w("QQQ", txt);
	}

	public static void showDebugMethodInfo(Object o, String data) {
		showDebugInfo(o.getClass().getName() + "|" + 
				Thread.currentThread().getStackTrace()[3].getMethodName() + 
				"|" + data);
	}
	
	public static String toSimpleDistance(Double distanceInMeter){
		double mileValue = distanceInMeter * 0.000621371;
		String mileString = null;
		if (mileValue < 10.0)
			mileString = new DecimalFormat("0.00").format(mileValue);
		else if (mileValue < 100.0) 
			mileString = new DecimalFormat("0.0").format(mileValue);
		else
			mileString = new DecimalFormat("0").format(mileValue);

		return mileString;
	}
	
	public static String toShortAddress(String completeAddress){
		try{
			return completeAddress.substring(0, completeAddress.indexOf(','));
		}
		catch(Exception e){
			return completeAddress;
		}
	}

}