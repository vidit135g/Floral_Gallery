package com.absolute.floral.adapter.photos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.R;
import com.absolute.floral.data.Memories;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.MemoryActivity;
import com.bumptech.glide.Glide;

import java.util.List;

/** The horizontal "Memories" carousel shown at the top of the Photos tab. */
public class MemoriesStrip extends HorizontalScrollView {

    private final LinearLayout row;
    private List<Memories.Memory> current;

    public MemoriesStrip(Context c) {
        super(c);
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        int p = dp(10);
        setPadding(p, dp(8), p, dp(6));
        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void bind(List<Memories.Memory> memories) {
        if (memories == current) return;
        current = memories;
        row.removeAllViews();
        Soma soma = SomaSkin.read(getContext());
        int cardW = dp(112), cardH = dp(170);
        for (int i = 0; i < memories.size(); i++) {
            final Memories.Memory m = memories.get(i);
            FrameLayout card = new FrameLayout(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cardW, cardH);
            lp.setMargins(dp(4), 0, dp(4), 0);
            card.setLayoutParams(lp);
            card.setClipToOutline(true);
            GradientDrawable clip = new GradientDrawable();
            clip.setCornerRadius(dp(20));
            clip.setColor(soma != null ? soma.surface : 0xFFEEEEEE);
            card.setBackground(clip);
            card.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(20));
                }
            });

            ImageView img = new ImageView(getContext());
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(cardW, cardH));
            if (m.cover() != null) {
                Object t = m.cover().getUri(getContext());
                Glide.with(getContext()).load(t != null ? t : m.cover().getPath())
                        .centerCrop().into(img);
            }

            View scrim = new View(getContext());
            GradientDrawable sg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{ 0x00000000, 0x00000000, 0xB3000000 });
            scrim.setBackground(sg);
            card.addView(scrim, new FrameLayout.LayoutParams(cardW, cardH));

            LinearLayout txt = new LinearLayout(getContext());
            txt.setOrientation(LinearLayout.VERTICAL);
            txt.setPadding(dp(12), dp(12), dp(12), dp(12));
            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            tlp.gravity = Gravity.BOTTOM;
            card.addView(txt, tlp);

            TextView tt = new TextView(getContext());
            tt.setText(m.title);
            tt.setTextColor(Color.WHITE);
            tt.setTextSize(13);
            tt.setMaxLines(2);
            tt.setTypeface(Soma.display(getContext()));
            tt.setShadowLayer(dp(6), 0, dp(1), 0x80000000);
            txt.addView(tt);

            final int idx = i;
            card.setOnClickListener(v -> {
                MemoryActivity.MEMORIES = current;
                Intent it = new Intent(getContext(), MemoryActivity.class);
                it.putExtra(MemoryActivity.EXTRA_INDEX, idx);
                getContext().startActivity(it);
            });
            row.addView(card);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
