/*
 * Copyright (C) 2013,2014 Michiel van Loon
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

package com.mfvl.trac.client;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


class TracJSONObject extends JSONObject {

    TracJSONObject() {
        super();
    }

    public JSONObject makeComplexCall(String id, String method, Object... params) throws
                                                                                  JSONException {
        this.put("method", method);
        this.put("id", id);
        final JSONArray args = new JSONArray();

        for (final Object o : params) {
            args.put(o);
        }
        this.put("params", args);
        return this;
    }
}
