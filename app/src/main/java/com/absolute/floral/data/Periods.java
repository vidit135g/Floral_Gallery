package com.absolute.floral.data;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups the flat timeline into Apple-Photos-style Month and Year periods. */
public class Periods {

    public static class Period {
        public final String title;      // "September" or "2026"
        public final String subtitle;   // "2026" / "34 items"
        public final long startMs;
        public final int count;
        public final AlbumItem hero;    // representative cover
        public final List<AlbumItem> items;
        Period(String t, String s, long ms, AlbumItem hero, List<AlbumItem> items) {
            this.title = t; this.subtitle = s; this.startMs = ms; this.count = items.size();
            this.hero = hero; this.items = items;
        }
    }

    public static List<Period> months(List<Album> albums) { return group(albums, true); }
    public static List<Period> years(List<Album> albums) { return group(albums, false); }

    private static List<Period> group(List<Album> albums, boolean byMonth) {
        List<AlbumItem> all = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        if (albums != null) for (Album a : albums) if (a.getAlbumItems() != null)
            for (AlbumItem it : a.getAlbumItems())
                if (it != null && (it.getPath() == null || seen.add(it.getPath()))) all.add(it);
        Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));

        String[] mn = { "January","February","March","April","May","June","July",
                "August","September","October","November","December" };
        Calendar c = Calendar.getInstance();
        Map<String, List<AlbumItem>> buckets = new LinkedHashMap<>();
        Map<String, long[]> meta = new LinkedHashMap<>();  // key -> [startMs, year, month]
        for (AlbumItem it : all) {
            long d = it.getDate();
            if (d <= 0) continue;
            c.setTimeInMillis(d);
            int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH);
            String key = byMonth ? (y + "-" + m) : String.valueOf(y);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
            if (!meta.containsKey(key)) meta.put(key, new long[]{ d, y, m });
        }
        List<Period> out = new ArrayList<>();
        for (Map.Entry<String, List<AlbumItem>> e : buckets.entrySet()) {
            List<AlbumItem> items = e.getValue();
            long[] mt = meta.get(e.getKey());
            AlbumItem hero = items.get(items.size() / 2 % items.size());   // a middle-ish shot
            String title, sub;
            if (byMonth) {
                title = mn[(int) mt[2]];
                sub = mt[1] + "  ·  " + items.size();
            } else {
                title = String.valueOf(mt[1]);
                sub = items.size() + (items.size() == 1 ? " item" : " items");
            }
            out.add(new Period(title, sub, mt[0], hero, items));
        }
        return out;
    }
}
