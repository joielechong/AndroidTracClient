package com.mfvl.trac.client;

public class TcObject {

	protected int hc(Object o) {
		return o == null ? 0 : o.hashCode();
	}
}
