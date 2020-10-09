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
package com.danhasting.radar.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;
import com.danhasting.radar.fragments.SelectMosaicFragment;
import com.danhasting.radar.fragments.SelectNWSFragment;

public class SelectActivity extends MainActivity
        implements SelectNWSFragment.OnNWSSelectedListener,
        SelectMosaicFragment.OnMosaicSelectedListener {

    private Source currentSelection = Source.NWS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_select, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        Intent intent = getIntent();
        Source selection = (Source) intent.getSerializableExtra("selection");
        if (selection != null) launchSelectionFragment(selection);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Source selection = (Source) intent.getSerializableExtra("selection");
        if (selection != null) launchSelectionFragment(selection);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        int id = menuItem.getItemId();

        switch (id) {
            case R.id.nav_nws:
                launchSelectionFragment(Source.NWS);
                break;
            case R.id.nav_mosaic:
                launchSelectionFragment(Source.MOSAIC);
                break;
        }

        super.onNavigationItemSelected(menuItem);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false)) {
            launchSelectionFragment(currentSelection, true);
        }
    }

    private void launchSelectionFragment(Source selection, Boolean force) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);

        switch (selection) {
            case NWS:
                if (!(fragment instanceof SelectNWSFragment) || force) {
                    setTitle(R.string.select_radar_image);
                    SelectNWSFragment nwsFragment = new SelectNWSFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, nwsFragment).commit();
                }
                break;

            case MOSAIC:
                if (!(fragment instanceof SelectMosaicFragment) || force) {
                    setTitle(R.string.select_mosaic_image);
                    SelectMosaicFragment mosaicFragment = new SelectMosaicFragment();
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, mosaicFragment).commit();
                }
                break;
        }

        currentSelection = selection;
    }

    private void launchSelectionFragment(Source selection) {
        launchSelectionFragment(selection, false);
    }

    private void onSelected(Source source, String location, String type,
                            Boolean loop, Boolean enhanced) {
        Intent radarIntent = new Intent(SelectActivity.this, RadarActivity.class);

        radarIntent.putExtra("source", source);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("name", location);
        radarIntent.putExtra("type", type);
        radarIntent.putExtra("loop", loop);
        radarIntent.putExtra("enhanced", enhanced);
        radarIntent.putExtra("distance", 50); //No longer used now that Wunderground is removed

        startActivity(radarIntent);
    }

    public void onNWSSelected(String location, String type, Boolean loop, Boolean enhanced) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_nws_location", location);
        editor.putString("last_nws_type", type);
        editor.putBoolean("last_nws_loop", loop);
        editor.putBoolean("last_nws_enhanced", enhanced);
        editor.apply();

        onSelected(Source.NWS, location, type, loop, enhanced);
    }

    public void onMosaicSelected(String location, Boolean loop) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_mosaic", location);
        editor.putBoolean("last_mosaic_loop", loop);
        editor.apply();

        onSelected(Source.MOSAIC, location, null, loop, false);
    }
}
