package com.absolute.floral.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Apple-Photos style interactive dismiss. When the current page reports it is
 * "at rest" (not zoomed), a vertical drag translates + scales the drag target
 * on the black backdrop; releasing past a threshold (or a downward fling)
 * dismisses, otherwise it springs back. A firm upward drag asks for the info
 * sheet instead.
 */
public class DismissFrameLayout extends FrameLayout {

    public interface Listener {
        /** true when the current page is not zoomed and can be dragged away. */
        boolean canDismiss();
        /** drag progress 0..1 — drive chrome / scrim fade. */
        void onDrag(float progress);
        void onDismiss();
        void onCancelled();
        void onInfoRequested();
    }

    private Listener listener;
    private View target;               // the view that moves (drag_target)

    private final int touchSlop;
    private float downX, downY;
    private boolean dragging;
    private boolean claimed;
    private VelocityTracker vt;
    private float dismissDistance;

    public DismissFrameLayout(Context c) { super(c); touchSlop = ViewConfiguration.get(c).getScaledTouchSlop(); }
    public DismissFrameLayout(Context c, AttributeSet a) { super(c, a); touchSlop = ViewConfiguration.get(c).getScaledTouchSlop(); }

    public void setDismissListener(Listener l) { this.listener = l; }
    public void setTarget(View v) { this.target = v; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        dismissDistance = h * 0.42f;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX(); downY = e.getY();
                dragging = false; claimed = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) return true;
                if (e.getPointerCount() > 1) return false;
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx) * 1.4f
                        && listener != null && listener.canDismiss()) {
                    dragging = true; claimed = true;
                    downY = e.getY(); downX = e.getX();
                    vt = VelocityTracker.obtain();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!claimed) return false;
        if (vt != null) vt.addMovement(e);
        float dy = e.getY() - downY;
        float dx = e.getX() - downX;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_MOVE: {
                if (target == null) break;
                float p = Math.min(1f, Math.abs(dy) / dismissDistance);
                target.setTranslationY(dy);
                target.setTranslationX(dx * 0.35f);
                float scale = 1f - 0.16f * p;
                target.setScaleX(scale);
                target.setScaleY(scale);
                target.setAlpha(1f - 0.35f * p);
                if (listener != null) listener.onDrag(p);
                break;
            }
            case MotionEvent.ACTION_UP: {
                float vy = 0f;
                if (vt != null) { vt.computeCurrentVelocity(1000); vy = vt.getYVelocity(); vt.recycle(); vt = null; }
                boolean down = dy > dismissDistance * 0.5f || vy > 2200f;
                boolean up = dy < -dismissDistance * 0.42f || vy < -2400f;
                claimed = false; dragging = false;
                if (down && listener != null) {
                    listener.onDismiss();
                } else if (up && listener != null) {
                    springBack();
                    listener.onInfoRequested();
                } else {
                    springBack();
                    if (listener != null) listener.onCancelled();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
                if (vt != null) { vt.recycle(); vt = null; }
                claimed = false; dragging = false;
                springBack();
                if (listener != null) listener.onCancelled();
                break;
        }
        return true;
    }

    private void springBack() {
        if (target == null) return;
        target.animate().translationX(0f).translationY(0f).scaleX(1f).scaleY(1f).alpha(1f)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .setDuration(240).start();
        if (listener != null) listener.onDrag(0f);
    }
}
