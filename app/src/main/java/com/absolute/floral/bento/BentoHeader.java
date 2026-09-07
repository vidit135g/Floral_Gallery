package com.absolute.floral.bento;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import com.absolute.floral.R;
import com.absolute.floral.data.FlagStore;
import com.absolute.floral.data.Memories;
import com.absolute.floral.data.Story;
import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.places.PlacesIndex;
import com.absolute.floral.ui.BrowseByDateActivity;
import com.absolute.floral.ui.BucketActivity;
import com.absolute.floral.ui.ColorSearchActivity;
import com.absolute.floral.ui.InsightsActivity;
import com.absolute.floral.ui.MemoryActivity;
import com.absolute.floral.ui.PlacesActivity;
import com.absolute.floral.ui.StoryPlayerActivity;

import java.util.List;

/** Featured-stories row + colourful bento mosaic, pinned above the Photos timeline. */
public final class BentoHeader {

    private BentoHeader() {}

    public static View build(final Activity a, LibrarySnapshot snap,
                             final List<Memories.Memory> memories,
                             final List<PeopleIndex.Person> people,
                             final List<Story> stories,
                             final List<PlacesIndex.Place> places) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (stories != null && !stories.isEmpty()) {
            wrap.addView(new StoriesTray(a, stories));
        }

        BentoLayout b = new BentoLayout(a);
        wrap.addView(b);

        final boolean hasMem = memories != null && !memories.isEmpty();

        /* row A : big story + stacked (recent, people) */
        LinearLayout rowA = b.row(168);
        BentoTile big = b.tile(rowA, 1.7f, v -> {
            if (stories != null && !stories.isEmpty()) {
                StoryPlayerActivity.STORIES = stories;
                StoryPlayerActivity.START_INDEX = 0;
                a.startActivity(new Intent(a, StoryPlayerActivity.class));
            } else if (hasMem) {
                MemoryActivity.MEMORIES = memories;
                Intent it = new Intent(a, MemoryActivity.class);
                it.putExtra(MemoryActivity.EXTRA_INDEX, 0);
                a.startActivity(it);
            }
        });
        Memories.Memory mem = hasMem ? memories.get(0) : null;
        if (mem != null && mem.cover() != null) {
            big.photo(cover(a, mem.cover()), 0).label(mem.title).sub(mem.items.size() + " photos")
                    .icon(R.drawable.ic_play_arrow_white);
        } else {
            big.gradient(2).label("Stories").sub("Your highlights, auto-made");
        }

        LinearLayout colA = b.column(rowA, 1f);
        BentoTile recent = b.stacked(colA, 1f, v -> openBucket(a, "Recently added", recentItems()));
        if (snap != null && snap.newest != null)
            recent.photo(cover(a, snap.newest), 1).label("Recent")
                    .value(snap.thisWeek > 0 ? String.valueOf(snap.thisWeek) : "");
        else recent.gradient(1).label("Recent");

        BentoTile peopleT = b.stacked(colA, 1f, v -> {
            if (people != null && !people.isEmpty()) {
                BucketActivity.TITLE = "Person 1"; BucketActivity.KICKER = "PEOPLE";
                BucketActivity.ITEMS = new java.util.ArrayList<>(people.get(0).photos);
                a.startActivity(new Intent(a, BucketActivity.class));
            }
        });
        int pc = people == null ? 0 : people.size();
        peopleT.gradient(6).label("People").value(pc > 0 ? String.valueOf(pc) : "").sub(pc == 0 ? "Scanning…" : null);

        /* row B : places / by date / insights */
        LinearLayout rowB = b.row(100);
        b.tile(rowB, 1f, v -> a.startActivity(new Intent(a, PlacesActivity.class)))
                .gradient(5).label("Places").icon(R.drawable.ic_location_on_white)
                .value(places != null && !places.isEmpty() ? String.valueOf(places.size()) : "");
        b.tile(rowB, 1f, v -> a.startActivity(new Intent(a, BrowseByDateActivity.class)))
                .gradient(3).label("By date").icon(R.drawable.ic_date_range_white);
        b.tile(rowB, 1f, v -> a.startActivity(new Intent(a, InsightsActivity.class)))
                .gradient(4).label("Insights")
                .value(snap != null ? String.valueOf(snap.photos + snap.videos) : "");

        /* row C : favourites / colours */
        LinearLayout rowC = b.row(96);
        BentoTile favs = b.tile(rowC, 1f, v -> openBucket(a, "Favorites", favItems(a)));
        favs.gradient(0).label("Favorites");
        if (snap != null) favs.countTo(snap.favorites, "");
        b.tile(rowC, 1.3f, v -> a.startActivity(new Intent(a, ColorSearchActivity.class)))
                .gradient(9).label("Search by colour").icon(0);

        return wrap;
    }

    private static Object cover(Activity a, AlbumItem it) {
        Object u = it.getUri(a);
        return u != null ? u : it.getPath();
    }

    private static List<AlbumItem> recentItems() {
        List<AlbumItem> l = new java.util.ArrayList<>();
        java.util.ArrayList<com.absolute.floral.data.models.Album> al =
                com.absolute.floral.data.provider.MediaProvider.getAlbums();
        if (al != null) for (com.absolute.floral.data.models.Album x : al)
            if (x.getAlbumItems() != null) l.addAll(x.getAlbumItems());
        java.util.Collections.sort(l, (p, q) -> Long.compare(q.getDate(), p.getDate()));
        return l.size() > 150 ? new java.util.ArrayList<>(l.subList(0, 150)) : l;
    }

    private static List<AlbumItem> favItems(Activity a) {
        java.util.Set<String> paths = FlagStore.favorites(a).all();
        List<AlbumItem> out = new java.util.ArrayList<>();
        java.util.ArrayList<com.absolute.floral.data.models.Album> al =
                com.absolute.floral.data.provider.MediaProvider.getAlbums();
        if (al != null) for (com.absolute.floral.data.models.Album x : al)
            if (x.getAlbumItems() != null)
                for (AlbumItem it : x.getAlbumItems())
                    if (it.getPath() != null && paths.contains(it.getPath())) out.add(it);
        return out;
    }

    private static void openBucket(Activity a, String title, List<AlbumItem> items) {
        BucketActivity.TITLE = title;
        BucketActivity.KICKER = "";
        BucketActivity.ITEMS = items;
        a.startActivity(new Intent(a, BucketActivity.class));
    }
}
