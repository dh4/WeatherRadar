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
package com.danhasting.radar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.x5.template.Chunk;
import com.x5.template.Theme;
import com.x5.template.providers.AndroidTemplates;

public class AboutActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            View contentView = inflater.inflate(R.layout.activity_about, mDrawerLayout, false);
            mDrawerLayout.addView(contentView, 0);
        }

        setTitle(R.string.about);

        final TextView about = findViewById(R.id.about_text);

        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);
        final Chunk html = theme.makeChunk("about");

        final ViewTreeObserver observer = about.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Html.ImageGetter imageGetter = new Html.ImageGetter() {
                    public Drawable getDrawable(String source) {
                        Drawable d = getResources().getDrawable(R.mipmap.wunderground);

                        int viewWidth = about.getWidth();
                        int width = Math.round(viewWidth * 4 / 5);

                        Float ratio = (float)d.getIntrinsicHeight() / d.getIntrinsicWidth();
                        int height = Math.round(width * ratio);

                        d.setBounds(0, 0, width, height);
                        return d;
                    }
                };


                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    about.setText(Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY,
                            imageGetter, null));
                } else {
                    about.setText(Html.fromHtml(html.toString(), imageGetter, null));
                }

                about.setMovementMethod(LinkMovementMethod.getInstance());
            }
       });
    }
}
