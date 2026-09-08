package com.absolute.floral.things;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.soma.Soma;

import java.util.LinkedHashMap;
import java.util.Map;

/** Apple-Photos "Media Types" / "Utilities" — a 2-column grid of pill rows. */
public final class MediaRows {

    private MediaRows() {}

    public static final class Row {
        public final int count;
        public final int icon;
        public final Runnable onTap;
        public Row(int count, int icon, Runnable onTap) {
            this.count = count; this.icon = icon; this.onTap = onTap;
        }
    }

    public static View pillRows(Activity a, Soma s, LinkedHashMap<String, Row> rows) {
        LinearLayout grid = new LinearLayout(a);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(a, 16), 0, dp(a, 16), 0);

        LinearLayout line = null;
        int i = 0;
        for (Map.Entry<String, Row> e : rows.entrySet()) {
            if (i % 2 == 0) {
                line = new LinearLayout(a);
                line.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.topMargin = dp(a, 9);
                grid.addView(line, llp);
            }
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            clp.leftMargin = i % 2 == 0 ? 0 : dp(a, 9);
            line.addView(pill(a, s, e.getKey(), e.getValue()), clp);
            i++;
        }
        if (i % 2 == 1 && line != null) {
            View sp = new View(a);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, 1, 1f);
            slp.leftMargin = dp(a, 9);
            line.addView(sp, slp);
        }
        return grid;
    }

    private static View pill(Activity a, Soma s, String label, Row r) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.surfaceStrong);
        bg.setCornerRadius(dp(a, 16));
        row.setBackground(bg);

        if (r.icon != 0) {
            ImageView ic = new ImageView(a);
            ic.setImageResource(r.icon);
            if (ic.getDrawable() != null) ic.getDrawable().mutate().setTint(
                    a.getResources().getColor(com.absolute.floral.R.color.ios_blue));
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(a, 18), dp(a, 18));
            ilp.rightMargin = dp(a, 8);
            row.addView(ic, ilp);
        }

        LinearLayout txt = new LinearLayout(a);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(a);
        t.setText(label);
        t.setTypeface(Soma.body(a), Typeface.BOLD);
        t.setTextSize(13);
        t.setTextColor(s.ink);
        t.setMaxLines(1);
        txt.addView(t);
        TextView c = new TextView(a);
        c.setText(String.valueOf(r.count));
        c.setTextSize(11);
        c.setTextColor(s.inkMute);
        txt.addView(c);
        row.addView(txt, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chev = new TextView(a);
        chev.setText("›");
        chev.setTextSize(15);
        chev.setTextColor(s.inkMute);
        row.addView(chev);

        row.setOnClickListener(v -> { if (r.onTap != null) r.onTap.run(); });
        return row;
    }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
