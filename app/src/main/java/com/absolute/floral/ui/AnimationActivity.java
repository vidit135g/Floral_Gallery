package com.absolute.floral.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.create.gif.AnimatedGifEncoder;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Turn a set of photos into a looping animated GIF — fully on-device. */
public class AnimationActivity extends AppCompatActivity {

    private static final int PICK = 81;
    private Soma s;
    private ImageView preview;
    private TextView status;
    private pl.droidsonroids.gif.GifImageView gifView;

    @Override protected void onCreate(@Nullable Bundle st) {
        super.onCreate(st);
        s = SomaSkin.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(s.ground[0]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, s);

        int p = d(20);
        TextView title = new TextView(this);
        title.setText("Animation");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(28);
        title.setTextColor(s.ink);
        title.setPadding(p, d(30), p, d(4));
        root.addView(title);
        Anim.enter(title, 20);

        gifView = new pl.droidsonroids.gif.GifImageView(this);
        gifView.setAdjustViewBounds(true);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        glp.setMargins(p, p, p, p);
        root.addView(gifView, glp);

        status = new TextView(this);
        status.setText("Pick 2–24 photos");
        status.setTextColor(s.inkMute);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(p, d(6), p, d(24));
        root.addView(status);

        Matisse.from(this).choose(MimeType.ofImage(), false)
                .countable(true).maxSelectable(24)
                .imageEngine(new Glide4Engine())
                .forResult(PICK);
    }

    @Override protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK) return;
        if (res != RESULT_OK || data == null) { finish(); return; }
        final List<Uri> uris = Matisse.obtainResult(data);
        final List<String> paths = Matisse.obtainPathResult(data);
        if (uris.isEmpty()) { finish(); return; }
        status.setText("Building your animation…");
        new Thread(() -> {
            try {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                AnimatedGifEncoder enc = new AnimatedGifEncoder();
                enc.setRepeat(0);
                enc.setDelay(180);
                enc.setQuality(12);
                enc.setSize(480, 480);
                enc.start(bos);
                for (int i = 0; i < uris.size(); i++) {
                    Bitmap b = decode(paths != null && i < paths.size() ? paths.get(i) : null, uris.get(i));
                    if (b == null) continue;
                    Bitmap sq = centerCropSquare(b, 480);
                    enc.addFrame(sq);
                    b.recycle(); sq.recycle();
                    final int done = i + 1;
                    runOnUiThread(() -> status.setText("Adding photo " + done + " / " + uris.size()));
                }
                enc.finish();
                byte[] gif = bos.toByteArray();
                Uri saved = save(gif);
                runOnUiThread(() -> {
                    status.setText("Saved to Floral · " + (gif.length / 1024) + " KB");
                    try {
                        gifView.setImageDrawable(new pl.droidsonroids.gif.GifDrawable(gif));
                    } catch (Exception e) {
                        preview(saved);
                    }
                    Toast.makeText(this, "Animation saved", Toast.LENGTH_SHORT).show();
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    status.setText("Couldn't build the animation");
                    Toast.makeText(this, "Animation failed", Toast.LENGTH_SHORT).show();
                });
            }
        }, "floral-gif").start();
    }

    private void preview(Uri u) {
        try { gifView.setImageURI(u); } catch (Exception ignored) {}
    }

    private Bitmap decode(String path, Uri uri) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            if (path != null) {
                Bitmap b = BitmapFactory.decodeFile(path, o);
                if (b != null) return b;
            }
            try (java.io.InputStream is = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, o);
            }
        } catch (Throwable t) { return null; }
    }

    private Bitmap centerCropSquare(Bitmap b, int size) {
        int min = Math.min(b.getWidth(), b.getHeight());
        int x = (b.getWidth() - min) / 2, y = (b.getHeight() - min) / 2;
        Bitmap crop = Bitmap.createBitmap(b, x, y, min, min);
        Bitmap scaled = Bitmap.createScaledBitmap(crop, size, size, true);
        if (crop != b && crop != scaled) crop.recycle();
        return scaled;
    }

    private Uri save(byte[] gif) throws Exception {
        String name = "Animation_" + System.currentTimeMillis() + ".gif";
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/gif");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Floral");
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            try (OutputStream os = getContentResolver().openOutputStream(uri)) { os.write(gif); }
        } else {
            java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Floral");
            dir.mkdirs();
            java.io.File f = new java.io.File(dir, name);
            try (OutputStream os = new java.io.FileOutputStream(f)) { os.write(gif); }
            uri = Uri.fromFile(f);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
        return uri;
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
