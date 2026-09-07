package com.absolute.floral.widget;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.format.DateFormat;

import androidx.core.content.res.ResourcesCompat;

import com.absolute.floral.R;

import java.util.Calendar;

/**
 * Draws the whole "Memories" widget as one Bitmap — RemoteViews can't host a
 * custom view, so the frame, the photo, the scrim and the Fraunces caption are
 * all painted here and handed back as an ImageView source.
 */
public final class WidgetRenderer {

    private WidgetRenderer() {}

    /** A photo for today — rotates once per day through the most recent 30. */
    public static PickedPhoto pickPhoto(Context ctx) {
        ContentResolver cr = ctx.getContentResolver();
        Uri col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] proj = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED };
        int total = 0;
        long id = -1, dateMs = 0;
        try (Cursor c = cr.query(col, proj, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 30")) {
            if (c != null) {
                total = c.getCount();
                if (total > 0) {
                    int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                    int pos = day % total;
                    if (c.moveToPosition(pos)) {
                        id = c.getLong(0);
                        long dt = c.getLong(1);
                        long da = c.getLong(2) * 1000L;
                        dateMs = dt > 0 ? dt : da;
                    }
                }
            }
        } catch (Exception ignored) {}
        // count everything for the caption
        int allCount = total;
        try (Cursor c = cr.query(col, new String[]{ "count(*)" }, null, null, null)) {
            if (c != null && c.moveToFirst()) allCount = c.getInt(0);
        } catch (Exception ignored) {}

        PickedPhoto p = new PickedPhoto();
        p.uri = id >= 0 ? ContentUris.withAppendedId(col, id) : null;
        p.dateMs = dateMs;
        p.libraryCount = allCount;
        return p;
    }

    public static class PickedPhoto {
        public Uri uri;
        public long dateMs;
        public int libraryCount;
    }

    public static Bitmap render(Context ctx, int wPx, int hPx, boolean dark) {
        if (wPx <= 0) wPx = dp(ctx, 250);
        if (hPx <= 0) hPx = dp(ctx, 160);
        Bitmap out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float radius = dp(ctx, 24);
        RectF frame = new RectF(0, 0, wPx, hPx);
        Path clip = new Path();
        clip.addRoundRect(frame, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        // ground fallback
        int[] g = dark ? new int[]{ 0xFF1B1712, 0xFF0C0B0A }
                       : new int[]{ 0xFFF3ECE0, 0xFFE7DECF };
        p.setShader(new LinearGradient(0, 0, wPx, hPx, g, null, Shader.TileMode.CLAMP));
        canvas.drawRect(frame, p);
        p.setShader(null);

        PickedPhoto photo = pickPhoto(ctx);
        Bitmap thumb = photo.uri != null ? decodeScaled(ctx, photo.uri, wPx, hPx) : null;
        if (thumb != null) {
            Rect src = centerCropSrc(thumb.getWidth(), thumb.getHeight(), wPx, hPx);
            canvas.drawBitmap(thumb, src, frame, p);
            thumb.recycle();
        }

        // bottom scrim for legibility
        p.setShader(new LinearGradient(0, hPx * 0.35f, 0, hPx,
                new int[]{ 0x00000000, 0xB3000000 }, null, Shader.TileMode.CLAMP));
        canvas.drawRect(frame, p);
        p.setShader(null);

        // top kicker chip
        Typeface body = font(ctx, R.font.poppins_medium);
        Typeface display = font(ctx, R.font.fraunces_semibold);
        float pad = dp(ctx, 16);

        TextPaint kicker = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        kicker.setTypeface(body);
        kicker.setColor(0xE6FFFFFF);
        kicker.setTextSize(dp(ctx, 10));
        kicker.setLetterSpacing(0.22f);
        canvas.drawText("MEMORIES", pad, pad + dp(ctx, 10), kicker);

        // date in Fraunces
        String dateStr = photo.dateMs > 0
                ? DateFormat.format("EEEE, d MMMM", photo.dateMs).toString()
                : "Your library";
        TextPaint date = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        date.setTypeface(display);
        date.setColor(0xFFFFFFFF);
        date.setTextSize(dp(ctx, Math.min(22, wPx / dp(ctx, 14))));
        date.setShadowLayer(dp(ctx, 6), 0, dp(ctx, 1), 0x66000000);
        canvas.drawText(ellipsize(dateStr, date, wPx - pad * 2), pad, hPx - pad - dp(ctx, 20), date);

        TextPaint sub = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        sub.setTypeface(body);
        sub.setColor(0xBFFFFFFF);
        sub.setTextSize(dp(ctx, 11));
        String subStr = photo.libraryCount > 0
                ? photo.libraryCount + (photo.libraryCount == 1 ? " photo" : " photos") + " · tap to open"
                : "Open Floral";
        canvas.drawText(subStr, pad, hPx - pad, sub);

        canvas.restore();

        // hairline edge
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(ctx, 1));
        p.setColor(dark ? 0x24FFFFFF : 0x1F000000);
        float inset = dp(ctx, 0.5f);
        canvas.drawRoundRect(new RectF(inset, inset, wPx - inset, hPx - inset),
                radius, radius, p);

        return out;
    }

    /* ---- helpers ---- */

    private static Bitmap decodeScaled(Context ctx, Uri uri, int reqW, int reqH) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (java.io.InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, o);
            }
            int sample = 1;
            while (o.outWidth / (sample * 2) >= reqW && o.outHeight / (sample * 2) >= reqH)
                sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            try (java.io.InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, d);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Rect centerCropSrc(int bw, int bh, int tw, int th) {
        float scale = Math.max((float) tw / bw, (float) th / bh);
        int cw = Math.round(tw / scale), ch = Math.round(th / scale);
        int l = (bw - cw) / 2, t = (bh - ch) / 2;
        return new Rect(l, t, l + cw, t + ch);
    }

    private static String ellipsize(String s, TextPaint tp, float maxW) {
        if (tp.measureText(s) <= maxW) return s;
        while (s.length() > 1 && tp.measureText(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static Typeface font(Context c, int res) {
        Typeface t = ResourcesCompat.getFont(c, res);
        return t != null ? t : Typeface.DEFAULT;
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
