package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The floating Google-Photos-style bottom nav — a frosted pill with
 * Photos / Collections / Create, drawn in the Soma language.
 */
public class NavPill extends LinearLayout {

    public interface OnTab { void onTab(int index); }

    public static final int PHOTOS = 0, COLLECTIONS = 1, CREATE = 2;
    private static final String[] LABELS = { "Photos", "Collections", "Create" };

    private final TextView[] tabs = new TextView[3];
    private int current = 0;
    private OnTab listener;
    private Soma soma;

    public NavPill(Context c) { super(c); init(); }
    public NavPill(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(6), padV = dp(6);
        setPadding(padH, padV, padH, padV);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = new TextView(getContext());
            t.setText(LABELS[i]);
            t.setTypeface(Soma.body(getContext()));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setMaxLines(1);
            t.setPadding(dp(15), dp(10), dp(15), dp(10));
            t.setOnClickListener(v -> select(idx, true));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(1), 0, dp(1), 0);
            addView(t, lp);
            tabs[i] = t;
        }
    }

    public void setSoma(Soma s) {
        this.soma = s;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.lightBase ? 0xF7FFFFFF : 0xF01D1B19);
        bg.setCornerRadius(dp(28));
        bg.setStroke(Math.max(1, dp(1)), s.hairline);
        setBackground(bg);
        setElevation(dp(10));
        restyle();
    }

    public void setOnTab(OnTab l) { this.listener = l; }
    public int current() { return current; }

    public void select(int idx, boolean notify) {
        current = idx;
        restyle();
        if (notify && listener != null) listener.onTab(idx);
        Anim.pulse(tabs[idx]);
    }

    private void restyle() {
        if (soma == null) return;
        for (int i = 0; i < 3; i++) {
            boolean on = i == current;
            tabs[i].setTextColor(on ? soma.ink : soma.inkMute);
            tabs[i].setTypeface(on ? Soma.display(getContext()) : Soma.body(getContext()));
            GradientDrawable chip = new GradientDrawable();
            chip.setCornerRadius(dp(22));
            chip.setColor(on ? soma.accentSoft : 0x00000000);
            tabs[i].setBackground(chip);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
