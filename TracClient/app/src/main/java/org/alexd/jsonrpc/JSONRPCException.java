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
 */org.alexd.jsonrpc;

/**
 * Represents an error during JSON-RPC method call. Various reasons can make a JSON-RPC call fail (network not available, non
 * existing method, error during the remote execution ...) You can use the inherited method getCause() to see which Exception has
 * caused a JSONRPCException to be thrown
 *
 * @author Alexandre
 */
public class JSONRPCException extends Exception {

    public JSONRPCException(Object error) {
        super(error.toString());
    }

    public JSONRPCException(String message, Throwable innerException) {
        super(message, innerException);
    }
}
