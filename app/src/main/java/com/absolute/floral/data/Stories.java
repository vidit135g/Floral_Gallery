package com.absolute.floral.data;

import com.absolute.floral.data.models.AlbumItem;
import com.absolute.floral.people.PeopleIndex;
import com.absolute.floral.places.PlacesIndex;

import java.util.ArrayList;
import java.util.List;

/** Assembles the featured-stories tray from memories, people and places. */
public final class Stories {

    private Stories() {}

    public static List<Story> build(List<Memories.Memory> memories,
                                    List<PeopleIndex.Person> people,
                                    List<PlacesIndex.Place> places) {
        List<Story> out = new ArrayList<>();
        int g = 0;
        if (memories != null) {
            for (Memories.Memory m : memories) {
                if (m.items.size() < 2) continue;
                out.add(new Story(m.kicker, m.title, m.items, g++ % 10));
            }
        }
        if (people != null) {
            int i = 1;
            for (PeopleIndex.Person p : people) {
                if (p.photos.size() < 2) { i++; continue; }
                out.add(new Story("PEOPLE", "Person " + (i++), new ArrayList<>(p.photos), g++ % 10));
            }
        }
        if (places != null) {
            for (PlacesIndex.Place pl : places) {
                if (pl.items.size() < 2) continue;
                out.add(new Story("PLACE", pl.label, new ArrayList<>(pl.items), g++ % 10));
            }
        }
        return out;
    }
}
