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
 */com.mfvl.trac.client;

class TcObject {

    boolean equalFields(Object f1, Object f2) {
        boolean retVal = f1 == f2;
        if (!retVal && f1 != null && f2 != null) {
            retVal = f1.equals(f2);
        }
//		MyLog.d("f1 = "+f1+" f2 = "+f2+" retVal = "+retVal);
        return retVal;
    }
}
