package edu.depaul.x86azul;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * This class is used for adapter our specific database + table (debris)
 */
public class DbAdapter implements BaseColumns {
	
	// If you change the database schema, you MUST increment the database version.
    private final int DATABASE_VERSION = 1;
    private final String DATABASE_NAME = "Vrrdl.db";
     

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
    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    public DbAdapter(Context context) 
    {
    	// this will create database plus table if not exist
    	mDbHelper = new DatabaseHelper(context);
    }
    
    //---opens the database---
    public void open() throws SQLException 
    {
    	mDb = mDbHelper.getWritableDatabase();
    }

    //---closes the database---    
    public void close() 
    {
    	// this will also close any opened database
    	mDbHelper.close();
    }
    
    //---insert a record into the database---
    public void insertDebris(Debris debris) 
    {
    	// we're going to set id too based on the record in database
    	debris.mDebrisId = mDb.insert(Debris.TABLE_NAME, null, debris.getDbFormat());
    }
    
    //---retrieves all the records---
    public List<Debris> getAllDebrisRecords() 
    {
    	Cursor cursor = mDb.query(Debris.TABLE_NAME, null, null, null, null, null, null);
    	List<Debris> debrisList = Debris.cursorToDebrisData(cursor);
    	
    	// Make sure to close the cursor
	    cursor.close();
	    
	    return debrisList;
    }
}