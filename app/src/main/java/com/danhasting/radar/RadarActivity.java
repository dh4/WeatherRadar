package com.danhasting.radar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;

import java.util.Arrays;
import java.util.List;

import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

public class RadarActivity extends MainActivity {

    private String type;
    private String location;
    private Boolean loop;
    private Boolean enhanced;
    private Boolean mosaic;

    private String radarName;

    private Menu actionsMenu;

    private MenuItem addFavorite;
    private MenuItem removeFavorite;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_radar, null, false);
        mDrawerLayout.addView(contentView, 0);

        Intent intent = getIntent();
        type = intent.getStringExtra("type");
        location = intent.getStringExtra("location");
        loop = intent.getBooleanExtra("loop", false);
        enhanced = intent.getBooleanExtra("enhanced", false);
        mosaic = intent.getBooleanExtra("mosaic", false);

        navigationView = findViewById(R.id.nav_view);

        if (mosaic) {
            int index = Arrays.asList(getResources().getStringArray(R.array.mosaic_values)).indexOf(location);
            radarName = getResources().getStringArray(R.array.mosaic_names)[index];
        } else {
            int index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
            radarName = getResources().getStringArray(R.array.location_names)[index];
        }

        if (intent.getBooleanExtra("favorite", false))
            radarName = intent.getStringExtra("name");
        else
            radarName = radarName.replaceAll("[^/]+/ ","");
        setTitle(radarName);

        WebView radarWebView = findViewById(R.id.radarWebView);
        radarWebView.getSettings().setLoadWithOverviewMode(true);
        radarWebView.getSettings().setUseWideViewPort(true);
        radarWebView.getSettings().setBuiltInZoomControls(true);
        radarWebView.getSettings().setDisplayZoomControls(false);
        radarWebView.getSettings().setJavaScriptEnabled(true);
        radarWebView.getSettings().setDomStorageEnabled(true);
        radarWebView.getSettings().setSupportZoom(true);

        if (enhanced) {
            radarWebView.loadData(displayEnhancedRadar(location, type), "text/html", null);
        } else if (mosaic) {
            radarWebView.loadData(displayMosaicImage(location, loop), "text/html", null);
        } else {
            radarWebView.loadData(displayLiteImage(location, type, loop), "text/html", null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.radar_actions, menu);
        actionsMenu = menu;
        addFavorite = actionsMenu.findItem(R.id.action_add_favorite);
        removeFavorite = actionsMenu.findItem(R.id.action_remove_favorite);

        List<Favorite> favorites = settingsDB.favoriteDao().findByData(location, type, loop, enhanced, mosaic);

        if (favorites.size() > 0) {
            addFavorite.setVisible(false);
        } else {
            removeFavorite.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite) {
            addFavoriteDialog();
        } else if (id == R.id.action_remove_favorite) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        List<Favorite> favorites = settingsDB.favoriteDao().findByData(location, type, loop, enhanced, mosaic);
                        for (Favorite favorite : favorites) {
                            settingsDB.favoriteDao().delete(favorite);
                        }

                        addFavorite.setVisible(true);
                        removeFavorite.setVisible(false);
                        populateFavorites(navigationView.getMenu());
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_favorite_removal).setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        return super.onOptionsItemSelected(item);
    }

    public void addFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Favorite");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(radarName);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing as we will override below
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String name = input.getText().toString();
                Favorite exists = settingsDB.favoriteDao().findByName(name);
                if (name.equals("")) {
                    input.setError(getString(R.string.empty_name_error));
                } else if (exists != null) {
                    input.setError(getString(R.string.already_exists_error));
                } else {
                    Favorite favorite = new Favorite();
                    favorite.setName(input.getText().toString());
                    favorite.setLocation(location);
                    favorite.setType(type);
                    favorite.setLoop(loop);
                    favorite.setEnhanced(enhanced);
                    favorite.setMosaic(mosaic);
                    settingsDB.favoriteDao().insertAll(favorite);

                    addFavorite.setVisible(false);
                    removeFavorite.setVisible(true);
                    populateFavorites(navigationView.getMenu());
                    dialog.dismiss();
                }
            }
        });
    }

    public String displayMosaicImage(String mosaic, Boolean loop) {
        String url = "https://radar.weather.gov/Conus/";
        if (loop) {
            if (mosaic.equals("latest")) {
                url += "Loop/NatLoop.gif";
            } else {
                url += "Loop/" + mosaic + "_loop.gif";
            }
        } else {
            url += "RadarImg/"+mosaic+".gif";
        }

        return displayRadar(url);
    }

    public String displayLiteImage(String loc, String type, Boolean loop) {
        String url = "https://radar.weather.gov/lite/"+type+"/";
        if (loop) {
            url += loc+"_loop.gif";
        } else {
            url += loc+"_0.png";
        }

        return displayRadar(url);
    }

    public String displayRadar(String url) {
        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("lite_radar");
        html.set("url", url);

        return html.toString();
    }

    public String displayEnhancedRadar(String location, String type) {
        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("enhanced_radar");
        html.set("location", location);
        html.set("type", type);

        return html.toString();
    }
}
