package com.mfvl.trac.client;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.mfvl.mfvllib.MyLog;

public class FilterActivity extends TcBaseActivity  {

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);
        setContentView(R.layout.content_main);
        tm = TicketModel.getInstance();
        MyLog.d(tm);
        FilterFragment ff = new FilterFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.displayList,ff);
        ft.commit();
    }
}
