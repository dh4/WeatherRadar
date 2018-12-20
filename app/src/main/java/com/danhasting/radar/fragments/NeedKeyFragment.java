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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.danhasting.radar.R;

public class NeedKeyFragment extends Fragment {

    private OnOpenSettingsListener callback;

    public interface OnOpenSettingsListener {
        void openSettings();

        void testWunderground();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_key_missing, container, false);

        Button needKey = view.findViewById(R.id.needKeyButton);
        needKey.setOnClickListener(view1 -> {
            Intent browser = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.wunderground.com/weather/api/"));
            startActivity(browser);
        });

        Button openSettings = view.findViewById(R.id.openSettingsButton);
        openSettings.setOnClickListener(view12 -> {
            if (callback != null)
                callback.openSettings();
        });

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Boolean limited = settings.getBoolean("is_test_limit", false);
        final int limit = settings.getInt("test_limit", 5);

        if (limited) {
            int used = settings.getInt("test_used", 0);
            TextView wundergroundTest = view.findViewById(R.id.testWunderground);
            wundergroundTest.setText(String.format(getString(R.string.test_wunderground_limit),
                    used, limit));
        }

        Button testWunderground = view.findViewById(R.id.testWundergroundButton);
        testWunderground.setOnClickListener(view13 -> {
            int used = settings.getInt("test_used", 0);

            if (settings.getBoolean("is_test_limit", false) && used >= limit)
                Toast.makeText(getActivity().getApplicationContext(),
                        getString(R.string.passed_limit_error), Toast.LENGTH_LONG).show();
            else if (callback != null)
                callback.testWunderground();
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (OnOpenSettingsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnOpenSettingsListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnOpenSettingsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnOpenSettingsListener");
        }
    }
}
