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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.danhasting.radar.R;
import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;
import com.danhasting.radar.database.Source;
import com.danhasting.radar.fragments.RadarWebsiteFragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RadarWebsiteActivity extends MainActivity {

    private String location;

    private String sourceName;
    private String radarName;

    private MenuItem addFavorite;
    private MenuItem removeFavorite;

    private MenuItem contextAddFavorite;
    private MenuItem contextRemoveFavorite;
    private MenuItem contextEditFavorite;

    private Boolean contextMenu = false;

    private RadarWebsiteFragment radarWebsiteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean fullscreen = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("show_fullscreen", false);
        if (fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_radar_website, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        Intent intent = getIntent();
        location = intent.getStringExtra("location");
        if (location == null) location = "";

        radarWebsiteFragment = new RadarWebsiteFragment();
        radarWebsiteFragment.setArguments(intent.getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, radarWebsiteFragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (fullscreen && actionBar != null) {
            getSupportActionBar().hide();
            findViewById(R.id.radarWebsiteLayout).setPadding(0, 0, 0, 0);
        }

        sourceName = getResources().getString(R.string.nws);
        setTitle(sourceName);

        // Listen for changed URL settings
        getSupportFragmentManager().setFragmentResultListener(
                "current_settings",
                this,
                (requestKey, bundle) -> {
                    location = bundle.getString("settings");
                    checkFavorite();
                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();

        if (extras != null) {
            Intent newIntent = new Intent(this, RadarWebsiteActivity.class);
            newIntent.putExtras(extras);
            startActivity(newIntent);
            finish();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        initializeMenu(menu, true);
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

    private void initializeMenu(Menu menu, final Boolean context) {
        getMenuInflater().inflate(R.menu.radar_actions, menu);

        contextMenu = context;

        if (contextMenu) {
            contextAddFavorite = menu.findItem(R.id.action_add_favorite);
            contextRemoveFavorite = menu.findItem(R.id.action_remove_favorite);
            contextEditFavorite = menu.findItem(R.id.action_edit_favorite);
        } else {
            addFavorite = menu.findItem(R.id.action_add_favorite);
            removeFavorite = menu.findItem(R.id.action_remove_favorite);
            MenuItem editFavorite = menu.findItem(R.id.action_edit_favorite);
            editFavorite.setVisible(false);
        }

        checkFavorite();
    }

    private void initializeMenu(Menu menu) {
        initializeMenu(menu, false);
    }

    private void hideItem(MenuItem item) {
        if (item != null) item.setVisible(false);
    }

    private void showItem(MenuItem item) {
        if (item != null) item.setVisible(true);
    }

    private void itemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite)
            addFavoriteDialog();
        else if (id == R.id.action_remove_favorite)
            removeFavoriteDialog();
        else if (id == R.id.action_edit_favorite)
            editFavoriteDialog();
        else if (id == R.id.action_refresh)
            refreshRadar();
    }

    private AlertDialog favoriteDialog(String title, String button) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        EditText input = new EditText(this);
        input.setId(R.id.dialog_input);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (radarName != null) input.setText(radarName);
        builder.setView(input);

        builder.setPositiveButton(button, (dialog, which) -> {
            // Do nothing as we will override below
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        return dialog;
    }

    private void addFavoriteDialog() {
        final AlertDialog dialog = favoriteDialog(getString(R.string.action_add_favorite),
                getString(R.string.button_add));
        final EditText input = dialog.findViewById(R.id.dialog_input);

        radarWebsiteFragment.getCurrentLocationNameAsync().thenAccept(name -> {
            assert input != null;
            input.setText(name);
        });

        String settings = radarWebsiteFragment.getCurrentSettings();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String name = input.getText().toString();

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(getApplication());
                boolean exists = database.favoriteDao().findByName(name) != null;

                if (name.equals("")) {
                    runOnUiThread(() -> input.setError(getString(R.string.empty_name_error)));
                } else if (exists) {
                    runOnUiThread(() -> input.setError(getString(R.string.already_exists_error)));
                } else {
                    Favorite favorite = new Favorite();
                    favorite.setSource(Source.RADAR.getInt());
                    favorite.setName(name);
                    favorite.setLocation(settings);
                    database.favoriteDao().insertAll(favorite);

                    runOnUiThread(() -> {
                        hideItem(addFavorite);
                        hideItem(contextAddFavorite);
                        showItem(removeFavorite);
                        showItem(contextRemoveFavorite);
                        showItem(contextEditFavorite);

                        radarName = name;
                        setTitle(radarName);
                        dialog.dismiss();
                    });
                }
            });
        });
    }

    private void editFavoriteDialog() {
        final AlertDialog dialog = favoriteDialog(getString(R.string.action_edit_favorite),
                getString(R.string.button_edit));
        final EditText input = dialog.findViewById(R.id.dialog_input);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String name = input.getText().toString();

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(getApplication());
                boolean exists = database.favoriteDao().findByName(name) != null;

                if (name.equals("")) {
                    runOnUiThread(() -> input.setError(getString(R.string.empty_name_error)));
                } else if (exists) {
                    runOnUiThread(() -> input.setError(getString(R.string.already_exists_error)));
                } else {
                    Favorite favorite = database.favoriteDao().findByName(radarName);

                    if (favorite != null) {
                        favorite.setName(name);
                        database.favoriteDao().updateFavorites(favorite);

                        runOnUiThread(() -> {
                            radarName = name;
                            setTitle(radarName);
                        });
                    }

                    runOnUiThread(dialog::dismiss);
                }
            });
        });
    }

    private void removeFavoriteDialog() {
        DialogInterface.OnClickListener dialogListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                ExecutorService service = Executors.newSingleThreadExecutor();
                service.submit(() -> {
                    AppDatabase database = AppDatabase.getAppDatabase(getApplication());
                    List<Favorite> favorites = database.favoriteDao().findByLocation(Source.RADAR.getInt(), location);

                    for (Favorite favorite : favorites) {
                        database.favoriteDao().delete(favorite);
                    }

                    runOnUiThread(() -> {
                        showItem(addFavorite);
                        showItem(contextAddFavorite);
                        hideItem(removeFavorite);
                        hideItem(contextRemoveFavorite);
                        hideItem(contextEditFavorite);

                        radarName = sourceName;
                        setTitle(radarName);
                    });
                });
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.confirm_favorite_removal)
                .setPositiveButton(getString(R.string.button_yes), dialogListener)
                .setNegativeButton(getString(R.string.button_no), dialogListener)
                .show();
    }

    private void checkFavorite() {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            AppDatabase database = AppDatabase.getAppDatabase(getApplication());
            final List<Favorite> favorites = database.favoriteDao().findByLocation(Source.RADAR.getInt(), location);

            runOnUiThread(() -> {
                if (!favorites.isEmpty()) {
                    if (contextMenu) {
                        hideItem(contextAddFavorite);
                        showItem(contextRemoveFavorite);
                        showItem(contextEditFavorite);
                    } else {
                        hideItem(addFavorite);
                        showItem(removeFavorite);
                    }

                    currentFavorite = favorites.get(0).getUid();
                } else if (contextMenu) {
                    hideItem(contextRemoveFavorite);
                    hideItem(contextEditFavorite);
                    showItem(contextAddFavorite);
                } else {
                    hideItem(removeFavorite);
                    showItem(addFavorite);
                }
            });
        });
    }

    private void refreshRadar() {
        if (radarWebsiteFragment != null) radarWebsiteFragment.refreshRadarWebsite("");
    }
}
