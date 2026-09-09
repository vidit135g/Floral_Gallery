package com.absolute.floral.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.absolute.floral.data.models.AlbumItem;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Guest Mode" — hand the phone to someone and only the photos you allowed are
 * visible; leaving it needs the guest PIN. A single global switch plus the
 * allow-list of paths, both persisted. Screens read {@link #active(Context)} and
 * filter their content through {@link #filter}.
 */
public final class GuestMode {

    private static final String PREF = "floral_guest";
    private static final String K_ACTIVE = "active";
    private static final String K_PIN = "pin";          // sha-256 hex
    private static final String K_PATHS = "paths";
    private static final String K_SINCE = "since";

    /** bumped whenever the switch or allow-list changes, so screens can invalidate. */
    public static volatile int version = 0;

    private GuestMode() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /* ---- switch ---- */

    public static boolean active(Context c) { return p(c).getBoolean(K_ACTIVE, false); }

    public static void enter(Context c) {
        p(c).edit().putBoolean(K_ACTIVE, true).putLong(K_SINCE, System.currentTimeMillis()).apply();
        version++;
    }

    public static void exit(Context c) {
        p(c).edit().putBoolean(K_ACTIVE, false).apply();
        version++;
    }

    public static long activeSince(Context c) { return p(c).getLong(K_SINCE, 0L); }

    /* ---- PIN ---- */

    public static boolean hasPin(Context c) { return p(c).contains(K_PIN); }

    public static void setPin(Context c, String pin) {
        p(c).edit().putString(K_PIN, sha(pin)).apply();
    }

    public static void clearPin(Context c) { p(c).edit().remove(K_PIN).apply(); }

    public static boolean checkPin(Context c, String pin) {
        String h = p(c).getString(K_PIN, null);
        return h != null && h.equals(sha(pin));
    }

    private static String sha(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("floral::" + s).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            return sb.toString();
        } catch (Exception e) { return "x" + s.hashCode(); }
    }

    /* ---- allow-list ---- */

    public static Set<String> allowed(Context c) {
        return new HashSet<>(p(c).getStringSet(K_PATHS, Collections.emptySet()));
    }

    public static boolean isAllowed(Context c, String path) {
        return path != null && p(c).getStringSet(K_PATHS, Collections.emptySet()).contains(path);
    }

    public static void setAllowed(Context c, Set<String> paths) {
        p(c).edit().putStringSet(K_PATHS, new HashSet<>(paths)).apply();
        version++;
    }

    public static void addAll(Context c, java.util.Collection<String> paths) {
        Set<String> s = allowed(c);
        s.addAll(paths);
        setAllowed(c, s);
    }

    public static void toggle(Context c, String path) {
        if (path == null) return;
        Set<String> s = allowed(c);
        if (!s.remove(path)) s.add(path);
        setAllowed(c, s);
    }

    /* ---- filtering ---- */

    /** No-op unless Guest Mode is active; otherwise keeps only allowed items. */
    public static List<AlbumItem> filter(Context c, List<AlbumItem> items) {
        if (items == null || !active(c)) return items;
        Set<String> ok = allowed(c);
        ArrayList<AlbumItem> out = new ArrayList<>();
        for (AlbumItem it : items) {
            if (it != null && it.getPath() != null && ok.contains(it.getPath())) out.add(it);
        }
        return out;
    }

    public static boolean blocks(Context c, String path) {
        return active(c) && !isAllowed(c, path);
    }
}
