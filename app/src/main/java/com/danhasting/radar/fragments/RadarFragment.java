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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

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
        settings = PreferenceManager.getDefaultSharedPreferences(requireActivity());

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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = requireArguments();

        source = (Source) bundle.getSerializable("source");
        String location = bundle.getString("location");
        Boolean loop = bundle.getBoolean("loop", false);

        if (source == null) source = Source.NWS;
        if (location == null) location = "";

        if (source == Source.MOSAIC)
            radarWebView.loadDataWithBaseURL(null, displayMosaicImage(location, loop), "text/html", "UTF-8", null);
        else
            radarWebView.loadDataWithBaseURL(null, displayLiteImage(location, loop), "text/html", "UTF-8", null);
    }

    public void refreshRadar() {
        if (radarWebView != null)
            radarWebView.reload();
    }

    private String displayMosaicImage(String mosaic, Boolean loop) {
        String url = getString(R.string.radar_images_location);
        if (loop)
            url += mosaic + "_loop.gif";
        else
            url += mosaic + "_0.gif";

        return displayRadar(url);
    }

    private String displayLiteImage(String loc, Boolean loop) {
        String url = getString(R.string.radar_images_location);
        if (loop)
            url += loc + "_loop.gif";
        else
            url += loc + "_0.gif";

        return displayRadar(url);
    }

    private String displayRadar(String url) {
        AndroidTemplates loader = new AndroidTemplates(requireActivity().getBaseContext());
        Theme theme = new Theme(loader);

        Chunk html = theme.makeChunk("lite_radar");
        html.set("url", url);
        if (source != null)
            html.set("maximized", Boolean.toString(settings.getBoolean("show_maximized", false)));

        return html.toString();
    }
}
