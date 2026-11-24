# SHAHRVAND — V1 Build and Deployment

**Version:** 1.0.0 · **Branch:** `release/v1.0.0`

## Requirements

- JDK 17
- Android SDK platform 36
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.2.21

The inspected branch has no Gradle wrapper. Use installed Gradle or GitHub Actions.

## Local build

From repository root:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:assembleRelease :app:bundleRelease
```

Outputs:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
app/build/outputs/bundle/release/app-release.aab
```

The release APK and AAB are intentionally unsigned. New Google Play apps must be published as an Android App Bundle (AAB), then signed with the owner's upload key before submission. From 2026-08-31, new Google Play submissions must target API level 36 or higher; this app uses target SDK 36. Keep the private keystore and passwords out of source control. The workflow uploads the unsigned AAB as a separate artifact for final signing.

## Android CI

`.github/workflows/android-ui.yml` runs on pushes to `release/**` or `main`, and pull requests targeting `main`. It checks the manifest for location/camera/microphone permissions, runs unit tests, assembles debug/release APKs, and uploads both artifacts.

## Public site and privacy policy

`.github/workflows/web-preview.yml` runs on relevant `site/**` changes on `release/**` and `main`, and supports manual dispatch. It validates and deploys `site/`.

Set Repository → Settings → Pages → Source to **GitHub Actions**. Expected URLs:

- `https://khayom1.github.io/SHAHRVAND/`
- `https://khayom1.github.io/SHAHRVAND/privacy/`

Verify both after a successful Pages deployment.

## Supabase catalogue request

The app uses the public project URL and publishable key in `app/build.gradle.kts`. It calls this endpoint only in 18+ mode:

```http
GET /rest/v1/shahrvand_missions?select=id,title_tg,title_ru,title_en,description_tg,description_ru,description_en,category,icon,xp,min_age&is_active=eq.true&order=sort_order.asc
apikey: <publishable key>
Accept: application/json
```

No nickname, age band, progress, auth token, or user profile is sent. Under-18 modes do not call the repository. If the request fails, bundled content is used. Verify that Data API is enabled and `public` is exposed.


## Website citizen accounts

The public browser site at `site/index.html` uses the existing Supabase project. Registration requires first name, surname, age (7–120), unique username (4–24 lowercase Latin letters/digits/dot/underscore/hyphen), and a password of at least 8 characters. No real email or phone number is collected.

- Profile table: `public.shahrvand_profiles`; each row is keyed by the Supabase Auth UUID.
- Security: RLS restricts profile reads to the authenticated owner. The table contains no password column.
- Edge Function source: `supabase/functions/shahrvand-auth/index.ts`; deployed function: `shahrvand-auth`.
- The function uses server-side Supabase credentials to create Auth users and map a username to an internal authentication identifier. The secret key is never included in website code.
- The function limits registration and login attempts using a hashed network identifier and a 10-minute window.
- Password recovery and account self-deletion are not implemented because no real email or phone is collected.
- Website profiles are separate from Android app-private progress; cross-device Android sync and public ranking are not implemented.

The deployed Edge Function has `verify_jwt=false` because registration and login begin before a user session exists. It validates the request, restricts browser origins to the GitHub Pages origin, uses the server-side secret key, and returns a Supabase Auth session only after successful account creation or password verification.

## Manual device verification

1. Fresh install: test age-group selection and optional nickname.
2. 7–12 and 13–17: verify local missions work without network access.
3. 18+: verify catalogue fetch; test offline fallback.
4. Switch all three languages.
5. Complete a mission twice in one day; XP should increase once.
6. Reopen on a new local date; today's completion set should reset while lifetime XP remains.
7. Reset all local data and relaunch.
8. Confirm no location/camera/microphone permission prompt appears.

## Store publication gates

Complete audience, content-rating, and Data safety declarations against the final signed APK. Publish the privacy policy URL. Test on a physical device. Sign the release APK with a private owner-controlled keystore. Only then attach the signed APK to a GitHub release.

A green CI build is necessary but is not proof of production readiness by itself.
