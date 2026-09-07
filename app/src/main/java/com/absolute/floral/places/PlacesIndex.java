package com.absolute.floral.places;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.exifinterface.media.ExifInterface;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Groups photos by where they were taken — EXIF GPS, clustered, reverse-geocoded when possible. */
public class PlacesIndex {

    public static class Place {
        public String label;
        public double lat, lon;
        public final List<AlbumItem> items = new ArrayList<>();
        public AlbumItem cover() { return items.isEmpty() ? null : items.get(0); }
    }

    public interface Listener { void onPlaces(List<Place> places); }

    private static PlacesIndex INSTANCE;
    public static PlacesIndex get() { return INSTANCE == null ? (INSTANCE = new PlacesIndex()) : INSTANCE; }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private List<Place> places = new ArrayList<>();
    private long builtAt;

    public List<Place> current() { return places; }

    public void ensure(Context ctx, Listener cb) {
        if (!places.isEmpty() && System.currentTimeMillis() - builtAt < 5 * 60_000L) {
            cb.onPlaces(places);
            return;
        }
        if (running.getAndSet(true)) return;
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                List<Place> r = scan(app);
                places = r;
                builtAt = System.currentTimeMillis();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onPlaces(places));
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                running.set(false);
            }
        }, "floral-places").start();
    }

    private List<Place> scan(Context ctx) {
        List<AlbumItem> all = new ArrayList<>();
        ArrayList<Album> al = null;
        for (int i = 0; i < 4 && al == null; i++) {
            try { al = new ArrayList<>(MediaProvider.getAlbums()); }
            catch (Exception e) { try { Thread.sleep(400); } catch (InterruptedException ignored) {} }
        }
        if (al != null) for (Album a : al) if (a.getAlbumItems() != null)
            for (AlbumItem it : new ArrayList<>(a.getAlbumItems()))
                if (it != null && !MediaType.isVideo(it.getPath())) all.add(it);
        Collections.sort(all, (x, y) -> Long.compare(y.getDate(), x.getDate()));

        List<Place> clusters = new ArrayList<>();
        int scanned = 0;
        for (AlbumItem it : all) {
            if (scanned >= 500) break;
            scanned++;
            double[] ll = latLon(ctx, it);
            if (ll == null) continue;
            Place near = null;
            for (Place p : clusters) {
                if (Math.abs(p.lat - ll[0]) < 0.25 && Math.abs(p.lon - ll[1]) < 0.25) { near = p; break; }
            }
            if (near == null) {
                near = new Place();
                near.lat = ll[0]; near.lon = ll[1];
                clusters.add(near);
            } else {
                near.lat = (near.lat * near.items.size() + ll[0]) / (near.items.size() + 1);
                near.lon = (near.lon * near.items.size() + ll[1]) / (near.items.size() + 1);
            }
            near.items.add(it);
        }

        // label — reverse geocode where available, else coarse coords
        Geocoder geo = Geocoder.isPresent() ? new Geocoder(ctx, Locale.getDefault()) : null;
        for (Place p : clusters) {
            p.label = null;
            if (geo != null) {
                try {
                    List<Address> a = geo.getFromLocation(p.lat, p.lon, 1);
                    if (a != null && !a.isEmpty()) {
                        Address ad = a.get(0);
                        String loc = ad.getLocality() != null ? ad.getLocality()
                                : ad.getSubAdminArea() != null ? ad.getSubAdminArea()
                                : ad.getAdminArea();
                        String country = ad.getCountryName();
                        p.label = loc != null && country != null ? loc + ", " + country
                                : loc != null ? loc : country;
                    }
                } catch (Exception ignored) {}
            }
            if (p.label == null) {
                p.label = String.format(Locale.US, "%.1f°%s, %.1f°%s",
                        Math.abs(p.lat), p.lat >= 0 ? "N" : "S",
                        Math.abs(p.lon), p.lon >= 0 ? "E" : "W");
            }
        }
        Collections.sort(clusters, (a, b) -> Integer.compare(b.items.size(), a.items.size()));
        List<Place> keep = new ArrayList<>();
        for (Place p : clusters) if (p.items.size() >= 1) keep.add(p);
        return keep;
    }

    private static double[] latLon(Context ctx, AlbumItem it) {
        // 1) straight from the file path (most reliable when we have it)
        try {
            if (it.getPath() != null) {
                double[] ll = new ExifInterface(it.getPath()).getLatLong();
                if (ll != null && (ll[0] != 0 || ll[1] != 0)) return ll;
            }
        } catch (Throwable ignored) {}
        // 2) via MediaStore — needs the *original* (un-redacted) uri + ACCESS_MEDIA_LOCATION
        try {
            android.net.Uri u = it.getUri(ctx);
            if (u == null) return null;
            android.net.Uri open = u;
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                try { open = android.provider.MediaStore.setRequireOriginal(u); } catch (Throwable ignored) {}
            }
            try (java.io.InputStream is = ctx.getContentResolver().openInputStream(open)) {
                double[] ll = new ExifInterface(is).getLatLong();
                if (ll != null && (ll[0] != 0 || ll[1] != 0)) return ll;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
