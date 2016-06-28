package com.mfvl.trac.client;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;

import com.mfvl.mfvllib.MyLog;

import java.util.ArrayList;
import java.util.ArrayDeque;

import static com.mfvl.trac.client.Const.*;
import static com.mfvl.trac.client.TracGlobal.*;

public class PrefSpecActivity extends TcBaseActivity {
    private static ArrayDeque<Message> msgQueue = null;

    @Override
    public ArrayDeque<Message> getMessageQueue() {
        MyLog.logCall();
        return msgQueue;
    }

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);

        String filterAction = getString(R.string.editFilterAction);
        String sortAction = getString(R.string.editSortAction);
        String loginAction = getString(R.string.editLoginAction);

        if (msgQueue == null) {
            msgQueue = new ArrayDeque<>(100);
        }
        Intent intent = getIntent();
        MyLog.d(intent);
        String action = intent.getAction();
        setContentView(R.layout.content_main);
        tm = TicketModel.getInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = makeArgs();
        TracClientFragment ff;

        if (filterAction.equals(action)) {
            ff = new FilterFragment();
            String filterString = getFilterString();
            final ArrayList<FilterSpec> filter = parseFilterString(filterString);
            args.putSerializable(FILTERLISTNAME, filter);
        } else if (sortAction.equals(action)) {
            ff = new SortFragment();
            String sortString = getSortString();
            final ArrayList<SortSpec> filter = parseSortString(sortString);
            args.putSerializable(SORTLISTNAME, filter);
        } else if (loginAction.equals(action)) {
            ff = new TracLoginFragment();
        } else {
            throw new RuntimeException("unkown action : " + action);
        }
        ff.setArguments(args);
        ft.add(R.id.displayList, ff);
        ft.commit();
    }

    @Override
    public boolean processMessage(Message msg) {
        MyLog.d(msg);
        switch (msg.what) {
            case MSG_DONE:
                finish();
                break;

            case MSG_SET_FILTER:
                //noinspection unchecked
                ArrayList<FilterSpec> filter = (ArrayList<FilterSpec>) msg.obj;
                String filterString = joinList(filter.toArray(), "&");
                storeFilterString(filterString);
                finish();
                break;

            case MSG_SET_SORT:
                //noinspection unchecked
                ArrayList<SortSpec> sort = (ArrayList<SortSpec>) msg.obj;
                String sortString = joinList(sort.toArray(), "&");
                storeSortString(sortString);
                finish();
                break;

            default:
                return super.processMessage(msg);
        }
        return true;
    }
}
