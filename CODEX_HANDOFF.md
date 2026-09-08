# Floral → Apple Photos replica — UI design handoff

You (Codex) are working on **Floral**, an Android gallery app being rebuilt into a
faithful replica of **Apple Photos (iOS 18 / 26)**. It's a fork of
Camera-Roll-Android-App; everything so far was built in Claude Code.

## Your scope

**UI and interaction design only.** Concretely:

- Build screens, layouts, components, motion, states, and polish so the app
  looks and feels like Apple Photos.
- Run the app on the emulator, take screenshots, and show them so progress is
  reviewable.
- Iterate on visual details: spacing, type, color, corner radii, transitions,
  empty states, dark mode.

**Not your scope — leave these to Claude Code / the user:**

- Release builds, `versionCode`/`versionName` bumps, `assembleRelease`,
  `bundleRelease`.
- Signing, keystores, fingerprints, `signingConfigs`, `playstore/`.
- Play Store listing, screenshots-for-store, AAB uploads.
- `git push`, PRs, anything that leaves the local machine.

If a task seems to need a release or signing, stop and hand it back.

## Golden rules

1. **Never `git push` or open a PR.** Work stays on local `master`.
2. **Commit per milestone.** End every commit message with
   `Co-Authored-By: <your agent id>`. Keep messages concrete (what changed + why).
3. **Verify visually before every commit** — build the *debug* APK, install,
   drive the changed screen, screenshot, look at it, check `logcat` is crash-free.
   Never ask the user to "check manually".
4. **Match the existing code.** Java for new files (the codebase is Java + a
   little Kotlin). No new heavy dependencies without flagging it.
5. **Edit the `-v21` resource copies.** `res/menu-v21/` and `res/layout-v21/`
   shadow the base `res/menu/` / `res/layout/` at `minSdk 21` — every device
   uses the `-v21` copy. Editing only the base does nothing at runtime.
6. **Go through the design system, never hardcode.** Colors and fonts come from
   `com.absolute.floral.soma.Soma` / `SomaSkin` (see §3.1). New screens that
   hardcode `#FFFFFF` or a typeface will look wrong in dark mode.

---

## 1. Repo & how to run

| | |
|---|---|
| Working copy | `…/scratchpad/Floral_Gallery` (a normal git repo; branch `master`) |
| Package | `com.absolute.floral` |
| Build | Gradle wrapper, AGP 8.4.2, Java 17, `minSdk 21`, `target/compileSdk 34` |
| Android SDK | `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools` |
| Emulator | AVD `wc34`, usually `emulator-5560`, API 34, screen 1080×2400 |

```bash
# fast compile check
./gradlew :app:compileDebugJavaWithJavac -q

# debug APK  ->  app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug -q

# install + launch  (uninstall first only if a signature clash appears)
adb -s emulator-5560 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5560 shell pm grant com.absolute.floral android.permission.READ_MEDIA_IMAGES
adb -s emulator-5560 shell pm grant com.absolute.floral android.permission.READ_MEDIA_VIDEO
adb -s emulator-5560 shell pm grant com.absolute.floral android.permission.ACCESS_MEDIA_LOCATION
adb -s emulator-5560 shell pm grant com.absolute.floral android.permission.POST_NOTIFICATIONS
adb -s emulator-5560 shell am start -n com.absolute.floral/.ui.FirstActivity
# then tap "Get started" on the onboarding screen

# screenshot
adb -s emulator-5560 exec-out screencap -p > shot.png
# adb taps/swipes use DEVICE pixels (1080x2400), not screenshot pixels
```

`SettingsActivity`, `ItemActivity`, `BucketActivity`, `MemoryActivity` etc. are
**not exported** — you can't `am start` them; reach them through the UI.
`FirstActivity` is the only launcher activity.

**Nav flow:** `ui.FirstActivity` → `SplashActivity` (onboarding, "Get started") →
`LoadingActivity` (instant) → `MainActivity` (the two-tab home).

