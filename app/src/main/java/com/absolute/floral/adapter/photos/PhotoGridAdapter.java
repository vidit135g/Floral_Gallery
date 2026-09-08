package com.absolute.floral.adapter.photos;

import android.app.Activity;
import android.content.Intent;
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
 * The <b>Library</b> tab grid — a flat photo/video grid, Apple-Photos iOS 18
 * style. Continuous by default (no date section headers; the toolbar subtitle
 * and the fast-scroller carry the date affordance). The Collections tab is a
 * separate screen ({@link com.absolute.floral.bento.CollectionsScreen}).
 */
public class PhotoGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    /** Optional filter applied to the timeline before display (Library ▸ … ▸ Filter). */
    public enum Filter { ALL, FAVORITES, EDITED, PHOTOS, VIDEOS, SCREENSHOTS }

    public interface SelectionListener { void onSelectionChanged(int count); }
    public interface DragStarter { void startDragAt(int position); }
    private DragStarter dragStarter;
    public void setDragStarter(DragStarter d) { this.dragStarter = d; }

    /** Select/deselect a contiguous run of grid positions (drag-select). */
    public void dragSelectRange(int start, int end, boolean sel) {
        selectionMode = true;
        for (int i = start; i <= end && i < getItemCount(); i++) {
            PhotoTimeline.Row r = rowAt(i);
            if (r.header || r.item == null || r.item.getPath() == null) continue;
            if (sel) selected.add(r.item.getPath()); else selected.remove(r.item.getPath());
        }
        if (selectionListener != null) selectionListener.onSelectionChanged(selected.size());
        notifyDataSetChanged();
    }

    private final Activity activity;
    private PhotoTimeline fullTimeline;   // as supplied
    private PhotoTimeline timeline;       // after filter
    private int spanCount = 3;
    private int lastAnimated = -1;
    private boolean continuous = false;
    private Filter filter = Filter.ALL;
    private java.util.Set<String> favoritePaths = new java.util.HashSet<>();

    private boolean selectionMode = false;
    private final java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
    private SelectionListener selectionListener;

    public PhotoGridAdapter(Activity activity, PhotoTimeline timeline) {
        this.activity = activity;
        this.fullTimeline = timeline;
        this.timeline = apply(timeline);
        setHasStableIds(true);
    }

    public void setTimeline(PhotoTimeline t) {
        if (t == null) return;
        this.fullTimeline = t;
        PhotoTimeline next = apply(t);
        if (timeline != null && next.rows.size() == timeline.rows.size()
                && next.items.size() == timeline.items.size()) {
            this.timeline = next;
            return;
        }
        this.timeline = next;
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    public void setSpanCount(int s) { this.spanCount = s; }
    public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }

    /** Library uses continuous (no headers); Bucket / Search keep the date headers. */
    public void setContinuous(boolean c) {
        if (this.continuous == c) return;
        this.continuous = c;
        this.timeline = apply(fullTimeline);
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    public void setFilter(Filter f) {
        if (f == null) f = Filter.ALL;
        if (this.filter == f) return;
        this.filter = f;
        this.timeline = apply(fullTimeline);
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    public Filter getFilter() { return filter; }

    public void setFavoritePaths(java.util.Set<String> paths) {
        this.favoritePaths = paths == null ? new java.util.HashSet<>() : paths;
        if (filter == Filter.FAVORITES) { this.timeline = apply(fullTimeline); notifyDataSetChanged(); }
    }

    /* -------- timeline shaping -------- */

    private PhotoTimeline apply(PhotoTimeline src) {
        if (src == null) return null;
        List<AlbumItem> keep = new ArrayList<>();
        java.util.Map<String, String> pathAlbum = new java.util.HashMap<>();
        for (PhotoTimeline.Row r : src.rows) {
            if (r.header || r.item == null) continue;
            if (!passesFilter(r.item)) continue;
            keep.add(r.item);
            if (r.item.getPath() != null) pathAlbum.put(r.item.getPath(), r.albumPath);
        }
        PhotoTimeline out;
        if (continuous || filter != Filter.ALL) {
            out = PhotoTimeline.flat(keep, pathAlbum);
        } else {
            out = src;
        }
        return out;
    }

    private boolean passesFilter(AlbumItem it) {
        String p = it.getPath() == null ? "" : it.getPath().toLowerCase(java.util.Locale.ROOT);
        String n = it.getName() == null ? "" : it.getName().toLowerCase(java.util.Locale.ROOT);
        boolean video = MediaType.isVideo(it.getPath()) || it instanceof Video;
        switch (filter) {
            case FAVORITES:   return it.getPath() != null && favoritePaths.contains(it.getPath());
            case PHOTOS:      return !video;
            case VIDEOS:      return video;
            case SCREENSHOTS: return p.contains("screenshot") || n.contains("screenshot");
            case EDITED:      return p.contains("/edited") || n.contains("edit") || p.contains("_edit");
            case ALL:
            default:          return true;
        }
    }

    private PhotoTimeline.Row rowAt(int position) { return timeline.rows.get(position); }

    public GridLayoutManager.SpanSizeLookup spanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return rowAt(position).header ? spanCount : 1;
            }
        };
    }

    @Override public int getItemCount() { return timeline == null ? 0 : timeline.rows.size(); }

    @Override public int getItemViewType(int position) {
        return rowAt(position).header ? TYPE_HEADER : TYPE_ITEM;
    }

    @Override public long getItemId(int position) {
        PhotoTimeline.Row r = rowAt(position);
        return r.header ? ("h" + r.title).hashCode()
                : (r.item.getPath() == null ? position : r.item.getPath().hashCode());
    }

    /* selection */
    public boolean isSelectionMode() { return selectionMode; }
    public List<String> selectedPaths() { return new ArrayList<>(selected); }
    public void enterSelection() {
        if (selectionMode) return;
        selectionMode = true;
        notifyDataSetChanged();
    }
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
        if (viewType == TYPE_HEADER)
            return new HeaderHolder(inf.inflate(R.layout.photos_grid_header, parent, false));
        return new ItemHolder(inf.inflate(R.layout.photos_grid_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
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
        if (isVideo) {
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
            if (dragStarter != null && item.getPath() != null && selected.contains(item.getPath())) {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) dragStarter.startDragAt(pos);
            }
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
