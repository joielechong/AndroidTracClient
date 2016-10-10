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

import org.alexd.jsonrpc.JSONRPCParams.Versions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import ch.boye.httpclientandroidlib.Consts;

public abstract class JSONRPCClient {

    protected Versions version;
    protected boolean debug = false;

    // public static final String VERSION_1 = "1.0";
    // public static final String VERSION_2 = "2.0";
    protected int soTimeout = 0, connectionTimeout = 0;
    String encoding = Consts.UTF_8.displayName();

    /**
     * Create a JSONRPCClient from a given uri
     *
     * @param uri The URI of the JSON-RPC service
     * @return a JSONRPCClient instance acting as a proxy for the web service
     */
    public static JSONRPCClient create(String uri, Versions version) {
        final JSONRPCClient client = new JSONRPCHttpClient(uri);
        client.version = version;
        return client;
    }

    protected static JSONArray getJSONArray(Object[] array) {
        final JSONArray arr = new JSONArray();
        for (final Object item : array) {
            if (item.getClass().isArray()) {
                arr.put(getJSONArray((Object[]) item));
            } else {
                arr.put(item);
            }
        }
        return arr;
    }

    /**
     * Get the debugging mode
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Setting the _debugging mode (ON / OFF -> FALSE / TRUE
     */
    public void setDebug(boolean _debug) {
        debug = _debug;
    }

    public void setEncoding(String _encoding) {
        encoding = _encoding;
    }

    public void delEncoding() {
        encoding = "";
    }

    protected JSONObject doRequest(String method, Object[] params) throws JSONRPCException {
        // Copy method arguments in a json array
        final JSONArray jsonParams = new JSONArray();
        for (final Object param : params) {
            if (param.getClass().isArray()) {
                final JSONArray ar = getJSONArray((Object[]) param);

                jsonParams.put(ar);
            }
            jsonParams.put(param);
        }

        return doRequest(method, jsonParams);
/*
    // Create the json request object
		final JSONObject jsonRequest = new JSONObject();
		try {
			jsonRequest.put("id", UUID.randomUUID().hashCode());
			jsonRequest.put("method", method);
			jsonRequest.put("params", jsonParams);
			jsonRequest.put("jsonrpc", "2.0");
		} catch (final JSONException e1) {
			throw new JSONRPCException("Invalid JSON request", e1);
		}
		return doJSONRequest(jsonRequest);
*/
    }

    //
    public void beginCall(String method, final Object... params) {
        // Handler
        class RequestThread extends Thread {
            String mMethod;
            Object[] mParams;

            public RequestThread(String method, Object[] params) {
                mMethod = method;
                mParams = params;
            }

            @Override
            public void run() {
                try {
                    doRequest(mMethod, mParams);
                } catch (final JSONRPCException ignored) {
                }
            }

        }

        final Thread requestThread = new RequestThread(method, params);
        requestThread.start();
    }

    /**
     * Get the socket operation timeout in milliseconds
     */
    int getSoTimeout() {
        return soTimeout;
    }

