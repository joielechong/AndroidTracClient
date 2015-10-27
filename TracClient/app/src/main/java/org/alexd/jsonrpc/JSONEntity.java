package org.alexd.jsonrpc;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.protocol.HTTP;

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
	 *            Chosen encoding from Consts.UTF_8, ISO_8859_1 or any other supported format
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
