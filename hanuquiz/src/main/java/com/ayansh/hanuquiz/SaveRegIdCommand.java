package com.ayansh.hanuquiz;

import android.net.Uri;
import android.util.Log;

import com.ayansh.CommandExecuter.Command;
import com.ayansh.CommandExecuter.Invoker;
import com.ayansh.CommandExecuter.ResultObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimeZone;

public class SaveRegIdCommand extends Command {
	
	private String regId;
	
	public SaveRegIdCommand(Invoker caller, String id) {
		super(caller);
		regId = id;
	}

	@Override
	protected void execute(ResultObject result) throws Exception {

		Log.v(Application.TAG, "Saving GCM RegId with our Server");
		Application app = Application.getApplicationInstance();
		String packageName = app.context.getPackageName();

		String iid = app.getSettings().get("InstanceID");

		// Get the Time Zone
		TimeZone tz = TimeZone.getDefault();
		String timeZone = tz.getID();
		app.addParameter("TimeZone", timeZone);

		String postURL = "http://apps.ayansh.com/HanuGCM/RegisterDevice.php";

		URL url = new URL(postURL);
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

		try{

			urlConnection.setDoOutput(true);
			urlConnection.setChunkedStreamingMode(0);
			urlConnection.setRequestMethod("POST");

			Uri.Builder uriBuilder = new Uri.Builder()
					.appendQueryParameter("package", packageName)
					.appendQueryParameter("regid", regId)
					.appendQueryParameter("iid", iid)
					.appendQueryParameter("tz", timeZone)
					.appendQueryParameter("platform", "Android")
					.appendQueryParameter("app_version", String.valueOf(app.getCurrentAppVersionCode()));

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

			String line = reader.readLine();

			if(line.toString().contentEquals("Success")){
				// Success
				app.addParameter("RegistrationStatus", "Success");
				Log.v(Application.TAG, "GCM RegId saved successfully on our server");
			}

		}
		catch (Exception e){
			// Nothing to do
			Log.w(Application.TAG, "Following error occured while saving GCM RegId with our servers:");
			Log.e(Application.TAG, e.getMessage(), e);
		}
		finally {
			urlConnection.disconnect();
		}
	}
}