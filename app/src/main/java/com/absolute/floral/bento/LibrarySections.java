package com.absolute.floral.bento;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.absolute.floral.R;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.AlbumActivity;
import com.absolute.floral.ui.BucketActivity;
import com.absolute.floral.ui.CleanupActivity;
import com.absolute.floral.ui.MemoryActivity;
import com.absolute.floral.ui.PhotoMapActivity;
import com.absolute.floral.ui.PinningActivity;
import com.absolute.floral.ui.PlacesActivity;
import com.absolute.floral.util.MediaType;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The "Collections" stack that lives beneath the photo grid in the single-scroll
 * Library — Apple-Photos sections (Memories, People &amp; Pets, Places, Media Types,
 * Utilities, Albums) rendered with a soft bento accent.
 */
public final class LibrarySections {

    private LibrarySections() {}

    public static View build(final Activity a,
                             List<Album> albums,
                             List<Memories.Memory> memories,
                             List<PeopleIndex.Person> people,
                             List<PlacesIndex.Place> places,
                             LibrarySnapshot snap) {
        Soma s = SomaSkin.read(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.setPadding(0, dp(a, 8), 0, dp(a, 40));

        TextView big = new TextView(a);
        big.setText("Collections");
        big.setTypeface(Soma.display(a), Typeface.BOLD);
        big.setTextSize(28);
        big.setTextColor(s.ink);
        big.setPadding(dp(a, 16), dp(a, 18), dp(a, 16), dp(a, 6));
        col.addView(big);

        List<AlbumItem> all = flatten(albums);

        /* Recent Days */
        List<Day> days = recentDays(all, 24);
        if (!days.isEmpty()) {
            col.addView(sectionHeader(a, s, "Recent Days", null));
            HorizontalScrollView hs = hs(a);
            LinearLayout row = (LinearLayout) hs.getChildAt(0);
            for (Day d : days) {
                row.addView(bigCard(a, s, d.cover, d.label, d.items.size() + " photos",
                        () -> openBucket(a, d.label, "", d.items)));
            }
            col.addView(hs);
        }

        /* Memories */
        if (memories != null && !memories.isEmpty()) {
            col.addView(sectionHeader(a, s, "Memories", null));
            HorizontalScrollView hs = hs(a);
            LinearLayout row = (LinearLayout) hs.getChildAt(0);
            for (int i = 0; i < memories.size(); i++) {
                final int idx = i;
                Memories.Memory m = memories.get(i);
                row.addView(bigCard(a, s, m.cover(), m.title, m.items.size() + " photos", () -> {
                    MemoryActivity.MEMORIES = memories;
                    Intent it = new Intent(a, MemoryActivity.class);
                    it.putExtra(MemoryActivity.EXTRA_INDEX, idx);
                    a.startActivity(it);
                }));
            }
            col.addView(hs);
        }

        /* People & Pets */
        if (people != null && !people.isEmpty()) {
            col.addView(sectionHeader(a, s, "People & Pets", null));
            HorizontalScrollView hs = hs(a);
            LinearLayout row = (LinearLayout) hs.getChildAt(0);
            int n = 0;
            for (PeopleIndex.Person p : people) {
                final int num = ++n;
                LinearLayout cell = new LinearLayout(a);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(0, 0, dp(a, 14), 0);
                ImageView face = new ImageView(a);
                face.setScaleType(ImageView.ScaleType.CENTER_CROP);
                clipOval(face);
                if (p.cover != null) face.setImageBitmap(p.cover);
                else face.setBackgroundColor(s.surfaceStrong);
                cell.addView(face, new LinearLayout.LayoutParams(dp(a, 74), dp(a, 74)));
                TextView t = new TextView(a);
                t.setText("Person " + num);
                t.setTextColor(s.inkMute);
                t.setTextSize(11);
                t.setPadding(0, dp(a, 6), 0, 0);
                cell.addView(t);
                cell.setOnClickListener(v -> openBucket(a, "Person " + num, "PEOPLE", p.photos));
                row.addView(cell);
            }
            col.addView(hs);
        }

        /* Places */
        if (places != null && !places.isEmpty()) {
            View ph = sectionHeader(a, s, "Places", "Map");
            ph.setOnClickListener(v -> a.startActivity(new Intent(a, PhotoMapActivity.class)));
            col.addView(ph);
            LinearLayout grid = new LinearLayout(a);
            grid.setOrientation(LinearLayout.HORIZONTAL);
            grid.setPadding(dp(a, 16), 0, dp(a, 16), 0);
            for (int i = 0; i < Math.min(2, places.size()); i++) {
                PlacesIndex.Place pl = places.get(i);
                BentoTile t = new BentoTile(a);
                t.photo(cover(a, pl.cover()), 5 + i).label(pl.label)
                        .icon(R.drawable.ic_location_on_white)
                        .sub(pl.items.size() + " photos");
                t.setOnClickListener(v -> openBucket(a, pl.label, "PLACE", pl.items));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(a, 120), 1f);
                lp.setMargins(i == 0 ? 0 : dp(a, 9), dp(a, 4), 0, 0);
                grid.addView(t, lp);
            }
            col.addView(grid);
            col.addView(pillButton(a, s, "All Places", () -> a.startActivity(new Intent(a, PlacesActivity.class))));
        }

        /* Featured Photos */
        List<AlbumItem> favs = withPaths(all, FlagStore.favorites(a).all());
        if (!favs.isEmpty()) {
            col.addView(sectionHeader(a, s, "Featured Photos", null));
            HorizontalScrollView hs = hs(a);
            LinearLayout row = (LinearLayout) hs.getChildAt(0);
            for (AlbumItem it : favs.subList(0, Math.min(12, favs.size())))
                row.addView(squareThumb(a, it, () -> openBucket(a, "Featured Photos", "", favs)));
            col.addView(hs);
        }

        /* Media Types */
        col.addView(sectionHeader(a, s, "Media Types", null));
        LinkedHashMap<String, List<AlbumItem>> media = mediaTypes(all);
        LinearLayout mgrid = new LinearLayout(a);
        mgrid.setOrientation(LinearLayout.VERTICAL);
        mgrid.setPadding(dp(a, 16), 0, dp(a, 16), 0);
        LinearLayout mrow = null;
        int mi = 0;
        int[] accents = { 6, 9, 1, 3, 8 };
        for (Map.Entry<String, List<AlbumItem>> e : media.entrySet()) {
            if (mi % 3 == 0) {
                mrow = new LinearLayout(a);
                mrow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 92));
                rlp.topMargin = dp(a, 9);
                mgrid.addView(mrow, rlp);
            }
            final String nm = e.getKey();
            final List<AlbumItem> set = e.getValue();
            BentoTile t = new BentoTile(a);
            if (!set.isEmpty()) t.photo(cover(a, set.get(0)), accents[mi % accents.length]);
            else t.gradient(accents[mi % accents.length]);
            t.label(nm).sub(set.size() + (set.size() == 1 ? " item" : " items"));
            t.setOnClickListener(v -> { if (!set.isEmpty()) openBucket(a, nm, "MEDIA TYPE", set); });
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tlp.setMargins(mi % 3 == 0 ? 0 : dp(a, 9), 0, 0, 0);
            mrow.addView(t, tlp);
            mi++;
        }
        if (mi % 3 != 0 && mrow != null) {
            for (int k = mi % 3; k < 3; k++) {
                View sp = new View(a);
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, 1, 1f);
                slp.setMargins(dp(a, 9), 0, 0, 0);
                mrow.addView(sp, slp);
            }
        }
        col.addView(mgrid);

        /* Utilities */
        col.addView(sectionHeader(a, s, "Utilities", null));
        LinearLayout util = new LinearLayout(a);
        util.setOrientation(LinearLayout.VERTICAL);
        com.absolute.floral.soma.Card.surface(util, s, 18f, 1.5f);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ulp.setMargins(dp(a, 16), 0, dp(a, 16), 0);
        util.setLayoutParams(ulp);
        util.addView(utilRow(a, s, "Duplicates", () -> a.startActivity(new Intent(a, CleanupActivity.class)), false));
        util.addView(utilRow(a, s, "Hidden", () -> a.startActivity(new Intent(a, PinningActivity.class)), true));
        if (android.os.Build.VERSION.SDK_INT >= 30)
            util.addView(utilRow(a, s, "Recently Deleted", () -> openBucket(a, "Recently Deleted", "", trashed(a)), true));
        col.addView(util);

        /* My Albums */
        List<Album> nonEmpty = new ArrayList<>();
        if (albums != null) for (Album al : albums)
            if (al.getAlbumItems() != null && !al.getAlbumItems().isEmpty()) nonEmpty.add(al);
        if (!nonEmpty.isEmpty()) {
            col.addView(sectionHeader(a, s, "My Albums", null));
            LinearLayout ag = new LinearLayout(a);
            ag.setOrientation(LinearLayout.VERTICAL);
            ag.setPadding(dp(a, 16), 0, dp(a, 16), 0);
            LinearLayout arow = null;
            for (int i = 0; i < nonEmpty.size(); i++) {
                if (i % 2 == 0) {
                    arow = new LinearLayout(a);
                    arow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rlp.topMargin = dp(a, 12);
                    ag.addView(arow, rlp);
                }
                final Album al = nonEmpty.get(i);
                LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                alp.setMargins(i % 2 == 0 ? 0 : dp(a, 12), 0, 0, 0);
                arow.addView(albumCard(a, s, al), alp);
            }
            if (nonEmpty.size() % 2 == 1 && arow != null) {
                View sp = new View(a);
                LinearLayout.LayoutParams splp = new LinearLayout.LayoutParams(0, 1, 1f);
                splp.leftMargin = dp(a, 12);
                arow.addView(sp, splp);
            }
            col.addView(ag);
        }

        Anim.enterChildren(col, 20, 24);
        return col;
    }

    /* ----------------------------------------------------------------- helpers */

    private static View sectionHeader(Activity a, Soma s, String title, String action) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 16), dp(a, 22), dp(a, 16), dp(a, 10));
        TextView h = new TextView(a);
        h.setText(title);
        h.setTypeface(Soma.display(a), Typeface.BOLD);
        h.setTextSize(20);
        h.setTextColor(s.ink);
        row.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) {
            TextView act = new TextView(a);
            act.setText(action);
            act.setTypeface(Soma.body(a), Typeface.BOLD);
            act.setTextSize(13);
            act.setTextColor(s.accent);
            row.addView(act);
        }
        return row;
    }

    private static HorizontalScrollView hs(Activity a) {
        HorizontalScrollView h = new HorizontalScrollView(a);
        h.setHorizontalScrollBarEnabled(false);
        h.setClipToPadding(false);
        h.setPadding(dp(a, 16), 0, dp(a, 16), 0);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        h.addView(row);
        return h;
    }

    private static View bigCard(Activity a, Soma s, AlbumItem cover, String title, String sub, Runnable onTap) {
        android.widget.FrameLayout f = new android.widget.FrameLayout(a);
        int w = dp(a, 210), ht = dp(a, 150);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(w, ht);
        flp.rightMargin = dp(a, 10);
        f.setLayoutParams(flp);
        final float r = dp(a, 18);
        f.setClipToOutline(true);
        f.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(s.surfaceStrong);
        if (cover != null) Glide.with(a).load(cover(a, cover)).centerCrop().into(img);
        f.addView(img, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0x00000000, 0x00000000, 0x99000000 }));
        f.addView(scrim, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout cap = new LinearLayout(a);
        cap.setOrientation(LinearLayout.VERTICAL);
        cap.setPadding(dp(a, 12), 0, dp(a, 12), dp(a, 11));
        android.widget.FrameLayout.LayoutParams clp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        f.addView(cap, clp);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTypeface(Soma.display(a), Typeface.BOLD);
        t.setTextSize(16);
        t.setTextColor(0xFFFFFFFF);
        t.setMaxLines(1);
        cap.addView(t);
        TextView st = new TextView(a);
        st.setText(sub);
        st.setTextSize(11);
        st.setTextColor(0xCCFFFFFF);
        cap.addView(st);
        f.setOnClickListener(v -> onTap.run());
        return f;
    }

    private static View squareThumb(Activity a, AlbumItem it, Runnable onTap) {
        ImageView img = new ImageView(a);
        int d = dp(a, 96);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(d, d);
        ilp.rightMargin = dp(a, 4);
        img.setLayoutParams(ilp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        final float r = dp(a, 10);
        img.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        Glide.with(a).load(cover(a, it)).centerCrop().into(img);
        img.setOnClickListener(v -> onTap.run());
        return img;
    }

    private static View pillButton(Activity a, Soma s, String label, Runnable onTap) {
        TextView t = new TextView(a);
        t.setText(label);
        t.setTypeface(Soma.body(a), Typeface.BOLD);
        t.setTextSize(13);
        t.setTextColor(s.accent);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(a, 16), dp(a, 10), dp(a, 16), dp(a, 10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(s.surfaceStrong);
        g.setCornerRadius(dp(a, 100));
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(a, 16), dp(a, 10), dp(a, 16), 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> onTap.run());
        return t;
    }

    private static View utilRow(Activity a, Soma s, String label, Runnable onTap, boolean divider) {
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        if (divider) {
            View d = new View(a);
            d.setBackgroundColor(s.hairline);
            col.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        TextView t = new TextView(a);
        t.setText(label);
        t.setTypeface(Soma.body(a));
        t.setTextSize(15);
        t.setTextColor(s.ink);
        t.setPadding(dp(a, 14), dp(a, 15), dp(a, 14), dp(a, 15));
        t.setOnClickListener(v -> onTap.run());
        col.addView(t);
        return col;
    }

    private static View albumCard(Activity a, Soma s, Album al) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        com.absolute.floral.ui.widget.SquareFrameLayout box =
                new com.absolute.floral.ui.widget.SquareFrameLayout(a);
        box.setClipToOutline(true);
        final float r = dp(a, 12);
        box.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(s.surfaceStrong);
        AlbumItem cov = al.getAlbumItems().get(0);
        Glide.with(a).load(cover(a, cov)).centerCrop().into(img);
        box.addView(img, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(box, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView name = new TextView(a);
        name.setText(al.getName());
        name.setTypeface(Soma.body(a), Typeface.BOLD);
        name.setTextSize(13);
        name.setTextColor(s.ink);
        name.setMaxLines(1);
        name.setPadding(dp(a, 2), dp(a, 6), 0, 0);
        card.addView(name);
        TextView count = new TextView(a);
        count.setText(String.valueOf(al.getAlbumItems().size()));
        count.setTextSize(11);
        count.setTextColor(s.inkMute);
        count.setPadding(dp(a, 2), 0, 0, 0);
        card.addView(count);
        card.setOnClickListener(v -> {
            Intent i = new Intent(a, AlbumActivity.class);
            i.putExtra(AlbumActivity.ALBUM_PATH, al.getPath());
            a.startActivity(i);
        });
        return card;
    }

    /* ---- data ---- */

    private static class Day { String label; List<AlbumItem> items = new ArrayList<>(); AlbumItem cover; }

    private static List<AlbumItem> flatten(List<Album> albums) {
        List<AlbumItem> l = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        if (albums != null) for (Album al : albums)
            if (al.getAlbumItems() != null)
                for (AlbumItem it : al.getAlbumItems())
                    if (it != null && (it.getPath() == null || seen.add(it.getPath()))) l.add(it);
        Collections.sort(l, (p, q) -> Long.compare(q.getDate(), p.getDate()));
        return l;
    }

    private static List<Day> recentDays(List<AlbumItem> all, int max) {
        LinkedHashMap<String, Day> map = new LinkedHashMap<>();
        long cutoff = System.currentTimeMillis() - 45L * 24 * 3600 * 1000;
        Calendar c = Calendar.getInstance();
        for (AlbumItem it : all) {
            if (it.getDate() < cutoff) break;
            c.setTimeInMillis(it.getDate());
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
            Day d = map.get(key);
            if (d == null) {
                d = new Day();
                d.cover = it;
                d.label = android.text.format.DateFormat.format("EEEE, d MMM", it.getDate()).toString();
                map.put(key, d);
            }
            d.items.add(it);
        }
        List<Day> out = new ArrayList<>(map.values());
        return out.size() > max ? out.subList(0, max) : out;
    }

    private static LinkedHashMap<String, List<AlbumItem>> mediaTypes(List<AlbumItem> all) {
        List<AlbumItem> videos = new ArrayList<>(), shots = new ArrayList<>(),
                anim = new ArrayList<>(), recent = new ArrayList<>();
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        for (AlbumItem it : all) {
            String p = it.getPath() == null ? "" : it.getPath().toLowerCase(Locale.ROOT);
            String n = it.getName() == null ? "" : it.getName().toLowerCase(Locale.ROOT);
            if (MediaType.isVideo(p)) videos.add(it);
            else if (p.endsWith(".gif")) anim.add(it);
            if (p.contains("screenshot") || n.contains("screenshot")) shots.add(it);
            if (it.getDate() > weekAgo) recent.add(it);
        }
        LinkedHashMap<String, List<AlbumItem>> m = new LinkedHashMap<>();
        m.put("Videos", videos);
        m.put("Screenshots", shots);
        m.put("Animated", anim);
        m.put("Recently Added", recent);
        return m;
    }

    private static List<AlbumItem> withPaths(List<AlbumItem> all, java.util.Set<String> paths) {
        List<AlbumItem> out = new ArrayList<>();
        for (AlbumItem it : all) if (it.getPath() != null && paths.contains(it.getPath())) out.add(it);
        return out;
    }

    private static List<AlbumItem> trashed(Activity a) {
        List<AlbumItem> out = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < 30) return out;
        try {
            android.os.Bundle q = new android.os.Bundle();
            q.putInt(android.provider.MediaStore.QUERY_ARG_MATCH_TRASHED, android.provider.MediaStore.MATCH_ONLY);
            android.database.Cursor cur = a.getContentResolver().query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{ android.provider.MediaStore.Images.Media._ID }, q, null);
            if (cur != null) {
                while (cur.moveToNext())
                    out.add(AlbumItem.getInstance(a, android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cur.getLong(0))));
                cur.close();
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static void openBucket(Activity a, String title, String kicker, List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = kicker == null ? "" : kicker;
        BucketActivity.ITEMS = new ArrayList<>(items);
        a.startActivity(new Intent(a, BucketActivity.class));
    }

    private static Object cover(Activity a, AlbumItem it) {
        if (it == null) return null;
        Object u = it.getUri(a);
        return u != null ? u : it.getPath();
    }

    private static void clipOval(final View v) {
        v.setClipToOutline(true);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline o) {
                o.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
    }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
