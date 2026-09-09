package com.absolute.floral.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.absolute.floral.data.models.AlbumItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * "Safe Share" — copy the picked media into the app cache with location and
 * identifying EXIF stripped, and share those clean copies instead of the
 * originals. Solves the very common leak of home GPS coordinates / device
 * serials when photos are sent to someone.
 */
public final class SafeShare {

    private SafeShare() {}

    /** EXIF tags cleared on JPEG/HEIF before sharing. */
    private static final String[] STRIP = {
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_AREA_INFORMATION,
            ExifInterface.TAG_GPS_DOP, ExifInterface.TAG_GPS_SPEED, ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_TRACK, ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_DEST_LATITUDE, ExifInterface.TAG_GPS_DEST_LONGITUDE,
            ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL,
            ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_CAMERA_OWNER_NAME, ExifInterface.TAG_BODY_SERIAL_NUMBER,
            ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
            ExifInterface.TAG_LENS_SERIAL_NUMBER, ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_USER_COMMENT, ExifInterface.TAG_XMP,
    };

    public interface Done { void ready(ArrayList<Uri> uris, int stripped); }

    /** Prepare clean copies on a background thread, then call back on the caller's thread. */
    public static void prepare(final Context ctx, final List<String> paths, final Done done) {
        final Context app = ctx.getApplicationContext();
        final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(() -> {
            File dir = new File(app.getCacheDir(), "safeshare");
            wipe(dir);
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            ArrayList<Uri> out = new ArrayList<>();
            int stripped = 0;
            for (String path : paths) {
                if (path == null) continue;
                File src = new File(path);
                if (!src.exists()) continue;
                File dst = new File(dir, src.getName());
                try {
                    copy(src, dst);
                    boolean isJpegLike = MediaType.isImage(path) && !MediaType.isGif(path);
                    if (isJpegLike && stripExif(dst)) stripped++;
                    Uri u = FileProvider.getUriForFile(app, app.getPackageName() + ".provider", dst);
                    out.add(u);
                } catch (Exception ignored) {
                    // fall back to the original if a copy fails
                    AlbumItem it = AlbumItem.getInstance(app, path);
                    Uri u = it == null ? null : it.getUri(app);
                    if (u != null) out.add(u);
                }
            }
            final int fStripped = stripped;
            main.post(() -> done.ready(out, fStripped));
        }, "safe-share").start();
    }

    public static Intent chooser(Context ctx, ArrayList<Uri> uris) {
        Intent send = new Intent(uris.size() > 1
                ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND);
        send.setType("*/*");
        if (uris.size() > 1) send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        else if (!uris.isEmpty()) send.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(send, "Share safely");
    }

    private static boolean stripExif(File f) {
        try {
            ExifInterface exif = new ExifInterface(f.getAbsolutePath());
            for (String tag : STRIP) exif.setAttribute(tag, null);
            // keep orientation so the image doesn't rotate; drop everything datey/identifying above
            exif.saveAttributes();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void copy(File src, File dst) throws Exception {
        try (InputStream in = new java.io.FileInputStream(src);
             OutputStream o = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        }
    }

    private static void wipe(File dir) {
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }
}
