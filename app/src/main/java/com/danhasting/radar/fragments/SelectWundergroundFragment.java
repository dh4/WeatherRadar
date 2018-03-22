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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.danhasting.radar.R;

import java.util.Arrays;

public class SelectWundergroundFragment extends Fragment {

    private EditText locationEditText;
    private Switch loopSwitch;
    private TextView radiusNumber;
    private Button viewButton;

    private SharedPreferences settings;

    OnWundergroundSelectedListener callback;

    public interface OnWundergroundSelectedListener {
        void onWundergroundSelected(String location, Boolean loop, int distance);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_wunderground, container, false);
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        locationEditText = view.findViewById(R.id.wunderground_location);
        locationEditText.setText(settings.getString("last_wunderground", ""));

        loopSwitch = view.findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_wunderground_loop", false));


        int distance = settings.getInt("last_wunderground_distance", 50);

        radiusNumber = view.findViewById(R.id.radiusNumber);
        radiusNumber.setText(String.valueOf(distance));

        final SeekBar radiusBar = view.findViewById(R.id.radiusBar);
        radiusBar.setProgress(getRadiusPercent(distance));

        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                radiusNumber.setText(String.valueOf(getRadiusValue(i)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        TextView radiusText = view.findViewById(R.id.radiusText);
        String currentText = radiusText.getText().toString();

        String unitsValue = settings.getString("distance_units", getString(R.string.distance_unit_default));
        int index = Arrays.asList(getResources().getStringArray(R.array.distance_unit_values)).indexOf(unitsValue);
        String unitsName = getResources().getStringArray(R.array.distance_unit_names)[index];
        String newText = currentText + String.format(" (in %s)", unitsName.toLowerCase());
        radiusText.setText(newText);

        viewButton = view.findViewById(R.id.viewButton);
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewWunderground();
                viewButton.setText(R.string.loading);
                viewButton.setEnabled(false);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnWundergroundSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnWundergroundSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (settings.getBoolean("api_key_activated", false))
            enableButton();
    }

    public void enableButton() {
        if(viewButton != null) {
            viewButton.setText(R.string.view_wunderground_image);
            viewButton.setEnabled(true);
        }
    }

    private String getRadiusValue(int i) {
        int result;

        if (i > 90)
            result = 1000 + (i - 90) * 100;
        else if (i > 80)
            result = 500 + (i - 80) * 50;
        else if (i > 70)
            result = 250 + (i - 70) * 25;
        else if (i > 55)
            result = 100 + (i - 55) * 10;
        else if (i > 45)
            result = 50 + (i - 45) * 5;
        else
            result = i + 5;

        return String.valueOf(result);
    }

    private int getRadiusPercent(int i) {
        int result;

        if (i <= 50)
            result = i - 5;
        else if (i <= 100)
            result = (i - 50) / 5 + 45;
        else if (i <= 250)
            result = (i - 100) / 10 + 55;
        else if (i <= 500)
            result = (i - 250) / 25 + 70;
        else if (i <= 1000)
            result = (i - 500) / 50 + 80;
        else
            result = (i - 1000) / 100 + 90;

        return result;
    }

    private void viewWunderground() {
        final String location = locationEditText.getText().toString();
        final Boolean loop = loopSwitch.isChecked();
        final int distance = Integer.parseInt(radiusNumber.getText().toString());

        callback.onWundergroundSelected(location, loop, distance);
    }
}

