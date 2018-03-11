package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout mDrawerLayout;
    protected SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = this.getSharedPreferences(getString(R.string.app_full_name), Context.MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (this.getClass().getSimpleName().equals("MainActivity")) {
            Boolean defaultRadar = settings.getBoolean("default", false);
            if (defaultRadar) {
                String location = settings.getString("default_location","BMX");
                String type = settings.getString("default_type","N0R");
                Boolean loop = settings.getBoolean("default_loop",false);

                Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);
                radarIntent.putExtra("location", location);
                radarIntent.putExtra("type", type);
                radarIntent.putExtra("loop", loop);
                MainActivity.this.startActivity(radarIntent);
            } else {
                Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
                MainActivity.this.startActivity(selectIntent);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        // set item as selected to persist highlight
        menuItem.setChecked(true);
        // close drawer when item is tapped
        mDrawerLayout.closeDrawers();

        // Add code here to update the UI based on the item selected
        // For example, swap UI fragments here
        int id = menuItem.getItemId();

        if (id == R.id.nav_select) {
            Intent selectIntent = new Intent(MainActivity.this, SelectActivity.class);
            MainActivity.this.startActivity(selectIntent);
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
}
