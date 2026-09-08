package com.absolute.floral.bento;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.absolute.floral.R;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Card;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.BucketActivity;
import com.absolute.floral.ui.MemoryActivity;
import com.absolute.floral.ui.PhotoMapActivity;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Soma home — a bento dashboard, consistent with the other Absolute apps:
 * a big hero header card, quick-access tiles, then on-this-day, albums and
 * places. Data comes from the shared {@link CollectionsScreen.Providers}.
 */
public final class HomeScreen {

    private HomeScreen() {}

    public interface OnOpenLibrary { void open(); }

    public static final class Holder {
        private final Activity a;
        private final NestedScrollView scroll;
        private final LinearLayout col;
        private CollectionsScreen.Providers p = new CollectionsScreen.Providers();
        private OnOpenLibrary onLibrary;
        private int signature = -1;

        Holder(Activity a) {
            this.a = a;
            scroll = new NestedScrollView(a);
            scroll.setFillViewport(true);
            col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(col, new NestedScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        public View view() { return scroll; }
        public void setOnOpenLibrary(OnOpenLibrary l) { this.onLibrary = l; }

        public void refresh(CollectionsScreen.Providers next) {
            if (next != null) p = next;
            int sig = sig(p) * 31 + p.version;
            if (sig == signature && col.getChildCount() > 0) return;
            signature = sig;
            build();
        }

        private void build() {
            Soma s = SomaSkin.read(a);
            col.removeAllViews();
            col.setPadding(dp(16), dp(4), dp(16), dp(120));

            final List<AlbumItem> all = CollectionsScreen.flatten(p.albums);
            int photos = 0, videos = 0;
            for (AlbumItem it : all) {
                if (MediaType.isVideo(it.getPath())) videos++; else photos++;
            }
            int albums = 0;
            if (p.albums != null) for (Album al : p.albums)
                if (al.getAlbumItems() != null && !al.getAlbumItems().isEmpty()) albums++;

            /* hero */
            col.addView(hero(s, all.isEmpty() ? null : all.get(0), photos, videos, albums));

            /* quick tiles */
            final List<AlbumItem> favs = withPaths(all, FlagStore.favorites(a).all());
            final List<AlbumItem> vids = new ArrayList<>();
            for (AlbumItem it : all) if (MediaType.isVideo(it.getPath())) vids.add(it);
            final List<AlbumItem> recent = new ArrayList<>();
            long weekAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
            for (AlbumItem it : all) { if (it.getDate() < weekAgo) break; recent.add(it); }

            LinearLayout tiles = new LinearLayout(a);
            tiles.setOrientation(LinearLayout.VERTICAL);
            tiles.setPadding(0, dp(4), 0, 0);
            tiles.addView(tileRow(s,
                    tile(s, favs.isEmpty() ? null : favs.get(0), 3, "Favourites", favs.size(),
                            R.drawable.ic_star_white, () -> open("Favourites", favs)),
                    tile(s, vids.isEmpty() ? null : vids.get(0), 6, "Videos", vids.size(),
                            0, () -> open("Videos", vids))));
            tiles.addView(tileRow(s,
                    tile(s, recent.isEmpty() ? null : recent.get(0), 1, "Recently Added", recent.size(),
                            0, () -> open("Recently Added", recent)),
                    tile(s, coverOf(p.people), 8, "People & Pets", p.people == null ? 0 : p.people.size(),
                            0, () -> { if (p.people != null && !p.people.isEmpty())
                                openBucket("People & Pets", "PEOPLE", p.people.get(0).photos); })));
            col.addView(tiles);

            /* on this day */
            if (p.memories != null && !p.memories.isEmpty()) {
                col.addView(header(s, "On This Day"));
                com.absolute.floral.bento.CollectionsScreen.Providers pp = p;
                android.widget.HorizontalScrollView hs = CollectionsScreen.hs(a);
                LinearLayout row = (LinearLayout) hs.getChildAt(0);
                for (int i = 0; i < p.memories.size(); i++) {
                    final int idx = i;
                    Memories.Memory m = p.memories.get(i);
                    row.addView(CollectionsScreen.bigCard(a, s, m.cover(), m.title,
                            m.items.size() + " photos", () -> {
                                MemoryActivity.MEMORIES = pp.memories;
                                Intent it = new Intent(a, MemoryActivity.class);
                                it.putExtra(MemoryActivity.EXTRA_INDEX, idx);
                                a.startActivity(it);
                            }));
                }
                col.addView(hs);
            }

            /* albums */
            final List<Album> nonEmpty = new ArrayList<>();
            if (p.albums != null) for (Album al : p.albums)
                if (al.getAlbumItems() != null && !al.getAlbumItems().isEmpty()) nonEmpty.add(al);
            if (!nonEmpty.isEmpty()) {
                col.addView(header(s, "Albums"));
                LinearLayout ag = new LinearLayout(a);
                ag.setOrientation(LinearLayout.VERTICAL);
                LinearLayout arow = null;
                for (int i = 0; i < nonEmpty.size(); i++) {
                    if (i % 2 == 0) {
                        arow = new LinearLayout(a);
                        arow.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rlp.topMargin = dp(12);
                        ag.addView(arow, rlp);
                    }
                    LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    alp.setMargins(i % 2 == 0 ? 0 : dp(12), 0, 0, 0);
                    arow.addView(CollectionsScreen.albumCard(a, s, nonEmpty.get(i)), alp);
                }
                if (nonEmpty.size() % 2 == 1 && arow != null) {
                    View sp = new View(a);
                    LinearLayout.LayoutParams splp = new LinearLayout.LayoutParams(0, 1, 1f);
                    splp.leftMargin = dp(12);
                    arow.addView(sp, splp);
                }
                col.addView(ag);
            }

            /* places */
            if (p.places != null && !p.places.isEmpty()) {
                col.addView(header(s, "Places"));
                TextView open = new TextView(a);
                open.setText("Open the map");
                open.setTypeface(Soma.body(a), Typeface.BOLD);
                open.setTextColor(a.getResources().getColor(R.color.ios_blue));
                open.setTextSize(14);
                open.setPadding(dp(2), dp(4), 0, 0);
                open.setOnClickListener(v -> a.startActivity(new Intent(a, PhotoMapActivity.class)));
                col.addView(open);
            }

            Anim.enterChildren(col, 16, 20);
        }

        /* ---------------------------------------------------------- pieces */

        private View hero(Soma s, AlbumItem cover, int photos, int videos, int albums) {
            FrameLayout f = new FrameLayout(a);
            LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(190));
            flp.topMargin = dp(6);
            f.setLayoutParams(flp);
            final float r = dp(26);

            // a beautiful multi-stop gradient — rose → violet → indigo, on the
            // Floral / iOS-18 family palette
            GradientDrawable body = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{ 0xFFFF6FA3, 0xFFA24CD6, 0xFF5B54E0 });
            body.setCornerRadius(r);
            GradientDrawable sheen = new GradientDrawable();
            sheen.setShape(GradientDrawable.RECTANGLE);
            sheen.setCornerRadius(r);
            sheen.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            sheen.setColors(new int[]{ 0x40FFFFFF, 0x0FFFFFFF, 0x00FFFFFF });
            sheen.setGradientCenter(0.16f, 0.0f);
            sheen.setGradientRadius(dp(260));
            f.setBackground(new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{ body, sheen }));
            f.setClipToOutline(true);
            f.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                }
            });
            f.setElevation(dp(3));

            LinearLayout cap = new LinearLayout(a);
            cap.setOrientation(LinearLayout.VERTICAL);
            cap.setPadding(dp(20), 0, dp(20), dp(18));
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.gravity = Gravity.BOTTOM;
            f.addView(cap, clp);

            TextView kicker = new TextView(a);
            kicker.setText("YOUR LIBRARY");
            kicker.setAllCaps(true);
            kicker.setLetterSpacing(0.14f);
            kicker.setTextSize(10);
            kicker.setTypeface(Soma.body(a), Typeface.BOLD);
            kicker.setTextColor(0xCCFFFFFF);
            cap.addView(kicker);

            TextView big = new TextView(a);
            big.setText(fmt(photos) + " Photos");
            big.setTypeface(Soma.display(a), Typeface.BOLD);
            big.setTextSize(26);
            big.setTextColor(0xFFFFFFFF);
            big.setLetterSpacing(-0.02f);
            big.setPadding(0, dp(2), 0, 0);
            cap.addView(big);

            TextView sub = new TextView(a);
            sub.setText(fmt(videos) + (videos == 1 ? " video · " : " videos · ")
                    + fmt(albums) + (albums == 1 ? " album" : " albums"));
            sub.setTextSize(12);
            sub.setTextColor(0xCCFFFFFF);
            cap.addView(sub);

            f.setOnClickListener(v -> { if (onLibrary != null) onLibrary.open(); });
            return f;
        }

        private LinearLayout tileRow(Soma s, View left, View right) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(110));
            rlp.topMargin = dp(10);
            row.setLayoutParams(rlp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            row.addView(left, lp);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp2.leftMargin = dp(10);
            row.addView(right, lp2);
            return row;
        }

        private View tile(Soma s, AlbumItem cover, int accent, String label, int count, int icon, Runnable onTap) {
            BentoTile t = new BentoTile(a);
            if (cover != null) t.photo(CollectionsScreen.cover(a, cover), accent);
            else t.gradient(accent);
            t.label(label).sub(count + (count == 1 ? " item" : " items"));
            if (icon != 0) t.icon(icon);
            t.setOnClickListener(v -> onTap.run());
            return t;
        }

        private TextView header(Soma s, String text) {
            TextView h = new TextView(a);
            h.setText(text);
            h.setTypeface(Soma.display(a), Typeface.BOLD);
            h.setTextSize(21);
            h.setTextColor(s.ink);
            h.setLetterSpacing(-0.01f);
            h.setPadding(dp(2), dp(24), 0, dp(8));
            return h;
        }

        private void open(String title, List<AlbumItem> items) {
            openBucket(title, "", items);
        }
        private void openBucket(String title, String kicker, List<AlbumItem> items) {
            BucketActivity.TITLE = title;
            BucketActivity.KICKER = kicker == null ? "" : kicker;
            BucketActivity.ITEMS = new ArrayList<>(items);
            a.startActivity(new Intent(a, BucketActivity.class));
        }

        private AlbumItem coverOf(List<com.absolute.floral.people.PeopleIndex.Person> people) {
            if (people == null || people.isEmpty() || people.get(0).photos.isEmpty()) return null;
            return people.get(0).photos.get(0);
        }

        private static List<AlbumItem> withPaths(List<AlbumItem> all, java.util.Set<String> paths) {
            List<AlbumItem> out = new ArrayList<>();
            for (AlbumItem it : all) if (it.getPath() != null && paths.contains(it.getPath())) out.add(it);
            return out;
        }

        private static String fmt(int n) {
            if (n >= 1000) return String.format(Locale.getDefault(), "%,d", n);
            return String.valueOf(n);
        }

        private int dp(float v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }

        private static int sig(CollectionsScreen.Providers p) {
            int n = 0;
            if (p.albums != null) for (Album al : p.albums)
                n = n * 31 + (al.getAlbumItems() == null ? 0 : al.getAlbumItems().size());
            n = n * 31 + (p.memories == null ? 0 : p.memories.size());
            n = n * 31 + (p.people == null ? 0 : p.people.size());
            n = n * 31 + (p.places == null ? 0 : p.places.size());
            return n;
        }
    }

    public static Holder build(Activity a, CollectionsScreen.Providers p) {
        Holder h = new Holder(a);
        h.refresh(p);
        return h;
    }
}
