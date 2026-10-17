# Agent Guide

This is the supported Android companion for <https://github.com/JasonGraber/graber-hub>.

## Source of truth

- `app/src/main/java/com/graberhub/companion/network/ApiService.kt` — server contract.
- `network/RetrofitClient.kt` — Retrofit lifecycle and `X-Data-Version` concurrency headers.
- `data/models/Models.kt` — wire models.
- `data/prefs/PrefsManager.kt` — local URLs, colors, and schedule templates.
- `viewmodel/MainViewModel.kt` — state, polling, mutation retries, and screen orchestration.
- `ui/screens/` — Compose screens.
- `app/build.gradle.kts` — app version and configurable default server URLs.

## Invariants

1. The Raspberry Pi API is the source of truth for shared family data.
2. Preserve the server's `X-Data-Version` and HTTP 409 refresh/retry behavior.
3. Keep unfinished chores above completed chores without reordering inside either group.
4. Server URLs must end in `/` for Retrofit.
5. Never commit a private IP, Tailscale address, family name, photo, signing key, or `local.properties`.
6. Use fictional data for screenshots and tests.
7. Do not add HTTPS-only assumptions; a trusted home LAN commonly uses HTTP.

## Configure local defaults

Add optional ignored values to `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
graberHub.homeUrl=http://192.168.1.50/
graberHub.tailscaleUrl=http://100.64.0.10/
```

If omitted, the build uses documentation-only example addresses. Users can always set the server in the Settings screen.

## Build and verify

```bash
./gradlew clean compileDebugKotlin lintDebug assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/graber-hub-VERSION.apk
```

For UI changes, install the debug APK on an emulator or phone, connect it to a demo Hub, and inspect every affected screen at a narrow phone width. Confirm schedule, chores, countdowns, timer, settings, connection switching, and foreground auto-refresh as applicable.

## Release discipline

1. Increment both `versionCode` and `versionName` for a distributed APK.
2. Build from a clean source tree.
3. Verify the APK package and version with `apkanalyzer` or `aapt`.
4. Commit and push before distributing the artifact.
5. Never replace or recreate the production signing key.
