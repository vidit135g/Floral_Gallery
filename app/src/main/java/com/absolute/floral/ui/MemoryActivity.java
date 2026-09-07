package com.absolute.floral.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.Album;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;

import java.util.ArrayList;
import java.util.List;

/** A single Memory opened from the carousel — its photos in a grid. */
public class MemoryActivity extends AppCompatActivity {

    public static final String EXTRA_INDEX = "index";
    public static List<Memories.Memory> MEMORIES;   // handed over statically to avoid Parcelable

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        Soma soma = SomaSkin.read(this);
        int idx = getIntent().getIntExtra(EXTRA_INDEX, 0);
        if (MEMORIES == null || idx >= MEMORIES.size()) { finish(); return; }
        Memories.Memory m = MEMORIES.get(idx);

        LinearLayout root = new LinearLayout(this);
        root.setId(R.id.root_view);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(soma.ground[1]);
        setContentView(root);
        SomaSkin.statusBarIcons(this, soma);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(getResources().getDisplayMetrics().density * 20);
        head.setPadding(p, Math.round(p * 2.2f), p, p / 2);
        TextView k = new TextView(this);
        k.setText(m.kicker);
        k.setTextColor(soma.inkMute);
        k.setLetterSpacing(0.18f);
        k.setTextSize(11);
        k.setTypeface(Soma.body(this));
        head.addView(k);
        TextView t = new TextView(this);
        t.setText(m.title);
        t.setTextColor(soma.ink);
        t.setTextSize(28);
        t.setTypeface(Soma.display(this));
        head.addView(t);
        root.addView(head);
        Anim.enter(head, 40);

        RecyclerView rv = new RecyclerView(this);
        rv.setClipToPadding(false);
        rv.setPadding(Math.round(p * 0.3f), 0, Math.round(p * 0.3f), p);
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        rv.setLayoutManager(glm);

        Album synthetic = new Album();
        synthetic.setPath(m.items.isEmpty() ? "memory" :
                parent(m.items.get(0).getPath()));
        synthetic.getAlbumItems().addAll(m.items);
        ArrayList<Album> bucket = new ArrayList<>();
        bucket.add(synthetic);

        PhotoGridAdapter adapter = new PhotoGridAdapter(this, PhotoTimeline.from(bucket));
        adapter.setSpanCount(3);
        glm.setSpanSizeLookup(adapter.spanSizeLookup());
        rv.setAdapter(adapter);
        rv.setItemAnimator(null);
        root.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private String parent(String path) {
        if (path == null) return "memory";
        int i = path.lastIndexOf('/');
        return i > 0 ? path.substring(0, i) : "memory";
    }
}
