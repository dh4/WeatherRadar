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
package com.danhasting.radar.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebView;

import java.util.Arrays;
import java.util.Set;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

public class RadarFragment extends Fragment {

    private Source source;
    private String location;
    private String type;
    private Boolean loop;
    private int distance;

    private WebView radarWebView;

    private SharedPreferences settings;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        radarWebView = view.findViewById(R.id.radarWebView);
        radarWebView.getSettings().setLoadWithOverviewMode(true);
        radarWebView.getSettings().setUseWideViewPort(true);
        radarWebView.getSettings().setBuiltInZoomControls(true);
        radarWebView.getSettings().setDisplayZoomControls(false);
        radarWebView.getSettings().setJavaScriptEnabled(true);
        radarWebView.getSettings().setDomStorageEnabled(true);
        radarWebView.getSettings().setSupportZoom(true);

        registerForContextMenu(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null) {
            source = (Source) bundle.getSerializable("source");
            location = bundle.getString("location");
            loop = bundle.getBoolean("loop", false);
            distance = bundle.getInt("distance", 50);
            type = bundle.getString("type");
            Boolean enhanced = bundle.getBoolean("enhanced", false);

            if (source == null) source = Source.NWS;
            if (type == null) type = "";
            if (location == null) location = "";

            if (enhanced) {
                radarWebView.loadData(displayEnhancedRadar(location, type), "text/html", null);
            } else if (source == Source.MOSAIC) {
                radarWebView.loadData(displayMosaicImage(location, loop), "text/html", null);
            } else if (source == Source.WUNDERGROUND) {
                // We dynamically set the size for wunderground images, so wait for the layout to load
                final ViewTreeObserver observer = radarWebView.getViewTreeObserver();
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!isAdded())
                            return;

                        radarWebView.loadData(displayWundergroundImage(location, type, loop, distance),
                                "text/html", null);

                        radarWebView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            } else {
                radarWebView.loadData(displayLiteImage(location, type, loop), "text/html", null);
            }
        }
    }

    public void refreshRadar() {
        if (radarWebView != null)
            radarWebView.reload();
    }

    private String displayMosaicImage(String mosaic, Boolean loop) {
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

    private String displayLiteImage(String loc, String type, Boolean loop) {
        String url = "https://radar.weather.gov/lite/"+type+"/";
        if (loop) {
            url += loc+"_loop.gif";
        } else {
            url += loc+"_0.png";
        }

        return displayRadar(url);
    }

    private String displayWundergroundImage(String loc, String type, Boolean loop, int distance) {
        String apiKey = settings.getString("api_key","");
        int time_label = settings.getBoolean("show_time_label", true) ? 1 : 0;
        int snow = settings.getBoolean("show_snow_mix", true) ? 1 : 0;
        int smooth = settings.getBoolean("smoothing", true) ? 1 : 0;
        int noclutter = settings.getBoolean("noclutter", true) ? 1 : 0;

        String animateText = "radar";

        if (type.startsWith("sat_") && loop)
            animateText = "animatedsatellite";
        else if (type.startsWith("sat_"))
            animateText = "satellite";
        else if (loop)
            animateText = "animatedradar";


        String defaultRes = getString(R.string.image_resolution_default);

        String units = settings.getString("distance_units", getString(R.string.distance_unit_default));
        String speed = settings.getString("animation_speed", getString(R.string.animation_speed_default));
        String res = settings.getString("image_resolution", defaultRes);
        String frames = settings.getString("animation_frames", getString(R.string.animation_frames_default));
        Boolean lower = settings.getBoolean("lower_resolution", false);

        if (res.equals("custom"))
            res = settings.getString("custom_resolution", defaultRes);
        if (!res.matches("\\d+"))
            res = defaultRes;

        if (lower && !onWifi())
            res = Long.toString(Math.round(Integer.parseInt(res) * 0.667));

        int width = radarWebView.getWidth();
        int height = radarWebView.getHeight();

        int imageWidth = Integer.parseInt(res);
        int imageHeight = Integer.parseInt(res);

        if (width > height) {
            Float aspect = (float)width / height;
            imageWidth = Math.round(imageHeight * aspect);
        } else {
            Float aspect = (float)height / width;
            imageHeight = Math.round(imageWidth * aspect);
        }

        String format = "png";
        if (loop)
            format = "gif";

        String url = "https://api.wunderground.com/api/%s/%s/q/zmw:%s.%s" +
                "?width=%s&height=%s&newmaps=1&radius=%s&radunits=%s&smooth=%s&delay=%s&num=%s" +
                "&rainsnow=%s&noclutter=%s&timelabel=%s&timelabel.y=15&timelabel.x=5";
        url = String.format(url, apiKey, animateText, loc, format, imageWidth, imageHeight,
                distance, units, smooth, speed, frames, snow, noclutter, time_label);

        if (type.startsWith("sat_"))
            url += String.format("&borders=1&key=%s", type);

        return displayRadar(url);
    }

    private String displayRadar(String url) {
        AndroidTemplates loader = new AndroidTemplates(getActivity().getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("lite_radar");
        html.set("url", url);
        if (source != null && source != Source.WUNDERGROUND)
            html.set("maximized", Boolean.toString(settings.getBoolean("show_maximized", false)));

        return html.toString();
    }

    private String displayEnhancedRadar(String location, String type) {
        AndroidTemplates loader = new AndroidTemplates(getActivity().getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("enhanced_radar");
        html.set("location", location);
        html.set("type", type);
        html.set("maximized", Boolean.toString(settings.getBoolean("show_maximized", false)));

        if (type.equals("N0Z"))
            html.set("distance", "Long");
        else
            html.set("distance", "Short");

        String[] layers;
        Set<String> layersSet = settings.getStringSet("enhanced_layers", null);
        if (layersSet != null)
            layers = layersSet.toArray(new String[] {});
        else
            layers = getResources().getStringArray(R.array.enhanced_layer_default);

        if (Arrays.asList(layers).contains("0"))
            html.set("image0", "true");
        if (Arrays.asList(layers).contains("1"))
            html.set("image1", "true");
        if (Arrays.asList(layers).contains("2"))
            html.set("image2", "true");
        if (Arrays.asList(layers).contains("3"))
            html.set("image3", "true");
        if (Arrays.asList(layers).contains("4"))
            html.set("image4", "true");
        if (Arrays.asList(layers).contains("5"))
            html.set("image5", "true");
        if (Arrays.asList(layers).contains("6"))
            html.set("image6", "true");
        if (Arrays.asList(layers).contains("7"))
            html.set("image7", "true");

        return html.toString();
    }

    private Boolean onWifi() {
        ConnectivityManager m = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (m != null) {
            NetworkInfo netInfo = m.getActiveNetworkInfo();
            return netInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }
}
