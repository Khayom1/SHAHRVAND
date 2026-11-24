# SHAHRVAND

**SHAHRVAND 1.0.0** is a privacy-first personal-growth game built around short missions in learning, kindness, nature, focus, and everyday responsibility.

- Android / Jetpack Compose · min API 26 · target SDK 36
- Tajik, Russian, and English interface
- Onboarding uses an optional nickname and age band; no email, phone, password, or real name is required
- Ages 7–17: local-only mode, no application server requests
- Ages 18+: read-only public mission catalogue from Supabase; no profile or progress upload
- No GPS, camera, microphone, photo/video uploads, chat, ads, analytics, or public rankings in V1

## Build

Use JDK 17, Android SDK platform 36, and Gradle 8.13:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

CI produces an unsigned release APK and an unsigned Android App Bundle (AAB). Google Play new apps must use an AAB; sign it with the owner's private upload key before submission. Target SDK 36 is configured.

## Documentation

- [V1 release definition](docs/V1_RELEASE.md)
- [Technical status](docs/TECHNICAL_STATUS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Build and deployment](docs/BUILD_AND_DEPLOYMENT.md)
- [Privacy policy (TG/RU/EN)](site/privacy/index.html)

## Supabase

SHAHRVAND uses only `public.shahrvand_missions`, a public content table with RLS and SELECT-only Data API access. No user profile, age band, XP, mission completion, or authentication record is stored in Supabase. The connected Supabase project is shared with other Khayom1 apps; SHAHRVAND changes are isolated to this prefixed table.

## Roadmap

Verified submissions, trusted moderation, cross-device profiles, and a public leaderboard are future plans, not active V1 features.
