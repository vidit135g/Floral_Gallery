package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Apple-Photos-style segmented control — a grey track with a white selected pill. */
public class Segmented extends LinearLayout {

    public interface OnChange { void onSegment(int index); }

    private String[] labels = { "Years", "Months", "Days" };
    private final java.util.ArrayList<TextView> tabs = new java.util.ArrayList<>();
    private int selected = 2;
    private OnChange listener;
    private Soma soma;

    public Segmented(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(3);
        setPadding(p, p, p, p);
        build();
    }

    private void build() {
        removeAllViews();
        tabs.clear();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = new TextView(getContext());
            t.setText(labels[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            t.setSingleLine(true);
            t.setPadding(dp(8), dp(7), dp(8), dp(7));
            t.setOnClickListener(v -> select(idx, true));
            addView(t, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            tabs.add(t);
        }
        restyle();
    }

    public void setItems(String... it) { labels = it; selected = it.length - 1; build(); }

    public void setSoma(Soma s) {
        this.soma = s;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(s.lightBase ? 0xFFE9EAEC : 0xFF2A2B2D);
        bg.setCornerRadius(dp(20));
        setBackground(bg);
        restyle();
    }

    public void setOnChange(OnChange l) { this.listener = l; }
    public int selected() { return selected; }

    public void select(int idx, boolean notify) {
        selected = idx;
        restyle();
        if (notify && listener != null) listener.onSegment(idx);
    }

    private void restyle() {
        if (soma == null) return;
        for (int i = 0; i < tabs.size(); i++) {
            TextView t = tabs.get(i);
            boolean on = i == selected;
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(dp(17));
            pill.setColor(on ? (soma.lightBase ? 0xFFFFFFFF : 0xFF4A4B4D) : 0x00000000);
            t.setBackground(pill);
            t.setTextColor(on ? soma.ink : soma.inkMute);
            t.setTypeface(on ? Typeface.create(Soma.body(getContext()), Typeface.BOLD)
                              : Soma.body(getContext()));
            if (on) t.setElevation(dp(2)); else t.setElevation(0);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
