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

import android.app.ActivityManager.TaskDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;

import com.danhasting.radar.R;
import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set color of the top bar on the recents screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap app_icon = BitmapFactory.decodeResource(getResources(), R.mipmap.app_icon);
            TaskDescription taskDesc = new TaskDescription(getString(R.string.app_name), app_icon,
                    ContextCompat.getColor(getApplicationContext(), R.color.recentsTopBar));
            setTaskDescription(taskDesc);
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        setTitle(R.string.nav_settings);
        if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("from_settings", true);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            final ListPreference selectedFavorite = (ListPreference) findPreference("default_favorite");
            final ListPreference showFavorite = (ListPreference) findPreference("show_favorite");

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(getActivity());
                List<Favorite> favorites = database.favoriteDao().getList();

                if (favorites.size() == 0) {
                    selectedFavorite.setEnabled(false);
                    showFavorite.setEnabled(false);
                    showFavorite.setEnabled(false);
                }

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> values = new ArrayList<>();

                for (Favorite favorite : favorites) {
                    names.add(favorite.getName());
                    values.add(String.valueOf(favorite.getUid()));
                }

                CharSequence[] n = names.toArray(new CharSequence[0]);
                CharSequence[] v = values.toArray(new CharSequence[0]);

                selectedFavorite.setEntries(n);
                selectedFavorite.setEntryValues(v);
            });

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String showDefault = getString(R.string.wifi_toggle_default);
            String showFav = settings.getString("show_favorite", showDefault);
            selectedFavorite.setEnabled(!showFav.equals(showDefault));

            showFavorite.setOnPreferenceChangeListener((preference, o) -> {
                selectedFavorite.setEnabled(!o.toString().equals(showDefault));
                return true;
            });
        }
    }
}
