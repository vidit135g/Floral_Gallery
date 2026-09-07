package com.absolute.floral.soma;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Recolours a subtree's text to the active Soma palette — larger text takes the
 * primary ink, body text steps down, captions/labels step down again.
 */
public final class SomaTint {

    private SomaTint() {}

    public static void apply(View root, Soma s) {
        if (root == null || s == null) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            float sp = tv.getTextSize() / tv.getResources().getDisplayMetrics().scaledDensity;
            int c = sp <= 11.5f ? s.inkMute : (sp <= 14.5f ? s.inkSoft : s.ink);
            tv.setTextColor(c);
            tv.setHintTextColor(s.inkMute);
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i), s);
        }
    }

    /** Force every TextView in a subtree white — for copy laid over a photo. */
    public static void onPhoto(View root) {
        if (root == null) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            float sp = tv.getTextSize() / tv.getResources().getDisplayMetrics().scaledDensity;
            tv.setTextColor(sp <= 12f ? 0xCCFFFFFF : 0xFFFFFFFF);
            float d = tv.getResources().getDisplayMetrics().density;
            tv.setShadowLayer(d * 8f, 0f, d * 1.5f, 0x59000000);
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) onPhoto(g.getChildAt(i));
        }
    }
}
