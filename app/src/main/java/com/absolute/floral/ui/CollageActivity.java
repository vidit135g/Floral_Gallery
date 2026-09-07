package com.absolute.floral.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** A small on-device collage maker for the "Create" menu. */
public class CollageActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    private static final int PICK = 71;

    private final List<Uri> picked = new ArrayList<>();
    private int layoutIndex = 0;
    private int bgIndex = 0;
    private final int[] BGS = { 0xFFF3ECDE, 0xFFFFFFFF, 0xFF15130F, 0xFFE9C9B3, 0xFF7C83C7 };

    private ImageView preview;
    private Soma soma;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        soma = SomaSkin.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(soma.ground[1]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, soma);

        TextView title = new TextView(this);
        title.setText("Collage");
        title.setTypeface(Soma.display(this));
        title.setTextSize(26);
        title.setTextColor(soma.ink);
        int p = d(20);
        title.setPadding(p, d(40), p, d(10));
        root.addView(title);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        plp.setMargins(p, p, p, p);
        root.addView(preview, plp);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(p, d(6), p, d(24));
        root.addView(bar);
        bar.addView(pillButton("Layout", v -> { layoutIndex = (layoutIndex + 1) % 4; rebuild(); }));
        bar.addView(pillButton("Colour", v -> { bgIndex = (bgIndex + 1) % BGS.length; rebuild(); }));
        bar.addView(pillButton("Save", v -> save()));

        Matisse.from(this).choose(MimeType.ofImage(), false)
                .countable(true).maxSelectable(9)
                .imageEngine(new Glide4Engine())
                .forResult(PICK);
    }

    private View pillButton(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTypeface(Soma.body(this));
        t.setTextColor(soma.ink);
        t.setTextSize(14);
        t.setPadding(d(20), d(12), d(20), d(12));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(soma.surface);
        g.setCornerRadius(d(22));
        g.setStroke(Math.max(1, d(1)), soma.hairline);
        t.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(d(6), 0, d(6), 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(l);
        return t;
    }

    @Override protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK) {
            if (res == RESULT_OK && data != null) {
                picked.clear();
                picked.addAll(Matisse.obtainResult(data));
            }
            if (picked.isEmpty()) { finish(); return; }
            rebuild();
        }
    }

    private Bitmap decode(Uri u, int reqPx) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(u), null, o);
            int sample = 1;
            while (Math.min(o.outWidth, o.outHeight) / (sample * 2) >= reqPx) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(u), null, d);
        } catch (Exception e) { return null; }
    }

    private Bitmap lastResult;

    private void rebuild() {
        if (picked.isEmpty()) return;
        int size = 1440;
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(BGS[bgIndex]);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        float gap = size * 0.02f;
        float rad = size * 0.03f;

        RectF[] cells = cells(picked.size(), layoutIndex, size, gap);
        for (int i = 0; i < cells.length && i < picked.size(); i++) {
            Bitmap b = decode(picked.get(i), 900);
            if (b == null) continue;
            RectF cell = cells[i];
            int save = c.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(cell, rad, rad, android.graphics.Path.Direction.CW);
            c.clipPath(clip);
            Rect src = centerCrop(b.getWidth(), b.getHeight(), cell.width(), cell.height());
            c.drawBitmap(b, src, cell, pt);
            c.restoreToCount(save);
            b.recycle();
        }
        lastResult = out;
        preview.setImageBitmap(out);
        Anim.pulse(preview);
    }

    private RectF[] cells(int n, int layout, int size, float gap) {
        List<RectF> r = new ArrayList<>();
        float s = size;
        if (n <= 1) { r.add(new RectF(gap, gap, s - gap, s - gap)); }
        else if (n == 2) {
            if (layout % 2 == 0) { r.add(new RectF(gap, gap, s - gap, s / 2 - gap / 2)); r.add(new RectF(gap, s / 2 + gap / 2, s - gap, s - gap)); }
            else { r.add(new RectF(gap, gap, s / 2 - gap / 2, s - gap)); r.add(new RectF(s / 2 + gap / 2, gap, s - gap, s - gap)); }
        } else if (n == 3) {
            if (layout % 2 == 0) {
                r.add(new RectF(gap, gap, s - gap, s * 0.55f - gap / 2));
                r.add(new RectF(gap, s * 0.55f + gap / 2, s / 2 - gap / 2, s - gap));
                r.add(new RectF(s / 2 + gap / 2, s * 0.55f + gap / 2, s - gap, s - gap));
            } else {
                r.add(new RectF(gap, gap, s / 2 - gap / 2, s - gap));
                r.add(new RectF(s / 2 + gap / 2, gap, s - gap, s / 2 - gap / 2));
                r.add(new RectF(s / 2 + gap / 2, s / 2 + gap / 2, s - gap, s - gap));
            }
        } else {
            int cols = n <= 4 ? 2 : 3;
            int rows = (int) Math.ceil(n / (float) cols);
            float cw = (s - gap * (cols + 1)) / cols;
            float ch = (s - gap * (rows + 1)) / rows;
            for (int i = 0; i < n; i++) {
                int cx = i % cols, cy = i / cols;
                float l = gap + cx * (cw + gap);
                float t = gap + cy * (ch + gap);
                r.add(new RectF(l, t, l + cw, t + ch));
            }
        }
        return r.toArray(new RectF[0]);
    }

    private Rect centerCrop(int bw, int bh, float tw, float th) {
        float scale = Math.max(tw / bw, th / bh);
        int cw = Math.round(tw / scale), ch = Math.round(th / scale);
        int l = (bw - cw) / 2, t = (bh - ch) / 2;
        return new Rect(l, t, l + cw, t + ch);
    }

    private void save() {
        if (lastResult == null) return;
        String name = "Collage_" + System.currentTimeMillis() + ".jpg";
        try {
            Uri uri;
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Floral");
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    lastResult.compress(Bitmap.CompressFormat.JPEG, 94, os);
                }
            } else {
                java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Floral");
                dir.mkdirs();
                java.io.File f = new java.io.File(dir, name);
                try (OutputStream os = new java.io.FileOutputStream(f)) {
                    lastResult.compress(Bitmap.CompressFormat.JPEG, 94, os);
                }
                uri = Uri.fromFile(f);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
            Toast.makeText(this, "Collage saved to Floral", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't save collage", Toast.LENGTH_SHORT).show();
        }
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
