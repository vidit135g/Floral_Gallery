package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The Google-Photos-style floating bottom nav — a white (or dark) pill with
 * Photos / Collections / Create; the active tab gets a filled indicator.
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
        int pad = dp(6);
        setPadding(pad, pad, pad, pad);
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = new TextView(getContext());
            t.setText(LABELS[i]);
            t.setTypeface(Soma.body(getContext()));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(dp(18), dp(10), dp(18), dp(10));
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
        bg.setColor(s.lightBase ? 0xFFFFFFFF : 0xFF2D2F31);
        bg.setCornerRadius(dp(26));
        setBackground(bg);
        setElevation(dp(6));
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
        for (int i = 0; i < 3; i++) {
            boolean on = i == current;
            tabs[i].setTextColor(on ? (soma.lightBase ? soma.accent : 0xFF1F1F1F) : soma.inkMute);
            GradientDrawable chip = new GradientDrawable();
            chip.setCornerRadius(dp(20));
            chip.setColor(on ? soma.accentSoft : 0x00000000);
            tabs[i].setBackground(chip);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
