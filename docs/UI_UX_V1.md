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
