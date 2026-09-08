package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The floating tab pill — a translucent rounded bar, docked bottom-left, with
 * <b>Home · Library · Collections</b>. The active tab gets a filled accent chip.
 * Search lives in its own circular button ({@link SearchFab}).
 */
public class PhotoNav extends LinearLayout {

    public interface OnTab { void onTab(int index); }

    public static final int HOME = 0, LIBRARY = 1, COLLECTIONS = 2;
    private static final String[] LABELS = { "Home", "Library", "Collections" };

    private final TextView[] tabs = new TextView[LABELS.length];
    private int current = 0;
    private OnTab listener;
    private Soma soma;

    public PhotoNav(Context c) { super(c); init(); }
    public PhotoNav(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(5);
        setPadding(pad, pad, pad, pad);
        for (int i = 0; i < LABELS.length; i++) {
            final int idx = i;
            TextView t = new TextView(getContext());
            t.setText(LABELS[i]);
            t.setTypeface(Soma.body(getContext()), android.graphics.Typeface.BOLD);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(dp(15), dp(9), dp(15), dp(9));
            t.setOnClickListener(v -> select(idx, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(2), 0, dp(2), 0);
            addView(t, lp);
            tabs[i] = t;
        }
    }

    public void setSoma(Soma s) {
        this.soma = s;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.lightBase ? 0xF2FFFFFF : 0xF21E1F20);
        bg.setCornerRadius(dp(26));
        bg.setStroke(Math.round(Soma.dp(getContext(), 1)), s.lightBase ? 0x14000000 : 0x1FFFFFFF);
        setBackground(bg);
        setElevation(dp(8));
        setClipToOutline(true);
        restyle();
    }

    public void setOnTab(OnTab l) { this.listener = l; }
    public int current() { return current; }

    public void select(int idx, boolean notify) {
        current = idx;
        restyle();
        if (notify && listener != null) listener.onTab(idx);
    }

    private void restyle() {
        if (soma == null) return;
        int active = getResources().getColor(com.absolute.floral.R.color.ios_blue);
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == current;
            tabs[i].setTextColor(on ? 0xFFFFFFFF : soma.inkMute);
            GradientDrawable chip = new GradientDrawable();
            chip.setCornerRadius(dp(20));
            chip.setColor(on ? active : 0x00000000);
            tabs[i].setBackground(chip);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
