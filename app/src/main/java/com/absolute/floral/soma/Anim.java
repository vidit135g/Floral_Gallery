package com.absolute.floral.soma;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

/** Shared motion — a soft, springy "rise and settle" used for entrances across Floral. */
public final class Anim {

    private Anim() {}

    /** easeOutExpo-ish — quick out of the gate, long gentle settle. */
    public static PathInterpolator ease() {
        return new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    }

    /** Fade + rise + a hair of scale, staggered across a container's children. */
    public static void enterChildren(ViewGroup group, long startDelay, long stagger) {
        if (group == null) return;
        float d = group.getResources().getDisplayMetrics().density;
        int shown = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            c.setAlpha(0f);
            c.setTranslationY(30f * d);
            c.setScaleX(0.96f);
            c.setScaleY(0.96f);
            c.animate()
                    .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(startDelay + (long) shown * stagger)
                    .setDuration(580)
                    .setInterpolator(ease())
                    .start();
            shown++;
        }
    }

    public static void enterChildren(ViewGroup group) { enterChildren(group, 40, 46); }

    /** A single view rising into place. */
    public static void enter(View v, long delay) {
        if (v == null) return;
        float d = v.getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(24f * d);
        v.animate().alpha(1f).translationY(0f)
                .setStartDelay(delay).setDuration(520).setInterpolator(ease()).start();
    }

    /** Staggered entrance for RecyclerView items — call from onBindViewHolder. */
    public static void item(View v, int position, int lastAnimated) {
        if (v == null || position <= lastAnimated) return;
        float d = v.getResources().getDisplayMetrics().density;
        v.setAlpha(0f);
        v.setTranslationY(46f * d);
        v.setScaleX(0.94f);
        v.setScaleY(0.94f);
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(Math.min(position, 9) * 34L)
                .setDuration(560).setInterpolator(ease()).start();
    }

    /** Gentle scale pulse — good for a tapped icon or a value that just changed. */
    public static void pulse(View v) {
        if (v == null) return;
        v.animate().scaleX(1.16f).scaleY(1.16f).setDuration(130)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(240).setInterpolator(ease()).start())
                .start();
    }

    /** Press-in feedback for a whole card. */
    public static void press(View v, boolean down) {
        if (v == null) return;
        float s = down ? 0.965f : 1f;
        v.animate().scaleX(s).scaleY(s).setDuration(down ? 120 : 260)
                .setInterpolator(ease()).start();
    }
}
