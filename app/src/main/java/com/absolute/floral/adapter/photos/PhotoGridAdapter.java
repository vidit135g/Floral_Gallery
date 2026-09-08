package com.absolute.floral.adapter.photos;

import android.app.Activity;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.absolute.floral.R;
import com.absolute.floral.data.PhotoTimeline;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.models.Video;
import com.absolute.floral.soma.Anim;
import com.absolute.floral.soma.Soma;
import com.absolute.floral.soma.SomaSkin;
import com.absolute.floral.ui.ItemActivity;
import com.absolute.floral.util.MediaType;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * The single-scroll Library: the flat, date-sectioned photo grid, followed by the
 * "Collections" stack ({@link com.absolute.floral.bento.LibrarySections}) as one
 * full-width row at the very bottom — Apple-Photos iOS 18 style.
 */
public class PhotoGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_COLLECTIONS = 2;

    private java.util.List<com.absolute.floral.data.Memories.Memory> memories = new ArrayList<>();
    private com.absolute.floral.bento.LibrarySnapshot snapshot;
    private java.util.List<com.absolute.floral.people.PeopleIndex.Person> people = new ArrayList<>();
    private java.util.List<com.absolute.floral.places.PlacesIndex.Place> places = new ArrayList<>();
    private java.util.List<com.absolute.floral.data.models.Album> albums = new ArrayList<>();
    private boolean bentoEnabled = true;

    public void setBentoEnabled(boolean b) { this.bentoEnabled = b; }

    public void setAlbums(java.util.List<com.absolute.floral.data.models.Album> a) {
        this.albums = a == null ? new ArrayList<>() : a;
        invalidateCollections();
    }
    public void setPlaces(java.util.List<com.absolute.floral.places.PlacesIndex.Place> p) {
        this.places = p == null ? new ArrayList<>() : p;
        invalidateCollections();
    }
    public void setMemories(java.util.List<com.absolute.floral.data.Memories.Memory> m) {
        boolean was = hasCollections();
        this.memories = m == null ? new ArrayList<>() : m;
        if (was != hasCollections()) notifyDataSetChanged();
        else invalidateCollections();
    }
    public void setSnapshot(com.absolute.floral.bento.LibrarySnapshot s) {
        this.snapshot = s;
        invalidateCollections();
    }
    public void setPeople(java.util.List<com.absolute.floral.people.PeopleIndex.Person> p) {
        this.people = p == null ? new ArrayList<>() : p;
        invalidateCollections();
    }

    private void invalidateCollections() {
        if (hasCollections()) notifyItemChanged(collectionsPos());
    }

    /** The Collections stack shows whenever we have any library data to summarise. */
    private boolean hasCollections() {
        return bentoEnabled && (snapshot != null || (memories != null && !memories.isEmpty())
                || (albums != null && !albums.isEmpty()));
    }
    private int collectionsPos() { return timeline == null ? 0 : timeline.rows.size(); }
    private PhotoTimeline.Row rowAt(int position) { return timeline.rows.get(position); }

    public interface SelectionListener { void onSelectionChanged(int count); }

    private final Activity activity;
    private PhotoTimeline timeline;
    private int spanCount = 3;
    private int lastAnimated = -1;

    private boolean selectionMode = false;
    private final java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
    private SelectionListener selectionListener;

    public PhotoGridAdapter(Activity activity, PhotoTimeline timeline) {
        this.activity = activity;
        this.timeline = timeline;
        setHasStableIds(true);
    }

    public void setTimeline(PhotoTimeline t) {
        if (t == null) return;
        if (timeline != null && t.rows.size() == timeline.rows.size() && t.items.size() == timeline.items.size()) {
            this.timeline = t;
            return;
        }
        this.timeline = t;
        lastAnimated = -1;
        notifyDataSetChanged();
    }
    public void setSpanCount(int s) { this.spanCount = s; }
    public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }

    public GridLayoutManager.SpanSizeLookup spanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                if (hasCollections() && position == collectionsPos()) return spanCount;
                return rowAt(position).header ? spanCount : 1;
            }
        };
    }

    @Override public int getItemCount() {
        int n = timeline == null ? 0 : timeline.rows.size();
        return n + (hasCollections() ? 1 : 0);
    }
    @Override public int getItemViewType(int position) {
        if (hasCollections() && position == collectionsPos()) return TYPE_COLLECTIONS;
        return rowAt(position).header ? TYPE_HEADER : TYPE_ITEM;
    }
    @Override public long getItemId(int position) {
        if (hasCollections() && position == collectionsPos()) return "collections".hashCode();
        PhotoTimeline.Row r = rowAt(position);
        return r.header ? ("h" + r.title).hashCode() : (r.item.getPath() == null ? position : r.item.getPath().hashCode());
    }

    /* selection */
    public boolean isSelectionMode() { return selectionMode; }
    public List<String> selectedPaths() { return new ArrayList<>(selected); }
    public void clearSelection() {
        selectionMode = false; selected.clear();
        if (selectionListener != null) selectionListener.onSelectionChanged(0);
        notifyDataSetChanged();
    }
    private void toggle(String path) {
        if (path == null) return;
        if (!selected.remove(path)) selected.add(path);
        if (selected.isEmpty()) selectionMode = false;
        if (selectionListener != null) selectionListener.onSelectionChanged(selected.size());
        notifyDataSetChanged();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_COLLECTIONS) {
            android.widget.FrameLayout box = new android.widget.FrameLayout(activity);
            box.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new MemoriesHolder(box);
        }
        if (viewType == TYPE_HEADER)
            return new HeaderHolder(inf.inflate(R.layout.photos_grid_header, parent, false));
        return new ItemHolder(inf.inflate(R.layout.photos_grid_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MemoriesHolder) {
            android.widget.FrameLayout box = (android.widget.FrameLayout) holder.itemView;
            box.removeAllViews();
            box.addView(com.absolute.floral.bento.LibrarySections.build(
                    activity, albums, memories, people, places, snapshot));
            return;
        }
        PhotoTimeline.Row row = rowAt(position);
        Soma soma = SomaSkin.read(activity);
        if (holder instanceof HeaderHolder) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(row.title);
            tv.setTypeface(Soma.display(activity));
            if (soma != null) tv.setTextColor(soma.ink);
            return;
        }
        ItemHolder h = (ItemHolder) holder;
        final AlbumItem item = row.item;
        final String albumPath = row.albumPath;

        Object load = item.getUri(activity);
        if (load == null) load = item.getPath();
        Glide.with(activity).load(load)
                .centerCrop()
                .placeholder(R.color.bento_card_stroke)
                .into(h.image);

        boolean isVideo = MediaType.isVideo(item.getPath()) || item instanceof Video;
        if (isVideo && item.getDate() >= 0) {
            h.dur.setVisibility(View.VISIBLE);
            h.dur.setText("▶");
        } else h.dur.setVisibility(View.GONE);

        h.scrim.setVisibility(selected.contains(item.getPath()) ? View.VISIBLE : View.GONE);
        float s = selected.contains(item.getPath()) ? 0.86f : 1f;
        h.itemView.setScaleX(s); h.itemView.setScaleY(s);

        h.image.setTransitionName(item.getPath());
        h.itemView.setOnClickListener(v -> {
            if (selectionMode) { toggle(item.getPath()); return; }
            Intent intent = new Intent(activity, ItemActivity.class);
            intent.putExtra(ItemActivity.ALBUM_ITEM, item);
            intent.putExtra(ItemActivity.ALBUM_PATH, albumPath);
            try {
                ActivityOptionsCompat opts = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(activity, h.image, item.getPath());
                ActivityCompat.startActivityForResult(activity, intent, ItemActivity.VIEW_IMAGE, opts.toBundle());
            } catch (Exception e) {
                activity.startActivityForResult(intent, ItemActivity.VIEW_IMAGE);
            }
        });
        h.itemView.setOnLongClickListener(v -> {
            selectionMode = true;
            toggle(item.getPath());
            return true;
        });

    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (selectionMode || !(holder instanceof ItemHolder)) return;
        int pos = holder.getBindingAdapterPosition();
        if (pos <= lastAnimated) return;
        lastAnimated = pos;
        View v = holder.itemView;
        v.setAlpha(0f);
        v.setTranslationY(v.getResources().getDisplayMetrics().density * 24f);
        v.animate().alpha(1f).translationY(0f).setDuration(420)
                .setInterpolator(Anim.ease()).start();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        HeaderHolder(View v) { super(v); }
    }
    static class MemoriesHolder extends RecyclerView.ViewHolder {
        MemoriesHolder(View v) {
            super(v);
            v.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        }
    }
    static class ItemHolder extends RecyclerView.ViewHolder {
        final ImageView image; final TextView dur; final View scrim;
        ItemHolder(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            dur = v.findViewById(R.id.video_dur);
            scrim = v.findViewById(R.id.sel_scrim);
        }
    }
}
