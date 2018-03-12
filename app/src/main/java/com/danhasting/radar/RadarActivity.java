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
        String data =
            "<html>\n" +
            "<head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "<style>\n" +
            "body {margin:0;}\n" +
            "img {max-width:100%;max-height:100%;}\n" +
            "#image {\n" +
            "    background:url("+url+");\n" +
            "    position:absolute;\n" +
            "    width:100%;\n" +
            "    height:100%;\n" +
            "    background-size:contain;\n" +
            "    background-position:center;\n" +
            "    background-repeat:no-repeat;\n" +
            "    z-index:1;\n" +
            "}\n" +
            "\n" +
            "#failed {\n" +
            "    position:absolute;\n" +
            "    width:100%;\n" +
            "    text-align:center;\n" +
            "    top:40%;\n" +
            "    z-index:0;\n" +
            "}\n" +
            "\n" +
            ".hidden {\n" +
            "    display:none;\n" +
            "}\n" +
            "</style>\n" +
            "<script>\n" +
            "setTimeout(function() {\n" +
            "    document.querySelector(\"#failed\").classList.remove(\"hidden\");\n" +
            "}, 10000);\n" +
            "</script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"image\"></div>\n" +
            "    <div id=\"failed\" class=\"hidden\">Failed to load image</div>\n" +
            "</body>\n" +
            "</html>";
        return data;
    }

    public String displayEnhancedRadar(String loc, String type) {
        String data =
            "<html>\n" +
            "<head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "<style>\n" +
            "body {margin:0;}\n" +
            "img {max-width:100%;max-height:100%;}\n" +
            ".image {\n" +
            "    position:absolute;\n" +
            "    width:100%;\n" +
            "    height:100%;\n" +
            "    background-size:contain !important;\n" +
            "    background-position:center !important;\n" +
            "    background-repeat:no-repeat !important;\n" +
            "}\n" +
            "\n" +
            "#image0 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Overlays/Topo/Short/"+loc+"_Topo_Short.jpg);\n" +
            "    z-index:1;\n" +
            "}\n" +
            "\n" +
            "#image1 {\n" +
            "    background:url(https://radar.weather.gov/ridge/RadarImg/"+type+"/"+loc+"_"+type+"_0.gif);\n" +
            "    z-index:2;\n" +
            "}\n" +
            "\n" +
            "#image2 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Overlays/County/Short/"+loc+"_County_Short.gif);\n" +
            "    z-index:3;\n" +
            "}\n" +
            "\n" +
            "#image4 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Overlays/Highways/Short/"+loc+"_Highways_Short.gif);\n" +
            "    z-index:4;\n" +
            "}\n" +
            "\n" +
            "#image5 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Overlays/Cities/Short/"+loc+"_City_Short.gif);\n" +
            "    z-index:5;\n" +
            "}\n" +
            "\n" +
            "#image6 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Warnings/Short/"+loc+"_Warnings_0.gif);\n" +
            "    z-index:6;\n" +
            "}\n" +
            "\n" +
            "#image7 {\n" +
            "    background:url(https://radar.weather.gov/ridge/Legend/"+type+"/"+loc+"_"+type+"_Legend_0.gif);\n" +
            "    z-index:7;\n" +
            "}\n" +
            "\n" +
            "#failed {\n" +
            "    position:absolute;\n" +
            "    width:100%;\n" +
            "    text-align:center;\n" +
            "    top:40%;\n" +
            "    z-index:0;\n" +
            "}\n" +
            "\n" +
            ".hidden {\n" +
            "    display:none;\n" +
            "}\n" +
            "</style>\n" +
            "<script>\n" +
            "setTimeout(function() {\n" +
            "    document.querySelector(\"#failed\").classList.remove(\"hidden\");\n" +
            "}, 5000);\n" +
            "</script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"image0\" class=\"image\"></div>\n" +
            "    <div id=\"image1\" class=\"image\"></div>\n" +
            "    <div id=\"image2\" class=\"image\"></div>\n" +
            "    <div id=\"image4\" class=\"image\"></div>\n" +
            "    <div id=\"image5\" class=\"image\"></div>\n" +
            "    <div id=\"image6\" class=\"image\"></div>\n" +
            "    <div id=\"image7\" class=\"image\"></div>\n" +
            "    <div id=\"failed\" class=\"hidden\">Failed to load image</div>\n" +
            "</body>\n" +
            "</html>";
        return data;
    }
}
