package edu.depaul.x86azul.activities;

import java.util.ArrayList;
import java.util.HashMap;

import edu.depaul.x86azul.DataCoordinator;
import edu.depaul.x86azul.MainActivity;
import edu.depaul.x86azul.R;
import edu.depaul.x86azul.R.id;
import edu.depaul.x86azul.R.layout;
import edu.depaul.x86azul.helper.DH;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class DebrisListActivity extends Activity 
implements OnItemClickListener {

	ListView mDebrisList ;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_listdebris);

		DH.showDebugMethodInfo(this);
		
		mDebrisList = (ListView) findViewById(R.id.debrisList);

		ArrayList <HashMap<String,String >> listItem = DataCoordinator.IntentToParcell(getIntent());

		SimpleAdapter mSchedule = new SimpleAdapter (this.getBaseContext(), listItem, R.layout.format_listdebris,
				new String[] {"img", "id", "address", "distance"}, new int[] {R.id.img, R.id.title, R.id.description, R.id.distance});

		//On set attribute based on the adapter
		mDebrisList.setAdapter(mSchedule);


		// we set the click feedback here
		mDebrisList.setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
		
		@SuppressWarnings("unchecked")
		HashMap<String,String> hMap = (HashMap<String,String>)a.getItemAtPosition(position);
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("debrisId" , hMap.get("idOriginal"));
		setResult(Activity.RESULT_OK, intent);
		//Toast.makeText(v.getContext(), selection, Toast.LENGTH_LONG).show();
		
		finish();	

		//Intent intent = new Intent(this, MainActivity.class);
		//startActivity(intent);
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        // make sure data can be accessed
        DH.showDebugMethodInfo(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  
        DH.showDebugMethodInfo(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        DH.showDebugMethodInfo(this);
    }


}