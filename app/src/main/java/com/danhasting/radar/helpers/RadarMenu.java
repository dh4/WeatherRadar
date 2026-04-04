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
package com.danhasting.radar.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.danhasting.radar.R;
import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;
import com.danhasting.radar.database.Source;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadarMenu {
    public interface Ui {
        Context getContext();
        AppCompatActivity getActivity();
        void setTitle(CharSequence title);
        void refreshRadar();
        void setCurrentFavorite(Integer favorite);

        int getSourceInt();
        String getLocation();
        String getType();
        Boolean getLoop();
        Boolean getEnhanced();
        int getDistance();

        default CompletableFuture<String> getCurrentLocationNameAsync() { return null; }
    }

    private final Ui ui;
    private MenuItem addFavorite, removeFavorite;
    private MenuItem contextAddFavorite, contextRemoveFavorite, contextEditFavorite;
    private boolean contextMenu = false;

    private String favoriteName;

    public RadarMenu(Ui ui) { this.ui = ui; }

    public void setFavoriteName(String name) {
        favoriteName = name;
    }

    public void initializeMenu(Menu menu) {
        initializeMenu(menu, false);
    }

    public void initializeMenu(Menu menu, boolean context) {
        ui.getActivity().getMenuInflater().inflate(R.menu.radar_actions, menu);
        contextMenu = context;

        if (contextMenu) {
            contextAddFavorite = menu.findItem(R.id.action_add_favorite);
            contextRemoveFavorite = menu.findItem(R.id.action_remove_favorite);
            contextEditFavorite = menu.findItem(R.id.action_edit_favorite);
        } else {
            addFavorite = menu.findItem(R.id.action_add_favorite);
            removeFavorite = menu.findItem(R.id.action_remove_favorite);
            MenuItem editFavorite = menu.findItem(R.id.action_edit_favorite);
            if (editFavorite != null) editFavorite.setVisible(false);
        }

        checkFavorite();
    }

    private void hideItem(MenuItem item) {
        if (item != null) item.setVisible(false);
    }
    private void showItem(MenuItem item) {
        if (item != null) item.setVisible(true);
    }

    public void itemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite)
            addFavoriteDialog();
        else if (id == R.id.action_remove_favorite)
            removeFavoriteDialog();
        else if (id == R.id.action_edit_favorite)
            editFavoriteDialog();
        else if (id == R.id.action_refresh && ui != null)
            ui.refreshRadar();
    }

    private AlertDialog favoriteDialog(String title, String button) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ui.getContext());
        builder.setTitle(title);

        EditText input = new EditText(ui.getContext());
        input.setId(R.id.dialog_input);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (favoriteName != null) input.setText(favoriteName);
        builder.setView(input);

        builder.setPositiveButton(button, (dialog, which) -> {
            // Do nothing as we will override below
        });
        builder.setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        return dialog;
    }

    public void addFavoriteDialog() {
        final AlertDialog dialog = favoriteDialog(ui.getContext().getString(R.string.action_add_favorite),
                ui.getContext().getString(R.string.button_add));
        final EditText input = dialog.findViewById(R.id.dialog_input);

        CompletableFuture<String> asyncName = ui.getCurrentLocationNameAsync();
        if (asyncName != null) {
            asyncName.thenAccept(name -> ui.getActivity().runOnUiThread(() -> {
                if (input != null) input.setText(name);
            }));
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            assert input != null;
            final String name = input.getText().toString();
            String location = ui.getLocation(); // Call this on our current thread since it accesses the webview

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(ui.getContext().getApplicationContext());
                boolean exists = database.favoriteDao().findByName(name) != null;

                if (name.isEmpty())
                    ui.getActivity().runOnUiThread(() -> input.setError(ui.getContext().getString(R.string.empty_name_error)));
                else if (exists)
                    ui.getActivity().runOnUiThread(() -> input.setError(ui.getContext().getString(R.string.already_exists_error)));
                else if (location != null) {
                    Favorite favorite = new Favorite();
                    favorite.setSource(ui.getSourceInt());
                    favorite.setName(name);
                    favorite.setLocation(location);

                    String type = ui.getType();
                    if (type != null) favorite.setType(type);

                    Boolean loop = ui.getLoop();
                    if (loop != null) favorite.setLoop(loop);

                    Boolean enhanced = ui.getEnhanced();
                    if (enhanced != null) favorite.setEnhanced(enhanced);

                    database.favoriteDao().insertAll(favorite);

                    ui.getActivity().runOnUiThread(() -> {
                        hideItem(addFavorite);
                        hideItem(contextAddFavorite);
                        showItem(removeFavorite);
                        showItem(contextRemoveFavorite);
                        showItem(contextEditFavorite);

                        favoriteName = name;
                        ui.setTitle(favoriteName);

                        dialog.dismiss();
                    });
                } else {
                    dialog.dismiss();
                }
            });
        });
    }

    public void editFavoriteDialog() {
        final AlertDialog dialog = favoriteDialog(ui.getContext().getString(R.string.action_edit_favorite),
                ui.getContext().getString(R.string.button_edit));
        final EditText input = dialog.findViewById(R.id.dialog_input);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            assert input != null;
            final String name = input.getText().toString();

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(ui.getContext().getApplicationContext());
                boolean exists = database.favoriteDao().findByName(name) != null;

                if (name.isEmpty()) {
                    ui.getActivity().runOnUiThread(() -> input.setError(ui.getContext().getString(R.string.empty_name_error)));
                } else if (exists) {
                    ui.getActivity().runOnUiThread(() -> input.setError(ui.getContext().getString(R.string.already_exists_error)));
                } else {
                    Favorite favorite = database.favoriteDao().findByName(favoriteName);

                    if (favorite != null) {
                        favorite.setName(name);
                        database.favoriteDao().updateFavorites(favorite);

                        ui.getActivity().runOnUiThread(() -> {
                            favoriteName = name;
                            ui.setTitle(favoriteName);
                        });
                    }

                    ui.getActivity().runOnUiThread(dialog::dismiss);
                }
            });
        });
    }

    public void removeFavoriteDialog() {
        DialogInterface.OnClickListener dialogListener = (dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                // Call this on our current thread since it accesses the webview
                String location = ui.getLocation();

                ExecutorService service = Executors.newSingleThreadExecutor();
                service.submit(() -> {
                    AppDatabase database = AppDatabase.getAppDatabase(ui.getContext().getApplicationContext());
                    List<Favorite> favorites;

                    if (ui.getSourceInt() == Source.RADAR.getInt() || ui.getSourceInt() == Source.AIR.getInt())
                        favorites = database.favoriteDao().findByLocation(ui.getSourceInt(), location);
                    else
                        favorites = database.favoriteDao().findByData(ui.getSourceInt(), location, ui.getType(),
                                ui.getLoop(), ui.getEnhanced(), ui.getDistance());

                    for (Favorite favorite : favorites)
                        database.favoriteDao().delete(favorite);

                    ui.getActivity().runOnUiThread(() -> {
                        showItem(addFavorite);
                        showItem(contextAddFavorite);
                        hideItem(removeFavorite);
                        hideItem(contextRemoveFavorite);
                        hideItem(contextEditFavorite);

                        favoriteName = "";
                        ui.setTitle(null);
                    });
                });
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ui.getContext());
        builder.setMessage(R.string.confirm_favorite_removal)
                .setPositiveButton(ui.getContext().getString(R.string.button_yes), dialogListener)
                .setNegativeButton(ui.getContext().getString(R.string.button_no), dialogListener)
                .show();
    }

    public void checkFavorite() {
        // Call this on our current thread since it accesses the webview
        String location = ui.getLocation();

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            AppDatabase database = AppDatabase.getAppDatabase(ui.getContext().getApplicationContext());
            List<Favorite> favorites;

            if (ui.getSourceInt() == Source.RADAR.getInt() || ui.getSourceInt() == Source.AIR.getInt())
                favorites = database.favoriteDao().findByLocation(ui.getSourceInt(), location);
            else
                favorites = database.favoriteDao().findByData(ui.getSourceInt(), location, ui.getType(),
                        ui.getLoop(), ui.getEnhanced(), ui.getDistance());

            List<Favorite> finalFavorites = favorites;
            ui.getActivity().runOnUiThread(() -> {
                if (!finalFavorites.isEmpty()) {
                    favoriteName = favorites.get(0).getName();
                    ui.setTitle(favoriteName);

                    if (contextMenu) {
                        hideItem(contextAddFavorite);
                        showItem(contextRemoveFavorite);
                        showItem(contextEditFavorite);
                    } else {
                        hideItem(addFavorite);
                        showItem(removeFavorite);
                    }

                    ui.setCurrentFavorite(favorites.get(0).getUid());
                } else if (contextMenu) {
                    hideItem(contextRemoveFavorite);
                    hideItem(contextEditFavorite);
                    showItem(contextAddFavorite);
                    ui.setCurrentFavorite(-1);
                } else {
                    hideItem(removeFavorite);
                    showItem(addFavorite);
                    ui.setCurrentFavorite(-1);
                }
            });
        });
    }

    public void setFullscreen(boolean fullscreen) {
        Window window = ui.getActivity().getWindow();

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

        ActionBar actionBar = ui.getActivity().getSupportActionBar();
        if (fullscreen && actionBar != null) {
            ui.getActivity().getSupportActionBar().hide();
            ui.getActivity().findViewById(R.id.radarWebsiteLayout).setPadding(0, 0, 0, 0);
        } else if (!fullscreen && actionBar != null) {
            actionBar.show();

            TypedValue tv = new TypedValue();
            int actionBarHeight = 0;
            if (ui.getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, ui.getActivity().getResources().getDisplayMetrics());

            ui.getActivity().findViewById(R.id.radarWebsiteLayout).setPadding(0, actionBarHeight, 0, 0);
        }
    }
}
