package com.danhasting.radar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.Arrays;

public class SelectMosaicActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_select_mosaic, null, false);
        mDrawerLayout.addView(contentView, 0);

        setTitle(R.string.select_mosaic_image);

        Spinner mosaicSpinner = findViewById(R.id.mosaicSpinner);
        ArrayAdapter<CharSequence> mosaicAdapter = ArrayAdapter.createFromResource(
                this, R.array.mosaic_names, android.R.layout.simple_spinner_dropdown_item);
        mosaicSpinner.setAdapter(mosaicAdapter);

        String mosaic = settings.getString("last_mosaic","");
        int index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values)).indexOf(mosaic);
        mosaicSpinner.setSelection(index);

        final Switch loopSwitch = findViewById(R.id.loopSwitch);
        loopSwitch.setChecked(settings.getBoolean("last_mosaic_loop",false));
    }

    public void viewMosaic(View v) {
        Intent radarIntent = new Intent(SelectMosaicActivity.this, RadarActivity.class);

        Spinner mosaicSpinner = findViewById(R.id.mosaicSpinner);
        Switch loopSwitch = findViewById(R.id.loopSwitch);

        String mosaic = getResources().getStringArray(R.array.mosaic_values)[mosaicSpinner.getSelectedItemPosition()];
        Boolean loop = loopSwitch.isChecked();

        radarIntent.putExtra("location", mosaic);
        radarIntent.putExtra("loop", loop);
        radarIntent.putExtra("mosaic", true);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_mosaic", mosaic);
        editor.putBoolean("last_mosaic_loop", loop);
        editor.apply();

        SelectMosaicActivity.this.startActivity(radarIntent);
    }
}

