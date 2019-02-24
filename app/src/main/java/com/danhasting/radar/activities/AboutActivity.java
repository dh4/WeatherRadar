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
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.danhasting.radar.BuildConfig;
import com.danhasting.radar.R;

public class AboutActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_about, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        setTitle(R.string.about);

        TextView version = findViewById(R.id.version);
        String versionName = String.format("%s %s", getText(R.string.version),
                BuildConfig.VERSION_NAME);
        version.setText(versionName);

        Button donateButton = findViewById(R.id.donate_button);
        donateButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://dh4.github.io/donate/"));
            startActivity(intent);
        });

        Button githubButton = findViewById(R.id.github_button);
        githubButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://github.com/dh4/WeatherRadar"));
            startActivity(intent);
        });

        Button issueButton = findViewById(R.id.issue_button);
        issueButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://github.com/dh4/WeatherRadar/issues"));
            startActivity(intent);
        });

        Button contactButton = findViewById(R.id.contact_button);
        contactButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://dh4.github.io/contact/"));
            startActivity(intent);
        });

        ImageView wundergroundImage = findViewById(R.id.wunderground_image);
        wundergroundImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://www.wunderground.com/"));
            startActivity(intent);
        });
    }
}
