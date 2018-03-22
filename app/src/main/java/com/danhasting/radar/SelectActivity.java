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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.danhasting.radar.fragments.SelectNWSFragment;
import com.danhasting.radar.fragments.SelectMosaicFragment;
import com.danhasting.radar.fragments.SelectWundergroundFragment;

public class SelectActivity extends MainActivity {
    private String currentSelection = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_select, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        Intent intent = getIntent();
        String selection = intent.getStringExtra("selection");
        if (selection != null) launchSelectionFragment(selection);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_nws)
            launchSelectionFragment("nws");
        else if (id == R.id.nav_mosaic)
            launchSelectionFragment("mosaic");
        else if (id == R.id.nav_wunderground)
            launchSelectionFragment("wunderground");

        super.onNavigationItemSelected(menuItem);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false))
            recreate();
    }

    private void launchSelectionFragment(String selection) {
        switch (selection) {
            case "nws":
                if (!currentSelection.equals("nws")) {
                    setTitle(R.string.select_radar_image);
                    SelectNWSFragment nwsFragment = new SelectNWSFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, nwsFragment).commit();
                }
                break;

            case "mosaic":
                if (!currentSelection.equals("mosaic")) {
                    setTitle(R.string.select_mosaic_image);
                    SelectMosaicFragment mosaicFragment = new SelectMosaicFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mosaicFragment).commit();
                }
                break;

            case "wunderground":
                if (!currentSelection.equals("wunderground")) {
                    if (!settings.getBoolean("api_key_activated", false)) {
                        inflateNeedKeyView();
                        return;
                    }
                    setTitle(R.string.select_wunderground_image);
                    SelectWundergroundFragment wundergroundFragment = new SelectWundergroundFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, wundergroundFragment).commit();
                }
                break;
        }

        currentSelection = selection;
    }
}
