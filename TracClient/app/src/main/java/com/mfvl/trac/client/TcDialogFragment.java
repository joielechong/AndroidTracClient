package com.mfvl.trac.client;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.app.DialogFragment;
import android.view.Display;
import android.view.WindowManager;

public class TcDialogFragment extends DialogFragment {
    @Override
    public void onResume() {
        super.onResume();
        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        getDialog().getWindow().setLayout(width*95/100, height*9/10);
//		getView().setAlpha(0.7f);
    }

}
