package com.absolute.floral.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.bento.BentoTile;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.things.ThingsIndex;
import com.absolute.floral.util.MediaType;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/** Google-Photos-style search: a discovery panel of People / Places / Things / categories, plus live text search. */
public class SearchActivity extends AppCompatActivity {

    private PhotoGridAdapter adapter;
    private final List<AlbumItem> all = new ArrayList<>();
    private final java.util.Map<AlbumItem, String> owner = new java.util.HashMap<>();
    private Soma soma;

    private ScrollView panel;
    private LinearLayout panelCol;
    private RecyclerView rv;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        soma = SomaSkin.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(soma.ground[1]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, soma);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(d(8), d(44), d(8), d(8));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_arrow_back_white);
        back.setColorFilter(soma.ink);
        int bp = d(10);
        back.setPadding(bp, bp, bp, bp);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(d(44), d(44)));

        LinearLayout fieldWrap = new LinearLayout(this);
        fieldWrap.setOrientation(LinearLayout.HORIZONTAL);
        fieldWrap.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable fg = new android.graphics.drawable.GradientDrawable();
        fg.setColor(soma.surfaceStrong);
        fg.setCornerRadius(d(100));
        fieldWrap.setBackground(fg);
        fieldWrap.setPadding(d(14), d(6), d(14), d(6));
        LinearLayout.LayoutParams fwlp = new LinearLayout.LayoutParams(0, d(44), 1f);
        fwlp.setMargins(d(2), 0, d(6), 0);
        bar.addView(fieldWrap, fwlp);

        ImageView mag = new ImageView(this);
        mag.setImageResource(R.drawable.ic_search_white);
        mag.setColorFilter(soma.inkMute);
        fieldWrap.addView(mag, new LinearLayout.LayoutParams(d(20), d(20)));

        EditText field = new EditText(this);
        field.setHint("Search people, places, things");
        field.setSingleLine(true);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setTypeface(Soma.body(this));
        field.setTextColor(soma.ink);
        field.setHintTextColor(soma.inkMute);
        field.setTextSize(16);
        field.setBackgroundColor(0x00000000);
        field.setPadding(d(10), 0, 0, 0);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            try {
                android.graphics.drawable.GradientDrawable cur = new android.graphics.drawable.GradientDrawable();
                cur.setSize(d(2), 0);
                cur.setColor(soma.accent);
                field.setTextCursorDrawable(cur);
            } catch (Throwable ignored) {}
        }
        fieldWrap.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        FrameLayout body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        panel = new ScrollView(this);
        panel.setClipToPadding(false);
        panel.setPadding(d(14), d(4), d(14), d(28));
        panelCol = new LinearLayout(this);
        panelCol.setOrientation(LinearLayout.VERTICAL);
        panel.addView(panelCol, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(d(6), 0, d(6), d(24));
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);
        adapter = new PhotoGridAdapter(this, emptyTimeline());
        adapter.setSpanCount(3);
        glm.setSpanSizeLookup(adapter.spanSizeLookup());
        rv.setAdapter(adapter);
        rv.setVisibility(View.GONE);
        body.addView(rv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ArrayList<Album> albums = MediaProvider.getAlbumsWithVirtualDirectories(this);
        if (albums != null) for (Album a : albums) if (a.getAlbumItems() != null)
            for (AlbumItem it : a.getAlbumItems()) { all.add(it); owner.put(it, a.getName()); }

        buildPanel();

        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            public void onTextChanged(CharSequence c, int a, int b, int d) { run(c.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        field.requestFocus();
    }

    /* ------------------------------------------------------------ discovery panel */

    private void buildPanel() {
        boolean any = false;

        // People — a strip of circular faces
        List<PeopleIndex.Person> people = PeopleIndex.get().current();
        if (!people.isEmpty()) { addPeople(people); any = true; }
        else PeopleIndex.get().ensure(this, p -> { if (!p.isEmpty()) rebuild(); });

        // Places
        List<PlacesIndex.Place> places = PlacesIndex.get().current();
        if (!places.isEmpty()) { addPlaces(places); any = true; }
        else PlacesIndex.get().ensure(this, p -> { if (!p.isEmpty()) rebuild(); });

        // Things (on-device labels)
        List<ThingsIndex.Thing> things = ThingsIndex.get().current();
        if (!things.isEmpty()) { addThings(things); any = true; }
        else ThingsIndex.get().ensure(this, t -> { if (!t.isEmpty()) rebuild(); });

        addCategories();

        if (!any) {
            TextView hint = new TextView(this);
            hint.setText("Finding people, places and things in your library…");
            hint.setTextColor(soma.inkMute);
            hint.setTextSize(13);
            hint.setPadding(d(4), d(14), d(4), 0);
            panelCol.addView(hint, 0);
        }
        Anim.enterChildren(panelCol);
    }

    private void rebuild() {
        panelCol.removeAllViews();
        buildPanel();
    }

    private TextView header(String t) {
        TextView h = new TextView(this);
        h.setText(t);
        h.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        h.setTextSize(19);
        h.setTextColor(soma.ink);
        h.setPadding(d(4), d(20), d(4), d(10));
        panelCol.addView(h);
        return h;
    }

    private void addPeople(List<PeopleIndex.Person> people) {
        header("People");
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row);
        int i = 0;
        for (PeopleIndex.Person p : people) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, 0, d(14), 0);
            ImageView face = new ImageView(this);
            face.setScaleType(ImageView.ScaleType.CENTER_CROP);
            face.setClipToOutline(true);
            face.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v, android.graphics.Outline o) {
                    o.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            if (p.cover != null) face.setImageBitmap(p.cover);
            else face.setBackgroundColor(soma.surfaceStrong);
            cell.addView(face, new LinearLayout.LayoutParams(d(64), d(64)));
            TextView name = new TextView(this);
            name.setText("Person " + (++i));
            name.setTextColor(soma.inkMute);
            name.setTextSize(11);
            name.setPadding(0, d(5), 0, 0);
            cell.addView(name);
            final int idx = i;
            cell.setOnClickListener(v -> openBucket("Person " + idx, "People", p.photos));
            row.addView(cell);
        }
        panelCol.addView(hs, wrap(d(96)));
    }

    private void addPlaces(List<PlacesIndex.Place> places) {
        header("Places");
        grid2(places.size(), i -> {
            PlacesIndex.Place pl = places.get(i);
            BentoTile t = new BentoTile(this);
            t.photo(cover(pl.cover()), 1 + i).label(pl.label).sub(count(pl.items.size()));
            t.setOnClickListener(v -> openBucket(pl.label, "Places", pl.items));
            return t;
        });
    }

    private void addThings(List<ThingsIndex.Thing> things) {
        header("Things");
        grid2(things.size(), i -> {
            ThingsIndex.Thing th = things.get(i);
            BentoTile t = new BentoTile(this);
            t.photo(cover(th.cover()), 4 + i).label(th.label).sub(count(th.photos.size()));
            t.setOnClickListener(v -> openBucket(th.label, "Things", th.photos));
            return t;
        });
    }

    private void addCategories() {
        header("Browse");
        List<AlbumItem> videos = new ArrayList<>(), shots = new ArrayList<>(), recent = new ArrayList<>();
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        for (AlbumItem it : all) {
            boolean v = MediaType.isVideo(it.getPath());
            if (v) videos.add(it);
            String pth = (it.getPath() == null ? "" : it.getPath()).toLowerCase();
            String nm = (it.getName() == null ? "" : it.getName()).toLowerCase();
            if (pth.contains("screenshot") || nm.contains("screenshot")) shots.add(it);
            if (it.getDate() > weekAgo) recent.add(it);
        }
        List<AlbumItem> favs = new ArrayList<>();
        java.util.Set<String> favPaths = FlagStore.favorites(this).all();
        for (AlbumItem it : all) if (favPaths.contains(it.getPath())) favs.add(it);

        final String[] names = { "Videos", "Screenshots", "Recently added", "Favourites" };
        @SuppressWarnings("unchecked")
        final List<AlbumItem>[] sets = new List[] { videos, shots, recent, favs };
        final int[] accents = { 6, 9, 3, 5 };
        grid2(4, i -> {
            BentoTile t = new BentoTile(this);
            List<AlbumItem> set = sets[i];
            AlbumItem cov = set.isEmpty() ? null : set.get(0);
            if (cov != null) t.photo(cover(cov), accents[i]);
            else t.gradient(accents[i]);
            t.label(names[i]).sub(set.size() + (set.size() == 1 ? " item" : " items"));
            t.setOnClickListener(v -> {
                if (set.isEmpty()) return;
                openBucket(names[i], null, set);
            });
            return t;
        });
    }

    /* ------------------------------------------------------------ helpers */

    private interface TileFactory { BentoTile make(int i); }

    private void grid2(int count, TileFactory f) {
        LinearLayout rowL = null;
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                rowL = new LinearLayout(this);
                rowL.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, d(112));
                rlp.topMargin = d(9);
                panelCol.addView(rowL, rlp);
            }
            BentoTile t = f.make(i);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tlp.setMargins(i % 2 == 0 ? 0 : d(9), 0, 0, 0);
            rowL.addView(t, tlp);
        }
        if (count % 2 == 1 && rowL != null) {
            View spacer = new View(this);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, 1, 1f);
            slp.setMargins(d(9), 0, 0, 0);
            rowL.addView(spacer, slp);
        }
    }

    private LinearLayout.LayoutParams wrap(int h) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h);
        lp.topMargin = d(2);
        return lp;
    }

    private String count(int n) { return n + (n == 1 ? " photo" : " photos"); }

    private Object cover(AlbumItem it) {
        if (it == null) return null;
        Object u = it.getUri(this);
        return u != null ? u : it.getPath();
    }

    private void openBucket(String title, String kicker, List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = kicker == null ? "" : kicker;
        BucketActivity.ITEMS = new ArrayList<>(items);
        startActivity(new android.content.Intent(this, BucketActivity.class));
    }

    /* ------------------------------------------------------------ text search */

    private void run(String q) {
        q = q.trim().toLowerCase();
        if (q.isEmpty()) {
            panel.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            adapter.setTimeline(emptyTimeline());
            return;
        }
        panel.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);

        boolean wantVideo = q.contains("video"), wantPhoto = q.equals("photos") || q.equals("photo") || q.equals("images");
        String thingHit = null;
        for (ThingsIndex.Thing t : ThingsIndex.get().current())
            if (t.label.toLowerCase().contains(q) || q.contains(t.label.toLowerCase())) thingHit = t.label;

        List<AlbumItem> hits = new ArrayList<>();
        if (thingHit != null) {
            for (ThingsIndex.Thing t : ThingsIndex.get().current())
                if (t.label.equals(thingHit)) hits.addAll(t.photos);
        } else {
            for (AlbumItem it : all) {
                boolean v = MediaType.isVideo(it.getPath());
                String name = (it.getName() == null ? "" : it.getName()).toLowerCase();
                String folder = owner.containsKey(it) ? owner.get(it).toLowerCase() : "";
                String month = android.text.format.DateFormat.format("MMMM yyyy",
                        it.getDate() > 0 ? it.getDate() : 0).toString().toLowerCase();
                boolean match = (wantVideo && v) || (wantPhoto && !v)
                        || name.contains(q) || folder.contains(q) || month.contains(q);
                if (match) hits.add(it);
            }
            for (PlacesIndex.Place pl : PlacesIndex.get().current())
                if (pl.label.toLowerCase().contains(q)) for (AlbumItem it : pl.items)
                    if (!hits.contains(it)) hits.add(it);
        }

        Album a = new Album();
        a.setPath("search");
        a.getAlbumItems().addAll(hits);
        ArrayList<Album> bucket = new ArrayList<>();
        bucket.add(a);
        adapter.setTimeline(PhotoTimeline.from(bucket));
    }

    private PhotoTimeline emptyTimeline() { return PhotoTimeline.from(new ArrayList<>()); }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
