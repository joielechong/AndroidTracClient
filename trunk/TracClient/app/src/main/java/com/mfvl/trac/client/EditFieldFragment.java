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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mfvl.mfvllib.MyLog;


public class EditFieldFragment extends TcDialogFragment {
	
	public static final String Veld = "veld";
	public static final String Waarde = "waarde";
	private static final String NieuwWaarde = "nieuwWaarde";
	private static TicketModel tm;
    private static String veld;
	private static String waarde;
	private static String nieuwWaarde;
	private static Spinner spinValue;
    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        MyLog.logCall();
		savedState.putString(Veld,veld);
		savedState.putString(Waarde,waarde);
		tm.onSaveInstanceState(savedState);
		nieuwWaarde = ((TextView)spinValue.getSelectedView()).getText().toString();
		MyLog.d(nieuwWaarde);
		savedState.putString(NieuwWaarde,nieuwWaarde);
     }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MyLog.d(savedInstanceState);

		if (savedInstanceState == null) {
			tm = TicketModel.getInstance();
			veld = getArguments().getString(Veld);
			waarde = getArguments().getString(Waarde);
			nieuwWaarde = waarde;
		} else {
			veld = savedInstanceState.getString(Veld);
			waarde = savedInstanceState.getString(Waarde);
			nieuwWaarde = savedInstanceState.getString(NieuwWaarde);
			tm = TicketModel.restore(savedInstanceState.getString(TicketModel.bundleKey));
		}
        final TicketModelVeld tmv = tm.getVeld(veld);

        View ll = inflater.inflate(tmv.options() == null ? R.layout.field_spec1 : R.layout.field_spec2, container);
        getDialog().setTitle(veld);

        final EditText et = (EditText) ll.findViewById(R.id.veldwaarde);
        if (et != null) {
            et.setText(waarde);
            et.requestFocus();
        }

        spinValue = (Spinner) ll.findViewById(R.id.spinval);
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
                spinValue.setSelection(tmv.options().indexOf(waarde) + (tmv.optional() ? 1 : 0),true);
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
                MyLog.logCall();
                String newValue = null;

                if (spinValue != null) {
                    newValue = spinValue.getSelectedItem().toString();
                }
                if (et != null) {
                    newValue = et.getText().toString();
                }

                DetailInterface dt = (DetailInterface) (getFragmentManager().findFragmentByTag(
                        TracStart.DetailFragmentTag));
                dt.setModVeld(veld, waarde, newValue);
                getFragmentManager().popBackStack();
            }
        });

        return ll;
    }

}

