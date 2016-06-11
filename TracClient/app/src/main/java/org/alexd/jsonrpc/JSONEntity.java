package org.alexd.jsonrpc;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.message.BasicHeader;
import ch.boye.httpclientandroidlib.protocol.HTTP;

/**
 * Provides a HttpEntity for json content
 */
class JSONEntity extends StringEntity {
    public JSONEntity(JSONObject jsonObject) throws UnsupportedEncodingException {
        super(jsonObject.toString());
    }

    public JSONEntity(JSONObject jsonObject, String encoding) {
        super(jsonObject.toString(), encoding);
        setContentEncoding(encoding);
    }

    @Override
    public Header getContentType() {
        return new BasicHeader(HTTP.CONTENT_TYPE, "application/json");
    }
}
