package com.absolute.floral.bento;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.bumptech.glide.Glide;

/** One colourful bento cell — dimensional gradient (or photo) ground, a soft sheen, a label + big value / icon. */
public class BentoTile extends FrameLayout {

    private int gi;
    private final Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float rad;
    private boolean photoMode;
    private boolean pressAnim = true;

    private final ImageView cover;
    private final View scrim;
    private final TextView label, value, sub;
    private final ImageView icon;
    private final FrameLayout iconWrap;

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
        setElevation(Soma.dp(c, 3));

        cover = new ImageView(c);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(cover, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        scrim = new View(c);
        addView(scrim, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        iconWrap = new FrameLayout(c);
        int chip = (int) Soma.dp(c, 34);
        LayoutParams wlp = new LayoutParams(chip, chip);
        wlp.gravity = Gravity.TOP | Gravity.START;
        wlp.setMargins((int) Soma.dp(c, 13), (int) Soma.dp(c, 13), 0, 0);
        addView(iconWrap, wlp);

        icon = new ImageView(c);
        int is = (int) Soma.dp(c, 19);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(is, is);
        ilp.gravity = Gravity.CENTER;
        iconWrap.addView(icon, ilp);
        iconWrap.setVisibility(GONE);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = (int) Soma.dp(c, 15);
        col.setPadding(p, p, p, p);
        LayoutParams clp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        addView(col, clp);

        value = new TextView(c);
        value.setTypeface(Soma.display(c), Typeface.BOLD);
        value.setTextSize(26);
        value.setMaxLines(1);
        value.setLetterSpacing(-0.01f);
        value.setIncludeFontPadding(false);
        col.addView(value);

        label = new TextView(c);
        label.setTypeface(Soma.body(c), Typeface.BOLD);
        label.setTextSize(13.5f);
        label.setMaxLines(1);
        label.setLetterSpacing(0.005f);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = (int) Soma.dp(c, 1);
        col.addView(label, llp);

        sub = new TextView(c);
        sub.setTypeface(Soma.body(c));
        sub.setTextSize(11);
        sub.setMaxLines(1);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(sub);

        stroke.setStyle(Paint.Style.STROKE);
    }

    /** Turn off the tap-to-shrink feedback (for non-interactive tiles). */
    public BentoTile flat() { pressAnim = false; return this; }

    public BentoTile gradient(int index) {
        this.gi = index;
        this.photoMode = false;
        cover.setImageDrawable(null);
        scrim.setBackground(null);
        int ink = Bento.inkOn(index), subInk = Bento.subInkOn(index);
        value.setTextColor(ink);
        label.setTextColor(ink);
        sub.setTextColor(subInk);
        value.setShadowLayer(0, 0, 0, 0);
        label.setShadowLayer(0, 0, 0, 0);
        icon.setColorFilter(ink);
        iconWrap.setBackground(chipBg(Bento.chipOn(index)));
        applyShadowTint(Bento.shadowOn(index));
        invalidate();
        return this;
    }

    public BentoTile photo(Object uriOrPath, int accentIndex) {
        this.gi = accentIndex;
        this.photoMode = true;
        if (uriOrPath != null) Glide.with(getContext()).load(uriOrPath).centerCrop().into(cover);
        android.graphics.drawable.GradientDrawable s = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0x0D000000, 0x00000000, 0x12000000, 0x80000000 });
        scrim.setBackground(s);
        value.setTextColor(0xFFFFFFFF);
        label.setTextColor(0xFFFFFFFF);
        sub.setTextColor(0xE6FFFFFF);
        icon.setColorFilter(0xFFFFFFFF);
        iconWrap.setBackground(chipBg(0x33FFFFFF));
        value.setShadowLayer(Soma.dp(getContext(), 8), 0, Soma.dp(getContext(), 1), 0x73000000);
        label.setShadowLayer(Soma.dp(getContext(), 8), 0, Soma.dp(getContext(), 1), 0x73000000);
        applyShadowTint(0x59000000);
        invalidate();
        return this;
    }

    private android.graphics.drawable.GradientDrawable chipBg(int color) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private void applyShadowTint(int color) {
        if (Build.VERSION.SDK_INT >= 28) {
            setOutlineAmbientShadowColor(color);
            setOutlineSpotShadowColor(color);
        }
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
        va.setDuration(950);
        va.setStartDelay(140);
        va.setInterpolator(Anim.ease());
        va.addUpdateListener(a -> value.setText(a.getAnimatedValue() + (suffix == null ? "" : suffix)));
        va.start();
        return this;
    }

    public BentoTile icon(int res) {
        if (res == 0) { iconWrap.setVisibility(GONE); }
        else { icon.setImageResource(res); iconWrap.setVisibility(VISIBLE); }
        return this;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (pressAnim && isClickable()) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(130).start();
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                    || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                animate().scaleX(1f).scaleY(1f).setInterpolator(new OvershootInterpolator(2.4f))
                        .setDuration(320).start();
            }
        }
        return super.onTouchEvent(e);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (photoMode) return;
        float inset = Soma.dp(getContext(), 0.5f);
        r.set(inset, inset, getWidth() - inset, getHeight() - inset);
        int[] g = Bento.gradient(gi);

        // body: bright top-left -> mid -> saturated bottom-right, on a gentle diagonal
        gp.setStyle(Paint.Style.FILL);
        gp.setShader(new LinearGradient(getWidth() * 0.08f, 0, getWidth() * 0.92f, getHeight(),
                new int[]{ g[0], g[1], g[2] }, new float[]{ 0f, 0.55f, 1f }, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, rad, rad, gp);
        gp.setShader(null);

        // sheen: a soft white glow falling from the upper-left corner
        float gr = Math.max(getWidth(), getHeight()) * 0.95f;
        sheen.setShader(new RadialGradient(getWidth() * 0.16f, getHeight() * 0.02f, gr,
                new int[]{ 0x40FFFFFF, 0x14FFFFFF, 0x00FFFFFF }, new float[]{ 0f, 0.35f, 1f },
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, rad, rad, sheen);

        // rims: bright hairline on top, faint shade on the bottom
        stroke.setStrokeWidth(Soma.dp(getContext(), 1f));
        stroke.setShader(new LinearGradient(0, 0, 0, getHeight(),
                new int[]{ 0x59FFFFFF, 0x0FFFFFFF, 0x14000000 }, new float[]{ 0f, 0.5f, 1f },
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, rad, rad, stroke);
        stroke.setShader(null);
    }
}
