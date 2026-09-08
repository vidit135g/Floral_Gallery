package com.absolute.floral.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

/**
 * A {@link SubsamplingScaleImageView} that plays nicely inside a horizontal
 * {@code ViewPager}. At (or near) minimum zoom the image has nothing to pan, so
 * a horizontal drag is released back to the pager (swiping between photos always
 * works); taps and vertical (dismiss) drags still reach the image, and once the
 * user zooms in the image keeps every gesture until a pan reaches the edge.
 *
 * <p>The work is in {@link #dispatchTouchEvent}: the view also has an
 * {@code OnTouchListener} for tap detection, so {@code onTouchEvent} alone never
 * sees a clean gesture stream.
 */
public class PagerScaleImageView extends SubsamplingScaleImageView {

    private final int touchSlop;
    private float downX, downY;
    private int decision;   // 0 = undecided, 1 = image keeps it, 2 = pager gets it

    public PagerScaleImageView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public PagerScaleImageView(Context context, AttributeSet attr) {
        super(context, attr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        setPanEnabled(false);
        setOnStateChangedListener(new DefaultOnStateChangedListener() {
            @Override public void onScaleChanged(float newScale, int origin) {
                setPanEnabled(zoomed());
            }
        });
    }

    private boolean zoomed() {
        float min = getMinScale();
        return min > 0f && getScale() > min * 1.05f;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return zoomed() && super.canScrollHorizontally(direction);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                decision = zoomed() ? 1 : 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (decision == 0 && ev.getPointerCount() == 1) {
                    float dx = Math.abs(ev.getX() - downX);
                    float dy = Math.abs(ev.getY() - downY);
                    if (dx > touchSlop && dx > dy * 1.2f) {
                        decision = 2;                       // horizontal -> pager
                        MotionEvent cancel = MotionEvent.obtain(ev);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                    } else if (dy > touchSlop || getScale() > getMinScale() * 1.05f) {
                        decision = 1;                       // vertical / zoom -> image
                    }
                }
                break;
        }

        if (decision == 2) {
            ViewParent p = getParent();
            if (p != null) p.requestDisallowInterceptTouchEvent(false);
            return false;   // let the ViewPager own the rest of this gesture
        }
        return super.dispatchTouchEvent(ev);
    }
}
