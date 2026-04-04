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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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
import com.danhasting.radar.database.Source;


public class EnhancedRadarFragment extends Fragment {

    private String location;
    private Source source;
    private boolean errorShown = false;
    private boolean pageLoaded = false;
    private boolean darkMode = false;

    private ProgressBar progressBar;
    private WebView radarEnhancedView;

    private SharedPreferences settings;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enhanced_radar, container, false);
        settings = PreferenceManager.getDefaultSharedPreferences(requireContext());

        darkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

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
                String value;

                if (source == Source.AIR)
                    value = uri.getFragment();
                else
                    value = uri.getQueryParameter("settings");

                if (value != null) {
                    // handle parameter on UI thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        SharedPreferences.Editor editor = settings.edit();

                        if (source == Source.AIR)
                            editor.putString("airnow_website_settings", value);
                        else
                            editor.putString("nws_website_settings", value);
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

                String css;
                int scale;

                if (source == Source.AIR) {
                    // Remove info at bottom (looks cluttered on a small screen)
                    css = ".maplibregl-ctrl-bottom-right  { display: none !important; } "
                            + ".refresh-div { bottom: 15px !important; right: 10px !important; } "
                            + ".map-legend { overflow:hidden !important; } ";

                    if (darkMode) {
                        css += "body { background: #333 !important; color: #FFF !important } "
                                + ".offcanvas, .modal-content { background-color: #333 !important; color: #FFF !important } "
                                + ".accordion-item, .accordion-button, .accessibility-container, .dropdown-menu, .form-control, .info-bubble "
                                + "{ background: #444 !important; color: #FFF !important } "
                                + ".refresh-div p { background-color: #333 !important; } "
                                + ".form-control::placeholder { color: #999 !important } "
                                + ".form-control { border-color: #666 } ";
                    }

                    // Inject JS to get onUrlChanged working for URI fragment changes
                    String fragment_js = "window.addEventListener('hashchange', function(){ AndroidBridge.onUrlChanged(location.href); });"
                            + "AndroidBridge.onUrlChanged(location.href);";
                    view.evaluateJavascript(fragment_js, null);

                    // Inject js to replace initial button text
                    String button_name_js = "(function(){"
                            + "  var els = document.querySelectorAll('button[data-bs-dismiss=\"modal\"]');"
                            + "  for (var i = 0; i < els.length; i++) {"
                            + "    var el = els[i];"
                            + "    if ((el.textContent || '').trim() === 'Click to Geolocate') {"
                            + "      el.textContent = 'Continue';"
                            + "    }"
                            + "  }"
                            + "})();";
                    view.evaluateJavascript(button_name_js, null);

                    scale = settings.getInt("air_scale", 100);
                } else {
                    // Remove website header
                    css = "header { display: none !important; } main { inset: 0px !important; }";

                    // Allow capturing URL changes
                    String capture_url_change_js = "(() => {"
                            + "function notify() { AndroidBridge.onUrlChanged(window.location.href); }"
                            + "const _pushState = history.pushState;"
                            + "history.pushState = function(){ _pushState.apply(this, arguments); notify(); };"
                            + "const _replaceState = history.replaceState;"
                            + "history.replaceState = function(){ _replaceState.apply(this, arguments); notify(); };"
                            + "window.addEventListener('popstate', notify);"
                            + "window.addEventListener('hashchange', notify);"
                            + "notify();"
                            + "})()";
                    view.evaluateJavascript(capture_url_change_js, null);

                    scale = settings.getInt("radar_scale", 100);
                }

                // Inject custom css
                String css_js = "(function(){" +
                        "var parent = document.head || document.documentElement;" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.appendChild(document.createTextNode('" + css + "'));" +
                        "parent.appendChild(style);" +
                        "})()";
                view.evaluateJavascript(css_js, null);

                // Set the overlay scale from settings
                float scaleFactor = scale / 100f;
                String scale_js = "document.querySelector('meta[name=viewport]')?.setAttribute('content', 'width=device-width, initial-scale="
                        + scaleFactor + ", maximum-scale=1.0, user-scalable=no');"
                        + "if(!document.querySelector('meta[name=viewport]')){"
                        + "var m=document.createElement('meta');m.name='viewport';m.content='width=device-width, initial-scale="
                        + scaleFactor + ",maximum-scale=1.0,user-scalable=no';document.head.appendChild(m);}";

                view.evaluateJavascript(scale_js, null);


                if (pageLoaded) return;

                radarEnhancedView.postDelayed(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (!errorShown) radarEnhancedView.setVisibility(View.VISIBLE);
                    pageLoaded = true;
                }, 250);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (errorShown) return;
                if (pageLoaded) return;

                progressBar.setVisibility(View.GONE);
                radarEnhancedView.setVisibility(View.GONE);

                Toast.makeText(requireActivity(), "Map did not load. Check your network connection.", Toast.LENGTH_LONG).show();
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

        if (bundle != null) {
            source = (Source) bundle.getSerializable("source");
            location = bundle.getString("location");
        }

        if (source == null) source = Source.RADAR;
        if (location == null) location = "";

        // Set the basemap to dark if it's the first launch and we're in dark mode
        if (darkMode) {
            if (source == Source.AIR) {
                String url = getString(R.string.air_quality_website);
                CookieManager cookieManager = CookieManager.getInstance();
                String currentCookie = CookieManager.getInstance().getCookie(url);

                if (currentCookie == null || !currentCookie.contains("basemap")) {
                    String cookieString = "basemap=%22esriDarkStyle%22;";

                    cookieManager.setAcceptCookie(true);
                    cookieManager.setCookie(url, cookieString);
                }
            } else {
                String website_settings = settings.getString("nws_website_settings", "");
                if (website_settings.isEmpty()) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("nws_website_settings", getString(R.string.nws_dark_base_map));
                    editor.apply();
                }

            }
        }

        refreshEnhancedRadar(location);
    }

    public void refreshEnhancedRadar(String location) {
        errorShown = false;
        pageLoaded = false;

        progressBar.setVisibility(View.VISIBLE);
        radarEnhancedView.setVisibility(View.GONE);

        if (source == Source.AIR) {
            String website_settings = settings.getString("airnow_website_settings", "");

            if (!location.isEmpty())
                website_settings = location;

            if (!Objects.equals(website_settings, ""))
                radarEnhancedView.loadUrl(getString(R.string.air_quality_website) + "#"+website_settings);
            else
                radarEnhancedView.loadUrl(getString(R.string.air_quality_website));
        } else {
            String website_settings = settings.getString("nws_website_settings", "");

            if (!location.isEmpty())
                website_settings = location;

            if (!Objects.equals(website_settings, ""))
                radarEnhancedView.loadUrl(getString(R.string.radar_website) + "?settings=" + website_settings);
            else
                radarEnhancedView.loadUrl(getString(R.string.radar_website));
        }
    }

    public String getCurrentSettings() {
        Uri uri = Uri.parse(radarEnhancedView.getUrl());

        if (source == Source.AIR)
            return uri.getFragment();
        else
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
