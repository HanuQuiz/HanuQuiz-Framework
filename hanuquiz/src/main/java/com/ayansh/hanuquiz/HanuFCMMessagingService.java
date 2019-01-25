package com.ayansh.hanuquiz;

import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.CommandExecuter;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.MultiCommand;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public abstract class HanuFCMMessagingService extends FirebaseMessagingService {
	
	protected ResultObject processMessage(RemoteMessage remoteMessage) {
		// Message received with Intent
		
		// Initialize the application
		Application app = Application.getApplicationInstance();
		app.setContext(getApplicationContext());

		Map<String,String> data = remoteMessage.getData();

		String message = data.get("message");

		if(message.contentEquals("PerformSync")){
			// Perform Sync
			Log.i(Application.TAG, "Message to Perform Sync recieved from FCM");
			return performSync();
		}

		if(message.contentEquals("SyncAll")){
			Log.i(Application.TAG, "Message to Perform Sync-All recieved from FCM");
			Application.getApplicationInstance().getSettings().put("LastQuestionsSyncTime", "1349328720");
			Application.getApplicationInstance().getSettings().put("LastQuizSyncTime", "1349328720");
			return performSync();
		}

		return new ResultObject();
		
	}
	
	private ResultObject performSync() {

		MultiCommand command = new MultiCommand(Command.DUMMY_CALLER);
			
		FetchArtifactsCommand fetchArtifacts = new FetchArtifactsCommand(Command.DUMMY_CALLER);
		command.addCommand(fetchArtifacts);
		
		DownloadQuestionsCommand downloadQuestions = new DownloadQuestionsCommand(Command.DUMMY_CALLER);
		command.addCommand(downloadQuestions);
		
		DownloadQuizCommand downloadQuiz = new DownloadQuizCommand(Command.DUMMY_CALLER);
		command.addCommand(downloadQuiz);
		
		return command.execute();
	}

	@Override
	public void onNewToken(String token) {

		// When this is called, refresh tokens.

		Application app = Application.getApplicationInstance();

		app.addParameter("RegistrationStatus", "");
		app.addParameter("RegistrationId", "");

		try {

			FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();
			String iid = instanceID.getId();
			app.addParameter("InstanceID", iid);

			// Implement this method to send any registration to your app's servers.
			Log.v(Application.TAG, "Registration with FCM success");
			Application.getApplicationInstance().addParameter("RegistrationId", token);

			SaveRegIdCommand command = new SaveRegIdCommand(Command.DUMMY_CALLER, token);
			command.execute();

		} catch (Exception e) {
			// Ignore
		}
	}

}