package com.absolute.floral.data;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * A flat, date-descending view over every photo/video on the device — the
 * Google-Photos "Photos" tab. Built by flattening the folder albums.
 */
public class PhotoTimeline {

    /** A row in the timeline: either a date header or a media item. */
    public static class Row {
        public final boolean header;
        public final String title;      // header only
        public final AlbumItem item;    // item only
        public final String albumPath;  // item only — owning folder
        Row(String title) { this.header = true; this.title = title; this.item = null; this.albumPath = null; }
        Row(AlbumItem it, String path) { this.header = false; this.title = null; this.item = it; this.albumPath = path; }
    }

    public final List<Row> rows = new ArrayList<>();
    public final List<AlbumItem> items = new ArrayList<>();   // items only, same order

    public static PhotoTimeline from(List<Album> albums) {
        PhotoTimeline t = new PhotoTimeline();
        List<Entry> all = new ArrayList<>();
        if (albums != null) {
            for (Album a : albums) {
                if (a == null || a.getAlbumItems() == null) continue;
                for (AlbumItem it : a.getAlbumItems()) {
                    if (it != null) all.add(new Entry(it, a.getPath()));
                }
            }
        }
        // de-dupe by path (virtual albums repeat items) keeping first
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        List<Entry> uniq = new ArrayList<>();
        for (Entry e : all) if (e.item.getPath() == null || seen.add(e.item.getPath())) uniq.add(e);

        Collections.sort(uniq, (x, y) -> Long.compare(y.item.getDate(), x.item.getDate()));

        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        String lastBucket = null;
        for (Entry e : uniq) {
            long d = e.item.getDate();
            cal.setTimeInMillis(d > 0 ? d : now);
            // recent photos bucket by day; older than ~3 weeks bucket by month (fuller rows, like GP)
            boolean byMonth = d > 0 && now - d > 21L * 86400_000L;
            String bucket = byMonth
                    ? "m" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH)
                    : "d" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
            if (!bucket.equals(lastBucket)) {
                t.rows.add(new Row(headerLabel(cal, d, byMonth)));
                lastBucket = bucket;
            }
            t.rows.add(new Row(e.item, e.albumPath));
            t.items.add(e.item);
        }
        return t;
    }

    /**
     * A continuous (header-free) timeline over an already-ordered item list — the
     * Apple-Photos iOS 18 Library grid, and any filtered view.
     */
    public static PhotoTimeline flat(List<AlbumItem> items, java.util.Map<String, String> pathAlbum) {
        PhotoTimeline t = new PhotoTimeline();
        if (items == null) return t;
        for (AlbumItem it : items) {
            if (it == null) continue;
            String path = it.getPath();
            String album = pathAlbum == null || path == null ? null : pathAlbum.get(path);
            t.rows.add(new Row(it, album));
            t.items.add(it);
        }
        return t;
    }

    /** A copy with the given item paths removed (headers left even if now empty — harmless). */
    public PhotoTimeline without(java.util.Set<String> paths) {
        PhotoTimeline t = new PhotoTimeline();
        if (paths == null || paths.isEmpty()) {
            t.rows.addAll(rows); t.items.addAll(items); return t;
        }
        for (Row r : rows) {
            if (!r.header && r.item != null && r.item.getPath() != null
                    && paths.contains(r.item.getPath())) continue;
            t.rows.add(r);
            if (!r.header && r.item != null) t.items.add(r.item);
        }
        return t;
    }

    private static String headerLabel(Calendar cal, long dateMs, boolean byMonth) {
        if (dateMs <= 0) return "Undated";
        Calendar now = Calendar.getInstance();
        boolean sameYear = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR);
        if (byMonth) {
            return android.text.format.DateFormat.format(
                    sameYear ? "MMMM" : "MMMM yyyy", cal).toString();
        }
        int dyDiff = sameYear ? now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) : 999;
        if (dyDiff == 0) return "Today";
        if (dyDiff == 1) return "Yesterday";
        CharSequence fmt = sameYear ? android.text.format.DateFormat.format("EEEE, d MMMM", cal)
                : android.text.format.DateFormat.format("d MMMM yyyy", cal);
        return fmt.toString();
    }

    private static class Entry {
        final AlbumItem item; final String albumPath;
        Entry(AlbumItem i, String p) { item = i; albumPath = p; }
    }
}
