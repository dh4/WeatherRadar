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

import android.app.Fragment;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;
import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

import java.util.Arrays;
import java.util.Set;

public class RadarFragment extends Fragment {

    private Source source;

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
            String location = bundle.getString("location");
            Boolean loop = bundle.getBoolean("loop", false);
            String type = bundle.getString("type");
            boolean enhanced = bundle.getBoolean("enhanced", false);

            if (source == null) source = Source.NWS;
            if (type == null) type = "";
            if (location == null) location = "";

            if (enhanced) {
                radarWebView.loadData(displayEnhancedRadar(location, type), "text/html", null);
            } else if (source == Source.MOSAIC) {
                radarWebView.loadData(displayMosaicImage(location, loop), "text/html", null);
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
        String url = "https://radar.weather.gov/ridge/lite/";
        if (loop) {
            url += mosaic + "_loop.gif";
        } else {
            url += mosaic + "_0.gif";
        }

        return displayRadar(url);
    }

    private String displayLiteImage(String loc, String type, Boolean loop) {
        String url = "https://radar.weather.gov/ridge/lite/";
        if (loop) {
            url += loc + "_loop.gif";
        } else {
            url += loc + "_0.gif";
        }

        return displayRadar(url);
    }

    private String displayRadar(String url) {
        AndroidTemplates loader = new AndroidTemplates(getActivity().getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("lite_radar");
        html.set("url", url);
        if (source != null)
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
            layers = layersSet.toArray(new String[]{});
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
}
