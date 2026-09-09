package com.absolute.floral.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.PhotoSelectionBar;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.List;

/** A generic "here are some photos" screen — Favorites, Archive, a Person, etc. */
public class BucketActivity extends AppCompatActivity {

    public static String TITLE = "";
    public static String KICKER = "";
    public static List<AlbumItem> ITEMS;

    private PhotoGridAdapter adapter;
    private PhotoSelectionBar selectionBar;
    private TextView countLabel;
    private final ArrayList<AlbumItem> items = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        Soma soma = SomaSkin.read(this);
        if (ITEMS == null) { finish(); return; }
        items.addAll(ITEMS);

        FrameLayout host = new FrameLayout(this);
        host.setId(R.id.root_view);
        host.setBackgroundColor(soma.ground[1]);
        setContentView(host);
        SomaSkin.statusBarIcons(this, soma);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        host.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int p = Math.round(getResources().getDisplayMetrics().density * 20);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(p, Math.round(p * 2.2f), p, p / 2);
        if (KICKER != null && !KICKER.isEmpty()) {
            TextView k = new TextView(this);
            k.setText(KICKER);
            k.setTextColor(soma.inkMute);
            k.setLetterSpacing(0.18f);
            k.setTextSize(11);
            k.setTypeface(Soma.body(this));
            head.addView(k);
        }
        TextView t = new TextView(this);
        t.setText(TITLE);
        t.setTextColor(soma.ink);
        t.setTextSize(28);
        t.setTypeface(Soma.display(this));
        head.addView(t);
        countLabel = new TextView(this);
        countLabel.setTextColor(soma.inkMute);
        countLabel.setTextSize(12);
        countLabel.setTypeface(Soma.body(this));
        head.addView(countLabel);
        root.addView(head);
        Anim.enter(head, 40);

        RecyclerView rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(Math.round(p * 0.3f), 0, Math.round(p * 0.3f), Math.round(p * 4f));
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);

        adapter = new PhotoGridAdapter(this, buildTimeline());
        adapter.setSpanCount(3);
        adapter.setProvidedAlbum(syntheticAlbum());
        adapter.setFavoritePaths(FlagStore.favorites(this).all());
        glm.setSpanSizeLookup(adapter.spanSizeLookup());
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);

        final com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener dsl =
                new com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener()
                        .withSelectListener((start, end, isSelected) ->
                                adapter.dragSelectRange(start, end, isSelected));
        rv.addOnItemTouchListener(dsl);
        adapter.setDragStarter(dsl::startDragSelection);

        root.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        selectionBar = new PhotoSelectionBar(this, host, adapter, 0)
                .onChanged(this::reload);
        selectionBar.wire();

        refreshCount();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Nothing here yet");
            empty.setTextColor(soma.inkMute);
            empty.setTypeface(Soma.serif(this));
            empty.setTextSize(16);
            empty.setPadding(p, p, p, p);
            head.addView(empty);
        }
    }

    private Album syntheticAlbum() {
        Album a = new Album();
        a.setPath("bucket");
        a.getAlbumItems().addAll(items);
        return a;
    }

    private PhotoTimeline buildTimeline() {
        ArrayList<Album> bucket = new ArrayList<>();
        bucket.add(syntheticAlbum());
        return PhotoTimeline.from(bucket);
    }

    private void refreshCount() {
        countLabel.setText(items.size() + (items.size() == 1 ? " item" : " items"));
    }

    /** After a delete: drop any items that no longer exist on disk. */
    private void reload() {
        java.util.Iterator<AlbumItem> it = items.iterator();
        while (it.hasNext()) {
            AlbumItem ai = it.next();
            String path = ai == null ? null : ai.getPath();
            if (path != null && !new java.io.File(path).exists()) it.remove();
        }
        adapter.setProvidedAlbum(syntheticAlbum());
        adapter.setTimeline(buildTimeline());
        adapter.setFavoritePaths(FlagStore.favorites(this).all());
        refreshCount();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (selectionBar != null) selectionBar.onActivityResult(requestCode, resultCode);
    }

    @Override public void onBackPressed() {
        if (adapter != null && adapter.isSelectionMode()) { adapter.clearSelection(); return; }
        super.onBackPressed();
    }
}
