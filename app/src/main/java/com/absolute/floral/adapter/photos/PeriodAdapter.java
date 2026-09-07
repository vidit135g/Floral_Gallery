package com.absolute.floral.adapter.photos;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.data.Periods;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/** Apple-Photos "Months" / "Years" — big hero cards, tap to drill down. */
public class PeriodAdapter extends RecyclerView.Adapter<PeriodAdapter.VH> {

    public interface OnPeriod { void onPeriod(Periods.Period p); }

    private final Activity a;
    private final boolean months;
    private List<Periods.Period> data = new ArrayList<>();
    private final OnPeriod onPeriod;
    private int lastAnim = -1;

    public PeriodAdapter(Activity a, boolean months, OnPeriod onPeriod) {
        this.a = a; this.months = months; this.onPeriod = onPeriod;
    }

    public void setData(List<Periods.Period> d) { this.data = d == null ? new ArrayList<>() : d; lastAnim = -1; notifyDataSetChanged(); }

    @Override public int getItemCount() { return data.size(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        Soma s = SomaSkin.read(a);
        FrameLayout card = new FrameLayout(a);
        int h = Math.round(a.getResources().getDisplayMetrics().density * (months ? 210 : 168));
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h);
        int m = dp(a, 8);
        lp.setMargins(dp(a, 14), m, dp(a, 14), m);
        card.setLayoutParams(lp);
        card.setClipToOutline(true);
        final float r = dp(a, 22);
        card.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });

        ImageView img = new ImageView(a);
        img.setId(android.R.id.icon);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(s != null ? s.surfaceStrong : 0xFFEEEEEE);
        card.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0x33000000, 0x00000000, 0x00000000, 0xB8000000 }));
        card.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout txt = new LinearLayout(a);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 16));
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.gravity = Gravity.BOTTOM;
        card.addView(txt, tl);

        TextView title = new TextView(a);
        title.setId(android.R.id.text1);
        title.setTypeface(Soma.display(a), Typeface.BOLD);
        title.setTextSize(months ? 26 : 30);
        title.setTextColor(0xFFFFFFFF);
        title.setShadowLayer(dp(a, 8), 0, dp(a, 2), 0x80000000);
        txt.addView(title);

        TextView sub = new TextView(a);
        sub.setId(android.R.id.text2);
        sub.setTypeface(Soma.body(a));
        sub.setTextSize(13);
        sub.setTextColor(0xCCFFFFFF);
        txt.addView(sub);

        return new VH(card, img, title, sub);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Periods.Period p = data.get(position);
        h.title.setText(p.title);
        h.sub.setText(p.subtitle);
        if (p.hero != null) {
            Object u = p.hero.getUri(a);
            Glide.with(a).load(u != null ? u : p.hero.getPath()).centerCrop().into(h.img);
        }
        h.itemView.setOnClickListener(v -> { if (onPeriod != null) onPeriod.onPeriod(p); });
        if (position > lastAnim) {
            lastAnim = position;
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationY(dp(a, 28));
            h.itemView.setScaleX(0.96f); h.itemView.setScaleY(0.96f);
            h.itemView.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(Math.min(position, 8) * 45L)
                    .setDuration(520).setInterpolator(Anim.ease()).start();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img; final TextView title, sub;
        VH(View v, ImageView img, TextView t, TextView s) { super(v); this.img = img; this.title = t; this.sub = s; }
    }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
