package com.ayansh.hanuquiz;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class QuizManager {

	private static QuizManager instance;
	
	private HashMap<Integer, Quiz> quizList;
	List<Integer> toDownload;
	ArrayList<Quiz> toSave;
	
	public static QuizManager getInstance(){
		
		if(instance == null){
			
			instance = new QuizManager();
			
		}
		
		return instance;
	}
	
	private QuizManager(){
		
		quizList = new HashMap<Integer,Quiz>();
		toDownload = new ArrayList<Integer>();
		toSave = new ArrayList<Quiz>();
		
	}
	
	void clearQuizList(){
		quizList.clear();
	}
	
	void addQuizToList(Quiz quiz){
		quizList.put(quiz.getQuizId(), quiz);
	}
	
	public Quiz getQuizById(int id){
		return quizList.get(id);
	}
	
	void filterArtifactsForDownload(HashMap<Integer, Date> artifactsList){
		
		toDownload.clear();
		
		Iterator<Integer> i = artifactsList.keySet().iterator();
		String quizIds = "";
		
		if(i.hasNext()){
			quizIds = String.valueOf(i.next());
		}
		
		while(i.hasNext()){
			quizIds += "," + String.valueOf(i.next());
		}
		
		//Log.i(Application.TAG, "Fetching DB artifacts for: " + quizIds);
		HashMap<Integer, Date> dbArtifacts = ApplicationDB.getInstance().getQuizArtifacts(quizIds);
		
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

	boolean saveQuizToDB() {
		
		Iterator<Quiz> i = toSave.iterator();
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

	List<Quiz> getQuizList() {
		
		ArrayList<Quiz> list = new ArrayList<Quiz>();
		list.addAll(quizList.values());
		
		return list;
		
	}
	
}