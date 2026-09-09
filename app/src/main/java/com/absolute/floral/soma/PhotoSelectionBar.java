package com.absolute.floral.soma;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.absolute.floral.R;
import com.absolute.floral.adapter.photos.PhotoGridAdapter;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.RecentStore;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.data.provider.MediaProvider;
import com.absolute.floral.util.MediaOps;

import java.util.ArrayList;
import java.util.List;

/**
 * The Apple-Photos style Select action bar (bottom: count · Share · Favourite ·
 * Delete · Done, top: Cancel · title · Select All), reusable by any screen with
 * a {@link PhotoGridAdapter} inside a {@link FrameLayout}-ish host. Wire it once;
 * it shows/hides itself with the adapter's selection state.
 */
public class PhotoSelectionBar {

    public static final int REQ_DELETE = 0x5E1D;

    public interface OnChanged { void changed(); }

    private final Activity a;
    private final ViewGroup host;
    private final PhotoGridAdapter adapter;
    private final int bottomInset;
    private final float d;
    private final Soma soma;

    private View bottomBar, topBar;
    private TextView countText, titleText;
    private OnChanged onChanged;
    private List<String> pendingDelete;

    public PhotoSelectionBar(Activity a, ViewGroup host, PhotoGridAdapter adapter, int bottomInset) {
        this.a = a;
        this.host = host;
        this.adapter = adapter;
        this.bottomInset = bottomInset;
        this.d = a.getResources().getDisplayMetrics().density;
        this.soma = SomaSkin.read(a);
    }

    public PhotoSelectionBar onChanged(OnChanged l) { this.onChanged = l; return this; }

    /** Drive show/hide from the adapter's selection changes. */
    public void wire() {
        adapter.setSelectionListener(count -> {
            if (count <= 0 && !adapter.isSelectionMode()) hide();
            else show(count);
        });
    }

    private int dp(float v) { return Math.round(v * d); }

    private void show(int count) {
        build();
        bottomBar.setVisibility(View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        String label = count == 0 ? "Select Items" : count + " Selected";
        countText.setText(label);
        titleText.setText(label);
    }

    public void hide() {
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        if (topBar != null) topBar.setVisibility(View.GONE);
    }

    private TextView text(String s, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(14);
        t.setTypeface(Soma.body(a), bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private TextView action(String label, Runnable r) {
        TextView t = text(label, soma.ink, false);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        t.setOnClickListener(v -> r.run());
        return t;
    }

    private void build() {
        if (bottomBar != null) return;
        int blue = a.getResources().getColor(R.color.ios_blue);

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(soma.lightBase ? 0xF7FFFFFF : 0xF71E1F20);
        bar.setBackground(bg);
        bar.setElevation(dp(10));
        countText = text("1 Selected", soma.ink, false);
        bar.addView(countText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(action("Share", this::bulkShare));
        bar.addView(action("Favourite", this::bulkFavourite));
        bar.addView(action("Delete", this::bulkDelete));
        TextView done = text("Done", blue, true);
        done.setPadding(dp(12), dp(8), dp(4), dp(8));
        done.setOnClickListener(v -> adapter.clearSelection());
        bar.addView(done);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        lp.bottomMargin = bottomInset;
        host.addView(bar, lp);
        bottomBar = bar;

        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(40), dp(12), dp(10));
        GradientDrawable tbg = new GradientDrawable();
        tbg.setColor(soma.lightBase ? 0xFFFFFFFF : 0xFF131314);
        top.setBackground(tbg);
        top.setElevation(dp(12));
        TextView cancel = text("Cancel", blue, false);
        cancel.setPadding(dp(6), dp(8), dp(10), dp(8));
        cancel.setOnClickListener(v -> adapter.clearSelection());
        top.addView(cancel);
        titleText = text("Select Items", soma.ink, true);
        titleText.setGravity(Gravity.CENTER);
        top.addView(titleText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView all = text("Select All", blue, false);
        all.setPadding(dp(10), dp(8), dp(6), dp(8));
        all.setOnClickListener(v -> adapter.dragSelectRange(0, adapter.getItemCount() - 1, true));
        top.addView(all);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.TOP;
        host.addView(top, tlp);
        topBar = top;
    }

    /* -------- bulk ops -------- */

    private List<Uri> selectedUris() {
        List<Uri> uris = new ArrayList<>();
        for (String p : adapter.selectedPaths()) {
            AlbumItem it = AlbumItem.getInstance(a, p);
            Uri u = it == null ? null : it.getUri(a);
            if (u != null) uris.add(u);
        }
        return uris;
    }

    private void bulkShare() {
        final java.util.List<String> paths = new ArrayList<>(adapter.selectedPaths());
        if (paths.isEmpty()) return;
        new androidx.appcompat.app.AlertDialog.Builder(a)
                .setTitle("Share " + paths.size() + (paths.size() == 1 ? " item" : " items"))
                .setItems(new CharSequence[]{ "Share", "Share without location & metadata" }, (d, which) -> {
                    RecentStore.markShared(a, paths);
                    if (which == 0) {
                        ArrayList<Uri> uris = new ArrayList<>(selectedUris());
                        if (uris.isEmpty()) return;
                        Intent send = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        send.setType("*/*");
                        send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        a.startActivity(Intent.createChooser(send, "Share"));
                    } else {
                        Toast.makeText(a, "Cleaning metadata…", Toast.LENGTH_SHORT).show();
                        com.absolute.floral.util.SafeShare.prepare(a, paths, (uris, stripped) -> {
                            if (uris.isEmpty()) return;
                            a.startActivity(com.absolute.floral.util.SafeShare.chooser(a, uris));
                        });
                    }
                    adapter.clearSelection();
                })
                .show();
    }

    private void bulkFavourite() {
        FlagStore fav = FlagStore.favorites(a);
        for (String p : adapter.selectedPaths()) fav.toggle(p);
        adapter.setFavoritePaths(fav.all());
        adapter.clearSelection();
        if (onChanged != null) onChanged.changed();
    }

    private void bulkDelete() {
        List<String> paths = new ArrayList<>(adapter.selectedPaths());
        if (paths.isEmpty()) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            PendingIntent pi = MediaOps.deleteRequest(a, MediaOps.urisFor(a, paths));
            if (pi != null) {
                try {
                    pendingDelete = paths;
                    a.startIntentSenderForResult(pi.getIntentSender(), REQ_DELETE, null, 0, 0, 0);
                    return;
                } catch (android.content.IntentSender.SendIntentException e) { e.printStackTrace(); }
            }
        }
        Toast.makeText(a, R.string.error, Toast.LENGTH_SHORT).show();
        adapter.clearSelection();
    }

    /** Host forwards its onActivityResult here. */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode != REQ_DELETE) return;
        if (resultCode == Activity.RESULT_OK && pendingDelete != null) {
            adapter.removePaths(pendingDelete);
            MediaProvider.dataChanged = true;
            if (onChanged != null) onChanged.changed();
        }
        pendingDelete = null;
        adapter.clearSelection();
    }
}
