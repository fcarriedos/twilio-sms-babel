package com.example.smsbabel.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class DisplayMessages {
	
	private static final String LOG_TAG = "Twilio.Babel.DisplayMessages";

    public static void showWarn(Context applicationContext, CharSequence message) {
    	Log.i(LOG_TAG, "showWarn(): showing toast with text \"" + message + "\"");
		Context context = applicationContext;
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
		
    }
	
}
