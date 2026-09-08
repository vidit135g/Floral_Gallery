package com.absolute.floral.util;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.models.Video;

import java.util.ArrayList;
import java.util.List;

/**
 * Scoped-storage-safe media operations. On API 30+ a delete goes through
 * {@link MediaStore#createDeleteRequest} with proper Images/Video collection
 * URIs — the legacy {@code FileOperation} path inserts a {@code _data} column,
 * which throws "Mutation of _data is not allowed" on modern Android.
 */
public final class MediaOps {

    private MediaOps() {}

    /** The deletable MediaStore URI for a file path, or {@code null} if it isn't in the media DB. */
    public static Uri contentUri(Context ctx, String path) {
        if (path == null) return null;
        boolean video = MediaType.isVideo(path)
                || AlbumItem.getInstance(path) instanceof Video;
        Uri coll = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                         : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri u = lookup(ctx, coll, path);
        if (u != null) return u;
        // hidden media can live outside the typed collections
        u = lookup(ctx, video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                              : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, path);
        return u;
    }

    private static Uri lookup(Context ctx, Uri collection, String path) {
        ContentResolver r = ctx.getContentResolver();
        try (Cursor c = r.query(collection,
                new String[]{ MediaStore.MediaColumns._ID },
                MediaStore.MediaColumns.DATA + "=?", new String[]{ path }, null)) {
            if (c != null && c.moveToFirst())
                return ContentUris.withAppendedId(collection, c.getLong(0));
        } catch (Exception ignored) {}
        return null;
    }

    /** Resolve a set of paths to deletable URIs (skips any that aren't in the media DB). */
    public static ArrayList<Uri> urisFor(Context ctx, List<String> paths) {
        ArrayList<Uri> out = new ArrayList<>();
        if (paths == null) return out;
        for (String p : paths) {
            Uri u = contentUri(ctx, p);
            if (u != null) out.add(u);
        }
        return out;
    }

    /**
     * A system delete confirmation for the given URIs, or {@code null} on older
     * Android / when nothing resolved. Launch with
     * {@code startIntentSenderForResult(pi.getIntentSender(), rc, null, 0, 0, 0)}.
     */
    public static PendingIntent deleteRequest(Context ctx, List<Uri> uris) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris == null || uris.isEmpty()) return null;
        try {
            return MediaStore.createDeleteRequest(ctx.getContentResolver(), uris);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
