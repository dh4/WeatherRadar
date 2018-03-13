package com.danhasting.radar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            final CheckBoxPreference showFavorite = (CheckBoxPreference) findPreference("show_favorite");

            AppDatabase settingsDB = AppDatabase.getAppDatabase(getActivity());
            List<Favorite> favorites = settingsDB.favoriteDao().getAll();

            if (favorites.size() == 0) {
                selectedFavorite.setEnabled(false);
                showFavorite.setEnabled(false);
                showFavorite.setChecked(false);
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

            final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            selectedFavorite.setEnabled(settings.getBoolean("show_favorite", false));

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
