package com.ayansh.hanuquiz;
/*
 * This class object will contain the DB table and data to be updated
 * This will help me to ensure that either all or no data is updated to DB
 * And then I use a different method to insert data. 
 */

import android.content.ContentValues;

public class DBContentValues {
	
	public DBContentValues(){
		Content = new ContentValues();
		dbOperation = DBOperation.INSERT;
		TableName = "";
		where = "";
	}
	
	public enum DBOperation {
		INSERT, UPDATE, DELETE;
	}
	
	public DBOperation dbOperation;
	public String TableName;
	public ContentValues Content;
	public String where;

}