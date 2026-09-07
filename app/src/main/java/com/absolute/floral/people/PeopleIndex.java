package com.absolute.floral.people;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On-device face grouping — no identity, no cloud. ML Kit finds faces; we crop
 * each one, reduce it to a tiny normalised grayscale vector and greedily group
 * by cosine similarity. Approximate by design.
 */
public class PeopleIndex {

    public static class Person {
        public final String id;
        public Bitmap cover;
        public final List<AlbumItem> photos = new ArrayList<>();
        float[] centroid;
        int n;
        Person(String id) { this.id = id; }
    }

    public interface Listener { void onPeople(List<Person> people); }

    private static PeopleIndex INSTANCE;
    public static PeopleIndex get() {
        if (INSTANCE == null) INSTANCE = new PeopleIndex();
        return INSTANCE;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private List<Person> people = new ArrayList<>();
    private long builtAt = 0;

    public List<Person> current() { return people; }

    public void ensure(Context ctx, Listener cb) {
        if (!people.isEmpty() && System.currentTimeMillis() - builtAt < 5 * 60_000L) {
            cb.onPeople(people);
            return;
        }
        if (running.getAndSet(true)) return;
        final Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                List<Person> result = scan(app);
                people = result;
                builtAt = System.currentTimeMillis();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onPeople(people));
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                running.set(false);
            }
        }, "floral-people").start();
    }

    private List<Person> scan(Context ctx) throws Exception {
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
        // newest first, cap the work
        Collections.sort(items, (x, y) -> Long.compare(y.getDate(), x.getDate()));
        if (items.size() > 400) items = items.subList(0, 400);

        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.12f)
                .build();
        FaceDetector detector = FaceDetection.getClient(opts);

        List<Person> clusters = new ArrayList<>();
        int personSeq = 0;

        for (AlbumItem it : items) {
            Bitmap bmp = decode(ctx, it, 640);
            if (bmp == null) continue;
            try {
                InputImage img = InputImage.fromBitmap(bmp, 0);
                List<Face> faces = Tasks.await(detector.process(img));
                for (Face f : faces) {
                    Rect b = f.getBoundingBox();
                    Rect c = clamp(b, bmp.getWidth(), bmp.getHeight());
                    if (c.width() < 40 || c.height() < 40) continue;
                    Bitmap crop = Bitmap.createBitmap(bmp, c.left, c.top, c.width(), c.height());
                    float[] emb = embed(crop);
                    Person best = null; float bestSim = 0f;
                    for (Person p : clusters) {
                        float s = cosine(p.centroid, emb);
                        if (s > bestSim) { bestSim = s; best = p; }
                    }
                    if (best != null && bestSim >= 0.90f) {
                        merge(best, emb);
                        if (!best.photos.contains(it)) best.photos.add(it);
                    } else {
                        Person p = new Person("person_" + (++personSeq));
                        p.centroid = emb.clone();
                        p.n = 1;
                        p.cover = Bitmap.createScaledBitmap(crop, 160, 160, true);
                        p.photos.add(it);
                        clusters.add(p);
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                bmp.recycle();
            }
        }
        detector.close();

        List<Person> keep = new ArrayList<>();
        for (Person p : clusters) if (!p.photos.isEmpty()) keep.add(p);
        Collections.sort(keep, (a, b) -> Integer.compare(b.photos.size(), a.photos.size()));
        return keep.size() > 12 ? keep.subList(0, 12) : keep;
    }

    private void merge(Person p, float[] emb) {
        for (int i = 0; i < p.centroid.length; i++)
            p.centroid[i] = (p.centroid[i] * p.n + emb[i]) / (p.n + 1);
        p.n++;
    }

    private static Rect clamp(Rect b, int w, int h) {
        int l = Math.max(0, b.left), t = Math.max(0, b.top);
        int r = Math.min(w, b.right), bo = Math.min(h, b.bottom);
        return new Rect(l, t, Math.max(l + 1, r), Math.max(t + 1, bo));
    }

    private static float[] embed(Bitmap face) {
        int S = 16;
        Bitmap g = Bitmap.createScaledBitmap(face, S, S, true);
        float[] v = new float[S * S];
        float mean = 0;
        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
            int px = g.getPixel(x, y);
            float lum = (0.299f * ((px >> 16) & 255) + 0.587f * ((px >> 8) & 255) + 0.114f * (px & 255)) / 255f;
            v[y * S + x] = lum;
            mean += lum;
        }
        g.recycle();
        mean /= v.length;
        double norm = 0;
        for (int i = 0; i < v.length; i++) { v[i] -= mean; norm += v[i] * v[i]; }
        norm = Math.sqrt(norm) + 1e-6;
        for (int i = 0; i < v.length; i++) v[i] /= norm;
        return v;
    }

    private static float cosine(float[] a, float[] b) {
        float d = 0;
        for (int i = 0; i < a.length; i++) d += a[i] * b[i];
        return d;
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
