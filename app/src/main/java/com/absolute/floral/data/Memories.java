package com.absolute.floral.data;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Auto-generated "Memories" — the carousel at the top of the Photos tab. */
public class Memories {

    public static class Memory {
        public final String title;
        public final String kicker;
        public final List<AlbumItem> items;
        Memory(String kicker, String title, List<AlbumItem> items) {
            this.kicker = kicker; this.title = title; this.items = items;
        }
        public AlbumItem cover() { return items.isEmpty() ? null : items.get(0); }
    }

    public static List<Memory> build(List<Album> albums) {
        List<AlbumItem> all = new ArrayList<>();
        if (albums != null) for (Album a : albums) if (a.getAlbumItems() != null) all.addAll(a.getAlbumItems());
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        List<AlbumItem> uniq = new ArrayList<>();
        for (AlbumItem it : all) if (it.getPath() == null || seen.add(it.getPath())) uniq.add(it);
        Collections.sort(uniq, (x, y) -> Long.compare(y.getDate(), x.getDate()));

        List<Memory> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();

        // Recently added — last 30 days
        List<AlbumItem> recent = new ArrayList<>();
        for (AlbumItem it : uniq) {
            long d = it.getDate();
            if (d > 0 && now - d < 30L * 86400_000L) recent.add(it);
        }
        if (recent.size() >= 3)
            out.add(new Memory("RECENT", "The last few weeks", cap(recent, 30)));

        // On this day — same month+day, previous years
        c.setTimeInMillis(now);
        int tm = c.get(Calendar.MONTH), td = c.get(Calendar.DAY_OF_MONTH), ty = c.get(Calendar.YEAR);
        List<AlbumItem> onThisDay = new ArrayList<>();
        for (AlbumItem it : uniq) {
            long d = it.getDate(); if (d <= 0) continue;
            c.setTimeInMillis(d);
            if (c.get(Calendar.MONTH) == tm && Math.abs(c.get(Calendar.DAY_OF_MONTH) - td) <= 2
                    && c.get(Calendar.YEAR) < ty) onThisDay.add(it);
        }
        if (onThisDay.size() >= 2)
            out.add(new Memory("ON THIS DAY", "Around this time", cap(onThisDay, 30)));

        // Best of <Month> — months with >= 4 photos
        Map<String, List<AlbumItem>> byMonth = new LinkedHashMap<>();
        for (AlbumItem it : uniq) {
            long d = it.getDate(); if (d <= 0) continue;
            c.setTimeInMillis(d);
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH);
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
        }
        int added = 0;
        for (Map.Entry<String, List<AlbumItem>> e : byMonth.entrySet()) {
            if (e.getValue().size() < 4) continue;
            c.setTimeInMillis(e.getValue().get(0).getDate());
            String month = android.text.format.DateFormat.format("MMMM yyyy", c).toString();
            out.add(new Memory("HIGHLIGHTS", "Best of " + month, cap(e.getValue(), 40)));
            if (++added >= 4) break;
        }

        if (out.isEmpty() && uniq.size() >= 3)
            out.add(new Memory("YOUR LIBRARY", "Everything, at a glance", cap(uniq, 40)));
        return out;
    }

    private static List<AlbumItem> cap(List<AlbumItem> l, int n) {
        return l.size() <= n ? new ArrayList<>(l) : new ArrayList<>(l.subList(0, n));
    }
}
