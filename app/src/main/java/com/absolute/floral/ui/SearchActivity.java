package com.absolute.floral.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.util.MediaType;

import java.util.ArrayList;
import java.util.List;

/** Lightweight on-device search — filename, folder, month, "videos", "photos". */
public class SearchActivity extends AppCompatActivity {

    private PhotoGridAdapter adapter;
    private List<AlbumItem> all = new ArrayList<>();
    private java.util.Map<AlbumItem, String> owner = new java.util.HashMap<>();
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

        // Google-Photos-style search bar: back arrow + rounded field with a magnifier
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int p = d(8);
        bar.setPadding(p, d(44), p, d(8));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_arrow_back_white);
        back.setColorFilter(soma.ink);
        int bp = d(10);
        back.setPadding(bp, bp, bp, bp);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(d(44), d(44)));

        LinearLayout fieldWrap = new LinearLayout(this);
        fieldWrap.setOrientation(LinearLayout.HORIZONTAL);
        fieldWrap.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable fg = new android.graphics.drawable.GradientDrawable();
        fg.setColor(soma.surfaceStrong);
        fg.setCornerRadius(d(100));
        fieldWrap.setBackground(fg);
        fieldWrap.setPadding(d(14), d(6), d(14), d(6));
        LinearLayout.LayoutParams fwlp = new LinearLayout.LayoutParams(0, d(44), 1f);
        fwlp.setMargins(d(2), 0, d(6), 0);
        bar.addView(fieldWrap, fwlp);

        ImageView mag = new ImageView(this);
        mag.setImageResource(R.drawable.ic_search_white);
        mag.setColorFilter(soma.inkMute);
        fieldWrap.addView(mag, new LinearLayout.LayoutParams(d(20), d(20)));

        EditText field = new EditText(this);
        field.setHint("Search your photos");
        field.setSingleLine(true);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setTypeface(Soma.body(this));
        field.setTextColor(soma.ink);
        field.setHintTextColor(soma.inkMute);
        field.setTextSize(16);
        field.setBackgroundColor(0x00000000);
        field.setPadding(d(10), 0, 0, 0);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            try {
                android.graphics.drawable.GradientDrawable cur = new android.graphics.drawable.GradientDrawable();
                cur.setSize(d(2), 0);
                cur.setColor(soma.accent);
                field.setTextCursorDrawable(cur);
            } catch (Throwable ignored) {}
        }
        fieldWrap.addView(field, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        RecyclerView rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(d(6), 0, d(6), d(24));
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);
        adapter = new PhotoGridAdapter(this, emptyTimeline());
        adapter.setSpanCount(3);
        glm.setSpanSizeLookup(adapter.spanSizeLookup());
        rv.setAdapter(adapter);
        root.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ArrayList<Album> albums = MediaProvider.getAlbumsWithVirtualDirectories(this);
        if (albums != null) for (Album a : albums) if (a.getAlbumItems() != null)
            for (AlbumItem it : a.getAlbumItems()) { all.add(it); owner.put(it, a.getName()); }

        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            public void onTextChanged(CharSequence c, int a, int b, int d) { run(c.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        field.requestFocus();
    }

    private void run(String q) {
        q = q.trim().toLowerCase();
        if (q.isEmpty()) { adapter.setTimeline(emptyTimeline()); return; }
        boolean wantVideo = q.contains("video"), wantPhoto = q.equals("photos") || q.equals("photo") || q.equals("images");
        ArrayList<Album> bucket = new ArrayList<>();
        List<AlbumItem> hits = new ArrayList<>();
        for (AlbumItem it : all) {
            boolean v = MediaType.isVideo(it.getPath());
            String name = (it.getName() == null ? "" : it.getName()).toLowerCase();
            String folder = owner.containsKey(it) ? owner.get(it).toLowerCase() : "";
            String month = android.text.format.DateFormat.format("MMMM yyyy", it.getDate() > 0 ? it.getDate() : 0).toString().toLowerCase();
            boolean match = (wantVideo && v) || (wantPhoto && !v)
                    || name.contains(q) || folder.contains(q) || month.contains(q);
            if (match) hits.add(it);
        }
        // build one synthetic album to reuse PhotoTimeline
        Album a = new Album();
        a.setPath("search");
        a.getAlbumItems().addAll(hits);
        bucket.add(a);
        adapter.setTimeline(PhotoTimeline.from(bucket));
    }

    private PhotoTimeline emptyTimeline() { return PhotoTimeline.from(new ArrayList<>()); }

    private int d(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
