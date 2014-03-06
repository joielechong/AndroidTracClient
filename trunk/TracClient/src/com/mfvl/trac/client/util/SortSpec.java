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
	
	private int hc(Object o) {
		return (o == null ? 0 : o.hashCode());
	}
	
	@Override 
	public int hashCode() {
     // Start with a non-zero constant.
		int result = 17;

     // Include a hash for each field.
		result = 31 * result + hc(_veld);
		result = 31 * result + hc(_richting);
		result = 31 * result + hc((Long)serialVersionUID);
		return result;
	}
}
