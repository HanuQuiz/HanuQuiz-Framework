package com.ayansh.hanuquiz;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class QuestionManager {

	private static QuestionManager instance;
	
	private HashMap<Integer, Question> questionList;
	
	ArrayList<Integer> toDownload;
	ArrayList<Question> toSave;
	
	public static QuestionManager getInstance(){
		
		if(instance == null){
			instance = new QuestionManager();
		}
		
		return instance;
	}
	
	private QuestionManager(){
		
		questionList = new HashMap<Integer,Question>();
		toDownload = new ArrayList<Integer>();
		toSave = new ArrayList<Question>();
	}
	
	public void clearQuestionList(){
		questionList.clear();
	}
	
	public void addQuestionToList(Question question){
		questionList.put(question.getId(), question);
	}
	
	void filterArtifactsForDownload(HashMap<Integer, Date> artifactsList) {
		/*
		 * Load Artifacts from DB
		 */
		toDownload.clear();
		
		Iterator<Integer> i = artifactsList.keySet().iterator();
		String questionIds = "";
		
		if(i.hasNext()){
			questionIds = String.valueOf(i.next());
		}
		
		while(i.hasNext()){
			questionIds += "," + String.valueOf(i.next());
		}
		
		//Log.i(Application.TAG, "Fetching DB artifacts for: " + questionIds);
		HashMap<Integer, Date> dbArtifacts = ApplicationDB.getInstance().getQuestionArtifacts(questionIds);
		
		Entry<Integer,Date> set;
		Date dbDate, date;
		
		Iterator<Entry<Integer,Date>> iterator = artifactsList.entrySet().iterator();
		
		while(iterator.hasNext()){
			
			set = iterator.next();
			date = set.getValue();
			dbDate = dbArtifacts.get(set.getKey());
			
			if(dbDate == null || date.compareTo(dbDate) > 0){
				// DB entry is older. So we must update this.
				toDownload.add(set.getKey());
			}
			else{
				//Log.w(Application.TAG, "Entry with DB date:" + dbDate.toString() + " is removed");
			}
			
		}
		
	}

	boolean saveQuestionsToDB() {
		
		Iterator<Question> i = toSave.iterator();
		boolean allSuccess = true;
		
		while(i.hasNext()){
			
			try {
				// Save to DB
				i.next().saveToDB();
				
			} catch (Exception e) {
				allSuccess = false;
				Log.w(Application.TAG, e.getMessage());
			}
			
		}
		
		return allSuccess;
	}
	
	public List<Question> getQuestionList() {
		
		ArrayList<Question> list = new ArrayList<Question>();
		list.addAll(questionList.values());
		
		return list;
		
	}
	
	public boolean deleteQuestion(int questionID){
		
		boolean success = questionList.get(questionID).delete();
		
		if(success){
			questionList.remove(questionID);
		}
		
		return success;
		
	}

}