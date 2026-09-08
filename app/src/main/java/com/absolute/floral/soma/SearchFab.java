package com.absolute.floral.soma;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.absolute.floral.R;

/**
 * The standalone circular Search button — translucent disc, docked bottom-right,
 * mirroring the Library / Collections pill. Apple-Photos iOS 18.
 */
public class SearchFab extends ImageView {

    public SearchFab(Context c) { super(c); init(); }
    public SearchFab(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setScaleType(ScaleType.CENTER_INSIDE);
        setImageResource(R.drawable.ic_search_white);
        int p = Math.round(Soma.dp(getContext(), 11));
        setPadding(p, p, p, p);
    }

    public void setSoma(Soma s) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(s.lightBase ? 0xF2FFFFFF : 0xF21E1F20);
        bg.setStroke(Math.round(Soma.dp(getContext(), 1)), s.lightBase ? 0x14000000 : 0x1FFFFFFF);
        setBackground(bg);
        setElevation(Soma.dp(getContext(), 8));
        setClipToOutline(true);
        if (getDrawable() != null) getDrawable().mutate().setTint(s.ink);
    }
}
