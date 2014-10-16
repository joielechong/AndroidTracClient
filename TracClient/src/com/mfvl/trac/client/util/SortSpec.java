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

package com.mfvl.trac.client.util;

import java.io.Serializable;

public class SortSpec extends Object implements Cloneable, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3435259758425014514L;
	private final String _veld;
	private Boolean _richting; // true = asc false = desc

	public SortSpec(String veld, Boolean richting) {
		_veld = veld;
		_richting = richting;
	}

	public SortSpec(String veld) {
		this(veld, true);
	}

	public String veld() {
		return _veld;
	}

	public void setRichting(Boolean r) {
		_richting = r;
	}

	public Boolean flip() {
		if (_richting != null) {
			_richting = !_richting;
		}
		return _richting;
	}

	public Boolean richting() {
		return _richting;
	}

	@Override
	public String toString() {
		return "order=" + _veld + (_richting ? "" : "&desc=1");
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	private int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	@Override
	public int hashCode() {
		// Start with a non-zero constant.
		int result = 19;

		// Include a hash for each field.
		result = 29 * result + hc(_veld);
		result = 29 * result + hc(_richting);
		return 29 * result + super.hashCode();
	}
}