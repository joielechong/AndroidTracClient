package com.mfvl.trac.client;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

public class FilterActivity extends TcBaseActivity  {

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);
        setContentView(R.layout.content_main);
        tm = TicketModel.getInstance();
        MyLog.d(tm);
        FilterFragment ff = new FilterFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = makeArgs();
        String filterString = getFilterString();
        final ArrayList<FilterSpec> filter = new ArrayList<>();

        if (filterString.length() > 0) {
            String[] fs;

            try {
                fs = filterString.split("&");
            } catch (final IllegalArgumentException e) {
                fs = new String[1];
                fs[0] = filterString;
            }
            final String[] operators = getResources().getStringArray(R.array.filter2_choice);

            for (final String f : fs) {
                filter.add(new FilterSpec(f, operators));
            }
        }
        args.putSerializable(FILTERLISTNAME, filter);
        ff.setArguments(args);
        ft.add(R.id.displayList,ff);
        ft.commit();
    }
	
    @Override
    public boolean handleMessage(Message msg) {
        MyLog.logCall();
        switch (msg.what) {
            case MSG_SET_FILTER:
                //noinspection unchecked
                ArrayList<FilterSpec> filter = (ArrayList<FilterSpec>)msg.obj;
				String filterString = joinList(filter.toArray(), "&");
				storeFilterString(filterString);
				finish();
                break;
				
            default:
                return super.handleMessage(msg);
        }
        return true;
    }	
}
