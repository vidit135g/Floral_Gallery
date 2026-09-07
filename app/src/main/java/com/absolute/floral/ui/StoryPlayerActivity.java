package com.absolute.floral.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.bento.Bento;
import com.absolute.floral.data.Story;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.bumptech.glide.Glide;

import java.util.List;

/** Full-screen, auto-advancing "featured story" player — segmented progress, Ken Burns, tap to skip. */
public class StoryPlayerActivity extends AppCompatActivity {

    public static List<Story> STORIES;
    public static int START_INDEX = 0;

    private static final long SEGMENT_MS = 3600;

    private int storyIdx, itemIdx;
    private ImageView image;
    private LinearLayout bars;
    private TextView kicker, title, date;
    private ValueAnimator segAnim, kenBurns;
    private boolean paused;

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (STORIES == null || STORIES.isEmpty()) { finish(); return; }
        storyIdx = Math.max(0, Math.min(START_INDEX, STORIES.size() - 1));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View topScrim = new View(this);
        topScrim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0xB3000000, 0x00000000 }));
        root.addView(topScrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, d(140)));
        View botScrim = new View(this);
        botScrim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{ 0xCC000000, 0x00000000 }));
        FrameLayout.LayoutParams bs = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, d(220));
        bs.gravity = Gravity.BOTTOM;
        root.addView(botScrim, bs);

        bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(d(10), d(44), d(10), 0);
        root.addView(bars, blp);

        LinearLayout caption = new LinearLayout(this);
        caption.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cap = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cap.gravity = Gravity.BOTTOM;
        cap.setMargins(d(20), 0, d(20), d(40));
        root.addView(caption, cap);

        kicker = new TextView(this);
        kicker.setTextColor(0xCCFFFFFF);
        kicker.setTextSize(11);
        kicker.setLetterSpacing(0.16f);
        kicker.setTypeface(Soma.body(this), android.graphics.Typeface.BOLD);
        caption.addView(kicker);
        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setShadowLayer(d(8), 0, d(2), 0x80000000);
        caption.addView(title);
        date = new TextView(this);
        date.setTextColor(0xB3FFFFFF);
        date.setTextSize(13);
        date.setTypeface(Soma.body(this));
        caption.addView(date);

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setPadding(d(16), d(16), d(16), d(16));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.END | Gravity.TOP;
        clp.setMargins(0, d(28), 0, 0);
        close.setOnClickListener(v -> finish());
        root.addView(close, clp);

        TextView film = new TextView(this);
        film.setText("✨  Save as film");
        film.setTextColor(Color.WHITE);
        film.setTextSize(13);
        film.setTypeface(Soma.body(this), android.graphics.Typeface.BOLD);
        film.setPadding(d(16), d(9), d(16), d(9));
        android.graphics.drawable.GradientDrawable fpill = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ 0xFFF14E8C, 0xFF9B3CC7 });
        fpill.setCornerRadius(d(100));
        film.setBackground(fpill);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.gravity = Gravity.START | Gravity.TOP;
        flp.setMargins(d(18), d(30), 0, 0);
        film.setOnClickListener(v -> {
            Story cur = STORIES.get(storyIdx);
            HighlightActivity.AUTO_ITEMS = new java.util.ArrayList<>(cur.items);
            HighlightActivity.AUTO_TITLE = cur.title;
            startActivity(new android.content.Intent(this, HighlightActivity.class));
        });
        root.addView(film, flp);

        final GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) {
                if (e.getX() > getResources().getDisplayMetrics().widthPixels * 0.35f) next();
                else prev();
                return true;
            }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (vy > 1500 && e2.getY() - e1.getY() > d(80)) { finish(); return true; }
                return false;
            }
        });
        root.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) setPaused(true);
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) setPaused(false);
            return gd.onTouchEvent(e);
        });

        loadStory(storyIdx);
    }

    private void loadStory(int idx) {
        storyIdx = idx;
        itemIdx = 0;
        Story s = STORIES.get(idx);
        kicker.setText(s.kicker);
        title.setText(s.title);
        bars.removeAllViews();
        for (int i = 0; i < s.items.size(); i++) {
            FrameLayout track = new FrameLayout(this);
            android.graphics.drawable.GradientDrawable tb = new android.graphics.drawable.GradientDrawable();
            tb.setColor(0x40FFFFFF); tb.setCornerRadius(d(2));
            track.setBackground(tb);
            View fill = new View(this);
            fill.setId(1000 + i);
            android.graphics.drawable.GradientDrawable fb = new android.graphics.drawable.GradientDrawable();
            fb.setColor(0xFFFFFFFF); fb.setCornerRadius(d(2));
            fill.setBackground(fb);
            fill.setScaleX(0f);
            fill.setPivotX(0f);
            track.addView(fill, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, d(3), 1f);
            lp.setMargins(d(2), 0, d(2), 0);
            bars.addView(track, lp);
        }
        showItem(0);
    }

    private void showItem(int i) {
        Story s = STORIES.get(storyIdx);
        if (i < 0) { prevStory(); return; }
        if (i >= s.items.size()) { nextStory(); return; }
        itemIdx = i;
        for (int k = 0; k < s.items.size(); k++) {
            View fill = bars.findViewById(1000 + k);
            if (fill != null) fill.setScaleX(k < i ? 1f : 0f);
        }
        AlbumItem it = s.items.get(i);
        Object u = it.getUri(this);
        Glide.with(this).load(u != null ? u : it.getPath()).centerCrop().into(image);
        long d = it.getDate();
        date.setText(d > 0 ? android.text.format.DateFormat.format("d MMMM yyyy", d) : "");

        if (kenBurns != null) kenBurns.cancel();
        image.setScaleX(1f); image.setScaleY(1f);
        kenBurns = ValueAnimator.ofFloat(1f, 1.12f);
        kenBurns.setDuration(SEGMENT_MS + 400);
        kenBurns.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            image.setScaleX(v); image.setScaleY(v);
        });
        kenBurns.start();

        final View fill = bars.findViewById(1000 + i);
        if (segAnim != null) segAnim.cancel();
        segAnim = ValueAnimator.ofFloat(0f, 1f);
        segAnim.setDuration(SEGMENT_MS);
        segAnim.setInterpolator(new android.view.animation.LinearInterpolator());
        segAnim.addUpdateListener(a -> { if (fill != null) fill.setScaleX((float) a.getAnimatedValue()); });
        final boolean[] cancelled = { false };
        segAnim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator a) { cancelled[0] = true; }
            @Override public void onAnimationEnd(Animator a) { if (!cancelled[0]) next(); }
        });
        segAnim.start();
        Anim.enter(image, 0);
    }

    private void setPaused(boolean p) {
        if (paused == p) return;
        paused = p;
        if (segAnim == null) return;
        if (p) { segAnim.pause(); if (kenBurns != null) kenBurns.pause(); }
        else { segAnim.resume(); if (kenBurns != null) kenBurns.resume(); }
    }

    private void next() { showItem(itemIdx + 1); }
    private void prev() { showItem(itemIdx - 1); }

    private void nextStory() {
        if (storyIdx + 1 < STORIES.size()) loadStory(storyIdx + 1);
        else finish();
    }
    private void prevStory() {
        if (storyIdx - 1 >= 0) { loadStory(storyIdx - 1); showItem(STORIES.get(storyIdx).items.size() - 1); }
        else showItem(0);
    }

    @Override protected void onPause() {
        super.onPause();
        setPaused(true);
    }
    @Override protected void onResume() {
        super.onResume();
        setPaused(false);
    }
    @Override protected void onDestroy() {
        if (segAnim != null) segAnim.cancel();
        if (kenBurns != null) kenBurns.cancel();
        super.onDestroy();
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
