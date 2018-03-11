package com.danhasting.radar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        setTitle(R.string.nav_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            AppDatabase settingsDB = AppDatabase.getAppDatabase(getActivity());
            List<Favorite> favorites = settingsDB.favoriteDao().getAll();
            final ListPreference selectedFavorite = (ListPreference) findPreference("default_favorite");

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

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            selectedFavorite.setEnabled(settings.getBoolean("show_favorite", false));

            CheckBoxPreference showFavorite = (CheckBoxPreference) findPreference("show_favorite");
            showFavorite.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    selectedFavorite.setEnabled(!settings.getBoolean("show_favorite", false));
                    return true;
                }
            });
        }
    }

}
