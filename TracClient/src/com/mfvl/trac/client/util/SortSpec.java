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
		_veld = veld;
		_richting = true;
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
}
