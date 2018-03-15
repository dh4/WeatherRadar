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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

public class RadarActivity extends MainActivity {

    private String type;
    private String location;
    private Boolean loop;
    private Boolean enhanced;
    private Boolean mosaic;
    private Boolean wunderground;
    private int distance;

    private String radarName;

    private MenuItem addFavorite;
    private MenuItem removeFavorite;
    private NavigationView navigationView;

    private WebView radarWebView;

    private Timer timer;
    private Boolean refreshed;

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

        Intent intent = getIntent();
        type = intent.getStringExtra("type");
        location = intent.getStringExtra("location");
        loop = intent.getBooleanExtra("loop", false);
        enhanced = intent.getBooleanExtra("enhanced", false);
        mosaic = intent.getBooleanExtra("mosaic", false);
        wunderground = intent.getBooleanExtra("wunderground", false);
        distance = intent.getIntExtra("distance", 50);


        if (type == null) type = "";
        if (location == null) location = "";


        if (wunderground && !settings.getBoolean("api_key_activated", false)) {
            inflateNeedKeyView();
            return;
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_radar, mDrawerLayout, false);
            mDrawerLayout.addView(contentView, 0);
        }

        ActionBar actionBar = getSupportActionBar();
        if (fullscreen && actionBar != null) {
            getSupportActionBar().hide();
            findViewById(R.id.radarLayout).setPadding(0, 0, 0, 0);
        }

        navigationView = findViewById(R.id.nav_view);

        if (wunderground) {
            radarName = intent.getStringExtra("name");
            if (radarName == null) radarName = getString(R.string.wunderground_title);
        } else if (mosaic) {
            int index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values)).indexOf(location);
            radarName = getResources().getStringArray(R.array.mosaic_names)[index];
        } else {
            int index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
            radarName = getResources().getStringArray(R.array.location_names)[index];
        }

        if (intent.getBooleanExtra("favorite", false)) {
            radarName = intent.getStringExtra("name");
            currentFavorite = intent.getIntExtra("favoriteID", -1);
        } else
            radarName = radarName.replaceAll("[^/]+/ ", "");
        setTitle(radarName);

        radarWebView = findViewById(R.id.radarWebView);
        radarWebView.getSettings().setLoadWithOverviewMode(true);
        radarWebView.getSettings().setUseWideViewPort(true);
        radarWebView.getSettings().setBuiltInZoomControls(true);
        radarWebView.getSettings().setDisplayZoomControls(false);
        radarWebView.getSettings().setJavaScriptEnabled(true);
        radarWebView.getSettings().setDomStorageEnabled(true);
        radarWebView.getSettings().setSupportZoom(true);

        if (enhanced) {
            radarWebView.loadData(displayEnhancedRadar(location, type), "text/html", null);
        } else if (mosaic) {
            radarWebView.loadData(displayMosaicImage(location, loop), "text/html", null);
        } else if (wunderground) {
            // We dynamically set the size for wunderground images, so wait for the layout to load
            final ViewTreeObserver observer = radarWebView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    radarWebView.loadData(displayWundergroundImage(location, loop, distance),
                            "text/html", null);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                        radarWebView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    else
                        radarWebView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        } else {
            radarWebView.loadData(displayLiteImage(location, type, loop), "text/html", null);
        }

        scheduleRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!wunderground || settings.getBoolean("api_key_activated", false)) {
            if (!refreshed && !(loop && mosaic)) { // Mosaic loops are large, don't auto-refresh
                radarWebView.reload();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.radar_actions, menu);

        addFavorite = menu.findItem(R.id.action_add_favorite);
        removeFavorite = menu.findItem(R.id.action_remove_favorite);

        List<Favorite> favorites = settingsDB.favoriteDao().findByData(location, type, loop,
                enhanced, mosaic, wunderground, distance);

        if (favorites.size() > 0) {
            addFavorite.setVisible(false);
            currentFavorite = favorites.get(0).getUid();
        } else {
            removeFavorite.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite) {
            addFavoriteDialog();
        } else if (id == R.id.action_remove_favorite) {
            removeFavoriteDialog();
        } else if (id == R.id.action_refresh) {
            refreshRadar();
        }

        return super.onOptionsItemSelected(item);
    }

    private void addFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Favorite");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(radarName);
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
                    favorite.setName(input.getText().toString());
                    favorite.setLocation(location);
                    favorite.setType(type);
                    favorite.setLoop(loop);
                    favorite.setEnhanced(enhanced);
                    favorite.setMosaic(mosaic);
                    favorite.setWunderground(wunderground);
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
                            .findByData(location, type, loop, enhanced, mosaic, wunderground, distance);

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
            radarWebView.reload();
            scheduleRefresh();
        } else {
            DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        radarWebView.reload();
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

    private String displayMosaicImage(String mosaic, Boolean loop) {
        String url = "https://radar.weather.gov/Conus/";
        if (loop) {
            if (mosaic.equals("latest")) {
                url += "Loop/NatLoop.gif";
            } else {
                url += "Loop/" + mosaic + "_loop.gif";
            }
        } else {
            url += "RadarImg/"+mosaic+".gif";
        }

        return displayRadar(url);
    }

    private String displayLiteImage(String loc, String type, Boolean loop) {
        String url = "https://radar.weather.gov/lite/"+type+"/";
        if (loop) {
            url += loc+"_loop.gif";
        } else {
            url += loc+"_0.png";
        }

        return displayRadar(url);
    }

    private String displayWundergroundImage(String loc, Boolean loop, int distance) {
        String apiKey = settings.getString("api_key","");
        int time_label = settings.getBoolean("show_time_label", true) ? 1 : 0;
        int snow = settings.getBoolean("show_snow_mix", true) ? 1 : 0;
        int smooth = settings.getBoolean("smoothing", true) ? 1 : 0;
        int noclutter = settings.getBoolean("noclutter", false) ? 1 : 0;
        String animateText = "radar";
        if (loop) animateText = "animatedradar";

        String units = settings.getString("distance_units", getString(R.string.distance_unit_default));
        String speed = settings.getString("animation_speed", getString(R.string.animation_speed_default));
        String res = settings.getString("image_resolution", getString(R.string.image_resolution_default));
        String frames = settings.getString("animation_frames", getString(R.string.animation_frames_default));

        int width = radarWebView.getWidth();
        int height = radarWebView.getHeight();

        int imageWidth = Integer.parseInt(res);
        int imageHeight = Integer.parseInt(res);

        if (width > height) {
            Float aspect = (float)width / height;
            imageWidth = Math.round(imageHeight * aspect);
        } else {
            Float aspect = (float)height / width;
            imageHeight = Math.round(imageWidth * aspect);
        }

        String url = "http://api.wunderground.com/api/%s/%s/q/%s.gif" +
                "?width=%s&height=%s&newmaps=1&radius=%s&radunits=%s&smooth=%s&delay=%s&num=%s" +
                "&rainsnow=%s&noclutter=%s&timelabel=%s&timelabel.y=15&timelabel.x=5";
        url = String.format(url, apiKey, animateText, loc, imageWidth, imageHeight,
                distance, units, smooth, speed, frames, snow, noclutter, time_label);

        return displayRadar(url);
    }

    private String displayRadar(String url) {
        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("lite_radar");
        html.set("url", url);
        if (!wunderground)
            html.set("maximized", Boolean.toString(settings.getBoolean("show_maximized", false)));

        return html.toString();
    }

    private String displayEnhancedRadar(String location, String type) {
        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("enhanced_radar");
        html.set("location", location);
        html.set("type", type);
        html.set("maximized", Boolean.toString(settings.getBoolean("show_maximized", false)));

        if (type.equals("N0Z"))
            html.set("distance", "Long");
        else
            html.set("distance", "Short");

        return html.toString();
    }
}
