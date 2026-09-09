package com.absolute.floral.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny binary snapshot of the last-scanned library (album path + each item's
 * path / date / content URI). Restored synchronously on a cold start so the grid
 * shows real thumbnails instantly instead of "0 items" while the full
 * MediaStore scan runs. Reconciled and overwritten by the next real scan.
 */
public final class GridCache {

    private static final String FILE = "grid.cache";
    private static final int VERSION = 1;
    private static final int MAX_ITEMS = 20000;   // safety cap

    private GridCache() {}

    private static File file(Context c) { return new File(c.getFilesDir(), FILE); }

    public static void save(final Context c, final List<Album> albums) {
        if (albums == null) return;
        final Context app = c.getApplicationContext();
        final ArrayList<Album> snapshot = new ArrayList<>(albums);
        new Thread(() -> {
            File tmp = new File(app.getFilesDir(), FILE + ".tmp");
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tmp)))) {
                out.writeInt(VERSION);
                // count non-empty albums first
                ArrayList<Album> real = new ArrayList<>();
                for (Album a : snapshot) {
                    if (a == null || a.getAlbumItems() == null || a.getAlbumItems().isEmpty()) continue;
                    if (a.getPath() == null) continue;
                    real.add(a);
                }
                out.writeInt(real.size());
                int written = 0;
                for (Album a : real) {
                    out.writeUTF(a.getPath());
                    List<AlbumItem> items = new ArrayList<>(a.getAlbumItems());
                    out.writeInt(items.size());
                    for (AlbumItem it : items) {
                        if (it == null || it.getPath() == null) { out.writeUTF(""); out.writeLong(0); out.writeUTF(""); continue; }
                        out.writeUTF(it.getPath());
                        out.writeLong(it.getDate());
                        Uri u = it.getUri(app);
                        out.writeUTF(u == null ? "" : u.toString());
                        if (++written > MAX_ITEMS) break;
                    }
                    if (written > MAX_ITEMS) break;
                }
                out.flush();
            } catch (Exception e) {
                Log.w("GridCache", "save failed", e);
                tmp.delete();
                return;
            }
            File dst = new File(app.getFilesDir(), FILE);
            //noinspection ResultOfMethodCallIgnored
            dst.delete();
            //noinspection ResultOfMethodCallIgnored
            tmp.renameTo(dst);
        }, "grid-cache-save").start();
    }

    /** @return a reconstructed album list, or null if there's no usable cache. */
    public static ArrayList<Album> restore(Context c) {
        File f = file(c);
        if (!f.exists() || f.length() < 8) return null;
        ArrayList<Album> albums = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f)))) {
            if (in.readInt() != VERSION) return null;
            int albumCount = in.readInt();
            if (albumCount < 0 || albumCount > 5000) return null;
            for (int i = 0; i < albumCount; i++) {
                String albumPath = in.readUTF();
                Album album = new Album();
                album.setPath(albumPath);
                int n = in.readInt();
                if (n < 0 || n > MAX_ITEMS) return null;
                for (int j = 0; j < n; j++) {
                    String path = in.readUTF();
                    long date = in.readLong();
                    String uri = in.readUTF();
                    if (path.isEmpty()) continue;
                    AlbumItem it = AlbumItem.getInstance(path);
                    if (it == null) continue;
                    it.setDate(date);
                    if (!uri.isEmpty()) {
                        try { it.setUri(Uri.parse(uri)); } catch (Exception ignored) {}
                    }
                    album.getAlbumItems().add(it);
                }
                if (!album.getAlbumItems().isEmpty()) albums.add(album);
            }
        } catch (Exception e) {
            Log.w("GridCache", "restore failed", e);
            return null;
        }
        return albums.isEmpty() ? null : albums;
    }

    public static void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        file(c).delete();
    }
}
