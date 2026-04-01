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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.danhasting.radar.R;
import com.danhasting.radar.database.Source;



public class RadarWebsiteFragment extends Fragment {

    private Source source;

    private WebView radarWebsiteView;

    private SharedPreferences settings;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar_website, container, false);
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        radarWebsiteView = view.findViewById(R.id.radarWebsiteView);
        radarWebsiteView.getSettings().setLoadWithOverviewMode(true);
        radarWebsiteView.getSettings().setUseWideViewPort(true);
        radarWebsiteView.getSettings().setBuiltInZoomControls(true);
        radarWebsiteView.getSettings().setDisplayZoomControls(false);
        radarWebsiteView.getSettings().setJavaScriptEnabled(true);
        radarWebsiteView.getSettings().setDomStorageEnabled(true);
        radarWebsiteView.getSettings().setSupportZoom(true);

        radarWebsiteView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String css = "header { display: none !important; } main { inset: 0px !important; }";
                // Escape single quotes and newlines for embedding in JS string
                css = css.replace("\n", "\\n").replace("'", "\\'");

                String js = "(function(){" +
                        "var parent = document.head || document.documentElement;" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.appendChild(document.createTextNode('" + css + "'));" +
                        "parent.appendChild(style);" +
                        "})()";

                view.evaluateJavascript(js, null);
            }
        });

        registerForContextMenu(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        radarWebsiteView.loadUrl("https://radar.weather.gov/");

    }
}
