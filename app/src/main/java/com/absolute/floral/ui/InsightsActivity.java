package com.absolute.floral.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.bento.BarChartView;
import com.absolute.floral.bento.BentoLayout;
import com.absolute.floral.bento.BentoTile;
import com.absolute.floral.bento.LibrarySnapshot;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

/** A colourful "Insights" dashboard — counts, a photos-per-month chart, top colours. */
public class InsightsActivity extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        final Soma s = SomaSkin.read(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setId(R.id.root_view);
        scroll.setBackgroundColor(s.ground[0]);
        setContentView(scroll);
        SomaSkin.statusBarIcons(this, s);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        int p = d(16);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(p + d(4), d(26), p, d(4));
        TextView title = new TextView(this);
        title.setText("Insights");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(30);
        title.setTextColor(s.ink);
        head.addView(title);
        root.addView(head);
        Anim.enter(head, 30);

        final BentoLayout b = new BentoLayout(this);
        root.addView(b);

        // chart card
        LinearLayout chartCard = new LinearLayout(this);
        chartCard.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable cbg = new android.graphics.drawable.GradientDrawable();
        cbg.setColor(s.surfaceStrong);
        cbg.setCornerRadius(d(24));
        chartCard.setBackground(cbg);
        chartCard.setPadding(d(16), d(16), d(16), d(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
         clp.setMargins(d(14), d(8), d(14), d(6));
        chartCard.setLayoutParams(clp);
        TextView chartTitle = new TextView(this);
        chartTitle.setText("Photos through the year");
        chartTitle.setTypeface(Soma.body(this), android.graphics.Typeface.BOLD);
        chartTitle.setTextSize(14);
        chartTitle.setTextColor(s.ink);
        chartCard.addView(chartTitle);
        final BarChartView chart = new BarChartView(this);
        chartCard.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, d(150)));
        root.addView(chartCard);

        final LinearLayout swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        swatchRow.setPadding(d(18), d(14), d(18), d(28));
        swatchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView swatchLabel = new TextView(this);
        swatchLabel.setText("Your palette   ");
        swatchLabel.setTypeface(Soma.body(this), android.graphics.Typeface.BOLD);
        swatchLabel.setTextColor(s.inkSoft);
        swatchLabel.setTextSize(13);
        swatchRow.addView(swatchLabel);
        root.addView(swatchRow);

        LibrarySnapshot.get(this, snap -> {
            b.removeAllViews();
            LinearLayout r1 = b.row(112);
            b.tile(r1, 1f, null).gradient(5).label("Photos").countTo(snap.photos, "");
            b.tile(r1, 1f, null).gradient(1).label("Videos").countTo(snap.videos, "");
            b.tile(r1, 1f, null).gradient(3).label("Undated").countTo(snap.undated, "");

            LinearLayout r2 = b.row(112);
            b.tile(r2, 1f, null).gradient(4).label("Albums").countTo(snap.albums, "");
            b.tile(r2, 1f, null).gradient(0).label("This week").countTo(snap.thisWeek, "");
            b.tile(r2, 1.2f, null).gradient(6).label(snap.busiestMonthLabel)
                    .value(String.valueOf(snap.busiestMonthCount)).sub("busiest month");

            String[] mn = { "J","F","M","A","M","J","J","A","S","O","N","D" };
            chart.setData(snap.months, mn);

            for (int i = 0; i < snap.topColourCount; i++) {
                View sw = new View(this);
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setColor(snap.topColours[i]);
                g.setCornerRadius(d(10));
                sw.setBackground(g);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d(40), d(40));
                lp.setMargins(0, 0, d(8), 0);
                swatchRow.addView(sw, lp);
            }
            if (snap.topColourCount == 0) {
                TextView none = new TextView(this);
                none.setText("scanning…");
                none.setTextColor(s.inkMute);
                swatchRow.addView(none);
            }
        });
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
