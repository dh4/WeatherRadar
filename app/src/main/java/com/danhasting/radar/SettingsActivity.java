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
package com.danhasting.radar;

import android.app.ActivityManager.TaskDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.Toast;

import com.danhasting.radar.database.AppDatabase;
import com.danhasting.radar.database.Favorite;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

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
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            final ListPreference selectedFavorite = (ListPreference) findPreference("default_favorite");
            final ListPreference showFavorite = (ListPreference) findPreference("show_favorite");

            ExecutorService service =  Executors.newSingleThreadExecutor();
            service.submit(new Runnable() {
                @Override
                public void run() {
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

                    CharSequence[] n = names.toArray(new CharSequence[names.size()]);
                    CharSequence[] v = values.toArray(new CharSequence[values.size()]);

                    selectedFavorite.setEntries(n);
                    selectedFavorite.setEntryValues(v);
                }
            });

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String showDefault = getString(R.string.wifi_toggle_default);
            selectedFavorite.setEnabled(!settings.getString("show_favorite", showDefault)
                    .equals(showDefault));

            showFavorite.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    selectedFavorite.setEnabled(!o.toString().equals(showDefault));
                    return true;
                }
            });

            checkApiKeyStatus(settings, false, false);

            final EditTextPreference apiKeyEditText = (EditTextPreference) findPreference("api_key");
            final Context context = getActivity().getApplicationContext();

            apiKeyEditText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    final String apiKey = o.toString();

                    if (apiKey.equals("")) {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("api_key_activated", false);
                        editor.apply();

                        checkApiKeyStatus(settings, false, false);
                    } else {
                        AsyncHttpClient client = new AsyncHttpClient();
                        String testURL = String.format("https://api.wunderground.com/api/%s/" +
                                "conditions/q/CA/San_Francisco.json", apiKey);

                        client.get(testURL, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int status, cz.msebera.android.httpclient.Header[] headers,
                                                  JSONObject json) {
                                try {
                                    String responseString = json.getString("response");
                                    JSONObject response = new JSONObject(responseString);
                                    String featuresString = response.getString("features");
                                    JSONObject features = new JSONObject(featuresString);
                                    String success = features.getString("conditions");
                                    if (success != null) {
                                        SharedPreferences.Editor editor = settings.edit();
                                        editor.putBoolean("api_key_activated", true);
                                        editor.apply();

                                        Toast.makeText(context, R.string.api_key_activated,
                                                Toast.LENGTH_LONG).show();

                                        checkApiKeyStatus(settings, false, true);
                                    }
                                } catch (JSONException e) {
                                    Toast.makeText(context, R.string.api_key_failed,
                                            Toast.LENGTH_LONG).show();

                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putBoolean("api_key_activated", false);
                                    editor.apply();

                                    checkApiKeyStatus(settings, true, true);
                                }
                            }

                            @Override
                            public void onFailure(int status, cz.msebera.android.httpclient.Header[] h,
                                                  Throwable t, JSONObject e) {
                                Toast.makeText(context, R.string.connection_error,
                                        Toast.LENGTH_LONG).show();
                            }

                        });
                    }

                    return true;
                }
            });

            final ListPreference resEdit = (ListPreference) findPreference("image_resolution");
            final EditTextPreference custom = (EditTextPreference)findPreference("custom_resolution");
            checkResolution(settings.getString("image_resolution",
                    getString(R.string.image_resolution_default)), custom);

            resEdit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    checkResolution(o.toString(), custom);
                    return true;
                }
            });

            custom.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String newValue = o.toString();

                if (newValue.matches("\\d+")) {
                    int value = Integer.parseInt(newValue);

                    if (value >= 100 && value <= 4096)
                        return true;
                }

                Toast.makeText(context, getString(R.string.custom_resolution_error),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        });
        }

        private void checkResolution(String resolution, EditTextPreference custom) {
            if (resolution.equals("custom"))
                custom.setEnabled(true);
            else
                custom.setEnabled(false);
        }

        private void checkApiKeyStatus(SharedPreferences settings, Boolean failed, Boolean async) {
            // Check to make sure user is still in settings
            if (async && !isAdded()) return;

            EditTextPreference apiKey = (EditTextPreference)findPreference("api_key");
            CheckBoxPreference timeLabel = (CheckBoxPreference)findPreference("show_time_label");
            CheckBoxPreference snow = (CheckBoxPreference)findPreference("show_snow_mix");
            CheckBoxPreference smoothing = (CheckBoxPreference)findPreference("smoothing");
            CheckBoxPreference noclutter = (CheckBoxPreference)findPreference("noclutter");
            ListPreference resolution = (ListPreference)findPreference("image_resolution");
            ListPreference speed = (ListPreference)findPreference("animation_speed");
            ListPreference frames = (ListPreference)findPreference("animation_frames");
            ListPreference units = (ListPreference)findPreference("distance_units");
            EditTextPreference custom = (EditTextPreference)findPreference("custom_resolution");
            CheckBoxPreference lower = (CheckBoxPreference)findPreference("lower_resolution");

            String resCurrent = settings.getString("image_resolution",
                    getString(R.string.image_resolution_default));

            String currentKey = apiKey.getText();
            if (currentKey == null) currentKey = "";

            if (settings.getBoolean("api_key_activated", false)) {
                apiKey.setSummary(R.string.api_key_activated);

                timeLabel.setEnabled(true);
                snow.setEnabled(true);
                smoothing.setEnabled(true);
                noclutter.setEnabled(true);
                resolution.setEnabled(true);
                speed.setEnabled(true);
                frames.setEnabled(true);
                units.setEnabled(true);
                lower.setEnabled(true);

                if (resCurrent.equals("custom"))
                    custom.setEnabled(true);
            } else {
                if (failed || !currentKey.equals(""))
                    apiKey.setSummary(R.string.api_key_error);
                else
                    apiKey.setSummary(R.string.api_key_summary);

                timeLabel.setEnabled(false);
                snow.setEnabled(false);
                smoothing.setEnabled(false);
                noclutter.setEnabled(false);
                resolution.setEnabled(false);
                speed.setEnabled(false);
                frames.setEnabled(false);
                units.setEnabled(false);
                custom.setEnabled(false);
                lower.setEnabled(false);
            }
        }
    }
}
