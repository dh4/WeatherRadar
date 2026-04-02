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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;
import com.danhasting.radar.fragments.RadarFragment;
import com.danhasting.radar.helpers.RadarMenu;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RadarActivity extends MainActivity implements RadarMenu.Ui {
    private RadarMenu radarMenu;

    private Source source;
    private String type;
    private String location;
    private Boolean loop;
    private Boolean enhanced;
    private int distance;

    private String sourceName;

    private Timer timer;
    private Boolean refreshed = true;
    private Boolean paused = false;
    private Date lastPause;

    private RadarFragment radarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean fullscreen = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("show_fullscreen", false);

        radarMenu = new RadarMenu(this);

        if (fullscreen) {
            Window window = getWindow();
            WindowCompat.setDecorFitsSystemWindows(window, false);

            WindowInsetsControllerCompat insetsController =
                    new WindowInsetsControllerCompat(window, window.getDecorView());

            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            insetsController.hide(WindowInsetsCompat.Type.systemBars());
        }

        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_radar, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        Intent intent = getIntent();
        source = (Source) intent.getSerializableExtra("source");
        type = intent.getStringExtra("type");
        location = intent.getStringExtra("location");
        loop = intent.getBooleanExtra("loop", false);
        enhanced = intent.getBooleanExtra("enhanced", false);
        distance = intent.getIntExtra("distance", 50);

        if (source == null) source = Source.NWS;
        if (type == null) type = "";
        if (location == null) location = "";

        radarFragment = new RadarFragment();
        radarFragment.setArguments(intent.getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, radarFragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (fullscreen && actionBar != null) {
            getSupportActionBar().hide();
            findViewById(R.id.radarLayout).setPadding(0, 0, 0, 0);
        }

        int index;
        switch (source) {
            case MOSAIC:
                index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values))
                        .indexOf(location);
                sourceName = getResources().getStringArray(R.array.mosaic_names)[index];
                break;
            case NWS:
                index = Arrays.asList(getResources().getStringArray(R.array.location_values))
                        .indexOf(location);
                sourceName = getResources().getStringArray(R.array.location_names)[index];
                sourceName = sourceName.replaceAll("[^/]+/ ", "");
                break;
        }

        if (intent.getBooleanExtra("favorite", false)) {
            String name = intent.getStringExtra("name");
            setTitle(name);
            radarMenu.setFavoriteName(name);
            currentFavorite = intent.getIntExtra("favoriteID", -1);
        } else {
            setTitle(sourceName);
            radarMenu.setFavoriteName(sourceName); // In case they want to add it as a favorite
        }

        scheduleRefresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        if (extras != null) {
            Intent newIntent = new Intent(this, RadarActivity.class);
            newIntent.putExtras(extras);
            startActivity(newIntent);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        paused = false;

        if (lastPause != null) {
            Date now = Calendar.getInstance().getTime();
            long seconds = (now.getTime() - lastPause.getTime()) / 1000;

            if (seconds > 60 * 5)
                refreshed = false;
        }

        // Mosaic loops are large, don't auto-refresh
        if (!refreshed && !(loop && source == Source.MOSAIC) && autoRefresh()) {
            if (radarFragment != null) radarFragment.refreshRadar();
            scheduleRefresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        paused = true;
        lastPause = Calendar.getInstance().getTime();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false))
            recreate();
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
        if (!refreshed || !settings.getBoolean("prompt_to_refresh",false)) {
            if (radarFragment != null) radarFragment.refreshRadar();
            scheduleRefresh();
        } else {
            DialogInterface.OnClickListener dialogListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (radarFragment != null) radarFragment.refreshRadar();
                    scheduleRefresh();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_refresh)
                    .setPositiveButton(getString(R.string.button_yes), dialogListener)
                    .setNegativeButton(getString(R.string.button_no), dialogListener)
                    .show();
        }
    }


    public void setCurrentFavorite(Integer favorite) {
        currentFavorite = favorite;
    }

    private Boolean autoRefresh() {
        String refresh = settings.getString("auto_refresh",
                getString(R.string.wifi_toggle_default));

        return (refresh.equals("always") || (refresh.equals("wifi") && onWifi()));
    }

    private void scheduleRefresh() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        refreshed = true;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshed = false;

                if (autoRefresh() && !paused) {
                    runOnUiThread(() -> {
                        if (radarFragment != null) radarFragment.refreshRadar();
                        scheduleRefresh();
                    });
                }
            }
        }, 1000 * 60 * 5);
    }

    // Ui interface implementations:
    @Override public Context getContext() { return this; }
    @Override public Activity getActivity() { return this; }
    @Override public void setTitle(CharSequence title) { super.setTitle(title == null ? sourceName : title); }
    @Override public int getSourceInt() { return source.getInt(); }
    @Override public String getLocation() { return location; }
    @Override public String getType() { return type; }
    @Override public Boolean getLoop() { return loop; }
    @Override public Boolean getEnhanced() { return enhanced; }
    @Override public int getDistance() { return distance; }
}
