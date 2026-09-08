package com.absolute.floral.ui;

import android.view.View;
import android.view.Window;
import android.view.animation.PathInterpolator;

import androidx.core.view.WindowInsetsControllerCompat;

/**
 * One controller for the immersive viewer chrome — the toolbar, both gradient
 * scrims and the bottom bar fade together, and the system bars follow. Replaces
 * the old {@code showUI()} + {@code showSystemUI()} + transition pokes.
 */
public final class ChromeController {

    private final Window window;
    private final View toolbar, scrimTop, scrimBottom, bottomBar;
    private final WindowInsetsControllerCompat insets;
    private final PathInterpolator ease = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private boolean visible = true;

    public ChromeController(Window window, View decor,
                            View toolbar, View scrimTop, View scrimBottom, View bottomBar) {
        this.window = window;
        this.toolbar = toolbar;
        this.scrimTop = scrimTop;
        this.scrimBottom = scrimBottom;
        this.bottomBar = bottomBar;
        this.insets = new WindowInsetsControllerCompat(window, decor);
        insets.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    public boolean isVisible() { return visible; }

    public void toggle() { setVisible(!visible, true); }

    public void setVisible(boolean show, boolean animate) {
        visible = show;
        if (show) insets.show(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        else insets.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());

        float a = show ? 1f : 0f;
        for (View v : new View[]{ toolbar, scrimTop, scrimBottom, bottomBar }) {
            if (v == null) continue;
            if (animate) {
                v.animate().alpha(a).setDuration(190).setInterpolator(ease).start();
            } else {
                v.setAlpha(a);
            }
        }
    }

    /** During an interactive dismiss drag: fade the chrome out proportionally. */
    public void setDragProgress(float p) {
        float a = visible ? (1f - Math.min(1f, p * 2f)) : 0f;
        for (View v : new View[]{ toolbar, scrimTop, scrimBottom, bottomBar }) {
            if (v != null) v.setAlpha(a);
        }
    }

    /** Snap everything hidden immediately (before a dismiss transition). */
    public void hideForExit() {
        for (View v : new View[]{ toolbar, scrimTop, scrimBottom, bottomBar }) {
            if (v != null) v.animate().cancel();
            if (v != null) v.setAlpha(0f);
        }
    }
}
