> **ARCHIVED ITERATION NOTES — NOT THE CURRENT TECHNICAL SOURCE OF TRUTH.**
>
> This file preserves notes from an earlier UI iteration. Some values and implementation details below were later changed. For the verified repository state, current XP values, branch layout, validation evidence, and known limitations, use [Technical Status](TECHNICAL_STATUS.md), [Architecture](ARCHITECTURE.md), and [Build and Deployment](BUILD_AND_DEPLOYMENT.md). In particular, the current native GPS test reward values and current CI status are documented in those files.

# SHAHRVAND UI prototype — iteration 2

## Test modes
- One-time Pages setup: in repository Settings → Pages → Build and deployment → Source, select GitHub Actions. The workflow then publishes the browser preview; after successful deployment the expected address is https://khayom1.github.io/SHAHRVAND/.
- Browser preview: web/index.html is a standalone frontend. Open it in a browser; no APK installation is required for registration, navigation, missions UI, ranking periods, profile editing, badge explanations, and local settings.
- Android APK: native Jetpack Compose build. Install it only when checking Android-specific behaviour such as the document picker and device keyboard.
- No backend: Supabase, authentication, remote uploads, live XP, and administrator verification remain disconnected.

## Changes in this iteration
- Registration places first name and surname on one row.
- Age accepts digits only and validates a range of 7–120.
- Android IME actions move from first name to surname to age; the final action hides the keyboard.
- Browser registration uses Enter/Next navigation and a sticky submit area, with an optional profile image.
- Browser state is persisted in localStorage under shahrvand.ui.v2.
- Android profile fields, language, selected avatar URI, registration state, and submitted mission IDs are persisted with app-private SharedPreferences.
- Ranking separates lifetime XP from period XP. Each of Today, Week, Month, and Year shows its own sample period points and sample rank; the pinned personal row uses the same selected period.
- Profile has an accessible settings control, profile editing, language controls, local test reset, earned and locked titles/badges, and a tap-to-open explanation of each badge's earning criteria.
- Browser demo profile supports editing and local persistence after refresh.

## Important demo-data limitations
- Ranking names, points, and positions are sample fixtures. They are internally consistent for each selected period but are not real standings.
- The lifetime 2,450 XP, completed mission count, and one earned starter badge are demo values.
- Browser mission evidence is selected locally and not uploaded. The native APK uses Android's document picker and stores the selected profile photo URI in app-private preferences. Mission submissions are local demo state only.
- The browser's optional avatar is held as a local data URL; very large images should not be used.
- Android persistence is app-private SharedPreferences, not browser localStorage.
- Clearing browser storage or resetting the test profile removes local demo data. Uninstalling the Android APK removes its private data.

## How to test
1. Enable GitHub Actions as the Pages source once in repository Settings → Pages. After the browser preview workflow succeeds, open https://khayom1.github.io/SHAHRVAND/. The first visit starts at registration.
2. Enter first name and surname, then age 7 or above. Refresh the page: profile and language should remain.
3. Change tabs, open a mission, select a local evidence file, and submit. Refresh: pending mission state should remain.
4. Change all four ranking periods. Confirm period XP/rank update, while lifetime XP remains a separate figure.
5. Open Profile → Settings, edit details, change language, and save. Tap each earned/locked badge for its criteria.
6. Install APK only for Android-specific checks: keyboard focus, document picker, profile URI persistence, and screen layout.

## Scope guard
This branch is still frontend-only. Do not connect Supabase or merge into main until UI review is approved.

## Android GPS and achievement test iteration (1.1.0-test)

### GPS missions
- Run 2 km: GPS-only. No photo or video evidence is requested. The normal target is 2,000 m of distance classified as running.
- Walk 5 km per day: GPS-only. The normal target is 5,000 m classified as walking. Daily distance accumulates across multiple tracking sessions and resets on the local calendar date.
- The user explicitly starts tracking while the app is visible. A location foreground service keeps recording after the screen is locked or the app is backgrounded, and shows an ongoing notification. The user can stop and restart; daily distance remains accumulated.
- GPS points with reported accuracy worse than 28 m, implausible jumps, and implausible speeds are filtered out.

### Movement classification
- This iteration is a transparent on-device GPS heuristic, not a trained AI model.
- Segment speed is smoothed over the latest five GPS samples. A smoothed speed of 1.72 m/s or higher is classified as running; a lower accepted movement speed is classified as walking.
- This estimate can misclassify slow running or very brisk walking. GPS conditions, accuracy, signal obstruction, and route geometry affect the result. A future sensor-fusion or trained classifier would need validation data before it could be described as AI.

### Test mode
- Test mode is enabled by default on a fresh install and can be disabled in Profile → Settings.
- Test targets: 120 m of running and 200 m of accumulated walking in the day.
- Test XP: running 3, walking 3, cleanup 2, tree planting 3, reading 1.
- With test mode enabled, selecting a photo/video and submitting a proof mission grants test XP immediately. Evidence stays local on the device; it is not uploaded to a server.
- Production XP values are running 40, walking 35, cleanup 80, tree planting 100, reading 30. In production mode, photo/video submissions remain pending review because no server-side administrator review is connected.

### Titles and animated awards
- First Step: first completed mission.
- City Runner: run GPS mission complete.
- Healthy Steps: daily walking target reached.
- Community Helper: cleanup mission complete.
- Nature Guardian: tree mission complete.
- Knowledge Seeker: reading mission complete.
- Active Citizen: two different completed missions in test mode; five in production mode.
- Profile shows earned and locked titles. Each title opens its earning rule. New titles appear in an animated award dialog with Tajik flag colours and national crown motif.

### Validation status and known limits
- GitHub Actions Android assembleDebug completed successfully on commit 9734b205714e83518852fc6f0ea59dbda4d8ac0d.
- APK artifact is shahrvand-ui-debug-apk (run 45142999315).
- Automated build success does not replace real-device GPS validation. GPS classification, accuracy, background tracking continuity, battery consumption, and award animations still require physical-device testing.
- This APK remains frontend/test-only: no Supabase, server upload, real admin approval, or real leaderboards are connected.
