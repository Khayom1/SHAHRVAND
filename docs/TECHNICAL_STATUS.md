# SHAHRVAND — Technical Status

**Snapshot date:** 2026-11-24  
**Repository:** [Khayom1/SHAHRVAND](https://github.com/Khayom1/SHAHRVAND)  
**Implementation branch inspected:** `uiux/v1-prototype`  
**Implementation commit inspected:** `108c0c2ea45eeb7f51dbab5b7ff44cbe5ef091f3`

This is a source-based status record. It distinguishes code that exists from behaviour validated by CI and from behaviour that still requires device or deployment verification.

## 1. Repository state

The repository has two branches:

| Branch | Role | State at inspection |
|---|---|---|
| `main` | Default branch | Minimal repository scaffold. Contains `LICENSE`, an 11-byte `README.md`, and two Pages workflows. It does not contain the Android module, browser app, admin mock, or prototype documentation. |
| `uiux/v1-prototype` | Current implementation/prototype | Contains the Android app, browser UI, static admin mock, UI iteration notes, and branch-specific CI workflows. Not merged into `main`. |

The default branch was at commit `c2256cc4ef5c034617caf73bab5d23cafd9bd309`. The implementation branch's product-code snapshot was commit `108c0c2ea45eeb7f51dbab5b7ff44cbe5ef091f3`. Documentation-only commits were then added to the same branch; the current branch head therefore also contains the documentation files.

**Consequences:** GitHub's default branch currently does not contain the product implementation described below. Changes made only on `uiux/v1-prototype` do not automatically become part of `main`.

## 2. Implemented source components

| Component | Source | What is actually present |
|---|---|---|
| Native Android application | [`app/`](../app/) | One Jetpack Compose application module. Main UI and most application logic are in `MainActivity.kt`. |
| GPS tracking | [`LocationTrackingService.kt`](../app/src/main/java/tj/shahrvand/app/LocationTrackingService.kt) | Android foreground service using the platform GPS provider and a local speed-based movement heuristic. |
| Browser prototype | [`web/index.html`](../web/index.html) | Single-page HTML/CSS/JavaScript frontend. No frontend build system or API client is present. |
| Admin/review mock | [`admin/index.html`](../admin/index.html) | Standalone review-screen mock with a hard-coded sample queue. It is not connected to the app or a server. |
| CI workflows | [`.github/workflows/`](../.github/workflows/) | Android debug build and browser-preview workflows on this branch. |

The Android implementation is concentrated in two Kotlin source files:
- `MainActivity.kt`: registration, navigation, mission definitions, local progress, ranking presentation, profile, badges, evidence-file picker, and GPS mission screen.
- `LocationTrackingService.kt`: location updates, filtering, distance accumulation, speed classification, foreground notification, and automatic GPS mission completion.

No backend service, database schema, API server, authentication provider, or remote file-storage integration is present in the inspected tree.

## 3. Android application configuration

Values read from `app/build.gradle.kts` and the Android manifest:

| Setting | Configured value |
|---|---|
| Application ID / namespace | `tj.shahrvand.app` |
| Version | `1.2.0-test` (`versionCode 3`) |
| Minimum Android SDK | 26 |
| Compile SDK | 36 |
| Target SDK | 36 |
| Java bytecode target | 17 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin Android plugin | 2.2.21 |
| Gradle used by CI | 8.13 |

The UI is built with Jetpack Compose and Material 3. Declared dependencies include Compose BOM `2025.10.01`, Activity Compose `1.11.0`, Compose UI, Material 3, extended Material icons, and Coil Compose `2.7.0`.

The manifest declares:
- `ACCESS_FINE_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION`

The location service is declared as a non-exported location foreground service. The manifest does not declare the Android `INTERNET` permission. The profile and evidence pickers use Android's document picker; selected proof files are not uploaded to a server.

## 4. Android behaviour present in source

### 4.1 User interface and profile

The app contains four main sections: Home, Missions, Ranking, and Profile. The interface strings in the inspected Kotlin source cover Tajik, Russian, and English (`tg`, `ru`, `en`).

Registration collects first name, surname, age, and an optional image URI. Names must be non-empty; age is restricted to 7–120. The profile image is selected through `OpenDocument`.

### 4.2 Built-in missions and configured XP

The native app defines five built-in missions:

| Mission | GPS-based | Normal XP configured | Test XP configured |
|---|---:|---:|---:|
| Run 2 km | Yes | 40 | 8 |
| Walk 5 km in a day | Yes | 35 | 8 |
| Street cleanup | No | 80 | 6 |
| Plant a tree | No | 100 | 7 |
| Read for 30 minutes | No | 30 | 5 |

The XP figures above are values in the current source. They are not evidence of a live rewards service. In particular, normal-mode evidence missions enter a local pending state; there is no connected server-side reviewer that can approve them and award production XP.

The Missions screen also allows a user to add a personal activity. At present, these activities live only in Compose memory and are not persisted. The completion callback looks up mission XP in the fixed built-in mission list, so custom activities do not yet have a complete, reliable persistence/reward path.

### 4.3 GPS tracking

The app starts GPS tracking only after the user starts a GPS mission and grants precise-location permission. The service:
- Requests updates from `LocationManager.GPS_PROVIDER` every 2,500 ms (with a 0 m minimum-distance parameter).
- Rejects fixes with accuracy worse than 28 m.
- Rejects elapsed intervals at or below 0.4 s or above 20 s.
- Rejects segment distances below 0.8 m or above 35 m.
- Rejects calculated speeds below 0.45 m/s or above 6.2 m/s.
- Averages the latest five accepted speed samples.
- Classifies a segment as running at a smoothed speed of at least 1.72 m/s; otherwise it classifies the accepted segment as walking.
- Accumulates walking and running distance separately in app-private preferences.
- Runs as a location foreground service and displays an ongoing notification.

Configured targets:

| Mission | Test mode (default on fresh install) | Normal mode |
|---|---:|---:|
| Run | 120 m | 2,000 m |
| Daily walk | 200 m | 5,000 m |

Daily distance counters are keyed to the device's local calendar date. The implementation is a transparent GPS heuristic, not a trained AI classifier. It can misclassify slow running or brisk walking and has not been validated across devices, routes, GPS conditions, or battery profiles.

### 4.4 Badges and ranking

The native app defines First Step, City Runner, Healthy Steps, Community Helper, Nature Guardian, Knowledge Seeker, and Active Citizen.

The native ranking screen contains four hard-coded sample participants plus the locally registered user: five rows in total. Selecting a row opens a profile dialog populated from the local sample data. This is a demonstration ranking, not a shared or live leaderboard.

There is a logic inconsistency to resolve: badge/profile evaluation uses a threshold of two different missions in test mode and five in normal mode, while the ranking screen derives the Active Citizen title using a threshold of two without checking the current mode.

## 5. Browser prototype and admin mock

### Browser frontend

`web/index.html` is a standalone single-page frontend with inline CSS and JavaScript. It provides Tajik, Russian, and English UI strings, profile registration/editing, mission screens, ranking periods, badges, and local settings.

Browser state is stored in `localStorage` under the key `shahrvand.ui.v2`. The initial state includes sample XP and sample ranking data. The ranking fixture contains 30 hard-coded names and four hard-coded period summaries. It is not connected to the Android app or a remote ranking service.

The browser mission list in this file contains four missions (run, cleanup, tree planting, and reading). It is not identical to the five-mission native Android list. Selecting a proof file updates local browser state; the file is not transmitted to a server. The optional browser avatar is converted to a data URL and the code limits the selected image to 1 MiB.

### Admin/review mock

`admin/index.html` contains three hard-coded pending sample submissions. Approve/reject actions change the JavaScript data objects in that page only. The page itself states that no server is connected. It has no authentication, role enforcement, database, or connection to either client. The current browser-preview workflow uploads only the `web/` directory, so `admin/index.html` is not included in that workflow's Pages artifact.

## 6. Data storage and trust boundaries

| Data / behaviour | Current storage | Shared with other users or services? |
|---|---|---|
| Android profile, language, avatar URI, XP, mission IDs, badges, GPS counters | App-private `SharedPreferences` named `shahrvand_local` | No |
| Android selected proof file | Selected locally through document picker; URI is held by the current screen state | No server upload |
| Browser profile, submitted mission IDs, personal activities, language, avatar data URL | Browser `localStorage` (`shahrvand.ui.v2`) | No |
| Admin queue and approve/reject state | JavaScript objects in the admin page | No; page reload resets decisions |
| Ranking data | Hard-coded sample data in client source | No live source |

There is no account system, backend authorization, cross-device synchronization, server-side validation, remote evidence storage, or authoritative review process in this version. Local UI state must not be treated as verified civic activity or authoritative reward data.

## 7. Automated validation evidence

The following GitHub Actions results were observed:

| Workflow | Run | Commit / branch | Result | What it proves |
|---|---|---|---|---|
| Android UI build | [#38](https://github.com/Khayom1/SHAHRVAND/actions/runs/45174529186) | `108c0c2` / `uiux/v1-prototype` | Success | `gradle :app:assembleDebug` completed and the debug APK artifact was uploaded. |
| SHAHRVAND browser preview | [#10](https://github.com/Khayom1/SHAHRVAND/actions/runs/45139722485) | `1b5b358` / `uiux/v1-prototype` | Success | The workflow's JavaScript syntax-check and Pages deployment steps completed on that run. |
| Jekyll Pages deployment | [#2](https://github.com/Khayom1/SHAHRVAND/actions/runs/45138421459) | `c2256cc` / `main` | Success | The main-branch Jekyll workflow completed. |
| Static Pages deployment | [#1](https://github.com/Khayom1/SHAHRVAND/actions/runs/45138421430) | `c2256cc` / `main` | Failure | Artifact upload completed, but the deploy step reported that no `github-pages` artifact could be found. |

**Validation limits:**
- The Android workflow builds a debug APK; it does not run unit tests, instrumentation tests, GPS device tests, or a full UI test suite.
- No Android test source sets were present in the inspected branch tree.
- The browser workflow checks JavaScript syntax and deploys static files; it does not perform browser automation or end-to-end tests.
- CI success does not prove GPS accuracy, background tracking reliability, battery behaviour, UI accessibility, production security, or real-world mission verification.
- The latest successful browser-preview deployment observed was run #10; it was not a deployment run for the current branch-head commit.

## 8. Known limitations before production use

1. There is no backend, login/authentication, database, remote API, or cross-device sync.
2. Proof images/videos are selected locally and are not uploaded or reviewed by a real administrator.
3. The admin interface is a disconnected mock with sample data and no access control.
4. Leaderboards and several profile figures are hard-coded demo values.
5. Test mode is enabled by default; local XP and completion state are not authoritative.
6. GPS movement classification is heuristic and needs physical-device validation.
7. Custom missions are in-memory only and do not have a complete reward/persistence path.
8. The Active Citizen threshold is inconsistent between profile evaluation and ranking presentation.
9. Multiple GitHub Pages workflows exist across the two branches; see [Build and deployment](BUILD_AND_DEPLOYMENT.md).
10. The repository's default branch does not currently contain the prototype implementation.

## 9. Scope and change policy

This document records the observed implementation; it does not claim that planned features are complete. A feature should be marked as production-ready only after its implementation, server trust boundary, failure handling, security controls, tests, and operational behaviour have been verified.

The prototype branch remains separate from `main`. No merge or backend integration is implied by this documentation update.
