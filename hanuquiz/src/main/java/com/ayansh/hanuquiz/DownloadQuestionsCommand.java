package com.ayansh.hanuquiz;

import android.net.Uri;
import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ResultObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;

public class DownloadQuestionsCommand extends Command {

	private QuestionManager qm;
	
	DownloadQuestionsCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		Application app = Application.getApplicationInstance();
		
		String appURL = app.appURL;
		String postURL = appURL + "/FetchQuestionsData.php";
		String questionList = "";
		
		InputStream is;
		InputStreamReader isr;
		
		qm = QuestionManager.getInstance();
		
		if(qm.toDownload.isEmpty()){
			Log.i(Application.TAG, "Nothing to download in Questions");
			return;
		}
		
		Iterator<Integer> iterator = qm.toDownload.listIterator();
		
		/*
		 * Right now, we are going to download all questions together...
		 * Later on, we can do it in packets of 25
		 */
		
		if(iterator.hasNext()){
			questionList = String.valueOf(iterator.next());
		}
		
		while(iterator.hasNext()){
			questionList += "," + String.valueOf(iterator.next());
		}
		
		Log.v(Application.TAG, "Downloading Questions...");

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try{

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");
			urlConnection.addRequestProperty("Referer","HanuQuizRocks");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("question_ids", questionList);
			String parameterQuery = uriBuilder.build().getEncodedQuery();

			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(parameterQuery);
			writer.flush();
			writer.close();
			os.close();

			urlConnection.connect();

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			if(builder.toString().contentEquals("null")){
				return;
			}

			// Parse the JSON Response
			JSONArray jsonResponse = new JSONArray(builder.toString());

			qm.toSave.clear();

			for(int i=0; i<jsonResponse.length(); i++) {

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

				for (int j = 0; j < options.length(); j++) {

					JSONObject option = options.getJSONObject(j);
					q.addOption(option.getInt("OptionId"), option.getString("OptionValue"));

				}

				for (int j = 0; j < answers.length(); j++) {

					JSONObject answer = answers.getJSONObject(j);
					q.addAnswer(answer.getInt("OptionId"));

				}

				for (int j = 0; j < metaData.length(); j++) {

					JSONObject meta = metaData.getJSONObject(j);
					q.addMetaData(meta.getString("MetaKey"), meta.getString("MetaValue"));

				}

				qm.toSave.add(q);
			}
		}
		finally {
			urlConnection.disconnect();
		}
		
		// Save Questions to db.
		boolean success = qm.saveQuestionsToDB();
		
		if(success){
			
			// If All success, then set sync time to now.
			app.addParameter("LastQuestionsSyncTime", String.valueOf((new Date()).getTime() - 2*60*1000));
			Log.v(Application.TAG, "All Questions downloaded successfully...");
			
		}
		else{
			// Some error occurred !
			Log.w(Application.TAG, "Error occured while downloading some questions !");
			if(app.isThisFirstUse()){
				// If first use, then set to HANU - Epoch
				app.addParameter("LastQuestionsSyncTime", "1349328720");
			}
			else{
				// leave whatever it was
			}
		}
		
		// Prepare result
		result.getData().putInt("QuestionsDownloaded", qm.toDownload.size());
		result.getData().putBoolean("ShowNotification", true);

	}

}