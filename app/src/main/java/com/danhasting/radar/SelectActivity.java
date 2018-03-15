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
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.Arrays;

public class SelectActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_select, mDrawerLayout, false);
            mDrawerLayout.addView(contentView, 0);
        }

        setTitle(R.string.select_radar_image);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.type_names, android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        String type = settings.getString("last_type","");
        int index = Arrays.asList(getResources().getStringArray(R.array.type_values)).indexOf(type);
        typeSpinner.setSelection(index);

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this, R.array.location_names, android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        String location = settings.getString("last_location","");
        index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
        locationSpinner.setSelection(index);

        final Switch loopSwitch = findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_loop",false));

        Switch enhancedSwitch = findViewById(R.id.enhancedSwitch);
        if (settings.getBoolean("last_enhanced",false)) {
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
    }

    public void viewRadar(View v) {
        Intent radarIntent = new Intent(SelectActivity.this, RadarActivity.class);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        Switch loopSwitch = findViewById(R.id.loopSwitch);
        Switch enhancedSwitch = findViewById(R.id.enhancedSwitch);

        String location = getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()];
        String type = getResources().getStringArray(R.array.type_values)[typeSpinner.getSelectedItemPosition()];
        Boolean loop = loopSwitch.isChecked();
        Boolean enhanced = enhancedSwitch.isChecked();

        if (enhanced) loop = false;

        radarIntent.putExtra("type", type);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("loop", loop);
        radarIntent.putExtra("enhanced", enhanced);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_location", location);
        editor.putString("last_type", type);
        editor.putBoolean("last_loop", loop);
        editor.putBoolean("last_enhanced", enhanced);
        editor.apply();

        SelectActivity.this.startActivity(radarIntent);
    }
}
