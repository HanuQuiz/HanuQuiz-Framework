package com.ayansh.hanuquiz;

import android.content.ContentValues;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Question {

	private int id, level, choiceType;
	private String question, createdAt;
	private HashMap<Integer,String> options;
	private List<Integer> answers;
	private List<String> tags;
	private String myAnswer;
	
	Question(){
		
		id = 0;
		question = "";
		options = new HashMap<Integer,String>();
		answers = new ArrayList<Integer>();
		tags = new ArrayList<String>();
		myAnswer = "";
	}
	
	@Override
	public String toString(){
		return String.valueOf(id) + "/" + question;
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @param level the level to set
	 */
	void setLevel(int level) {
		this.level = level;
	}

	/**
	 * @return the choiceType
	 */
	public int getChoiceType() {
		return choiceType;
	}

	/**
	 * @param choiceType the choiceType to set
	 */
	void setChoiceType(int choiceType) {
		this.choiceType = choiceType;
	}

	/**
	 * @return the question
	 */
	public String getQuestion() {
		return question;
	}
	/**
	 * @param question the question to set
	 */
	void setQuestion(String question) {
		this.question = question;
	}
	
	public String getCreatedAt() {
		return createdAt;
	}

	void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * @return the options
	 */
	public HashMap<Integer,String> getOptions() {
		return options;
	}
	/**
	 * @param options the options to set
	 */
	void addOption(int optionID, String optionValue) {
		options.put(optionID, optionValue);
	}
	/**
	 * @return the answers
	 */
	public List<Integer> getAnswers() {
		return answers;
	}
	/**
	 * @param answers the answers to set
	 */
	void addAnswer(int answer) {
		answers.add(answer);
	}
	/**
	 * @return the Tags
	 */
	public List<String> getTags() {
		return tags;
	}
	/**
	 * @param add the metaData to set
	 */
	void addMetaData(String key, String value) {
		
		if(key.contentEquals("tag")){
			tags.add(value);
		}
		
	}

	void saveToDB() throws Exception{
		/*
		 * If Error occurs during save, then throw Exception
		 * Remember to maintain the transactional integrity. Either all or none.
		 * Call DB Method executeDBTransactions - It will ensure either all or none.
		 */		
			
		ApplicationDB Appdb = ApplicationDB.getInstance();
		
		// -- Save Question Table --
		List<DBContentValues> transactionData = new ArrayList<DBContentValues>();
		DBContentValues QuestionData = new DBContentValues();

		QuestionData.TableName = ApplicationDB.QuestionsTable;
		QuestionData.Content = new ContentValues();
		QuestionData.Content.put("ID", id);
		QuestionData.Content.put("Question", question);
		QuestionData.Content.put("Level", level);
		QuestionData.Content.put("Choice", choiceType);
		QuestionData.Content.put("CreatedAt", createdAt);
		
		// Check if question exists. If so, delete it and then re-insert
		if (Appdb.checkQuestionExists(id)) {
			prepareForUpdate();
			QuestionData.where = "ID=" + String.valueOf(id);
			QuestionData.Content.remove("ID");
			QuestionData.dbOperation = DBContentValues.DBOperation.UPDATE;
		}
		else{
			QuestionData.dbOperation = DBContentValues.DBOperation.INSERT;
		}
		
		transactionData.add(QuestionData);

		// -- Save Options Table --
		Iterator<Integer> iter_options = options.keySet().iterator();

		while (iter_options.hasNext()) {
			
			DBContentValues OptionsData = new DBContentValues();
			OptionsData.TableName = ApplicationDB.OptionsTable;
			OptionsData.Content = new ContentValues();

			Integer option_id = (Integer) iter_options.next();
			String option_value = options.get(option_id);

			OptionsData.Content.put("QuestionId", id);
			OptionsData.Content.put("OptionId", option_id);
			OptionsData.Content.put("OptionValue", option_value);

			OptionsData.dbOperation = DBContentValues.DBOperation.INSERT;
			transactionData.add(OptionsData);
		}

		// -- Save Answers Table --
		Iterator<Integer> iter_ans = answers.iterator();

		while (iter_ans.hasNext()) {
			
			DBContentValues AnswersData = new DBContentValues();
			AnswersData.TableName = ApplicationDB.AnswersTable;
			AnswersData.Content = new ContentValues();
			
			Integer answer_id = (Integer) iter_ans.next();
			AnswersData.Content.put("QuestionId", id);
			AnswersData.Content.put("OptionId", answer_id);
			
			AnswersData.dbOperation = DBContentValues.DBOperation.INSERT;
			transactionData.add(AnswersData);
		}

		// -- Save Tags (Metadata) --
		
		Iterator<String> iter_tag = tags.iterator();

		while (iter_tag.hasNext()) {
			
			DBContentValues MetaData = new DBContentValues();
			MetaData.TableName = ApplicationDB.QuestionMetaDataTable;
			MetaData.Content = new ContentValues();
			
			String tag = (String) iter_tag.next();
			MetaData.Content.put("QuestionId", id);
			MetaData.Content.put("MetaKey", "tag");
			MetaData.Content.put("MetaValue", tag);
			
			MetaData.dbOperation = DBContentValues.DBOperation.INSERT;
			transactionData.add(MetaData);
			
		}

		try {

			Appdb.executeDBTransaction(transactionData);

		} catch (Exception e) {
			throw e;
		}
				
	}
	
	public void updateMyAnswer(String answer){
		
		if(answer != null){
			myAnswer = answer;
		}
		
	}
	
	private void prepareForUpdate(){
		
		/*
		 * Delete Option, Answers, MetaData
		 * DO NOT delete other stuff.
		 */
		List<DBContentValues> transactionData = new ArrayList<DBContentValues>();
			
		// - Options Table
		DBContentValues OptionsData = new DBContentValues();
		OptionsData.TableName = ApplicationDB.OptionsTable;
		OptionsData.Content = new ContentValues();
		OptionsData.where = "QuestionId = " + id;
		
		OptionsData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(OptionsData);
		
		// - Answers Table
		DBContentValues AnswersData = new DBContentValues();
		AnswersData.TableName = ApplicationDB.AnswersTable;
		AnswersData.Content = new ContentValues();
		AnswersData.where = "QuestionId = " + id;
				
		AnswersData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(AnswersData);
		
		// - MetaData Table
		DBContentValues MetaData = new DBContentValues();
		MetaData.TableName = ApplicationDB.QuestionMetaDataTable;
		MetaData.Content = new ContentValues();
		MetaData.where = "QuestionId = " + id;
				
		MetaData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(MetaData);

		try {
			ApplicationDB Appdb = ApplicationDB.getInstance();
			Appdb.executeDBTransaction(transactionData);

		} catch (Exception e) {
			Log.e(Application.TAG, e.getMessage(), e);
		}
		
	}
	
	public String getMyAnswer()
	{
		return myAnswer;
	}
	
	boolean evaluateQuestion(){
		
		/*
		 * The users answer is available as an attribute
		 * Compare with the correct answers and evaluate
		 * If correct then return true, else false
		 */
		
		if(myAnswer == null || myAnswer == "") return false; // NO User Answer Found
		
		String[] userAnswers  = myAnswer.split(",");
		
		boolean found = true; //defaulted found == true
		
		Iterator <Integer>Iter = answers.iterator();
		
		//IF user answers contains all correct answers FOUND = TRUE
		if(Iter.hasNext())
		{
			do{
				if( !Arrays.asList(userAnswers).contains(Iter.next().toString()) ) found = false;
			}while(Iter.hasNext());
			
		}
		else found = false;
		
		
		//Continue if above check was successful, additional check performed below...
		if(found == false || userAnswers.length != answers.size() ) // Check if number of "user answers" and "correct answers" are the same 
		{
			found = false;
		}
		else found = true;
	
		
		return found;
	}

	public String getHTML() {
		
		String html = "";
		
		html = "<html><body>" +
				question + "<br>" +
				"</body></html>";
		
		return html;
	}

	boolean delete() {
		
		List<DBContentValues> transactionData = new ArrayList<DBContentValues>();
		
		// - Options Table
		DBContentValues OptionsData = new DBContentValues();
		OptionsData.TableName = ApplicationDB.OptionsTable;
		OptionsData.Content = new ContentValues();
		OptionsData.where = "QuestionId = " + id;
		
		OptionsData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(OptionsData);
		
		// - Answers Table
		DBContentValues AnswersData = new DBContentValues();
		AnswersData.TableName = ApplicationDB.AnswersTable;
		AnswersData.Content = new ContentValues();
		AnswersData.where = "QuestionId = " + id;
				
		AnswersData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(AnswersData);
		
		// - MetaData Table
		DBContentValues MetaData = new DBContentValues();
		MetaData.TableName = ApplicationDB.QuestionMetaDataTable;
		MetaData.Content = new ContentValues();
		MetaData.where = "QuestionId = " + id;
				
		MetaData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(MetaData);
		
		// - Question Table
		DBContentValues QuestionData = new DBContentValues();
		QuestionData.TableName = ApplicationDB.QuestionsTable;
		QuestionData.Content = new ContentValues();
		QuestionData.where = "ID = " + id;

		QuestionData.dbOperation = DBContentValues.DBOperation.DELETE;
		transactionData.add(QuestionData);

		try {
			
			ApplicationDB Appdb = ApplicationDB.getInstance();
			Appdb.executeDBTransaction(transactionData);
			return true;

		} catch (Exception e) {
			Log.e(Application.TAG, e.getMessage(), e);
			return false;
		}
	}
	
}