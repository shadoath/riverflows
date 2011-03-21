package com.riverflows.test;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.riverflows.wsclient.CODWRDataSource;

public class AssetCoDwrCsvHttpClient extends AssetHttpClientWrapper {
	
	private static final Log LOG = LogFactory.getLog(AssetCoDwrCsvHttpClient.class);
	
	public static String sourceDir = "codwr/";
	
	/**
	 * tail end of the URL that needs to be chopped off
	 */
	private static final String SUFFIX = "&START=00/00/00&END=00/00/00";
	
	public AssetCoDwrCsvHttpClient(AssetManager assetManager) {
		super(assetManager);
	}

	@Override
	public HttpResponse doGet(HttpGet getCmd) throws ClientProtocolException,
			IOException {
		String requestUrl = getCmd.getURI().toString();
		
		if(!requestUrl.startsWith(CODWRDataSource.SITE_DATA_URL)) {
			throw new IllegalArgumentException("url not supported by this mock http client: " + requestUrl);
		}
		
		requestUrl = requestUrl.substring(CODWRDataSource.SITE_DATA_URL.length(), requestUrl.length() - SUFFIX.length());
		
		try {
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
		    		  new ProtocolVersion("HTTP", 1, 1), 200, ""));
			response.setStatusCode(200);
			
			if(LOG.isInfoEnabled()) {
				String[] files = assetManager.list("codwr");
				for(String file:files) {
					LOG.info(file);
				}
			}
			
			AssetFileDescriptor assetFile = assetManager.openFd(sourceDir + requestUrl + ".mp3");
			
			response.setEntity(new InputStreamEntity(assetFile.createInputStream(), assetFile.getLength()));
			
			return response;
		} catch(IOException ioe) {
			LOG.error("problem accessing file: " + sourceDir + requestUrl, ioe);
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
		    		  new ProtocolVersion("HTTP", 1, 1), 404, ""));
			response.setStatusCode(404);
			response.setEntity(new StringEntity("<html><head><title>No Such File</title></head><body>No Such File</body></html>"));
			
			return response;
			
		}
	}
}