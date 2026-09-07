package com.absolute.floral.data;

import com.absolute.floral.data.models.AlbumItem;

import java.util.List;

/** A tappable, auto-advancing "featured story" — the ring in the tray + the full-screen player. */
public class Story {
    public final String kicker;      // RECENT / ON THIS DAY / HIGHLIGHTS / <NAME> / <PLACE>
    public final String title;
    public final List<AlbumItem> items;
    public final int gradient;       // Bento.GRADIENTS index for the ring

    public Story(String kicker, String title, List<AlbumItem> items, int gradient) {
        this.kicker = kicker;
        this.title = title;
        this.items = items;
        this.gradient = gradient;
    }

    public AlbumItem cover() { return items == null || items.isEmpty() ? null : items.get(0); }
}
