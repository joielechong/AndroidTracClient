/*
 * Copyright (C) 2014 Michiel van Loon
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

import java.io.Serializable;

abstract public class Spec extends TcObject implements Serializable, Cloneable {
	private static final long serialVersionUID = -4398082467476637503L;
	protected String _veld;
	
	public Spec(String veld) {
		_veld = veld;
	}

	public String getVeld() {
		return _veld;
	}

	@Override
	public int hashCode() {
		return 19 * (29 + hc(_veld)) + super.hashCode();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
