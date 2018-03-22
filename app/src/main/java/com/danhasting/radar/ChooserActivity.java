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
package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ChooserActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_chooser, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        setTitle(R.string.chooser_title);

        ListView chooserList = findViewById(R.id.chooser_list);
        final ArrayList<String> optionNames = new ArrayList<>();

        Intent intent = getIntent();
        @SuppressWarnings("unchecked") final HashMap<String, String> optionsHash =
                (HashMap<String, String>) intent.getSerializableExtra("location_options");
        final Boolean loop = intent.getBooleanExtra("loop", false);
        final int distance = intent.getIntExtra("distance", 50);

        for (Map.Entry<String, String> option : optionsHash.entrySet())
            optionNames.add(option.getKey().replace(",","  ,"));

        Collections.sort(optionNames, String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < optionNames.size(); i++)
            optionNames.set(i, optionNames.get(i).replace("  ,",","));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, optionNames);
        chooserList.setAdapter(adapter);

        chooserList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = optionNames.get(position);
                String location = optionsHash.get(name);

                Intent radarIntent = new Intent(ChooserActivity.this, RadarActivity.class);
                radarIntent.putExtra("location", location);
                radarIntent.putExtra("name", name);
                radarIntent.putExtra("loop", loop);
                radarIntent.putExtra("distance", distance);
                radarIntent.putExtra("wunderground", true);

                SharedPreferences.Editor editor = settings.edit();
                editor.putString("last_wunderground", name);
                editor.putBoolean("last_wunderground_loop", loop);
                editor.apply();

                ChooserActivity.this.startActivity(radarIntent);
            }
        });
    }
}
