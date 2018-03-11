package com.danhasting.radar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class RadarActivity extends MainActivity {

    private String type;
    private String location;
    private Boolean loop;

    private Menu actionsMenu;

    MenuItem addFavorite;
    MenuItem removeFavorite;
    MenuItem setDefault;
    MenuItem removeDefault;
    NavigationView navigationView;

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

        navigationView = findViewById(R.id.nav_view);

        WebView radarWebView = findViewById(R.id.radarWebView);
        radarWebView.getSettings().setLoadWithOverviewMode(true);
        radarWebView.getSettings().setUseWideViewPort(true);

        radarWebView.loadData(displayLiteImage(location, type, loop), "text/html", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.radar_actions, menu);
        actionsMenu = menu;
        addFavorite = actionsMenu.findItem(R.id.action_add_favorite);
        removeFavorite = actionsMenu.findItem(R.id.action_remove_favorite);
        setDefault = actionsMenu.findItem(R.id.action_set_default);
        removeDefault = actionsMenu.findItem(R.id.action_remove_default);

        List<Favorite> favorites = settingsDB.favoriteDao().findByData(location, type, loop);

        if (favorites.size() > 0) {
            addFavorite.setVisible(false);
        } else {
            removeFavorite.setVisible(false);
        }

        Boolean defaultRadar = settings.getBoolean("default", false);
        String defaultLocation = settings.getString("default_location","BMX");
        String defaultType = settings.getString("default_type","N0R");
        Boolean defaultLoop = settings.getBoolean("default_loop",false);

        if (defaultRadar && defaultLocation.equals(location) && defaultType.equals(type) && defaultLoop == loop) {
            setDefault.setVisible(false);
        } else {

            removeDefault.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_favorite) {
            addFavoriteDialog();
        } else if (id == R.id.action_remove_favorite) {
            List<Favorite> favorites = settingsDB.favoriteDao().findByData(location, type, loop);
            for (Favorite favorite : favorites) {
                settingsDB.favoriteDao().delete(favorite);
            }

            addFavorite.setVisible(true);
            removeFavorite.setVisible(false);
            populateFavorites(navigationView.getMenu());
        } else if (id == R.id.action_set_default) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("default", true);
            editor.putString("default_location", location);
            editor.putString("default_type", type);
            editor.putBoolean("default_loop", loop);
            editor.apply();

            setDefault.setVisible(false);
            removeDefault.setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_default).setVisible(true);
        } else if (id == R.id.action_remove_default) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("default", false);
            editor.apply();

            setDefault.setVisible(true);
            removeDefault.setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_default).setVisible(false);
        }

        return super.onOptionsItemSelected(item);
    }

    public void addFavoriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Favorite");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        int index = Arrays.asList(getResources().getStringArray(R.array.location_values)).indexOf(location);
        input.setText(getResources().getStringArray(R.array.location_names)[index]);

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
            url += "Loop/"+mosaic+"_loop.gif";
        } else {
            url += "RadarImg/"+mosaic+".gif";
        }

        return displayRadar(url);
    }

    public String displayMosaicImage(String mosaic) {
        return displayMosaicImage(mosaic, false);
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

    public String displayLiteImage(String loc, String type) {
        return displayLiteImage(loc, type, false);
    }

    public String displayLiteImage(String loc) {
        return displayLiteImage(loc, "N0R", false);
    }

    public String displayRadar(String url) {
        String data =
            "<html>" +
            "<head>" +
                "<meta name=\"viewport\" content=\"user-scalable=yes, width=device-width, height=device-height, target-densitydpi=device-dpi, initial-scale=1, minimum-scale=1, maximum-scale=2\">" +
                "<style>body {margin:0;} img {min-width:100%}</style>" +
                    "<script>document.addEventListener('DOMContentLoaded', function() {\n" +
                    "    scrollTo(($(document).width() - $(window).width()) / 2, 0);\n" +
                    "});</script>" +
            "</head>" +
            "<body>" +
                "<img src=\""+url+"\" alt=\"Failed to load image.\" />" +
            "</body>" +
            "</html>";
        return data;
    }

    public String displayEnhancedRadar(String loc, String type) {
        String data =
            "<html>" +
                "<head>" +
                    "<meta name=\"viewport\" content=\"user-scalable=yes, width=device-width, height=device-height, target-densitydpi=device-dpi, initial-scale=1, minimum-scale=1, maximum-scale=2\">" +
                    "<style>body {margin:0;} div {position:absolute;top:0;}img {min-width:100%}</style>" +
                "</head>" +
                "<body>\n" +
                    "<div id=\"image0\"><img style=\"z-index:0\" src=\"https://radar.weather.gov/ridge/Overlays/Topo/Short/"+loc+"_Topo_Short.jpg\"></div>\n" +
                    "<div id=\"image1\"><img style=\"z-index:1\" src=\"https://radar.weather.gov/ridge/RadarImg/"+type+"/"+loc+"_"+type+"_0.gif\" name=\"conditionalimage\"></div>\n" +
                    "<div id=\"image2\"><img style=\"z-index:2\" src=\"https://radar.weather.gov/ridge/Overlays/County/Short/"+loc+"_County_Short.gif\"></div>\n" +
                    "<div id=\"image4\"><img style=\"z-index:4\" src=\"https://radar.weather.gov/ridge/Overlays/Highways/Short/"+loc+"_Highways_Short.gif\"></div>\n" +
                    "<div id=\"image5\"><img style=\"z-index:5\" src=\"https://radar.weather.gov/ridge/Overlays/Cities/Short/"+loc+"_City_Short.gif\"></div>\n" +
                    "<div id=\"image6\"><img style=\"z-index:6\" src=\"https://radar.weather.gov/ridge/Warnings/Short/"+loc+"_Warnings_0.gif\" border=\"0\"></div>\n" +
                    "<div id=\"image7\"><img style=\"z-index:7\" src=\"https://radar.weather.gov/ridge/Legend/"+type+"/"+loc+"_N0R_Legend_0.gif\" name=\"conditionallegend\" border=\"0\"></div>\n" +
                "</body>" +
            "</html>";
        return data;
    }
}
