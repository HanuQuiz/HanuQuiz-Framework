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
import java.util.Date;

public class ValidateApplicationCommand extends Command {

	public ValidateApplicationCommand(Invoker caller) {
		super(caller);
	}

	@Override
	protected void execute(ResultObject result) throws Exception {
		
		/*
		 * Validate Application.
		 */
		
		Application app = Application.getApplicationInstance();
		
		// Check if already validated.
		String validationTime = app.getSettings().get("ValidationTime");
		if(validationTime == null || validationTime.contentEquals("")){
			validationTime = "1349328720";	// HANU Epoch
		}
		
		Date lastValidationTime = new Date(Long.valueOf(validationTime));
		Date now = new Date();
		
		if(now.getTime() - lastValidationTime.getTime() > 7*24*60*60*1000){
			// We validated more than a week ago !
			
			Log.v(Application.TAG, "Validating for error 420");
			String line = "";

			URL url = new URL("http://apps.ayansh.com/HanuGCM/Validate.php");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try{

				urlConnection.setDoOutput(true);
				urlConnection.setChunkedStreamingMode(0);
				urlConnection.setRequestMethod("POST");

				Uri.Builder builder = new Uri.Builder()
						.appendQueryParameter("blogurl", app.appURL);
				String query = builder.build().getEncodedQuery();

				OutputStream os = urlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				writer.write(query);
				writer.flush();
				writer.close();
				os.close();

				urlConnection.connect();

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				line = reader.readLine();


			}
			catch (Exception e){
				// Nothing to do.
				Log.w(Application.TAG, "Following error occured while validating for error 420");
				Log.e(Application.TAG, e.getMessage(), e);
			}
			finally {
				urlConnection.disconnect();
			}
			
			if(line.toString().contentEquals("Not Found")){
				// OMG !
				result.setResultCode(420);
				Log.w(Application.TAG, "Validation for error 420 failed. This seems to be an invalid application");
				throw new Exception("This application is not registered with Hanu-Droid. " +
						"Please inform the developer.");
			}
			else if (line.toString().contentEquals("Success")){
				// Success
				app.addParameter("ValidationTime", String.valueOf(now.getTime()));
				Log.v(Application.TAG, "Validation for error 420 was success.");
			}
		}	
	}
}