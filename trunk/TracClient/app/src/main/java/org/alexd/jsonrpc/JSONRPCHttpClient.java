package org.alexd.jsonrpc;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.config.RequestConfig;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.protocol.HttpClientContext;
import ch.boye.httpclientandroidlib.conn.ssl.AllowAllHostnameVerifier;
import ch.boye.httpclientandroidlib.conn.ssl.SSLConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLContextBuilder;
import ch.boye.httpclientandroidlib.conn.ssl.TrustStrategy;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.impl.client.TargetAuthenticationStrategy;
import ch.boye.httpclientandroidlib.util.EntityUtils;

import com.mfvl.trac.client.tcLog;

/**
 * Implementation of JSON-RPC over HTTP/POST
 */
public class JSONRPCHttpClient extends JSONRPCClient {
	private String _username;
	private String _password;
	public JSONObject lastJsonRequest = null;
	public String lastResponse = null;

	/**
	 * HttpClient to issue the HTTP/POST request
	 */
	protected final CloseableHttpClient httpClient;
	/**
	 * Service URI
	 */
	private final String serviceUri;

	// HTTP 1.0
	private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
/*
	protected class MyTrustSelfSignedStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			if (_debug) {
				for (final X509Certificate x : chain) {
					tcLog.d( "cert: " + x);
				}
				tcLog.d( "chain = " + chain.length + " authType = " + authType);
			}
			return chain.length == 1;
		}

	}
*/
	protected class MyTrustAlwaysStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			return true;
		}

	}

	/**
	 * Construct a JsonRPCClient with the given service uri
	 *
	 * @param uri
	 *            uri of the service
	 */

	public JSONRPCHttpClient(final String uri) {
		this(uri, false, false);
	}

	public JSONRPCHttpClient(final String uri, final boolean sslHack, final boolean sslHostNameHack) {
		serviceUri = uri;
		final HttpClientBuilder hcb = HttpClientBuilder.create();
		hcb.setMaxConnTotal(30);
		hcb.setMaxConnPerRoute(30);
		hcb.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(getSoTimeout()).setConnectionRequestTimeout(getConnectionTimeout()).build());

		final SSLContextBuilder builder = new SSLContextBuilder();
		SSLConnectionSocketFactory sslsf;

		if (sslHack) {
			try {
				builder.loadTrustMaterial(null, new MyTrustAlwaysStrategy());
			} catch (GeneralSecurityException e) {
				tcLog.e( "Exception after sslHack", e);
			}
		}

		try {
			if (sslHostNameHack) {
				sslsf = new SSLConnectionSocketFactory(builder.build(), new AllowAllHostnameVerifier());
			} else {
				sslsf = new SSLConnectionSocketFactory(builder.build());
			}
			hcb.setSSLSocketFactory(sslsf);
		} catch (final GeneralSecurityException e) {
			tcLog.e( "Exception after sslHostNameHack", e);
		}

		hcb.setTargetAuthenticationStrategy(new TargetAuthenticationStrategy());
		httpClient = hcb.build();
	}

	/**
	 * Construct a JsonRPCClient with the given httpClient and service uri
	 *
	 * @param client
	 *            httpClient to use
	 * @param uri
	 *            uri of the service
	 */
	// public JSONRPCHttpClient(final CloseableHttpClient client, final String
	// uri) {
	// httpClient = client;
	// serviceUri = uri;
	// }

	@Override
	protected JSONObject doJSONRequest(JSONObject jsonRequest) throws JSONRPCException {
		// Create HTTP/POST request with a JSON entity containing the request
		try {
			HttpResponse response;
			String actualUri = serviceUri;
			boolean retry;
			int retrycount = 0;
			do {
				retry = false;
				final Uri u = Uri.parse(actualUri);
				final HttpClientContext httpContext = HttpClientContext.create();
				if (_username != null) {
					final BasicCredentialsProvider cp = new BasicCredentialsProvider();
					cp.setCredentials(new AuthScope(u.getHost(), u.getPort()),
							new UsernamePasswordCredentials(_username, _password));
					httpContext.setCredentialsProvider(cp);
					httpContext.setAuthCache(new BasicAuthCache());
				}

				final HttpPost request = new HttpPost(actualUri);
				lastJsonRequest = jsonRequest;

				HttpEntity entity;

				if (encoding.length() > 0) {
					entity = new JSONEntity(jsonRequest, encoding);
				} else {
					entity = new JSONEntity(jsonRequest);
				}
				request.setEntity(entity);
				request.setProtocolVersion(PROTOCOL_VERSION);
				response = null; 

				// Execute the request and try to decode the JSON Response
				try {
//					tcLog.d("httpClient = "+httpClient+"request = "+request+" httpContext = "+httpContext);
					response = httpClient.execute(request, httpContext);
					int statusCode = response.getStatusLine().getStatusCode();
					if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
						final Header headers[] = response.getHeaders("Location");
						actualUri = headers[0].getValue();
						retry = true;
					}
				} catch (SSLException e) {
					// Catch 1st 3 times
					tcLog.w( "SSLException in 	JSONRPCHTTPClient.doJSONRequest retrycount = "+retrycount);
					if (retrycount++ < 3) {
						retry = true;
					} else {
						throw(e);
					}
				}
			} while (retry);
			
			// response cannot be null here, if it is it is correct to throw an exception :-)
			
			String responseString = EntityUtils.toString(response.getEntity());
			lastResponse = responseString;

			responseString = responseString.trim();

			final JSONObject jsonResponse = new JSONObject(responseString);
			// Check for remote errors
			if (jsonResponse.has("error")) {
				final Object jsonError = jsonResponse.get("error");
				//noinspection ObjectEqualsNull
				if (!jsonError.equals(null)) {
					throw new JSONRPCException(((JSONObject) jsonError).get("message"));
				}
			}
			return jsonResponse;
		} catch (final JSONRPCException e) {
//			tcLog.e( "JSONRPCException in JSONRPCHTTPClient.doJSONRequest", e);
			throw e;
			} catch (final SSLException e) { // 4th time
//			tcLog.d( "SSLException in JSONRPCHTTPClient.doJSONRequest", e);
			throw new JSONRPCException(e.getMessage());
		} catch (final JSONException e) {
//			tcLog.e( "JSONException in JSONRPCHTTPClient.doJSONRequest", e);
			if (lastResponse.length() == 0) {
				throw new JSONRPCException("JSONException: " + e.getMessage());
			} else {
				final int titelstart = lastResponse.indexOf("<title");
				final int titeleind = lastResponse.indexOf("</title>");
				if (titelstart == -1 || titeleind == -1) {
//					tcLog.toast(lastResponse.substring(0, 20) + "==");
//					tcLog.i( "lastResonse = "+lastResponse.substring(0, 20) + "==");
					if ("No protocol matching".equals(lastResponse.substring(0, 20))) {
						throw new JSONRPCException("NOJSON");
					} else {
						throw new JSONRPCException("Invalid JSON response: " + lastResponse);
					}
				} else {
					final String titel = lastResponse.substring(titelstart + 7, titeleind).trim();
					throw new JSONRPCException("Invalid JSON response: title = " + titel);
				}
			}
		} catch (final Exception e) {
//			tcLog.e( "Exception in JSONRPCHTTPClient.doJSONRequest", e);
			throw new JSONRPCException("Exception in doRequest: " + e.getMessage(), e);
		}
	}
	
	public void setCredentials(final String username, final String password) {
		if (username != null && !"".equals(username)) {
			_username = username;
			_password = password;
		} else {
			_username = null;
			_password = null;
		}
	}
}
