package com.danhasting.radar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.WindowManager;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout mDrawerLayout;
    protected SharedPreferences settings;
    protected AppDatabase settingsDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settingsDB = AppDatabase.getAppDatabase(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        populateFavorites(navigationView.getMenu());

        if (this.getClass().getSimpleName().equals("MainActivity")) {
            startDefaultView();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        mDrawerLayout.closeDrawers();

        int id = menuItem.getItemId();

        if (id == R.id.nav_select) {
            Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
            MainActivity.this.startActivity(selectIntent);
        } else if (id == R.id.nav_mosaic) {
            Intent mosaicIntent = new Intent(MainActivity.this, SelectMosaicActivity.class);
            MainActivity.this.startActivity(mosaicIntent);
        } else if (id == R.id.nav_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.nav_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
        } else {
            Favorite favorite = settingsDB.favoriteDao().loadById(id);
            if (favorite != null) {
                startFavoriteView(favorite.getName(), favorite.getLocation(), favorite.getType(),
                        favorite.getLoop(), favorite.getEnhanced(), favorite.getMosaic());
            }
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationView navigationView = findViewById(R.id.nav_view);
        for (int i = 0; i < navigationView.getMenu().size(); i++) {
            navigationView.getMenu().getItem(i).setChecked(false);
        }
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

    protected void populateFavorites(Menu menu) {
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
            if (favorite != null) {
                Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
                radarIntent.putExtra("location", favorite.getLocation());
                radarIntent.putExtra("type", favorite.getType());
                radarIntent.putExtra("loop", favorite.getLoop());
                radarIntent.putExtra("enhanced", favorite.getEnhanced());
                radarIntent.putExtra("mosaic", favorite.getMosaic());
                MainActivity.this.startActivity(radarIntent);
            } else {
                Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
                MainActivity.this.startActivity(selectIntent);
            }
        } else {
            Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
            MainActivity.this.startActivity(selectIntent);
        }
    }

    private void startFavoriteView(String name, String location, String type, Boolean loop, Boolean enhanced, Boolean mosaic) {
        Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("type", type);
        radarIntent.putExtra("loop", loop);
        radarIntent.putExtra("enhanced", enhanced);
        radarIntent.putExtra("mosaic", mosaic);
        radarIntent.putExtra("favorite", true);
        radarIntent.putExtra("name", name);
        MainActivity.this.startActivity(radarIntent);
    }
}
