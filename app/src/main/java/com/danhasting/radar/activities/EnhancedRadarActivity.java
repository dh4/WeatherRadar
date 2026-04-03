/*
 * Copyright (c) 2026, Dan Hasting
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;
import com.danhasting.radar.helpers.RadarMenu;
import com.danhasting.radar.fragments.EnhancedRadarFragment;

import java.util.concurrent.CompletableFuture;


public class EnhancedRadarActivity extends MainActivity implements RadarMenu.Ui {
    private RadarMenu radarMenu;

    private String location;

    private EnhancedRadarFragment radarWebsiteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        radarMenu = new RadarMenu(this);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_enhanced_radar, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        if (location == null) location = "";

        radarWebsiteFragment = new EnhancedRadarFragment();
        radarWebsiteFragment.setArguments(intent.getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, radarWebsiteFragment).commit();

        if (intent.getBooleanExtra("favorite", false))
            radarMenu.setFavoriteName(intent.getStringExtra("name"));

        setTitle(getResources().getString(R.string.nws));
        radarMenu.setFullscreen(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("show_radar_fullscreen", false));

        // Listen for changed URL settings
        getSupportFragmentManager().setFragmentResultListener(
                "current_settings",
                this,
                (requestKey, bundle) -> {
                    location = bundle.getString("settings");
                    radarMenu.checkFavorite();
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false)) {
            radarMenu.setFullscreen(PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("show_radar_fullscreen", false));
            refreshRadar();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        radarMenu.setFullscreen(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("show_radar_fullscreen", false));

        Bundle extras = intent.getExtras();

        if (extras != null) {
            Intent newIntent = new Intent(this, EnhancedRadarActivity.class);
            newIntent.putExtras(extras);
            startActivity(newIntent);
            finish();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        radarMenu.initializeMenu(menu, true);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        radarMenu.initializeMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        radarMenu.itemSelected(item);
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        radarMenu.itemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    public void refreshRadar() {
        if (radarWebsiteFragment != null) radarWebsiteFragment.refreshEnhancedRadar("");
    }



    @Override public Context getContext() { return this; }
    @Override public AppCompatActivity getActivity() { return this; }
    @Override public void setTitle(CharSequence title) { super.setTitle(getResources().getString(R.string.nws)); } // Keep the title static
    @Override public int getSourceInt() { return Source.RADAR.getInt(); }
    @Override public String getLocation() {
        return radarWebsiteFragment != null ? radarWebsiteFragment.getCurrentSettings() : null;
    }
    @Override public String getType() { return null; }
    @Override public Boolean getLoop() { return null; }
    @Override public Boolean getEnhanced() { return null; }
    @Override public int getDistance() { return 0; }
    @Override public CompletableFuture<String> getCurrentLocationNameAsync() {
        return radarWebsiteFragment != null ? radarWebsiteFragment.getCurrentLocationNameAsync() : null;
    }
    @Override public void setCurrentFavorite(Integer favorite) {} // Do nothing since enhanced radar can move away from favorite
}
