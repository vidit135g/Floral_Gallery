package com.absolute.floral.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** A simple SharedPreferences-backed set of media paths — used for Favorites and Archive. */
public class FlagStore {

    public static final String FAVORITES = "floral_favorites";
    public static final String ARCHIVE = "floral_archive";

    private final SharedPreferences prefs;
    private final String key;

    private FlagStore(Context c, String name) {
        this.prefs = c.getApplicationContext().getSharedPreferences("floral_flags", Context.MODE_PRIVATE);
        this.key = name;
    }

    public static FlagStore favorites(Context c) { return new FlagStore(c, FAVORITES); }
    public static FlagStore archive(Context c) { return new FlagStore(c, ARCHIVE); }

    public Set<String> all() {
        return new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
    }

    public boolean contains(String path) {
        return path != null && prefs.getStringSet(key, Collections.emptySet()).contains(path);
    }

    public void set(String path, boolean on) {
        if (path == null) return;
        Set<String> s = all();
        if (on) s.add(path); else s.remove(path);
        prefs.edit().putStringSet(key, s).apply();
    }

    public boolean toggle(String path) {
        boolean now = !contains(path);
        set(path, now);
        return now;
    }
}
