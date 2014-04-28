package com.example.smsbabel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.smsbabel.Exception.TranslationException;
import com.example.smsbabel.utils.DisplayMessages;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.TwilioRestResponse;

public class MessageAsync extends AsyncTask<String, Void, String> {
	
	// Fixed
	public final String METHOD = "POST";
	private final String LOG_TAG = "Twilio.Babel.MessageAsync";
	
	// Customizable
	public String ACCOUNT_SID;
	public String AUTH_TOKEN;
	public String HTTP_PATH;
	public int SUCCESS_HTTP_ERROR_CODE;
	public String ORIGINATING_NUMBER;
	private String BING_TRANSLATE_ID;
	private String BING_TRANSLATE_SECRET;
	private String TRANSLATION_ERROR_TOKEN;
	
	private Context context;
	
	public final DefaultHttpClient httpclient = new DefaultHttpClient();
	
	
    protected void onPreExecute(Context context, String accountSid, String authToken, String httpPath, int successHttpErrorCode, String originatingNumber, String bingTranslateID, String bingTranslateSecret, String translationErrorToken) {
    	this.context = context;
    	ACCOUNT_SID = accountSid;
    	AUTH_TOKEN = authToken;
    	HTTP_PATH = httpPath;
    	SUCCESS_HTTP_ERROR_CODE = successHttpErrorCode;
    	ORIGINATING_NUMBER = originatingNumber;
    	BING_TRANSLATE_ID = bingTranslateID;
    	BING_TRANSLATE_SECRET = bingTranslateSecret;
    	TRANSLATION_ERROR_TOKEN = translationErrorToken;
    	Log.i(LOG_TAG, "onPreExecute(): setting up properties for task");    	
    }
	
    
	@Override
	protected String doInBackground(String... arg0) {
		return sendTheMessage(arg0[0], arg0[1], arg0[2]);
	}

	
	private String sendTheMessage(String text, String terminatingNumber, String terminatingLanguage) {
		
		int httpErrorCode;
		List<NameValuePair> params;
		String translatedText;
		
        try {
        	
        	Log.i(LOG_TAG, "sendTheMessage(): translating...");
    		translatedText = translateText(text, terminatingLanguage);
    		
    		params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("To", terminatingNumber)); 
            params.add(new BasicNameValuePair("From", ORIGINATING_NUMBER)); 
            params.add(new BasicNameValuePair("Body", translatedText));
        	
            Log.i(LOG_TAG, "sendTheMessage(): sending...");
        	httpErrorCode = sendTheMessage(HTTP_PATH, METHOD, params, ACCOUNT_SID, AUTH_TOKEN).getHttpStatus();
        	
        	if (httpErrorCode != SUCCESS_HTTP_ERROR_CODE)        		
        		return "Unexpected response from API: " + httpErrorCode;
        	else
        		return "Message sent successfully!!!";
        	
		} catch (TwilioRestException e) {
			Log.e(LOG_TAG, "Exception arose while requesting the delivery to the API...");
			return "Could not request the delivery, retry later on...";
		} catch (TranslationException e) {
			Log.e(LOG_TAG, "Exception arose while requesting the translation to the API...");
			return "Could get the translation, retry later on...";
		}
        
	}
	
	
	private String translateText(String text, String endLanguage) throws TranslationException {
		Translate.setClientId(BING_TRANSLATE_ID);
		Translate.setClientSecret(BING_TRANSLATE_SECRET);
		String translatedText;
		try {
			Log.i(LOG_TAG, "translateText(): requesting translation to API for text " + text + " to " + endLanguage + " language");
			translatedText = Translate.execute(text, Language.valueOf(endLanguage));
			Log.i(LOG_TAG, "translateText(): checking if " + text + " is a valid translation");
			isAValidTranslation(translatedText);
			return translatedText;
		} catch (Exception e) {
			Log.e(LOG_TAG, "translateText(): could not obtain translation for " + text);
			throw new TranslationException();
		}
	}
	
	
	private boolean isAValidTranslation(String translatedText) throws TranslationException {
		
		Log.i(LOG_TAG, "isAValidTranslation(): checking correctness of translated text " + translatedText);
		if (translatedText != null) {
			if (translatedText.contains(TRANSLATION_ERROR_TOKEN)) {
				Log.e(LOG_TAG, "isAValidTranslation(): the translated text " + translatedText + " seems to be an error result");
				throw new TranslationException();
			}
		} else {
			Log.e(LOG_TAG, "isAValidTranslation(): the translated text resulted to be null");
			throw new TranslationException();
		}
		Log.i(LOG_TAG, "isAValidTranslation(): " + translatedText + " seems to be a valid translated text");
		return true;
			
	}


	/* ***********************************************************************************************
	*
	*				FROM HERE ON THE CODE IS ADAPTED FROM THE TWILIO LIBRARY FOR JAVA
	*
	*		· The reason to get the code is a conflict with the Apache HTTP client provided by Android
	*		· There are minor changes in the code from the library
	*
	**************************************************************************************************/
	
	
	

	private TwilioRestResponse sendTheMessage(String path, String method, List<NameValuePair> paramList, String accountSid, String authToken) throws TwilioRestException {

		HttpUriRequest request = setupRequest(path, method, paramList, accountSid, authToken);

		HttpResponse response;
		try {
			response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();

			Header[] contentTypeHeaders = response.getHeaders("Content-Type");
			String responseBody = "";

			if (entity != null) {
				responseBody = EntityUtils.toString(entity);
			}

			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();

			TwilioRestResponse restResponse = new TwilioRestResponse(request.getURI().toString(), responseBody, statusCode);

			// For now we only set the first content type seen
			for (Header h : contentTypeHeaders) {
				restResponse.setContentType(h.getValue());
				break;
			}
			
			return restResponse;

		} catch (ClientProtocolException e1) {
			throw new RuntimeException(e1);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}
	
	
	private HttpUriRequest setupRequest(String path, String method, List<NameValuePair> params, String accountSid, String authToken) {

		StringBuilder sb = new StringBuilder();

		// If we've given a fully qualified uri then skip building the endpoint
		sb.append(path);

		path = sb.toString();

		HttpUriRequest request = buildMethod(method, path, params);

		request.addHeader(new BasicHeader("X-Twilio-Client", "java-3.4.1"));
		request.addHeader(new BasicHeader("User-Agent", "twilio-java/3.4.1"));
		request.addHeader(new BasicHeader("Accept", "application/json"));
		request.addHeader(new BasicHeader("Accept-Charset", "utf-8"));

		if (httpclient instanceof DefaultHttpClient) { // as DefaultHttpClient class has final method, I need httpClient to be a plain interface to be able to mock it
            ((DefaultHttpClient) httpclient).getCredentialsProvider()
				.setCredentials(
						new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
						new UsernamePasswordCredentials(accountSid, authToken));
        }
		return request;
	}
	
	
	private HttpUriRequest buildMethod(String method, String path,
			List<NameValuePair> params) {
		if (method.equalsIgnoreCase("GET")) {
			return generateGetRequest(path, params);
		} else if (method.equalsIgnoreCase("POST")) {
			return generatePostRequest(path, params);
		} else {
			throw new IllegalArgumentException("Unknown Method: " + method);
		}
	}
	
	
	private HttpGet generateGetRequest(String path, List<NameValuePair> params) {

		URI uri = buildUri(path, params);
		return new HttpGet(uri);
	}
	
	
	private URI buildUri(String path, List<NameValuePair> queryStringParams) {
		StringBuilder sb = new StringBuilder();
		sb.append(path);

		if (queryStringParams != null && queryStringParams.size() > 0) {
			sb.append("?");
			sb.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
		}

		URI uri;
		try {
			uri = new URI(sb.toString());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid uri", e);
		}

		return uri;
	}
	
	
	private HttpPost generatePostRequest(String path, List<NameValuePair> params) {
		URI uri = buildUri(path);

		UrlEncodedFormEntity entity = buildEntityBody(params);

		HttpPost post = new HttpPost(uri);
		post.setEntity(entity);

		return post;
	}
	
	
	private URI buildUri(String path) {
		return buildUri(path, null);
	}
	
	
	private UrlEncodedFormEntity buildEntityBody(List<NameValuePair> params) {
		UrlEncodedFormEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(params, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		return entity;
	}
	
	
	@Override
    protected void onPostExecute(String result) {
    	DisplayMessages.showWarn(this.context, result);
    }
    
	
}
