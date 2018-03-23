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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.danhasting.radar.database.Favorite;
import com.danhasting.radar.fragments.NeedKeyFragment;
import com.danhasting.radar.fragments.RadarFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RadarActivity extends MainActivity {

    private String source;
    private String type;
    private String location;
    private Boolean loop;
    private Boolean enhanced;
    private int distance;

    private String radarName;
    private Boolean needKey;

    private NavigationView navigationView;

    private MenuItem addFavorite;
    private MenuItem removeFavorite;

    private Timer timer;
    private Boolean refreshed = true;

    private RadarFragment radarFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Boolean fullscreen = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("show_fullscreen", false);
        if (fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_radar, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        navigationView = findViewById(R.id.nav_view);

        Intent intent = getIntent();
        source = intent.getStringExtra("source");
        type = intent.getStringExtra("type");
        location = intent.getStringExtra("location");
        loop = intent.getBooleanExtra("loop", false);
        enhanced = intent.getBooleanExtra("enhanced", false);
        distance = intent.getIntExtra("distance", 50);


        needKey = source.equals("wunderground") && !settings.getBoolean("api_key_activated", false);
        if (needKey) {
            NeedKeyFragment needKeyFragment = new NeedKeyFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, needKeyFragment).commit();
            return;
        } else {
            radarFragment = new RadarFragment();
            radarFragment.setArguments(intent.getExtras());
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, radarFragment).commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (fullscreen && actionBar != null) {
            getSupportActionBar().hide();
            findViewById(R.id.radarLayout).setPadding(0, 0, 0, 0);
        }

        int index;
        switch (source) {
            case "wunderground":
                radarName = intent.getStringExtra("name");
                if (radarName == null) radarName = getString(R.string.wunderground_title);
                break;
            case "mosaic":
                index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values)).indexOf(location);
                radarName = getResources().getStringArray(R.array.mosaic_names)[index];
                break;
            case "nws":
                index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
                radarName = getResources().getStringArray(R.array.location_names)[index];
                break;
        }

        if (intent.getBooleanExtra("favorite", false)) {
            radarName = intent.getStringExtra("name");
            currentFavorite = intent.getIntExtra("favoriteID", -1);
        } else
            radarName = radarName.replaceAll("[^/]+/ ", "");

        if (radarName != null)
            setTitle(radarName);

        scheduleRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!source.equals("wunderground") || settings.getBoolean("api_key_activated", false)) {
            if (!refreshed && !(loop && source.equals("mosaic"))) { // Mosaic loops are large, don't auto-refresh
                if (radarFragment != null) radarFragment.refreshRadar();
                scheduleRefresh();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data.getBooleanExtra("from_settings", false))
            recreate();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        initializeMenu(menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        initializeMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        itemSelected(item);
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        itemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    private void initializeMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.radar_actions, menu);

        addFavorite = menu.findItem(R.id.action_add_favorite);
        removeFavorite = menu.findItem(R.id.action_remove_favorite);
        MenuItem refresh = menu.findItem(R.id.action_refresh);

        List<Favorite> favorites = settingsDB.favoriteDao().findByData(
                source, location, type, loop, enhanced, distance);

        if (favorites.size() > 0) {
            addFavorite.setVisible(false);
            currentFavorite = favorites.get(0).getUid();
        } else {
            removeFavorite.setVisible(false);
        }

        if (needKey)
            refresh.setVisible(false);
    }

    private void itemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite) {
            addFavoriteDialog();
        } else if (id == R.id.action_remove_favorite) {
            removeFavoriteDialog();
        } else if (id == R.id.action_refresh) {
            refreshRadar();
        }
    }

    private void addFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Favorite");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (radarName != null) input.setText(radarName);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing as we will override below
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String name = input.getText().toString();
                Favorite exists = settingsDB.favoriteDao().findByName(name);
                if (name.equals("")) {
                    input.setError(getString(R.string.empty_name_error));
                } else if (exists != null) {
                    input.setError(getString(R.string.already_exists_error));
                } else {
                    Favorite favorite = new Favorite();
                    favorite.setSource(source);
                    favorite.setName(input.getText().toString());
                    favorite.setLocation(location);
                    favorite.setType(type);
                    favorite.setLoop(loop);
                    favorite.setEnhanced(enhanced);
                    favorite.setDistance(distance);
                    settingsDB.favoriteDao().insertAll(favorite);

                    addFavorite.setVisible(false);
                    removeFavorite.setVisible(true);
                    populateFavorites(navigationView.getMenu());
                    dialog.dismiss();
                }
            }
        });
    }

    private void removeFavoriteDialog() {
        DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    List<Favorite> favorites = settingsDB.favoriteDao()
                            .findByData(source, location, type, loop, enhanced, distance);

                    for (Favorite favorite : favorites) {
                        settingsDB.favoriteDao().delete(favorite);
                    }

                    addFavorite.setVisible(true);
                    removeFavorite.setVisible(false);
                    populateFavorites(navigationView.getMenu());
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.confirm_favorite_removal)
                .setPositiveButton("Yes", dialogListener)
                .setNegativeButton("No", dialogListener)
                .show();
    }

    private void refreshRadar() {
        if (!refreshed) {
            if (radarFragment != null) radarFragment.refreshRadar();
            scheduleRefresh();
        } else {
            DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (radarFragment != null) radarFragment.refreshRadar();
                        scheduleRefresh();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_refresh)
                    .setPositiveButton("Yes", dialogListener)
                    .setNegativeButton("No", dialogListener)
                    .show();
        }
    }

    private void scheduleRefresh() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        refreshed = true;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshed = false;
            }
        }, 1000 * 60 * 5, 1000 * 60 * 5);
    }
}
