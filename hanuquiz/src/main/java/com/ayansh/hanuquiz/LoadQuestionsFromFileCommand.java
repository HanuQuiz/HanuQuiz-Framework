package com.ayansh.hanuquiz;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LoadQuestionsFromFileCommand extends Command {

	LoadQuestionsFromFileCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		try{
			
			Context c = Application.getApplicationInstance().context;
			AssetManager assetManager = c.getAssets();
			
			Log.v(Application.TAG, "Loading default qiestions from file");
			InputStream is = assetManager.open("default_data.json");
			InputStreamReader isr;

			// Get Input Stream Reader.
			isr = new InputStreamReader(is);

			BufferedReader reader = new BufferedReader(isr);
			
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			if(builder.toString().contentEquals("null")){
				return;
			}

			// Parse the JSON Response
			JSONObject jsonResponse = new JSONObject(builder.toString());
			
			JSONArray questions = jsonResponse.getJSONArray("question_list");
			loadQuestions(questions);
			
			JSONArray quizzes = jsonResponse.getJSONArray("quiz_list");
			loadQuizzes(quizzes);
			
			result.setResultCode(200);
			ProgressInfo pi = new ProgressInfo("Show UI");
			publishProgress(pi);		
			
		}catch (Exception e){
			// We ignore this :D
			Log.e(Application.TAG, e.getMessage(), e);
		}
		
	}

	private void loadQuestions(JSONArray jsonResponse) throws Exception {
		
		QuestionManager qm = QuestionManager.getInstance();
		
		qm.toSave.clear();
		
		for(int i=0; i<jsonResponse.length(); i++){
			
			JSONObject questionData = jsonResponse.getJSONObject(i);
			
			JSONObject question = questionData.getJSONObject("question");
			JSONArray options = questionData.getJSONArray("options");
			JSONArray answers = questionData.getJSONArray("answers");
			JSONArray metaData = questionData.getJSONArray("meta");
			
			Question q = new Question();
			
			q.setId(question.getInt("ID"));
			q.setLevel(question.getInt("Level"));
			q.setChoiceType(question.getInt("ChoiceType"));
			q.setQuestion(question.getString("Question"));
			q.setCreatedAt(question.getString("CreatedAt"));
			
			for(int j=0; j<options.length(); j++){
				
				JSONObject option = options.getJSONObject(j);
				q.addOption(option.getInt("OptionId"), option.getString("OptionValue"));
				
			}
			
			for(int j=0; j<answers.length(); j++){
				
				JSONObject answer = answers.getJSONObject(j);
				q.addAnswer(answer.getInt("OptionId"));
				
			}
			
			for(int j=0; j<metaData.length(); j++){
				
				JSONObject meta = metaData.getJSONObject(j);
				q.addMetaData(meta.getString("MetaKey"), meta.getString("MetaValue"));
				
			}
			
			qm.toSave.add(q);
			
		}

		// Save Questions to db.
		qm.saveQuestionsToDB();
		
	}

	private void loadQuizzes(JSONArray jsonResponse) throws Exception {
		
		QuizManager qm = QuizManager.getInstance();
		
		qm.toSave.clear();
		
		for(int i=0; i<jsonResponse.length(); i++){
			
			JSONObject jsonData = jsonResponse.getJSONObject(i);
			
			JSONObject quizData = jsonData.getJSONObject("quiz");
			JSONArray metaData = jsonData.getJSONArray("meta");
			
			Quiz q = new Quiz();
			
			q.setQuizId(quizData.getInt("QuizId"));
			q.setDescription(quizData.getString("Description"));
			q.setLevel(quizData.getInt("Level"));
			q.setCreatedAt(quizData.getString("CreatedAt"));

			String questions = quizData.getString("QuestionIds");
			String[] questionList = questions.split(",");
			for(int j=0; j<questionList.length; j++){
				q.addQuestion(Integer.valueOf(questionList[j]));
			}
			
			for (int j = 0; j < metaData.length(); j++) {

				JSONObject meta = metaData.getJSONObject(j);
				q.addMetaData(meta.getString("MetaKey"),meta.getString("MetaValue"));

			}
			
			qm.toSave.add(q);
			
		}
		
		// Save Quizzes to db.
		qm.saveQuizToDB();
		
	}
}