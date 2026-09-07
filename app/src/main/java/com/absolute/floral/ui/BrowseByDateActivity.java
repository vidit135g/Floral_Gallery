package com.absolute.floral.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.bento.Bento;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Browse the library by year and month — tap a month to open its photos. */
public class BrowseByDateActivity extends AppCompatActivity {

    private static final String[] MONTHS = { "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec" };

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        Soma s = SomaSkin.read(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setId(R.id.root_view);
        scroll.setBackgroundColor(s.ground[0]);
        setContentView(scroll);
        SomaSkin.statusBarIcons(this, s);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(d(18), d(26), d(18), d(28));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("By date");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(30);
        title.setTextColor(s.ink);
        root.addView(title);
        Anim.enter(title, 20);

        // gather year -> month -> items
        List<AlbumItem> all = new ArrayList<>();
        ArrayList<Album> al = MediaProvider.getAlbums();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        if (al != null) for (Album a : al) if (a.getAlbumItems() != null)
            for (AlbumItem it : a.getAlbumItems())
                if (it != null && (it.getPath() == null || seen.add(it.getPath()))) all.add(it);

        TreeMap<Integer, Map<Integer, List<AlbumItem>>> byYear =
                new TreeMap<>(java.util.Collections.reverseOrder());
        Calendar c = Calendar.getInstance();
        for (AlbumItem it : all) {
            long dt = it.getDate();
            if (dt <= 0) continue;
            c.setTimeInMillis(dt);
            int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH);
            byYear.computeIfAbsent(y, k -> new LinkedHashMap<>())
                    .computeIfAbsent(m, k -> new ArrayList<>()).add(it);
        }

        if (byYear.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("Your photos need dates to browse by month.");
            none.setTextColor(s.inkMute);
            none.setPadding(0, d(20), 0, 0);
            root.addView(none);
            return;
        }

        int gi = 0;
        for (Map.Entry<Integer, Map<Integer, List<AlbumItem>>> ye : byYear.entrySet()) {
            final int year = ye.getKey();
            int yearTotal = 0;
            for (List<AlbumItem> l : ye.getValue().values()) yearTotal += l.size();

            TextView yh = new TextView(this);
            yh.setText(String.valueOf(year));
            yh.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
            yh.setTextSize(20);
            yh.setTextColor(s.ink);
            yh.setPadding(0, d(24), 0, d(4));
            root.addView(yh);
            TextView yc = new TextView(this);
            yc.setText(yearTotal + " photos");
            yc.setTextColor(s.inkMute);
            yc.setTextSize(12);
            root.addView(yc);

            // simple manual wrap: 3 chips per row
            LinearLayout cur = null;
            int idx = 0;
            List<Integer> months = new ArrayList<>(ye.getValue().keySet());
            java.util.Collections.sort(months, java.util.Collections.reverseOrder());
            for (final int m : months) {
                if (idx % 3 == 0) {
                    cur = new LinearLayout(this);
                    cur.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, d(76));
                    clp.topMargin = d(8);
                    root.addView(cur, clp);
                }
                final List<AlbumItem> items = ye.getValue().get(m);
                final int[] g = Bento.gradient(gi++);
                LinearLayout chip = new LinearLayout(this);
                chip.setOrientation(LinearLayout.VERTICAL);
                chip.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR, g);
                bg.setCornerRadius(d(18));
                chip.setBackground(bg);
                TextView mn = new TextView(this);
                mn.setText(MONTHS[m]);
                mn.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
                mn.setTextSize(15);
                mn.setTextColor(0xFF1C1B1F);
                chip.addView(mn);
                TextView mc = new TextView(this);
                mc.setText(String.valueOf(items.size()));
                mc.setTextColor(0xB0000000);
                mc.setTextSize(11);
                chip.addView(mc);
                chip.setOnClickListener(v -> {
                    BucketActivity.TITLE = MONTHS[m] + " " + year;
                    BucketActivity.KICKER = "";
                    BucketActivity.ITEMS = new ArrayList<>(items);
                    startActivity(new android.content.Intent(this, BucketActivity.class));
                });
                LinearLayout.LayoutParams chlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                chlp.setMargins(idx % 3 == 0 ? 0 : d(8), 0, 0, 0);
                cur.addView(chip, chlp);
                idx++;
            }
            // pad last row
            if (cur != null) while (idx % 3 != 0) {
                cur.addView(new android.view.View(this),
                        new LinearLayout.LayoutParams(0, 1, 1f));
                idx++;
            }
        }
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
