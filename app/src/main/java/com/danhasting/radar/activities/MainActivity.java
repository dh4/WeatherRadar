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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.danhasting.radar.R;
import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;
import com.danhasting.radar.database.FavoriteViewModel;
import com.danhasting.radar.database.Source;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    SharedPreferences settings;

    Integer currentFavorite = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                NavigationView navigationView = findViewById(R.id.nav_view);
                for (int i = 0; i < navigationView.getMenu().size(); i++)
                    navigationView.getMenu().getItem(i).setChecked(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FavoriteViewModel viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        viewModel.getFavorites().observe(this, favorites -> {
            if (favorites != null)
                populateFavorites(navigationView.getMenu(), favorites);
        });

        ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {}
        );

        Button settingsButton = navigationView.getHeaderView(0).findViewById(R.id.nav_settings);
        settingsButton.setOnClickListener(view -> {
            drawerLayout.closeDrawers();
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(settingsIntent);
        });

        Button aboutButton = navigationView.getHeaderView(0).findViewById(R.id.nav_about);
        aboutButton.setOnClickListener(view -> {
            drawerLayout.closeDrawers();
            if (!classNameEquals("AboutActivity")) {
                Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                aboutIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(aboutIntent);
            }
        });

        if (classNameEquals("MainActivity"))
            startDefaultView();
        else if (settings.getBoolean("first_run", true)) {
            firstRunWelcome();

            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("first_run", false);
            editor.apply();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        drawerLayout.closeDrawers();

        final int id = menuItem.getItemId();

        if (!classNameEquals("SelectActivity")) {
            Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
            selectIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            if (id == R.id.nav_radar)
                selectIntent.putExtra("selection", Source.RADAR);
            else if (id == R.id.nav_nws)
                selectIntent.putExtra("selection", Source.NWS);
            else if (id == R.id.nav_mosaic)
                selectIntent.putExtra("selection", Source.MOSAIC);

            if (selectIntent.hasExtra("selection")) {
                MainActivity.this.startActivity(selectIntent);
                return true;
            }
        }


        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            AppDatabase database = AppDatabase.getAppDatabase(getApplication());
            Favorite favorite = database.favoriteDao().loadById(id);
            if (favorite != null && (id != currentFavorite || favorite.getSource().equals(Source.RADAR.getInt())))
                startFavoriteView(favorite);
        });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationView navigationView = findViewById(R.id.nav_view);
        for (int i = 0; i < navigationView.getMenu().size(); i++)
            navigationView.getMenu().getItem(i).setChecked(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean classNameEquals(String name) {
        return this.getClass().getSimpleName().equals(name);
    }

    private void firstRunWelcome() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.welcome_header));
        builder.setMessage(getString(R.string.welcome_text));

        builder.setPositiveButton(R.string.welcome_more, (dialog, which) -> {
            Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(aboutIntent);
        });
        builder.setNegativeButton(R.string.welcome_dismiss, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void populateFavorites(Menu menu, List<Favorite> favorites) {
        SubMenu favMenu = menu.findItem(R.id.nav_favorites).getSubMenu();

        assert favMenu != null;
        favMenu.clear();

        int i = 0;
        for (Favorite favorite : favorites) {
            favMenu.add(0, favorite.getUid(), i, favorite.getName());
            i++;
        }
    }

    private void startDefaultView() {
        String show = settings.getString("show_favorite", getString(R.string.wifi_toggle_default));

        if (show.equals("always") || show.equals("wifi") && onWifi()) {
            String favID = settings.getString("default_favorite", "0");
            final int favoriteID = Integer.parseInt(favID);

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(getApplication());
                final Favorite favorite = database.favoriteDao().loadById(favoriteID);

                runOnUiThread(() -> {
                    if (favorite != null)
                        startFavoriteView(favorite);
                    else
                        startRadarView();

                    if (classNameEquals("MainActivity"))
                        finish();
                });
            });
        } else {
            startRadarView();

            if (classNameEquals("MainActivity"))
                finish();
        }
    }

    private void startRadarView() {
        Intent radarWebsiteIntent = new Intent(MainActivity.this, RadarWebsiteActivity.class);
        radarWebsiteIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        MainActivity.this.startActivity(radarWebsiteIntent);
    }

    private void startFavoriteView(Favorite favorite) {
        if (favorite.getSource().equals(Source.RADAR.getInt())) {
            Intent radarWebsiteIntent = new Intent(MainActivity.this, RadarWebsiteActivity.class);
            radarWebsiteIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            radarWebsiteIntent.putExtra("location", favorite.getLocation());
            radarWebsiteIntent.putExtra("favorite", true);
            radarWebsiteIntent.putExtra("name", favorite.getName());
            radarWebsiteIntent.putExtra("favoriteID", favorite.getUid());
            MainActivity.this.startActivity(radarWebsiteIntent);
            overridePendingTransition(0,0);
        } else {
            Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);

            radarIntent.putExtra("source", Source.fromInt(favorite.getSource()));
            radarIntent.putExtra("location", favorite.getLocation());
            radarIntent.putExtra("type", favorite.getType());
            radarIntent.putExtra("loop", favorite.getLoop());
            radarIntent.putExtra("enhanced", favorite.getEnhanced());
            radarIntent.putExtra("distance", favorite.getDistance());
            radarIntent.putExtra("favorite", true);
            radarIntent.putExtra("name", favorite.getName());
            radarIntent.putExtra("favoriteID", favorite.getUid());
            MainActivity.this.startActivity(radarIntent);
        }
    }

    Boolean onWifi() {
        ConnectivityManager m = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (m != null) {
            Network netInfo = m.getActiveNetwork();
            if (netInfo != null) {
                NetworkCapabilities caps = m.getNetworkCapabilities(netInfo);
                return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else
                return false;
        }
        return false;
    }
}
