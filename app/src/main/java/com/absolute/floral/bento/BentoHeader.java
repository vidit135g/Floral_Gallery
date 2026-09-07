package com.absolute.floral.bento;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import com.absolute.floral.R;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.ui.BucketActivity;
import com.absolute.floral.ui.ColorSearchActivity;
import com.absolute.floral.ui.InsightsActivity;
import com.absolute.floral.ui.MemoryActivity;
import com.absolute.floral.data.FlagStore;

import java.util.List;

/** The colourful bento mosaic pinned above the Photos timeline. */
public final class BentoHeader {

    private BentoHeader() {}

    public static View build(final Activity a, LibrarySnapshot snap,
                             final List<Memories.Memory> memories,
                             final List<PeopleIndex.Person> people) {
        BentoLayout b = new BentoLayout(a);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final boolean hasMem = memories != null && !memories.isEmpty();
        final Memories.Memory mem = hasMem ? memories.get(0) : null;

        /* ---- row A : big memory + stacked (recent, people) ---- */
        LinearLayout rowA = b.row(168);
        BentoTile big = b.tile(rowA, 1.7f, v -> {
            if (!hasMem) return;
            MemoryActivity.MEMORIES = memories;
            Intent it = new Intent(a, MemoryActivity.class);
            it.putExtra(MemoryActivity.EXTRA_INDEX, 0);
            a.startActivity(it);
        });
        if (mem != null && mem.cover() != null) {
            big.photo(cover(a, mem.cover()), 0).label(mem.title).sub(mem.items.size() + " photos").icon(R.drawable.ic_star_border_white).icon(0);
        } else {
            big.gradient(2).label("Memories").sub("Your highlights appear here");
        }

        LinearLayout colA = b.column(rowA, 1f);
        BentoTile recent = b.stacked(colA, 1f, v ->
                openBucket(a, "Recently added", recentItems(snap)));
        if (snap != null && snap.newest != null)
            recent.photo(cover(a, snap.newest), 1).label("Recent").value(snap.thisWeek > 0 ? String.valueOf(snap.thisWeek) : "");
        else recent.gradient(1).label("Recent");

        BentoTile peopleT = b.stacked(colA, 1f, v -> {
            if (people != null && !people.isEmpty()) {
                BucketActivity.TITLE = "Person 1";
                BucketActivity.KICKER = "PEOPLE";
                BucketActivity.ITEMS = new java.util.ArrayList<>(people.get(0).photos);
                a.startActivity(new Intent(a, BucketActivity.class));
            }
        });
        int pc = people == null ? 0 : people.size();
        if (people != null && !people.isEmpty() && people.get(0).cover != null) {
            peopleT.gradient(6);
            peopleT.label("People").value(String.valueOf(pc));
        } else {
            peopleT.gradient(6).label("People").sub("Scanning…");
        }

        /* ---- row B : three stat tiles ---- */
        LinearLayout rowB = b.row(112);
        BentoTile lib = b.tile(rowB, 1f, v -> a.startActivity(new Intent(a, InsightsActivity.class)));
        lib.gradient(4).label("In your library").icon(R.drawable.ic_search_white).icon(0);
        if (snap != null) lib.countTo(snap.photos + snap.videos, "");
        else lib.value("—");
        lib.sub("Tap for insights");

        BentoTile favs = b.tile(rowB, 1f, v ->
                openBucket(a, "Favorites", favItems(a)));
        favs.gradient(0).label("Favorites");
        if (snap != null) favs.countTo(snap.favorites, "");
        else favs.value("0");

        BentoTile colours = b.tile(rowB, 1f, v -> a.startActivity(new Intent(a, ColorSearchActivity.class)));
        colours.gradient(9).label("Colours").sub("Search by hue");
        colours.value("");

        return b;
    }

    private static Object cover(Activity a, AlbumItem it) {
        Object u = it.getUri(a);
        return u != null ? u : it.getPath();
    }

    private static java.util.List<AlbumItem> recentItems(LibrarySnapshot snap) {
        java.util.List<AlbumItem> l = new java.util.ArrayList<>();
        // recompute from provider — cheap
        java.util.ArrayList<com.absolute.floral.data.models.Album> al =
                com.absolute.floral.data.provider.MediaProvider.getAlbums();
        if (al != null) for (com.absolute.floral.data.models.Album x : al)
            if (x.getAlbumItems() != null) l.addAll(x.getAlbumItems());
        java.util.Collections.sort(l, (p, q) -> Long.compare(q.getDate(), p.getDate()));
        return l.size() > 120 ? new java.util.ArrayList<>(l.subList(0, 120)) : l;
    }

    private static java.util.List<AlbumItem> favItems(Activity a) {
        java.util.Set<String> paths = FlagStore.favorites(a).all();
        java.util.List<AlbumItem> out = new java.util.ArrayList<>();
        java.util.ArrayList<com.absolute.floral.data.models.Album> al =
                com.absolute.floral.data.provider.MediaProvider.getAlbums();
        if (al != null) for (com.absolute.floral.data.models.Album x : al)
            if (x.getAlbumItems() != null)
                for (AlbumItem it : x.getAlbumItems())
                    if (it.getPath() != null && paths.contains(it.getPath())) out.add(it);
        return out;
    }

    private static void openBucket(Activity a, String title, java.util.List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = "";
        BucketActivity.ITEMS = items;
        a.startActivity(new Intent(a, BucketActivity.class));
    }
}
