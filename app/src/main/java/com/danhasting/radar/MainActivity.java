package com.danhasting.radar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        Intent radarIntent = new Intent(MainActivity.this, RadarActivity.class);

        Spinner typeSpinner = findViewById(R.id.typeSpinner);
        radarIntent.putExtra("type",
                getResources().getStringArray(R.array.type_values)[typeSpinner.getSelectedItemPosition()]);

        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        radarIntent.putExtra("location",
                getResources().getStringArray(R.array.location_values)[locationSpinner.getSelectedItemPosition()]);

        Switch loopSwitch = findViewById(R.id.loopSwitch);
        radarIntent.putExtra("loop", loopSwitch.isChecked());

        MainActivity.this.startActivity(radarIntent);
    }
}
