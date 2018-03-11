package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.Arrays;

public class SelectActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_select, null, false);
        mDrawerLayout.addView(contentView, 0);

        setTitle(R.string.select_radar_image);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.type_names, android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        String type = settings.getString("last_type","");
        int index = Arrays.asList(getResources().getStringArray(R.array.type_values)).indexOf(type);
        typeSpinner.setSelection(index);

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this, R.array.location_names, android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        String location = settings.getString("last_location","");
        index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
        locationSpinner.setSelection(index);

        Switch loopSwitch = findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_loop",false));
    }

    public void viewRadar(View v) {
        Intent radarIntent = new Intent(SelectActivity.this, RadarActivity.class);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        Switch loopSwitch = findViewById(R.id.loopSwitch);

        String location = getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()];
        String type = getResources().getStringArray(R.array.type_values)[typeSpinner.getSelectedItemPosition()];
        Boolean loop = loopSwitch.isChecked();

        radarIntent.putExtra("type", type);
        radarIntent.putExtra("location", location);
        radarIntent.putExtra("loop", loop);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_location", location);
        editor.putString("last_type", type);
        editor.putBoolean("last_loop", loop);
        editor.apply();

        SelectActivity.this.startActivity(radarIntent);
    }
}
