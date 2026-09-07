package com.absolute.floral.bento;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;

/**
 * A vertical stack of horizontal rows of bento tiles. Rows can carry a fixed
 * height or a nested vertical column so tiles of different heights sit together.
 */
public class BentoLayout extends LinearLayout {

    private int gap;
    private int animIndex = 0;

    public BentoLayout(Context c) {
        super(c);
        setOrientation(VERTICAL);
        gap = (int) Soma.dp(c, Bento.GAP);
        int pad = (int) Soma.dp(c, 14);
        setPadding(pad, (int) Soma.dp(c, 6), pad, (int) Soma.dp(c, 6));
    }

    /** A row that is `heightDp` tall; tiles are added with weights. */
    public LinearLayout row(int heightDp) {
        LinearLayout r = new LinearLayout(getContext());
        r.setOrientation(HORIZONTAL);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, (int) Soma.dp(getContext(), heightDp));
        lp.topMargin = getChildCount() == 0 ? 0 : gap;
        addView(r, lp);
        return r;
    }

    /** A row that hosts a big left tile and a stacked right column. */
    public LinearLayout splitRow(int heightDp) {
        return row(heightDp);
    }

    /** Add a tile to a row with the given weight (relative width). */
    public BentoTile tile(LinearLayout row, float weight, View.OnClickListener onTap) {
        BentoTile t = new BentoTile(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        lp.leftMargin = row.getChildCount() == 0 ? 0 : gap;
        row.addView(t, lp);
        if (onTap != null) {
            t.setClickable(true);
            t.setOnClickListener(onTap);
        }
        prime(t);
        return t;
    }

    /** A vertical column inside a row (for stacking two short tiles beside a tall one). */
    public LinearLayout column(LinearLayout row, float weight) {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        lp.leftMargin = row.getChildCount() == 0 ? 0 : gap;
        row.addView(col, lp);
        return col;
    }

    public BentoTile stacked(LinearLayout column, float heightWeight, View.OnClickListener onTap) {
        BentoTile t = new BentoTile(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, heightWeight);
        lp.topMargin = column.getChildCount() == 0 ? 0 : gap;
        column.addView(t, lp);
        if (onTap != null) { t.setClickable(true); t.setOnClickListener(onTap); }
        prime(t);
        return t;
    }

    private void prime(final BentoTile t) {
        final int idx = animIndex++;
        t.setAlpha(0f);
        t.setScaleX(0.92f);
        t.setScaleY(0.92f);
        t.setTranslationY(Soma.dp(getContext(), 16));
        t.post(() -> t.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setStartDelay(60L + idx * 55L)
                .setDuration(520)
                .setInterpolator(Anim.ease()).start());
    }
}