    /**
     * Set the socket operation timeout
     *
     * @param soTimeout timeout in milliseconds
     */
    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    /**
     * Get the connection timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
//

    /**
     * Set the connection timeout
     *
     * @param connectionTimeout timeout in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public Object call(String method, Object... params) throws JSONRPCException {
        try {
            return doRequest(method, params).get("result");
        } catch (final JSONException e) {
            throw new JSONRPCException("Cannot convert result", e);
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public Object call(String method, JSONObject params) throws JSONRPCException {
        try {
            return doRequest(method, params).get("result");
        } catch (final JSONException e) {
            throw new JSONRPCException("Cannot convert result to String", e);
        }
    }

    protected JSONObject doRequest(String method, JSONObject params) throws JSONRPCException {
        final JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("id", UUID.randomUUID().hashCode());
            jsonRequest.put("method", method);
            jsonRequest.put("params", params);
            jsonRequest.put("jsonrpc", "2.0");
        } catch (final JSONException e1) {
            throw new JSONRPCException("Invalid JSON request", e1);
        }
        return doJSONRequest(jsonRequest);
    }

    protected abstract JSONObject doJSONRequest(JSONObject request) throws JSONRPCException;

    public Object call(String method, JSONArray params) throws JSONRPCException {
        try {
            return doRequest(method, params).get("result");
        } catch (final JSONException e) {
            throw new JSONRPCException("Cannot convert result to String", e);
        }
    }

    protected JSONObject doRequest(String method, JSONArray params) throws JSONRPCException {
        final JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("id", UUID.randomUUID().hashCode());
            jsonRequest.put("method", method);
            jsonRequest.put("params", params);
            jsonRequest.put("jsonrpc", "2.0");
        } catch (final JSONException e1) {
            throw new JSONRPCException("Invalid JSON request", e1);
        }
        return doJSONRequest(jsonRequest);
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a String
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public String callString(String method, Object... params) throws JSONRPCException {
        try {
            return doRequest(method, params).getString("result");
        } catch (final JSONRPCException | JSONException e) {
            throw new JSONRPCException("Cannot convert result to String", e);
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a String
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public String callString(String method, JSONObject params) throws JSONRPCException {
        try {
            return doRequest(method, params).getString("result");
        } catch (final Exception e) {
            throw new JSONRPCException("Cannot convert result to String", e);
        }
    }

    public String callString(String method, JSONArray params) throws JSONRPCException {
        try {
            return doRequest(method, params).getString("result");
        } catch (final Exception e) {
            throw new JSONRPCException("Cannot convert result to String", e);
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as an int
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public int callInt(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getInt("result");
        } catch (final JSONException e) {
            try {
                return Integer.parseInt(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to int", e1);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as an int
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public Object callInt(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getInt("result");
        } catch (final JSONException e) {
            try {
                return Integer.parseInt(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to int", e1);
            }
        }
    }

    public Object callInt(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getInt("result");
        } catch (final JSONException e) {
            try {
                return Integer.parseInt(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to int", e1);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a long
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public long callLong(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getLong("result");
        } catch (final JSONException e) {
            try {
                return Long.parseLong(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to long", e);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a long
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public long callLong(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getLong("result");
        } catch (final JSONException e) {
            try {
                return Long.parseLong(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to long", e);
            }

        }
    }

    public long callLong(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }
            return response.getLong("result");
        } catch (final JSONException e) {
            try {
                return Long.parseLong(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to long", e);
            }

        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a boolean
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public boolean callBoolean(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getBoolean("result");
        } catch (final JSONException e) {
            try {
                return Boolean.parseBoolean(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to boolean", e1);
            }

        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a boolean
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public boolean callBoolean(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getBoolean("result");
        } catch (final JSONException e) {
            try {
                return Boolean.parseBoolean(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to boolean", e);
            }

        }
    }

    public boolean callBoolean(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getBoolean("result");
        } catch (final JSONException e) {
            try {
                return Boolean.parseBoolean(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to boolean", e);
            }

        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a double
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public double callDouble(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getDouble("result");
        } catch (final JSONException e) {
            try {
                return Double.parseDouble(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to double", e);
            }

        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a double
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public double callDouble(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getDouble("result");
        } catch (final JSONException e) {
            try {
                return Double.parseDouble(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to double", e);
            }

        }
    }

    public double callDouble(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getDouble("result");
        } catch (final JSONException e) {
            try {
                return Double.parseDouble(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to double", e);
            }

        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a JSONObject
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public JSONObject callJSONObject(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONObject("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONObject(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONObject", e);
            }
        }
    }

    public JSONObject callJSONObject(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONObject("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONObject().put("result", response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONObject", e);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a JSONObject
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public JSONObject callJSONObject(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONObject("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONObject(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONObject", e);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a JSONArray
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public JSONArray callJSONArray(String method, Object... params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONArray("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONArray(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONArray", e);
            }
        }
    }

    /**
     * Perform a remote JSON-RPC method call
     *
     * @param method The name of the method to invoke
     * @param params Arguments of the method
     * @return The result of the RPC as a JSONArray
     * @throws JSONRPCException if an error is encountered during JSON-RPC method call
     */
    public JSONArray callJSONArray(String method, JSONObject params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONArray("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONArray(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONArray", e);
            }
        }
    }

    public JSONArray callJSONArray(String method, JSONArray params) throws JSONRPCException {
        JSONObject response = null;
        try {
            response = doRequest(method, params);
            if (response == null) {
                throw new JSONRPCException("Cannot call method: " + method);
            }

            return response.getJSONArray("result");
//		} catch (final JSONRPCException e) {
//			throw e;
        } catch (final JSONException e) {
            try {
                return new JSONArray(response.getString("result"));
            } catch (final NumberFormatException | JSONException e1) {
                throw new JSONRPCException("Cannot convert result to JSONArray", e);
            }
        }
    }
}
