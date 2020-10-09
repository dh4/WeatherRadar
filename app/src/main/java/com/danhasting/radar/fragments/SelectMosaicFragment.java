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

import android.app.Fragment;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import com.danhasting.radar.R;

import java.util.Arrays;

public class SelectMosaicFragment extends Fragment {

    private View view;
    private OnMosaicSelectedListener callback;

    public interface OnMosaicSelectedListener {
        void onMosaicSelected(String location, Boolean loop);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_select_mosaic, container, false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Spinner mosaicSpinner = view.findViewById(R.id.mosaicSpinner);
        ArrayAdapter<CharSequence> mosaicAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.mosaic_names, android.R.layout.simple_spinner_dropdown_item);
        mosaicSpinner.setAdapter(mosaicAdapter);

        String mosaic = settings.getString("last_mosaic", "");
        int index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values)).indexOf(mosaic);
        mosaicSpinner.setSelection(index);

        final Switch loopSwitch = view.findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_mosaic_loop", false));

        Button viewButton = view.findViewById(R.id.viewButton);
        viewButton.setOnClickListener(view -> viewMosaic());

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (OnMosaicSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMosaicSelectedListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnMosaicSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMosaicSelectedListener");
        }
    }

    private void viewMosaic() {
        Spinner mosaicSpinner = view.findViewById(R.id.mosaicSpinner);
        Switch loopSwitch = view.findViewById(R.id.loopSwitch);

        String mosaic = getResources().getStringArray(R.array.mosaic_values)[mosaicSpinner.getSelectedItemPosition()];
        Boolean loop = loopSwitch.isChecked();

        callback.onMosaicSelected(mosaic, loop);
    }
}
