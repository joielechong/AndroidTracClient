package org.alexd.jsonrpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.mfvl.trac.client.util.tcLog;

/**
 * Implementation of JSON-RPC over HTTP/POST
 */
public class JSONRPCHttpClient extends JSONRPCClient {
	private String _username;
	private String _password;
	private boolean _auth = false;

	public JSONObject lastJsonRequest = null;
	public String lastResponse = null;

	/*
	 * HttpClient to issue the HTTP/POST request
	 */
	private final HttpClient httpClient;
	/*
	 * Service URI
	 */
	private final String serviceUri;

	// HTTP 1.0
	private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	/**
	 * Construct a JsonRPCClient with the given service uri
	 * 
	 * @param uri
	 *            uri of the service
	 */

	public JSONRPCHttpClient(String uri) {
		this(uri, false);
	}

	public JSONRPCHttpClient(String uri, boolean trustAll) {
		final SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		registry.register(new Scheme("https", (trustAll ? new FakeSocketFactory() : SSLSocketFactory.getSocketFactory()), 443));
		final HttpParams params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
		params.setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, PROTOCOL_VERSION);
		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, registry), params);
		serviceUri = uri;
	}

	/**
	 * Construct a JsonRPCClient with the given httpClient and service uri
	 * 
	 * @param client
	 *            httpClient to use
	 * @param uri
	 *            uri of the service
	 */
	public JSONRPCHttpClient(HttpClient client, String uri) {
		httpClient = client;
		serviceUri = uri;
	}

	public void setCredentials(final String username, final String password) {
		if (username != null && !username.equals("")) {
			_username = username;
			_password = password;
			_auth = true;
		}
	}

	@Override
	protected JSONObject doJSONRequest(JSONObject jsonRequest) throws JSONRPCException {
		// Create HTTP/POST request with a JSON entity containing the request
		final HttpPost request = new HttpPost(serviceUri);
		HttpParams params = httpClient.getParams();
		if (params == null) {
			params = new BasicHttpParams();
		}
		HttpConnectionParams.setConnectionTimeout(params, getConnectionTimeout());
		HttpConnectionParams.setSoTimeout(params, getSoTimeout());
		HttpProtocolParams.setVersion(params, PROTOCOL_VERSION);
		request.setParams(params);
		if (_auth) {
			try {
				final BasicScheme bs = new BasicScheme();
				final Header authenticate = bs.authenticate(new UsernamePasswordCredentials(_username, _password), request);
				request.addHeader(authenticate);
			} catch (final AuthenticationException e) {
				if (_debug) {
					tcLog.i(JSONRPCHttpClient.class.toString(), "Cannot authenticate", e);
				}
			}
		}

		if (_debug) {
			tcLog.i(JSONRPCHttpClient.class.toString(), "Request: " + jsonRequest.toString());
		}
		lastJsonRequest = jsonRequest;

		HttpEntity entity;

		try {
			if (encoding.length() > 0) {
				entity = new JSONEntity(jsonRequest, encoding);
			} else {
				entity = new JSONEntity(jsonRequest);
			}
		} catch (final UnsupportedEncodingException e1) {
			throw new JSONRPCException("Unsupported encoding", e1);
		}
		request.setEntity(entity);

		try {
			// Execute the request and try to decode the JSON Response
			long t = System.currentTimeMillis();
			final HttpResponse response = httpClient.execute(request);

			t = System.currentTimeMillis() - t;
			String responseString = EntityUtils.toString(response.getEntity());
			lastResponse = responseString;

			responseString = responseString.trim();

			if (_debug) {
				tcLog.i(JSONRPCHttpClient.class.toString(), "Response: " + responseString);
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
		}
		// Underlying errors are wrapped into a JSONRPCException instance
		catch (final ClientProtocolException e) {
			if (_debug) {
				tcLog.i(JSONRPCHttpClient.class.toString(), "ClientProtocol Exception", e);
			}
			throw new JSONRPCException("HTTP error: " + e.getMessage(), e);
		} catch (final IOException e) {
			if (_debug) {
				tcLog.i(JSONRPCHttpClient.class.toString(), "IO Exception", e);
			}
			throw new JSONRPCException("IO error: " + e.getMessage(), e);
		} catch (final JSONException e) {
			if (_debug) {
				tcLog.i(JSONRPCHttpClient.class.toString(), "JSON Exception", e);
			}
			final int titelstart = lastResponse.indexOf("<title>");
			final int titeleind = lastResponse.indexOf("</title>");
			if (titelstart == -1 || titeleind == -1) {
				throw new JSONRPCException(lastResponse, e);
			} else {
				final String titel = lastResponse.substring(titelstart + 7, titeleind).trim();
				throw new JSONRPCException("Invalid JSON response: " + titel, e);
			}
		}
	}
}
