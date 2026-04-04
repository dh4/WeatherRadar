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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.danhasting.radar.R;


public class EnhancedRadarFragment extends Fragment {

    private String location;
    private boolean errorShown = false;

    private ProgressBar progressBar;
    private WebView radarEnhancedView;

    private SharedPreferences settings;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enhanced_radar, container, false);
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());

        progressBar = view.findViewById(R.id.radarEnhancedProgress);

        radarEnhancedView = view.findViewById(R.id.radarEnhancedView);
        radarEnhancedView.getSettings().setLoadWithOverviewMode(true);
        radarEnhancedView.getSettings().setUseWideViewPort(true);
        radarEnhancedView.getSettings().setBuiltInZoomControls(true);
        radarEnhancedView.getSettings().setDisplayZoomControls(false);
        radarEnhancedView.getSettings().setJavaScriptEnabled(true);
        radarEnhancedView.getSettings().setDomStorageEnabled(true);
        radarEnhancedView.getSettings().setSupportZoom(true);

        radarEnhancedView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onUrlChanged(String url) {
                // Runs on background thread; post to UI if needed
                Uri uri = Uri.parse(url);
                String value = uri.getQueryParameter("settings");
                if (value != null) {
                    // handle parameter on UI thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("radar_website_settings", value);
                        editor.apply();

                        Bundle result = new Bundle();
                        result.putString("settings", value);
                        if (!isAdded() || getParentFragmentManager() == null) return;
                        getParentFragmentManager().setFragmentResult("current_settings", result);
                    });
                }
            }
        }, "AndroidBridge");

        radarEnhancedView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Remove website header
                String css = "header { display: none !important; } main { inset: 0px !important; }";

                String js = "(function(){" +
                        "var parent = document.head || document.documentElement;" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.appendChild(document.createTextNode('" + css + "'));" +
                        "parent.appendChild(style);" +
                        "})()";
                view.evaluateJavascript(js, null);

                // Allow capturing URL changes
                String js2 = "(() => {"
                        + "function notify() { AndroidBridge.onUrlChanged(window.location.href); }"
                        + "const _pushState = history.pushState;"
                        + "history.pushState = function(){ _pushState.apply(this, arguments); notify(); };"
                        + "const _replaceState = history.replaceState;"
                        + "history.replaceState = function(){ _replaceState.apply(this, arguments); notify(); };"
                        + "window.addEventListener('popstate', notify);"
                        + "window.addEventListener('hashchange', notify);"
                        + "notify();"
                        + "})()";
                view.evaluateJavascript(js2, null);

                // Set the overlay scale from settings
                int scale = settings.getInt("radar_scale",100);
                float scaleFactor = scale / 100f;

                String js3 = "document.querySelector('meta[name=viewport]')?.setAttribute('content', 'width=device-width, initial-scale="
                        + scaleFactor + ", maximum-scale=1.0, user-scalable=no');"
                        + "if(!document.querySelector('meta[name=viewport]')){"
                        + "var m=document.createElement('meta');m.name='viewport';m.content='width=device-width, initial-scale="
                        + scaleFactor + ",maximum-scale=1.0,user-scalable=no';document.head.appendChild(m);}";

                view.evaluateJavascript(js3, null);

                radarEnhancedView.postDelayed(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!errorShown) radarEnhancedView.setVisibility(View.VISIBLE );
                }, 250);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (errorShown) return;

                progressBar.setVisibility(View.GONE);
                radarEnhancedView.setVisibility(View.GONE);

                Toast.makeText(requireActivity(), "Radar did not load. Check your network connection.", Toast.LENGTH_LONG).show();
                errorShown = true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                // Prevent loading websites other than radar.weather.gov
                Uri uri = request.getUrl();
                String radarWebsite = getString(R.string.radar_website);
                if (uri.toString().equals(radarWebsite) || uri.toString().startsWith(radarWebsite + "?")
                        || uri.toString().startsWith(radarWebsite + "#"))
                    return false;
                else if (uri.toString().equals(radarWebsite + "null"))
                    return true;
                else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                    view.getContext().startActivity(intent);
                    return true;
                }
            }
        });

        registerForContextMenu(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle != null)
            location = bundle.getString("location");

        if (location == null) location = "";

        refreshEnhancedRadar(location);
    }

    public void refreshEnhancedRadar(String location) {
        errorShown = false;

        progressBar.setVisibility(View.VISIBLE);
        radarEnhancedView.setVisibility(View.GONE);

        String website_settings = settings.getString("radar_website_settings", "");

        if (!location.isEmpty())
            website_settings = location;

        if (!Objects.equals(website_settings, ""))
            radarEnhancedView.loadUrl(getString(R.string.radar_website) + "?settings="+website_settings);
        else
            radarEnhancedView.loadUrl(getString(R.string.radar_website));
    }

    public String getCurrentSettings() {
        Uri uri = Uri.parse(radarEnhancedView.getUrl());
        return uri.getQueryParameter("settings");
    }

    public CompletableFuture<String> getCurrentLocationNameAsync() {
        CompletableFuture<String> f = new CompletableFuture<>();
        radarEnhancedView.evaluateJavascript(
                "document.querySelector('.search-location').innerText;",
                value -> {
                    String result = "";
                    if (value != null && !value.equals("null")) {
                        result = value.replaceAll("^\"|\"$", "")
                                .replaceAll("\\\\n", "\n")
                                .replaceAll("\\\\t", "\t")
                                .replaceAll("\\\\\"", "\"");
                    }
                    f.complete(result);
                }
        );
        return f;
    }
}
