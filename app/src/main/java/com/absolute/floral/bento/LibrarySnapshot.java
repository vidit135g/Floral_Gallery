package com.absolute.floral.bento;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.palette.graphics.Palette;

import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** A one-shot computed read of the whole library, for the bento tiles + Insights. */
public class LibrarySnapshot {

    public int photos, videos, albums, favorites, thisWeek, undated;
    public AlbumItem newest;                 // cover for the "recently added" tile
    public AlbumItem randomMemory;           // cover for the memory tile
    public String busiestMonthLabel; public int busiestMonthCount;
    public final int[] months = new int[12]; // photos per calendar month (rolling year -> index by month-of-year)
    public final List<int[]> monthSeries = new ArrayList<>();   // [yearMonthKey, count] recent-first
    public final int[] topColours = new int[5];
    public int topColourCount = 0;
    public long totalBytesEstimate;

    private static LibrarySnapshot cached;
    private static long cachedAt;

    public interface Cb { void onSnapshot(LibrarySnapshot s); }

    public static void get(final Context ctx, final Cb cb) {
        if (cached != null && System.currentTimeMillis() - cachedAt < 60_000L) {
            cb.onSnapshot(cached);
            return;
        }
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            LibrarySnapshot s = compute(app);
            // don't cache an empty read — the media provider may just not be ready yet
            if (s.photos + s.videos + s.albums > 0) {
                cached = s;
                cachedAt = System.currentTimeMillis();
            }
            final LibrarySnapshot out = (s.photos + s.videos + s.albums == 0 && cached != null) ? cached : s;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onSnapshot(out));
        }, "floral-snapshot").start();
    }

    public static LibrarySnapshot cachedOrNull() { return cached; }

    private static LibrarySnapshot compute(Context ctx) {
        LibrarySnapshot s = new LibrarySnapshot();
        List<AlbumItem> all = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        ArrayList<Album> albums = null;
        for (int attempt = 0; attempt < 4 && albums == null; attempt++) {
            try { albums = new ArrayList<>(MediaProvider.getAlbums()); }
            catch (Exception e) { try { Thread.sleep(500); } catch (InterruptedException ignored) {} }
        }
        if (albums == null) albums = new ArrayList<>();
        int realAlbums = 0;
        for (Album a : albums) {
            if (a == null || a.getAlbumItems() == null || a.getAlbumItems().isEmpty()) continue;
            realAlbums++;
            for (AlbumItem it : new ArrayList<>(a.getAlbumItems())) {
                if (it == null || (it.getPath() != null && !seen.add(it.getPath()))) continue;
                all.add(it);
            }
        }
        s.albums = realAlbums;

        java.util.Set<String> favs = FlagStore.favorites(ctx).all();
        Calendar c = Calendar.getInstance();
        long now = System.currentTimeMillis();
        java.util.LinkedHashMap<String, Integer> monthMap = new java.util.LinkedHashMap<>();

        Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));
        for (AlbumItem it : all) {
            boolean v = MediaType.isVideo(it.getPath());
            if (v) s.videos++; else s.photos++;
            if (it.getPath() != null && favs.contains(it.getPath())) s.favorites++;
            long d = it.getDate();
            if (d <= 0) { s.undated++; continue; }
            if (now - d < 7L * 86400_000L) s.thisWeek++;
            c.setTimeInMillis(d);
            s.months[c.get(Calendar.MONTH)]++;
            String key = c.get(Calendar.YEAR) + "-" + String.format(java.util.Locale.US, "%02d", c.get(Calendar.MONTH));
            monthMap.put(key, (monthMap.containsKey(key) ? monthMap.get(key) : 0) + 1);
        }
        if (!all.isEmpty()) {
            s.newest = all.get(0);
            s.randomMemory = all.get((int) (now / 86400000L) % all.size());
        }
        for (java.util.Map.Entry<String, Integer> e : monthMap.entrySet())
            s.monthSeries.add(new int[]{ 0, e.getValue() });
        // busiest month-of-year
        int bi = 0;
        for (int i = 1; i < 12; i++) if (s.months[i] > s.months[bi]) bi = i;
        s.busiestMonthCount = s.months[bi];
        String[] mn = { "January","February","March","April","May","June","July","August","September","October","November","December" };
        s.busiestMonthLabel = mn[bi];

        // top colours — sample up to 24 recent photos
        int[] hueBuckets = new int[12];
        int[] hueSample = new int[12];
        int sampled = 0;
        for (AlbumItem it : all) {
            if (sampled >= 24) break;
            if (MediaType.isVideo(it.getPath())) continue;
            Bitmap bmp = thumb(ctx, it);
            if (bmp == null) continue;
            sampled++;
            try {
                Palette p = Palette.from(bmp).clearFilters().generate();
                Palette.Swatch sw = p.getDominantSwatch();
                if (sw != null) {
                    float[] hsv = new float[3];
                    android.graphics.Color.colorToHSV(sw.getRgb(), hsv);
                    int b = (int) (hsv[0] / 30f) % 12;
                    hueBuckets[b]++;
                    hueSample[b] = android.graphics.Color.HSVToColor(new float[]{ b * 30f + 15f, 0.62f, 0.92f });
                }
            } catch (Exception ignored) {}
            bmp.recycle();
        }
        Integer[] order = new Integer[12];
        for (int i = 0; i < 12; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(hueBuckets[b], hueBuckets[a]));
        for (int i = 0; i < 5; i++) {
            if (hueBuckets[order[i]] > 0) {
                s.topColours[s.topColourCount++] = hueSample[order[i]] == 0
                        ? android.graphics.Color.HSVToColor(new float[]{ order[i] * 30f + 15f, 0.6f, 0.9f })
                        : hueSample[order[i]];
            }
        }
        return s;
    }

    public static Bitmap thumb(Context ctx, AlbumItem it) {
        try {
            android.net.Uri u = it.getUri(ctx);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 8;
            if (u != null) {
                try (java.io.InputStream is = ctx.getContentResolver().openInputStream(u)) {
                    return BitmapFactory.decodeStream(is, null, o);
                }
            }
            return BitmapFactory.decodeFile(it.getPath(), o);
        } catch (Throwable t) { return null; }
    }
}
