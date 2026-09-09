package com.absolute.floral.bento;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;

import com.absolute.floral.R;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.RecentStore;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Card;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.AlbumActivity;
import com.absolute.floral.ui.BucketActivity;
import com.absolute.floral.ui.CleanupActivity;
import com.absolute.floral.ui.MemoryActivity;
import com.absolute.floral.ui.PhotoMapActivity;
import com.absolute.floral.ui.PinningActivity;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The <b>Collections</b> tab — a strict Apple-Photos replica: collapsible titled
 * sections (Memories, Pinned, Albums, People &amp; Pets, Trips, Media Types,
 * Utilities, Wallpaper Suggestions) inside one scroll view, each remembering
 * whether it's open. Data arrives already loaded via {@link Providers}; a
 * {@link Holder#refresh} rebuilds the stack when the library changes.
 */
public final class CollectionsScreen {

    private CollectionsScreen() {}

    /** Everything the screen renders from — populated by MainActivity.refreshPhotos(). */
    public static final class Providers {
        /** what the bento screens display — already Guest-Mode filtered. */
        public List<Album> albums = new ArrayList<>();
        /** the unfiltered scan result. */
        public List<Album> rawAlbums = new ArrayList<>();
        public List<Memories.Memory> memories = new ArrayList<>();
        public List<PeopleIndex.Person> people = new ArrayList<>();
        public List<PlacesIndex.Place> places = new ArrayList<>();
        public LibrarySnapshot snap;
        public java.util.LinkedHashMap<String, List<AlbumItem>> mediaTypes = new java.util.LinkedHashMap<>();
        /** bumped on any favourite / delete so Home + Collections rebuild even
         *  when album counts are unchanged. */
        public int version = 0;
        public boolean favoritesDirty = false;

        /** Store the raw scan and (re)compute the displayed {@link #albums}.
         *  Pass the Guest-Mode allow-list to hide everything else, or null. */
        public void setAlbums(List<Album> raw, java.util.Set<String> guestAllow) {
            this.rawAlbums = raw != null ? raw : new ArrayList<>();
            if (guestAllow == null) { this.albums = this.rawAlbums; return; }
            List<Album> f = new ArrayList<>();
            for (Album a : this.rawAlbums) {
                if (a == null || a.getAlbumItems() == null) continue;
                Album copy = new Album();
                copy.setPath(a.getPath());
                for (AlbumItem it : a.getAlbumItems())
                    if (it != null && it.getPath() != null && guestAllow.contains(it.getPath()))
                        copy.getAlbumItems().add(it);
                if (!copy.getAlbumItems().isEmpty()) f.add(copy);
            }
            this.albums = f;
        }
    }

    public static final class Holder {
        private final Activity a;
        private final NestedScrollView scroll;
        private final LinearLayout col;
        private Providers p = new Providers();
        private int signature = -1;

        Holder(Activity a) {
            this.a = a;
            scroll = new NestedScrollView(a);
            scroll.setFillViewport(true);
            col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new NestedScrollView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            scroll.addView(col);
        }

        public View view() { return scroll; }

        public void refresh(Providers next) {
            if (next != null) this.p = next;
            int sig = sigOf(p) * 31 + p.version;
            if (sig == signature && col.getChildCount() > 0) return;
            signature = sig;
            build();
        }

        private void build() {
            Soma s = SomaSkin.read(a);
            col.removeAllViews();
            col.setPadding(0, dp(a, 2), 0, dp(a, 120));

            final List<AlbumItem> all = flatten(p.albums);
            final SharedPreferences ui = a.getSharedPreferences("collections_ui", Context.MODE_PRIVATE);

            /* Memories */
            if (p.memories != null && !p.memories.isEmpty()) {
                section(s, ui, "memories", "Memories", false, null, null, body -> {
                    HorizontalScrollView hs = hs(a);
                    LinearLayout row = (LinearLayout) hs.getChildAt(0);
                    for (int i = 0; i < p.memories.size(); i++) {
                        final int idx = i;
                        Memories.Memory m = p.memories.get(i);
                        row.addView(bigCard(a, s, m.cover(), m.title,
                                m.items.size() + " photos", () -> {
                                    MemoryActivity.MEMORIES = p.memories;
                                    Intent it = new Intent(a, MemoryActivity.class);
                                    it.putExtra(MemoryActivity.EXTRA_INDEX, idx);
                                    a.startActivity(it);
                                }));
                    }
                    body.addView(hs);
                });
            }

            /* Pinned */
            {
                final List<AlbumItem> favs = withPaths(all, FlagStore.favorites(a).all());
                final List<AlbumItem> recent = new ArrayList<>();
                long weekAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
                for (AlbumItem it : all) { if (it.getDate() < weekAgo) break; recent.add(it); }
                final List<AlbumItem> viewed = RecentStore.resolve(a, RecentStore.viewed(a), all);
                final List<AlbumItem> shared = RecentStore.resolve(a, RecentStore.shared(a), all);
                boolean any = !favs.isEmpty() || !recent.isEmpty() || !p.places.isEmpty()
                        || !viewed.isEmpty() || !shared.isEmpty();
                if (any) {
                    section(s, ui, "pinned", "Pinned", true, null, pillEdit(s), body -> {
                        LinearLayout grid = tileGrid(a);
                        if (!favs.isEmpty())
                            addTile(grid, s, tile(a, s, favs.get(0), 3, "Favorites", favs.size(),
                                    R.drawable.ic_star_white,
                                    () -> openBucket(a, "Favorites", "", favs)));
                        if (!recent.isEmpty())
                            addTile(grid, s, tile(a, s, recent.get(0), 6, "Recently Saved", recent.size(),
                                    0, () -> openBucket(a, "Recently Saved", "", recent)));
                        if (!p.places.isEmpty())
                            addTile(grid, s, tile(a, s, p.places.get(0).cover(), 8, "Map", locatedCount(),
                                    R.drawable.ic_location_on_white,
                                    () -> a.startActivity(new Intent(a, PhotoMapActivity.class))));
                        if (!viewed.isEmpty())
                            addTile(grid, s, tile(a, s, viewed.get(0), 1, "Recently Viewed", viewed.size(),
                                    0, () -> openBucket(a, "Recently Viewed", "", viewed)));
                        if (!shared.isEmpty())
                            addTile(grid, s, tile(a, s, shared.get(0), 9, "Recently Shared", shared.size(),
                                    0, () -> openBucket(a, "Recently Shared", "", shared)));
                        body.addView(grid);
                    });
                }
            }

            /* Albums */
            final List<Album> nonEmpty = new ArrayList<>();
            final java.util.Set<String> reserved = new java.util.HashSet<>(java.util.Arrays.asList(
                    "people", "people & pets", "trips", "memories", "recent days",
                    "featured photos", "media types", "utilities", "favorites", "favourites"));
            if (p.albums != null) for (Album al : p.albums)
                if (al.getAlbumItems() != null && !al.getAlbumItems().isEmpty()
                        && (al.getName() == null
                            || !reserved.contains(al.getName().trim().toLowerCase(Locale.ROOT))))
                    nonEmpty.add(al);
            if (!nonEmpty.isEmpty()) {
                section(s, ui, "albums", "Albums", true, null, null, body -> {
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
                    body.addView(ag);
                });
            }

            /* Recent Days */
            final List<Day> days = recentDays(all, 20);
            if (!days.isEmpty()) {
                section(s, ui, "recentdays", "Recent Days", true, null, null, body -> {
                    HorizontalScrollView hs = hs(a);
                    LinearLayout row = (LinearLayout) hs.getChildAt(0);
                    for (Day d : days)
                        row.addView(bigCard(a, s, d.cover, d.label, d.items.size() + " photos",
                                () -> openBucket(a, d.label, "", d.items)));
                    body.addView(hs);
                });
            }

            /* People & Pets */
            if (p.people != null && !p.people.isEmpty()) {
                section(s, ui, "people", "People & Pets", true, null, null, body -> {
                    HorizontalScrollView hs = hs(a);
                    LinearLayout row = (LinearLayout) hs.getChildAt(0);
                    int n = 0;
                    for (PeopleIndex.Person person : p.people) {
                        final int num = ++n;
                        final PeopleIndex.Person pp = person;
                        LinearLayout cell = new LinearLayout(a);
                        cell.setOrientation(LinearLayout.VERTICAL);
                        cell.setGravity(Gravity.CENTER_HORIZONTAL);
                        cell.setPadding(0, 0, dp(a, 14), 0);
                        ImageView face = new ImageView(a);
                        face.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        clipOval(face);
                        if (person.cover != null) face.setImageBitmap(person.cover);
                        else face.setBackgroundColor(s.surfaceStrong);
                        cell.addView(face, new LinearLayout.LayoutParams(dp(a, 78), dp(a, 78)));
                        TextView t = new TextView(a);
                        boolean named = person.name != null && !person.name.isEmpty();
                        t.setText(named ? person.name : "Add Name");
                        t.setTextColor(named ? s.ink : a.getResources().getColor(R.color.ios_blue));
                        t.setTextSize(11);
                        t.setTypeface(Soma.body(a), named ? Typeface.BOLD : Typeface.NORMAL);
                        t.setPadding(0, dp(a, 6), 0, 0);
                        cell.addView(t);
                        final String label = named ? person.name : "Person " + num;
                        cell.setOnClickListener(v -> {
                            if (named) openBucket(a, label, "PEOPLE", pp.photos);
                            else renamePerson(pp);
                        });
                        cell.setOnLongClickListener(v -> { renamePerson(pp); return true; });
                        row.addView(cell);
                    }
                    body.addView(hs);
                });
            }

            /* Trips */
            final List<Trips.Trip> trips = Trips.from(p.places);
            if (!trips.isEmpty()) {
                section(s, ui, "trips", "Trips", true, null, null, body -> {
                    HorizontalScrollView hs = hs(a);
                    LinearLayout row = (LinearLayout) hs.getChildAt(0);
                    for (Trips.Trip t : trips) {
                        final Trips.Trip tt = t;
                        row.addView(bigCard(a, s, t.cover(), t.title, t.items.size() + " photos",
                                () -> openBucket(a, tt.title, "TRIP", tt.items)));
                    }
                    body.addView(hs);
                });
            }

            /* Featured Photos */
            final List<AlbumItem> feat = withPaths(all, FlagStore.favorites(a).all());
            if (feat.isEmpty() && p.snap != null && p.snap.newest != null) feat.add(p.snap.newest);
            if (!feat.isEmpty()) {
                section(s, ui, "featured", "Featured Photos", true,
                        () -> openBucket(a, "Featured Photos", "", feat), null, body -> {
                    HorizontalScrollView hs = hs(a);
                    LinearLayout row = (LinearLayout) hs.getChildAt(0);
                    for (AlbumItem it : feat.subList(0, Math.min(14, feat.size()))) {
                        final AlbumItem fit = it;
                        row.addView(squareThumb(a, s, it,
                                () -> openBucket(a, "Featured Photos", "", feat)));
                    }
                    body.addView(hs);
                });
            }

            /* Media Types */
            section(s, ui, "media", "Media Types", true, null, null, body -> {
                LinkedHashMap<String, List<AlbumItem>> media =
                        p.mediaTypes != null && !p.mediaTypes.isEmpty() ? p.mediaTypes : mediaTypes(all);
                java.util.LinkedHashMap<String, com.absolute.floral.things.MediaRows.Row> rows =
                        new java.util.LinkedHashMap<>();
                for (Map.Entry<String, List<AlbumItem>> e : media.entrySet()) {
                    final String nm = e.getKey();
                    final List<AlbumItem> set = e.getValue();
                    rows.put(nm, new com.absolute.floral.things.MediaRows.Row(
                            set.size(), 0, () -> openBucket(a, nm, "MEDIA TYPE", set)));
                }
                body.addView(com.absolute.floral.things.MediaRows.pillRows(a, s, rows));
            });

            /* Utilities */
            section(s, ui, "utilities", "Utilities", true, null, null, body -> {
                final List<AlbumItem> favs2 = withPaths(all, FlagStore.favorites(a).all());
                final List<AlbumItem> saved = new ArrayList<>();
                long mAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
                for (AlbumItem it : all) { if (it.getDate() < mAgo) break; saved.add(it); }
                final List<AlbumItem> vw = RecentStore.resolve(a, RecentStore.viewed(a), all);
                final List<AlbumItem> sh = RecentStore.resolve(a, RecentStore.shared(a), all);
                java.util.LinkedHashMap<String, com.absolute.floral.things.MediaRows.Row> rows =
                        new java.util.LinkedHashMap<>();
                rows.put("Favorites", new com.absolute.floral.things.MediaRows.Row(favs2.size(), 0,
                        () -> openBucket(a, "Favorites", "", favs2)));
                rows.put("Recently Saved", new com.absolute.floral.things.MediaRows.Row(saved.size(), 0,
                        () -> openBucket(a, "Recently Saved", "", saved)));
                rows.put("Hidden", new com.absolute.floral.things.MediaRows.Row(0, 0,
                        () -> a.startActivity(new Intent(a, PinningActivity.class))));
                rows.put("Duplicates", new com.absolute.floral.things.MediaRows.Row(0, 0,
                        () -> a.startActivity(new Intent(a, CleanupActivity.class))));
                rows.put("Recently Viewed", new com.absolute.floral.things.MediaRows.Row(vw.size(), 0,
                        () -> openBucket(a, "Recently Viewed", "", vw)));
                rows.put("Recently Shared", new com.absolute.floral.things.MediaRows.Row(sh.size(), 0,
                        () -> openBucket(a, "Recently Shared", "", sh)));
                if (android.os.Build.VERSION.SDK_INT >= 30)
                    rows.put("Recently Deleted", new com.absolute.floral.things.MediaRows.Row(0, 0,
                            () -> openBucket(a, "Recently Deleted", "", trashed(a))));
                body.addView(com.absolute.floral.things.MediaRows.pillRows(a, s, rows));
            });

            /* Wallpaper Suggestions — header + chevron only */
            final List<AlbumItem> featured = withPaths(all, FlagStore.favorites(a).all());
            if (!featured.isEmpty()) {
                section(s, ui, "wallpaper", "Wallpaper Suggestions", false,
                        () -> openBucket(a, "Wallpaper Suggestions", "", featured), null, null);
            }

            /* Reorder */
            TextView reorder = new TextView(a);
            reorder.setText("Reorder");
            reorder.setTypeface(Soma.body(a), Typeface.BOLD);
            reorder.setTextSize(15);
            reorder.setTextColor(a.getResources().getColor(R.color.ios_blue));
            reorder.setPadding(dp(a, 18), dp(a, 22), dp(a, 18), dp(a, 8));
            reorder.setOnClickListener(v ->
                    Toast.makeText(a, "Section reordering is coming soon", Toast.LENGTH_SHORT).show());
            col.addView(reorder);

            Anim.enterChildren(col, 20, 22);
        }

        private int locatedCount() {
            int n = 0;
            for (PlacesIndex.Place pl : p.places) n += pl.items.size();
            return n;
        }

        private Runnable pillEdit(Soma s) {
            return () -> Toast.makeText(a, "Editing Pinned is coming soon", Toast.LENGTH_SHORT).show();
        }

        private void renamePerson(final PeopleIndex.Person person) {
            final android.widget.EditText in = new android.widget.EditText(a);
            in.setHint("Name");
            in.setText(person.name == null ? "" : person.name);
            in.setSingleLine(true);
            int pad = dp(a, 20);
            android.widget.FrameLayout wrap = new android.widget.FrameLayout(a);
            wrap.setPadding(pad, dp(a, 8), pad, 0);
            wrap.addView(in);
            new androidx.appcompat.app.AlertDialog.Builder(a)
                    .setTitle("Name this person")
                    .setView(wrap)
                    .setPositiveButton("Save", (d, w) -> {
                        String nm = in.getText().toString().trim();
                        com.absolute.floral.people.PeopleNames.get(a).save(nm, person.embedding());
                        person.name = nm.isEmpty() ? null : nm;
                        signature = -1;
                        scroll.post(this::build);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        /* --------------------------------------------------------- section */

        private interface BodyBuilder { void build(LinearLayout body); }

        private void section(Soma s, SharedPreferences ui, String id, String title,
                             boolean collapsible, Runnable onOpen, Runnable action,
                             BodyBuilder body) {
            final boolean collapsed = collapsible && ui.getBoolean(id, false);

            LinearLayout header = new LinearLayout(a);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(a, 16), dp(a, 22), dp(a, 12), dp(a, 10));

            TextView h = new TextView(a);
            h.setText(title);
            h.setTypeface(Soma.display(a), Typeface.BOLD);
            h.setTextSize(22);
            h.setTextColor(s.ink);
            header.addView(h, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (action != null) {
                TextView act = new TextView(a);
                act.setText("Edit");
                act.setTypeface(Soma.body(a), Typeface.BOLD);
                act.setTextSize(13);
                act.setTextColor(a.getResources().getColor(R.color.ios_blue));
                act.setPadding(dp(a, 8), dp(a, 6), dp(a, 8), dp(a, 6));
                act.setOnClickListener(v -> action.run());
                header.addView(act);
            }

            if (onOpen != null) {
                TextView chev = new TextView(a);
                chev.setText("›");
                chev.setTextSize(22);
                chev.setTextColor(s.inkMute);
                chev.setPadding(dp(a, 6), 0, dp(a, 6), 0);
                header.addView(chev);
                header.setOnClickListener(v -> onOpen.run());
            }

            final TextView toggle;
            if (collapsible) {
                toggle = new TextView(a);
                toggle.setText("⌄");
                toggle.setTextSize(20);
                toggle.setGravity(Gravity.CENTER);
                toggle.setTextColor(s.ink);
                GradientDrawable disc = new GradientDrawable();
                disc.setShape(GradientDrawable.OVAL);
                disc.setColor(s.surfaceStrong);
                toggle.setBackground(disc);
                int d = dp(a, 30);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(d, d);
                tlp.leftMargin = dp(a, 4);
                header.addView(toggle, tlp);
                toggle.setRotation(collapsed ? -90f : 0f);
            } else {
                toggle = null;
            }
            col.addView(header);

            if (body == null) return; // header-only section (Wallpaper Suggestions)

            final LinearLayout bodyBox = new LinearLayout(a);
            bodyBox.setOrientation(LinearLayout.VERTICAL);
            bodyBox.setClipChildren(false);
            col.addView(bodyBox, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final boolean[] built = { false };
            if (!collapsed) {
                body.build(bodyBox);
                built[0] = true;
            } else {
                bodyBox.getLayoutParams().height = 0;
                bodyBox.requestLayout();
            }

            if (toggle != null) {
                toggle.setOnClickListener(v -> {
                    boolean makeCollapsed = !ui.getBoolean(id, false);
                    ui.edit().putBoolean(id, makeCollapsed).apply();
                    if (!built[0]) { body.build(bodyBox); built[0] = true; }
                    animateSection(bodyBox, toggle, makeCollapsed);
                });
            }
        }

        private void animateSection(final View bodyBox, final View toggle, final boolean collapse) {
            bodyBox.measure(
                    View.MeasureSpec.makeMeasureSpec(col.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            final int target = bodyBox.getMeasuredHeight();
            final int from = collapse ? target : 0;
            final int to = collapse ? 0 : target;
            bodyBox.getLayoutParams().height = from;
            bodyBox.requestLayout();
            ValueAnimator va = ValueAnimator.ofInt(from, to);
            va.setDuration(240);
            va.setInterpolator(Anim.ease());
            va.addUpdateListener(anim -> {
                bodyBox.getLayoutParams().height = (int) anim.getAnimatedValue();
                bodyBox.requestLayout();
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if (!collapse) {
                        bodyBox.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        bodyBox.requestLayout();
                    }
                }
            });
            toggle.animate().rotation(collapse ? -90f : 0f).setDuration(240)
                    .setInterpolator(Anim.ease()).start();
            va.start();
        }

        private static int sigOf(Providers p) {
            int n = 0;
            if (p.albums != null) for (Album al : p.albums)
                n = n * 31 + (al.getAlbumItems() == null ? 0 : al.getAlbumItems().size());
            n = n * 31 + (p.memories == null ? 0 : p.memories.size());
            n = n * 31 + (p.people == null ? 0 : p.people.size());
            n = n * 31 + (p.places == null ? 0 : p.places.size());
            if (p.mediaTypes != null) for (List<AlbumItem> v : p.mediaTypes.values())
                n = n * 31 + (v == null ? 0 : v.size());
            return n;
        }

        private LinearLayout tileGrid(Activity a) {
            LinearLayout g = new LinearLayout(a);
            g.setOrientation(LinearLayout.VERTICAL);
            g.setPadding(dp(a, 16), 0, dp(a, 16), 0);
            return g;
        }

        private void addTile(LinearLayout grid, Soma s, View tile) {
            LinearLayout row;
            if (grid.getChildCount() == 0
                    || ((LinearLayout) grid.getChildAt(grid.getChildCount() - 1)).getChildCount() >= 2) {
                row = new LinearLayout(a);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 96));
                rlp.topMargin = dp(a, 9);
                grid.addView(row, rlp);
            } else {
                row = (LinearLayout) grid.getChildAt(grid.getChildCount() - 1);
            }
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tlp.leftMargin = row.getChildCount() == 0 ? 0 : dp(a, 9);
            row.addView(tile, tlp);
        }

        private View tile(Activity a, Soma s, AlbumItem cover, int accent, String label,
                          int count, int icon, Runnable onTap) {
            BentoTile t = new BentoTile(a);
            if (cover != null) t.photo(cover(a, cover), accent);
            else t.gradient(accent);
            t.label(label).sub(count + (count == 1 ? " item" : " items"));
            if (icon != 0) t.icon(icon);
            t.setOnClickListener(v -> onTap.run());
            return t;
        }
    }

    /* ------------------------------------------------------------- entry */

    public static Holder build(Activity a, Providers p) {
        Holder h = new Holder(a);
        h.refresh(p);
        return h;
    }

    /* ------------------------------------------------- ported helpers */

    static HorizontalScrollView hs(Activity a) {
        HorizontalScrollView h = new HorizontalScrollView(a);
        h.setHorizontalScrollBarEnabled(false);
        h.setClipToPadding(false);
        h.setPadding(dp(a, 16), dp(a, 2), dp(a, 16), dp(a, 2));
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        h.addView(row);
        return h;
    }

    static View bigCard(Activity a, Soma s, AlbumItem cover, String title, String sub, Runnable onTap) {
        FrameLayout f = new FrameLayout(a);
        int w = dp(a, 220), ht = dp(a, 150);
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
        if (cover != null) com.bumptech.glide.Glide.with(a).load(cover(a, cover)).centerCrop().into(img);
        f.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrim = new View(a);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ 0x00000000, 0x00000000, 0x99000000 }));
        f.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout cap = new LinearLayout(a);
        cap.setOrientation(LinearLayout.VERTICAL);
        cap.setPadding(dp(a, 12), 0, dp(a, 12), dp(a, 11));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
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

    static View albumCard(Activity a, Soma s, Album al) {
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
        com.bumptech.glide.Glide.with(a).load(cover(a, cov)).centerCrop().into(img);
        box.addView(img, new FrameLayout.LayoutParams(
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

    private static View utilRow(Activity a, Soma s, String label, Runnable onTap, boolean divider) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        if (divider) {
            View d = new View(a);
            d.setBackgroundColor(s.hairline);
            c.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        TextView t = new TextView(a);
        t.setText(label);
        t.setTypeface(Soma.body(a));
        t.setTextSize(15);
        t.setTextColor(s.ink);
        t.setPadding(dp(a, 14), dp(a, 15), dp(a, 14), dp(a, 15));
        t.setOnClickListener(v -> onTap.run());
        c.addView(t);
        return c;
    }

    private static final class Day {
        String label; AlbumItem cover; final List<AlbumItem> items = new ArrayList<>();
    }

    private static List<Day> recentDays(List<AlbumItem> all, int max) {
        LinkedHashMap<String, Day> map = new LinkedHashMap<>();
        long cutoff = System.currentTimeMillis() - 45L * 24 * 3600 * 1000;
        Calendar c = Calendar.getInstance();
        for (AlbumItem it : all) {
            if (it.getDate() <= 0 || it.getDate() < cutoff) break;
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

    static View squareThumb(Activity a, Soma s, AlbumItem it, Runnable onTap) {
        ImageView img = new ImageView(a);
        int d = dp(a, 96);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(d, d);
        ilp.rightMargin = dp(a, 6);
        img.setLayoutParams(ilp);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(s.surfaceStrong);
        img.setClipToOutline(true);
        final float r = dp(a, 12);
        img.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        com.bumptech.glide.Glide.with(a).load(cover(a, it)).centerCrop().into(img);
        img.setOnClickListener(v -> onTap.run());
        return img;
    }

    static List<AlbumItem> flatten(List<Album> albums) {
        List<AlbumItem> l = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        if (albums != null) for (Album al : albums)
            if (al.getAlbumItems() != null)
                for (AlbumItem it : al.getAlbumItems())
                    if (it != null && (it.getPath() == null || seen.add(it.getPath()))) l.add(it);
        Collections.sort(l, (x, q) -> Long.compare(q.getDate(), x.getDate()));
        return l;
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

    static List<AlbumItem> trashed(Activity a) {
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

    static void openBucket(Activity a, String title, String kicker, List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = kicker == null ? "" : kicker;
        BucketActivity.ITEMS = new ArrayList<>(items);
        a.startActivity(new Intent(a, BucketActivity.class));
    }

    static Object cover(Activity a, AlbumItem it) {
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

    static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
