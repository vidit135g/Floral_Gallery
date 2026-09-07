package com.absolute.floral.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absolute.floral.R;
import com.absolute.floral.create.SlideshowEncoder;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.util.ArrayList;
import java.util.List;

/** Build an auto Highlight film (Ken-Burns + crossfade MP4) from a memory or a manual pick. */
public class HighlightActivity extends AppCompatActivity {

    /** When set, skip the picker and use these items. */
    public static List<AlbumItem> AUTO_ITEMS;
    public static String AUTO_TITLE;

    private static final int PICK = 82;
    private Soma s;
    private TextView status;
    private com.absolute.floral.bento.BarChartView bar;   // reuse as a simple progress strip? no
    private android.widget.ProgressBar progress;
    private VideoView video;

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
        title.setText("Highlight film");
        title.setTypeface(Soma.display(this), android.graphics.Typeface.BOLD);
        title.setTextSize(28);
        title.setTextColor(s.ink);
        title.setPadding(p, d(30), p, d(4));
        root.addView(title);
        Anim.enter(title, 20);

        video = new VideoView(this);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        vlp.setMargins(p, p, p, p);
        root.addView(video, vlp);

        progress = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.setMargins(p, 0, p, d(6));
        root.addView(progress, plp);

        status = new TextView(this);
        status.setText("Preparing…");
        status.setTextColor(s.inkMute);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(p, d(2), p, d(24));
        root.addView(status);

        if (AUTO_ITEMS != null && !AUTO_ITEMS.isEmpty()) {
            List<AlbumItem> items = new ArrayList<>(AUTO_ITEMS);
            AUTO_ITEMS = null;
            if (AUTO_TITLE != null) title.setText(AUTO_TITLE);
            build(items);
        } else {
            Matisse.from(this).choose(MimeType.ofImage(), false)
                    .countable(true).maxSelectable(30)
                    .imageEngine(new Glide4Engine())
                    .forResult(PICK);
        }
    }

    @Override protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK) return;
        if (res != RESULT_OK || data == null) { finish(); return; }
        List<Uri> uris = Matisse.obtainResult(data);
        List<String> paths = Matisse.obtainPathResult(data);
        if (uris.isEmpty()) { finish(); return; }
        buildFrom(paths, uris);
    }

    private void build(List<AlbumItem> items) {
        List<String> paths = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();
        for (AlbumItem it : items) {
            paths.add(it.getPath());
            uris.add(it.getUri(this));
        }
        buildFrom(paths, uris);
    }

    private void buildFrom(List<String> paths, List<Uri> uris) {
        status.setText("Rendering your film — this can take a minute…");
        new SlideshowEncoder(this).encode(paths, uris, new SlideshowEncoder.Progress() {
            @Override public void onProgress(float f) {
                runOnUiThread(() -> progress.setProgress(Math.round(f * 1000)));
            }
            @Override public void onDone(Uri uri) {
                runOnUiThread(() -> {
                    progress.setProgress(1000);
                    status.setText("Saved to Floral");
                    Toast.makeText(HighlightActivity.this, "Highlight film saved", Toast.LENGTH_SHORT).show();
                    try {
                        video.setVideoURI(uri);
                        video.setOnPreparedListener(mp -> { mp.setLooping(true); video.start(); });
                        video.start();
                    } catch (Exception ignored) {}
                });
            }
            @Override public void onError(String msg) {
                runOnUiThread(() -> {
                    status.setText("Couldn't render the film");
                    Toast.makeText(HighlightActivity.this, "Render failed: " + msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
