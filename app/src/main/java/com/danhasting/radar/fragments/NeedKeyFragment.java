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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.danhasting.radar.R;
import com.danhasting.radar.SettingsActivity;

public class NeedKeyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_missing, container, false);

        Button needKey = view.findViewById(R.id.needKeyButton);
        needKey.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent browser = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.wunderground.com/weather/api/"));
                startActivity(browser);
            }

        });

        Button openSettings = view.findViewById(R.id.openSettingsButton);
        openSettings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivityForResult(settingsIntent, 1);
            }

        });

        return view;
    }
}
