package org.alexd.jsonrpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.wifi.WifiConfiguration.Status;
import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.ClientProtocolException;
import ch.boye.httpclientandroidlib.client.config.RequestConfig;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.client.protocol.HttpClientContext;
import ch.boye.httpclientandroidlib.conn.ssl.SSLConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLContextBuilder;
import ch.boye.httpclientandroidlib.conn.ssl.TrustStrategy;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;
import ch.boye.httpclientandroidlib.impl.client.TargetAuthenticationStrategy;
import ch.boye.httpclientandroidlib.util.EntityUtils;

import com.mfvl.trac.client.util.tcLog;

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
	private CloseableHttpClient httpClient;
	/**
	 * HttpClientContext to be used
	 */
	private HttpClientContext httpContext;
	/**
	 * Service URI
	 */
	private String serviceUri;

	// HTTP 1.0
	private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	/**
	 * Construct a JsonRPCClient with the given service uri
	 * 
	 * @param uri
	 *            uri of the service
	 */

	public JSONRPCHttpClient(final String uri) {
		this(uri, false);
	}

	public class MyTrustSelfSignedStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			for (final X509Certificate x : chain) {
				tcLog.d(getClass().getName(), "cert: " + x);
			}
			tcLog.d(getClass().getName(), "chain = " + chain.length + " authType = " + authType);
			return chain.length == 1;
			// return true;
		}

	}

	public JSONRPCHttpClient(final String uri, final boolean trustAll) {
		try {
			serviceUri = uri;
			final HttpClientBuilder hcb = HttpClientBuilder.create();
			hcb.setMaxConnTotal(30);
			hcb.setMaxConnPerRoute(30);
			hcb.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(getSoTimeout())
					.setConnectionRequestTimeout(getConnectionTimeout()).build());

			if (trustAll) {
				try {
					final SSLContextBuilder builder = new SSLContextBuilder();
					builder.loadTrustMaterial(null, new MyTrustSelfSignedStrategy());
					final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
					hcb.setSSLSocketFactory(sslsf);
				} catch (final Exception e) {
					tcLog.e(getClass().getName(), "Exception after trustAll", e);
					tcLog.e(getClass().getName(), "  " + tcLog.getStackTraceString(e));
				}
			}
			hcb.setTargetAuthenticationStrategy(new TargetAuthenticationStrategy());
			httpClient = hcb.build();

		} catch (final Exception e) {
			httpClient = null;
			serviceUri = null;
			tcLog.e(getClass().getName(), "Exception in JSONHTTPClient", e);
			tcLog.e(getClass().getName(), "  " + tcLog.getStackTraceString(e));
		}
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

	public void setCredentials(final String username, final String password) {
		if (username != null && !"".equals(username)) {
			_username = username;
			_password = password;
		}
	}
	
	@Override
	protected JSONObject doJSONRequest(JSONObject jsonRequest) throws JSONRPCException {
		// Create HTTP/POST request with a JSON entity containing the request
		try {
			int statusCode = 0;
			HttpResponse response;
			String actualUri = serviceUri;
			do {
				final Uri u = Uri.parse(actualUri);
				final BasicCredentialsProvider cp = new BasicCredentialsProvider();
				cp.setCredentials(new AuthScope(u.getHost(), u.getPort()), new UsernamePasswordCredentials(_username, _password));
				httpContext = HttpClientContext.create();
				httpContext.setCredentialsProvider(cp);
				httpContext.setAuthCache(new BasicAuthCache());

				final HttpPost request = new HttpPost(actualUri);

				if (_debug) {
					tcLog.i(getClass().getName(), "Request: " + jsonRequest.toString());
				}
				lastJsonRequest = jsonRequest;

				HttpEntity entity;

				if (encoding.length() > 0) {
					entity = new JSONEntity(jsonRequest, encoding);
				} else {
					entity = new JSONEntity(jsonRequest);
				}
				request.setEntity(entity);
				request.setProtocolVersion(PROTOCOL_VERSION);

				// Execute the request and try to decode the JSON Response
				// long t = System.currentTimeMillis();
				response = httpClient.execute(request, httpContext);
				tcLog.i(getClass().getName(), "RawResponse: " + response);
				statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
					Header headers[] = response.getHeaders("Location");
					tcLog.i(getClass().getName(), "Headers: " + headers);
					actualUri = headers[0].getValue();
				}
			} while (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY);
			// t = System.currentTimeMillis() - t;
			String responseString = EntityUtils.toString(response.getEntity());
			lastResponse = responseString;

			responseString = responseString.trim();

			if (_debug) {
				tcLog.i(getClass().getName(), "Response: " + responseString);
			}

			final JSONObject jsonResponse = new JSONObject(responseString);
			// Check for remote errors
			if (jsonResponse.has("error")) {
				final Object jsonError = jsonResponse.get("error");
				if (!jsonError.equals(null)) {
					throw new JSONRPCException(jsonResponse.get("error"));
				}
				return jsonResponse; // JSON-RPC 1.0
			} else {
				return jsonResponse; // JSON-RPC 2.0
			}
		} catch (final JSONRPCException e) {
			tcLog.e(getClass().getName(), "JSONRPCException in JSONHTTPClient", e);
			throw new JSONRPCException(e);
		} catch (final ClientProtocolException e) {
			// Underlying errors are wrapped into a JSONRPCException
			// instance
			tcLog.e(getClass().getName(), "ClientProtocolException in JSONHTTPClient", e);
			throw new JSONRPCException("HTTP error: " + e.getMessage());
		} catch (final IOException e) {
			tcLog.e(getClass().getName(), "IOException in JSONHTTPClient", e);
			throw new JSONRPCException("IO error: " + e.getMessage());
		} catch (final JSONException e) {
			tcLog.e(getClass().getName(), "JSONException in JSONHTTPClient", e);
			if (lastResponse.length() == 0) {
				throw new JSONRPCException("JSONException: " + e.getMessage());
			} else {
				final int titelstart = lastResponse.indexOf("<title>");
				final int titeleind = lastResponse.indexOf("</title>");
				if (titelstart == -1 || titeleind == -1) {
					throw new JSONRPCException("Invalid JSON response: " + lastResponse);
				} else {
					final String titel = lastResponse.substring(titelstart + 7, titeleind).trim();
					throw new JSONRPCException("Invalid JSON response: title = " + titel);
				}
			}
		} catch (final Exception e) {
			tcLog.e(getClass().getName(), "Exception in JSONHTTPClient", e);
			throw new JSONRPCException("Exception in doRequest: " + e.getMessage());
		}
	}
}
