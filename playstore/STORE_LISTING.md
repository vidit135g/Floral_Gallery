# Floral — Google Play Store listing

**Package:** `com.absolute.floral`
**Developer:** The Absolute Corporation
**Category:** Photography
**Content rating:** Everyone
**versionCode:** 11 · **versionName:** 3.0

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

One continuously scrolling Library, laid out like the photos app you already
know:

— THE GRID —
Every photo and video in one clean, date-sectioned timeline, newest first —
Today, Yesterday, then by day and by month. Pinch to change how many sit in a
row. Keep scrolling and the grid flows into Collections.

— COLLECTIONS —
Beneath the grid: Recent Days, Memories (auto-made from your own library),
People & Pets grouped by faces found on your device, Places, Featured Photos,
Media Types (Videos, Screenshots, Animated, Recently Added), Utilities
(Duplicates, Hidden, Recently Deleted) and your device folders as My Albums.
Soft bento-gradient cards throughout.

— THE VIEWER —
Tap a photo for a full, edge-to-edge view. Everything fades away as you look;
swipe down and the photo follows your finger to dismiss, swipe up for the
info sheet — date, camera and EXIF, and a map for geotagged shots. Videos
play inline with a scrubber. The bottom bar is share, favourite, info, trash.

— PLACES & MAP —
Photos with a location are grouped by city (read from the photo and geocoded
on your device) and plotted on “Your map” — an offline photo-map you can pan
and zoom. No map tiles are ever downloaded.

— SETTINGS —
Deliberately small. Light or Dark, and the handful of switches that change how
your library is read. Nothing to sign into.

— MORE —
• A home-screen “Memories” widget that shows a different photo each day
• Favourites, Hidden folder, EXIF editor, set as wallpaper, file manager
• Works offline, always

No ads. No account. No cloud. Made with care in India.
```

## What’s new (release notes, 500 chars max)
```
Floral 3.0 — rebuilt as a Photos-style gallery:
• One continuously scrolling Library — the grid, then Collections
• A full immersive viewer: chrome fades away, swipe down to dismiss,
  swipe up for info, videos play inline
• Recent Days, Memories, People & Pets, Places, Media Types, Utilities
• Light / Dark only; a much smaller Settings; a leaner, faster app
```

<details><summary>Floral 2.3 notes</summary>

```
Floral 2.3 — a place for everything:
• "Your map" — every geotagged place on an offline photo-map you can
  pan and zoom (no tiles, no network)
• One consistent card language across the app (new Card system)
• Settings restyled as a floating bento card
• The photo viewer's action bar is now a clean floating pill
```

<details><summary>Floral 2.2 notes</summary>

```
Floral 2.2 — deeper, more capable, still 100% on-device:
• A richer bento theme — dimensional gradients, soft glow, hue-matched shadows
• Search rebuilt: People, Places, Things and a Browse grid on one page
• Things — on-device photo tagging (Food, Nature, Sky, Beaches, Animals, …)
• Apple-style info card in the viewer — date, camera/EXIF, a map for GPS shots
• Collections gains a "Media types" row: Videos, Screenshots, Animated,
  Recently added, Recently Deleted
```

<details><summary>Floral 2.1 notes</summary>

```
Floral 2.1 — best of Apple & Google Photos, on-device:
• Soft pastel bento theme across every screen + a redrawn welcome
• Apple-style Years / Months / Days on the Photos tab
• Featured Stories — full-screen, auto-advancing; tap “Save as film”
• Places (from photo GPS) and Browse by date
• Create, for real: Animation (looping GIF), Highlight film (Ken-Burns MP4),
  Collage, and Free up space (screenshots, big videos, look-alikes)
• Search icon now springs in as you scroll
```

</details>

</details>

</details>

<details><summary>Floral 2.0 notes</summary>

```
Floral 2.0 — a ground-up rebuild:
• A colourful bento layout — memories, people and library stats as bright tiles
• New Insights dashboard with an animated photos-per-year chart + your palette
• Search by colour — pick a hue, matched entirely on-device
• New Photos / Collections / Create layout with a floating nav
• One date-grouped timeline of everything, pinch to zoom the grid
• Memories carousel — auto-picked highlights from your own library
• Collections: Favourites, Archive, Trash, albums and on-device People
• Collage maker in Create, instant Search, a redrawn home-screen widget
• A new app icon and a warm, gallery-first design throughout
```

</details>

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
