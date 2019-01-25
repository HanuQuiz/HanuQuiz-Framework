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

public class DeleteRegIdCommand extends Command {
	
	private String regId;
	
	public DeleteRegIdCommand(Invoker caller, String id) {
		super(caller);
		regId = id;
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		try{
			
			Log.v(Application.TAG, "Deleting GCM RegId with our Server");
			Application app = Application.getApplicationInstance();
			String packageName = app.context.getPackageName();

			String postURL = "https://apps.ayansh.com/HanuGCM/UnRegisterDevice.php";

			URL url = new URL(postURL);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			try {

				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);
				urlConnection.setRequestMethod("POST");

				Uri.Builder uriBuilder = new Uri.Builder()
						.appendQueryParameter("package", packageName)
						.appendQueryParameter("regid", regId);

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

				String line = reader.readLine();

				if(line.toString().contentEquals("Success")){
					// Success
					app.addParameter("RegistrationStatus", ""); // TODO
					Log.v(Application.TAG, "GCM RegId deleted successfully on our server");
				}

			}
			finally {
				urlConnection.disconnect();
			}

		}
		catch (Exception e){
			// Nothing to do
			Log.w(Application.TAG, "Following error occured while deleting GCM RegId with our servers:");
			Log.e(Application.TAG, e.getMessage(), e);
		}
	}
}