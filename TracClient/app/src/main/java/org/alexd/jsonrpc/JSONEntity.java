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
