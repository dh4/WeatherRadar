/*
 * Copyright (c) 2018, Dan Hasting
 *
 * This file is part of WeatherRadar
 *
 * WeatherRadar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WeatherRadar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WeatherRadar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.danhasting.radar.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import com.danhasting.radar.R;

import java.util.Arrays;

public class SelectNWSFragment extends Fragment {

    private View view;
    private OnNWSSelectedListener callback;

    public interface OnNWSSelectedListener {
        void onNWSSelected(String location, String type, Boolean loop, Boolean enhanced);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_select_nws, container, false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Spinner typeSpinner = view.findViewById(R.id.typeSpinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.type_names, android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        String type = settings.getString("last_nws_type","");
        int index = Arrays.asList(getResources().getStringArray(R.array.type_values)).indexOf(type);
        typeSpinner.setSelection(index);

        Spinner locationSpinner = view.findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.location_names, android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        String location = settings.getString("last_nws_location","");
        index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
        locationSpinner.setSelection(index);

        final Switch loopSwitch = view.findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_nws_loop",false));

        Switch enhancedSwitch = view.findViewById(R.id.enhancedSwitch);
        if (settings.getBoolean("last_nws_enhanced",false)) {
            enhancedSwitch.setChecked(true);
            loopSwitch.setEnabled(false);
        }

        enhancedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    loopSwitch.setEnabled(false);
                } else {
                    loopSwitch.setEnabled(true);
                }
            }
        });

        Button viewButton = view.findViewById(R.id.viewButton);
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewRadar();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (OnNWSSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnNWSSelectedListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnNWSSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnNWSSelectedListener");
        }
    }

    private void viewRadar() {
        Spinner typeSpinner = view.findViewById(R.id.typeSpinner);
        Spinner locationSpinner = view.findViewById(R.id.locationSpinner);
        Switch loopSwitch = view.findViewById(R.id.loopSwitch);
        Switch enhancedSwitch = view.findViewById(R.id.enhancedSwitch);

        String location = getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()];
        String type = getResources().getStringArray(R.array.type_values)[typeSpinner.getSelectedItemPosition()];
        Boolean loop = loopSwitch.isChecked();
        Boolean enhanced = enhancedSwitch.isChecked();

        if (enhanced) loop = false;

        callback.onNWSSelected(location, type, loop, enhanced);
    }
}
