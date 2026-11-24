# SHAHRVAND — Technical Status

**Release:** 1.0.0 · **Snapshot:** 2026-11-24 · **Branch:** `release/v1.0.0`

## Product scope

V1 is a small personal-growth game for age 7+: seven short missions in learning, kindness, nature, community, and focus. The Android app interface is available in Tajik, Russian, and English. Android onboarding asks only for an optional nickname and age band. The website has a separate Supabase-backed citizen account requiring first name, surname, age (7+), username, and password.

## Implemented

- Four tabs: Home, Missions, Progress, Future.
- Seven bundled missions, translated to three languages.
- XP, levels, four achievement thresholds, and a three-mission daily goal.
- A mission can award XP once per local calendar day.
- Local app-private storage for nickname, age band, language, XP, lifetime completion count, and today's completion IDs.
- Local data reset in Settings.
- Offline catalogue fallback.
- XP is labelled as personal progress, not money or official verification.
- Roadmap distinguishes active features from plans.

## Age groups and network boundary

| Age band | Mission catalogue | Profile/progress | App-server requests |
|---|---|---|---|
| 7–12 | Bundled | Device only | None |
| 13–17 | Bundled | Device only | None |
| 18+ | Supabase catalogue with bundled fallback | Device only | Read-only GET for public mission content |

The app does not send nickname, age band, XP, mission IDs, profile data, or a user token to Supabase. An 18+ catalogue request carries normal HTTP connection metadata; this is disclosed in the privacy policy. No Supabase user/auth account is created.

## Android configuration

| Setting | Value |
|---|---|
| Application ID | `tj.shahrvand.app` |
| Version | `1.0.0` / code 1 |
| Min SDK | 26 |
| Compile / target SDK | 36 / 36 |
| Java target | 17 |
| AGP / Kotlin | 8.13.2 / 2.2.21 |
| CI Gradle | 8.13 |

UI: Jetpack Compose + Material 3. The manifest declares only `INTERNET`. There are no location, camera, microphone, or location foreground-service permissions. The former GPS service has been removed from this release branch.

The APK includes the public Supabase publishable key, not a service-role or secret key.

## Supabase

Connected project: `bgfcpnlxztziigrsyuqp` (existing Khayom1 project, also used by other applications).

Tables: `public.shahrvand_missions` (7 public mission rows) and `public.shahrvand_profiles` (website citizen profiles). The profile table stores the Auth UUID, first name, surname, age, unique username, and creation time; it does not store passwords. RLS is enabled. Authenticated users can SELECT only their own profile; anonymous clients have no profile-table privileges. The `service_role` is used only inside the Edge Function.

Edge Function: `shahrvand-auth` (active, `verify_jwt=false`). It supports username/password registration and login, creates confirmed Supabase Auth identities using an internal account identifier, returns a Supabase session, and enforces rate limits through a service-role-only RPC. A synthetic authentication identifier is not a mailbox; no real email or phone is collected, so self-service password recovery is unavailable. The website stores the session through Supabase JS.

The Supabase project is shared with other Khayom1 apps. SHAHRVAND touched only its prefixed mission/profile/rate-limit objects and the `shahrvand-auth` function; existing application functions were not modified. The Android mission catalogue still uses only the public read-only endpoint in 18+ mode. Website profiles are separate from Android local progress and are not yet synchronised.

## CI and tests

`ProgressRulesTest.kt` covers level progression, current-level progress, duplicate daily completion, and daily-goal clamping.

Android CI runs the manifest permission guard, `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, and `bundleRelease`. Artifacts are a debug APK, unsigned release APK, and unsigned release AAB. CI success does not prove physical-device usability and does not sign the release.

## Explicit V1 exclusions

Android V1 has no GPS, location verification, photos/videos, uploads, chat, public ranking, admin review panel, cross-device progress sync, payment, ads, or in-app purchase. The website has private user profiles but no public member directory or real leaderboard. Mission completion is self-reported; local Android XP can be reset or modified on-device and is not independently verified.

## Remaining publication gates

1. Green CI on the final release branch head.
2. Physical Android device testing.
3. Verify no network request in 7–17 modes and successful/fallback catalogue behaviour in 18+ mode.
4. Verify Pages landing/privacy site.
5. Complete Google Play target-audience, content-rating, and Data safety declarations for the final signed AAB. Verify the package name and developer identity registration in Play Console.
6. Sign with an owner-controlled private release keystore.
7. Create the public release only after the signed APK is validated.

A signed upload AAB (for Google Play), any separately signed APK for direct distribution, and physical-device validation remain release blockers. New Google Play submissions must target API 36 or higher; this app targets 36.
