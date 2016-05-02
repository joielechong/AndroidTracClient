package com.mfvl.trac.client;

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
