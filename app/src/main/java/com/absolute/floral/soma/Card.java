package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.bento.Bento;

/**
 * One place for the app's card language — rounded surfaces, pastel bento fills,
 * section headers and key/value rows. Everything here matches {@link com.absolute.floral.bento.BentoTile}.
 */
public final class Card {

    private Card() {}

    public static final float RADIUS = 22f;   // dp — the app-wide card corner

    /* ---------------------------------------------------------------- surfaces */

    /** Plain rounded card: surface fill, hairline, soft elevation, clipped corners. */
    public static void surface(View v, Soma s) { surface(v, s, RADIUS, 2f); }

    public static void surface(View v, Soma s, float radiusDp, float elevationDp) {
        Context c = v.getContext();
        final float r = Soma.dp(c, radiusDp);
        GradientDrawable g = new GradientDrawable();
        g.setColor(s.surface);
        g.setCornerRadius(r);
        g.setStroke(Math.round(Soma.dp(c, 1)), s.hairline);
        v.setBackground(g);
        clip(v, r);
        v.setElevation(Soma.dp(c, elevationDp));
    }

    /** Pastel bento fill for any view — 3-stop diagonal gradient + a soft white sheen. */
    public static void pastel(View v, int gradientIndex) { pastel(v, gradientIndex, RADIUS); }

    public static void pastel(View v, int gradientIndex, float radiusDp) {
        Context c = v.getContext();
        final float r = Soma.dp(c, radiusDp);
        v.setBackground(pastelDrawable(c, gradientIndex, radiusDp));
        clip(v, r);
        v.setElevation(Soma.dp(c, 3));
        if (Build.VERSION.SDK_INT >= 28) {
            int sh = Bento.shadowOn(gradientIndex);
            v.setOutlineAmbientShadowColor(sh);
            v.setOutlineSpotShadowColor(sh);
        }
    }

    public static Drawable pastelDrawable(Context c, int gradientIndex, float radiusDp) {
        final float r = Soma.dp(c, radiusDp);
        int[] g = Bento.gradient(gradientIndex);

        GradientDrawable body = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{ g[0], g[1], g[2] });
        body.setCornerRadius(r);

        GradientDrawable sheen = new GradientDrawable();
        sheen.setShape(GradientDrawable.RECTANGLE);
        sheen.setCornerRadius(r);
        sheen.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        sheen.setColors(new int[]{ 0x3DFFFFFF, 0x0FFFFFFF, 0x00FFFFFF });
        sheen.setGradientCenter(0.18f, 0.04f);
        sheen.setGradientRadius(Soma.dp(c, 220));

        GradientDrawable rim = new GradientDrawable();
        rim.setColor(0x00000000);
        rim.setCornerRadius(r);
        rim.setStroke(Math.round(Soma.dp(c, 1)), 0x33FFFFFF);

        return new LayerDrawable(new Drawable[]{ body, sheen, rim });
    }

    private static void clip(View v, final float r) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        v.setClipToOutline(true);
    }

    /* ---------------------------------------------------------------- text */

    /** A bold section header, matching the bento screens. */
    public static TextView header(Context c, Soma s, CharSequence text) {
        TextView h = new TextView(c);
        h.setText(text);
        h.setTypeface(Soma.display(c), Typeface.BOLD);
        h.setTextSize(19);
        h.setTextColor(s.ink);
        h.setLetterSpacing(-0.01f);
        int p = Math.round(Soma.dp(c, 4));
        h.setPadding(p, Math.round(Soma.dp(c, 20)), p, Math.round(Soma.dp(c, 10)));
        return h;
    }

    /** A small caps label. */
    public static TextView kicker(Context c, Soma s, CharSequence text) {
        TextView k = new TextView(c);
        k.setText(text);
        k.setAllCaps(true);
        k.setTypeface(Soma.body(c), Typeface.BOLD);
        k.setTextSize(11);
        k.setLetterSpacing(0.12f);
        k.setTextColor(s.inkMute);
        return k;
    }

    /* ---------------------------------------------------------------- rows */

    public static LinearLayout box(Context c, Soma s) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        surface(l, s, 18f, 1.5f);
        int p = Math.round(Soma.dp(c, 4));
        l.setPadding(Math.round(Soma.dp(c, 16)), p, Math.round(Soma.dp(c, 16)), p);
        return l;
    }

    /** Adds a key/value row (with a divider above it if the box is non-empty). */
    public static void addRow(LinearLayout box, Soma s, String key, String value) {
        if (value == null || value.isEmpty()) return;
        Context c = box.getContext();
        if (box.getChildCount() > 0) {
            View div = new View(c);
            div.setBackgroundColor(s.hairline);
            box.addView(div, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int vp = Math.round(Soma.dp(c, 11));
        row.setPadding(0, vp, 0, vp);

        TextView k = new TextView(c);
        k.setText(key);
        k.setTypeface(Soma.body(c));
        k.setTextSize(13);
        k.setTextColor(s.inkMute);
        row.addView(k, new LinearLayout.LayoutParams(Math.round(Soma.dp(c, 104)),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView v = new TextView(c);
        v.setText(value);
        v.setTypeface(Soma.body(c), Typeface.BOLD);
        v.setTextSize(13);
        v.setTextColor(s.ink);
        row.addView(v, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        box.addView(row);
    }
}
