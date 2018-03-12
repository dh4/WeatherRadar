package com.danhasting.radar;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
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

        AndroidTemplates loader = new AndroidTemplates(getBaseContext());
        Theme theme = new Theme(loader);
        Chunk html = theme.makeChunk("about");

        TextView about = findViewById(R.id.about_text);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            about.setText(Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            about.setText(Html.fromHtml(html.toString()));
        }
    }
}
