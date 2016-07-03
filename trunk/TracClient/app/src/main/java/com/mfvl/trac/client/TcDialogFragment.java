/*
 * Copyright (C) 2013 - 2016 Michiel van Loon
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
 */com.mfvl.trac.client;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.app.DialogFragment;
import android.view.Display;
import android.view.WindowManager;

public class TcDialogFragment extends DialogFragment {
    @Override
    public void onResume() {
        super.onResume();
        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        getDialog().getWindow().setLayout(width * 95 / 100, height * 9 / 10);
//		getView().setAlpha(0.7f);
    }

}
