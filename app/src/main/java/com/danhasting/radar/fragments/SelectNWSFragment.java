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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        Spinner locationSpinner = view.findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                requireActivity(), R.array.location_names, android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        String location = settings.getString("last_nws_location", "");
        int index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
        locationSpinner.setSelection(index);

        final SwitchCompat loopSwitch = view.findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_nws_loop", false));

        Button viewButton = view.findViewById(R.id.viewButton);
        viewButton.setOnClickListener(view -> viewRadar());

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            callback = (OnNWSSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnNWSSelectedListener");
        }
    }

    private void viewRadar() {
        Spinner locationSpinner = view.findViewById(R.id.locationSpinner);
        SwitchCompat loopSwitch = view.findViewById(R.id.loopSwitch);

        String location = getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()];
        boolean loop = loopSwitch.isChecked();

        String type = "";
        Boolean enhanced = false;

        callback.onNWSSelected(location, type, loop, enhanced);
    }
}
