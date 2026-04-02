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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.danhasting.radar.R;
import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SettingsActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_settings, drawerLayout, false);
            drawerLayout.addView(contentView, 0);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
            setTitle(R.string.nav_settings);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // load preferences from XML
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = super.onCreateView(inflater, container, savedInstanceState);

            final ListPreference selectedFavorite = findPreference("default_favorite");
            final ListPreference defaultView = findPreference("default_view");

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> {
                AppDatabase database = AppDatabase.getAppDatabase(getActivity());
                List<Favorite> favorites = database.favoriteDao().getList();

                if (favorites.isEmpty()) {
                    assert selectedFavorite != null;
                    selectedFavorite.setEnabled(false);
                }

                ArrayList<String> names = new ArrayList<>();
                ArrayList<String> values = new ArrayList<>();

                for (Favorite favorite : favorites) {
                    names.add(favorite.getName());
                    values.add(String.valueOf(favorite.getUid()));
                }

                CharSequence[] n = names.toArray(new CharSequence[0]);
                CharSequence[] v = values.toArray(new CharSequence[0]);

                assert selectedFavorite != null;
                selectedFavorite.setEntries(n);
                selectedFavorite.setEntryValues(v);
            });

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            String defaultViewValue = settings.getString("default_view", getString(R.string.default_view_default));
            assert selectedFavorite != null;
            selectedFavorite.setEnabled(defaultViewValue.equals("favorite"));

            assert defaultView != null;
            defaultView.setOnPreferenceChangeListener((preference, o) -> {
                service.submit(() -> {
                    AppDatabase database = AppDatabase.getAppDatabase(getActivity());
                    List<Favorite> favorites = database.favoriteDao().getList();

                    if (!favorites.isEmpty())
                        selectedFavorite.setEnabled(o.toString().equals("favorite"));
                    else
                        selectedFavorite.setEnabled(false);
                });
                return true;
            });


            // Tell previous activity if we came from settings
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent();
                        intent.putExtra("from_settings", true);
                        requireActivity().setResult(Activity.RESULT_OK, intent);
                        requireActivity().finish();
                    }
                });

            return root;
        }
    }
}
