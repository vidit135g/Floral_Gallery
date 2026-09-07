package com.absolute.floral.ui;

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Segmented;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Free up space — screenshots, large videos, near-duplicate photos. Multi-select + delete. */
public class CleanupActivity extends AppCompatActivity {

    static class Item {
        Uri uri; long id; long size; String path;
    }

    private Soma s;
    private final List<Item> screenshots = new ArrayList<>();
    private final List<Item> largeVideos = new ArrayList<>();
    private final List<List<Item>> dupeGroups = new ArrayList<>();
    private final List<Item> dupeFlat = new ArrayList<>();

    private int tab = 0;
    private final Set<Long> selected = new LinkedHashSet<>();
    private RecyclerView rv;
    private TextView status, deleteBtn;
    private GridAdapter adapter;

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        s = SomaSkin.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(s.ground[0]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, s);

        int p = d(16);
        TextView title = new TextView(this);
        title.setText("Free up space");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(28);
        title.setTextColor(s.ink);
        title.setPadding(p + d(4), d(28), p, d(8));
        root.addView(title);
        Anim.enter(title, 20);

        Segmented seg = new Segmented(this);
        seg.setItems("Screenshots", "Large", "Look-alikes");
        seg.setSoma(s);
        seg.select(0, false);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, d(40));
        glp.setMargins(p, 0, p, d(6));
        root.addView(seg, glp);
        seg.setOnChange(i -> { tab = i; selected.clear(); refresh(); });

        status = new TextView(this);
        status.setText("Scanning your library…");
        status.setTextColor(s.inkMute);
        status.setTextSize(12);
        status.setPadding(p + d(4), d(2), p, d(6));
        root.addView(status);

        rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(d(4), 0, d(4), d(8));
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);
        adapter = new GridAdapter();
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        root.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        deleteBtn = new TextView(this);
        deleteBtn.setGravity(Gravity.CENTER);
        deleteBtn.setTypeface(Soma.body(this), android.graphics.Typeface.BOLD);
        deleteBtn.setTextSize(15);
        deleteBtn.setTextColor(0xFFFFFFFF);
        deleteBtn.setPadding(0, d(15), 0, d(15));
        GradientDrawable db = new GradientDrawable();
        db.setColor(0xFFE5484D);
        db.setCornerRadius(d(16));
        deleteBtn.setBackground(db);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.setMargins(p, 0, p, d(24));
        deleteBtn.setLayoutParams(dlp);
        deleteBtn.setVisibility(View.GONE);
        deleteBtn.setOnClickListener(v -> doDelete());
        root.addView(deleteBtn);

        scan();
    }

    private void scan() {
        new Thread(() -> {
            scanScreenshots();
            scanLargeVideos();
            scanDupes();
            runOnUiThread(this::refresh);
        }, "floral-cleanup").start();
    }

    private void scanScreenshots() {
        String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH };
        try (Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (c == null) return;
            int idc = c.getColumnIndex(MediaStore.Images.Media._ID);
            int sc = c.getColumnIndex(MediaStore.Images.Media.SIZE);
            int dc = c.getColumnIndex(MediaStore.Images.Media.DATA);
            int nc = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int rc = c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH);
            while (c.moveToNext()) {
                String name = nc >= 0 ? c.getString(nc) : "";
                String rel = rc >= 0 && !c.isNull(rc) ? c.getString(rc) : "";
                String data = dc >= 0 ? c.getString(dc) : "";
                boolean isShot = (name != null && name.toLowerCase().startsWith("screenshot"))
                        || (rel != null && rel.toLowerCase().contains("screenshot"))
                        || (data != null && data.toLowerCase().contains("/screenshot"));
                if (!isShot) continue;
                Item it = new Item();
                it.id = c.getLong(idc);
                it.size = sc >= 0 ? c.getLong(sc) : 0;
                it.path = data;
                it.uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id);
                screenshots.add(it);
            }
        } catch (Exception ignored) {}
    }

    private void scanLargeVideos() {
        String[] proj = { MediaStore.Video.Media._ID, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATA };
        try (Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, MediaStore.Video.Media.SIZE + " DESC")) {
            if (c == null) return;
            int idc = c.getColumnIndex(MediaStore.Video.Media._ID);
            int sc = c.getColumnIndex(MediaStore.Video.Media.SIZE);
            int dc = c.getColumnIndex(MediaStore.Video.Media.DATA);
            while (c.moveToNext()) {
                long size = sc >= 0 ? c.getLong(sc) : 0;
                if (size < 20L * 1024 * 1024) continue;   // > 20 MB
                Item it = new Item();
                it.id = c.getLong(idc);
                it.size = size;
                it.path = dc >= 0 ? c.getString(dc) : "";
                it.uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it.id);
                largeVideos.add(it);
                if (largeVideos.size() >= 60) break;
            }
        } catch (Exception ignored) {}
    }

    private void scanDupes() {
        // dHash the 400 most-recent photos and group by identical hash
        String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE };
        java.util.LinkedHashMap<Long, List<Item>> byHash = new java.util.LinkedHashMap<>();
        try (Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (c == null) return;
            int idc = c.getColumnIndex(MediaStore.Images.Media._ID);
            int sc = c.getColumnIndex(MediaStore.Images.Media.SIZE);
            int scanned = 0;
            while (c.moveToNext() && scanned++ < 400) {
                long id = c.getLong(idc);
                Uri u = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                Long h = dhash(u);
                if (h == null) continue;
                Item it = new Item();
                it.id = id; it.size = sc >= 0 ? c.getLong(sc) : 0; it.uri = u;
                byHash.computeIfAbsent(h, k -> new ArrayList<>()).add(it);
            }
        } catch (Exception ignored) {}
        for (List<Item> g : byHash.values()) {
            if (g.size() >= 2) { dupeGroups.add(g); dupeFlat.addAll(g); }
        }
    }

    private Long dhash(Uri u) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 16;
            Bitmap bmp;
            try (java.io.InputStream is = getContentResolver().openInputStream(u)) {
                bmp = BitmapFactory.decodeStream(is, null, o);
            }
            if (bmp == null) return null;
            Bitmap g = Bitmap.createScaledBitmap(bmp, 9, 8, true);
            bmp.recycle();
            long hash = 0; int bit = 0;
            for (int y = 0; y < 8; y++) {
                int prev = lum(g.getPixel(0, y));
                for (int x = 1; x < 9; x++) {
                    int cur = lum(g.getPixel(x, y));
                    if (cur > prev) hash |= (1L << bit);
                    bit++; prev = cur;
                }
            }
            g.recycle();
            return hash;
        } catch (Throwable t) { return null; }
    }
    private int lum(int c) { return (int) (0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255)); }

    private List<Item> current() {
        return tab == 0 ? screenshots : tab == 1 ? largeVideos : dupeFlat;
    }

    private void refresh() {
        List<Item> l = current();
        long total = 0; for (Item it : l) if (selected.contains(it.id)) total += it.size;
        long allSize = 0; for (Item it : l) allSize += it.size;
        String label = tab == 0 ? l.size() + " screenshots"
                : tab == 1 ? l.size() + " large videos"
                : dupeGroups.size() + " look-alike groups";
        status.setText(label + "  ·  " + mb(allSize) + " total");
        adapter.notifyDataSetChanged();
        if (selected.isEmpty()) deleteBtn.setVisibility(View.GONE);
        else {
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setText("Delete " + selected.size() + "  ·  free ~" + mb(total));
        }
    }

    private String mb(long bytes) {
        if (bytes > 1024L * 1024 * 1024) return String.format(java.util.Locale.US, "%.1f GB", bytes / 1024f / 1024f / 1024f);
        return Math.round(bytes / 1024f / 1024f) + " MB";
    }

    private void doDelete() {
        List<Uri> uris = new ArrayList<>();
        for (Item it : current()) if (selected.contains(it.id)) uris.add(it.uri);
        if (uris.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), uris);
                startIntentSenderForResult(pi.getIntentSender(), 99, null, 0, 0, 0);
                return;
            } catch (Exception e) { e.printStackTrace(); }
        }
        int n = 0;
        for (Uri u : uris) { try { n += getContentResolver().delete(u, null, null); } catch (Exception ignored) {} }
        Toast.makeText(this, "Deleted " + n, Toast.LENGTH_SHORT).show();
        purge();
    }

    @Override protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 99 && res == RESULT_OK) {
            Toast.makeText(this, "Space freed", Toast.LENGTH_SHORT).show();
            purge();
        }
    }

    private void purge() {
        removeSelected(screenshots);
        removeSelected(largeVideos);
        removeSelected(dupeFlat);
        java.util.Iterator<List<Item>> gi = dupeGroups.iterator();
        while (gi.hasNext()) {
            List<Item> g = gi.next();
            removeSelected(g);
            if (g.size() < 2) gi.remove();
        }
        selected.clear();
        refresh();
    }

    private void removeSelected(List<Item> l) {
        java.util.Iterator<Item> it = l.iterator();
        while (it.hasNext()) if (selected.contains(it.next().id)) it.remove();
    }

    /* ---- grid ---- */
    class GridAdapter extends RecyclerView.Adapter<GridAdapter.VH> {
        @Override public int getItemCount() { return current().size(); }
        @Override public VH onCreateViewHolder(ViewGroup parent, int vt) {
            FrameLayout box = new FrameLayout(CleanupActivity.this);
            box.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, d(120)));
            int m = d(2);
            box.setPadding(m, m, m, m);
            ImageView img = new ImageView(CleanupActivity.this);
            img.setId(android.R.id.icon);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setBackgroundColor(s.surfaceStrong);
            box.addView(img, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            View check = new View(CleanupActivity.this);
            check.setId(android.R.id.checkbox);
            GradientDrawable cd = new GradientDrawable();
            cd.setColor(0xCC1A73E8);
            check.setBackground(cd);
            box.addView(check, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            TextView sz = new TextView(CleanupActivity.this);
            sz.setId(android.R.id.text1);
            sz.setTextColor(0xFFFFFFFF);
            sz.setTextSize(10);
            sz.setPadding(d(4), d(2), d(4), d(2));
            sz.setShadowLayer(d(3), 0, d(1), 0x99000000);
            FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tl.gravity = Gravity.BOTTOM | Gravity.START;
            box.addView(sz, tl);
            return new VH(box, img, check, sz);
        }
        @Override public void onBindViewHolder(VH h, int position) {
            Item it = current().get(position);
            Glide.with(CleanupActivity.this).load(it.uri).centerCrop().into(h.img);
            h.sz.setText(mb(it.size));
            boolean sel = selected.contains(it.id);
            h.check.setVisibility(sel ? View.VISIBLE : View.GONE);
            h.itemView.setScaleX(sel ? 0.88f : 1f);
            h.itemView.setScaleY(sel ? 0.88f : 1f);
            h.itemView.setOnClickListener(v -> {
                if (!selected.remove(it.id)) selected.add(it.id);
                refresh();
            });
        }
        class VH extends RecyclerView.ViewHolder {
            final ImageView img; final View check; final TextView sz;
            VH(View v, ImageView i, View c, TextView s2) { super(v); img = i; check = c; sz = s2; }
        }
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
