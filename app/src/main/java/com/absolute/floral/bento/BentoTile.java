package com.absolute.floral.bento;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.bumptech.glide.Glide;

/** One colourful bento cell — gradient (or photo) background, a label, a big value / icon. */
public class BentoTile extends FrameLayout {

    private int gi;
    private final Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float rad;
    private boolean photoMode;

    private final ImageView cover;
    private final View scrim;
    private final TextView label, value, sub;
    private final ImageView icon;

    public BentoTile(Context c) {
        super(c);
        setWillNotDraw(false);
        rad = Soma.dp(c, Bento.RADIUS);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad);
            }
        });
        setClipToOutline(true);

        cover = new ImageView(c);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(cover, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        scrim = new View(c);
        addView(scrim, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        icon = new ImageView(c);
        int is = (int) Soma.dp(c, 22);
        LayoutParams ilp = new LayoutParams(is, is);
        ilp.gravity = Gravity.TOP | Gravity.START;
        ilp.setMargins((int) Soma.dp(c, 14), (int) Soma.dp(c, 14), 0, 0);
        addView(icon, ilp);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = (int) Soma.dp(c, 13);
        col.setPadding(p, p, p, p);
        LayoutParams clp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        addView(col, clp);

        value = new TextView(c);
        value.setTypeface(Soma.display(c), Typeface.BOLD);
        value.setTextSize(23);
        value.setMaxLines(1);
        value.setIncludeFontPadding(false);
        col.addView(value);

        label = new TextView(c);
        label.setTypeface(Soma.body(c), Typeface.BOLD);
        label.setTextSize(13);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(label);

        sub = new TextView(c);
        sub.setTypeface(Soma.body(c));
        sub.setTextSize(11);
        sub.setMaxLines(1);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(sub);
    }

    public BentoTile gradient(int index) {
        this.gi = index;
        this.photoMode = false;
        cover.setImageDrawable(null);
        scrim.setBackground(null);
        int ink = Bento.inkOn(index), subInk = Bento.subInkOn(index);
        value.setTextColor(ink);
        label.setTextColor(ink);
        sub.setTextColor(subInk);
        icon.setColorFilter(ink);
        invalidate();
        return this;
    }

    public BentoTile photo(Object uriOrPath, int accentIndex) {
        this.gi = accentIndex;
        this.photoMode = true;
        if (uriOrPath != null) Glide.with(getContext()).load(uriOrPath).centerCrop().into(cover);
        android.graphics.drawable.GradientDrawable s = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0x0A000000, 0x00000000, 0x00000000, 0x82000000 });
        scrim.setBackground(s);
        value.setTextColor(0xFFFFFFFF);
        label.setTextColor(0xFFFFFFFF);
        sub.setTextColor(0xCCFFFFFF);
        icon.setColorFilter(0xFFFFFFFF);
        value.setShadowLayer(Soma.dp(getContext(), 6), 0, Soma.dp(getContext(), 1), 0x66000000);
        label.setShadowLayer(Soma.dp(getContext(), 6), 0, Soma.dp(getContext(), 1), 0x66000000);
        invalidate();
        return this;
    }

    public BentoTile label(String s) { label.setText(s); label.setVisibility(s == null ? GONE : VISIBLE); return this; }
    public BentoTile sub(String s) { sub.setText(s); sub.setVisibility(s == null || s.isEmpty() ? GONE : VISIBLE); return this; }

    public BentoTile value(String s) {
        value.setText(s);
        value.setVisibility(s == null || s.isEmpty() ? GONE : VISIBLE);
        return this;
    }

    /** Count up to a number on entry. */
    public BentoTile countTo(final int target, final String suffix) {
        value.setVisibility(VISIBLE);
        value.setText("0" + (suffix == null ? "" : suffix));
        ValueAnimator va = ValueAnimator.ofInt(0, target);
        va.setDuration(900);
        va.setStartDelay(120);
        va.setInterpolator(Anim.ease());
        va.addUpdateListener(a -> value.setText(a.getAnimatedValue() + (suffix == null ? "" : suffix)));
        va.start();
        return this;
    }

    public BentoTile icon(int res) {
        if (res == 0) { icon.setVisibility(GONE); }
        else { icon.setImageResource(res); icon.setVisibility(VISIBLE); }
        return this;
    }

    @Override protected void onDraw(Canvas canvas) {
        if (photoMode) return;
        float inset = Soma.dp(getContext(), 0.5f);
        r.set(inset, inset, getWidth() - inset, getHeight() - inset);
        int[] g = Bento.gradient(gi);
        gp.setStyle(Paint.Style.FILL);
        gp.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), g[0], g[1], Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, rad, rad, gp);
        gp.setShader(null);
        gp.setStyle(Paint.Style.STROKE);
        gp.setStrokeWidth(Soma.dp(getContext(), 1f));
        gp.setColor(0x12000000);
        canvas.drawRoundRect(r, rad, rad, gp);
    }
}
