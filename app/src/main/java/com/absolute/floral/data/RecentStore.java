package com.absolute.floral.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.absolute.floral.data.models.AlbumItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Tiny most-recently-used stores backing the Collections ▸ Pinned tiles
 * "Recently Viewed" and "Recently Shared". Ordered path lists, newest first,
 * capped. On-device only.
 */
public final class RecentStore {

    private RecentStore() {}

    private static final String PREFS = "recent_store";
    private static final String K_VIEWED = "viewed";
    private static final String K_SHARED = "shared";
    private static final int CAP = 120;
    private static final String SEP = "\n";

    public static void markViewed(Context c, String path) { push(c, K_VIEWED, java.util.Collections.singletonList(path)); }

    public static void markShared(Context c, List<String> paths) { push(c, K_SHARED, paths); }

    public static List<String> viewed(Context c) { return read(c, K_VIEWED); }

    public static List<String> shared(Context c) { return read(c, K_SHARED); }

    /** Map stored paths to live AlbumItems, preserving recency order. */
    public static List<AlbumItem> resolve(Context c, List<String> paths, List<AlbumItem> pool) {
        List<AlbumItem> out = new ArrayList<>();
        if (paths == null || pool == null) return out;
        java.util.HashMap<String, AlbumItem> byPath = new java.util.HashMap<>();
        for (AlbumItem it : pool) if (it != null && it.getPath() != null) byPath.put(it.getPath(), it);
        for (String p : paths) {
            AlbumItem it = byPath.get(p);
            if (it != null) out.add(it);
        }
        return out;
    }

    private static void push(Context c, String key, List<String> paths) {
        if (paths == null || paths.isEmpty()) return;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String p : paths) if (p != null && !p.isEmpty()) ordered.add(p);
        ordered.addAll(read(c, key));
        List<String> trimmed = new ArrayList<>(ordered);
        if (trimmed.size() > CAP) trimmed = trimmed.subList(0, CAP);
        sp.edit().putString(key, android.text.TextUtils.join(SEP, trimmed)).apply();
    }

    private static List<String> read(Context c, String key) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = sp.getString(key, "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String p : raw.split(SEP)) if (!p.isEmpty()) out.add(p);
        return out;
    }
}
