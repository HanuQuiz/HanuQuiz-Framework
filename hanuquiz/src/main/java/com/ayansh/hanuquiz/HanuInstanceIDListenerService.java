package com.ayansh.hanuquiz;

import android.util.Log;

import com.ayansh.CommandExecuter.CommandExecuter;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ProgressInfo;
import com.ayansh.CommandExecuter.ResultObject;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class HanuInstanceIDListenerService extends FirebaseInstanceIdService {

	@Override
	public void onTokenRefresh() {

		// When this is called, refresh tokens.
		
		Application app = Application.getApplicationInstance();
		
		app.addParameter("RegistrationStatus", "");
		app.addParameter("RegistrationId", "");
		
		try {

			// [START get_token]
			FirebaseInstanceId instanceID = FirebaseInstanceId.getInstance();

			if(app.getSettings().get("InstanceID") == null){
				// If we don't have, then get Instance ID and save.
				String iid = instanceID.getId();
				app.addParameter("InstanceID", iid);
			}
			
            String token = instanceID.getToken();
            // [END get_token]
            
            // Implement this method to send any registration to your app's servers.
            Log.v(Application.TAG, "Registration with GCM success");
    		Application.getApplicationInstance().addParameter("RegistrationId", token);
    		
    		CommandExecuter ce = new CommandExecuter();
    		
    		SaveRegIdCommand command = new SaveRegIdCommand(new Invoker(){

				@Override
				public void NotifyCommandExecuted(ResultObject arg0) {
					// Nothing to do.
					// Save will happen in the command itself.
				}

				@Override
				public void ProgressUpdate(ProgressInfo arg0) {
					// Nothing to do.
				}}, token);
    		
    		command.execute();

        } catch (Exception e) {
            
        }
	}

}