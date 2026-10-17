# Android Setup and Development

## Prerequisites

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.x
- Android Studio or the command-line SDK tools
- A running [Graber Hub server](https://github.com/JasonGraber/graber-hub)

## Clone and build

```bash
git clone https://github.com/JasonGraber/graber-hub-app.git
cd graber-hub-app
```

Create `local.properties` with your SDK path and optional server defaults:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
graberHub.homeUrl=http://192.168.1.50/
graberHub.tailscaleUrl=http://100.64.0.10/
```

Both URLs must include a scheme and trailing slash. The Tailscale value is optional; use any private-VPN address reachable by the phone.

```bash
./gradlew assembleDebug
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/graber-hub-1.4.9.apk
```

## First connection

1. Start the app and open **Settings**.
2. Under **Server Connection**, enter `http://PI_ADDRESS/`.
3. Tap **Save & Connect**.
4. Confirm the status changes to **Connected**.
5. Open Schedule and Chores and verify the same data appears on the wall display.

Android and the Pi must be on the same non-guest Wi-Fi, or both must be signed into the same Tailscale network.

## Demo server for screenshots and development

In a separate checkout of the server repository:

```bash
./scripts/dev-demo.sh
```

An Android emulator reaches the host at `http://10.0.2.2:8080/`. A physical phone needs the host computer's LAN address and a firewall rule permitting port 8080.

## Verification

```bash
./gradlew clean compileDebugKotlin lintDebug assembleDebug
```

Manual regression checklist:

- schedule list and schedule-template apply;
- one-time and recurring chore creation;
- chore completion and unfinished-first ordering;
- timer start/stop;
- countdown add/delete;
- verse create/select/delete;
- display-mode switch;
- photo upload/rename/delete and source switch;
- Home/Tailscale/custom URL connection behavior;
- refresh after returning from the background.

## Architecture notes

The app uses one activity and Compose Navigation. `MainViewModel` owns shared screen state and initializes `RetrofitClient` from `PrefsManager`. Mutations attach the last server `X-Data-Version`; HTTP 409 means another client changed the flat-file store and the app must refresh before retrying.

Schedule templates are currently stored on the phone. Shared schedules, chores, countdowns, verses, display state, and photos live on the Hub.
