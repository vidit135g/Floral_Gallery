package com.absolute.floral.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.exifinterface.media.ExifInterface;

import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.util.MediaType;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Apple-Photos-style info card: a big date, an EXIF spec card and a location card. */
public final class InfoSheet {

    private InfoSheet() {}

    public static void show(final ItemActivity a, final AlbumItem item) {
        final Soma s = SomaSkin.read(a);
        final BottomSheetDialog dlg = new BottomSheetDialog(a);

        ScrollView sv = new ScrollView(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(a, 22);
        col.setPadding(pad, dp(a, 18), pad, dp(a, 30));
        col.setBackgroundColor(s.surface);
        sv.addView(col);

        // grabber
        View grab = new View(a);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(s.hairline);
        gd.setCornerRadius(dp(a, 3));
        grab.setBackground(gd);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(a, 36), dp(a, 5));
        glp.gravity = Gravity.CENTER_HORIZONTAL;
        glp.bottomMargin = dp(a, 16);
        col.addView(grab, glp);

        long when = item.getDate();
        TextView day = new TextView(a);
        day.setTypeface(Soma.display(a), Typeface.BOLD);
        day.setTextSize(21);
        day.setTextColor(s.ink);
        day.setText(when > 0 ? new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(new Date(when))
                : "Date unknown");
        col.addView(day);

        if (when > 0) {
            TextView time = new TextView(a);
            time.setTypeface(Soma.body(a));
            time.setTextSize(14);
            time.setTextColor(s.inkMute);
            time.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(when)));
            time.setPadding(0, dp(a, 2), 0, 0);
            col.addView(time);
        }

        TextView name = new TextView(a);
        name.setTypeface(Soma.body(a));
        name.setTextSize(13);
        name.setTextColor(s.inkSoft);
        name.setText(item.getName() == null ? "" : item.getName());
        name.setPadding(0, dp(a, 10), 0, 0);
        col.addView(name);

        // spec card
        LinearLayout card = card(a, s);
        col.addView(card, cardLp(a));

        int[] dim = safeDimens(a, item);
        String res = dim != null && dim[0] > 0
                ? dim[0] + " × " + dim[1] + mp(dim[0], dim[1])
                : null;
        String size = humanSize(fileSize(a, item));

        addRow(a, s, card, "Type", MediaType.isVideo(item.getPath()) ? "Video" : typeLabel(item.getName()));
        if (res != null) addRow(a, s, card, "Resolution", res);
        if (size != null) addRow(a, s, card, "File size", size);

        // EXIF (photos only)
        boolean isPhoto = !MediaType.isVideo(item.getPath());
        if (isPhoto) {
            double[] latlon = fillExif(a, item, card, s);
            if (latlon != null) addMap(a, s, col, latlon[0], latlon[1]);
        }

        // favourite toggle
        final TextView fav = new TextView(a);
        fav.setTypeface(Soma.body(a), Typeface.BOLD);
        fav.setTextSize(14);
        fav.setGravity(Gravity.CENTER);
        fav.setPadding(0, dp(a, 13), 0, dp(a, 13));
        boolean[] on = { FlagStore.favorites(a).contains(item.getPath()) };
        styleFav(a, s, fav, on[0]);
        fav.setOnClickListener(v -> {
            on[0] = FlagStore.favorites(a).toggle(item.getPath());
            styleFav(a, s, fav, on[0]);
        });
        LinearLayout.LayoutParams flp = cardLp(a);
        col.addView(fav, flp);
        android.graphics.drawable.GradientDrawable fb = new android.graphics.drawable.GradientDrawable();
        fb.setColor(s.surfaceStrong);
        fb.setCornerRadius(dp(a, 16));
        fav.setBackground(fb);

        if (isPhoto) {
            TextView edit = new TextView(a);
            edit.setTypeface(Soma.body(a), Typeface.BOLD);
            edit.setTextSize(13);
            edit.setTextColor(s.accent);
            edit.setText("Edit date & metadata");
            edit.setGravity(Gravity.CENTER);
            edit.setPadding(0, dp(a, 16), 0, dp(a, 4));
            edit.setOnClickListener(v -> {
                dlg.dismiss();
                android.content.Intent it = new android.content.Intent(a, ExifEditorActivity.class);
                it.putExtra(ExifEditorActivity.ALBUM_ITEM, item);
                a.startActivity(it);
            });
            col.addView(edit);
        }

        dlg.setContentView(sv);
        dlg.show();
    }

    private static void styleFav(Context c, Soma s, TextView t, boolean on) {
        t.setText(on ? "♥  In Favourites" : "♡  Add to Favourites");
        t.setTextColor(on ? 0xFFE0245E : s.ink);
    }

    /* ---- exif ---- */

    private static double[] fillExif(Context c, AlbumItem item, LinearLayout card, Soma s) {
        try {
            InputStream is = openStream(c, item);
            if (is == null) return null;
            ExifInterface ex = new ExifInterface(is);
            is.close();

            String make = ex.getAttribute(ExifInterface.TAG_MAKE);
            String model = ex.getAttribute(ExifInterface.TAG_MODEL);
            if (model != null) addRow(c, s, card, "Camera",
                    (make != null && !model.startsWith(make) ? make + " " : "") + model);

            String lens = ex.getAttribute(ExifInterface.TAG_LENS_MODEL);
            if (lens != null) addRow(c, s, card, "Lens", lens);

            String f = ex.getAttribute(ExifInterface.TAG_F_NUMBER);
            String exp = ex.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            String iso = ex.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);
            String focal = ex.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            StringBuilder shot = new StringBuilder();
            if (focal != null) shot.append(mm(focal)).append("mm  ");
            if (f != null) shot.append("ƒ/").append(trimNum(f)).append("  ");
            if (exp != null) shot.append(shutter(exp)).append("  ");
            if (iso != null) shot.append("ISO ").append(iso);
            if (shot.length() > 0) addRow(c, s, card, "Exposure", shot.toString().trim());

            double[] ll = ex.getLatLong();
            if (ll != null && (ll[0] != 0 || ll[1] != 0)) return ll;
        } catch (Throwable ignored) {}
        return null;
    }

    private static InputStream openStream(Context c, AlbumItem item) {
        try {
            Uri u = item.getUri(c);
            if (u != null) {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    try { u = android.provider.MediaStore.setRequireOriginal(u); } catch (Throwable ignored) {}
                }
                return c.getContentResolver().openInputStream(u);
            }
            return new java.io.FileInputStream(item.getPath());
        } catch (Throwable t) { return null; }
    }

    /* ---- map card ---- */

    private static void addMap(final ItemActivity a, final Soma s, LinearLayout col, final double lat, final double lon) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(s.surfaceStrong);
        bg.setCornerRadius(dp(a, 18));
        wrap.setBackground(bg);
        wrap.setClipToOutline(true);
        col.addView(wrap, cardLp(a));

        MapCard map = new MapCard(a, s.accent);
        wrap.addView(map, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 130)));

        final TextView place = new TextView(a);
        place.setTypeface(Soma.body(a), Typeface.BOLD);
        place.setTextSize(13);
        place.setTextColor(s.ink);
        place.setPadding(dp(a, 14), dp(a, 11), dp(a, 14), dp(a, 4));
        place.setText(String.format(Locale.US, "%.4f, %.4f", lat, lon));
        wrap.addView(place);

        final TextView coords = new TextView(a);
        coords.setTypeface(Soma.body(a));
        coords.setTextSize(11);
        coords.setTextColor(s.inkMute);
        coords.setPadding(dp(a, 14), 0, dp(a, 14), dp(a, 12));
        coords.setText(String.format(Locale.US, "%.4f, %.4f", lat, lon));
        wrap.addView(coords);

        new Thread(() -> {
            try {
                List<Address> res = new Geocoder(a, Locale.getDefault()).getFromLocation(lat, lon, 1);
                if (res != null && !res.isEmpty()) {
                    Address ad = res.get(0);
                    String loc = ad.getLocality() != null ? ad.getLocality()
                            : ad.getSubAdminArea() != null ? ad.getSubAdminArea() : ad.getAdminArea();
                    String country = ad.getCountryName();
                    final String label = loc != null && country != null ? loc + ", " + country
                            : country != null ? country : loc;
                    if (label != null) a.runOnUiThread(() -> place.setText(label));
                }
            } catch (Throwable ignored) {}
        }, "floral-geocode").start();
    }

    /** A stylised, offline "map" — soft gradient, grid lines and a pin. */
    static class MapCard extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int accent;
        MapCard(Context c, int accent) { super(c); this.accent = accent; }

        @Override protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, w, h,
                    blend(accent, 0xFFFFFFFF, 0.82f), blend(accent, 0xFFFFFFFF, 0.62f), Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h, p);
            p.setShader(null);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(getContext(), 1));
            p.setColor(0x1A000000);
            float step = w / 6f;
            for (float x = step; x < w; x += step) cv.drawLine(x, 0, x, h, p);
            for (float y = step; y < h; y += step) cv.drawLine(0, y, w, y, p);

            float cx = w / 2f, cy = h / 2f;
            p.setStyle(Paint.Style.FILL);
            p.setColor((accent & 0x00FFFFFF) | 0x33000000);
            cv.drawCircle(cx, cy, dp(getContext(), 24), p);
            p.setColor(accent);
            cv.drawCircle(cx, cy, dp(getContext(), 7), p);
            p.setColor(0xFFFFFFFF);
            cv.drawCircle(cx, cy, dp(getContext(), 3), p);
        }
    }

    /* ---- small ui helpers ---- */

    private static LinearLayout card(Context c, Soma s) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(s.surfaceStrong);
        bg.setCornerRadius(dp(c, 18));
        l.setBackground(bg);
        l.setPadding(dp(c, 16), dp(c, 6), dp(c, 16), dp(c, 6));
        return l;
    }

    private static LinearLayout.LayoutParams cardLp(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 16);
        return lp;
    }

    private static void addRow(Context c, Soma s, LinearLayout card, String k, String v) {
        if (v == null || v.isEmpty()) return;
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(c, 10), 0, dp(c, 10));
        TextView key = new TextView(c);
        key.setTypeface(Soma.body(c));
        key.setTextSize(13);
        key.setTextColor(s.inkMute);
        key.setText(k);
        row.addView(key, new LinearLayout.LayoutParams(dp(c, 96), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView val = new TextView(c);
        val.setTypeface(Soma.body(c), Typeface.BOLD);
        val.setTextSize(13);
        val.setTextColor(s.ink);
        val.setText(v);
        row.addView(val, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (card.getChildCount() > 0) {
            View div = new View(c);
            div.setBackgroundColor(s.hairline);
            card.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        card.addView(row);
    }

    /* ---- format helpers ---- */

    private static String typeLabel(String n) {
        if (n == null) return "Photo";
        String x = n.toLowerCase(Locale.ROOT);
        if (x.endsWith(".gif")) return "GIF";
        if (x.endsWith(".png")) return "PNG image";
        if (x.endsWith(".webp")) return "WebP image";
        if (x.endsWith(".dng") || x.endsWith(".raw")) return "RAW photo";
        if (x.endsWith(".heic") || x.endsWith(".heif")) return "HEIF photo";
        return "JPEG image";
    }

    private static String mp(int w, int h) {
        double m = (w * (double) h) / 1_000_000.0;
        return m >= 0.1 ? String.format(Locale.US, "  ·  %.1f MP", m) : "";
    }

    private static String mm(String focal) {
        try {
            if (focal.contains("/")) {
                String[] q = focal.split("/");
                return String.valueOf(Math.round(Double.parseDouble(q[0]) / Double.parseDouble(q[1])));
            }
            return String.valueOf(Math.round(Double.parseDouble(focal)));
        } catch (Throwable t) { return focal; }
    }

    private static String trimNum(String v) {
        try {
            double d = v.contains("/")
                    ? Double.parseDouble(v.split("/")[0]) / Double.parseDouble(v.split("/")[1])
                    : Double.parseDouble(v);
            return d == Math.rint(d) ? String.valueOf((int) d) : String.valueOf(Math.round(d * 10) / 10.0);
        } catch (Throwable t) { return v; }
    }

    private static String shutter(String exp) {
        try {
            double s = Double.parseDouble(exp);
            if (s >= 1) return trimNum(exp) + "s";
            return "1/" + Math.round(1.0 / s) + "s";
        } catch (Throwable t) { return exp + "s"; }
    }

    private static int[] safeDimens(Context c, AlbumItem it) {
        try { return it.getImageDimens(c); } catch (Throwable t) { return null; }
    }

    private static long fileSize(Context c, AlbumItem it) {
        try {
            java.io.File f = new java.io.File(it.getPath());
            if (f.exists() && f.length() > 0) return f.length();
        } catch (Throwable ignored) {}
        try (android.os.ParcelFileDescriptor pfd =
                     c.getContentResolver().openFileDescriptor(it.getUri(c), "r")) {
            if (pfd != null) return pfd.getStatSize();
        } catch (Throwable ignored) {}
        return 0;
    }

    private static String humanSize(long b) {
        if (b <= 0) return null;
        if (b < 1024) return b + " B";
        double kb = b / 1024.0;
        if (kb < 1024) return Math.round(kb) + " KB";
        double mb = kb / 1024.0;
        return (mb < 10 ? String.format(Locale.US, "%.1f", mb) : String.valueOf(Math.round(mb))) + " MB";
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
