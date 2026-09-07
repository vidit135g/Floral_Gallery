package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import java.util.Calendar;

/**
 * Design tokens for Floral. The app follows the Google Photos look — flat white
 * (or true-dark) Material 3 surfaces, Google-Sans-style typography, a blue
 * accent, edge-to-edge photo grids.
 *
 * The class name and public surface are unchanged from the earlier iteration so
 * existing call-sites keep working; only the values are now Material.
 */
public final class Soma {

    public static final int BASE_LIGHT = 0;
    public static final int BASE_DARK  = 1;
    public static final int BASE_OLED  = 2;

    /** Card corner radius, dp (memories cards, album cards). */
    public static final float RADIUS = 16f;
    /** Hairline stroke, dp. */
    public static final float HAIRLINE = 1f;

    public final int[] ground;      // flat window background (3 equal stops)
    public final int surface;       // card / sheet fill
    public final int surfaceStrong; // container fill (chips, search field)
    public final int hairline;      // dividers / card edges
    public final int ink;           // primary text / icons
    public final int inkSoft;       // secondary text
    public final int inkMute;       // tertiary text / captions
    public final int accent;        // primary (Google blue)
    public final int accentSoft;    // selected-state container
    public final int scrim;         // over-photo text protection
    public final boolean lightBase;

    private Soma(int[] ground, int surface, int surfaceStrong, int hairline,
                 int ink, int inkSoft, int inkMute, int accent, int accentSoft,
                 int scrim, boolean lightBase) {
        this.ground = ground;
        this.surface = surface;
        this.surfaceStrong = surfaceStrong;
        this.hairline = hairline;
        this.ink = ink;
        this.inkSoft = inkSoft;
        this.inkMute = inkMute;
        this.accent = accent;
        this.accentSoft = accentSoft;
        this.scrim = scrim;
        this.lightBase = lightBase;
    }

    public static Soma forNow(Context ctx, int base) {
        return forHour(ctx, base, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }

    /** Kept for source compatibility; the palette no longer varies by time. */
    public static int accentForHour(int hour) {
        return 0xFF0B57D0;
    }

    public static Soma forHour(Context ctx, int base, int hour) {
        if (base == BASE_OLED) {
            int[] g = { 0xFF000000, 0xFF000000, 0xFF000000 };
            return new Soma(g,
                    0xFF1A1A1A,   // surface
                    0xFF262626,   // surfaceStrong
                    0xFF2A2A2A,   // hairline
                    0xFFE3E3E3,   // ink
                    0xFFC4C7C5,   // inkSoft
                    0xFF9AA0A6,   // inkMute
                    0xFFA8C7FA,   // accent
                    0xFF0842A0,   // accentSoft
                    0x99000000, false);
        }
        if (base == BASE_DARK) {
            int[] g = { 0xFF131314, 0xFF131314, 0xFF131314 };
            return new Soma(g,
                    0xFF1E1F20, 0xFF2D2F31, 0xFF3C4043,
                    0xFFE3E3E3, 0xFFC4C7C5, 0xFF9AA0A6,
                    0xFFA8C7FA, 0xFF0842A0,
                    0x99000000, false);
        }
        // light — Google Photos default
        int[] g = { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
        return new Soma(g,
                0xFFFFFFFF,   // surface
                0xFFF0F1F3,   // surfaceStrong  (M3 surfaceContainer)
                0xFFE3E3E3,   // hairline
                0xFF1F1F1F,   // ink
                0xFF444746,   // inkSoft
                0xFF5F6368,   // inkMute  (Google grey)
                0xFF0B57D0,   // accent   (M3 primary blue)
                0xFFE8F0FE,   // accentSoft
                0x66000000, true);
    }

    /* ----- fonts -----
       google.ttf (bundled, a Google-Sans-style face) is the app font. It is
       already the global default via CustomFontApp; these helpers hand the same
       face to the few views that set it explicitly. */

    private static Typeface base;

    private static Typeface load(Context c) {
        if (base == null) {
            try {
                base = Typeface.createFromAsset(c.getApplicationContext().getAssets(), "fonts/google.ttf");
            } catch (Exception e) {
                base = Typeface.SANS_SERIF;
            }
        }
        return base;
    }

    /** Titles / headers — a touch heavier. */
    public static Typeface display(Context c) {
        return Typeface.create(load(c), Typeface.NORMAL);
    }

    /** Alias kept for old call-sites (no more serif). */
    public static Typeface serif(Context c) { return load(c); }

    /** Body / labels. */
    public static Typeface body(Context c) { return load(c); }

    public static Typeface bodyRegular(Context c) { return load(c); }

    /* ----- drawables ----- */

    public GradientDrawable groundDrawable() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(ground[0]);
        return d;
    }

    public GradientDrawable card(Context c, boolean strong, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(strong ? surfaceStrong : surface);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public GradientDrawable card(Context c, boolean strong) {
        return card(c, strong, RADIUS);
    }

    /* ----- helpers ----- */

    public static float dp(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    public static int blend(int a, int b, float t) {
        float ia = 1f - t;
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa * ia + ba * t) << 24)
                | (Math.round(ar * ia + br * t) << 16)
                | (Math.round(ag * ia + bg * t) << 8)
                | Math.round(ab * ia + bb * t);
    }

    private Soma() { throw new AssertionError(); }
}
