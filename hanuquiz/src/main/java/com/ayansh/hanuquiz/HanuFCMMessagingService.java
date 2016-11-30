package com.ayansh.hanuquiz;

import android.util.Log;

import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.MultiCommand;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public abstract class HanuFCMMessagingService extends FirebaseMessagingService implements Invoker {
	
	protected ResultObject processMessage(RemoteMessage remoteMessage) {
		// Message received with Intent
		
		// Initialize the application
		Application app = Application.getApplicationInstance();
		app.setContext(getApplicationContext());

		Map<String,String> data = remoteMessage.getData();

		String message = data.get("message");

		if(message.contentEquals("PerformSync")){
			// Perform Sync
			Log.i(Application.TAG, "Message to Perform Sync recieved from GCM");
			return performSync();
		}

		if(message.contentEquals("SyncAll")){
			Log.i(Application.TAG, "Message to Perform Sync-All recieved from GCM");
			Application.getApplicationInstance().getSettings().put("LastQuestionsSyncTime", "1349328720");
			Application.getApplicationInstance().getSettings().put("LastQuizSyncTime", "1349328720");
			return performSync();
		}

		return new ResultObject();
		
	}
	
	private ResultObject performSync() {

		MultiCommand command = new MultiCommand(this);
			
		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(this);
		command.addCommand(fetchArtifacts);
		
		DownloadQuestionsCommand downloadQuestions = new DownloadQuestionsCommand(this);
		command.addCommand(downloadQuestions);
		
		DownloadQuizCommand downloadQuiz = new DownloadQuizCommand(this);
		command.addCommand(downloadQuiz);
		
		return command.execute();
	}

	@Override
	public void NotifyCommandExecuted(ResultObject result) {
		// Nothing to do
	}

	@Override
	public void ProgressUpdate(ProgressInfo result) {
		// Nothing to do
	}

}