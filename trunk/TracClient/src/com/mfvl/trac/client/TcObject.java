package com.mfvl.trac.client;

public class TcObject {

	protected boolean equalFields(Object f1, Object f2) {
		boolean retVal = true;
		if (f1 == null) {
			retVal |= (f2 == null);
		} else {
			retVal |= f1.equals(f2);
		}
		return retVal;			
	}
}
