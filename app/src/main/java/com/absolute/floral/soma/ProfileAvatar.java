package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import com.absolute.floral.data.Settings;

/**
 * A letter-based profile chip — the initials of the profile name on a single-hue
 * gradient disc, the hue derived from the name so it's stable but personal.
 * Same idea as the Soma avatar in the other Absolute apps.
 */
public class ProfileAvatar extends TextView {

    public ProfileAvatar(Context c) { super(c); init(); }
    public ProfileAvatar(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setGravity(Gravity.CENTER);
        setTextColor(0xFFFFFFFF);
        setTypeface(Soma.display(getContext()), android.graphics.Typeface.BOLD);
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.5f);
        setLetterSpacing(0.02f);
        setIncludeFontPadding(false);
        refresh();
    }

    public void refresh() {
        String name = Settings.getInstance(getContext()).getProfileName(getContext());
        setText(initials(name));
        int[] g = hueGradient(name);
        GradientDrawable disc = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, g);
        disc.setShape(GradientDrawable.OVAL);
        setBackground(disc);
        setClipToOutline(true);
        setElevation(Soma.dp(getContext(), 1));
    }

    public static String initials(String name) {
        if (name == null) return "F";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.length() == 0 ? "F" : sb.toString();
    }

    public static int[] hueGradient(String name) {
        int h = (name == null ? 0 : name.hashCode());
        float hue = ((h % 360) + 360) % 360;
        float[] a = { hue, 0.62f, 0.68f };
        float[] b = { (hue + 26f) % 360f, 0.66f, 0.52f };
        return new int[]{ android.graphics.Color.HSVToColor(a),
                android.graphics.Color.HSVToColor(b) };
    }
}
