package com.absolute.floral.things;

import android.content.Context;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Buckets the library into Apple-Photos "Media Types" — done on-device from
 * path / filename / MIME signals only (no per-file I/O), so it's cheap. Follows
 * the {@link com.absolute.floral.people.PeopleIndex} lifecycle contract.
 */
public class MediaTypeIndex {

    public interface Listener { void onReady(LinkedHashMap<String, List<AlbumItem>> types); }

    private static MediaTypeIndex INSTANCE;
    public static MediaTypeIndex get() { return INSTANCE == null ? (INSTANCE = new MediaTypeIndex()) : INSTANCE; }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private LinkedHashMap<String, List<AlbumItem>> types = new LinkedHashMap<>();
    private long builtAt;

    public LinkedHashMap<String, List<AlbumItem>> current() { return types; }

    public void ensure(Context ctx, Listener cb) {
        if (!types.isEmpty() && System.currentTimeMillis() - builtAt < 5 * 60_000L) {
            cb.onReady(types);
            return;
        }
        if (running.getAndSet(true)) { cb.onReady(types); return; }
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                LinkedHashMap<String, List<AlbumItem>> r = scan();
                types = r;
                builtAt = System.currentTimeMillis();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onReady(types));
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                running.set(false);
            }
        }, "floral-mediatypes").start();
    }

    private LinkedHashMap<String, List<AlbumItem>> scan() {
        List<AlbumItem> all = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        ArrayList<Album> al = null;
        for (int i = 0; i < 4 && al == null; i++) {
            try { al = new ArrayList<>(MediaProvider.getAlbums()); }
            catch (Exception e) { try { Thread.sleep(300); } catch (InterruptedException ignored) {} }
        }
        if (al != null) for (Album a : al) if (a.getAlbumItems() != null)
            for (AlbumItem it : new ArrayList<>(a.getAlbumItems()))
                if (it != null && (it.getPath() == null || seen.add(it.getPath()))) all.add(it);
        Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));

        String[] keys = { "Videos", "Selfies", "Live Photos", "Panoramas", "Time-lapse",
                "Bursts", "Screenshots", "Screen Recordings", "Animated", "RAW" };
        LinkedHashMap<String, List<AlbumItem>> m = new LinkedHashMap<>();
        for (String k : keys) m.put(k, new ArrayList<>());

        for (AlbumItem it : all) {
            String p = low(it.getPath());
            String n = low(it.getName());
            boolean video = MediaType.isVideo(it.getPath());
            String folder = folderOf(p);

            if (video) m.get("Videos").add(it);
            if (MediaType.isGif(it.getPath())) m.get("Animated").add(it);
            if (MediaType.isRAWImage(it.getPath())) m.get("RAW").add(it);
            if (has(p, n, "screenshot") || folder.contains("screenshot"))
                m.get("Screenshots").add(it);
            if (video && (has(p, n, "screen_recording") || has(p, n, "screenrecord")
                    || has(p, n, "screen-recording") || folder.contains("screen recording")))
                m.get("Screen Recordings").add(it);
            if (has(p, n, "selfie") || folder.contains("selfie"))
                m.get("Selfies").add(it);
            if (n.contains("pano") || has(p, n, "panorama"))
                m.get("Panoramas").add(it);
            if (has(p, n, "timelapse") || has(p, n, "time_lapse") || has(p, n, "time-lapse"))
                m.get("Time-lapse").add(it);
            if (n.contains("_burst") || n.contains("_cover") || folder.contains("burst"))
                m.get("Bursts").add(it);
            if (n.startsWith("mvimg") || n.contains("motion") || folder.contains("motion")
                    || n.contains("_mp.") /* Samsung */)
                m.get("Live Photos").add(it);
        }
        return m;
    }

    private static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
    private static boolean has(String p, String n, String needle) {
        return p.contains(needle) || n.contains(needle);
    }
    private static String folderOf(String p) {
        int i = p.lastIndexOf('/');
        if (i <= 0) return "";
        int j = p.lastIndexOf('/', i - 1);
        return j < 0 ? p.substring(0, i) : p.substring(j + 1, i);
    }
}
