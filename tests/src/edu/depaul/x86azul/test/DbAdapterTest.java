package edu.depaul.x86azul.test;

import java.util.ArrayList;

import edu.depaul.x86azul.DbAdapter;
import edu.depaul.x86azul.Debris;
import edu.depaul.x86azul.MyLatLng;
import android.content.Context;
import android.test.AndroidTestCase;


public class DbAdapterTest extends AndroidTestCase
implements DbAdapter.OnCompleteDBOperation {

	private static final MyLatLng CHICAGO = new MyLatLng(41.878114, -87.629798);


	public void testReadWriteDb(){
		Context ctx = getContext();

		DbAdapter dba = new DbAdapter(ctx);
		dba.initialize();

		try {Thread.sleep(500);} 
		catch (InterruptedException e) {e.printStackTrace();}

		dba.clearTable();
		
		// WRITE

		Debris debris = new Debris(CHICAGO);

		// debris ID use default
		assertTrue(debris.mDebrisId == Debris.DefaultId);

		dba.insertDebris(debris);

		assertTrue(debris.mDebrisId != Debris.DefaultId);

		// READ
		ArrayList<Debris> debrisList = dba.readDebris(debris.mDebrisId);
		assertNotNull(debrisList);
		assertTrue(debrisList.size() == 1);

		Debris tempDebris = debrisList.get(0);
		assertTrue(debris.equals(tempDebris));

		dba.clearTable();
		
		dba.close();
	}
	
	public void testUpdateDb(){
		Context ctx = getContext();

		DbAdapter dba = new DbAdapter(ctx);
		dba.initialize();

		try {Thread.sleep(500);} 
		catch (InterruptedException e) {e.printStackTrace();}

		dba.clearTable();
		
		// WRITE

		Debris debris = new Debris(CHICAGO);

		// debris ID use default
		assertTrue(debris.mDebrisId == Debris.DefaultId);

		dba.insertDebris(debris);

		assertTrue(debris.mDebrisId != Debris.DefaultId);

		// UPDATE 
		assertNull(debris.mAddress);

		String tempAddress = "525 N Quentin";
		debris.mAddress = tempAddress;

		dba.updateAddress(debris, tempAddress);

		ArrayList<Debris> debrisList = dba.readDebris(debris.mDebrisId);
		assertNotNull(debrisList);
		assertTrue(debrisList.size() == 1);

		Debris tempDebris = debrisList.get(0);
		assertTrue(tempDebris.mAddress.equals(tempAddress));

		dba.clearTable();
		
		dba.close();
	}


	public void testResetDb(){
		Context ctx = getContext();

		DbAdapter dba = new DbAdapter(ctx);
		dba.initialize();

		try {Thread.sleep(500);} 
		catch (InterruptedException e) {e.printStackTrace();}
		
		// RESET
		dba.clearTable();
		
		ArrayList<Debris> debrisList = dba.readAllDebrises();
		assertNotNull(debrisList);
		assertTrue(debrisList.size() == 0);
		
		// TRY WRITE
		
		Debris debris = new Debris(CHICAGO);

		// debris ID use default
		assertTrue(debris.mDebrisId == Debris.DefaultId);
		dba.insertDebris(debris);
		assertTrue(debris.mDebrisId != Debris.DefaultId);
		
		// READ BACK
		
		debrisList = dba.readAllDebrises();
		assertNotNull(debrisList);
		assertTrue(debrisList.size() == 1);
		
		// RESET BACK
		dba.clearTable();
		
		dba.close();
	}

	@Override
	public void onInitDbCompleted(ArrayList<Debris> data) {

	}

	@Override
	public void onCloseDbCompleted() {

	}
}
