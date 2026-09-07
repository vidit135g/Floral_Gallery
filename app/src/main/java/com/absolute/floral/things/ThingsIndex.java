package com.absolute.floral.things;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On-device scene tagging — ML Kit's default image labeler runs entirely locally,
 * no network, no identity. We fold its ~400 raw labels into a handful of friendly
 * "things" (Food, Nature, Animals, Cityscape, Documents, …) and group photos under them.
 */
public class ThingsIndex {

    public static class Thing {
        public final String label;
        public final List<AlbumItem> photos = new ArrayList<>();
        Thing(String label) { this.label = label; }
        public AlbumItem cover() { return photos.isEmpty() ? null : photos.get(0); }
    }

    public interface Listener { void onThings(List<Thing> things); }

    private static ThingsIndex INSTANCE;
    public static ThingsIndex get() {
        if (INSTANCE == null) INSTANCE = new ThingsIndex();
        return INSTANCE;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private List<Thing> things = new ArrayList<>();
    private long builtAt = 0;

    public List<Thing> current() { return things; }

    public void ensure(Context ctx, Listener cb) {
        if (!things.isEmpty() && System.currentTimeMillis() - builtAt < 6 * 60_000L) {
            cb.onThings(things);
            return;
        }
        if (running.getAndSet(true)) { cb.onThings(things); return; }
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                List<Thing> result = scan(app);
                things = result;
                builtAt = System.currentTimeMillis();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onThings(things));
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                running.set(false);
            }
        }, "floral-things").start();
    }

    /** raw ML Kit label -> friendly bucket name. */
    private static final Map<String, String> MAP = new HashMap<>();
    static {
        put("Food", "Food"); put("Fruit", "Food"); put("Dessert", "Food"); put("Baked goods", "Food");
        put("Cooking", "Food"); put("Drink", "Food"); put("Coffee", "Food");
        put("Plant", "Nature"); put("Flower", "Nature"); put("Tree", "Nature"); put("Leaf", "Nature");
        put("Garden", "Nature"); put("Grass", "Nature"); put("Mountain", "Nature"); put("Park", "Nature");
        put("Sky", "Sky"); put("Cloud", "Sky"); put("Sunset", "Sky"); put("Moon", "Sky"); put("Sunrise", "Sky");
        put("Beach", "Beaches"); put("Sea", "Beaches"); put("Ocean", "Beaches"); put("Coast", "Beaches");
        put("Water", "Water"); put("Waterfall", "Water"); put("Lake", "Water"); put("River", "Water");
        put("Snow", "Snow"); put("Winter", "Snow");
        put("Dog", "Animals"); put("Cat", "Animals"); put("Bird", "Animals"); put("Animal", "Animals");
        put("Pet", "Animals"); put("Horse", "Animals"); put("Fish", "Animals");
        put("Building", "Cityscape"); put("Skyscraper", "Cityscape"); put("City", "Cityscape");
        put("Bridge", "Cityscape"); put("Street", "Cityscape"); put("Architecture", "Cityscape");
        put("Vehicle", "Cars"); put("Car", "Cars"); put("Motorcycle", "Cars"); put("Bus", "Cars");
        put("Bicycle", "Cars"); put("Airplane", "Travel"); put("Boat", "Travel"); put("Train", "Travel");
        put("Text", "Documents"); put("Document", "Documents"); put("Whiteboard", "Documents");
        put("Receipt", "Documents"); put("Menu", "Documents"); put("Handwriting", "Documents");
        put("Crowd", "People");
        put("Wedding", "Events"); put("Party", "Events"); put("Concert", "Events"); put("Stage", "Events");
        put("Nightclub", "Events"); put("Birthday", "Events");
        put("Sport", "Sport"); put("Ball", "Sport"); put("Ski", "Sport"); put("Surfing", "Sport");
        put("Art", "Art"); put("Painting", "Art"); put("Drawing", "Art"); put("Sculpture", "Art");
        put("Flower arranging", "Nature");
    }
    private static void put(String k, String v) { MAP.put(k.toLowerCase(Locale.ROOT), v); }

    private List<Thing> scan(Context ctx) throws Exception {
        List<AlbumItem> items = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (int attempt = 0; attempt < 4; attempt++) {
            items.clear(); seen.clear();
            try {
                ArrayList<Album> albums = new ArrayList<>(MediaProvider.getAlbums());
                for (Album a : albums) {
                    if (a == null || a.getAlbumItems() == null) continue;
                    for (AlbumItem it : new ArrayList<>(a.getAlbumItems())) {
                        if (it == null) continue;
                        if (!com.absolute.floral.util.MediaType.isVideo(it.getPath())
                                && (it.getPath() == null || seen.add(it.getPath()))) items.add(it);
                    }
                }
                break;
            } catch (java.util.ConcurrentModificationException cme) {
                Thread.sleep(600);
            }
        }
        Collections.sort(items, (x, y) -> Long.compare(y.getDate(), x.getDate()));
        if (items.size() > 350) items = items.subList(0, 350);

        ImageLabeler labeler = ImageLabeling.getClient(
                new ImageLabelerOptions.Builder().setConfidenceThreshold(0.62f).build());

        LinkedHashMap<String, Thing> buckets = new LinkedHashMap<>();
        for (AlbumItem it : items) {
            Bitmap bmp = decode(ctx, it, 480);
            if (bmp == null) continue;
            try {
                List<ImageLabel> labels = Tasks.await(labeler.process(InputImage.fromBitmap(bmp, 0)));
                java.util.HashSet<String> added = new java.util.HashSet<>();
                for (ImageLabel l : labels) {
                    String bucket = MAP.get(l.getText().toLowerCase(Locale.ROOT));
                    if (bucket == null || !added.add(bucket)) continue;
                    Thing t = buckets.get(bucket);
                    if (t == null) { t = new Thing(bucket); buckets.put(bucket, t); }
                    t.photos.add(it);
                }
            } catch (Throwable ignored) {
            } finally {
                bmp.recycle();
            }
        }
        labeler.close();

        List<Thing> keep = new ArrayList<>();
        for (Thing t : buckets.values()) if (t.photos.size() >= 2) keep.add(t);
        Collections.sort(keep, (a, b) -> Integer.compare(b.photos.size(), a.photos.size()));
        return keep;
    }

    private static Bitmap decode(Context ctx, AlbumItem it, int reqPx) {
        try {
            Uri u = it.getUri(ctx);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            openDecode(ctx, it, u, o);
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= reqPx) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            return openDecode(ctx, it, u, d);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap openDecode(Context ctx, AlbumItem it, Uri u, BitmapFactory.Options o) throws Exception {
        if (u != null) {
            try (java.io.InputStream is = ctx.getContentResolver().openInputStream(u)) {
                return BitmapFactory.decodeStream(is, null, o);
            }
        }
        return BitmapFactory.decodeFile(it.getPath(), o);
    }
}
