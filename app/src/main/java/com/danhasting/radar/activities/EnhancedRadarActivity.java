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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;

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
            View contentView = inflater.inflate(R.layout.activity_radar_website, drawerLayout, false);
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
        setFullscreen();

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
            setFullscreen();
            refreshRadar();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setFullscreen();

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
        if (radarWebsiteFragment != null) radarWebsiteFragment.refreshRadarWebsite("");
    }

    private void setFullscreen() {
        boolean fullscreen = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("show_radar_fullscreen", false);

        Window window = getWindow();

        if (fullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(window, window.getDecorView());

            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            insetsController.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true);

            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(window, window.getDecorView());

            insetsController.show(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
        }

        ActionBar actionBar = getSupportActionBar();
        if (fullscreen && actionBar != null) {
            getSupportActionBar().hide();
            findViewById(R.id.radarWebsiteLayout).setPadding(0, 0, 0, 0);
        } else if (!fullscreen && actionBar != null) {
            actionBar.show();

            TypedValue tv = new TypedValue();
            int actionBarHeight = 0;
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());

            findViewById(R.id.radarWebsiteLayout).setPadding(0, actionBarHeight, 0, 0);
        }
    }

    @Override public Context getContext() { return this; }
    @Override public Activity getActivity() { return this; }
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
