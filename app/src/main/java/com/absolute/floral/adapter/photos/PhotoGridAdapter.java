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
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

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

    private static final Object PAYLOAD_SEL = new Object();

    /** Select/deselect a contiguous run of grid positions (drag-select). */
    public void dragSelectRange(int start, int end, boolean sel) {
        selectionMode = true;
        int lo = Math.max(0, Math.min(start, end)), hi = Math.min(getItemCount() - 1, Math.max(start, end));
        for (int i = lo; i <= hi; i++) {
            PhotoTimeline.Row r = rowAt(i);
            if (r.header || r.item == null || r.item.getPath() == null) continue;
            if (sel) selected.add(r.item.getPath()); else selected.remove(r.item.getPath());
        }
        if (selectionListener != null) selectionListener.onSelectionChanged(selected.size());
        notifyItemRangeChanged(lo, hi - lo + 1, PAYLOAD_SEL);
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

    private Soma skin;                 // cached palette — refreshed on data change, not per bind
    private int thumbPx = 320;         // Glide decode target for one grid cell
    private final RequestOptions gridOpts = new RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop();

    public PhotoGridAdapter(Activity activity, PhotoTimeline timeline) {
        this.activity = activity;
        this.fullTimeline = timeline;
        this.timeline = apply(timeline);
        this.skin = SomaSkin.read(activity);
        recomputeThumb();
        setHasStableIds(true);
    }

    /** Re-read the palette once (call on theme change / data refresh), not per-bind. */
    public void refreshSkin() { this.skin = SomaSkin.read(activity); }

    /** A short "Jan 3 – Feb 12" label for the date span of the given position range. */
    public String rangeLabel(int first, int last) {
        if (timeline == null || timeline.rows.isEmpty()) return "";
        long lo = Long.MAX_VALUE, hi = Long.MIN_VALUE;
        int n = timeline.rows.size();
        for (int i = Math.max(0, first); i <= Math.min(n - 1, last); i++) {
            PhotoTimeline.Row r = timeline.rows.get(i);
            if (r.header || r.item == null) continue;
            long d = r.item.getDate();
            if (d <= 0) continue;
            if (d < lo) lo = d;
            if (d > hi) hi = d;
        }
        if (hi == Long.MIN_VALUE) return "";
        java.util.Calendar a = java.util.Calendar.getInstance(), b = java.util.Calendar.getInstance();
        a.setTimeInMillis(hi); b.setTimeInMillis(lo);   // newest first in the grid
        boolean sameYear = a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR);
        java.util.Calendar now = java.util.Calendar.getInstance();
        String fmtA = sameYear ? "MMM d" : "MMM d, yyyy";
        String hiS = android.text.format.DateFormat.format(fmtA, hi).toString();
        if (a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR))
            return hiS;
        String loS = android.text.format.DateFormat.format(
                sameYear ? "MMM d" : "MMM d, yyyy", lo).toString();
        return loS + " – " + hiS;
    }

    private void recomputeThumb() {
        int screen = activity.getResources().getDisplayMetrics().widthPixels;
        thumbPx = Math.max(160, Math.min(640, Math.round((screen / (float) Math.max(1, spanCount)) * 1.15f)));
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

    public void setSpanCount(int s) { this.spanCount = s; recomputeThumb(); }
    public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }

    /** When set, taps open the viewer against this exact list (buckets, search, home). */
    private com.absolute.floral.data.models.Album providedAlbum;
    public void setProvidedAlbum(com.absolute.floral.data.models.Album a) { this.providedAlbum = a; }

    /** Optimistically drop items (e.g. right after a confirmed delete) so the grid updates now. */
    public void removePaths(java.util.Collection<String> paths) {
        if (paths == null || paths.isEmpty() || fullTimeline == null) return;
        fullTimeline = fullTimeline.without(new java.util.HashSet<>(paths));
        this.timeline = apply(fullTimeline);
        lastAnimated = -1;
        notifyDataSetChanged();
    }

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
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SEL);
    }
    public void clearSelection() {
        if (!selectionMode && selected.isEmpty()) return;
        selectionMode = false; selected.clear();
        if (selectionListener != null) selectionListener.onSelectionChanged(0);
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SEL);
    }
    private void toggle(String path, int pos) {
        if (path == null) return;
        if (!selected.remove(path)) selected.add(path);
        if (selected.isEmpty()) selectionMode = false;
        if (selectionListener != null) selectionListener.onSelectionChanged(selected.size());
        if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos, PAYLOAD_SEL);
        else notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SEL);
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER)
            return new HeaderHolder(inf.inflate(R.layout.photos_grid_header, parent, false));
        return new ItemHolder(inf.inflate(R.layout.photos_grid_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && holder instanceof ItemHolder) {
            PhotoTimeline.Row r = rowAt(position);
            if (r.item != null) applySelectionVisual((ItemHolder) holder, r.item.getPath());
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PhotoTimeline.Row row = rowAt(position);
        if (holder instanceof HeaderHolder) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(row.title);
            tv.setTypeface(Soma.display(activity));
            if (skin != null) tv.setTextColor(skin.ink);
            return;
        }
        ItemHolder h = (ItemHolder) holder;
        final AlbumItem item = row.item;
        final String albumPath = row.albumPath;

        Object load = item.getUri(activity);
        if (load == null) load = item.getPath();
        Glide.with(activity).load(load)
                .apply(gridOpts)
                .override(thumbPx)
                .dontAnimate()
                .placeholder(R.color.bento_card_stroke)
                .into(h.image);

        boolean isVideo = MediaType.isVideo(item.getPath()) || item instanceof Video;
        h.dur.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        if (isVideo) h.dur.setText("▶");

        applySelectionVisual(h, item.getPath());

        h.image.setTransitionName(item.getPath());
        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (selectionMode) { toggle(item.getPath(), pos); return; }
            Intent intent = new Intent(activity, ItemActivity.class);
            intent.putExtra(ItemActivity.ALBUM_ITEM, item);
            if (providedAlbum != null) {
                ItemActivity.PROVIDED = providedAlbum;
                intent.putExtra(ItemActivity.ALBUM_PATH, ItemActivity.PROVIDED_PATH);
            } else {
                intent.putExtra(ItemActivity.ALBUM_PATH, albumPath);
            }
            try {
                ActivityOptionsCompat opts = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(activity, h.image, item.getPath());
                ActivityCompat.startActivityForResult(activity, intent, ItemActivity.VIEW_IMAGE, opts.toBundle());
            } catch (Exception e) {
                activity.startActivityForResult(intent, ItemActivity.VIEW_IMAGE);
            }
        });
        h.itemView.setOnLongClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            selectionMode = true;
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            toggle(item.getPath(), pos);
            if (dragStarter != null && pos != RecyclerView.NO_POSITION
                    && item.getPath() != null && selected.contains(item.getPath()))
                dragStarter.startDragAt(pos);
            return true;
        });
    }

    private void applySelectionVisual(ItemHolder h, String path) {
        boolean sel = path != null && selected.contains(path);
        h.scrim.setVisibility(sel ? View.VISIBLE : View.GONE);
        float s = sel ? 0.88f : 1f;
        if (h.itemView.getScaleX() != s) {
            h.itemView.animate().cancel();
            h.itemView.animate().scaleX(s).scaleY(s).setDuration(110)
                    .setInterpolator(Anim.ease()).start();
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (selectionMode || !(holder instanceof ItemHolder)) return;
        int pos = holder.getBindingAdapterPosition();
        if (pos <= lastAnimated) return;
        // don't chase a fast fling — only the first sweep gets the entrance
        if (pos > lastAnimated + 14) { lastAnimated = pos; return; }
        lastAnimated = pos;
        View v = holder.itemView;
        v.setAlpha(0f);
        v.setTranslationY(v.getResources().getDisplayMetrics().density * 14f);
        v.animate().alpha(1f).translationY(0f).setDuration(240)
                .setInterpolator(Anim.ease()).start();
    }

    // Stop any in-flight entrance/selection animation and reset the view before
    // RecyclerView detaches or recycles it — otherwise it can throw
    // "Tmp detached view should be removed ... before it can be recycled".
    private void resetAnim(RecyclerView.ViewHolder holder) {
        View v = holder.itemView;
        v.animate().cancel();
        v.setAlpha(1f);
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        resetAnim(holder);
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        resetAnim(holder);
        super.onViewRecycled(holder);
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
