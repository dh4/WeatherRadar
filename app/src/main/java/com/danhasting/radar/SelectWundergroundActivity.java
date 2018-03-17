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
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

public class SelectWundergroundActivity extends MainActivity {

    private EditText locationEditText;
    private Switch loopSwitch;
    private TextView radiusNumber;
    private Button viewButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.select_wunderground_image);

        if (!settings.getBoolean("api_key_activated", false)) {
            inflateNeedKeyView();
            return;
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_select_wunderground, mDrawerLayout, false);
            mDrawerLayout.addView(contentView, 0);
        }

        locationEditText = findViewById(R.id.wunderground_location);
        locationEditText.setText(settings.getString("last_wunderground", ""));

        loopSwitch = findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_wunderground_loop", false));


        int distance = settings.getInt("last_wunderground_distance", 50);

        radiusNumber = findViewById(R.id.radiusNumber);
        radiusNumber.setText(String.valueOf(distance));

        final SeekBar radiusBar = findViewById(R.id.radiusBar);
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

        TextView radiusText = findViewById(R.id.radiusText);
        String currentText = radiusText.getText().toString();

        String unitsValue = settings.getString("distance_units", getString(R.string.distance_unit_default));
        int index = Arrays.asList(getResources().getStringArray(R.array.distance_unit_values)).indexOf(unitsValue);
        String unitsName = getResources().getStringArray(R.array.distance_unit_names)[index];
        String newText = currentText + String.format(" (in %s)", unitsName.toLowerCase());
        radiusText.setText(newText);

        viewButton = findViewById(R.id.viewButton);
        viewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewWunderground();
                viewButton.setText(R.string.loading);
                viewButton.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (settings.getBoolean("api_key_activated", false) && viewButton != null) {
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

        RequestParams params = new RequestParams();
        params.put("query", location);

        AsyncHttpClient client = new AsyncHttpClient();
        String autocompleteURL = "https://autocomplete.wunderground.com/aq";

        client.get(autocompleteURL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int status, Header[] headers, JSONObject json) {
                TreeMap<String, String> options = new TreeMap<>();

                try {
                    String resultsString = json.getString("RESULTS");
                    JSONArray results = new JSONArray(resultsString);
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject row = results.getJSONObject(i);
                        options.put(row.getString("name"), row.getString("zmw"));
                    }

                    if (options.size() > 1) {
                        Intent chooserIntent = new Intent(SelectWundergroundActivity.this,
                                ChooserActivity.class);
                        chooserIntent.putExtra("location_options", options);
                        chooserIntent.putExtra("loop", loop);
                        chooserIntent.putExtra("distance", distance);
                        chooserIntent.putExtra("wunderground", true);

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("last_wunderground", location);
                        editor.putBoolean("last_wunderground_loop", loop);
                        editor.putInt("last_wunderground_distance", distance);
                        editor.apply();

                        SelectWundergroundActivity.this.startActivity(chooserIntent);
                    } else if (options.size() == 1) {
                        Intent radarIntent = new Intent(SelectWundergroundActivity.this,
                                RadarActivity.class);
                        radarIntent.putExtra("location", options.firstEntry().getValue());
                        radarIntent.putExtra("name", options.firstEntry().getKey());
                        radarIntent.putExtra("loop", loop);
                        radarIntent.putExtra("distance", distance);
                        radarIntent.putExtra("wunderground", true);

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("last_wunderground", options.firstEntry().getKey());
                        editor.putBoolean("last_wunderground_loop", loop);
                        editor.putInt("last_wunderground_distance", distance);
                        editor.apply();

                        SelectWundergroundActivity.this.startActivity(radarIntent);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.no_results_error, Toast.LENGTH_LONG).show();
                        viewButton.setText(R.string.view_wunderground_image);
                        viewButton.setEnabled(true);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(),
                            R.string.connection_error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    viewButton.setText(R.string.view_wunderground_image);
                    viewButton.setEnabled(true);
                }
            }

            @Override
            public void onFailure(int status, Header[] h, Throwable t, JSONObject e) {
                Toast.makeText(getApplicationContext(),
                        R.string.connection_error, Toast.LENGTH_LONG).show();
                viewButton.setText(R.string.view_wunderground_image);
                viewButton.setEnabled(true);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false))
            recreate();
    }
}

