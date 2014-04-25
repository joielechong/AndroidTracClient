package org.alexd.jsonrpc;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

import com.mfvl.android.http.Header;
import com.mfvl.android.http.entity.StringEntity;
import com.mfvl.android.http.message.BasicHeader;
import com.mfvl.android.http.protocol.HTTP;

/**
 * Provides a HttpEntity for json content
 */
class JSONEntity extends StringEntity {
	/**
	 * Basic constructor
	 * 
	 * @param jsonObject
	 * @throws UnsupportedEncodingException
	 */
	public JSONEntity(JSONObject jsonObject) throws UnsupportedEncodingException {
		super(jsonObject.toString());
	}

	/**
	 * Constructor with encoding specified
	 * 
	 * @param jsonObject
	 * @param encoding
	 *            Chosen encoding from Consts.UTF_8, ISO_8859_1 or any other
	 *            supported format
	 * @throws UnsupportedEncodingException
	 */
	public JSONEntity(JSONObject jsonObject, String encoding) throws UnsupportedEncodingException {
		super(jsonObject.toString(), encoding);
		setContentEncoding(encoding);
	}

	@Override
	public Header getContentType() {
		return new BasicHeader(HTTP.CONTENT_TYPE, "application/json");
	}
}
