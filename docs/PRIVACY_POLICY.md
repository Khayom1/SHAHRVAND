# SHAHRVAND Privacy Policy

**Updated:** 2026-11-24  
**Website:** https://khayom1.github.io/SHAHRVAND/  
**Public policy:** https://khayom1.github.io/SHAHRVAND/privacy/

## Website account data

Website registration is required from age 7. The website collects:

- First name and surname.
- Age (7–120).
- A unique username selected by the user.
- A UUID assigned automatically to the account.
- Account creation time and authentication/session data managed by Supabase Auth.

No real email address or phone number is requested. The login service uses an internal account identifier for password authentication. Password verification and credential storage are handled by Supabase Auth; SHAHRVAND does not store a plain-text password in the profile table.

The profile is stored in `public.shahrvand_profiles`, linked to the Supabase Auth user ID. Row Level Security allows a signed-in user to read only their own profile. Passwords are not returned by the profile API. A private Edge Function performs registration and username-based sign-in using server-side credentials; secret keys are never shipped to the browser.

For users under 18, a parent or guardian should know about and approve registration. The website does not currently implement independent parental identity verification.

The website does not currently provide self-service account deletion or automatic password recovery because no email address or phone number is collected. Users should keep their username and password safe. A profile correction/deletion request can be submitted through GitHub Issues; do not include passwords or sensitive information in a public issue.

## Abuse prevention

The username/password Edge Function applies registration and login limits in 10-minute windows. A hashed form of the request's network identifier is used for this purpose; the raw IP address is not stored in the private rate-limit table. Old rate-limit rows are cleaned during later authentication requests.

## Android app data

Android version 1.0.0 is currently separate from the website account. The app may store a local nickname, selected age band, language, XP, completed-mission count, and today's completed mission IDs in app-private Android preferences. These values are not synced to the website profile and remain on the device until app data is reset or the app is uninstalled.

The Android app does not request GPS, camera, microphone, contacts, or SMS permissions. It does not upload photos/videos, provide chat, use advertising SDKs, or provide a public leaderboard in V1.

## Service provider

Website profile data and authentication are hosted by Supabase. The provider may process technical request data according to its own policy: https://supabase.com/privacy.

## Changes

This policy will be updated before material data-processing changes are released. Planned features are not active features.
