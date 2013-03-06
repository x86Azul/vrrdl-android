package edu.depaul.x86azul;

import java.util.ArrayList;
import java.util.HashMap;


import edu.depaul.x86azul.helper.DialogHelper;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class WebServiceAddressActivity extends Activity {
	
	public static final String HTTPBIN = "http://httpbin.org/put";
	public static final String POSTTEST = "http://posttestserver.com/post.php";		
		
	public static final String Param = "webAddress";
	public static final String Result = "webAddress";

	private EditText mCustom;
    private RadioGroup mRadioGroup;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_webaddress);

		DialogHelper.showDebugMethodInfo(this);
		
		String currentWebAddress = getIntent().getStringExtra(Param);
		
		DialogHelper.showDebugInfo("currentWebAddress:" + currentWebAddress);
		
		mRadioGroup = (RadioGroup) findViewById(R.id.menu);
		
		// insert default values
		RadioButton newRadioButton0 = new RadioButton(this);
		newRadioButton0.setText(HTTPBIN);
		newRadioButton0.setId(0);
		mRadioGroup.addView(newRadioButton0, 0);
		
		RadioButton newRadioButton1 = new RadioButton(this);
		newRadioButton1.setText(POSTTEST);
		newRadioButton1.setId(1);
		mRadioGroup.addView(newRadioButton1, 1);
		
		
		
		mCustom = (EditText)findViewById(R.id.customWebAddress);
		mCustom.setOnFocusChangeListener(new OnFocusChangeListener(){
		    public void onFocusChange(View v, boolean hasFocus){
		        if (hasFocus){
		        	int count = mRadioGroup.getChildCount();
		        	((RadioButton)mRadioGroup.getChildAt(count-1)).setChecked(true);
		        }
		    }
		});
		
		int count = mRadioGroup.getChildCount();
		for(int i=0; i<count; i++){
			RadioButton rb = (RadioButton)mRadioGroup.getChildAt(i);
			
			// last idx is custom
			if(i == count-1){
				rb.setChecked(true);
				mCustom.setText(currentWebAddress);				
				DialogHelper.showDebugInfo("setSelected:id=" + i);
				break;
			}
			
			if(rb.getText().equals(currentWebAddress)){
				DialogHelper.showDebugInfo("setSelected:id=" + i);
				rb.setChecked(true);
				break;
			}
		}
		
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				int count = mRadioGroup.getChildCount();
				// check if custom button is checked
				RadioButton rb = (RadioButton)mRadioGroup.getChildAt(count-1);
				if(rb.isChecked()){
					mCustom.requestFocus();
				}else{
					findViewById(R.id.dummy).requestFocus();
					findViewById(R.id.dummy).requestFocusFromTouch();
				}
				
			}
			
		});
	
	}
	
	private String getSelectedWebAddress(){
		
		String res = null;
		
		// let see if custom being checked
		int count = mRadioGroup.getChildCount();
		RadioButton rbCustom = (RadioButton)mRadioGroup.getChildAt(count - 1);
		
		int radioButtonID = mRadioGroup.getCheckedRadioButtonId();
		
		if(rbCustom.isChecked()) 
			res = mCustom.getText().toString();
		else
			res = ((RadioButton)mRadioGroup.findViewById(radioButtonID)).getText().toString();

		DialogHelper.showDebugInfo("selectedWebAddress:id=" + radioButtonID+ ",res=" + res);
		return res;
	}
	
	public void onSaveSelectionButtonClick(View view){
		
		Intent intent = new Intent(this, MainActivity.class);
		
		intent.putExtra("webAddress" , getSelectedWebAddress());
		setResult(Activity.RESULT_OK, intent);
		finish();
	}
	
	
	@Override
    protected void onStart() {
        super.onStart();
        // make sure data can be accessed
        DialogHelper.showDebugMethodInfo(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  
        DialogHelper.showDebugMethodInfo(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        DialogHelper.showDebugMethodInfo(this);
    }


}