package com.absolute.floral.bento;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;

/** A small animated bar chart — photos per month. */
public class BarChartView extends View {

    private int[] values = new int[0];
    private String[] labels = new String[0];
    private float t = 0f;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context c) {
        super(c);
        txt.setColor(0xFF5F6368);
        txt.setTextSize(Soma.dp(c, 10));
        txt.setTypeface(Soma.body(c));
        txt.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        t = 0f;
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1100);
        va.setInterpolator(Anim.ease());
        va.addUpdateListener(a -> { t = (float) a.getAnimatedValue(); invalidate(); });
        va.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (values.length == 0) return;
        int w = getWidth(), h = getHeight();
        float labelH = Soma.dp(getContext(), 16);
        float chartH = h - labelH;
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        int n = values.length;
        float gap = Soma.dp(getContext(), 5);
        float bw = (w - gap * (n - 1)) / n;
        float rad = Math.min(bw / 2f, Soma.dp(getContext(), 6));
        for (int i = 0; i < n; i++) {
            float bh = chartH * (values[i] / (float) max) * t;
            float left = i * (bw + gap);
            float top = chartH - bh;
            bar.setShader(new LinearGradient(0, top, 0, chartH,
                    0xFF4FACFE, 0xFF9F7BFF, Shader.TileMode.CLAMP));
            RectF r = new RectF(left, Math.min(top, chartH - 1), left + bw, chartH);
            canvas.drawRoundRect(r, rad, rad, bar);
            if (labels.length == n && (n <= 12))
                canvas.drawText(labels[i], left + bw / 2f, h - Soma.dp(getContext(), 3), txt);
        }
    }
}
