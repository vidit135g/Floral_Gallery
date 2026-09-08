package com.absolute.floral.bento;

import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.places.PlacesIndex;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Trips" — Apple-Photos groups geotagged photos into outings. We approximate:
 * take every located place, bucket its photos by day, then greedily merge days
 * that fall within a 4-day window into one Trip. Title = the places visited.
 */
public final class Trips {

    private Trips() {}

    public static class Trip {
        public String title;
        public long start, end;
        public final List<AlbumItem> items = new ArrayList<>();
        public AlbumItem cover() { return items.isEmpty() ? null : items.get(0); }
    }

    private static final long WINDOW = 4L * 24 * 3600 * 1000;

    public static List<Trip> from(List<PlacesIndex.Place> places) {
        List<Trip> out = new ArrayList<>();
        if (places == null || places.isEmpty()) return out;

        // one (place, dayItems) entry per place per day it has photos
        List<Object[]> segments = new ArrayList<>(); // { long dayStart, String label, List<AlbumItem> }
        Calendar c = Calendar.getInstance();
        for (PlacesIndex.Place p : places) {
            LinkedHashMap<Long, List<AlbumItem>> byDay = new LinkedHashMap<>();
            for (AlbumItem it : p.items) {
                if (it == null || it.getDate() <= 0) continue;
                c.setTimeInMillis(it.getDate());
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                long day = c.getTimeInMillis();
                List<AlbumItem> l = byDay.get(day);
                if (l == null) { l = new ArrayList<>(); byDay.put(day, l); }
                l.add(it);
            }
            for (Map.Entry<Long, List<AlbumItem>> e : byDay.entrySet())
                segments.add(new Object[]{ e.getKey(), p.label, e.getValue() });
        }
        if (segments.isEmpty()) return out;
        Collections.sort(segments, (x, y) -> Long.compare((Long) x[0], (Long) y[0]));

        Trip cur = null;
        java.util.LinkedHashSet<String> labels = new java.util.LinkedHashSet<>();
        for (Object[] seg : segments) {
            long day = (Long) seg[0];
            String label = (String) seg[1];
            @SuppressWarnings("unchecked")
            List<AlbumItem> its = (List<AlbumItem>) seg[2];
            if (cur == null || day - cur.end > WINDOW) {
                cur = finish(cur, labels, out);
                cur = new Trip();
                cur.start = day;
                labels = new java.util.LinkedHashSet<>();
            }
            cur.end = day;
            cur.items.addAll(its);
            if (label != null && !label.isEmpty()) labels.add(label);
        }
        finish(cur, labels, out);

        // newest trip first; keep the ones that actually look like an outing
        List<Trip> keep = new ArrayList<>();
        for (Trip t : out) if (t.items.size() >= 4) keep.add(t);
        Collections.sort(keep, (x, y) -> Long.compare(y.end, x.end));
        return keep;
    }

    private static Trip finish(Trip cur, java.util.LinkedHashSet<String> labels, List<Trip> out) {
        if (cur == null) return null;
        Collections.sort(cur.items, (x, y) -> Long.compare(y.getDate(), x.getDate()));
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String l : labels) {
            if (n == 2) { sb.append(" & more"); break; }
            if (n > 0) sb.append(" · ");
            sb.append(l);
            n++;
        }
        cur.title = sb.length() == 0 ? "Trip" : sb.toString();
        out.add(cur);
        return cur;
    }
}
