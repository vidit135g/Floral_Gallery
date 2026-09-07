package com.absolute.floral.adapter.collections;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.AlbumActivity;
import com.absolute.floral.ui.BucketActivity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/** The Collections tab — Pinned row, People strip, then the album grid. Soma look. */
public class CollectionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int T_PINNED = 0, T_SECTION = 1, T_PEOPLE = 2, T_ALBUM = 3;

    private final Activity a;
    private List<Album> albums = new ArrayList<>();
    private List<PeopleIndex.Person> people = new ArrayList<>();
    private com.absolute.floral.bento.LibrarySnapshot snapshot;
    private final List<Row> rows = new ArrayList<>();

    public void setSnapshot(com.absolute.floral.bento.LibrarySnapshot s) {
        this.snapshot = s;
        if (!rows.isEmpty()) notifyItemChanged(0);
    }

    private static class Row {
        int type; String text; Album album;
        Row(int t) { type = t; }
        Row(int t, String s) { type = t; text = s; }
        Row(Album al) { type = T_ALBUM; album = al; }
    }

    public CollectionsAdapter(Activity a) { this.a = a; }

    public void setData(List<Album> albums) {
        this.albums = albums == null ? new ArrayList<>() : albums;
        rebuild();
    }
    public void setPeople(List<PeopleIndex.Person> p) {
        this.people = p == null ? new ArrayList<>() : p;
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        rows.add(new Row(T_PINNED));
        if (!people.isEmpty()) {
            rows.add(new Row(T_SECTION, "People & pets"));
            rows.add(new Row(T_PEOPLE));
        }
        rows.add(new Row(T_SECTION, "Albums"));
        for (Album al : albums) if (al.getAlbumItems() != null && !al.getAlbumItems().isEmpty())
            rows.add(new Row(al));
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup spanSizeLookup(int span) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return rows.get(position).type == T_ALBUM ? 1 : span;
            }
        };
    }

    @Override public int getItemCount() { return rows.size(); }
    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Soma s = SomaSkin.read(a);
        switch (viewType) {
            case T_PINNED: {
                android.widget.FrameLayout box = new android.widget.FrameLayout(a);
                box.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new VH(box);
            }
            case T_SECTION: return new VH(buildSection(s));
            case T_PEOPLE: return new VH(new PeopleStrip(a));
            default: return new AlbumVH(buildAlbumCard(s));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Row r = rows.get(position);
        Soma s = SomaSkin.read(a);
        if (r.type == T_PINNED) {
            android.widget.FrameLayout box = (android.widget.FrameLayout) h.itemView;
            box.removeAllViews();
            box.addView(buildBento(s));
        } else if (r.type == T_SECTION) {
            ((TextView) h.itemView).setText(r.text);
        } else if (r.type == T_PEOPLE) {
            ((PeopleStrip) h.itemView).bind(people);
        } else if (r.type == T_ALBUM) {
            AlbumVH av = (AlbumVH) h;
            Album al = r.album;
            av.name.setText(al.getName());
            int n = al.getAlbumItems().size();
            av.count.setText(String.valueOf(n));
            AlbumItem cover = al.getAlbumItems().get(0);
            Object t = cover.getUri(a);
            Glide.with(a).load(t != null ? t : cover.getPath()).centerCrop().into(av.image);
            if (s != null) { av.name.setTextColor(s.ink); av.count.setTextColor(s.inkMute); }
            av.itemView.setOnClickListener(v -> {
                Intent i = new Intent(a, AlbumActivity.class);
                i.putExtra(AlbumActivity.ALBUM_PATH, al.getPath());
                a.startActivity(i);
            });
            Anim.item(av.itemView, position, -1);
        }
    }

    /* ---- colourful bento ---- */
    private View buildBento(Soma s) {
        com.absolute.floral.bento.BentoLayout b = new com.absolute.floral.bento.BentoLayout(a);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        com.absolute.floral.bento.LibrarySnapshot snap = snapshot;

        LinearLayout r1 = b.row(108);
        b.tile(r1, 1f, v -> openBucket("Favorites", "", collect(FlagStore.favorites(a).all())))
                .gradient(0).label("Favorites").value(snap != null ? String.valueOf(snap.favorites) : "");
        b.tile(r1, 1f, v -> openBucket("Archive", "", collect(FlagStore.archive(a).all())))
                .gradient(3).label("Archive").value(snap != null && !FlagStore.archive(a).all().isEmpty()
                        ? String.valueOf(FlagStore.archive(a).all().size()) : "");
        b.tile(r1, 1f, v -> openBucket("Trash", "", trashed()))
                .gradient(8).label("Trash").icon(com.absolute.floral.R.drawable.ic_delete_white);

        LinearLayout r2 = b.row(96);
        b.tile(r2, 1.3f, v -> a.startActivity(new Intent(a, com.absolute.floral.ui.InsightsActivity.class)))
                .gradient(4).label("Insights").sub(snap != null
                        ? (snap.photos + snap.videos) + " items" : "Explore your library");
        b.tile(r2, 1f, v -> a.startActivity(new Intent(a, com.absolute.floral.ui.ColorSearchActivity.class)))
                .gradient(9).label("Colours");
        b.tile(r2, 1f, v -> a.startActivity(new Intent(a, com.absolute.floral.ui.InsightsActivity.class)))
                .gradient(6).label("This month").value(snap != null && snap.busiestMonthCount > 0
                        ? String.valueOf(snap.months[java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)]) : "");
        return b;
    }

    private View buildPinned(Soma s) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int p = dp(12);
        row.setPadding(p, dp(4), p, dp(8));
        row.addView(chip(s, com.absolute.floral.R.drawable.ic_star_white, "Favorites", () ->
                openBucket("Favorites", "", collect(FlagStore.favorites(a).all()))));
        row.addView(chip(s, com.absolute.floral.R.drawable.ic_archive_white, "Archive", () ->
                openBucket("Archive", "", collect(FlagStore.archive(a).all()))));
        row.addView(chip(s, com.absolute.floral.R.drawable.ic_delete_white, "Trash", () ->
                openBucket("Trash", "", trashed())));
        return row;
    }

    private View chip(Soma s, int iconRes, String label, Runnable onTap) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(6), dp(16), dp(6), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        c.setLayoutParams(lp);
        GradientDrawable g = new GradientDrawable();
        g.setColor(s.surfaceStrong);
        g.setCornerRadius(dp(16));
        c.setBackground(g);
        c.setClickable(true);
        applyRipple(c);
        c.setOnClickListener(v -> onTap.run());

        ImageView icon = new ImageView(a);
        icon.setImageResource(iconRes);
        icon.setColorFilter(s.ink);
        int d = dp(24);
        c.addView(icon, new LinearLayout.LayoutParams(d, d));

        TextView t = new TextView(a);
        t.setText(label);
        t.setTextSize(12);
        t.setTypeface(Soma.body(a));
        t.setTextColor(s.inkSoft);
        t.setPadding(0, dp(8), 0, 0);
        c.addView(t);
        return c;
    }

    private void applyRipple(View v) {
        if (android.os.Build.VERSION.SDK_INT < 23) return;
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            v.setForeground(androidx.core.content.ContextCompat.getDrawable(a, tv.resourceId));
        } catch (Exception ignored) {}
    }

    private View buildSection(Soma s) {
        TextView t = new TextView(a);
        t.setTypeface(Soma.display(a), android.graphics.Typeface.BOLD);
        t.setTextSize(22);
        t.setTextColor(s.ink);
        int p = dp(16);
        t.setPadding(p, dp(22), p, dp(10));
        return t;
    }

    private View buildAlbumCard(Soma s) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), dp(8), dp(8), dp(12));
        card.setLayoutParams(lp);
        card.setClickable(true);
        applyRipple(card);

        com.absolute.floral.ui.widget.SquareFrameLayout coverBox =
                new com.absolute.floral.ui.widget.SquareFrameLayout(a);
        coverBox.setClipToOutline(true);
        final int r = dp(14);
        coverBox.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        ImageView img = new ImageView(a);
        img.setId(android.R.id.icon);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(s.surfaceStrong);
        coverBox.addView(img, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(coverBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView name = new TextView(a);
        name.setId(android.R.id.text1);
        name.setTypeface(Soma.display(a));
        name.setTextSize(14);
        name.setTextColor(s.ink);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(dp(2), dp(8), dp(2), 0);
        card.addView(name);

        TextView count = new TextView(a);
        count.setId(android.R.id.text2);
        count.setTypeface(Soma.body(a));
        count.setTextSize(12);
        count.setTextColor(s.inkMute);
        count.setPadding(dp(2), dp(1), dp(2), 0);
        card.addView(count);
        return card;
    }

    /* ---- helpers ---- */
    private List<AlbumItem> collect(java.util.Set<String> paths) {
        List<AlbumItem> out = new ArrayList<>();
        for (Album al : albums) if (al.getAlbumItems() != null)
            for (AlbumItem it : al.getAlbumItems())
                if (it.getPath() != null && paths.contains(it.getPath())) out.add(it);
        return out;
    }

    private List<AlbumItem> trashed() {
        List<AlbumItem> out = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < 30) return out;
        try {
            android.os.Bundle q = new android.os.Bundle();
            q.putInt(android.provider.MediaStore.QUERY_ARG_MATCH_TRASHED,
                    android.provider.MediaStore.MATCH_ONLY);
            android.database.Cursor c = a.getContentResolver().query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{ android.provider.MediaStore.Images.Media._ID },
                    q, null);
            if (c != null) {
                while (c.moveToNext()) {
                    android.net.Uri u = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0));
                    out.add(AlbumItem.getInstance(a, u));
                }
                c.close();
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private void openBucket(String title, String kicker, List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = kicker;
        BucketActivity.ITEMS = items;
        a.startActivity(new Intent(a, BucketActivity.class));
    }

    private int dp(float v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }

    static class VH extends RecyclerView.ViewHolder { VH(View v) { super(v); } }
    static class AlbumVH extends RecyclerView.ViewHolder {
        final ImageView image; final TextView name, count;
        AlbumVH(View v) {
            super(v);
            image = v.findViewById(android.R.id.icon);
            name = v.findViewById(android.R.id.text1);
            count = v.findViewById(android.R.id.text2);
        }
    }

    /* ---- People strip ---- */
    class PeopleStrip extends HorizontalScrollView {
        private final LinearLayout row;
        PeopleStrip(Activity a) {
            super(a);
            setHorizontalScrollBarEnabled(false);
            int p = dp(10);
            setPadding(p, dp(4), p, dp(10));
            row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            addView(row);
            setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        void bind(List<PeopleIndex.Person> people) {
            row.removeAllViews();
            Soma s = SomaSkin.read(a);
            int i = 1;
            for (PeopleIndex.Person p : people) {
                LinearLayout col = new LinearLayout(a);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setGravity(Gravity.CENTER_HORIZONTAL);
                col.setPadding(dp(8), 0, dp(8), 0);
                ImageView face = new ImageView(a);
                int d = dp(64);
                LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(d, d);
                face.setLayoutParams(flp);
                face.setScaleType(ImageView.ScaleType.CENTER_CROP);
                face.setClipToOutline(true);
                face.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override public void getOutline(View v, android.graphics.Outline o) {
                        o.setOval(0, 0, v.getWidth(), v.getHeight());
                    }
                });
                if (p.cover != null) face.setImageBitmap(p.cover);
                col.addView(face);
                TextView lbl = new TextView(a);
                lbl.setText("Person " + (i++));
                lbl.setTextSize(11);
                lbl.setTypeface(Soma.body(a));
                if (s != null) lbl.setTextColor(s.inkSoft);
                lbl.setPadding(0, dp(6), 0, 0);
                col.addView(lbl);
                final PeopleIndex.Person pp = p;
                final int num = i - 1;
                col.setOnClickListener(v -> openBucket("Person " + num, "PEOPLE", new ArrayList<>(pp.photos)));
                row.addView(col);
            }
        }
    }
}
