package com.absolute.floral.ui;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.bento.Bento;
import com.absolute.floral.bento.BentoTile;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.List;

/** Photos grouped by where they were taken (EXIF GPS, clustered on-device). */
public class PlacesActivity extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        final Soma s = SomaSkin.read(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setId(R.id.root_view);
        scroll.setBackgroundColor(s.ground[0]);
        setContentView(scroll);
        SomaSkin.statusBarIcons(this, s);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = d(16);
        root.setPadding(d(10), d(26), d(10), d(24));
        scroll.addView(root);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(d(8), 0, d(8), d(6));
        TextView title = new TextView(this);
        title.setText("Places");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(30);
        title.setTextColor(s.ink);
        head.addView(title);
        final TextView sub = new TextView(this);
        sub.setText("Reading location from your photos…");
        sub.setTextColor(s.inkMute);
        sub.setTextSize(13);
        head.addView(sub);
        root.addView(head);
        Anim.enter(head, 30);

        PlacesIndex.get().ensure(this, places -> {
            if (places.isEmpty()) {
                sub.setText("No photos with location yet");
                return;
            }
            sub.setText(places.size() + (places.size() == 1 ? " place" : " places"));
            LinearLayout rowRef = null;
            for (int i = 0; i < places.size(); i++) {
                final PlacesIndex.Place pl = places.get(i);
                if (i % 2 == 0) {
                    rowRef = new LinearLayout(this);
                    rowRef.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, d(150));
                    rlp.setMargins(d(6), d(6), d(6), d(6));
                    root.addView(rowRef, rlp);
                }
                BentoTile tile = new BentoTile(this);
                if (pl.cover() != null) {
                    Object u = pl.cover().getUri(this);
                    tile.photo(u != null ? u : pl.cover().getPath(), i % 10);
                } else tile.gradient(i % 10);
                tile.label(pl.label).sub(pl.items.size() + (pl.items.size() == 1 ? " photo" : " photos"))
                        .icon(R.drawable.ic_location_on_white);
                tile.setClickable(true);
                tile.setOnClickListener(v -> {
                    BucketActivity.TITLE = pl.label;
                    BucketActivity.KICKER = "PLACE";
                    BucketActivity.ITEMS = new java.util.ArrayList<>(pl.items);
                    startActivity(new android.content.Intent(this, BucketActivity.class));
                });
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                tlp.setMargins(d(6), 0, d(6), 0);
                tile.setAlpha(0f); tile.setTranslationY(d(16));
                final int fi = i;
                tile.postDelayed(() -> tile.animate().alpha(1f).translationY(0f)
                        .setDuration(420).setInterpolator(Anim.ease()).start(), 40L + fi * 45L);
                rowRef.addView(tile, tlp);
            }
        });
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
