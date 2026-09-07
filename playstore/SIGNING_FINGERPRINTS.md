# Floral — signing & fingerprints

## This build

- **APK/AAB:** `Floral-2.0-release.apk` / `Floral-2.0-release.aab`
- **Signed with:** `/Users/viditgupta/Documents/Project fix/release.keystore`, alias `releaseKey`
- **Store / key password:** `android123`  ← **dev password — rotate before publishing**
- **Owner:** `CN=Vidit Gupta, OU=Mobile, O=Absolute, L=San Francisco, ST=CA, C=US`
- **Valid:** 2026-09-06 → 2054-01-22 · SHA256withRSA
- **APK signature schemes:** v1 + v2 + v3 (verified with `apksigner verify`)

| Hash | This build's certificate |
|---|---|
| SHA-1   | `CB:29:61:BA:E2:35:17:3C:6D:F0:8A:2E:0B:FD:BA:F3:DA:70:D0:EE` |
| SHA-256 | `2E:B4:A2:E8:77:DC:9C:B7:1A:00:25:7F:D4:A7:44:E5:65:0D:60:7F:29:25:2D:F3:67:9C:20:5A:6E:93:56:EE` |
| MD5     | `9F:72:B8:3F:C9:4E:C7:DD:C6:5E:71:AB:B7:3E:E9:C7` |

`upload_certificate_releasekey.pem` in this folder is the PEM export of that cert.

## ⚠️ If `com.absolute.floral` already has a Play listing

This is the same situation as Weathercast: `release.keystore` was created
2026-09-06, so if the existing `com.absolute.floral` listing was set up with a
different upload key, Play will reject this AAB with a fingerprint mismatch.

**Pick one:**

1. **You still have the original upload keystore** (another machine, a backup,
   Time Machine, an old project zip, a CI secret). Verify its cert SHA-1 matches
   what Play Console shows under *Test and release → Setup → App integrity →
   App signing → Upload key certificate*, then point
   `app/build.gradle → signingConfigs.release.storeFile` at it and rebuild.

2. **The original upload key is lost.** Play Console → your app → *Test and
   release → Setup → App integrity → App signing → "Request upload key reset"*
   (form: https://support.google.com/googleplay/android-developer/answer/9842756#reset).
   Attach `upload_certificate_releasekey.pem`. Google reviews it in ~1–2
   business days; after approval this AAB uploads as-is.

3. **Brand-new listing.** Create a new app; on first upload let Google generate
   the app signing key and register this cert as the upload key.

## After the app is live

Play Console → App signing shows the **app signing key** SHA-1/SHA-256 (Google
re-signs the delivered APK). Use *that* one for any Maps/Firebase SHA allow-list.
Floral uses no network APIs — ML Kit face detection is fully on-device — so
there is nothing else to configure.

## Reproduce
```bash
keytool -list -v -keystore "/Users/viditgupta/Documents/Project fix/release.keystore" \
  -storepass android123 -alias releaseKey
apksigner verify --print-certs Floral-2.0-release.apk
```
