package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.res.ResourcesCompat;

import com.absolute.floral.R;

import java.util.Calendar;

/**
 * Soma — Floral's design language. A quiet, gallery-first surface: a warm paper
 * (or deep charcoal) ground carrying a faint time-of-day tint, frosted cards with
 * a hairline edge, Fraunces for display and Poppins Medium for everything else.
 */
public final class Soma {

    public static final int BASE_LIGHT = 0;
    public static final int BASE_DARK  = 1;
    public static final int BASE_OLED  = 2;

    /** Card corner radius, dp. */
    public static final float RADIUS = 22f;
    /** Hairline stroke, dp. */
    public static final float HAIRLINE = 1f;

    public final int[] ground;      // 3-stop full-screen gradient
    public final int surface;       // frosted card fill
    public final int surfaceStrong; // hero / emphasised card fill
    public final int hairline;      // card edge
    public final int ink;           // primary text
    public final int inkSoft;       // secondary text
    public final int inkMute;       // labels / captions
    public final int accent;        // time-of-day accent
    public final int accentSoft;    // accent wash
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

    /** Period accent — dawn coral, midday sky, golden dusk, deep-night indigo. */
    public static int accentForHour(int hour) {
        if (hour < 6)  return 0xFF7C83C7;   // night — muted indigo
        if (hour < 10) return 0xFFE99274;   // dawn — coral
        if (hour < 16) return 0xFF6FA8C7;   // day — soft sky
        if (hour < 20) return 0xFFE0A25C;   // dusk — amber
        return 0xFF8A7FB8;                  // evening — dusky violet
    }

    public static Soma forHour(Context ctx, int base, int hour) {
        int accent = accentForHour(hour);
        int accentSoft = (accent & 0x00FFFFFF) | 0x1F000000;

        if (base == BASE_LIGHT) {
            int tintTop = blend(0xFFF7F3EC, accent, 0.05f);
            int[] g = { tintTop, 0xFFF1EBE1, 0xFFEAE3D6 };
            return new Soma(g,
                    0xF2FFFFFF,          // surface — near-opaque paper white
                    0xFFFFFFFF,
                    0x14000000,          // hairline
                    0xFF1E1B17,          // ink
                    0xB0231F1A,          // inkSoft
                    0x73231F1A,          // inkMute
                    accent, accentSoft,
                    0x66000000, true);
        }
        if (base == BASE_OLED) {
            int[] g = { 0xFF0B0A09, 0xFF060606, 0xFF000000 };
            return new Soma(g,
                    0x14FFFFFF, 0x1FFFFFFF, 0x1FFFFFFF,
                    0xFFF4EFE7, 0xC7FFFFFF, 0x8AFFFFFF,
                    accent, (accent & 0x00FFFFFF) | 0x24000000,
                    0x8A000000, false);
        }
        // dark — warm charcoal, faint accent tint at the top
        int tintTop = blend(0xFF17130F, accent, 0.10f);
        int[] g = { tintTop, 0xFF121110, 0xFF0C0B0A };
        return new Soma(g,
                0x1AFFFFFF, 0x24FFFFFF, 0x24FFFFFF,
                0xFFF6F1E9, 0xC7FFFFFF, 0x8AFFFFFF,
                accent, (accent & 0x00FFFFFF) | 0x2E000000,
                0x8A000000, false);
    }

    /* ----- fonts ----- */

    public static Typeface display(Context c) {  // Fraunces SemiBold
        return ResourcesCompat.getFont(c, R.font.fraunces_semibold);
    }

    public static Typeface serif(Context c) {    // Fraunces Regular
        return ResourcesCompat.getFont(c, R.font.fraunces_regular);
    }

    public static Typeface body(Context c) {     // Poppins Medium
        return ResourcesCompat.getFont(c, R.font.poppins_medium);
    }

    public static Typeface bodyRegular(Context c) {
        return ResourcesCompat.getFont(c, R.font.poppins_regular);
    }

    /* ----- drawables ----- */

    /** Full-screen ground gradient. */
    public GradientDrawable groundDrawable() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, ground);
        d.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return d;
    }

    /** A frosted card background at the given dp radius. */
    public GradientDrawable card(Context c, boolean strong, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(strong ? surfaceStrong : surface);
        d.setCornerRadius(dp(c, radiusDp));
        d.setStroke(Math.max(1, Math.round(dp(c, HAIRLINE))), hairline);
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
