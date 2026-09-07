# Floral — Google Play Store listing

**Package:** `com.absolute.floral`
**Developer:** The Absolute Corporation
**Category:** Photography
**Content rating:** Everyone
**versionCode:** 6 · **versionName:** 2.0

---

## App name (30 chars max)
```
Floral — Gallery
```

## Short description (80 chars max)
```
A private, on-device gallery — timeline, memories, albums and people. No cloud.
```

## Full description (4000 chars max)
```
Floral is a photo gallery that keeps everything on your device. No account, no
sign-in, nothing uploaded — your library never leaves your phone.

It’s laid out the way you expect a modern gallery to be:

— PHOTOS —
One clean, scrollable timeline of every photo and video, newest first. Recent
days are grouped as Today, Yesterday and by date; older stretches condense to
the month so long periods fill the screen instead of trickling by. Pinch to
change how many photos sit in a row. Tap any picture for the full-screen
viewer, with a star to favourite, share, edit and archive one tap away.

— MEMORIES —
A carousel across the top surfaces moments automatically: “The last few weeks”,
“Around this time” from previous years, and a “Best of <month>” for months you
shot a lot. Open one to see just those photos.

— COLLECTIONS —
Favourites, Archive and Trash pinned at the top. Your device folders show up as
albums with a cover and a count. A People & pets row groups faces found
entirely on your device — no names, no recognition service, nothing sent
anywhere.

— CREATE —
Combine two to nine photos into a single collage — four layouts, five
backgrounds — and save it straight to your gallery.

— SEARCH —
Type to find photos by file name, folder, month, or just “videos” / “photos”.
Results appear instantly, grouped by date.

— MORE —
• A warm, quiet design that lets your photographs be the loudest thing on screen
• Light, dark and true-black (AMOLED) themes
• A home-screen “Memories” widget that shows a different photo each day
• Fast HD viewing, EXIF details, hidden folders, file manager, set as wallpaper
• Works offline, always

No ads. No account. No cloud. Made with care in India.
```

## What’s new (release notes, 500 chars max)
```
Floral 2.0 — a ground-up rebuild:
• New Photos / Collections / Create layout with a floating nav
• One date-grouped timeline of everything, pinch to zoom the grid
• Memories carousel — auto-picked highlights from your own library
• Collections: Favourites, Archive, Trash, albums and on-device People
• Collage maker in Create, instant Search, a redrawn home-screen widget
• A new app icon and a warm, gallery-first design throughout
```

## Graphic assets (in this folder)
| Asset | File | Spec |
|---|---|---|
| App icon (hi-res) | `icon-512.png` | 512×512 PNG |
| Feature graphic | `feature-graphic.png` | 1024×500 PNG |
| Phone screenshots | `screenshots/*.png` | 1080×2400 PNG (min 2, max 8) |
| Adaptive launcher icon | shipped in the APK/AAB | `@mipmap/ic_launcher` (gradient bg + pinwheel bloom + monochrome) |

## Data safety (suggested answers)
- **No data collected and no data shared.** Photos, videos, faces and all
  derived data stay on the device.
- Permissions: read media images/video (to show your gallery), optional
  camera-launch shortcut, set-wallpaper.
- Face grouping runs on-device (ML Kit face detection, no identity); results
  are cached locally only.
