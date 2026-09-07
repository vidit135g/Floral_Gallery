package com.absolute.floral.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.absolute.floral.R;
import com.absolute.floral.bento.LibrarySnapshot;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.List;

/** "Search by colour" — pick a hue, we scan on-device and open the matches. */
public class ColorSearchActivity extends AppCompatActivity {

    private static final String[] NAMES = { "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Violet", "Pink" };
    private static final float[] HUES = { 0, 35, 55, 130, 180, 220, 275, 320 };

    private Soma s;
    private TextView status;
    private volatile boolean busy;

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        s = SomaSkin.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(s.ground[0]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, s);

        int p = d(20);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(p, d(26), p, d(6));
        TextView title = new TextView(this);
        title.setText("Search by colour");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(28);
        title.setTextColor(s.ink);
        head.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Pick a hue — matched entirely on your device");
        subtitle.setTextColor(s.inkMute);
        subtitle.setTextSize(13);
        head.addView(subtitle);
        root.addView(head);
        Anim.enter(head, 30);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setPadding(d(12), d(10), d(12), d(10));
        root.addView(grid);

        for (int i = 0; i < NAMES.length; i++) {
            final int idx = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.BOTTOM);
            cell.setPadding(d(16), d(16), d(16), d(14));
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    new int[]{ Color.HSVToColor(new float[]{ HUES[i], 0.55f, 0.98f }),
                               Color.HSVToColor(new float[]{ (HUES[i] + 20) % 360, 0.7f, 0.86f }) });
            g.setCornerRadius(d(22));
            cell.setBackground(g);
            TextView t = new TextView(this);
            t.setText(NAMES[i]);
            t.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
            t.setTextSize(17);
            t.setTextColor(i == 6 ? 0xFFFFFFFF : 0xFF1C1B1F);
            cell.addView(t);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = d(104);
            lp.columnSpec = GridLayout.spec(i % 2, 1f);
            lp.setMargins(d(6), d(6), d(6), d(6));
            cell.setLayoutParams(lp);
            cell.setClickable(true);
            cell.setOnClickListener(v -> pick(idx));
            grid.addView(cell);

            cell.setAlpha(0f);
            cell.setTranslationY(d(20));
            final int fi = i;
            cell.postDelayed(() -> cell.animate().alpha(1f).translationY(0f)
                    .setDuration(420).setInterpolator(Anim.ease()).start(), 40L + fi * 45L);
        }

        status = new TextView(this);
        status.setPadding(p, d(8), p, d(8));
        status.setTextColor(s.inkMute);
        status.setTextSize(13);
        root.addView(status);
    }

    private void pick(final int hueIndex) {
        if (busy) return;
        busy = true;
        status.setText("Scanning your photos for " + NAMES[hueIndex].toLowerCase() + "…");
        final float target = HUES[hueIndex];
        new Thread(() -> {
            List<AlbumItem> all = new ArrayList<>();
            ArrayList<Album> al = MediaProvider.getAlbums();
            if (al != null) for (Album a : al) if (a.getAlbumItems() != null) all.addAll(a.getAlbumItems());
            java.util.Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));
            List<AlbumItem> hits = new ArrayList<>();
            int scanned = 0;
            for (AlbumItem it : all) {
                if (scanned >= 300 || hits.size() >= 120) break;
                if (MediaType.isVideo(it.getPath())) continue;
                Bitmap bmp = LibrarySnapshot.thumb(this, it);
                if (bmp == null) continue;
                scanned++;
                try {
                    Palette pal = Palette.from(bmp).clearFilters().generate();
                    for (Palette.Swatch sw : pal.getSwatches()) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(sw.getRgb(), hsv);
                        if (hsv[1] < 0.18f) continue;                 // skip greys
                        float diff = Math.abs(hsv[0] - target);
                        diff = Math.min(diff, 360 - diff);
                        if (diff < 24f) { hits.add(it); break; }
                    }
                } catch (Exception ignored) {}
                bmp.recycle();
            }
            final List<AlbumItem> result = hits;
            runOnUiThread(() -> {
                busy = false;
                if (result.isEmpty()) {
                    Toast.makeText(this, "No " + NAMES[hueIndex].toLowerCase() + " photos found", Toast.LENGTH_SHORT).show();
                    status.setText("");
                    return;
                }
                BucketActivity.TITLE = NAMES[hueIndex] + " photos";
                BucketActivity.KICKER = "COLOUR";
                BucketActivity.ITEMS = result;
                startActivity(new android.content.Intent(this, BucketActivity.class));
                status.setText("");
            });
        }, "floral-colour").start();
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
