package edu.depaul.x86azul;

import java.util.List;

import edu.depaul.x86azul.helper.DialogHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * This class is used for adapter our specific database + table (debris)
 */
public class DbAdapter implements BaseColumns {
	
	// If you change the database schema, you MUST increment the database version.
    private final int DATABASE_VERSION = 3;
    private final String DATABASE_NAME = "Vrrdl.db";
    
    private MainActivity mContext;
    private Client mClient;
    
    private boolean mOpen;
    private boolean mFirstTime;
    
    public interface Client {
		public void onInitDbCompleted(List<Debris> data);
		public void onCloseDbCompleted();
	}

    private class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
        	// this will not create database. The actual creation is when
        	// client call getWritableDatabase() or getReadableDatabase()
        	// where it will get forwarded to onCreate()
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	try {
        		// create the table as well
        		db.execSQL(Debris.SQL_DEBRIS_TABLE_CREATE);	
        	} catch (SQLException e) {
        		e.printStackTrace();
        	}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
        {
        	// rebuild a new table here
        	Log.w("QQQ", "down/upgrade database from version " + oldVersion + " to "
        	            + newVersion + ", all data will get deleted");
            db.execSQL(Debris.SQL_DEBRIS_TABLE_DELETE);
            onCreate(db);
        }
        
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }    
    
	private class InitDatabase extends AsyncTask<DatabaseHelper, Void, List<Debris>> {

		protected void onPostExecute(List<Debris> result) {
			// let user know we complete the initilization
			// Log.w("QQQ", "doInBackground");
			if(mClient != null)
				mClient.onInitDbCompleted(result);
		}

		@Override
		protected List<Debris> doInBackground(DatabaseHelper... dbHelper) {
			
			// Log.w("QQQ", "doInBackground");
	    	// this will create database plus table if not exist
	    	mDb = dbHelper[0].getWritableDatabase();
	    
	    	if(mFirstTime){
	    		mFirstTime = false;
	    		return getAllDebrisRecords();
	    	}
	    	else{
	    		return null;
	    	}
		}
	}

    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    public DbAdapter(MainActivity context) 
    {
    	mContext = context;
    	mDbHelper = new DatabaseHelper(mContext);
    	mOpen = false;
    	mFirstTime = true;
    }
    
    public void subscribe(Client client) {
		mClient = client;
	}
    
    //---opens the database---
    public void initialize() throws SQLException 
    {
    	// don't open twice
    	if(mOpen == true)
    		return;
    	
    	
    	if(mFirstTime){
    		// this will retrieve all data from database too
    		new InitDatabase().execute(mDbHelper);
    	}
    	else
    		mDb = mDbHelper.getWritableDatabase();

    	mOpen = true;
    }

    //---closes the database---    
    public void close() 
    {
    	if(mOpen == false)
    		return;
    	
    	// this will also close any opened database
    	mDbHelper.close();
    	
    	mOpen = false;
    }
    
    //---insert a record into the database---
    public void insertDebris(Debris debris) 
    {
    	if(mOpen == false)
    		return;
    	
    	// we're going to set id too based on the record in database
    	debris.mDebrisId = mDb.insert(Debris.TABLE_NAME, null, debris.getDbFormat());
    	debris.putDatabaseStamp(this);
    	debris.mInDatabase = true;
    }
    
    public void updateDebris(Debris debris, String column, Object value) 
    {
    	if(mOpen == false)
    		return;
    	
    	ContentValues values = debris.getContentValue(column, value);
    	String[] selectionArgs = { String.valueOf(debris.mDebrisId) };
    	
    	// we're going to upgrade base on the assigned ID n database
    	mDb.update(Debris.TABLE_NAME, values, Debris.SQL_DEBRIS_SELECTION, selectionArgs);
    }
    
    //---retrieves all the records---
    private List<Debris> getAllDebrisRecords() 
    {
    	if(mOpen == false)
    		return null;
    	
    	Cursor cursor = mDb.query(Debris.TABLE_NAME, null, null, null, null, null, null);
    	List<Debris> debrisList = Debris.cursorToDebrisData(cursor);
    	
    	for (int i=0; i< debrisList.size();i++){
    		// mark that this one already in database
    		debrisList.get(i).mInDatabase = true;
    	}
    	
    	// Make sure to close the cursor
	    cursor.close();
	    
	    return debrisList;
    }

	public void clearTable() {
		// clear all data
		mDb.delete(Debris.TABLE_NAME, null, null);
	
	}

	public void needAddressUpdate(Debris debris) {
		if(mOpen == false)
    		return;
		
		updateDebris(debris, Debris.COLUMN_NAME_ADDRESS, debris.mAddress);
	}

	
}