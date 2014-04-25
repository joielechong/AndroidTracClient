package org.alexd.jsonrpc;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.mfvl.android.http.Header;
import com.mfvl.android.http.HttpEntity;
import com.mfvl.android.http.HttpResponse;
import com.mfvl.android.http.HttpStatus;
import com.mfvl.android.http.ProtocolVersion;
import com.mfvl.android.http.auth.AuthScope;
import com.mfvl.android.http.auth.UsernamePasswordCredentials;
import com.mfvl.android.http.client.config.RequestConfig;
import com.mfvl.android.http.client.methods.HttpPost;
import com.mfvl.android.http.client.protocol.HttpClientContext;
import com.mfvl.android.http.conn.ssl.AllowAllHostnameVerifier;
import com.mfvl.android.http.conn.ssl.SSLConnectionSocketFactory;
import com.mfvl.android.http.conn.ssl.SSLContextBuilder;
import com.mfvl.android.http.conn.ssl.TrustStrategy;
import com.mfvl.android.http.impl.client.BasicAuthCache;
import com.mfvl.android.http.impl.client.BasicCredentialsProvider;
import com.mfvl.android.http.impl.client.CloseableHttpClient;
import com.mfvl.android.http.impl.client.HttpClientBuilder;
import com.mfvl.android.http.impl.client.TargetAuthenticationStrategy;
import com.mfvl.android.http.util.EntityUtils;
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
		this(uri, false, false);
	}

	public class MyTrustSelfSignedStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			for (final X509Certificate x : chain) {
				tcLog.d(getClass().getName(), "cert: " + x);
			}
			tcLog.d(getClass().getName(), "chain = " + chain.length + " authType = " + authType);
			return chain.length == 1;
		}

	}

	public class MyTrustAlwaysStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
			return true;
		}

	}

	public JSONRPCHttpClient(final String uri, final boolean sslHack, boolean sslHostNameHack) {
		try {
			serviceUri = uri;
			final HttpClientBuilder hcb = HttpClientBuilder.create();
			hcb.setMaxConnTotal(30);
			hcb.setMaxConnPerRoute(30);
			hcb.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(getSoTimeout())
					.setConnectionRequestTimeout(getConnectionTimeout()).build());

			final SSLContextBuilder builder = new SSLContextBuilder();
			SSLConnectionSocketFactory sslsf;

			if (sslHack) {
				try {
					builder.loadTrustMaterial(null, new MyTrustAlwaysStrategy());
				} catch (final Exception e) {
					tcLog.e(getClass().getName(), "Exception after sslHack", e);
				}
			}

			if (sslHostNameHack) {
				sslsf = new SSLConnectionSocketFactory(builder.build(), new AllowAllHostnameVerifier());
			} else {
				sslsf = new SSLConnectionSocketFactory(builder.build());
			}

			hcb.setSSLSocketFactory(sslsf);
			hcb.setTargetAuthenticationStrategy(new TargetAuthenticationStrategy());
			httpClient = hcb.build();

		} catch (final Exception e) {
			httpClient = null;
			serviceUri = null;
			tcLog.e(getClass().getName(), "Exception in JSONRPCHTTPClient", e);
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
				final HttpClientContext httpContext = HttpClientContext.create();
				if (_username != null) {
					final BasicCredentialsProvider cp = new BasicCredentialsProvider();
					cp.setCredentials(new AuthScope(u.getHost(), u.getPort()),
							new UsernamePasswordCredentials(_username, _password));
					httpContext.setCredentialsProvider(cp);
					httpContext.setAuthCache(new BasicAuthCache());
				}
				tcLog.d(getClass().getName(), "httpContext: " + httpContext);

				final HttpPost request = new HttpPost(actualUri);
				tcLog.d(getClass().getName(), "request: " + request);

				if (_debug) {
					tcLog.d(getClass().getName(), "Request: " + jsonRequest.toString());
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
				response = httpClient.execute(request, httpContext);
				statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
					final Header headers[] = response.getHeaders("Location");
					tcLog.i(getClass().getName(), "Headers: " + Arrays.asList(headers));
					actualUri = headers[0].getValue();
				}
			} while (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY);
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
					throw new JSONRPCException(((JSONObject) jsonError).get("message"));
				}
				return jsonResponse; // JSON-RPC 1.0
			} else {
				return jsonResponse; // JSON-RPC 2.0
			}
		} catch (final JSONRPCException e) {
			tcLog.e(getClass().getName(), "JSONRPCException in JSONRPCHTTPClient.doJSONRequest", e);
			throw e;
		} catch (final SSLException e) {
			tcLog.d(getClass().getName(), "SSLException in JSONRPCHTTPClient.doJSONRequest", e);
			throw new JSONRPCException(e.getMessage());
		} catch (final JSONException e) {
			tcLog.e(getClass().getName(), "JSONException in JSONRPCHTTPClient.doJSONRequest", e);
			if (lastResponse.length() == 0) {
				throw new JSONRPCException("JSONException: " + e.getMessage());
			} else {
				final int titelstart = lastResponse.indexOf("<title");
				final int titeleind = lastResponse.indexOf("</title>");
				if (titelstart == -1 || titeleind == -1) {
					throw new JSONRPCException("Invalid JSON response: " + lastResponse);
				} else {
					final String titel = lastResponse.substring(titelstart + 7, titeleind).trim();
					throw new JSONRPCException("Invalid JSON response: title = " + titel);
				}
			}
		} catch (final Exception e) {
			tcLog.e(getClass().getName(), "Exception in JSONRPCHTTPClient.doJSONRequest", e);
			throw new JSONRPCException("Exception in doRequest: " + e.getMessage(), e);
		}
	}
}