### Debug signing note (not your job, just context)

The debug build signs itself with the standard Android debug key — nothing for
you to configure. If `adb install` ever fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, run
`adb uninstall com.absolute.floral` and reinstall. Anything touching the
*release* keystore is out of scope — hand it back to Claude Code.

---

## 2. Design system — `com.absolute.floral.soma`

**Read `Soma`; don't hardcode.**

- **`Soma.java`** — token palette. `SomaSkin.read(ctx)` returns a `Soma` for the
  current Light/Dark theme. Fields: `ground[3]` (window bg), `surface`,
  `surfaceStrong` (chips / fields), `hairline`, `ink` (primary text/icon),
  `inkSoft`, `inkMute` (captions), `accent` (`#0B57D0`), `accentSoft`, `scrim`,
  `boolean lightBase`. Helpers: `Soma.dp(ctx, v)`, `Soma.blend(a, b, t)`,
  `Soma.display(ctx)` / `Soma.body(ctx)` → the bundled `assets/fonts/google.ttf`
  (a Google-Sans-ish face; it's the global default already).
- **iOS blue** — `@color/gred` is aliased to `@color/ios_blue` `#0A84FF`; use it
  for selection, links, active tabs. **Settings switches are green `#34C759`.**
- **`SomaSkin.java`** — `ground(activity, root, soma)` paints the window;
  `wordmark(toolbar, soma, big, small)` sets the left-aligned screen title
  (`R.id.toolbar_title`); `toolbarIcons(toolbar, soma)` tints menu icons;
  `statusBarIcons(activity, soma)`.
- **`Card.java`** — `surface(v, soma, radiusDp, elevDp)`, `pastel(v, i)`,
  `header(ctx, soma, text)`, `box(ctx, soma)`, `addRow(box, soma, key, value)`.
  App-wide card radius = 22 dp.
- **`Anim.java`** — `ease()` = `PathInterpolator(0.16, 1, 0.3, 1)`;
  `enterChildren(group, startDelay, stagger)`, `enter(v, delay)`, `pulse(v)`,
  `press(v, down)`. Use these for entrances/taps so motion is consistent.
- **`PhotoNav.java`** — the floating **Library | Collections** pill (blue active
  chip), bottom-left. `SearchFab.java` — the circular Search button, bottom-right.
- **`Bento` / `BentoTile` / `BentoLayout`** — the pastel-gradient tile system for
  Collections. `BentoTile`: `.gradient(i)`, `.photo(uriOrPath, accentIndex)`,
  `.label(s)`, `.sub(s)`, `.icon(res)`, `.countTo(target, suffix)`.
- Theme system (pre-existing): `themes/{Theme,LightTheme,DarkTheme}` +
  `ThemeableActivity`. Only Light and Dark are exposed.
- Dead / unused, ignore or delete when a task naturally touches them:
  `soma/NavPill`, `soma/Segmented`, `soma/AmbientView`, `soma/SomaTint`,
  `bento/BentoHeader`, `adapter/collections/CollectionsAdapter`; parts of
  `styles/` and `adapter/main/` are still used by the system photo-picker path —
  check references first.

---

## 3. Where each screen lives

### Two-tab host — `ui/MainActivity`

`setContentView(R.layout.activity_main)` → includes `recycler_view_layout` (the
**`-v21`** copy is authoritative). `#root_view` (FrameLayout) holds:

- `#recyclerView` — the Library grid (`FastScrollerRecyclerView`).
- `#collectionsScroll` — a `NestedScrollView` hosting the Collections screen.
- `#toolbar` with `#toolbar_title`.
- Overlays added in code: `photoNav` (pill), `searchFab`, the **account avatar
  chip** (top-right, opens Settings), and — only while selecting — a top bar
  (`Cancel · N Selected · Select All`) and a bottom bar
  (`Share · Favourite · Delete · Done`).

Exactly one of `recyclerView` / `collectionsScroll` is `VISIBLE`.
`selectTab(int)` cross-fades (120 ms) and swaps the wordmark title.
`refreshPhotos()` is the data hub — one media load, then it kicks the background
indices and feeds `CollectionsScreen`.

### Library grid — `adapter/photos/PhotoGridAdapter`

`RecyclerView.Adapter`, stable ids. `setContinuous(true)` (Library) drops date
headers; Bucket/Memory/Search keep them. `Filter` enum
(`ALL/FAVORITES/EDITED/PHOTOS/VIDEOS/SCREENSHOTS`) + `setFilter`. Selection:
`enterSelection()`, `dragSelectRange(start, end, sel)`, `setDragStarter`,
`selectedPaths()`. Tap → `ItemActivity` with a shared-element transition.

The `…` menu: `MainActivity.showLibraryMenu()` inflates
`R.menu.library_more_popup` (Select, Sort ✓, Filter ▸, View Options ▸).

### Collections — `bento/CollectionsScreen`

`build(Activity, Providers) → Holder`; `Holder.view()` is a `NestedScrollView`,
`Holder.refresh(Providers)` rebuilds when data changes. Each section via
`section(soma, prefs, id, title, collapsible, onOpen, action, BodyBuilder)` —
header (title + optional `>` chevron + circular `⌄` collapse toggle) + a lazily
built body. Collapse state persists in `SharedPreferences("collections_ui")`,
height-animated.

Section order: **Memories · Pinned · Albums · Recent Days · People & Pets ·
Trips · Featured Photos · Media Types · Utilities · Wallpaper Suggestions** +
a blue **Reorder** link (stub). A section only renders when it has content
(Apple does the same). Media Types / Utilities use `things/MediaRows.pillRows`
(2-column pill grid).

### The viewer — `ui/ItemActivity` (built, treat as done unless asked)

Immersive: chrome fades together, tap toggles, swipe down dismisses (photo
follows the finger), swipe up → info sheet, inline video with a scrubber, bottom
bar `share · favourite · info · trash · edit`.
**Do not regress the Android-14 transition fix** — there are deliberate timeout
nets in `dismissToGrid()` / `setResultAndFinish()` / `onCreate` that stop the
viewer freezing "paused for transition". Keep them.

### Settings — `ui/SettingsActivity`

A hand-built `NestedScrollView` grouped table: big title + circular ✕, profile
card ("N Photos, M Videos"), rounded section cards, green `#34C759` switches, blue
Reset. Only rows that do something real (no iCloud / HDR / Shared-Library rows).

### People naming — `people/PeopleNames` + `PeopleIndex`

Face clusters get a fresh id each rescan, so names are keyed by the cluster's
embedding centroid (cosine ≥ 0.86 re-match). `CollectionsScreen.renamePerson`
shows an `AlertDialog`. Face tile shows the name or a blue "Add Name".

### Data / background indices — `com.absolute.floral.data`, `people/`, `places/`, `things/`

Read-only from your side, but useful to know what feeds the UI:
`PhotoTimeline`, `Memories`, `Periods`, `FlagStore` (favourites),
`RecentStore` (recently viewed/shared), `Settings` (framework
`PreferenceManager` — persisting setters are the `Context` overloads),
`PeopleIndex` (ML Kit faces), `PlacesIndex` (EXIF GPS + geocoder —
returns bare coords on the emulator), `LibrarySnapshot` (counts),
`MediaTypeIndex` (path/name/MIME buckets).

---

## 4. Apple Photos parity — what to design

Legend: ✅ built · 🟡 partial / needs polish · ⛔ not feasible on Android · ⬜ TODO

### Library tab
| Apple | status | your work |
|---|---|---|
| Continuous grid, opens scrolled to the newest photo | 🟡 | grid is continuous; add "start at bottom" + a smooth first-paint |
| Pinch zoom: years → months → all → single | 🟡 | only column count 2–5 today; design the 4-level zoom |
| `…` menu: Select, Sort, Filter, View Options | ✅ | Sort/Filter menus exist; "Aspect Ratio Grid" is a visual no-op — make it real |
| Select mode: tap, drag-select, Select All, top+bottom bars | ✅ | polish the bar styling, animate them in/out |
| Date-range subtitle under the title while scrolling | ⬜ | needs a two-line title; guard `SomaSkin.wordmark`'s `Toolbar.LayoutParams` cast so it only runs when the title's parent is the toolbar |
| Days / Months / Years segmented views | ⬜ | `Periods` helper already has the data |
| Fast scrubber with a floating month bubble | 🟡 | scrubber present, no bubble |

### Collections tab
| Apple | status | your work |
|---|---|---|
| Collapsible titled sections, remember state | ✅ | |
| Section reordering (the "Reorder" link) | 🟡 | design a drag-reorder; persist order in `SharedPreferences("collections_ui")` under a `__order` key |
| Memories — full-screen playback | 🟡 | `MemoryActivity` plays a slideshow; design name-card first frame, auto-advance, Ken-Burns |
| Pinned Collections (user-editable) | 🟡 | fixed set now; wire the "Edit" pill |
| Albums (+ user albums, folders, shared) | 🟡 | device folders only |
| People & Pets — name / merge / "not this person" / favourite / pets | 🟡 | naming ✅; the rest ⬜; design a dedicated People screen |
| Trips — auto, map + place title | 🟡 | clustered; titles are coords on the emulator |
| Recent Days · Featured Photos | ✅ | |
| Media Types — Videos, Selfies, Live, Portrait, Panoramas, Time-lapse, Slo-mo, Bursts, Screenshots, Screen Recordings, Animated, RAW, Cinematic, Spatial, QR, Documents, Handwriting, Illustrations | 🟡 | 10 detected; QR/Docs/Handwriting/Illustrations need on-device ML Kit; Portrait/Cinematic/Depth/Spatial/Slo-mo ⛔ |
| Utilities — Imports, Duplicates, Hidden, Recently Deleted, Recently Viewed, Recently Shared | ✅ | |
| Map | ✅ | offline `PhotoMapActivity` |
| Wallpaper Suggestions | 🟡 | header-only |

### Viewer
Immersive fade ✅ · swipe-down dismiss ✅ · info sheet ✅ · inline video ✅ ·
bottom bar ✅ · Adjust/Filters/Crop editor 🟡 (existing `EditImageActivity`, not
Apple-styled) · Live Photo press-and-hold ⛔/🟡 · Portrait/Cinematic depth ⛔.

### Chrome
Library|Collections pill + circular Search ✅ · account avatar top-right ✅ ·
app icon ✅ (photo-stack mark on a teal→violet gradient — an Apple-*style* icon,
deliberately not a copy of Apple's pinwheel; the user asked for "not a flower").

---

## 5. Backlog (priority order) — each is a UI task

1. **Library opens at the newest photo + a date-range subtitle.** Files:
   `ui/MainActivity.setupLibrary`, `recycler_view_layout-v21.xml`,
   `soma/SomaSkin.wordmark`. Accept: grid starts at the bottom; the title's
   second line reads e.g. "1 Jan – 8 Sep" and updates on scroll-idle.

2. **"Aspect Ratio Grid" actually changes the grid** (square ↔ aspect-fit
   tiles). `PhotoGridAdapter`, `photos_grid_item.xml`.

3. **People screen.** New `people/PeopleActivity` — grid of named + unnamed
   people like Apple's. Design merge / "not this person" / favourite / pet
   affordances (the storage side can be stubbed and handed back).

4. **Section reordering** for Collections (backlog above).

5. **Days / Months / Years segmented Library.** `Periods.months` / `.years`
   already produce the data; design the segmented control + the hero-per-period
   layout.

6. **Memories playback polish** — full-bleed cards, gradient name-card first
   frame, auto-advance, optional muted looping cover.

7. **Media Types rows via on-device ML Kit** (QR / Documents / Handwriting /
   Illustrations) — design the "still preparing" empty state and the opt-in flow;
   Claude can wire the GMS dependency.

8. **Apple-style editor** — restyle `EditImageActivity` to Apple's
   Adjust / Filters / Crop tabbed layout. Lower priority.

9. **Dark-mode + empty-state sweep** across every screen once the above land.

---

## 6. Verification loop (every commit)

1. `./gradlew :app:assembleDebug -q`
2. `adb install -r …`, grant the 4 permissions, `am start …FirstActivity`, tap
   "Get started".
3. Drive the changed screen with `adb shell input tap/swipe/text`, screenshot
   with `adb exec-out screencap -p`, and **look at the screenshot**. Compare
   against a real Apple Photos screen.
4. `adb logcat -d | grep -iE "FATAL|AndroidRuntime|Exception"` — must be clean.
5. Commit with a concrete message + the `Co-Authored-By` trailer.
6. Post the before/after screenshots for review.

No release builds. No signing. No `playstore/`. No push.

---

## 7. Gotchas

- **`-v21` shadowing** — golden rule 5.
- **Jetifier** — `com.github.MFlisar:DragSelectRecyclerView:0.3` is an old
  `android.support` AAR that works because AGP rewrites it to AndroidX at build
  time. Don't be thrown by `android.support` in its decompiled API.
- **`Settings` persistence** — plain setters don't write SharedPreferences; use
  the `Context` overloads (e.g. `setCameraShortcut(Context, boolean)`).
- **`SettingsActivity` result across theme change** — a theme toggle calls
  `recreate()`, which drops a pending `setResult`; it's carried via the `static
  SettingsActivity.sChanged` flag that `MainActivity` resets before launching.
  Don't remove it.
- **Virtual albums** — `MediaProvider.getAlbumsWithVirtualDirectories` injects
  auto folders ("People", "Trips", "Camera"…); Collections ▸ Albums filters ones
  whose name collides with a section.
- **Emulator geocoder** returns coordinates, not city names — Places / Trips
  titles look worse here than on a real device; don't "fix" that in code.
- **Grid→viewer shared-element transition** is fragile on API 34 — the timeout
  nets in `ItemActivity` are load-bearing; keep them.
- **Screenshot vs device coords** — `adb input tap` uses device pixels
  (1080×2400), not the pixels of your screenshot image.

---

## 8. Out of scope for this project

Portrait / Cinematic / Depth / Spatial / Slo-mo detection (no Android metadata) ·
iCloud / Shared-Library / HDR settings rows · Live Photo press-and-hold ·
**release builds, signing, the Play Store, and `git push`** — all handled by
Claude Code / the user.

---

## 9. Commit history since the fork point (`d812d9d`)

```
029f672 Add CODEX_HANDOFF.md
d05b6f0 Collections depth + Apple-style selection + people naming
55315a2 Stage E5: release 3.1 (versionCode 12) + Play package
f86cdbf Stage E4: MediaTypeIndex + RecentStore + full Media Types / Utilities rows
40985ca Stage E3: hand-built Apple grouped Settings, drop PreferenceFragment
d24871b New app icon: photo-stack mark (drop the flower)
1259259 Stage E2: Library chrome (… menu, Select action bar)
1bd517b Stage E1: two-tab shell (Library + Collections) + CollectionsScreen
16610c7 Stage D wrap: release 3.0
922498a Fix: viewer could freeze on dismiss ("paused for transition")
edffbd0 Stage D: viewer polish + splash trim
5a9b7e1 Stage C: full Apple photo viewer
2bb6a9f Stage B: iOS 18 single-scroll Library
fae1b6c Stage A: strip to Apple-minimal feature set, Light/Dark, iOS-blue accent
… (earlier "Google Photos" / initial "Soma" phases — superseded)
```

Design notes for Stages A–E are in
`/Users/viditgupta/.claude/plans/crispy-waddling-lighthouse.md`.
