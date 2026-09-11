# Seal YouTube (prototype)

A minimal Android app for DiLink-style head units: search YouTube via the
official Data API, play results through the official embedded IFrame
Player API (WebView). No reverse-engineered/unofficial YouTube client code.

## What's here
- `app/src/main/java/.../MainActivity.kt` — search box, results list, WebView player
- `app/src/main/assets/youtube_player.html` — hosts the YouTube IFrame Player API
- `app/src/main/res/values/strings.xml` — put your API key here

## Setup
1. Open this folder in Android Studio (it will generate the Gradle wrapper
   for you on first sync — or run `gradle wrapper` if you have Gradle installed).
2. Get a YouTube Data API v3 key:
   - console.cloud.google.com → new project → enable "YouTube Data API v3"
     → Credentials → Create API key.
   - Restrict the key to that API (and to your app's package name /
     signing certificate once you have a release keystore).
3. Paste the key into `app/src/main/res/values/strings.xml`
   (`youtube_api_key`).
4. Build → Run on a connected device, or `Build > Build Bundle(s)/APK(s) > Build APK(s)`
   to get a `.apk` you can sideload.

## Getting it onto the Seal 5's head unit
- DiLink is Android-based; the usual path is USB with a file manager app,
  or ADB (`adb install app-debug.apk`) if you can get developer options /
  USB debugging enabled on that particular DiLink build — this varies by
  software version and isn't guaranteed on every unit.
- No Play Store on the China build, so updates are manual — you'll push new
  APKs yourself as you iterate.

## Network
- YouTube and the Data API are blocked on mainland Chinese networks.
  You mentioned routing over wifi — connect the head unit to a hotspot
  (phone or MiFi) that has a VPN active, so both the search API calls and
  the embedded player can actually reach Google's servers.
- If the hotspot's VPN drops, search calls will fail and the embedded
  player will just show a loading/error state — worth adding a visible
  "no connection" indicator in a later pass.

## Known rough edges (prototype, not production)
- No caching / no offline handling of search failures beyond a Toast.
- No thumbnails in the results list yet (title only) — `snippet.thumbnails`
  is already in the API response if you want to add them.
- No persistent watch history / queue.
- Landscape-only, sized for a wide head-unit screen; not tested against
  the Seal's rotating-screen portrait mode.
- Driving-safety note: keep interaction minimal/voice-driven if this will
  be used while the car is moving — a scrolling search list is a bad thing
  to be tapping through at speed.

## Legal note
This uses YouTube's official Data API and IFrame Player — both intended
for exactly this kind of use (building your own front-end for personal use).
Keep your API key restricted and don't exceed the free quota tier without
setting up billing, or search calls will start failing.
