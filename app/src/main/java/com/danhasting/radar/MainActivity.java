package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

        settings = this.getSharedPreferences(getString(R.string.app_full_name), Context.MODE_PRIVATE);
        settingsDB = AppDatabase.getAppDatabase(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        toggleDefaultMenuItem(navigationView.getMenu());
        populateFavorites(navigationView.getMenu());


        if (this.getClass().getSimpleName().equals("MainActivity")) {
            Boolean defaultRadar = settings.getBoolean("default", false);
            if (defaultRadar) {
                startDefaultView();
            } else {
                Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
                MainActivity.this.startActivity(selectIntent);
            }
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
        } else if (id == R.id.nav_default) {
            startDefaultView();
        } else {
            Favorite favorite = settingsDB.favoriteDao().loadById(id);
            if (favorite != null) {
                startFavoriteView(favorite.getLocation(), favorite.getType(), favorite.getLoop());
            }
        }

        return true;
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

    protected void toggleDefaultMenuItem(Menu menu) {
        Boolean defaultRadar = settings.getBoolean("default", false);
        if (!defaultRadar) {
            menu.findItem(R.id.nav_default).setVisible(false);
        }
    }

    private void startDefaultView() {
        String location = settings.getString("default_location","BMX");
        String type = settings.getString("default_type","N0R");
        Boolean loop = settings.getBoolean("default_loop",false);

        Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("type", type);
        radarIntent.putExtra("loop", loop);
        MainActivity.this.startActivity(radarIntent);
    }

    private void startFavoriteView(String location, String type, Boolean loop) {
        Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("type", type);
        radarIntent.putExtra("loop", loop);
        MainActivity.this.startActivity(radarIntent);
    }
}
