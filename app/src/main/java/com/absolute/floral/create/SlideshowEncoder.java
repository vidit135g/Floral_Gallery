package com.absolute.floral.create;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

/** Renders a Ken-Burns + crossfade slideshow of photos to an H.264 MP4 — no GL, byte-buffer path. */
public class SlideshowEncoder {

    public interface Progress { void onProgress(float f); void onDone(Uri uri); void onError(String msg); }

    private static final int W = 1080, H = 1080;
    private static final int FPS = 30;
    private static final float SECONDS_PER = 2.6f;
    private static final float FADE = 0.55f;

    private final Context ctx;
    public SlideshowEncoder(Context c) { this.ctx = c; }

    public void encode(List<String> paths, List<Uri> uris, Progress cb) {
        new Thread(() -> {
            try {
                Uri out = run(paths, uris, cb);
                cb.onDone(out);
            } catch (Throwable t) {
                t.printStackTrace();
                cb.onError(t.getMessage() == null ? "encode failed" : t.getMessage());
            }
        }, "floral-slideshow").start();
    }

    private Uri run(List<String> paths, List<Uri> uris, Progress cb) throws Exception {
        int n = paths != null ? paths.size() : uris.size();
        if (n == 0) throw new IllegalStateException("no photos");

        MediaFormat fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H);
        fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        fmt.setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000);
        fmt.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
        fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        MediaCodec codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();

        java.io.File tmp = new java.io.File(ctx.getCacheDir(), "slideshow_" + System.currentTimeMillis() + ".mp4");
        MediaMuxer muxer = new MediaMuxer(tmp.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int trackIndex = -1;
        boolean muxing = false;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        int framesPer = Math.round(SECONDS_PER * FPS);
        int fadeFrames = Math.round(FADE * FPS);
        int totalFrames = framesPer * n;
        byte[] yuv = new byte[W * H * 3 / 2];
        Bitmap frame = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(frame);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        long ptsUs = 0;
        int emitted = 0;

        Bitmap cur = load(paths, uris, 0);
        Bitmap next = n > 1 ? load(paths, uris, 1) : null;

        for (int i = 0; i < n; i++) {
            for (int f = 0; f < framesPer; f++) {
                float t = f / (float) framesPer;                     // 0..1 within this photo
                canvas.drawColor(0xFF000000);
                drawKenBurns(canvas, paint, cur, i % 2 == 0, t);
                boolean fading = next != null && f >= framesPer - fadeFrames;
                if (fading) {
                    float ft = (f - (framesPer - fadeFrames)) / (float) fadeFrames;
                    paint.setAlpha(Math.round(255 * ft));
                    drawKenBurns(canvas, paint, next, (i + 1) % 2 == 0, 0f + ft * 0.15f);
                    paint.setAlpha(255);
                }
                rgbToYuv420sp(frame, yuv);
                ptsUs = (long) (emitted * 1_000_000L / FPS);
                feed(codec, yuv, ptsUs);
                emitted++;

                trackIndex = drain(codec, muxer, info, trackIndex, false);
                if (trackIndex >= 0 && !muxing) { muxer.start(); muxing = true; }
                if (muxing) trackIndex = drain(codec, muxer, info, trackIndex, false);

                if (cb != null && emitted % 6 == 0) cb.onProgress(emitted / (float) totalFrames);
            }
            if (cur != null && cur != next) cur.recycle();
            cur = next;
            next = (i + 2 < n) ? load(paths, uris, i + 2) : null;
        }

        feedEos(codec);
        if (!muxing) { muxer.start(); muxing = true; }
        drain(codec, muxer, info, trackIndex, true);

        codec.stop(); codec.release();
        muxer.stop(); muxer.release();
        if (cur != null) cur.recycle();
        if (next != null) next.recycle();
        frame.recycle();

        return publish(tmp);
    }

    private void drawKenBurns(Canvas c, Paint p, Bitmap b, boolean zoomIn, float t) {
        if (b == null) return;
        float scale0 = 1.02f, scale1 = 1.12f;
        float s = zoomIn ? lerp(scale0, scale1, t) : lerp(scale1, scale0, t);
        float bw = b.getWidth(), bh = b.getHeight();
        float target = Math.max(W / bw, H / bh) * s;
        float dw = bw * target, dh = bh * target;
        float panX = (dw - W) * (0.3f + 0.4f * t);
        float panY = (dh - H) * (0.5f);
        RectF dst = new RectF(-panX, -panY, dw - panX, dh - panY);
        c.drawBitmap(b, null, dst, p);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private Bitmap load(List<String> paths, List<Uri> uris, int i) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap bmp;
            if (paths != null && paths.get(i) != null) {
                bmp = BitmapFactory.decodeFile(paths.get(i), o);
                if (bmp == null && uris != null) bmp = decodeUri(uris.get(i), o);
            } else {
                bmp = decodeUri(uris.get(i), o);
            }
            return bmp;
        } catch (Throwable t) { return null; }
    }

    private Bitmap decodeUri(Uri u, BitmapFactory.Options o) throws Exception {
        try (java.io.InputStream is = ctx.getContentResolver().openInputStream(u)) {
            return BitmapFactory.decodeStream(is, null, o);
        }
    }

    private void feed(MediaCodec codec, byte[] yuv, long ptsUs) {
        int in = codec.dequeueInputBuffer(10_000);
        if (in >= 0) {
            ByteBuffer b = codec.getInputBuffer(in);
            b.clear();
            b.put(yuv);
            codec.queueInputBuffer(in, 0, yuv.length, ptsUs, 0);
        }
    }

    private void feedEos(MediaCodec codec) {
        int in = codec.dequeueInputBuffer(10_000);
        if (in >= 0) codec.queueInputBuffer(in, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    }

    private int drain(MediaCodec codec, MediaMuxer muxer, MediaCodec.BufferInfo info,
                      int trackIndex, boolean endOfStream) {
        for (;;) {
            int out = codec.dequeueOutputBuffer(info, endOfStream ? 20_000 : 0);
            if (out == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) return trackIndex;
            } else if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(codec.getOutputFormat());
            } else if (out >= 0) {
                ByteBuffer buf = codec.getOutputBuffer(out);
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0;
                if (info.size > 0 && trackIndex >= 0) {
                    buf.position(info.offset);
                    buf.limit(info.offset + info.size);
                    muxer.writeSampleData(trackIndex, buf, info);
                }
                codec.releaseOutputBuffer(out, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return trackIndex;
            }
        }
    }

    /** ARGB_8888 bitmap -> YUV420 semi-planar (NV12: Y plane then interleaved VU). */
    private void rgbToYuv420sp(Bitmap bmp, byte[] yuv) {
        int[] argb = new int[W * H];
        bmp.getPixels(argb, 0, W, 0, 0, W, H);
        int ySize = W * H;
        int uvIndex = ySize;
        for (int j = 0; j < H; j++) {
            for (int i = 0; i < W; i++) {
                int c = argb[j * W + i];
                int r = (c >> 16) & 0xff, g = (c >> 8) & 0xff, b = c & 0xff;
                int y = (66 * r + 129 * g + 25 * b + 128 >> 8) + 16;
                yuv[j * W + i] = (byte) clamp(y);
                if ((j & 1) == 0 && (i & 1) == 0) {
                    int u = (-38 * r - 74 * g + 112 * b + 128 >> 8) + 128;
                    int v = (112 * r - 94 * g - 18 * b + 128 >> 8) + 128;
                    yuv[uvIndex++] = (byte) clamp(v);
                    yuv[uvIndex++] = (byte) clamp(u);
                }
            }
        }
    }
    private int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    private Uri publish(java.io.File tmp) throws Exception {
        String name = "Highlight_" + System.currentTimeMillis() + ".mp4";
        Uri uri;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Video.Media.DISPLAY_NAME, name);
            cv.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            cv.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Floral");
            uri = ctx.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                 java.io.InputStream is = new java.io.FileInputStream(tmp)) {
                byte[] buf = new byte[65536]; int r;
                while ((r = is.read(buf)) > 0) os.write(buf, 0, r);
            }
            tmp.delete();
        } else {
            java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES), "Floral");
            dir.mkdirs();
            java.io.File dest = new java.io.File(dir, name);
            tmp.renameTo(dest);
            uri = Uri.fromFile(dest);
            ctx.sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
        return uri;
    }
}
