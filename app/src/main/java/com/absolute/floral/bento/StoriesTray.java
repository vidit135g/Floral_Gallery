package com.absolute.floral.bento;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.data.Story;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.StoryPlayerActivity;
import com.bumptech.glide.Glide;

import java.util.List;

/** The Instagram-style featured-stories row — a gradient ring per story. */
public class StoriesTray extends HorizontalScrollView {

    public StoriesTray(final Activity a, final List<Story> stories) {
        super(a);
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        int pad = (int) Soma.dp(a, 12);
        setPadding(pad, (int) Soma.dp(a, 6), pad, (int) Soma.dp(a, 4));

        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row);

        for (int i = 0; i < stories.size(); i++) {
            final int idx = i;
            final Story s = stories.get(i);

            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding((int) Soma.dp(a, 6), 0, (int) Soma.dp(a, 6), 0);

            Ring ring = new Ring(a, Bento.gradient(s.gradient));
            int rs = (int) Soma.dp(a, 68);
            col.addView(ring, new LinearLayout.LayoutParams(rs, rs));

            ImageView cover = ring.cover;
            if (s.cover() != null) {
                Object u = s.cover().getUri(a);
                Glide.with(a).load(u != null ? u : s.cover().getPath()).centerCrop().into(cover);
            }

            TextView lbl = new TextView(a);
            lbl.setText(s.title);
            lbl.setTextSize(11);
            lbl.setMaxLines(2);
            lbl.setEllipsize(android.text.TextUtils.TruncateAt.END);
            lbl.setWidth((int) Soma.dp(a, 78));
            lbl.setGravity(Gravity.CENTER);
            lbl.setTypeface(Soma.body(a));
            com.absolute.floral.soma.Soma s0 = SomaSkin.read(a);
            lbl.setTextColor(s0 != null ? s0.inkSoft : 0xFF444746);
            lbl.setPadding(0, (int) Soma.dp(a, 5), 0, 0);
            col.addView(lbl);

            col.setOnClickListener(v -> {
                StoryPlayerActivity.STORIES = stories;
                StoryPlayerActivity.START_INDEX = idx;
                a.startActivity(new Intent(a, StoryPlayerActivity.class));
            });

            col.setAlpha(0f);
            col.setTranslationX(Soma.dp(a, 16));
            final int fi = i;
            col.postDelayed(() -> col.animate().alpha(1f).translationX(0f)
                    .setDuration(360).setInterpolator(Anim.ease()).start(), 30L + fi * 40L);

            row.addView(col);
        }
    }

    /** A circular photo with a thick gradient ring around it. */
    static class Ring extends FrameLayout {
        final ImageView cover;
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] grad;

        Ring(Activity a, int[] grad) {
            super(a);
            this.grad = grad;
            setWillNotDraw(false);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(Soma.dp(a, 3f));

            cover = new ImageView(a);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setClipToOutline(true);
            cover.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            int inset = (int) Soma.dp(a, 5);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            lp.setMargins(inset, inset, inset, inset);
            addView(cover, lp);
        }

        @Override protected void onDraw(Canvas c) {
            float r = Math.min(getWidth(), getHeight()) / 2f - Soma.dp(getContext(), 2f);
            ringPaint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                    grad[0], grad[1], Shader.TileMode.CLAMP));
            c.drawCircle(getWidth() / 2f, getHeight() / 2f, r, ringPaint);
        }
    }
}
