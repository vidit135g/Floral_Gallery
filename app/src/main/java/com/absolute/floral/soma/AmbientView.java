package com.absolute.floral.soma;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The full-screen Soma ground: a 3-stop diagonal gradient with two very faint,
 * slowly drifting accent blooms. Deliberately quiet so photographs stay the
 * loudest thing on screen. Cross-fades when the palette changes.
 */
public class AmbientView extends View {

    private Soma soma;
    private int[] from, to;
    private float mix = 1f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private ValueAnimator drift, fade;

    public AmbientView(Context c) { super(c); init(); }
    public AmbientView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        drift = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        drift.setDuration(38000);
        drift.setRepeatCount(ValueAnimator.INFINITE);
        drift.setInterpolator(new LinearInterpolator());
        drift.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
    }

    public void setSoma(Soma s) {
        if (soma == null) {
            soma = s; from = s.ground; to = s.ground; mix = 1f;
            invalidate();
            return;
        }
        from = (from != null && to != null) ? blendStops(from, to, mix) : soma.ground;
        to = s.ground;
        soma = s;
        if (fade != null) fade.cancel();
        fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(900);
        fade.setInterpolator(Anim.ease());
        fade.addUpdateListener(a -> { mix = (float) a.getAnimatedValue(); invalidate(); });
        fade.start();
    }

    private int[] blendStops(int[] a, int[] b, float t) {
        int[] r = new int[3];
        for (int i = 0; i < 3; i++) r[i] = Soma.blend(a[i], b[i], t);
        return r;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (drift != null && !drift.isStarted()) drift.start();
    }
    @Override protected void onDetachedFromWindow() {
        if (drift != null) drift.cancel();
        if (fade != null) fade.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (soma == null) return;
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        int[] stops = (from != null && to != null) ? blendStops(from, to, mix) : soma.ground;

        paint.setShader(new LinearGradient(0, 0, w, h, stops, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        int bloom = (soma.accent & 0x00FFFFFF) | (soma.lightBase ? 0x14000000 : 0x1E000000);
        float r1 = Math.max(w, h) * 0.9f;
        float cx1 = w * (0.2f + 0.12f * (float) Math.sin(phase));
        float cy1 = h * (0.16f + 0.06f * (float) Math.cos(phase * 0.7f));
        paint.setShader(new RadialGradient(cx1, cy1, r1,
                new int[]{ bloom, bloom & 0x00FFFFFF }, new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        float cx2 = w * (0.85f + 0.1f * (float) Math.cos(phase * 0.9f));
        float cy2 = h * (0.8f + 0.05f * (float) Math.sin(phase));
        paint.setShader(new RadialGradient(cx2, cy2, r1 * 0.8f,
                new int[]{ bloom, bloom & 0x00FFFFFF }, new float[]{ 0f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        paint.setShader(null);
    }
}
