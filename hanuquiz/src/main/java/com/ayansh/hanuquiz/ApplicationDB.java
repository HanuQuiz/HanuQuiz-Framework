package com.ayansh.hanuquiz;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ApplicationDB extends SQLiteOpenHelper{

	protected static ApplicationDB appDB;
	
	static final String SettingsTable = "Settings";
	static final String QuizTable = "Quiz";
	protected static final String QuestionsTable = "Questions";
	static final String OptionsTable = "Options";
	static final String AnswersTable = "Answers";
	static final String QuestionMetaDataTable = "QuestionMetaData";
	static final String QuizMetaDataTable = "QuizMetaData";
	static final String MyAnswersTable = "MyAnswers";

	protected SQLiteDatabase data_base;
	
	static ApplicationDB getInstance(Context context, int dbVersion, String dbName){
			
		if(appDB == null){
			appDB = new ApplicationDB(context, dbVersion, dbName);
		}
		
		return appDB;
	}
	
	static ApplicationDB getInstance(){
		return appDB;
	}
	
	protected ApplicationDB(Context context, int dbVersion, String dbName) {
		
		super(context, dbName, null, dbVersion);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		/*
		 * Create Questions, Options, Answers, MetaData tables.
		 * 
		 * Questions will be a FTS4 table. Others will be normal tables.
		 * Read about FTS : http://www.sqlite.org/fts3.html
		 * 
		 * As an example, I am creating a Settings Table
		 * Note: Use constants as table names
		 */
		
		String createQuestionsTable = "CREATE VIRTUAL TABLE " + QuestionsTable + "  USING fts3(" + //Questions table - FTS4 Virtual Table
		        "ID INTEGER PRIMARY KEY ASC, " +	//Question Id
				"Question VARCHAR(200), " +			//Question Text
		        "Level INT, " +						//Level 
				"Choice INT, " +					//Choice
				"CreatedAt DATETIME " +				//Set the default value of the Created at time stamp
		        ")";
		        
		String createAnswersTable = "CREATE TABLE " + AnswersTable + " (" + 
				"QuestionId INT, " + 		// Question ID
				"OptionId INT " + 			// Option ID
				")";
		        
		String createSettingsTable = "CREATE TABLE " + SettingsTable + " (" + 
				"ParamName VARCHAR(20), " + 		// Parameter Name
				"ParamValue VARCHAR(20)" + 			// Parameter Value
				")";
		
		String createOptionsTable = "CREATE TABLE " + OptionsTable + " (" + 
				"QuestionId INT, " + 		// Question ID
				"OptionId INT, " + 			// Option ID
				"OptionValue VARCHAR(20) " + 			// Option Value
				")";
		
		String createQuestionsMetaTable = "CREATE TABLE " + QuestionMetaDataTable + " (" + 
				"QuestionId INT, " + 		// Question ID
				"MetaKey VARCHAR(20), " + 			// Meta Key
				"MetaValue VARCHAR(20) " + 			// Meta Value
				")";
		
		String createQuizMetaTable = "CREATE TABLE " + QuizMetaDataTable + " (" + 
				"QuizId INT, " + 		// Question ID
				"MetaKey VARCHAR(20), " + 			// Meta Key
				"MetaValue VARCHAR(20) " + 			// Meta Value
				")";
		
		String createQuizTable = "CREATE TABLE " + QuizTable + " (" + 
				"ID INTEGER PRIMARY KEY ASC, " + // Question ID
				"Description VARCHAR, " + 		// Quiz Description
				"Level INT, " + 					// Level
				"Count INT, " + 					// Count
				"QuestionIds VARCHAR(100), " + 	// Question IDs seperated by comma
				"Status INT, " + 					// Status of Quiz
				"MyScore INT, " + 					// Current Quiz Score
				"CreatedAt DATETIME, " +
				"PlayedAt DATETIME " +
				")";
		
		String createMyAnswersTable = "CREATE TABLE " + MyAnswersTable + " (" + 
				"QuizId INTEGER , " + // Quiz ID
				"QuestionId INTEGER , " + // Question Id
				"MyAnswers VARCHAR(100), "  +
				"PRIMARY KEY (QuizId, QuestionId)" +
				")";
		
		// create a new table - if not existing
		try {
			// Create Tables.
			Log.i(Application.TAG, "Creating Tables for Version:" + String.valueOf(db.getVersion()));
				
			db.execSQL(createQuestionsTable);
			db.execSQL(createAnswersTable);			
			db.execSQL(createOptionsTable);
			db.execSQL(createQuestionsMetaTable);
			db.execSQL(createQuizTable);
			db.execSQL(createQuizMetaTable);
			db.execSQL(createSettingsTable);
			db.execSQL(createMyAnswersTable);			
						
			Log.i(Application.TAG, "Tables created successfully");

		} catch (SQLException e) {
			Log.e(Application.TAG, e.getMessage(), e);
		}
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Nothing to do
		switch(oldVersion){
		
		case 1:
			/*
			 * This is a dummy (technical) upgrade. No real changes are
			 * done in the table schemas.
			 *  
			 * This code is added due to a bug until version 5(1.0.3)
			 * Due to the bug, the settings were re-inserted in DB 
			 * every time the app was launched. 
			 * As a result, we had too many entries in the DB table...
			 * and poor performance on the launch of app.
			 * 
			 * This code will consolidate settings. Notice that we are
			 * selecting only distinct values
			 */
			
			String settingsBackup = "CREATE TABLE SETTINGSBACKUP AS SELECT * FROM " + SettingsTable + ";";
			String dropSettings = "DROP TABLE " + SettingsTable + ";";
			String createSettings = "CREATE TABLE " + SettingsTable + " AS SELECT DISTINCT * FROM SETTINGSBACKUP;";
			String dropBackup = "DROP TABLE SETTINGSBACKUP;";
			
			try {
				// Upgrading database to version 2
				Log.i(Application.TAG, "Upgrading DB from version 1 to 2");
					
				db.execSQL(settingsBackup);
				db.execSQL(dropSettings);			
				db.execSQL(createSettings);
				db.execSQL(dropBackup);		
							
				Log.i(Application.TAG, "Upgrade successfully");

			} catch (SQLException e) {
				// Oops !!
				Log.e(Application.TAG, e.getMessage(), e);
			}
			
			/*
			 * Very important - No break statement here !
			 */
		
		}
	}
	
	protected void openDBForWriting(){
		data_base = getWritableDatabase();
 	}

	public synchronized void executeDBTransaction(List<DBContentValues> dbData) throws Exception{
		
		try{
			
			data_base.beginTransaction();
			
			Iterator<DBContentValues> i = dbData.iterator();
			
			while(i.hasNext()){
				
				DBContentValues data = i.next();
				
				if (data.dbOperation == DBContentValues.DBOperation.INSERT) {
					
					data_base.insertOrThrow(data.TableName, null, data.Content);

				}

				if (data.dbOperation == DBContentValues.DBOperation.UPDATE) {
					
					data_base.update(data.TableName, data.Content, data.where, null);

				}

				if (data.dbOperation == DBContentValues.DBOperation.DELETE) {
					
					data_base.delete(data.TableName, data.where, null);

				}

			}
			
			data_base.setTransactionSuccessful();
			data_base.endTransaction();
			
		} catch (Exception e) {

			data_base.endTransaction();
			throw e;

		}
		
	}
		
	synchronized void loadSettings(){
		
		/* 
		 * Select from Settings table and load into memory.
		 * Populate the Application->Settings attribute
		 */
		
		Application app = Application.getApplicationInstance();
		String ParamName, ParamValue;
		Cursor SettingsCursor = data_base.query(SettingsTable, null, null, null, null, null, null);
		if (SettingsCursor.moveToFirst()) {
			
			Log.i(Application.TAG, SettingsCursor.getCount() + " settings loaded");

			do {
				ParamName = SettingsCursor.getString(SettingsCursor.getColumnIndex("ParamName"));
				ParamValue = SettingsCursor.getString(SettingsCursor.getColumnIndex("ParamValue"));
				app.Settings.put(ParamName, ParamValue);
				
			} while (SettingsCursor.moveToNext());
		}
		
		SettingsCursor.close();
		
	}
	
	synchronized void loadQuizListByLevel(int level) {
		/*
		 * populate them in the QuizManager.quizList
		 * Populate via method: addQuizToList
		 */
		QuizManager qmgr = QuizManager.getInstance();
		String questionIds;
		int questionId, index, quizId;
		String selection = "Level = '" + level + "'";

		Cursor qCursor = data_base.query(QuizTable, null, selection, null, null, null, "ID ASC");
		if (qCursor.moveToFirst()) {

			do {
				Quiz quiz_obj = new Quiz();

				questionIds = "";
				questionIds = qCursor.getString(qCursor.getColumnIndex("QuestionIds"));
				quizId = qCursor.getInt(qCursor.getColumnIndex("ID"));

				quiz_obj.setLevel(level);
				quiz_obj.setQuizId(quizId);
				quiz_obj.setDescription(qCursor.getString(qCursor.getColumnIndex("Description")));
				quiz_obj.setScore(qCursor.getInt(qCursor.getColumnIndex("MyScore")));
				
				String quizStatus = qCursor.getString(qCursor.getColumnIndex("Status"));
				if(quizStatus != null){
					if(quizStatus.contentEquals(Quiz.QuizStatus.Completed.toString())){
						quiz_obj.setStatus(Quiz.QuizStatus.Completed);
					}
					if(quizStatus.contentEquals(Quiz.QuizStatus.Paused.toString())){
						quiz_obj.setStatus(Quiz.QuizStatus.Paused);
					}
					if(quizStatus.contentEquals(Quiz.QuizStatus.NotStarted.toString())){
						quiz_obj.setStatus(Quiz.QuizStatus.NotStarted);
					}
				}

				index = 0;
				do {
					index = questionIds.indexOf(",");
					if (index <= 0) {
						questionId = Integer.parseInt(questionIds); // No comma found, thats the only questionId left
						quiz_obj.addQuestion(questionId);
						break;
					} else {
						questionId = Integer.parseInt(questionIds.substring(0,index));
						quiz_obj.addQuestion(questionId);
						questionIds = questionIds.substring(index + 1,questionIds.length()); // Shrink the question Ids to fetch the next
					}
				} while (index > 0);

				qmgr.addQuizToList(quiz_obj);
				
				// Load Quiz Meta Data
				loadQuizMetaData(quiz_obj);
				
				quiz_obj = null; // Removing object properties, since we're in loop

			} while (qCursor.moveToNext());
		}
		
		qCursor.close();
			
	}		
	
	private void loadQuizMetaData(Quiz quiz) {
		
		String selection = "QuizId='" + quiz.getQuizId() + "'";
		
		String metaKey, metaValue;
		Cursor mCursor = data_base.query(QuizMetaDataTable, null, selection, null, null, null, null);
		
		if (mCursor.moveToFirst()) {

			do {
				
				metaKey = mCursor.getString(mCursor.getColumnIndex("MetaKey"));
				metaValue = mCursor.getString(mCursor.getColumnIndex("MetaValue"));
				quiz.addMetaData(metaKey, metaValue);
				
			} while (mCursor.moveToNext());
		}

		mCursor.close();
		
	}

	synchronized List<Question> getQuestionsByIds(String questionIds){
		
		// questionIds is CSV
		String selection = "ID IN (" + questionIds + ")";
		
		List<Question> list = new ArrayList<Question>();
		
		Question question;
		Cursor qCursor = data_base.query(QuestionsTable, null, selection, null, null, null, null);
		
		if(qCursor.moveToFirst()){
			
			do{
				
				question = buildQuestionObject(qCursor);
				list.add(question);
				
			}while(qCursor.moveToNext());
			
		}
		
		qCursor.close();
		
		return list;
	}
	
	protected Question buildQuestionObject(Cursor qCursor) {
		
		Question question = new Question();
		
		question.setId(qCursor.getInt(qCursor.getColumnIndex("ID")));
		question.setQuestion(qCursor.getString(qCursor.getColumnIndex("Question")));
		question.setLevel(qCursor.getInt(qCursor.getColumnIndex("Level")));
		question.setChoiceType(qCursor.getInt(qCursor.getColumnIndex("Choice")));
		
		String selection = "QuestionId='" + question.getId() + "'";
		
		// Select Options
		Cursor oCursor = data_base.query(OptionsTable, null, selection, null, null, null, "OptionId ASC");
		if(oCursor.moveToFirst()){
			
			do {
				question.addOption(oCursor.getInt(oCursor.getColumnIndex("OptionId")),
						oCursor.getString(oCursor.getColumnIndex("OptionValue")));
			} while (oCursor.moveToNext());
		}
		
		oCursor.close();

		// Select Answers
		Cursor aCursor = data_base.query(AnswersTable, null, selection, null, null, null, null);
		if (aCursor.moveToFirst()) {

			do {
				question.addAnswer(aCursor.getInt(aCursor.getColumnIndex("OptionId")));
			} while (aCursor.moveToNext());
		}

		aCursor.close();
		
		// Select Meta Data
		String metaKey, metaValue;
		Cursor mCursor = data_base.query(QuestionMetaDataTable, null, selection, null, null, null, null);
		if (mCursor.moveToFirst()) {

			do {
				
				metaKey = mCursor.getString(mCursor.getColumnIndex("MetaKey"));
				metaValue = mCursor.getString(mCursor.getColumnIndex("MetaValue"));
				question.addMetaData(metaKey, metaValue);
				
			} while (mCursor.moveToNext());
		}

		mCursor.close();

		return question;
	}

	HashMap<Integer, Date> getQuizArtifacts(String quizIds) {
		
		HashMap<Integer, Date> artifactList = new HashMap<Integer, Date>();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String[] columns = {"ID","CreatedAt"};
		String selection = "ID in (" + quizIds + ")";
		
		Cursor cursor = data_base.query(QuizTable, columns, selection, null, null, null, null);
		
		if(cursor.moveToFirst()){
			
			do{
				
				try {
					artifactList.put(cursor.getInt(0), df.parse(cursor.getString(1)));
				} catch (ParseException e) {
					Log.w(Application.TAG, e.getMessage());
				}
				
			}while(cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return artifactList;
		
	}
	
	HashMap<Integer, Date> getQuestionArtifacts(String questionIds) {

		HashMap<Integer, Date> artifactList = new HashMap<Integer, Date>();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String[] columns = {"ID","CreatedAt"};
		String selection = "ID in (" + questionIds + ")";;
		
		Cursor cursor = data_base.query(QuestionsTable, columns, selection, null, null, null, null);
		
		if(cursor.moveToFirst()){
			
			do{
				
				try {
					artifactList.put(cursor.getInt(0), df.parse(cursor.getString(1)));
				} catch (ParseException e) {
					Log.w(Application.TAG, e.getMessage());
				}
				
			}while(cursor.moveToNext());
			
		}
		
		cursor.close();
		
		return artifactList;
	}

	HashMap<Integer, String> getUserAnswers(int quizId,	String questionIds) {
		
		HashMap<Integer,String> userAnswers = new HashMap<Integer,String>();
		
		/*
		 * Read the User Answers table
		 * Then populate the hash map and return
		 */
		
		String myAnswers,selection;
		Integer questionId;
		selection = "QuizId = '"+ quizId +"' AND QuestionId IN (" + questionIds + ")";
		
		Cursor ansCursor = data_base.query(MyAnswersTable, null, selection, null, null, null, null);
		if (ansCursor.moveToFirst()) {

			do {
				myAnswers = ""; //defaulted
				myAnswers = ansCursor.getString(ansCursor.getColumnIndex("MyAnswers"));
				questionId = ansCursor.getInt(ansCursor.getColumnIndex("QuestionId"));
				userAnswers.put(questionId, myAnswers);
			} while (ansCursor.moveToNext());
		}

		ansCursor.close();

		return userAnswers;
	}

	boolean checkQuestionExists(int questionID) {
		
		String[] columns = {"ID"};
		String selection = "ID=" + questionID ;
		
		Cursor cursor = data_base.query(QuestionsTable, columns, selection, null, null, null, null);
		
		int count = cursor.getCount();
		cursor.close();
		
		if(count > 0){
			return true;
		}
		else{
			return false;
		}
		
	}

	boolean checkQuizExists(int quizId) {
		
		String[] columns = {"ID"};
		String selection = "ID=" + quizId ;
		
		Cursor cursor = data_base.query(QuizTable, columns, selection, null, null, null, null);
		
		int count = cursor.getCount();
		cursor.close();
		
		if(count > 0){
			return true;
		}
		else{
			return false;
		}
	}
	
}