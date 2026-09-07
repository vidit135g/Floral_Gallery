package com.absolute.floral.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** A FrameLayout that is always as tall as it is wide. */
public class SquareFrameLayout extends FrameLayout {
    public SquareFrameLayout(Context c) { super(c); }
    public SquareFrameLayout(Context c, AttributeSet a) { super(c, a); }
    public SquareFrameLayout(Context c, AttributeSet a, int s) { super(c, a, s); }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
