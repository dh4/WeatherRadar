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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout mDrawerLayout;
    SharedPreferences settings;
    AppDatabase settingsDB;

    Integer currentFavorite = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settingsDB = AppDatabase.getAppDatabase(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {}

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                NavigationView navigationView = findViewById(R.id.nav_view);
                for (int i = 0; i < navigationView.getMenu().size(); i++)
                    navigationView.getMenu().getItem(i).setChecked(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {}
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        populateFavorites(navigationView.getMenu());

        if (this.getClass().getSimpleName().equals("MainActivity"))
            startDefaultView();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        int id = menuItem.getItemId();

        if (id == R.id.nav_select && !this.getClass().getSimpleName().equals("SelectNWSActivity")) {
            Intent selectIntent = new Intent(MainActivity.this, SelectNWSActivity.class);
            MainActivity.this.startActivity(selectIntent);
        } else if (id == R.id.nav_mosaic && !this.getClass().getSimpleName().equals("SelectMosaicActivity")) {
            Intent mosaicIntent = new Intent(MainActivity.this, SelectMosaicActivity.class);
            MainActivity.this.startActivity(mosaicIntent);
        } else if (id == R.id.nav_wunderground && !this.getClass().getSimpleName().equals("SelectWundergroundActivity")) {
            Intent wIntent = new Intent(MainActivity.this, SelectWundergroundActivity.class);
            MainActivity.this.startActivity(wIntent);
        } else if (id == R.id.nav_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, 1);
        } else if (id == R.id.nav_about && !this.getClass().getSimpleName().equals("AboutActivity")) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        } else if (id != currentFavorite) {
            Favorite favorite = settingsDB.favoriteDao().loadById(id);
            if (favorite != null) startFavoriteView(favorite);
        }

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
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void populateFavorites(Menu menu) {
        SubMenu favMenu = menu.findItem(R.id.nav_favorites).getSubMenu();
        favMenu.clear();
        List<Favorite> favorites = settingsDB.favoriteDao().getAll();

        int i = 0;
        for (Favorite favorite : favorites) {
            favMenu.add(0, favorite.getUid(), i, favorite.getName());
            i++;
        }
    }

    private void startDefaultView() {
        if (settings.getBoolean("show_favorite", false)) {
            int favoriteID = Integer.parseInt(settings.getString("default_favorite","0"));
            Favorite favorite = settingsDB.favoriteDao().loadById(favoriteID);
            if (favorite != null)
                startFavoriteView(favorite);
            else
                startFormView();
        } else
            startFormView();

        if (this.getClass().getSimpleName().equals("MainActivity"))
            this.finish();
    }

    private void startFormView() {
        Intent selectIntent;

        if (settings.getBoolean("api_key_activated", false))
            selectIntent = new Intent(MainActivity.this, SelectWundergroundActivity.class);
        else
            selectIntent = new Intent(MainActivity.this, SelectNWSActivity.class);

        MainActivity.this.startActivity(selectIntent);
    }

    private void startFavoriteView(Favorite favorite) {
        Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
        radarIntent.putExtra("location", favorite.getLocation());
        radarIntent.putExtra("type", favorite.getType());
        radarIntent.putExtra("loop", favorite.getLoop());
        radarIntent.putExtra("enhanced", favorite.getEnhanced());
        radarIntent.putExtra("mosaic", favorite.getMosaic());
        radarIntent.putExtra("wunderground", favorite.getWunderground());
        radarIntent.putExtra("distance", favorite.getDistance());
        radarIntent.putExtra("favorite", true);
        radarIntent.putExtra("name", favorite.getName());
        radarIntent.putExtra("favoriteID", favorite.getUid());
        MainActivity.this.startActivity(radarIntent);
    }

    void inflateNeedKeyView() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.wunderground_key_missing, mDrawerLayout, false);
            mDrawerLayout.addView(contentView, 0);
        }

        Button needKey = findViewById(R.id.needKeyButton);
        needKey.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent browser= new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.wunderground.com/weather/api/"));
                startActivity(browser);
            }

        });
    }
}
