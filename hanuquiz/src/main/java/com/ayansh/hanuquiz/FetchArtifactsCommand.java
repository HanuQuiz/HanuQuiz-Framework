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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class FetchArtifactsCommand extends Command {

	FetchArtifactsCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		// Fetch the post artifacts.
		
		String baseUrl = Application.getApplicationInstance().appURL;
		String postURL = baseUrl + "/FetchArtifacts.php";
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String questionsSyncTime = Application.getApplicationInstance().getSettings().get("LastQuestionsSyncTime");
		String quizSyncTime = Application.getApplicationInstance().getSettings().get("LastQuizSyncTime");
		
		if(questionsSyncTime == null || questionsSyncTime.contentEquals("")){
			questionsSyncTime = "1349328720";	// HANU Epoch
		}
		
		if(quizSyncTime == null || quizSyncTime.contentEquals("")){
			quizSyncTime = "1349328720";		// HANU Epoch
		}
		
		Date questionSyncDate = new Date(Long.valueOf(questionsSyncTime));
		Date quizSyncDate = new Date(Long.valueOf(quizSyncTime));
		
		boolean justSynced = false;
		
		// If last sync was very recent, no need to check again.
		if( ( (new Date()).getTime() - questionSyncDate.getTime() ) < 5*60*1000 ){
			justSynced = true;
		}
		
		if(!justSynced){
			
			if( ( (new Date()).getTime() - quizSyncDate.getTime() ) < 5*60*1000 ){
				justSynced = true;
			}
			
		}
		
		if(justSynced){
			throw new Exception("Last sync done less than 5 min ago...");
		}
		
		Log.v(Application.TAG, "Trying to fetch artifacts.");
		
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		questionsSyncTime = df.format(questionSyncDate);
		quizSyncTime = df.format(quizSyncDate);
		
		Log.i(Application.TAG, "Last Question Sync done at (GMT): " + questionsSyncTime);
		Log.i(Application.TAG, "Last Quiz Sync done at (GMT): " + quizSyncTime);

		// Get the sync tags
		String syncTag = Application.getApplicationInstance().getSettings().get("SyncTag");
		JSONObject syncTags = new JSONObject();
		syncTags.put("meta_key", "sync");
		syncTags.put("meta_value", syncTag);
		JSONArray metaData = new JSONArray();
		metaData.put(syncTags);


		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try{

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");
			urlConnection.addRequestProperty("Referer","HanuQuizRocks");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("question_sync_time", questionsSyncTime)
					.appendQueryParameter("quiz_sync_time", quizSyncTime)
					.appendQueryParameter("meta_data", metaData.toString());

			String parameterQuery = uriBuilder.build().getEncodedQuery();

			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			writer.write(parameterQuery);
			writer.flush();
			writer.close();
			os.close();

			urlConnection.connect();

			// Get Input Stream Reader.

			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}

			if(builder.toString().contentEquals("null")){
				//Log.i(Application.TAG, "Got null response from Artifacts !");
				return;
			}

			// Parse the result.
			//Log.i(Application.TAG, "Response: " + builder.toString());
			JSONObject jsonResponse = new JSONObject(builder.toString());

			int id;
			Date createdAt;
			HashMap<Integer,Date> questionArtifactsList = new HashMap<Integer,Date>();
			HashMap<Integer,Date> quizArtifactsList = new HashMap<Integer,Date>();

			JSONArray questions = jsonResponse.getJSONArray("questions");
			for(int i=0; i<questions.length(); i++){

				JSONObject artifact = questions.getJSONObject(i);

				id = artifact.getInt("ID");
				createdAt = df.parse(artifact.getString("CreatedAt"));
				questionArtifactsList.put(id, createdAt);

			}

			JSONArray quiz = jsonResponse.getJSONArray("quizzes");
			for(int i=0; i<quiz.length(); i++){

				JSONObject quizArtifact = quiz.getJSONObject(i);

				id = quizArtifact.getInt("QuizId");
				createdAt = df.parse(quizArtifact.getString("CreatedAt"));
				quizArtifactsList.put(id, createdAt);

			}

			// Filter Question Artifacts for download
			//Log.v(Application.TAG, questions.length() + " Question Artifacts fetched, will filter now...");
			QuestionManager.getInstance().filterArtifactsForDownload(questionArtifactsList);

			//Log.v(Application.TAG, quiz.length() + " Quiz Artifacts fetched, will filter now...");
			QuizManager.getInstance().filterArtifactsForDownload(quizArtifactsList);

		}
		finally {
			urlConnection.disconnect();
		}
	}

}