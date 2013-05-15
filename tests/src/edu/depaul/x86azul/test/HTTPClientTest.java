package edu.depaul.x86azul.test;

import org.apache.http.HttpStatus;
import org.apache.http.params.HttpParams;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.depaul.x86azul.HTTPClient;

public class HTTPClientTest extends TestCase 
	implements HTTPClient.OnFinishProcessHttp {

	private String getResponse;
	private String putResponse;
	private String delResponse;
	
	private int getStatus;
	private int putStatus;
	private int delStatus;
	
	private String baseUrlTarget = "http://httpbin.org/";
	
	public void testConstructor(){
		HTTPClient httpClient = new HTTPClient(this);
		
		Assert.assertNotNull(httpClient.getHttpParameters());
	}
	
	public void testHttpMethodGet(){
		HTTPClient httpClient = new HTTPClient(this);
		getResponse = null;
		getStatus = 0;
		
		// this will spawn the http methods paralelly
		httpClient.get("GetMethod", baseUrlTarget + "get");

		// wait until return
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertEquals(HttpStatus.SC_OK, getStatus);
		Assert.assertNotNull(getResponse);
	}
	
	public void testHttpMethodPut(){
		HTTPClient httpClient = new HTTPClient(this);
		putResponse = null;
		putStatus = 0;
		
		// this will spawn the http methods paralelly
		httpClient.put("PutMethod", baseUrlTarget + "put", "random param");

		// wait until return
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertEquals(HttpStatus.SC_OK, putStatus);
		Assert.assertNotNull(putResponse);
	}
	
	public void testHttpMethodDelete(){
		HTTPClient httpClient = new HTTPClient(this);
		delResponse = null;
		
		// this will spawn the http methods paralelly
		httpClient.delete("DeleteMethod", baseUrlTarget + "delete");

		// wait until return
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Assert.assertEquals(HttpStatus.SC_OK, delStatus);
		Assert.assertNotNull(delResponse);
		
	}
	

	@Override
	public void onFinishProcessHttp(String token, String uri,
			String requestBody, int statusCode, String geohash,
			String responseBody) {
		
		// make sure it works
		if(statusCode >= 400)
			return;
		
		if(token.equals("GetMethod")){
			getResponse = responseBody;
			getStatus = statusCode;
		}
		else if(token.equals("PutMethod")){
			putResponse = responseBody;
			putStatus = statusCode;
		}
		else if(token.equals("DeleteMethod")){
			delResponse = responseBody;
			delStatus = statusCode;
		}
	}

}
