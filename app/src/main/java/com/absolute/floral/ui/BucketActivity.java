package com.absolute.floral.ui;

import android.os.Bundle;
import android.view.ViewGroup;
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
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.List;

/** A generic "here are some photos" screen — Favorites, Archive, a Person, etc. */
public class BucketActivity extends AppCompatActivity {

    public static String TITLE = "";
    public static String KICKER = "";
    public static List<AlbumItem> ITEMS;

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        Soma soma = SomaSkin.read(this);
        if (ITEMS == null) { finish(); return; }
        List<AlbumItem> items = new ArrayList<>(ITEMS);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(soma.ground[1]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, soma);

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
        TextView c = new TextView(this);
        c.setText(items.size() + (items.size() == 1 ? " item" : " items"));
        c.setTextColor(soma.inkMute);
        c.setTextSize(12);
        c.setTypeface(Soma.body(this));
        head.addView(c);
        root.addView(head);
        Anim.enter(head, 40);

        RecyclerView rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(Math.round(p * 0.3f), 0, Math.round(p * 0.3f), p);
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);

        Album synthetic = new Album();
        synthetic.setPath("bucket");
        synthetic.getAlbumItems().addAll(items);
        ArrayList<Album> bucket = new ArrayList<>();
        bucket.add(synthetic);

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, PhotoTimeline.from(bucket));
        adapter.setSpanCount(3);
        glm.setSpanSizeLookup(adapter.spanSizeLookup());
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        root.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

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
}
