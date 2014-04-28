package com.example.smsbabel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.smsbabel.utils.DisplayMessages;

public class BabelText extends Activity {
	
	public static final int CONTACT_PICKER_RESULT = 1001;
	public static final String LOG_TAG = "Twilio.Babel.BabelText";
	private static final String NO_NUMBER =  "No number";
	public Properties properties = new Properties();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_babel_text);      
        try {
			properties = loadProperties();
		} catch (Exception e) {
			Log.e(LOG_TAG, "onCreate(): invalid configuration properties, please check README file at the root of the project");
			DisplayMessages.showWarn(getApplicationContext(), "Application is not configured, please check README file at the root of the project");
			Log.e(LOG_TAG, "onCreate(): closing application");
			this.finish();
		}
        Log.i(LOG_TAG, "onCreate(): configuration finished");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.babel_text, menu);
        return true;
    }
    
    
    public void doLaunchContactPicker(View view) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }

    
    public void getTheContactPicker(View v) {
    	Log.i(LOG_TAG, "Invoking contact picker");
    	Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        Log.i(LOG_TAG, "Contact picker invoked");
    }
    
    
    public void sendTheMessage(View v) {
    	
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	
    	String theText = ((EditText) findViewById(R.id.editText1)).getText().toString();
    	String theNumber = ((EditText) findViewById(R.id.editText2)).getText().toString();
    	String theEndLanguage = ((Spinner) findViewById(R.id.spinner1)).getSelectedItem().toString();
    	// Hide the phone number dialer when the user sends the message
    	imm.hideSoftInputFromWindow(((EditText) findViewById(R.id.editText2)).getWindowToken(), 0);

   		MessageAsync sendMessage = new MessageAsync();
   		String params[] = new String[3];
   		params[0] = theText;
   		params[1] = theNumber.replaceAll(" ", "");
   		params[2] = theEndLanguage;
   		
   		if (!validInput(params[0], params[1], params[2])) {
   			Log.i(LOG_TAG, "sendTheMessage(): parameters are not present or invalid (text=" + params[0] + ", number=" + params[0] + ", endLanguage" + params[0] + ")");
   			DisplayMessages.showWarn(getApplicationContext(), "There are missing or wrong parameters, check them please!");
   			return;
   		} 
   		
   		Log.i(LOG_TAG, "sendTheMessage(): texting " + theNumber + " with original text \"" + theText + "\" to be translated to " + theEndLanguage);
   		
		sendMessage.onPreExecute(getApplicationContext(), properties.getProperty("ACCOUNT_SID"),
														  properties.getProperty("AUTH_TOKEN"),
														  properties.getProperty("HTTP_PATH"), 
														  Integer.valueOf(properties.getProperty("SUCCESS_HTTP_ERROR_CODE")), 
														  properties.getProperty("ORIGINATING_NUMBER"), 
														  properties.getProperty("BING_TRANSLATE_ID"), 
														  properties.getProperty("BING_TRANSLATE_SECRET"), 
														  properties.getProperty("TRANSLATION_ERROR_TOKEN"));
		
   		sendMessage.execute(params);
   		DisplayMessages.showWarn(getApplicationContext(), "Requesting...");
   		Log.i(LOG_TAG, "Sending your message...");    	
    }
    
    
    private boolean validInput(String theText, String theNumber, String theEndLanguage) {
		
    	String phoneNumberRegexPattern = "^[+]?[0-9]{8,20}$";
    	
    	boolean areParametersValid = (!"".equals(theText) && !(theText == null)) &&
    								  (!"".equals(theNumber)) && (theNumber.matches(phoneNumberRegexPattern)) &&
    								  (!theEndLanguage.contains("Choose"));
    	
		return areParametersValid;
	}


	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case CONTACT_PICKER_RESULT:
                Log.i(LOG_TAG, "Extracting phone number from result");
                String thePhoneNumber = getThePhoneNumberFromResult(data);
                EditText phoneNumberText = (EditText) findViewById(R.id.editText2);
                phoneNumberText.setText(thePhoneNumber);
                break;
            }

        } else {
            // gracefully handle failure
            Log.e(LOG_TAG, "The activity " + requestCode + " did not succeed, result was " + resultCode);
        }
    }
    
    
    private String getThePhoneNumberFromResult(Intent result) {
    	Uri resultingUri = ((Intent) result).getData();
    	Log.i(LOG_TAG, "Got a result: " + resultingUri.toString());
    	return id2PhoneNumber(resultingUri);
    }
    
 
    private String id2PhoneNumber(Uri URIForContact) {
    	String id = URIForContact.getLastPathSegment();
    	String returnValue = NO_NUMBER;
    	String hasPhoneNumber;
    	Cursor cursor;
    	
    	try {
    		
    		Log.i(LOG_TAG, "id2PhoneNumber(): Trying to retrieve a phone number for the entry in contact list");
			cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);
			cursor.moveToFirst();
			hasPhoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
			
			if("1".equalsIgnoreCase(hasPhoneNumber)) 
				returnValue = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			
			Log.i(LOG_TAG, "id2PhoneNumber(): returning contact " + returnValue);
			
		} catch (Exception e) {
			Log.w(LOG_TAG, "id2PhoneNumber(): The choosen contact does not contain any phone number");
			DisplayMessages.showWarn(getApplicationContext(), "The choosen contact does not contain any phone number");
		}
    		
    	return returnValue;
    }
    
    
	private Properties loadProperties() throws Exception {
		
		Iterator it;
		Entry<String, String> currentEntry;
		
		try {
		 	Log.i(LOG_TAG, "loadProperties(): loading property file");
            AssetManager assetManager = getApplicationContext().getAssets();
            InputStream inputStream = assetManager.open("configuration.properties");
            properties.load(inputStream);
            Log.i(LOG_TAG, "loadProperties(): properties successfully loaded from file");
            
            it = properties.entrySet().iterator();
            
            while (it.hasNext()) {
            	currentEntry = (Entry<String, String>) it.next();
            	if (("".equals(currentEntry.getValue())) || (currentEntry.getValue() == null)) {
            		throw new Exception("loadProperties(): invalid configuration properties");
            	}
            }
            
            
     } catch (IOException e) {             
            Log.e(LOG_TAG, "loadProperties(): could not load custom properties ");
            DisplayMessages.showWarn(getApplicationContext(), "The configuration could not be loaded, application will not work!!!");
     } 
     return properties;
     
	}
    
    
}
