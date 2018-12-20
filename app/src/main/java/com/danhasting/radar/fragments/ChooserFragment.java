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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.danhasting.radar.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


public class ChooserFragment extends Fragment {

    private View view;
    private OnChooserSelectedListener callback;

    public interface OnChooserSelectedListener {
        void onChooserSelected(String name, String location, String type, Boolean loop, int distance);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_chooser, container, false);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            callback = (OnChooserSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnChooserSelectedListener");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            callback = (OnChooserSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChooserSelectedListener");
        }
    }

    public void populateList(final TreeMap<String, String> options, final String type,
                             final Boolean loop, final int distance) {
        ListView chooserList = view.findViewById(R.id.chooser_list);
        final ArrayList<String> optionNames = new ArrayList<>();

        for (Map.Entry<String, String> option : options.entrySet())
            optionNames.add(option.getKey().replace(",", "  ,"));

        Collections.sort(optionNames, String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < optionNames.size(); i++)
            optionNames.set(i, optionNames.get(i).replace("  ,", ","));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, optionNames);
        chooserList.setAdapter(adapter);

        chooserList.setOnItemClickListener((parent, view, position, id) -> {
            String name = optionNames.get(position);
            String location = options.get(name);
            callback.onChooserSelected(name, location, type, loop, distance);
        });
    }
}
