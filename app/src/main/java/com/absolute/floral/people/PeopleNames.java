package com.absolute.floral.people;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Remembers the names a user assigns to detected people. Face clusters get a new
 * "person_N" id on every rescan, so names are keyed by the cluster's <b>embedding
 * centroid</b> instead — on each scan we re-attach a saved name to whichever
 * fresh cluster its centroid best matches (cosine ≥ 0.86).
 *
 * Storage: one pref entry per person, value = "name\tv0,v1,v2,…".
 */
public final class PeopleNames {

    private static PeopleNames INSTANCE;
    public static PeopleNames get(Context c) {
        if (INSTANCE == null) INSTANCE = new PeopleNames(c.getApplicationContext());
        return INSTANCE;
    }

    private static final String PREFS = "people_names";
    private static final float MATCH = 0.86f;

    private final SharedPreferences sp;

    private PeopleNames(Context c) { sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static final class Entry { String key; String name; float[] vec; }

    private List<Entry> load() {
        List<Entry> out = new ArrayList<>();
        for (String key : sp.getAll().keySet()) {
            String v = sp.getString(key, null);
            if (v == null) continue;
            int tab = v.indexOf('\t');
            if (tab < 0) continue;
            Entry e = new Entry();
            e.key = key;
            e.name = v.substring(0, tab);
            String[] parts = v.substring(tab + 1).split(",");
            e.vec = new float[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) e.vec[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException ex) { continue; }
            out.add(e);
        }
        return out;
    }

    /** Best saved name for this centroid, or null. */
    public String nameFor(float[] centroid) {
        if (centroid == null) return null;
        String best = null; float bestSim = MATCH;
        for (Entry e : load()) {
            float s = cosine(e.vec, centroid);
            if (s >= bestSim) { bestSim = s; best = e.name; }
        }
        return best;
    }

    /** Assign (or clear, when name is blank) a name for the cluster with this centroid. */
    public void save(String name, float[] centroid) {
        if (centroid == null) return;
        SharedPreferences.Editor ed = sp.edit();
        for (Entry e : load())
            if (cosine(e.vec, centroid) >= MATCH) ed.remove(e.key);
        if (name != null && !name.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder(name.trim().replace('\t', ' ')).append('\t');
            for (int i = 0; i < centroid.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(centroid[i]);
            }
            ed.putString("p_" + System.currentTimeMillis(), sb.toString());
        }
        ed.apply();
    }

    private static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
