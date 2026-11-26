# SHAHRVAND V1 privacy policy

**Version:** 1.0.0  
**Effective date:** 2026-11-24  
**Public policy page:** [site/privacy/index.html](../site/privacy/index.html)

## Data stored locally

The app may store an optional nickname, selected age band, language, XP, completed mission count, and today's mission IDs in app-private Android preferences. It does not require a real name, surname, date of birth, phone number, or email.

## Optional sleep data

When a user opens the Sleep section and grants Health Connect's `READ_SLEEP` permission, the app reads available `SleepSessionRecord` entries, including session start/end times and recorded sleep stages. These health records are processed on the device only; they are not uploaded to Supabase or the SHAHRVAND server. The app stores only local XP and a per-night reward marker in app-private preferences. The user can revoke the permission at any time in Health Connect settings. Manually entered sleep records are not eligible for sleep XP.

## Network behaviour by age band

- **7–12 and 13–17:** bundled mission catalogue only. No request to Supabase or another application server is made. Profile and progress remain local.
- **18+:** the app may issue a read-only GET request for the public mission catalogue. The request does not include nickname, age band, profile, XP, mission history, or a user identifier. If the request fails, the app uses the bundled catalogue. The hosting provider may process IP address, request time, and standard HTTP request metadata in operational/security logs.

No Supabase account is created and no user authentication is performed.

## Not used in V1

- GPS or precise location
- Camera, microphone, contacts, SMS, or photo/video uploads
- Chat or direct messages
- Advertising or behavioural analytics SDKs
- Public user leaderboard

## User control

The in-app Settings screen provides a local data reset. Uninstalling the app also removes app-private local data. There is no server-side user profile to delete in V1.

## Child safety

Children should ask a parent or responsible adult before outdoor tasks or activities requiring permission. Points represent personal progress; they are not money, official verification, or a health assessment.

The policy is also published in Tajik, Russian, and English at the public policy page. The actual Google Play Data safety declaration must be completed against the final signed APK and store configuration before submission.
