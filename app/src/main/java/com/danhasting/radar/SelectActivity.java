package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

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

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this, R.array.location_names, android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);
    }

    public void viewRadar(View v) {
        Intent radarIntent = new Intent(SelectActivity.this, RadarActivity.class);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        radarIntent.putExtra("type",
                getResources().getStringArray(R.array.type_values)[typeSpinner.getSelectedItemPosition()]);

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        radarIntent.putExtra("location",
                getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()]);

        Switch loopSwitch = findViewById(R.id.loopSwitch);
        radarIntent.putExtra("loop", loopSwitch.isChecked());

        SelectActivity.this.startActivity(radarIntent);
    }
}
