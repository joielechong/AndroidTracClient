/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alexd.jsonrpc;

import android.net.Uri;

import com.mfvl.mfvllib.MyLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.ProtocolVersion;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.CredentialsProvider;
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

/**
 * Implementation of JSON-RPC over HTTP/POST
 */
public class JSONRPCHttpClient extends JSONRPCClient {
    // HTTP 1.0
    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);
    private static final int MAX_CONN_TOTAL = 30;
    private static final int MAX_CONN_PER_ROUTE = 30;
    /**
     * HttpClient to issue the HTTP/POST request
     */
    protected final CloseableHttpClient httpClient;
    /**
     * Service URI
     */
    private final String serviceUri;
    public JSONObject lastJsonRequest = null;
    public String lastResponse = null;
    private String _username = null;
    private String _password = null;

    public JSONRPCHttpClient(final String uri) {
        this(uri, false, false);
    }

    @SuppressWarnings("AllowAllHostnameVerifier")
    public JSONRPCHttpClient(final String uri, final boolean sslHack, final boolean sslHostNameHack) {
        serviceUri = uri;
        final HttpClientBuilder hcb = HttpClientBuilder.create();
        hcb.setMaxConnTotal(MAX_CONN_TOTAL);
        hcb.setMaxConnPerRoute(MAX_CONN_PER_ROUTE);
        hcb.setDefaultRequestConfig(
                RequestConfig.custom().setSocketTimeout(getSoTimeout()).setConnectionRequestTimeout(
                        getConnectionTimeout()).build());

        final SSLContextBuilder builder = new SSLContextBuilder();

        if (sslHack) {
            try {
                builder.loadTrustMaterial(null, new MyTrustAlwaysStrategy());
            } catch (GeneralSecurityException e) {
                MyLog.e("Exception after sslHack", e);
            }
        }

        try {
            SSLConnectionSocketFactory sslsf = sslHostNameHack ? new SSLConnectionSocketFactory(builder.build(),
                    new AllowAllHostnameVerifier()) : new SSLConnectionSocketFactory(builder.build());
            hcb.setSSLSocketFactory(sslsf);
        } catch (final GeneralSecurityException e) {
            MyLog.e("Exception after sslHostNameHack", e);
        }

        hcb.setTargetAuthenticationStrategy(new TargetAuthenticationStrategy());
        httpClient = hcb.build();
    }

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
                    final CredentialsProvider cp = new BasicCredentialsProvider();
                    cp.setCredentials(new AuthScope(u.getHost(), u.getPort()),
                            new UsernamePasswordCredentials(_username, _password));
                    httpContext.setCredentialsProvider(cp);
                    httpContext.setAuthCache(new BasicAuthCache());
                }

                final HttpPost request = new HttpPost(actualUri);
                lastJsonRequest = jsonRequest;

                HttpEntity entity = encoding.length() > 0 ? new JSONEntity(jsonRequest, encoding) : new JSONEntity(jsonRequest);
                request.setEntity(entity);
                request.setProtocolVersion(PROTOCOL_VERSION);
                response = null;

                // Execute the request and try to decode the JSON Response
                try {
//					MyLog.d("httpClient = "+httpClient+"request = "+request+" httpContext = "+httpContext);
                    response = httpClient.execute(request, httpContext);
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                        final Header headers[] = response.getHeaders("Location");
                        actualUri = headers[0].getValue();
                        retry = true;
                    }
                } catch (SSLException e) {
                    // Catch 1st 3 times
                    MyLog.w("SSLException in 	JSONRPCHTTPClient.doJSONRequest retrycount = " + retrycount);
                    if (retrycount++ < 3) {
                        retry = true;
                    } else {
                        throw (e);
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
//			MyLog.e( "JSONRPCException in JSONRPCHTTPClient.doJSONRequest", e);
            throw e;
        } catch (final SSLException e) { // 4th time
//			MyLog.d( "SSLException in JSONRPCHTTPClient.doJSONRequest", e);
            throw new JSONRPCException(e.getMessage());
        } catch (final JSONException e) {
//			MyLog.e("JSONException in JSONRPCHTTPClient.doJSONRequest", e);
//			MyLog.d("lastResonse = "+lastResponse);
            if (lastResponse.length() == 0) {
                throw new JSONRPCException("JSONException: " + e.getMessage());
            } else {
                final int titelstart = lastResponse.indexOf("<title");
                final int titeleind = lastResponse.indexOf("</title>");
                if (titelstart == -1 || titeleind == -1) {
                    MyLog.toast(lastResponse.substring(0, 20) + "==");
                    if ("No protocol matching".equals(lastResponse.substring(0, 20))) {
                        throw new JSONRPCException("NOJSON");
                    } else {
                        throw new JSONRPCException("Invalid JSON response: " + lastResponse);
                    }
                } else {
                    final String titel = lastResponse.substring(titelstart + 7, titeleind).trim();
                    throw new JSONRPCException("Invalid JSON response: message = " + titel);
                }
            }
        } catch (final Exception e) {
//			MyLog.e( "Exception in JSONRPCHTTPClient.doJSONRequest", e);
            throw new JSONRPCException("Exception in doRequest: " + e.getMessage(), e);
        }
    }

    protected void setCredentials(final String username, final String password) {
        if (username != null && !"".equals(username)) {
            _username = username;
            _password = password;
        } else {
            _username = null;
            _password = null;
        }
    }

    /*
    protected class MyTrustSelfSignedStrategy implements TrustStrategy {

            @Override
            public boolean isTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                if (debug) {
                    for (final X509Certificate x : chain) {
                        MyLog.d( "cert: " + x);
                    }
                    MyLog.d( "chain = " + chain.length + " authType = " + authType);
                }
                return chain.length == 1;
            }

        }
    */
    protected class MyTrustAlwaysStrategy implements TrustStrategy {

        @Override
        public boolean isTrusted(final X509Certificate[] chain, final String authType) throws
                CertificateException {
            return true;
        }

    }
}
