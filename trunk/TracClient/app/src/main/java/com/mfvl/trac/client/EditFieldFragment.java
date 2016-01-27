/*
 * Copyright (C) 2016 Michiel van Loon
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
 */

package com.mfvl.trac.client;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


public class EditFieldFragment extends DialogFragment {

    public EditFieldFragment() {
    }
	
	@Override
	public void onResume() {
		super.onResume();
		int width = getResources().getDimensionPixelSize(R.dimen.popup_width);
		int height = getResources().getDimensionPixelSize(R.dimen.popup_height);        
		getDialog().getWindow().setLayout(width, height);
//		getView().setAlpha(0.7f);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        tcLog.logCall();
        final String veld = getArguments().getString("veld");
        final String waarde = getArguments().getString("waarde");
        final TicketModel tm = (TicketModel) getArguments().getSerializable("tm");
        final TicketModelVeld tmv = tm.getVeld(veld);

        View ll = inflater.inflate(
                tmv.options() == null ? R.layout.field_spec1 : R.layout.field_spec2, container);
        getDialog().setTitle(veld);

        final EditText et = (EditText) ll.findViewById(R.id.veldwaarde);
        if (et != null) {
            et.setText(waarde);
            et.requestFocus();
        }

        final Spinner spinValue = (Spinner) ll.findViewById(R.id.spinval);
        if (spinValue != null) {
            final ArrayAdapter<Object> spinAdapter = new ArrayAdapter<>(getActivity(),
                                                                        android.R.layout.simple_spinner_item);
            spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            if (tmv.optional()) {
                spinAdapter.add("");
            }
            spinAdapter.addAll(tmv.options());
            spinValue.setAdapter(spinAdapter);
            if (waarde != null && !"".equals(waarde)) {
                spinValue.setSelection(tmv.options().indexOf(waarde) + (tmv.optional() ? 1 : 0),
                                       true);
            }
        }

        final Button canBut = (Button) ll.findViewById(R.id.cancelpw);
        canBut.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });

        final Button storBut = (Button) ll.findViewById(R.id.okBut);
        storBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tcLog.logCall();
                String newValue = null;

                if (spinValue != null) {
                    newValue = spinValue.getSelectedItem().toString();
                }
                if (et != null) {
                    newValue = et.getText().toString();
                }

                DetailFragment dt = (DetailFragment) (getFragmentManager().findFragmentByTag(
                        TracStart.DetailFragmentTag));
                dt.setModVeld(veld, waarde, newValue);
                getFragmentManager().popBackStack();
            }
        });

        return ll;
    }

}

