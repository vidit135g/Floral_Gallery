package com.absolute.floral.things;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.absolute.floral.R;
import com.absolute.floral.soma.Soma;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Apple-Photos "Media Types" / "Utilities" — a 2-column grid of bento pill rows. */
public final class MediaRows {

    private MediaRows() {}

    public static final class Row {
        public final int count;
        public final int icon;          // ignored — derived from the label
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

    /* ---------- label → glyph + hue ---------- */

    private static int[] style(String label) {   // { drawableRes, hue }
        String l = label.toLowerCase(Locale.ROOT);
        if (has(l, "video", "recording")) return new int[]{ R.drawable.ic_movie_creation_white, 0xFF5B6CF0 };
        if (has(l, "selfie"))             return new int[]{ R.drawable.ic_camera_alt_white,     0xFFF06BA8 };
        if (has(l, "live", "motion"))     return new int[]{ R.drawable.ic_autorenew_white,      0xFFEC6AA0 };
        if (has(l, "burst"))              return new int[]{ R.drawable.ic_photo_white,          0xFFEC6AA0 };
        if (has(l, "panorama", "pano"))   return new int[]{ R.drawable.ic_crop_free_white,      0xFF15B8A6 };
        if (has(l, "time-lapse", "timelapse", "time lapse"))
                                          return new int[]{ R.drawable.ic_date_range_white,     0xFF3FA9C9 };
        if (has(l, "raw"))                return new int[]{ R.drawable.ic_iso_white,            0xFFF0A83C };
        if (has(l, "animated", "gif"))    return new int[]{ R.drawable.ic_movie_creation_white, 0xFF7C4DFF };
        if (has(l, "screenshot"))         return new int[]{ R.drawable.ic_crop_original_white,  0xFF6B7A99 };
        if (has(l, "favorite", "favourite")) return new int[]{ R.drawable.ic_star_white,        0xFFF25B6B };
        if (has(l, "hidden"))             return new int[]{ R.drawable.ic_pin_white,            0xFF7A7A82 };
        if (has(l, "duplicate"))          return new int[]{ R.drawable.ic_content_copy_white,   0xFFF07C4A };
        if (has(l, "deleted", "trash"))   return new int[]{ R.drawable.ic_delete_white,         0xFFEA5B6B };
        if (has(l, "recently", "saved", "imports", "viewed", "shared"))
                                          return new int[]{ R.drawable.ic_date_range_white,     0xFF9B6BF0 };
        if (has(l, "document"))           return new int[]{ R.drawable.ic_insert_drive_file_white, 0xFF5C97C4 };
        return new int[]{ R.drawable.ic_photo_white, 0xFF0A84FF };
    }

    private static boolean has(String s, String... any) {
        for (String x : any) if (s.contains(x)) return true;
        return false;
    }

    private static View pill(Activity a, Soma s, String label, Row r) {
        int[] st = style(label);
        int hue = st[1];

        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 10), dp(a, 10), dp(a, 12), dp(a, 10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.surfaceStrong);
        bg.setCornerRadius(dp(a, 18));
        bg.setStroke(Math.max(1, dp(a, 1)), (hue & 0x00FFFFFF) | 0x1F000000);
        row.setBackground(bg);

        // bento icon chip
        ImageView ic = new ImageView(a);
        Drawable d = ContextCompat.getDrawable(a, st[0]);
        if (d != null) {
            d = DrawableCompat.wrap(d.mutate());
            DrawableCompat.setTint(d, hue);
            ic.setImageDrawable(d);
        }
        int pad = dp(a, 7);
        ic.setPadding(pad, pad, pad, pad);
        GradientDrawable chip = new GradientDrawable();
        chip.setColor((hue & 0x00FFFFFF) | 0x24000000);
        chip.setCornerRadius(dp(a, 11));
        ic.setBackground(chip);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(a, 34), dp(a, 34));
        ilp.rightMargin = dp(a, 10);
        row.addView(ic, ilp);

        LinearLayout txt = new LinearLayout(a);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(a);
        t.setText(label);
        t.setTypeface(Soma.body(a), Typeface.BOLD);
        t.setTextSize(12.5f);
        t.setTextColor(s.ink);
        t.setMaxLines(1);
        txt.addView(t);
        TextView c = new TextView(a);
        c.setText(r.count == 0 ? "—" : String.valueOf(r.count));
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
